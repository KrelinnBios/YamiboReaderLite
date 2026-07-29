package org.shirakawatyu.yamibo.novel.util.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderSpacingOptionsTest {
    @Test
    fun eachSettingHasSixOptions() {
        assertEquals(6, ReaderSpacingOptions.FONT_SIZES.size)
        assertEquals(6, ReaderSpacingOptions.LINE_HEIGHT_RATIOS.size)
        assertEquals(6, ReaderSpacingOptions.PAGE_PADDINGS.size)
    }

    @Test
    fun legacyValuesSnapToNearestNewOption() {
        assertEquals(18f, ReaderSpacingOptions.snapFontSize(14f))
        assertEquals(30f, ReaderSpacingOptions.snapFontSize(34f))
        assertEquals(43.2f, ReaderSpacingOptions.snapLineHeight(43f, 24f), 0.001f)
        assertEquals(8f, ReaderSpacingOptions.snapPadding(4f))
        assertEquals(28f, ReaderSpacingOptions.snapPadding(40f))
    }
}
