package org.shirakawatyu.yamibo.novel.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageJsScriptsTest {
    @Test
    fun forumChromeKeepsPrivateMessageComposerVisible() {
        val css = PageJsScripts.getForumChromeHideCss()

        assertTrue(css.contains(".foot.flex-box:not(.foot_reply):not(.msg_post)"))
        assertTrue(css.contains("#pmform .foot_height { display: block !important; }"))
        assertTrue(css.contains("#pmform .foot.msg_post { display: flex !important; }"))
        assertFalse(css.contains(".foot.flex-box:not(.foot_reply) {"))
    }

    @Test
    fun bbsNavigationInterceptsThreadLinksBeforeLegacyListHandler() {
        val script = PageJsScripts.BBS_COMMIT_BOOTSTRAP_JS
        val nativeNavigationIndex = script.indexOf("BBS_THREAD_NAVIGATION_JS")
        val legacyListHandlerIndex = script.indexOf("THREAD_LIST_CLICK_FIX_JS")

        assertTrue(nativeNavigationIndex >= 0)
        assertTrue(legacyListHandlerIndex > nativeNavigationIndex)
        assertTrue(script.contains("document.getElementById('toptb')"))
        assertTrue(script.contains("navigateToPostWithTemplate(link.href, desktopTemplate)"))
        assertTrue(script.contains("navigateToPost(link.href)"))
        assertTrue(script.contains("mod === 'redirect' || goto === 'findpost'"))
        assertTrue(script.contains("/^[1-9]\\d*${'$'}/.test(pid)"))
        assertTrue(script.contains("event.stopImmediatePropagation()"))
    }

    @Test
    fun threadNavigationIsInjectedIntoBbsAndMineWebViewsOnly() {
        val marker = "__yamiboBbsThreadNavigationV2"

        assertTrue(PageJsScripts.BBS_COMMIT_BOOTSTRAP_JS.contains(marker))
        assertTrue(PageJsScripts.BBS_MANGA_REINJECT_JS.contains(marker))
        assertTrue(PageJsScripts.MINE_COMMIT_BOOTSTRAP_JS.contains(marker))
        assertTrue(PageJsScripts.MINE_MANGA_REINJECT_JS.contains(marker))
        assertFalse(PageJsScripts.OTHER_COMMIT_BOOTSTRAP_JS.contains(marker))
        assertFalse(PageJsScripts.MANGA_BOOTSTRAP_JS.contains(marker))
    }

    @Test
    fun forumBlockerCoversAuxiliaryUserContent() {
        val script = PageJsScripts.getForumBlockerJs(
            enabled = true,
            itemsJson = "[]",
            isDark = false
        )

        assertTrue(script.contains("syncAuxiliaryUserContent(map)"))
        assertTrue(script.contains("div.pstl.xs1.cl a.xi2.xw1"))
        assertTrue(script.contains("[id^=\"comment_\"] > [id^=\"commentdetail_\"] .authi"))
        assertTrue(script.contains("container: '[id^=\"commentdetail_\"]'"))
        assertTrue(script.contains("tr[id] td > a[target=_blank]"))
        assertTrue(script.contains("[id^=\"ratelog_\"] .post_box > li a[href*=\"mod=space\"][href*=\"uid=\"]"))
        assertTrue(script.contains("#floatlayout_topicadmin #return_rate ~ .post_box > li"))
        assertTrue(script.contains("container: 'li'"))
        assertTrue(script.contains("div.quote > blockquote > font:first-child"))
        assertTrue(script.contains("quoteHeader.querySelector('a[target=_blank]') || quoteHeader"))
        assertTrue(script.contains("syncBlogUserContent(map)"))
        assertFalse(script.contains("if (/home\\.php/i.test(location.href))"))
        assertTrue(script.contains("#comment_ul > dl[id^=\"comment_\"]"))
        assertTrue(script.contains(".doing_list_box.threadlist > ul > li[id^=\"comment_\"]"))
        assertTrue(script.contains("a[id^=\"author_\"][href*=\"space-uid-\"]"))
        assertTrue(script.contains("a[id^=\"author_\"][href*=\"mod=space\"][href*=\"uid=\"]"))
        assertTrue(script.contains("hideAuxiliaryContent(comment, commentUser, '该用户的评论')"))
        assertTrue(script.contains("#ct .yamibo-blocked-message a.yamibo-unblock-action"))
        assertTrue(script.contains("#feed_div dl.bbda.cl"))
        assertTrue(script.contains("#feed_div > ul.el > li[id^=\"feed_\"]"))
        assertTrue(script.contains("hideAuxiliaryContent(group, groupUser, '该用户的 BLOG ')"))
        assertTrue(script.contains("td.by > cite > a[c]"))
        assertTrue(script.contains("document.querySelectorAll('p > em')"))
        assertTrue(script.contains("getBlockedUser(map, authorUid, authorName)"))
    }

    @Test
    fun ratingReturnPreservesThreadPageAndRatedPost() {
        val script = PageJsScripts.PRESERVE_RATE_POSITION_JS

        assertTrue(script.contains("yamibo:rate-context:v1"))
        assertTrue(script.contains("yamibo:pending-rate-return:v1"))
        assertTrue(script.contains("form.id === 'rateform'"))
        assertTrue(script.contains("input[name=\"referer\"]"))
        assertTrue(script.contains("window.succeedhandle_rate"))
        assertTrue(script.contains("url.hash = 'pid'"))
        assertTrue(script.contains("window.location.replace(target)"))
        assertTrue(script.contains("var result = original.apply(this, args)"))
        assertTrue(script.contains("scrollToPost(pending.pid, function()"))
        assertTrue(script.contains("document.getElementById('pid' + normalizedPid)"))
        assertTrue(script.contains("document.getElementById('post_' + normalizedPid)"))
        assertTrue(script.contains("document.getElementById('postmessage_' + normalizedPid)"))
        assertTrue(script.contains("post.scrollIntoView"))
    }

    @Test
    fun ratingReturnIsInjectedIntoEveryForumCapableWebView() {
        val marker = "__yamiboPreserveRatePositionV1"

        assertTrue(PageJsScripts.BBS_COMMIT_BOOTSTRAP_JS.contains(marker))
        assertTrue(PageJsScripts.BBS_MANGA_REINJECT_JS.contains(marker))
        assertTrue(PageJsScripts.MINE_COMMIT_BOOTSTRAP_JS.contains(marker))
        assertTrue(PageJsScripts.MINE_MANGA_REINJECT_JS.contains(marker))
        assertTrue(PageJsScripts.OTHER_COMMIT_BOOTSTRAP_JS.contains(marker))
        assertTrue(PageJsScripts.MANGA_BOOTSTRAP_JS.contains(marker))
    }
}
