package org.shirakawatyu.yamibo.novel.util.forum

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.annotation.JSONCreator
import com.alibaba.fastjson2.annotation.JSONField
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.shirakawatyu.yamibo.novel.global.GlobalData
import org.shirakawatyu.yamibo.novel.util.CurrentUserUtil
import org.shirakawatyu.yamibo.novel.util.YamiboSession
import java.util.concurrent.atomic.AtomicBoolean

data class ForumBlockedItem @JSONCreator constructor(
    @JSONField(name = "type")
    val type: String,
    @JSONField(name = "id")
    val id: String,
    @JSONField(name = "title")
    val title: String = "",
    @JSONField(name = "authorUid")
    val authorUid: String = "",
    @JSONField(name = "authorName")
    val authorName: String = ""
) {
    companion object {
        const val TYPE_THREAD = "thread"
        const val TYPE_POST = "post"
        const val TYPE_USER = "user"
    }
}

private data class PendingUserAction @JSONCreator constructor(
    @JSONField(name = "action")
    val action: String,
    @JSONField(name = "uid")
    val uid: String,
    @JSONField(name = "username")
    val username: String = ""
) {
    companion object {
        const val ACTION_ADD = "add"
        const val ACTION_REMOVE = "remove"
    }
}

object ForumBlocklistManager {
    private val enabledKey = stringPreferencesKey("forum_blocklist_enabled")
    private val itemsKey = stringPreferencesKey("forum_blocklist_items")
    private val pendingUserActionsKey = stringPreferencesKey("forum_blocklist_pending_user_actions")
    private val remoteUidKey = stringPreferencesKey("forum_blocklist_remote_uid")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private val remoteSyncMutex = Mutex()
    private val initialized = AtomicBoolean(false)
    private val initialization = CompletableDeferred<Unit>()
    private var lastRemoteSyncAt = 0L
    private var lastRemoteAuthHash: Int? = null
    private var remoteUid = ""

    private val _enabled = MutableStateFlow(true)
    val enabled = _enabled.asStateFlow()

    private val _items = MutableStateFlow<List<ForumBlockedItem>>(emptyList())
    val items = _items.asStateFlow()

