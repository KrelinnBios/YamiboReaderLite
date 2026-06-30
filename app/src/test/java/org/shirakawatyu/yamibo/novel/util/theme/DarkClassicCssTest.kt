package org.shirakawatyu.yamibo.novel.util.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class DarkClassicCssTest {
    private val css = DARK_MODE_CSS_RULES_CLASSIC.joinToString("\n")

    @Test
    fun keepsDesktopDarkroomRules() {
        assertTrue(css.contains("#darkroomtable"))
        assertTrue(css.contains("#darkroomtable a"))
    }

    @Test
    fun keepsDesktopPaginationJumpRules() {
        assertTrue(css.contains(".pg label"))
        assertTrue(css.contains("input.px"))
    }

    @Test
    fun keepsDesktopRanklistSelectedRules() {
        assertTrue(css.contains(".pg_ranklist .tbn li.a"))
    }
}