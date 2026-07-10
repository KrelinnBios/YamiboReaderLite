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
}
