package org.shirakawatyu.yamibo.novel.util.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.shirakawatyu.yamibo.novel.util.PageJsScripts

class MemberSpaceGuardTest {
    @Test
    fun customDiySpacesAreExcludedFromThemeInjection() {
        // body#space 且用了 data/attachment 自定义背景图 → 自定义 DIY 空间，排除暗黑
        assertTrue(
            MemberSpaceGuard.isMemberSpaceHtml(
                "<body id=\"space\"><style>.a{background-image:url('https://bbs.yamibo.com/data/attachment/album/x.jpg')}</style></body>"
            )
        )
        assertTrue(
            MemberSpaceGuard.isMemberSpaceHtml(
                "<body id='space'><div style=\"background:url(https://bbs.yamibo.com/data/attachment/album/y.png)\"></div></body>"
            )
        )
    }

    @Test
    fun plainSpacesAndOtherPagesReceiveTheme() {
        // body#space 但只有 static 默认背景（无自定义）→ 照常暗黑
        assertFalse(
            MemberSpaceGuard.isMemberSpaceHtml(
                "<body id=\"space\"><style>.a{background-image:url(https://bbs.yamibo.com/static/image/feed/blog_b.png)}</style></body>"
            )
        )
        // body#space 但完全没有背景图 → 照常暗黑
        assertFalse(MemberSpaceGuard.isMemberSpaceHtml("<body id=\"space\"></body>"))
        // 非空间页 → 照常暗黑
        assertFalse(MemberSpaceGuard.isMemberSpaceHtml("<body id=\"nv_home\" class=\"pg_space\">"))
    }

    @Test
    fun runtimeJsUsesTheSharedRuleSource() {
        assertTrue(
            PageJsScripts.getDarkModeSetJs(enable = true)
                .contains(MemberSpaceGuard.jsExpression())
        )
    }
}
