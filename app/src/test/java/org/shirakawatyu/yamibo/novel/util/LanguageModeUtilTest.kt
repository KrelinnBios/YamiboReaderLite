package org.shirakawatyu.yamibo.novel.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageModeUtilTest {
    @Test
    fun normalizesKnownLanguageModes() {
        assertEquals(LanguageModeUtil.SIMPLIFIED, LanguageModeUtil.normalize("zh-hans"))
        assertEquals(LanguageModeUtil.TRADITIONAL, LanguageModeUtil.normalize("zh-tw"))
        assertEquals(LanguageModeUtil.TRADITIONAL, LanguageModeUtil.normalize("繁體中文"))
        assertEquals(LanguageModeUtil.SIMPLIFIED, LanguageModeUtil.normalize("unknown"))
    }

    @Test
    fun buildsAcceptLanguageFromMode() {
        assertTrue(LanguageModeUtil.acceptLanguageHeader(LanguageModeUtil.SIMPLIFIED).startsWith("zh-CN"))
        assertTrue(LanguageModeUtil.acceptLanguageHeader(LanguageModeUtil.TRADITIONAL).startsWith("zh-TW"))
    }
}
