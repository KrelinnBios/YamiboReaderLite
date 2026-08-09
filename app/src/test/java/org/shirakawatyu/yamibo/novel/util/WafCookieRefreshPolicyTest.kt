package org.shirakawatyu.yamibo.novel.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WafCookieRefreshPolicyTest {
    @Test
    fun successfulChallenge_refreshesBeforeThirtyMinuteExpiry() {
        assertEquals(25 * 60 * 1000L, WafCookieRefreshPolicy.nextDelayMs(succeeded = true))
    }

    @Test
    fun failedChallenge_retriesSooner() {
        assertEquals(2 * 60 * 1000L, WafCookieRefreshPolicy.nextDelayMs(succeeded = false))
    }

    @Test
    fun recentSuccessfulChallenge_isSharedByConcurrentRetries() {
        val lastSuccess = 100_000L

        assertEquals(true, WafCookieRefreshPolicy.hasRecentSuccess(lastSuccess, 114_999L))
        assertEquals(false, WafCookieRefreshPolicy.hasRecentSuccess(lastSuccess, 115_001L))
    }
}
