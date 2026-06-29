package org.shirakawatyu.yamibo.novel.util.manga

import java.text.Normalizer

/**
 * 漫画标题清洗工具
 */
class MangaTitleCleaner {
    companion object {
        fun getCleanThreadTitle(rawTitle: String): String {
            return rawTitle.replace(
                Regex("(?i)\\s+[-—–_]+\\s+(.*?[区板]\\s+[-—–_]+\\s+)?(百合会|论坛|手机版|Powered by).*$"),
                ""
            ).trim()
        }

        /**
         * 提取纯净书名 (用于后续去重和搜索)
         */
        fun getCleanBookName(rawTitle: String): String {
            var clean = getCleanThreadTitle(rawTitle)

            clean = clean.replace(Regex("\\s+-\\s+.*?(中文百合漫画区|百合会|论坛).*$"), "")
            clean = clean.replace(Regex("【.*?】|\\[.*?\\]"), "")
            clean = clean.replace(Regex("(?i)[\\(（]?c\\d+[\\)）]?"), "")
            clean = clean.replace(Regex("\\s*[|｜].*$"), "")
            // 3. 截断章节标记及其后面的所有内容
            val chapterMarkerPattern = Regex(
                "(?i)(" +
                        "第\\s*[\\d\\.\\-零一二两三四五六七八九十百千]+|" +
                        "[-—\\s]*[#＃]\\s*\\d+|" +
                        "[-—\\s]*S\\d+(\\s*EP\\d+)?|" +
                        "[-—\\s]*EP\\d+|" +
                        "[-—\\s]*Vol\\.?\\s*\\d+|" +
                        "[-—\\s]*Ch\\.?\\s*\\d+|" +
                        "[-—\\s]*(番外|特典|卷后附|卷彩页|附录|短篇|单行本|最终话|最終話|最终回|最終回|大结局)|" +
                        "(前篇|上篇|中篇|后篇|下篇)|" +
                        "[-—\\s]+(上|中|下)|" +
                        "[-—\\s]*[(（]\\s*[\\d\\.\\-零一二两三四五六七八九十百千]+\\s*[)）]|" +
                        "\\s*(?<!\\d)\\d+(?:\\.\\d+)?\\s*(?:[话話织回章节幕折更])?\\s*(?=[：:—\\-「【\\[(（《]|\\s|$)" +
                        ")"
            )
            val markerMatch = chapterMarkerPattern.find(clean)
            if (markerMatch != null) {
                clean = clean.substring(0, markerMatch.range.first)
            }

            clean = clean.replace(Regex("\\s*\\d+(\\.\\d+)?\\s*$"), "")
            clean = clean.replace(Regex("[！？\\?！!~。，、\\.]+$"), "")
            clean = clean.replace(Regex("^[\\s\\-|/\\)#]+|[\\s\\-|/\\(#:]+$"), "").trim()

            return clean
        }

        /**
         * 独立提取作者名前缀
         */
        fun extractAuthorPrefix(rawTitle: String): String {
            val prefixMatch = Regex("^(?:【.*?】|\\[.*?\\]|[\\s\\u00A0\\u3000])+").find(rawTitle)

            if (prefixMatch != null) {
                val bracketMatch =
                    Regex("【(.*?)】|\\[(.*?)\\]").findAll(prefixMatch.value).lastOrNull()
                if (bracketMatch != null) {
                    return bracketMatch.groupValues[1].ifEmpty { bracketMatch.groupValues[2] }
                        .trim()
                }
            }
            return ""
        }

        fun extractTranslationGroup(rawTitle: String): String {
            val title = getCleanThreadTitle(rawTitle)
            val bracketCandidate = Regex("""[【\[\(（「『]([^】\]\)）」』]{2,64})[】\]\)）」』]""")
                .findAll(title)
                .mapNotNull { extractExplicitTranslationGroup(it.groupValues[1]) }
                .firstOrNull()
            if (!bracketCandidate.isNullOrBlank()) return bracketCandidate

            return extractExplicitTranslationGroup(title).orEmpty()
        }

        fun matchesTranslationGroup(rawTitle: String, translationGroup: String): Boolean {
            if (translationGroup.isBlank()) return true
            val normalizedTitle = normalizeTranslationGroup(rawTitle)
            val normalizedGroup = normalizeTranslationGroup(translationGroup)
            if (normalizedGroup.isNotBlank() && normalizedTitle.contains(normalizedGroup)) return true

            val groupCore = normalizedGroup.replace(TRANSLATION_ROLE_PATTERN, "")
            if (groupCore.length >= 2 && normalizedTitle.contains(groupCore)) return true

            val titleGroup = extractTranslationGroup(rawTitle)
            if (titleGroup.isBlank()) return false
            val normalizedTitleGroup = normalizeTranslationGroup(titleGroup)
            val titleGroupCore = normalizedTitleGroup.replace(TRANSLATION_ROLE_PATTERN, "")
            return normalizedTitleGroup == normalizedGroup ||
                    (groupCore.length >= 2 && titleGroupCore.contains(groupCore)) ||
                    (titleGroupCore.length >= 2 && groupCore.contains(titleGroupCore))
        }

        private fun extractExplicitTranslationGroup(value: String): String? {
            val suffixForm = Regex(
                """(?<![\p{L}\p{N}·._&＆+＋×、/／\-])([\p{L}\p{N}·._&＆+＋×、/／\-]{1,40}(?:汉化组|漢化組))""",
                RegexOption.IGNORE_CASE
            )
            val prefixForm = Regex(
                """(?<![\p{L}\p{N}·._&＆+＋×、/／\-])((?:汉化|漢化)[\p{L}\p{N}·._&＆+＋×、/／\-]{1,40}(?:组|組))""",
                RegexOption.IGNORE_CASE
            )
            return sequenceOf(suffixForm, prefixForm)
                .mapNotNull { pattern -> pattern.find(value)?.groupValues?.get(1) }
                .map(::sanitizeTranslationGroup)
                .firstOrNull(String::isNotBlank)
        }

        private fun sanitizeTranslationGroup(value: String): String =
            value.replace(Regex("\\s+"), " ")
                .trim(' ', '-', '—', '_', '|', '｜', ':', '：', '·')

        private fun normalizeTranslationGroup(value: String): String =
            value.lowercase()
                .replace(Regex("[\\s\\p{Punct}【】（）「」『』·・＆＋×、／]+"), "")

        private val TRANSLATION_ROLE_PATTERN = Regex(
            "(?:联合|聯合|个人|個人|自)?(?:汉化|漢化|翻译|翻譯|译制|譯製|制作|製作|扫图|掃圖|嵌字|修图|修圖|校对|校對|图源|圖源|润色|潤色|字幕)(?:组|組|社|团队|團隊|工作室|team)?",
            RegexOption.IGNORE_CASE
        )

        /**
         * 提取核心搜索词
         */
        fun getSearchKeyword(rawTitle: String): String {
            val cleanName = getCleanBookName(rawTitle)
                .replace(Regex("\\s+"), " ")
                .trim()
            val rawSegments = getCleanThreadTitle(rawTitle)
                .replace(Regex("【.*?】|\\[.*?\\]"), "|")
                .split('|')
                .map { segment ->
                    segment
                        .replace(
                            Regex(
                                "(?i)(第\\s*[\\d\\.零一二两三四五六七八九十百千]+.*|" +
                                        "\\d+(?:\\.\\d+)?\\s*[话話回章节].*|" +
                                        "番外.*|特典.*|最终话.*|最終話.*)$"
                            ),
                            ""
                        )
                        .replace(Regex("\\s+"), "")
                        .trim(' ', '-', '—', '_', '|', '｜', '。', '！', '!', '？', '?')
                }
                .filter { it.length >= 4 && !extractTranslationGroup(it).equals(it, true) }

            return rawSegments
                .maxByOrNull { it.length }
                ?.take(24)
                ?.takeIf { it.isNotBlank() }
                ?: cleanName.take(24).trim()
        }

        fun isAdministrativeThread(rawTitle: String): Boolean {
            val normalized = rawTitle.replace(Regex("\\s+"), "")
            return listOf(
                "公告", "版规", "论坛规则", "新人须知", "快速导航",
                "任务帖", "问题反馈", "找回账号", "修改密码", "版务"
            ).any { normalized.contains(it, ignoreCase = true) }
        }

        fun matchesSearchQuery(rawTitle: String, rawQuery: String): Boolean {
            val titleVariants = buildList {
                add(
                    normalizeSearchText(rawTitle)
                )
                add(
                    normalizeSearchText(rawTitle.replace(Regex("【.*?】|\\[.*?\\]"), ""))
                )
                add(normalizeSearchText(getCleanBookName(rawTitle)))
                add(normalizeSearchText(extractTranslationGroup(rawTitle)))
            }.filter(String::isNotBlank).distinct()
            val normalizedFullQuery = normalizeSearchText(rawQuery)
            if (normalizedFullQuery.isNotBlank() &&
                titleVariants.any { it.contains(normalizedFullQuery) }
            ) {
                return true
            }

            val terms = rawQuery
                .trim()
                .split(Regex("[\\s【】\\[\\]（）()《》「」『』·・,，、/／|｜]+"))
                .filter(String::isNotBlank)
            return terms.all { term ->
                val queryVariants = listOf(
                    normalizeSearchText(term),
                    normalizeSearchText(getCleanBookName(term)),
                    normalizeSearchText(term.replace(Regex("【.*?】|\\[.*?\\]"), ""))
                ).filter(String::isNotBlank).distinct()
                queryVariants.any { query ->
                    titleVariants.any { title ->
                        title.contains(query) ||
                                fuzzyChunkMatch(title, query)
                    }
                }
            }
        }

        fun getForumSearchKeyword(rawQuery: String): String {
            val candidates = rawQuery
                .trim()
                .split(Regex("[\\s【】\\[\\]（）()《》「」『』·・,，、/／|｜]+"))
                .map { candidate ->
                    candidate
                        .replace(
                            Regex(
                                "(?i)(第\\s*[\\d\\.零一二两三四五六七八九十百千]+.*|" +
                                        "\\d+(?:\\.\\d+)?\\s*[话話回章节卷].*|" +
                                        "番外.*|特典.*|最终话.*|最終話.*)$"
                            ),
                            ""
                        )
                        .trim()
                }
                .filter { normalizeSearchText(it).length >= 2 }
            val contentCandidates = candidates.filter { candidate ->
                extractTranslationGroup(candidate).isBlank()
            }
            return (contentCandidates.ifEmpty { candidates })
                .maxByOrNull { normalizeSearchText(it).length }
                ?.take(40)
                ?.takeIf(String::isNotBlank)
                ?: rawQuery.trim().take(40)
        }

        fun getDirectoryForumSearchKeyword(
            cleanBookName: String,
            rawTitle: String,
            configuredKeywords: String?
        ): String {
            val bookKeyword = getForumSearchKeyword(cleanBookName)
                .takeIf { normalizeSearchText(it).length >= 2 }
            if (bookKeyword != null) return bookKeyword

            val configuredKeyword = configuredKeywords
                ?.takeIf(String::isNotBlank)
                ?.let(::getForumSearchKeyword)
                ?.takeIf { normalizeSearchText(it).length >= 2 }
            return configuredKeyword ?: getSearchKeyword(rawTitle)
        }

        fun matchesDirectoryCandidate(
            rawText: String,
            cleanBookName: String,
            configuredKeywords: String?
        ): Boolean {
            if (cleanBookName.isNotBlank() && matchesSearchQuery(rawText, cleanBookName)) {
                return true
            }

            val normalizedBookName = normalizeSearchText(cleanBookName)
            return configuredKeywords
                .orEmpty()
                .split(Regex("[\\s【】\\[\\]（）()《》「」『』·・,，、/／|｜]+"))
                .map(String::trim)
                .filter(String::isNotBlank)
                .filterNot { normalizeSearchText(it) == normalizedBookName }
                .any { matchesSearchQuery(rawText, it) }
        }

        fun matchesPublisher(
            authorUid: String?,
            authorName: String?,
            publisherUid: String?,
            publisherName: String?
        ): Boolean {
            val targetUid = publisherUid?.trim().orEmpty()
            val targetName = normalizeSearchText(publisherName.orEmpty())
            if (targetUid.isBlank() && targetName.isBlank()) return true

            val candidateUid = authorUid?.trim().orEmpty()
            if (targetUid.isNotBlank() && candidateUid == targetUid) return true

            val candidateName = normalizeSearchText(authorName.orEmpty())
            return targetName.isNotBlank() && candidateName == targetName
        }
        private fun normalizeSearchText(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFKC)
                .lowercase()
                .replace('臺', '台')
                .replace('裏', '里')
                .replace('話', '话')
                .replace('冊', '册')
                .replace('巻', '卷')
                .replace(Regex("[\\s\\p{Punct}【】（）《》「」『』·・]+"), "")

        private fun fuzzyChunkMatch(title: String, query: String): Boolean {
            if (query.length < 4) return false
            val chunks = query.windowed(size = 3, step = 2, partialWindows = true)
                .filter { it.length >= 2 }
            if (chunks.isEmpty()) return false
            val matched = chunks.count(title::contains)
            return matched * 3 >= chunks.size * 2
        }

        private const val NUM =
            "(\\d+(?:\\.\\d+)?|[０-９]+(?:\\.[０-９]+)?|[〇零一二两三四五六七八九十百千]+|[①-⑳]|[Ⅰ-Ⅻ])"

        private const val ARABIC = "(\\d+(?:\\.\\d+)?|[０-９]+(?:\\.[０-９]+)?)"

        fun extractChapterNum(rawTitle: String): Float {
            val cleanTitle = rawTitle
                .replace(Regex("【.*?】|\\[.*?\\]|\\(.*?\\)|（.*?）|「.*?」|《.*?》"), "")
                .replace(Regex("\\d+\\s*[xX×]\\s*\\d+"), "")
                .replace(Regex("(?i)\\bc\\d+\\b"), "")

            if (Regex(
                    "番外|特典|附录|SP|卷后附|卷彩页|小剧场|小漫画",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(rawTitle)
            ) {
                return 0f
            }
            if (Regex("最终话|最終話|最终回|最終回|大结局").containsMatchIn(cleanTitle)) {
                return 999f
            }

            // 3. 计算微调值 (前中后篇、①②③)
            var subModifier = 0f

            val modPrefix = "(?<=[\\s\\-—_/(（\\[【话話回章节幕折更\\d]|^)"
            val modSuffix = "(?=[\\s)）\\]】!！？?。，~]*$)"

            if (Regex("(?:前篇|上篇|${modPrefix}上)$modSuffix").containsMatchIn(cleanTitle)) subModifier =
                0.1f
            else if (Regex("(?:中篇|${modPrefix}中)$modSuffix").containsMatchIn(cleanTitle)) subModifier =
                0.2f
            else if (Regex("(?:后篇|下篇|${modPrefix}下)$modSuffix").containsMatchIn(cleanTitle)) subModifier =
                0.3f

            val circleMap = mapOf(
                '①' to 0.1f,
                '②' to 0.2f,
                '③' to 0.3f,
                '④' to 0.4f,
                '⑤' to 0.5f,
                '⑥' to 0.6f,
                '⑦' to 0.7f,
                '⑧' to 0.8f,
                '⑨' to 0.9f
            )
            Regex("[①②③④⑤⑥⑦⑧⑨]").find(cleanTitle)
                ?.let { subModifier = circleMap[it.value[0]] ?: 0f }

            val baseNum =
                // 规则 1.1: 明确带“话”等字眼的 其之 (e.g., "14话 其之2")
                Regex("(?:第)?\\s*$NUM\\s*[话話织回章节幕折更].*?其[之の]?\\s*$NUM").find(cleanTitle)
                    ?.let {
                        parseNumber(it.groupValues[1]) + (parseNumber(it.groupValues[2]) / 100f)
                    }
                // 规则 1.2: 没带话，但是靠得很近的其之 (限制中间长度，防止跨越太长匹配到“百合”)
                    ?: Regex("(?:第)?\\s*$NUM\\s*[^\\d零一二两三四五六七八九十百千]{0,5}?其[之の]?\\s*$NUM").find(
                        cleanTitle
                    )?.let {
                        parseNumber(it.groupValues[1]) + (parseNumber(it.groupValues[2]) / 100f)
                    }
                    // 规则 2: 第X-Y
                    ?: Regex("第\\s*$NUM\\s*[-—]\\s*$NUM").find(cleanTitle)?.let {
                        parseNumber(it.groupValues[1]) + (parseNumber(it.groupValues[2]) / 100f)
                    }
                    // 规则 3.1: (第)X话 (核心修复：不需要“第”字也能完美匹配“02话”)
                    ?: Regex("(?:第)?\\s*$NUM\\s*[话話织回章节幕折更]").find(cleanTitle)?.let {
                        parseNumber(it.groupValues[1])
                    }
                    // 规则 3.2: 第X (必须有第)
                    ?: Regex("第\\s*$NUM(?=[\\s:：,，.。!！?？|｜\\-—]|$)").find(cleanTitle)?.let {
                        parseNumber(it.groupValues[1])
                    }
                    // 规则 3.3: 无"第"无"话"的纯 X-Y (如"向笨蛋告白 2-1")。必须先于规则4匹配，
                    // 否则规则4只截取横杠后半段（如"-1"→1），把横杠前的章号（2）丢掉。
                    ?: Regex("(?<![\\d.])$ARABIC\\s*[-—]\\s*$ARABIC(?!\\d)").find(cleanTitle)?.let {
                        parseNumber(it.groupValues[1]) + (parseNumber(it.groupValues[2]) / 100f)
                    }
                    // 规则 4: 分隔符后跟数字 (限制为纯阿拉伯数字 ARABIC)
                    ?: Regex("[-—|｜]\\s*$ARABIC(?:\\s|\\.|$)").find(cleanTitle)?.let {
                        it.groupValues[1].toFloatOrNull() ?: 0f
                    }
                    // 规则 5: 孤立数字 (限制为纯阿拉伯数字 ARABIC，拒绝把“百”当成孤立数字)
                    ?: Regex("(?:^|\\s)([^\\d\\s部季名次期天卷]?)\\s*$ARABIC\\s*([^\\d\\s部季名次期天卷]?)(?=[\\s:：—\\-,，.。!！?？|｜]|$|[^\\d])").find(
                        cleanTitle
                    )?.let {
                        it.groupValues[2].toFloatOrNull() ?: 0f
                    }
                    // 规则 6: 结尾数字 (限制为纯阿拉伯数字 ARABIC)
                    ?: Regex("$ARABIC(?!.*\\d)").find(cleanTitle)?.let {
                        if (it.groupValues[1] != ".") it.groupValues[1].toFloatOrNull()
                            ?: 0f else 0f
                    }
                    ?: 0f // 兜底

            return Math.round((baseNum + subModifier) * 1000) / 1000f
        }

        private fun formatLabelNum(numStr: String): String {
            val value = parseNumber(numStr)
            return if (value % 1f == 0f) value.toInt().toString() else value.toString()
        }

        /**
         * 提取"X-Y"形式的章节显示文案（如"第7-2话"→"7-2"），用于列表直接展示原始分段，
         * 避免 extractChapterNum 为排序需要把 Y 编码进小数位（7.02）后直接拿来显示。
         * 仅当标题确实是 X-Y / 其之 这类分段形式时返回；其余情况返回 null，由调用方按
         * chapterNum 数值格式化（如单纯的"7"或前后篇的"7.1"）。
         */
        fun extractChapterLabel(rawTitle: String): String? {
            val cleanTitle = rawTitle
                .replace(Regex("【.*?】|\\[.*?\\]|\\(.*?\\)|（.*?）|「.*?」|《.*?》"), "")
                .replace(Regex("\\d+\\s*[xX×]\\s*\\d+"), "")
                .replace(Regex("(?i)\\bc\\d+\\b"), "")

            if (Regex(
                    "番外|特典|附录|SP|卷后附|卷彩页|小剧场|小漫画|最终话|最終話|最终回|最終回|大结局",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(rawTitle)
            ) {
                return null
            }

            Regex("(?:第)?\\s*$NUM\\s*[话話织回章节幕折更].*?其[之の]?\\s*$NUM").find(cleanTitle)?.let {
                return "${formatLabelNum(it.groupValues[1])}-${formatLabelNum(it.groupValues[2])}"
            }
            Regex("(?:第)?\\s*$NUM\\s*[^\\d零一二两三四五六七八九十百千]{0,5}?其[之の]?\\s*$NUM").find(cleanTitle)
                ?.let {
                    return "${formatLabelNum(it.groupValues[1])}-${formatLabelNum(it.groupValues[2])}"
                }
            Regex("第\\s*$NUM\\s*[-—]\\s*$NUM").find(cleanTitle)?.let {
                return "${formatLabelNum(it.groupValues[1])}-${formatLabelNum(it.groupValues[2])}"
            }
            Regex("(?<![\\d.])$ARABIC\\s*[-—]\\s*$ARABIC(?!\\d)").find(cleanTitle)?.let {
                return "${formatLabelNum(it.groupValues[1])}-${formatLabelNum(it.groupValues[2])}"
            }
            return null
        }

        /**
         * 暴力提取标题中出现的所有数字
         */
        fun extractAllPossibleNumbers(rawTitle: String): List<Float> {
            val cleanTitle = rawTitle
                .replace(Regex("【.*?】|\\[.*?\\]|\\(.*?\\)|（.*?）|「.*?」|《.*?》"), "")
                .replace(Regex("(?i)\\bc\\d+\\b"), "")
            // 抓出所有范围在 [0, 999) 的有效数字
            val matches = Regex(NUM)
                .findAll(cleanTitle)
                .map { parseNumber(it.groupValues[1]) }
                .filter { it in 0f..<999f }
                .toList()

            val pool = mutableSetOf<Float>()
            pool.addAll(matches)

            for (i in 0 until matches.size) {
                for (j in 0 until matches.size) {
                    if (i == j) continue

                    val major = matches[i]
                    val minor = matches[j]

                    var divisor = 10f
                    while (minor >= divisor) {
                        divisor *= 10f
                    }
                    pool.add(major + minor / divisor)

                    pool.add(major + minor / (divisor * 10f))
                }
            }
            return pool.toList()
        }

        /**
         * 从 URL 提取 tid (用于去重唯一键)
         */
        fun extractTidFromUrl(url: String): String? {
            val match = Regex("tid=(\\d+)").find(url)
                ?: Regex("thread-(\\d+)-").find(url)
                ?: Regex("[?&]ptid=(\\d+)").find(url)
            return match?.groupValues?.get(1)
        }

        /**
         * 将中文数字/阿拉伯数字/全角/罗马/圆圈 统一解析为 Float 浮点数
         */
        private fun parseNumber(numStr: String): Float {
            // 1. 标准半角阿拉伯数字
            numStr.toFloatOrNull()?.let { return it }

            // 2. 全角数字转半角
            val halfWidthStr = numStr.map {
                if (it in '０'..'９') (it.code - '０'.code + '0'.code).toChar()
                else if (it == '．') '.'
                else it
            }.joinToString("")
            halfWidthStr.toFloatOrNull()?.let { return it }

            // 3. 特殊符号映射字典
            val specialMap = mapOf(
                '①' to 1f, '②' to 2f, '③' to 3f, '④' to 4f, '⑤' to 5f,
                '⑥' to 6f, '⑦' to 7f, '⑧' to 8f, '⑨' to 9f, '⑩' to 10f,
                '⑪' to 11f, '⑫' to 12f, '⑬' to 13f, '⑭' to 14f, '⑮' to 15f,
                '⑯' to 16f, '⑰' to 17f, '⑱' to 18f, '⑲' to 19f, '⑳' to 20f,
                'Ⅰ' to 1f, 'Ⅱ' to 2f, 'Ⅲ' to 3f, 'Ⅳ' to 4f, 'Ⅴ' to 5f,
                'Ⅵ' to 6f, 'Ⅶ' to 7f, 'Ⅷ' to 8f, 'Ⅸ' to 9f, 'Ⅹ' to 10f,
                'Ⅺ' to 11f, 'Ⅻ' to 12f
            )
            // 如果提取出来的刚好是单个特殊符号
            if (numStr.length == 1 && specialMap.containsKey(numStr[0])) {
                return specialMap[numStr[0]]!!
            }

            // 4. 中文数字处理
            var total = 0f
            var number = -1f
            for (i in numStr.indices) {
                val c = numStr[i]
                val v = when (c) {
                    '〇', '零' -> 0f; '一' -> 1f; '二', '两' -> 2f; '三' -> 3f
                    '四' -> 4f; '五' -> 5f; '六' -> 6f; '七' -> 7f
                    '八' -> 8f; '九' -> 9f; else -> -1f
                }
                if (v != -1f) {
                    number = v
                } else {
                    val unit = when (c) {
                        '十' -> 10f; '百' -> 100f; '千' -> 1000f; else -> 0f
                    }
                    if (unit > 0) {
                        if (number == -1f) number = 1f
                        total += number * unit
                        number = -1f
                    }
                }
            }
            if (number != -1f) {
                total += number
            }

            if (total > 0f || numStr.contains(Regex("[〇零]"))) {
                return total
            }
            return -1f // 兜底：解析失败
        }
    }
}
