package org.shirakawatyu.yamibo.novel.util

import org.junit.Assert.assertEquals
import org.junit.Test

class YamiboSessionTest {
    @Test
    fun mergeCookieHeaders_prefersFreshWebViewValuesAndKeepsMissingCookies() {
        val merged = YamiboSession.mergeCookieHeaders(
            listOf(
                "auth=fresh; salt=web",
                "auth=stale; sid=stored"
            )
        )

        assertEquals("auth=fresh; salt=web; sid=stored", merged)
    }
}
