package org.shirakawatyu.yamibo.novel.util.forum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForumBlacklistRemoteClientTest {
    @Test
    fun parsesDiscuzBlacklistPage() {
        val html = """
            <html>
            <script>var discuz_uid = '615797';</script>
            <form name="blackform">
              <input type="hidden" name="formhash" value="4328c6d7">
            </form>
            <div id="friend_ul">
              <ul class="buddy">
                <li id="friend_489445_li">
                  <h4><a href="https://bbs.yamibo.com/space-uid-489445.html">425</a></h4>
                </li>
              </ul>
            </div>
            </html>
        """.trimIndent()

        val snapshot = ForumBlacklistRemoteClient.parseSnapshot(html)

        assertEquals("4328c6d7", snapshot?.formHash)
        assertEquals("615797", snapshot?.currentUid)
        assertEquals(
            listOf(
                ForumBlockedItem(
                    type = ForumBlockedItem.TYPE_USER,
                    id = "489445",
                    title = "425",
                    authorUid = "489445",
                    authorName = "425"
                )
            ),
            snapshot?.users
        )
    }

    @Test
    fun rejectsLoginOrUnrelatedPage() {
        assertNull(ForumBlacklistRemoteClient.parseSnapshot("<html><form name=\"loginform\"></form></html>"))
    }
}
