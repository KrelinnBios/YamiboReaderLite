package org.shirakawatyu.yamibo.novel.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadHtmlParserTest {
    @Test
    fun parseReaderPage_keepsAuthorizedLockedContentAndFiltersReplies() {
        val html = """
            <html>
            <head><title>测试小说 - 文學區 - 百合会</title></head>
            <body>
              <h1 id="thread_subject">测试小说</h1>
              <div id="post_1">
                <a href="forum.php?mod=viewthread&tid=1&authorid=100">只看该作者</a>
                <table><tbody><tr><td class="t_f" id="postmessage_1">
                    第三章
                    <div class="locked-content"><span class="locked-tip">30</span>有权限正文</div>
                </td></tr></tbody></table>
              </div>
              <div id="post_2">
                <a href="forum.php?mod=viewthread&tid=1&authorid=200">只看该作者</a>
                <table><tbody><tr><td class="t_f" id="postmessage_2">普通回复</td></tr></tbody></table>
              </div>
              <div class="pg"><a href="forum.php?mod=viewthread&tid=1&page=3">3</a></div>
            </body>
            </html>
        """.trimIndent()

        val result = ThreadHtmlParser.parseReaderPage(
            html = html,
            baseUrl = "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=1",
            expectedAuthorId = "100"
        )

        assertEquals("测试小说", result.title)
        assertEquals("100", result.authorId)
        assertEquals(3, result.maxPage)
        assertTrue(result.combinedHtml.contains("有权限正文"))
        assertFalse(result.combinedHtml.contains("普通回复"))
        assertFalse(result.combinedHtml.contains("locked-tip"))
    }

    @Test
    fun parseMangaPage_usesOriginalAttachmentUrl() {
        val html = """
            <html>
            <body>
              <h1 id="thread_subject">权限漫画</h1>
              <div id="post_1">
                <a href="forum.php?mod=viewthread&tid=1&authorid=100">只看该作者</a>
                <table><tbody><tr><td class="t_f" id="postmessage_1">
                  <img src="thumb.png"
                       zoomfile="data/attachment/forum/202606/original.png">
                  <img src="static/image/smiley/smile.gif">
                </td></tr></tbody></table>
              </div>
            </body>
            </html>
        """.trimIndent()

        val result = ThreadHtmlParser.parseMangaPage(
            html = html,
            baseUrl = "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=1"
        )

        assertEquals(
            listOf("https://bbs.yamibo.com/data/attachment/forum/202606/original.png"),
            result.imageUrls
        )
        assertEquals("权限漫画", result.title)
        assertTrue(result.compatibleHtml.contains("class=\"message\""))
    }

    @Test
    fun parseMangaPage_reportsPermissionPageWithoutCrashing() {
        val html = """
            <html>
            <body>
              <div id="messagetext" class="showmessage">
                抱歉，您的阅读权限不足，无法查看本帖。
              </div>
            </body>
            </html>
        """.trimIndent()

        val result = ThreadHtmlParser.parseMangaPage(
            html = html,
            baseUrl = "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=20"
        )

        assertTrue(result.imageUrls.isEmpty())
        assertTrue(result.compatibleHtml.isEmpty())
        assertTrue(result.accessDeniedReason?.contains("权限不足") == true)
    }
}
