package org.shirakawatyu.yamibo.novel.ui.vm

import org.jsoup.nodes.Element

private val readerDateHeadingRegex = Regex(
    """^(?:(?:现在|如今|当时|过去|未来|[零〇一二三四五六七八九十百\d]+年(?:前|后))\s*[-—–~～：:]\s*)?\d{3,4}\s*年\s*\d{1,2}\s*月(?:\s*\d{1,2}\s*[日号])?$"""
)
private val readerTextChapterHeadingRegex = Regex(
    """^(?:序章|楔子|引子|尾声|尾聲|后记|後記|番外|第[零〇一二三四五六七八九十百千万兩两\d]+(?:章|节|節|卷|篇|幕|话|話|回))"""
)
private const val readerStructuralHeadingSelector =
    "h1, h2, h3, center, div[align=center], font[size=4], font[size=5], font[size=6], font[size=7]"
private val readerHeadingSkipAncestors = setOf("li", "a", "ul", "ol", "table")
private val readerNumberedChapterLineRegex = Regex("""^\s*(\d{1,2})(?:\s+.*)?$""")
private val readerHeadingTrailingDeny = "。！？!?".toSet()

internal data class ReaderNumberedChapterSegment(
    val text: String,
    val title: String?
)

internal fun containsReaderNumberedChapterStart(
    segments: List<ReaderNumberedChapterSegment>
): Boolean = segments.any { it.title != null }

private fun normalizeReaderHeading(value: String): String = value
    .replace(' ', ' ')
    .replace(Regex("\\s+"), " ")
    .trim()
    .replace(Regex("^[#＃]+\\s*"), "")
    .trim()

/** 提取楼层首行的“第 N 话/番外/后记”等章节标题，并兼容作者使用的 Markdown # 前缀。 */
internal fun extractReaderTextChapterHeading(firstLine: String): String? {
    val candidate = normalizeReaderHeading(firstLine)
    return candidate.takeIf {
        it.isNotBlank() && readerTextChapterHeadingRegex.containsMatchIn(it)
    }
}

/**
 * 从居中、大字号或标题标签中提取短标题。
 *
 * 不能只看第一个匹配元素：Discuz 常用一个居中/大字号父容器包住整章，真正标题在其子元素中。
 */
internal fun extractReaderStructuralHeading(node: Element): String? {
    return node.select(readerStructuralHeadingSelector)
        .asSequence()
        .filterNot { element ->
            element.parents().any { it.tagName() in readerHeadingSkipAncestors }
        }
        .map { normalizeReaderHeading(it.text()) }
        .firstOrNull { candidate ->
            candidate.isNotBlank() &&
                candidate.length <= 24 &&
                candidate.first().isLetterOrDigit() &&
                candidate.last() !in readerHeadingTrailingDeny
        }
}

/**
 * 提取楼层顶部的日期式章节标题。
 *
 * 只接受整行时间标题，例如“现在 - 2044 年 1 月”或“三年前 - 2041 年 5 月”；
 * 正文句子中即使包含年月日期，也不能被当成章节标题。
 */
internal fun extractReaderDateSubHeading(node: Element): String? {
    return node.select("p, div, center, h1, h2, h3, h4, strong, b, font")
        .firstNotNullOfOrNull { element ->
            if (element.parents().any { it.tagName() in readerHeadingSkipAncestors }) {
                return@firstNotNullOfOrNull null
            }
            element.text()
                .replace(' ', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
                .takeIf { candidate ->
                    candidate.isNotBlank() &&
                        candidate.length <= 24 &&
                        readerDateHeadingRegex.matches(candidate)
                }
        }
}

/** 将楼主正文中独立的数字章节（如“1”或“2 一边这样想着……”）拆成独立段落。 */
internal fun splitReaderNumberedChapterSegments(text: String): List<ReaderNumberedChapterSegment> {
    val segments = mutableListOf<ReaderNumberedChapterSegment>()
    val currentLines = mutableListOf<String>()
    var currentTitle: String? = null

    fun appendCurrentSegment() {
        val segmentText = currentLines.joinToString("\n").trim()
        if (segmentText.isNotBlank()) {
            segments += ReaderNumberedChapterSegment(segmentText, currentTitle)
        }
        currentLines.clear()
    }

    text.lines().forEach { line ->
        val match = readerNumberedChapterLineRegex.matchEntire(line.trim())
        if (match != null) {
            appendCurrentSegment()
            currentTitle = match.groupValues[1]
        }
        currentLines += line
    }
    appendCurrentSegment()

    return segments.ifEmpty {
        listOf(ReaderNumberedChapterSegment(text.trim(), null))
    }
}
