package org.shirakawatyu.yamibo.novel.ui.vm

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderChapterHeadingParserTest {
    @Test
    fun extractReaderTextChapterHeading_stripsMarkdownHeadingMarker() {
        assertEquals("第一话", extractReaderTextChapterHeading("# 第一话"))
        assertEquals(
            "番外篇 蟑螂 · 任务",
            extractReaderTextChapterHeading("＃ 番外篇  蟑螂 · 任务")
        )
        assertNull(extractReaderTextChapterHeading("我一直憧憬着莉莉。"))
    }

    @Test
    fun extractReaderStructuralHeading_skipsLongContainerAndFindsNestedTitle() {
        val body = "正文段落。".repeat(30)
        val node = Jsoup.parse(
            """
            <div class="message">
              <div align="center">
                <font size="4"># 最强的蛋包饭</font>
                <font size="4">$body</font>
              </div>
            </div>
            """.trimIndent()
        ).selectFirst(".message")!!

        assertEquals("最强的蛋包饭", extractReaderStructuralHeading(node))
    }

    @Test
    fun extractReaderStructuralHeading_doesNotUseBodySentenceAsTitle() {
        val node = Jsoup.parse(
            """<div class="message"><font size="4">我一直憧憬着莉莉。</font></div>"""
        ).selectFirst(".message")!!

        assertNull(extractReaderStructuralHeading(node))
    }

    @Test
    fun extractReaderDateSubHeading_doesNotTreatSentenceContainingDateAsHeading() {
        val node = Jsoup.parse(
            """
            <div class="message">
              <div align="center">
                <font size="1"><i>值得一提的是，在2022年5月15日开办的</i></font>
                <a href="https://example.com"><strong>日本第53届星云奖投票活动</strong></a>
                <font size="1"><i>中，作品列入最终候选名单</i></font>
              </div>
            </div>
            """.trimIndent()
        ).selectFirst(".message")!!

        assertNull(extractReaderDateSubHeading(node))
    }

    @Test
    fun extractReaderDateSubHeading_acceptsStandaloneDateHeadings() {
        val currentDateHeading = Jsoup.parse(
            """<div class="message"><div align="center">现在 - 2044 年 1 月</div></div>"""
        ).selectFirst(".message")!!
        val relativeDateHeading = Jsoup.parse(
            """<div class="message"><font size="4">三年前 - 2041 年 5 月</font></div>"""
        ).selectFirst(".message")!!

        assertEquals("现在 - 2044 年 1 月", extractReaderDateSubHeading(currentDateHeading))
        assertEquals("三年前 - 2041 年 5 月", extractReaderDateSubHeading(relativeDateHeading))
    }

    @Test
    fun splitReaderNumberedChapterSegments_splitsEmbeddedAndLeadingChapterNumbers() {
        val embedded = splitReaderNumberedChapterSegments(
            "卖身之事\n著：小野美由纪\n1\n当我得到这闪耀着银色光芒的新身体时……"
        )
        val leading = splitReaderNumberedChapterSegments(
            "2 一边这样想着，我在位于贫民窟15公里外的市区办公窗口办理完手续。"
        )

        assertEquals(listOf(null, "1"), embedded.map { it.title })
        assertEquals(listOf("2"), leading.map { it.title })
        assertEquals("2 一边这样想着，我在位于贫民窟15公里外的市区办公窗口办理完手续。", leading.single().text)
        assertEquals(true, containsReaderNumberedChapterStart(embedded))
        assertEquals(true, containsReaderNumberedChapterStart(leading))
        assertEquals(
            false,
            containsReaderNumberedChapterStart(
                splitReaderNumberedChapterSegments("没有数字章节的普通正文")
            )
        )
    }
}
