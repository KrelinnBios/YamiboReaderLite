package org.shirakawatyu.yamibo.novel.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun forcesMobileTemplateBeforeDesktopIsSelected() {
        // 手机版会话中，无版本参数的帖子与楼层跳转必须补 mobile=2
        assertEquals(
            "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=573162&mobile=2",
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=573162",
                desktopSession = false
            )
        )
        assertEquals(
            "https://bbs.yamibo.com/thread-520058-1-1.html?mobile=2",
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/thread-520058-1-1.html",
                desktopSession = false
            )
        )
        assertEquals(
            "https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&ptid=572320&pid=41559541&mobile=2",
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&ptid=572320&pid=41559541",
                desktopSession = false
            )
        )
        // 版块下一页同样补参数，m 子域名顺便归一到主论坛域名
        assertEquals(
            "https://bbs.yamibo.com/forum-30-2.html?mobile=2",
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/forum-30-2.html",
                desktopSession = false
            )
        )
        assertEquals(
            "https://bbs.yamibo.com/forum.php?mod=forumdisplay&fid=30&page=2&mobile=2",
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/forum.php?mod=forumdisplay&fid=30&page=2",
                desktopSession = false
            )
        )
        assertEquals(
            "https://bbs.yamibo.com/thread-520058-2-1.html?mobile=2",
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://m.yamibo.com/thread-520058-2-1.html",
                desktopSession = false
            )
        )
    }

    @Test
    fun detectsDesktopSessionFromMobileCookie() {
        // 点过论坛底部「电脑版」后 Discuz 写入带站点前缀的 mobile=no cookie
        assertTrue(
            YamiboPostLinkUtil.isDesktopSessionCookie(
                "EeqY_2132_saltkey=abc; EeqY_2132_mobile=no; EeqY_2132_sid=xyz"
            )
        )
        assertTrue(YamiboPostLinkUtil.isDesktopSessionCookie("mobile=no"))
        // 手机版会话（mobile=2 或无 mobile cookie）不是电脑版
        assertFalse(YamiboPostLinkUtil.isDesktopSessionCookie("EeqY_2132_mobile=2"))
        assertFalse(YamiboPostLinkUtil.isDesktopSessionCookie("EeqY_2132_saltkey=abc"))
        assertFalse(YamiboPostLinkUtil.isDesktopSessionCookie(null))
        // 名字只是碰巧含 mobile 的 cookie 不误判
        assertFalse(YamiboPostLinkUtil.isDesktopSessionCookie("automobile=no"))
    }

    @Test
    fun recognizesOnlyExplicitForumTemplateSwitches() {
        assertEquals(
            true,
            YamiboPostLinkUtil.explicitDesktopTemplateSelection(
                "https://bbs.yamibo.com/forum.php?mobile=no"
            )
        )
        assertEquals(
            false,
            YamiboPostLinkUtil.explicitDesktopTemplateSelection(
                "https://bbs.yamibo.com/forum.php?mobile=2"
            )
        )
        assertEquals(
            false,
            YamiboPostLinkUtil.explicitDesktopTemplateSelection(
                "https://bbs.yamibo.com/?mobile=yes"
            )
        )
        assertEquals(
            false,
            YamiboPostLinkUtil.explicitDesktopTemplateSelection(
                "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=573162&mobile=2"
            )
        )
        // 分页、标签页携带 mobile 参数不代表用户切换了全局模板
        assertNull(
            YamiboPostLinkUtil.explicitDesktopTemplateSelection(
                "https://bbs.yamibo.com/forum.php?mod=forumdisplay&fid=30&page=2&mobile=no"
            )
        )
        assertNull(
            YamiboPostLinkUtil.explicitDesktopTemplateSelection(
                "https://bbs.yamibo.com/misc.php?mod=tag&id=20563&mobile=no"
            )
        )
    }

    @Test
    fun forceMobileRespectsExplicitTemplateAndDesktopSession() {
        // 链接已经明确指定模板时视为用户切换，不再改写
        assertNull(
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=573162&mobile=2",
                desktopSession = false
            )
        )
        assertNull(
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/forum.php?mobile=no",
                desktopSession = false
            )
        )
        assertEquals(
            "https://bbs.yamibo.com/forum.php?mod=forumdisplay&fid=30&page=2&mobile=2",
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/forum.php?mod=forumdisplay&fid=30&page=2&mobile=no",
                desktopSession = false
            )
        )
        // 用户已经点过电脑版后，无参数的版块和帖子跳转也继续保持电脑版
        assertNull(
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/forum-30-2.html",
                desktopSession = true
            )
        )
        assertNull(
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/thread-520058-2-1.html",
                desktopSession = true
            )
        )
        // 电脑版专属标签页、明确进入的电脑版空间页仍保留电脑版
        assertNull(
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/misc.php?mod=tag&id=20563&mobile=no",
                desktopSession = false
            )
        )
        assertNull(
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/home.php?mod=space&uid=399468&mobile=no",
                desktopSession = false
            )
        )
        // 非常规论坛 HTML 页面、附件和站外链接不改写
        assertNull(
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/search.php?mod=forum&mobile=no",
                desktopSession = false
            )
        )
        assertNull(
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://bbs.yamibo.com/data/attachment/forum/example.jpg",
                desktopSession = false
            )
        )
        assertNull(
            YamiboPostLinkUtil.forceMobileForumPageUrl(
                "https://example.com/forum.php?mod=viewthread&tid=1",
                desktopSession = false
            )
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
