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

    @Test
    fun rewritesTagPageLinkToDesktopTemplate() {
        // 标签页是电脑版专属页，手机版会话下必须强制 mobile=no
        assertEquals(
            "https://bbs.yamibo.com/misc.php?mod=tag&id=20563&mobile=no",
            YamiboPostLinkUtil.normalizePcOnlyPageUrl(
                "https://bbs.yamibo.com/misc.php?mod=tag&id=20563"
            )
        )
        // 已带 mobile=2 的也要改成 mobile=no
        assertEquals(
            "https://bbs.yamibo.com/misc.php?mod=tag&id=20563&mobile=no",
            YamiboPostLinkUtil.normalizePcOnlyPageUrl(
                "https://bbs.yamibo.com/misc.php?mod=tag&id=20563&mobile=2"
            )
        )
    }

    @Test
    fun forcesMobileTemplateOnBarePostLinks() {
        // mobile cookie 被污染成 no 后，不带参数的帖子跳转会渲染电脑版，必须补 mobile=2
        assertEquals(
            "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=573162&mobile=2",
            YamiboPostLinkUtil.forceMobilePostUrl(
                "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=573162"
            )
        )
        assertEquals(
            "https://bbs.yamibo.com/thread-520058-1-1.html?mobile=2",
            YamiboPostLinkUtil.forceMobilePostUrl(
                "https://bbs.yamibo.com/thread-520058-1-1.html"
            )
        )
        // 我的回复的 findpost 跳转链接
        assertEquals(
            "https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&ptid=572320&pid=41559541&mobile=2",
            YamiboPostLinkUtil.forceMobilePostUrl(
                "https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&ptid=572320&pid=41559541"
            )
        )
    }

    @Test
    fun forceMobileSkipsAlreadyTaggedAndNonPostLinks() {
        // 已带 mobile 参数（无论 2 还是 no）不再改写，避免 loadUrl 循环
        assertNull(
            YamiboPostLinkUtil.forceMobilePostUrl(
                "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=573162&mobile=2"
            )
        )
        // 非帖子链接（版块列表、标签页、空间页）不改写
        assertNull(
            YamiboPostLinkUtil.forceMobilePostUrl("https://bbs.yamibo.com/forum-30-1.html")
        )
        assertNull(
            YamiboPostLinkUtil.forceMobilePostUrl(
                "https://bbs.yamibo.com/misc.php?mod=tag&id=20563&mobile=no"
            )
        )
        assertNull(
            YamiboPostLinkUtil.forceMobilePostUrl("https://bbs.yamibo.com/space-uid-399468.html")
        )
        assertNull(
            YamiboPostLinkUtil.forceMobilePostUrl("https://example.com/forum.php?mod=viewthread&tid=1")
        )
    }

    @Test
    fun tagPageRewriteSkipsAlreadyDesktopAndNonTagLinks() {
        // 已经是 mobile=no 时返回 null，避免 loadUrl 重写循环
        assertNull(
            YamiboPostLinkUtil.normalizePcOnlyPageUrl(
                "https://bbs.yamibo.com/misc.php?mod=tag&id=20563&mobile=no"
            )
        )
        // 非标签页 misc 链接与普通帖子链接不重写
        assertNull(
            YamiboPostLinkUtil.normalizePcOnlyPageUrl(
                "https://bbs.yamibo.com/misc.php?mod=seccode"
            )
        )
        assertNull(
            YamiboPostLinkUtil.normalizePcOnlyPageUrl(
                "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=573162"
            )
        )
        // 非百合会域名不重写
        assertNull(
            YamiboPostLinkUtil.normalizePcOnlyPageUrl(
                "https://example.com/misc.php?mod=tag&id=1"
            )
        )
    }
}
