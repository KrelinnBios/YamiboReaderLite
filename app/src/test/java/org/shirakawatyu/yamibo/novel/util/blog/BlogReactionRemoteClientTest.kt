package org.shirakawatyu.yamibo.novel.util.blog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlogReactionRemoteClientTest {
    @Test
    fun parsesDesktopBlogReactionOptionsAndUsers() {
        val html = """
            <div id="click_div">
              <table class="atd"><tr>
                <td><a id="click_blogid_117517_1"
                  href="/home.php?mod=spacecp&amp;ac=click&amp;op=add&amp;clickid=1&amp;idtype=blogid&amp;id=117517&amp;hash=abc123">
                  <img src="/static/image/click/luguo.gif"><br>路过</a></td>
                <td><a id="click_blogid_117517_4"
                  href="/home.php?mod=spacecp&amp;ac=click&amp;op=add&amp;clickid=4&amp;idtype=blogid&amp;id=117517&amp;hash=abc123">
                  <div class="atdc"><div class="ac1"><em>10</em></div></div>
                  <img src="/static/image/click/xianhua.gif"><br>鲜花</a></td>
              </tr></table>
              <h3>刚表态过的朋友 (<a>10 人</a>)</h3>
              <ul id="trace_ul">
                <li>
                  <div class="avt"><a href="/space-uid-656245.html" title="鲜花">
                    <img src="/uc_server/data/avatar/noavatar.svg"></a></div>
                  <p><a href="/space-uid-656245.html" title="abcdefg39">abcdefg39</a></p>
                </li>
              </ul>
            </div>
        """.trimIndent()

        val snapshot = BlogReactionRemoteClient.parseSnapshot(
            html,
            "https://bbs.yamibo.com/home.php?mod=space&uid=615797&do=blog&id=117517"
        )!!

        assertEquals(2, snapshot.options.size)
        assertEquals("1", snapshot.options[0].clickId)
        assertEquals("路过", snapshot.options[0].label)
        assertEquals("https://bbs.yamibo.com/static/image/click/luguo.gif", snapshot.options[0].iconUrl)
        assertEquals(0, snapshot.options[0].count)
        assertEquals("4", snapshot.options[1].clickId)
        assertEquals("鲜花", snapshot.options[1].label)
        assertEquals(10, snapshot.options[1].count)
        assertEquals("ac1", snapshot.options[1].barClass)
        assertTrue(snapshot.options[1].actionUrl.contains("hash=abc123"))
        assertEquals(10, snapshot.totalCount)
        assertEquals(1, snapshot.users.size)
        assertEquals("656245", snapshot.users[0].uid)
        assertEquals("abcdefg39", snapshot.users[0].username)
        assertEquals("鲜花", snapshot.users[0].reaction)
    }

    @Test
    fun ignoresMobileBlogWithoutDesktopReactionArea() {
        val mobileHtml = """
            <body id="home" class="pg_space">
              <div class="viewthread"><div class="threadlist_foot"></div></div>
            </body>
        """.trimIndent()

        assertNull(
            BlogReactionRemoteClient.parseSnapshot(
                mobileHtml,
                "https://bbs.yamibo.com/home.php?mod=space&uid=615797&do=blog&id=117517&mobile=2"
            )
        )
    }

    @Test
    fun extractsDiscuzAjaxDialogMessage() {
        val response = """<script>showDialog('表态成功', 'right');</script>"""

        assertEquals("表态成功", BlogReactionRemoteClient.extractResponseMessage(response))
    }
}