    private val pendingUserActions = MutableStateFlow<List<PendingUserAction>>(emptyList())

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        scope.launch {
            try {
                val preferences = GlobalData.dataStore?.data?.first() ?: return@launch
                _enabled.value = preferences[enabledKey]?.toBooleanStrictOrNull() ?: true
                _items.value = parseItems(preferences[itemsKey])
                pendingUserActions.value = parsePendingActions(preferences[pendingUserActionsKey])
                remoteUid = preferences[remoteUidKey].orEmpty()
                    .takeIf { it.matches(Regex("[1-9]\\d*")) }
                    .orEmpty()
            } finally {
                initialization.complete(Unit)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        scope.launch {
            GlobalData.dataStore?.edit { preferences ->
                preferences[enabledKey] = enabled.toString()
            }
        }
    }

    fun add(
        type: String,
        id: String,
        title: String = "",
        authorUid: String = "",
        authorName: String = ""
    ) {
        val normalizedType = normalizeType(type) ?: return
        val normalizedId = id.trim().takeIf { it.matches(Regex("\\d+")) } ?: return
        val normalizedAuthorUid = authorUid.trim().takeIf { it.matches(Regex("\\d+")) }.orEmpty()
        val normalizedAuthorName = authorName.trim()
        if (normalizedType == ForumBlockedItem.TYPE_USER) {
            val username = normalizedAuthorName.ifBlank { title.trim() }
            if (username.isBlank()) return
            addUser(normalizedId, username)
            return
        }
        scope.launch {
            writeMutex.withLock {
                val current = _items.value
                if (current.any { it.type == normalizedType && it.id == normalizedId }) return@withLock
                val fallbackTitle =
                    if (normalizedType == ForumBlockedItem.TYPE_THREAD) "主题 $normalizedId" else "楼层 $normalizedId"
                val next = current + ForumBlockedItem(
                    type = normalizedType,
                    id = normalizedId,
                    title = title.trim().ifBlank { fallbackTitle },
                    authorUid = normalizedAuthorUid,
                    authorName = normalizedAuthorName
                )
                persistItems(next)
            }
        }
    }

    fun remove(type: String, id: String) {
        val normalizedType = normalizeType(type) ?: return
        val normalizedId = id.trim()
        if (normalizedType == ForumBlockedItem.TYPE_USER) {
            if (!normalizedId.matches(Regex("[1-9]\\d*"))) return
            removeUser(normalizedId)
            return
        }
        scope.launch {
            writeMutex.withLock {
                val next = _items.value.filterNot {
                    it.type == normalizedType && it.id == normalizedId
                }
                if (next.size != _items.value.size) persistItems(next)
            }
        }
    }

    fun clear() {
        scope.launch {
            writeMutex.withLock {
                val removeActions = _items.value
                    .filter { it.type == ForumBlockedItem.TYPE_USER }
                    .map {
                        PendingUserAction(
                            action = PendingUserAction.ACTION_REMOVE,
                            uid = it.id,
                            username = it.authorName.ifBlank { it.title }
                        )
                    }
                val nextPending = mergePendingActions(pendingUserActions.value, removeActions)
                persistState(emptyList(), nextPending)
            }
            syncRemote(force = true)
        }
    }

    fun syncRemote(force: Boolean = false) {
        initialize()
        scope.launch {
            initialization.await()
            syncRemoteInternal(force)
        }
    }

    fun clearSyncedUsers() {
        scope.launch {
            initialization.await()
            writeMutex.withLock {
                persistState(
                    items = _items.value.filterNot { it.type == ForumBlockedItem.TYPE_USER },
                    pending = emptyList(),
                    syncedUid = ""
                )
            }
            lastRemoteSyncAt = 0L
            lastRemoteAuthHash = null
        }
    }

    fun itemsJson(items: List<ForumBlockedItem> = _items.value): String =
        JSON.toJSONString(items)

    fun parseInput(input: String): ForumBlockedItem? {
        val value = input.trim()
        if (value.isBlank() || Regex("[?&]authorid=\\d+", RegexOption.IGNORE_CASE).containsMatchIn(value)) {
            return null
        }

        val threadId = Regex("thread-(\\d+)", RegexOption.IGNORE_CASE)
            .find(value)?.groupValues?.getOrNull(1)
            ?: Regex("[?&]tid=(\\d+)", RegexOption.IGNORE_CASE)
                .find(value)?.groupValues?.getOrNull(1)
        if (threadId != null) {
            return ForumBlockedItem(
                type = ForumBlockedItem.TYPE_THREAD,
                id = threadId,
                title = "主题 $threadId"
            )
        }

        val postId = Regex("[?&]pid=(\\d+)", RegexOption.IGNORE_CASE)
            .find(value)?.groupValues?.getOrNull(1)
        if (postId != null) {
            return ForumBlockedItem(
                type = ForumBlockedItem.TYPE_POST,
                id = postId,
                title = "楼层 $postId"
            )
        }

        if (value.matches(Regex("\\d+"))) {
            return ForumBlockedItem(
                type = ForumBlockedItem.TYPE_THREAD,
                id = value,
                title = "主题 $value"
            )
        }
        return null
    }

    private fun addUser(uid: String, username: String) {
        scope.launch {
            initialization.await()
            writeMutex.withLock {
                val item = ForumBlockedItem(
                    type = ForumBlockedItem.TYPE_USER,
                    id = uid,
                    title = username,
                    authorUid = uid,
                    authorName = username
                )
                val nextItems = _items.value
                    .filterNot { it.type == ForumBlockedItem.TYPE_USER && it.id == uid } + item
                val nextPending = mergePendingActions(
                    pendingUserActions.value,
                    listOf(
                        PendingUserAction(
                            action = PendingUserAction.ACTION_ADD,
                            uid = uid,
                            username = username
                        )
                    )
                )
                persistState(nextItems, nextPending)
            }
            syncRemote(force = true)
        }
    }

    private fun removeUser(uid: String) {
        scope.launch {
            initialization.await()
            writeMutex.withLock {
                val existing = _items.value.firstOrNull {
                    it.type == ForumBlockedItem.TYPE_USER && it.id == uid
                }
                val nextItems = _items.value.filterNot {
                    it.type == ForumBlockedItem.TYPE_USER && it.id == uid
                }
                val nextPending = mergePendingActions(
                    pendingUserActions.value,
                    listOf(
                        PendingUserAction(
                            action = PendingUserAction.ACTION_REMOVE,
                            uid = uid,
                            username = existing?.authorName.orEmpty().ifBlank {
                                existing?.title.orEmpty()
                            }
                        )
                    )
                )
                persistState(nextItems, nextPending)
            }
            syncRemote(force = true)
        }
    }

    private suspend fun syncRemoteInternal(force: Boolean) {
        val cookie = YamiboSession.cookieFor("https://bbs.yamibo.com/")
        val auth = Regex("EeqY_2132_auth=([^;]+)").find(cookie)?.groupValues?.getOrNull(1)
            ?: return
        val authHash = auth.hashCode()
        val now = System.currentTimeMillis()
        if (!force && authHash == lastRemoteAuthHash && now - lastRemoteSyncAt < 5 * 60_000L) {
            return
        }

        remoteSyncMutex.withLock {
            val refreshedCookie = YamiboSession.cookieFor("https://bbs.yamibo.com/")
            val refreshedAuth = Regex("EeqY_2132_auth=([^;]+)")
                .find(refreshedCookie)
                ?.groupValues
                ?.getOrNull(1)
                ?: return@withLock
            val refreshedHash = refreshedAuth.hashCode()
            val refreshedNow = System.currentTimeMillis()
            if (
                !force &&
                refreshedHash == lastRemoteAuthHash &&
                refreshedNow - lastRemoteSyncAt < 5 * 60_000L
            ) {
                return@withLock
            }

            var snapshot = runCatching { ForumBlacklistRemoteClient.fetchSnapshot() }
                .getOrNull()
                ?: return@withLock
            if (snapshot.currentUid.isNotBlank()) CurrentUserUtil.save(snapshot.currentUid)
            val accountChanged =
                remoteUid.isNotBlank() &&
                    snapshot.currentUid.isNotBlank() &&
                    remoteUid != snapshot.currentUid
            if (accountChanged) {
                writeMutex.withLock {
                    persistRemoteSnapshot(snapshot, emptyList())
                }
            } else {
                applyRemoteSnapshot(snapshot)
            }

            while (true) {
                val action = writeMutex.withLock { pendingUserActions.value.firstOrNull() }
                    ?: break
                val updatedSnapshot = runCatching {
                    when (action.action) {
                        PendingUserAction.ACTION_ADD ->
                            ForumBlacklistRemoteClient.addUser(action.username)

                        PendingUserAction.ACTION_REMOVE ->
                            ForumBlacklistRemoteClient.removeUser(action.uid)

                        else -> snapshot
                    }
                }.getOrNull() ?: break
                snapshot = updatedSnapshot
                val actionApplied = when (action.action) {
                    PendingUserAction.ACTION_ADD ->
                        snapshot.users.any { it.id == action.uid }

                    PendingUserAction.ACTION_REMOVE ->
                        snapshot.users.none { it.id == action.uid }

                    else -> true
                }
                if (!actionApplied) {
                    applyRemoteSnapshot(snapshot)
                    break
                }
                writeMutex.withLock {
                    val nextPending = pendingUserActions.value.filterNot { it == action }
                    persistRemoteSnapshot(snapshot, nextPending)
                }
            }

            val finalSnapshot = runCatching { ForumBlacklistRemoteClient.fetchSnapshot() }
                .getOrNull()
                ?: snapshot
            if (finalSnapshot.currentUid.isNotBlank()) CurrentUserUtil.save(finalSnapshot.currentUid)
            applyRemoteSnapshot(finalSnapshot)
            lastRemoteAuthHash = refreshedHash
            lastRemoteSyncAt =
                if (pendingUserActions.value.isEmpty()) System.currentTimeMillis() else 0L
        }
    }

    private suspend fun applyRemoteSnapshot(snapshot: ForumBlacklistSnapshot) {
        writeMutex.withLock {
            persistRemoteSnapshot(snapshot, pendingUserActions.value)
        }
    }

    private suspend fun persistRemoteSnapshot(
        snapshot: ForumBlacklistSnapshot,
        pending: List<PendingUserAction>
    ) {
        val users = snapshot.users.associateBy { it.id }.toMutableMap()
        pending.forEach { action ->
            when (action.action) {
                PendingUserAction.ACTION_ADD -> {
                    users[action.uid] = ForumBlockedItem(
                        type = ForumBlockedItem.TYPE_USER,
                        id = action.uid,
                        title = action.username,
                        authorUid = action.uid,
                        authorName = action.username
                    )
                }

                PendingUserAction.ACTION_REMOVE -> users.remove(action.uid)
            }
        }
        val contentItems = _items.value.filterNot { it.type == ForumBlockedItem.TYPE_USER }
        persistState(
            items = contentItems + users.values.sortedBy { it.authorName.lowercase() },
            pending = pending,
            syncedUid = snapshot.currentUid.ifBlank { remoteUid }
        )
    }

    private suspend fun persistItems(items: List<ForumBlockedItem>) {
        persistState(items, pendingUserActions.value)
    }

    private suspend fun persistState(
        items: List<ForumBlockedItem>,
        pending: List<PendingUserAction>,
        syncedUid: String = remoteUid
    ) {
        _items.value = items
        pendingUserActions.value = pending
        remoteUid = syncedUid
        GlobalData.dataStore?.edit { preferences ->
            preferences[itemsKey] = JSON.toJSONString(items)
            preferences[pendingUserActionsKey] = JSON.toJSONString(pending)
            preferences[remoteUidKey] = syncedUid
        }
    }

    private fun parseItems(raw: String?): List<ForumBlockedItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            JSON.parseArray(raw, ForumBlockedItem::class.java)
                .orEmpty()
                .mapNotNull { item ->
                    val type = normalizeType(item.type) ?: return@mapNotNull null
                    val id = item.id.trim().takeIf { it.matches(Regex("\\d+")) }
                        ?: return@mapNotNull null
                    item.copy(type = type, id = id)
                }
                .distinctBy { "${it.type}:${it.id}" }
        }.getOrDefault(emptyList())
    }

    private fun parsePendingActions(raw: String?): List<PendingUserAction> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            JSON.parseArray(raw, PendingUserAction::class.java)
                .orEmpty()
                .mapNotNull { action ->
                    val uid = action.uid.trim().takeIf { it.matches(Regex("[1-9]\\d*")) }
                        ?: return@mapNotNull null
                    val normalizedAction = when (action.action) {
                        PendingUserAction.ACTION_ADD -> PendingUserAction.ACTION_ADD
                        PendingUserAction.ACTION_REMOVE -> PendingUserAction.ACTION_REMOVE
                        else -> return@mapNotNull null
                    }
                    if (
                        normalizedAction == PendingUserAction.ACTION_ADD &&
                        action.username.isBlank()
                    ) {
                        return@mapNotNull null
                    }
                    action.copy(
                        action = normalizedAction,
                        uid = uid,
                        username = action.username.trim()
                    )
                }
                .fold(emptyList(), ::mergePendingAction)
        }.getOrDefault(emptyList())
    }

    private fun mergePendingActions(
        current: List<PendingUserAction>,
        incoming: List<PendingUserAction>
    ): List<PendingUserAction> = incoming.fold(current, ::mergePendingAction)

    private fun mergePendingAction(
        current: List<PendingUserAction>,
        incoming: PendingUserAction
    ): List<PendingUserAction> = current.filterNot { it.uid == incoming.uid } + incoming

    private fun normalizeType(type: String): String? = when (type.lowercase()) {
        ForumBlockedItem.TYPE_THREAD -> ForumBlockedItem.TYPE_THREAD
        ForumBlockedItem.TYPE_POST -> ForumBlockedItem.TYPE_POST
        ForumBlockedItem.TYPE_USER -> ForumBlockedItem.TYPE_USER
        else -> null
    }
}
