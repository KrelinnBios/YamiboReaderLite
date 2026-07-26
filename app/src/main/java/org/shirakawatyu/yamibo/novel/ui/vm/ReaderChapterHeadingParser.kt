package org.shirakawatyu.yamibo.novel.ui.vm

import org.jsoup.nodes.Element

private val readerDateHeadingRegex = Regex(
    """^(?:(?:现在|如今|当时|过去|未来|[零〇一二三四五六七八九十百\d]+年(?:前|后))\s*[-—–~～：:]\s*)?\d{3,4}\s*年\s*\d{1,2}\s*月(?:\s*\d{1,2}\s*[日号])?$"""
)
private val readerHeadingSkipAncestors = setOf("li", "a", "ul", "ol", "table")
private val readerNumberedChapterLineRegex = Regex("""^\s*(\d{1,2})(?:\s+.*)?$""")

internal data class ReaderNumberedChapterSegment(
    val text: String,
    val title: String?
)

internal fun containsReaderNumberedChapterStart(
    segments: List<ReaderNumberedChapterSegment>
): Boolean = segments.any { it.title != null }

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
