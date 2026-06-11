package org.shirakawatyu.yamibo.novel.repository

import android.content.Context
import android.util.Log
import com.alibaba.fastjson2.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.shirakawatyu.yamibo.novel.bean.DirectoryStrategy
import org.shirakawatyu.yamibo.novel.bean.MangaChapterItem
import org.shirakawatyu.yamibo.novel.bean.MangaDirectory
import org.shirakawatyu.yamibo.novel.global.YamiboRetrofit
import org.shirakawatyu.yamibo.novel.network.MangaApi
import org.shirakawatyu.yamibo.novel.parser.MangaHtmlParser
import org.shirakawatyu.yamibo.novel.util.manga.MangaTitleCleaner
import java.io.File
import java.io.IOException
import java.util.Collections

data class DirectoryUpdateResult(val directory: MangaDirectory, val searchPerformed: Boolean)

class DirectoryRepository private constructor(private val context: Context) {
    private val mangaApi = YamiboRetrofit.getInstance().create(MangaApi::class.java)
    private val DIRECTORY_DIR = "manga_directory"
    private val LOG_TAG = "DirectoryRepo"

    private val STRIPE_COUNT = 32
    private val locks = Array(STRIPE_COUNT) { Mutex() }
    private fun getFileLock(name: String) =
        locks[(name.hashCode() and Int.MAX_VALUE) % STRIPE_COUNT]

