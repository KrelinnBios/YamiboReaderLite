package org.shirakawatyu.yamibo.novel.util.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.shirakawatyu.yamibo.novel.util.PageJsScripts

class MemberSpaceGuardTest {
    @Test
    fun memberSpaceUrlsAreExcludedFromThemeInjection() {
        assertTrue(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/space-uid-615797.html"))
        assertTrue(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/blog-615797-117517.html"))
        assertTrue(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/home.php?mod=blog&id=117517"))
        assertTrue(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/home.php?mod=space&uid=615797&do=blog&id=117517"))
        assertTrue(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/home.php?mod=space&username=test"))
    }

    @Test
    fun functionalAndMobileCenterPagesStillReceiveTheme() {
        assertFalse(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/home.php"))
        assertFalse(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/home.php?mod=space&do=notice"))
        assertFalse(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/home.php?mod=spacecp&ac=profile"))
        assertFalse(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/home.php?mod=blog&mobile=2"))
        assertFalse(MemberSpaceGuard.isMemberSpaceUrl("https://bbs.yamibo.com/home.php?mod=space&uid=615797&mycenter=1"))
    }

    @Test
    fun runtimeJsUsesTheSharedUrlRuleSource() {
        assertTrue(
            PageJsScripts.getDarkModeSetJs(enable = true)
                .contains(MemberSpaceGuard.jsExpression())
        )
    }

    @Test
    fun htmlFallbackRecognizesBodySpaceRegardlessOfAttributeOrderOrQuotes() {
        assertTrue(MemberSpaceGuard.isMemberSpaceHtml("<body id=\"space\">"))
        assertTrue(MemberSpaceGuard.isMemberSpaceHtml("<body class='custom' id='space' data-x='1'>"))
        assertFalse(MemberSpaceGuard.isMemberSpaceHtml("<body id=\"nv_home\" class=\"pg_space\">"))
    }
}
