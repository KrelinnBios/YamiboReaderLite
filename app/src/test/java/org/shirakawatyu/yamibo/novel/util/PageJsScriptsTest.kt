package org.shirakawatyu.yamibo.novel.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageJsScriptsTest {
    @Test
    fun bbsNavigationInterceptsThreadLinksBeforeLegacyListHandler() {
        val script = PageJsScripts.BBS_COMMIT_BOOTSTRAP_JS
        val nativeNavigationIndex = script.indexOf("BBS_THREAD_NAVIGATION_JS")
        val legacyListHandlerIndex = script.indexOf("THREAD_LIST_CLICK_FIX_JS")

        assertTrue(nativeNavigationIndex >= 0)
        assertTrue(legacyListHandlerIndex > nativeNavigationIndex)
        assertTrue(script.contains("window.AndroidSearchNav.navigateToPost(link.href)"))
        assertTrue(script.contains("event.stopImmediatePropagation()"))
    }

    @Test
    fun bbsThreadNavigationIsNotInjectedIntoOtherWebViews() {
        val marker = "__yamiboBbsThreadNavigationV1"

        assertTrue(PageJsScripts.BBS_COMMIT_BOOTSTRAP_JS.contains(marker))
        assertTrue(PageJsScripts.BBS_MANGA_REINJECT_JS.contains(marker))
        assertFalse(PageJsScripts.MINE_COMMIT_BOOTSTRAP_JS.contains(marker))
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
        assertTrue(script.contains("tr[id] td > a[target=_blank]"))
        assertTrue(script.contains("div.quote > blockquote > font > a[target=_blank] > font"))
        assertTrue(script.contains("syncBlogUserContent(map)"))
        assertTrue(script.contains("#feed_div dl.bbda.cl"))
        assertTrue(script.contains("#feed_div > ul.el > li[id^=\"feed_\"]"))
        assertTrue(script.contains("hideAuxiliaryContent(group, groupUser, '该用户的 BLOG ')"))
        assertTrue(script.contains("td.by > cite > a[c]"))
        assertTrue(script.contains("document.querySelectorAll('p > em')"))
        assertTrue(script.contains("getBlockedUser(map, authorUid, authorName)"))
    }
}
