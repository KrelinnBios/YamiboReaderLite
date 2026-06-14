package org.shirakawatyu.yamibo.novel.util.forum

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.annotation.JSONCreator
import com.alibaba.fastjson2.annotation.JSONField
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
import java.util.concurrent.atomic.AtomicBoolean

data class ForumBlockedItem @JSONCreator constructor(
    @JSONField(name = "type")
    val type: String = TYPE_THREAD,
    @JSONField(name = "id")
    val id: String = "",
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
    }
}

object ForumBlocklistManager {
    private val enabledKey = stringPreferencesKey("forum_blocklist_enabled")
    private val itemsKey = stringPreferencesKey("forum_blocklist_items")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private val initialized = AtomicBoolean(false)

    private val _enabled = MutableStateFlow(true)
    val enabled = _enabled.asStateFlow()

    private val _items = MutableStateFlow<List<ForumBlockedItem>>(emptyList())
    val items = _items.asStateFlow()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        scope.launch {
            val preferences = GlobalData.dataStore?.data?.first() ?: return@launch
            _enabled.value = preferences[enabledKey]?.toBooleanStrictOrNull() ?: true
            _items.value = parseItems(preferences[itemsKey])
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
                persistItems(emptyList())
            }
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

    private suspend fun persistItems(items: List<ForumBlockedItem>) {
        _items.value = items
        GlobalData.dataStore?.edit { preferences ->
            preferences[itemsKey] = JSON.toJSONString(items)
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

    private fun normalizeType(type: String): String? = when (type.lowercase()) {
        ForumBlockedItem.TYPE_THREAD -> ForumBlockedItem.TYPE_THREAD
        ForumBlockedItem.TYPE_POST -> ForumBlockedItem.TYPE_POST
        else -> null
    }
}
