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

    @Test
    fun mergeCookieHeaders_prefersFreshWafCookieOverPersistedSnapshot() {
        val merged = YamiboSession.mergeCookieHeaders(
            listOf(
                "waf_token=fresh; EeqY_2132_auth=secret",
                "waf_token=expired; EeqY_2132_sid=abc"
            )
        )

        assertEquals(
            "waf_token=fresh; EeqY_2132_auth=secret; EeqY_2132_sid=abc",
            merged
        )
    }

    @Test
    fun desktopCookie_replacesPrefixedMobileCookieWithoutChangingLogin() {
        assertEquals(
            "EeqY_2132_auth=secret; EeqY_2132_mobile=no; EeqY_2132_sid=abc",
            YamiboSession.desktopCookie(
                "EeqY_2132_auth=secret; EeqY_2132_mobile=2; EeqY_2132_sid=abc"
            )
        )
    }

    @Test
    fun desktopCookie_addsMatchingPrefixedMobileCookieWhenMissing() {
        assertEquals(
            "EeqY_2132_auth=secret; EeqY_2132_sid=abc; EeqY_2132_mobile=no",
            YamiboSession.desktopCookie("EeqY_2132_auth=secret; EeqY_2132_sid=abc")
        )
    }
}
