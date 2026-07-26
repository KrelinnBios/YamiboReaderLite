package org.shirakawatyu.yamibo.novel.ui.vm

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderChapterHeadingParserTest {
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
}