    private val memoryCache: MutableMap<String, MangaDirectory> = Collections.synchronizedMap(
        object : LinkedHashMap<String, MangaDirectory>(20, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MangaDirectory>?) =
                size > 50
        }
    )
    private val threadFidCache: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(200, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) =
                size > 500
        }
    )

    companion object {
        private val MANGA_FIDS = setOf("30", "37")

        @Volatile
        private var instance: DirectoryRepository? = null
        fun getInstance(context: Context): DirectoryRepository = instance ?: synchronized(this) {
            instance ?: DirectoryRepository(context.applicationContext).also { instance = it }
        }
    }

    private fun getDirectoryFile(cleanName: String): File {
        val safeName = cleanName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .let { if (it.length > 50) it.substring(0, 50) else it }
        return File(
            File(context.filesDir, DIRECTORY_DIR).apply { if (!exists()) mkdirs() },
            "${safeName}_${cleanName.hashCode().toString(16)}_dir.json"
        )
    }

    private suspend fun loadDirectory(cleanName: String): MangaDirectory? {
        memoryCache[cleanName]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val file = getDirectoryFile(cleanName)
                if (!file.exists()) return@withContext null
                JSON.parseObject(file.readText(), MangaDirectory::class.java)
                    .also { if (it != null) memoryCache[cleanName] = it }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun saveDirectory(directory: MangaDirectory) = withContext(Dispatchers.IO) {
        val name = directory.cleanBookName
        val file = getDirectoryFile(name)
        val tempFile = File(file.parent, "${file.name}.tmp")
        try {
            memoryCache[name] = directory
            tempFile.writeText(JSON.toJSONString(directory))
            if (tempFile.exists()) {
                if (file.exists()) file.delete()
                tempFile.renameTo(file)
            }
            Unit
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Save failed: $name", e)
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    // 基于 TID 排序并重组序号
    private fun mergeAndSortChapters(
        old: List<MangaChapterItem>,
        new: List<MangaChapterItem>
    ): List<MangaChapterItem> {
        val map = LinkedHashMap<String, MangaChapterItem>()

        old.forEach { map[it.tid] = it }
        new.forEach { map[it.tid] = it }

        val sortedByTid = map.values.sortedBy { it.tid.toLongOrNull() ?: 0L }

        val extractedChapters = sortedByTid.map { item ->
            val extractedNum = MangaTitleCleaner.extractChapterNum(item.rawTitle)
            item.copy(chapterNum = extractedNum)
        }

        return smoothChapterDisplayNumbers(extractedChapters)
    }

    /**
     * 话数纠错
     */
    private fun smoothChapterDisplayNumbers(items: List<MangaChapterItem>): List<MangaChapterItem> {
        if (items.size < 3) return items

        val processed = items.toMutableList()
        val isBad = BooleanArray(processed.size)

        for (i in processed.indices) {
            val curr = processed[i].chapterNum
            if (curr <= 0f || curr >= 999f) continue

            var leftNum = -1f
            for (j in i - 1 downTo 0) {
                if (!isBad[j] && processed[j].chapterNum > 0f && processed[j].chapterNum < 999f) {
                    leftNum = processed[j].chapterNum
                    break
                }
            }

            if (leftNum != -1f && curr < leftNum - 3f) {
                isBad[i] = true
                continue
            }

            if (leftNum != -1f && curr > leftNum + 3f) {
                var dropCount = 0
                for (j in i + 1 until minOf(processed.size, i + 8)) {
                    val futureNum = processed[j].chapterNum
                    if (futureNum > 0f && futureNum < curr - 5f) {
                        dropCount++
                        if (dropCount >= 3) {
                            isBad[i] = true
                            break
                        }
                    }
                }
                if (isBad[i]) continue
            }

            if (leftNum == -1f) {
                var dropCount = 0
                for (j in i + 1 until minOf(processed.size, i + 8)) {
                    if (processed[j].chapterNum > 0f && processed[j].chapterNum < curr - 3f) {
                        dropCount++
                        if (dropCount >= 3) {
                            isBad[i] = true
                            break
                        }
                    }
                }
            }
        }

        for (i in processed.indices) {
            if (isBad[i]) {
                val currItem = processed[i]

                var prevValidNum = 0f
                for (j in i - 1 downTo 0) if (!isBad[j] && processed[j].chapterNum > 0f && processed[j].chapterNum < 999f) {
                    prevValidNum = processed[j].chapterNum; break
                }

                var nextValidNum = 9999f
                for (j in i + 1 until processed.size) if (!isBad[j] && processed[j].chapterNum > 0f && processed[j].chapterNum < 999f) {
                    nextValidNum = processed[j].chapterNum; break
                }

                val candidates = MangaTitleCleaner.extractAllPossibleNumbers(currItem.rawTitle)
                var bestFit = -1f

                val beautyComparator = compareBy<Float> { num ->
                    val str = java.text.DecimalFormat("0.###").format(num)
                    if (str.contains(".")) str.substringAfter(".").length else 0
                }.thenBy { it }

                val validCandidates = candidates.filter { it >= prevValidNum && it <= nextValidNum }

                if (validCandidates.isNotEmpty()) {
                    val strictGreater = validCandidates.filter { it > prevValidNum }
                    bestFit = if (strictGreater.isNotEmpty()) {
                        strictGreater.minWithOrNull(beautyComparator) ?: -1f
                    } else {
                        validCandidates.minWithOrNull(beautyComparator) ?: -1f
                    }
                }

                if (bestFit == -1f) {
                    var smartFill = -1f

                    if (prevValidNum > 0f && nextValidNum < 9999f) {
                        val expectedInt = kotlin.math.floor(prevValidNum.toDouble()).toFloat() + 1f

                        if (expectedInt > prevValidNum && expectedInt < nextValidNum && (nextValidNum - prevValidNum) <= 3.5f) {
                            val existsGlobally = processed.any { it.chapterNum == expectedInt }
                            val someoneElseClaimsIt = items.any {
                                it.tid != currItem.tid &&
                                        MangaTitleCleaner.extractAllPossibleNumbers(it.rawTitle)
                                            .contains(expectedInt)
                            }

                            if (!existsGlobally && !someoneElseClaimsIt) {
                                smartFill = expectedInt
                            }
                        }
                    }

                    if (smartFill != -1f) {
                        bestFit = smartFill
                    } else if (candidates.isNotEmpty()) {
                        bestFit = candidates.minWithOrNull(beautyComparator) ?: candidates.first()
                    }
                }

                // 最终回填
                if (bestFit != -1f) {
                    val formattedNum = Math.round(bestFit * 1000) / 1000f
                    processed[i] = currItem.copy(chapterNum = formattedNum)
                    isBad[i] = false
                } else {
                    val safeBase =
                        if (i > 0 && processed[i - 1].chapterNum > 0f && processed[i - 1].chapterNum < 999f) {
                            processed[i - 1].chapterNum
                        } else {
                            prevValidNum
                        }
                    val fallbackNum = Math.round((safeBase + 0.001f) * 1000) / 1000f
                    processed[i] = currItem.copy(chapterNum = fallbackNum)
                    isBad[i] = false
                }
            }
        }

        return processed
    }

    suspend fun initDirectoryForThread(
        tid: String,
        currentUrl: String,
        rawTitle: String,
        mobileHtml: String
    ): MangaDirectory {
        val threadTitle = MangaTitleCleaner.getCleanThreadTitle(rawTitle)
        val allDirs = getAllDirectories()
        val existingDir = allDirs.find { dir -> dir.chapters.any { it.tid == tid } }
        val detectedSourceFid = existingDir?.sourceFid ?: resolveSourceFid(tid)

        val cleanName = existingDir?.cleanBookName ?: MangaTitleCleaner.getCleanBookName(rawTitle)

        return getFileLock(cleanName).withLock {
            val cachedDir = loadDirectory(cleanName)

            // 直接提取当前页超链接
            val rawSamePageLinks = MangaHtmlParser.extractSamePageLinks(mobileHtml)

            val currentChapter = MangaChapterItem(
                tid = tid,
                rawTitle = threadTitle,
                chapterNum = MangaTitleCleaner.extractChapterNum(threadTitle),
                url = currentUrl,
                authorUid = null,
                authorName = null
            )

            val gatheredFromPage = (rawSamePageLinks + currentChapter).distinctBy { it.tid }

            if (cachedDir != null) {
                val detectedGroup = MangaTitleCleaner.extractTranslationGroup(threadTitle)
                val newStrategy =
                    if (cachedDir.strategy == DirectoryStrategy.PENDING_SEARCH && rawSamePageLinks.isNotEmpty()) {
                        DirectoryStrategy.LINKS
                    } else cachedDir.strategy

                val existingTids = cachedDir.chapters.map { it.tid }.toSet()

                val supplementaryChapters = gatheredFromPage.filter { it.tid !in existingTids }

                val detectedOrSavedGroup = cachedDir.translationGroup
                    ?: detectedGroup.takeIf { it.isNotBlank() }
                val mergedAll = mergeAndSortChapters(cachedDir.chapters, supplementaryChapters)
                val mergedChapters = if (detectedOrSavedGroup.isNullOrBlank()) {
                    mergedAll
                } else {
                    mergedAll.filter {
                        MangaTitleCleaner.matchesTranslationGroup(
                            it.rawTitle,
                            detectedOrSavedGroup
                        )
                    }
                }

                val updatedDir = cachedDir.copy(
                    chapters = mergedChapters,
                    strategy = newStrategy,
                    sourceFid = cachedDir.sourceFid ?: detectedSourceFid,
                    translationGroup = detectedOrSavedGroup
                )

                if (mergedChapters.size != cachedDir.chapters.size ||
                    updatedDir.strategy != cachedDir.strategy ||
                    updatedDir.sourceFid != cachedDir.sourceFid
                ) {
                    saveDirectory(updatedDir)
                }
                return@withLock updatedDir
            } else {
                val tagIds = MangaHtmlParser.findTagIdsMobile(mobileHtml)

                val strategy: DirectoryStrategy
                val sourceKey: String

                if (tagIds.isNotEmpty()) {
                    strategy = DirectoryStrategy.TAG
                    sourceKey = tagIds.joinToString(",")
                } else if (rawSamePageLinks.isNotEmpty()) {
                    strategy = DirectoryStrategy.LINKS
                    sourceKey = cleanName
                } else {
                    strategy = DirectoryStrategy.PENDING_SEARCH
                    sourceKey = cleanName
                }

                // 调用核心 TID 排序初始化
                val detectedGroup = MangaTitleCleaner.extractTranslationGroup(threadTitle)
                val initialAll = mergeAndSortChapters(emptyList(), gatheredFromPage)
                val initialChapters = if (detectedGroup.isBlank()) {
                    initialAll
                } else {
                    initialAll.filter {
                        MangaTitleCleaner.matchesTranslationGroup(it.rawTitle, detectedGroup)
                    }
                }

                val newDir = MangaDirectory(
                    cleanBookName = cleanName,
                    strategy = strategy,
                    sourceKey = sourceKey,
                    chapters = initialChapters,
                    sourceFid = detectedSourceFid,
                    translationGroup = detectedGroup.takeIf { it.isNotBlank() }
                )

                saveDirectory(newDir)
                return@withLock newDir
            }
        }
    }

    suspend fun manuallyUpdateDirectory(
        currentDir: MangaDirectory,
        forceSearch: Boolean = false,
        currentTid: String? = null
    ): Result<DirectoryUpdateResult> = withContext(Dispatchers.IO) {
        val newChapters = mutableListOf<MangaChapterItem>()
        var searchPerformed = false
        try {
            val targetChapter = currentDir.chapters.find { it.tid == currentTid }
                ?: currentDir.chapters.lastOrNull()

            val firstRawTitle = targetChapter?.rawTitle ?: currentDir.cleanBookName
            val exactKeyword = currentDir.searchKeyword
            val sourceFid = (
                    resolveSourceFid(currentTid ?: targetChapter?.tid)
                        ?: currentDir.sourceFid
                    )?.takeIf { it in MANGA_FIDS }
                ?: return@withContext Result.failure(
                    Exception("无法确认当前漫画所属版区，请重新打开原帖后再更新")
                )

            if (!forceSearch && currentDir.strategy == DirectoryStrategy.TAG) {
                val tagIdList = currentDir.sourceKey.split(",")
                for ((index, tagId) in tagIdList.withIndex()) {
                    if (tagId.isBlank()) continue
                    val html1 = mangaApi.getTagPageHtml(tagId, 1).string()
                    val parsed = MangaHtmlParser.parseListHtml(html1, index)
                    if (parsed.isNotEmpty()) {
                        newChapters.addAll(parsed)
                        val total = MangaHtmlParser.extractTotalPages(html1)
                        if (total > 1) {
                            for (p in 2..total) {
                                newChapters.addAll(
                                    MangaHtmlParser.parseListHtml(
                                        mangaApi.getTagPageHtml(tagId, p).string(), index
                                    )
                                )
                            }
                        }
                    }
                }

                if (newChapters.isEmpty()) {
                    searchPerformed = true
                    val res = performSearch(firstRawTitle, exactKeyword, sourceFid)
                    if (res.isFailure) return@withContext Result.failure(res.exceptionOrNull()!!)
                    newChapters.addAll(res.getOrNull()!!)
                }
            } else {
                searchPerformed = true
                val res = performSearch(firstRawTitle, exactKeyword, sourceFid)
                if (res.isFailure) return@withContext Result.failure(res.exceptionOrNull()!!)
                newChapters.addAll(res.getOrNull()!!)
            }

            val verifiedNewChapters = filterChaptersBySourceFid(newChapters, sourceFid)
            val latestSnapshot = loadDirectory(currentDir.cleanBookName) ?: currentDir
            val targetGroup = latestSnapshot.translationGroup.orEmpty()
            val existingCandidates = if (targetGroup.isBlank()) {
                latestSnapshot.chapters
            } else {
                latestSnapshot.chapters.filter {
                    MangaTitleCleaner.matchesTranslationGroup(it.rawTitle, targetGroup)
                }
            }
            val verifiedExistingChapters =
                filterChaptersBySourceFid(
                    chapters = existingCandidates,
                    sourceFid = sourceFid,
                    keepUnresolved = true
                )

            val finalDir = getFileLock(currentDir.cleanBookName).withLock {
                val latest = loadDirectory(currentDir.cleanBookName) ?: currentDir
                val filteredNewChapters = if (targetGroup.isBlank()) {
                    verifiedNewChapters
                } else {
                    verifiedNewChapters.filter {
                        MangaTitleCleaner.matchesTranslationGroup(it.rawTitle, targetGroup)
                    }
                }
                val merged =
                    mergeAndSortChapters(verifiedExistingChapters, filteredNewChapters)
                val updated = latest.copy(
                    chapters = merged,
                    lastUpdateTime = System.currentTimeMillis(),
                    sourceFid = sourceFid,
                    strategy = if (latest.strategy != DirectoryStrategy.TAG) DirectoryStrategy.SEARCHED else latest.strategy
                )
                saveDirectory(updated)
                updated
            }
            Result.success(DirectoryUpdateResult(finalDir, searchPerformed))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun performSearch(
        rawTitle: String,
        exactKeyword: String? = null,
        sourceFid: String?
    ): Result<List<MangaChapterItem>> {
        val safeSourceFid = sourceFid
            ?.takeIf { it in MANGA_FIDS }
            ?: return Result.failure(Exception("无法确认当前漫画所属版区，请重新打开原帖后再更新"))
        val safeKeyword = exactKeyword
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: MangaTitleCleaner.getSearchKeyword(rawTitle)

        val profileJson = JSON.parseObject(mangaApi.getForumDisplay(safeSourceFid, 1).string())
        val formHash = profileJson.getJSONObject("Variables")?.getString("formhash")
            ?.takeIf(String::isNotBlank)
            ?: return Result.failure(Exception("无法获取搜索校验信息"))
        val firstPageHtml = mangaApi.searchForum(
            formHash = formHash,
            fids = listOf(safeSourceFid),
            keyword = safeKeyword
        ).string()
        if (MangaHtmlParser.isFloodControlOrError(firstPageHtml)) return Result.failure(Exception("触发论坛防灌水限制，请稍后再试"))

        val allItems = mutableListOf<MangaChapterItem>()
        allItems.addAll(MangaHtmlParser.parseListHtml(firstPageHtml))

        val totalPages = MangaHtmlParser.extractTotalPages(firstPageHtml)
        val searchId = MangaHtmlParser.extractSearchId(firstPageHtml)

        if (searchId != null && totalPages > 1) {
            for (p in 2..totalPages) {
                val pageHtml = mangaApi.searchForumPage(searchid = searchId, page = p).string()
                allItems.addAll(MangaHtmlParser.parseListHtml(pageHtml))
            }
        }

        val matchedItems = allItems
            .asSequence()
            .filterNot { MangaTitleCleaner.isAdministrativeThread(it.rawTitle) }
            .filter { MangaTitleCleaner.matchesSearchQuery(it.rawTitle, safeKeyword) }
            .distinctBy { it.tid }
            .toList()

        return Result.success(matchedItems)
    }

    private suspend fun resolveThreadFid(tid: String?): String? {
        if (tid.isNullOrBlank()) return null
        threadFidCache[tid]?.let { return it }
        val fid = runCatching {
            val root = JSON.parseObject(mangaApi.getThreadDetailApi(tid).string())
            root.getJSONObject("Variables")
                ?.getJSONObject("thread")
                ?.getString("fid")
                ?.takeIf(String::isNotBlank)
        }.getOrNull()
        if (fid != null) threadFidCache[tid] = fid
        return fid
    }

    private suspend fun resolveSourceFid(tid: String?): String? =
        resolveThreadFid(tid)?.takeIf { it in MANGA_FIDS }

    private suspend fun filterChaptersBySourceFid(
        chapters: List<MangaChapterItem>,
        sourceFid: String,
        keepUnresolved: Boolean = false
    ): List<MangaChapterItem> = coroutineScope {
        val limiter = Semaphore(4)
        chapters
            .distinctBy { it.tid }
            .map { chapter ->
                async {
                    val chapterFid = limiter.withPermit {
                        resolveThreadFid(chapter.tid)
                    }
                    chapter.takeIf {
                        chapterFid == sourceFid || (keepUnresolved && chapterFid == null)
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    suspend fun getDirectoryByCleanName(cleanName: String): MangaDirectory? = loadDirectory(cleanName)

    suspend fun getAllDirectories(): List<MangaDirectory> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, DIRECTORY_DIR)
        if (!dir.exists()) return@withContext emptyList()

        val files = dir.listFiles { _, name -> name.endsWith("_dir.json") }
            ?: return@withContext emptyList()
        files.mapNotNull { file ->
            try {
                JSON.parseObject(file.readText(), MangaDirectory::class.java).also {
                    if (it != null) memoryCache[it.cleanBookName] = it
                }
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.lastUpdateTime }
    }

    suspend fun deleteDirectory(cleanName: String): Boolean = withContext(Dispatchers.IO) {
        getFileLock(cleanName).withLock {
            memoryCache.remove(cleanName)
            val file = getDirectoryFile(cleanName)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        }
    }

    suspend fun clearAllDirectories(): Boolean = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, DIRECTORY_DIR)
        if (!dir.exists()) return@withContext true

        memoryCache.clear()
        val files =
            dir.listFiles { _, name -> name.endsWith("_dir.json") } ?: return@withContext true
        var allDeleted = true
        for (file in files) {
            if (!file.delete()) allDeleted = false
        }
        allDeleted
    }

    suspend fun renameAndMergeDirectory(
        currentDir: MangaDirectory,
        newCleanName: String,
        newSearchKeyword: String,
        newTranslationGroup: String,
        currentTid: String
    ): MangaDirectory = withContext(Dispatchers.IO) {
        val oldName = currentDir.cleanBookName
        val resolvedSourceFid = resolveSourceFid(currentTid)
            ?: currentDir.sourceFid
            ?: resolveSourceFid(currentDir.chapters.lastOrNull()?.tid)
        val sourceChanged = currentDir.sourceFid != null &&
                resolvedSourceFid != null &&
                currentDir.sourceFid != resolvedSourceFid
        val currentSectionChapters = if (sourceChanged) {
            currentDir.chapters.filter { it.tid == currentTid }
        } else {
            currentDir.chapters
        }

        if (oldName == newCleanName &&
            currentDir.searchKeyword == newSearchKeyword &&
            currentDir.translationGroup.orEmpty() == newTranslationGroup &&
            currentDir.sourceFid == resolvedSourceFid
        ) return@withContext currentDir

        fun filteredChapters(chapters: List<MangaChapterItem>): List<MangaChapterItem> =
            if (newTranslationGroup.isBlank()) chapters
            else chapters.filter {
                MangaTitleCleaner.matchesTranslationGroup(it.rawTitle, newTranslationGroup)
            }

        if (oldName == newCleanName) {
            getFileLock(oldName).withLock {
                val mergedDir = currentDir.copy(
                    searchKeyword = newSearchKeyword,
                    sourceFid = resolvedSourceFid,
                    translationGroup = newTranslationGroup.takeIf { it.isNotBlank() },
                    chapters = filteredChapters(currentSectionChapters),
                    lastUpdateTime = System.currentTimeMillis()
                )
                saveDirectory(mergedDir)
                return@withLock mergedDir
            }
        } else {
            val lock1 = getFileLock(if (oldName < newCleanName) oldName else newCleanName)
            val lock2 = getFileLock(if (oldName < newCleanName) newCleanName else oldName)

            lock1.withLock {
                lock2.withLock {
                    val targetDir = loadDirectory(newCleanName)
                        ?.takeIf { it.sourceFid == resolvedSourceFid }

                    val mergedChapters = if (targetDir != null) {
                        mergeAndSortChapters(
                            filteredChapters(targetDir.chapters),
                            filteredChapters(currentSectionChapters)
                        )
                    } else filteredChapters(currentSectionChapters)

                    val newStrategy = targetDir?.strategy ?: currentDir.strategy
                    val newSourceKey = targetDir?.sourceKey
                        ?: if (currentDir.strategy == DirectoryStrategy.TAG) currentDir.sourceKey else newCleanName

                    val mergedDir = MangaDirectory(
                        cleanBookName = newCleanName,
                        strategy = newStrategy,
                        sourceKey = newSourceKey,
                        chapters = mergedChapters,
                        lastUpdateTime = System.currentTimeMillis(),
                        searchKeyword = newSearchKeyword,
                        sourceFid = resolvedSourceFid,
                        translationGroup = newTranslationGroup.takeIf { it.isNotBlank() }
                    )

                    saveDirectory(mergedDir)

                    val oldFile = getDirectoryFile(oldName)
                    if (oldFile.exists()) oldFile.delete()
                    memoryCache.remove(oldName)

                    return@withLock mergedDir
                }
            }
        }
    }
}
