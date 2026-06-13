package org.shirakawatyu.yamibo.novel.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YamiboPostLinkUtilTest {
    @Test
    fun extractsViewThreadLinkRegardlessOfQueryOrder() {
        assertEquals(
            "https://bbs.yamibo.com/forum.php?tid=572320&mod=viewthread&mobile=2",
            YamiboPostLinkUtil.extractPostUrl(
                "看看 https://m.yamibo.com/forum.php?tid=572320&mod=viewthread&highlight=test 。"
            )
        )
    }

    @Test
    fun normalizesSeoThreadLinkAndKeepsPage() {
        assertEquals(
            "https://bbs.yamibo.com/thread-572320-3-1.html?mobile=2",
            YamiboPostLinkUtil.normalizePostUrl(
                "yamibo.com/thread-572320-3-1.html"
            )
        )
    }

    @Test
    fun acceptsDirectFloorRedirectLink() {
        assertEquals(
            "https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&ptid=572320&pid=41559541&mobile=2",
            YamiboPostLinkUtil.extractPostUrl(
                "https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&ptid=572320&pid=41559541"
            )
        )
    }

    @Test
    fun rejectsNonPostAndImageLinks() {
        assertNull(YamiboPostLinkUtil.extractPostUrl("https://bbs.yamibo.com/forum.php"))
        assertNull(
            YamiboPostLinkUtil.extractPostUrl(
                "https://bbs.yamibo.com/data/attachment/forum/example.jpg"
            )
        )
    }
}
