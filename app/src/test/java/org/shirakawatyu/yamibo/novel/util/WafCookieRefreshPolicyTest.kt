package org.shirakawatyu.yamibo.novel.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WafCookieRefreshPolicyTest {
    @Test
    fun challengeUrl_doesNotForceForumTemplate() {
        assertEquals("https://bbs.yamibo.com/forum.php", WafCookieRefreshPolicy.CHALLENGE_URL)
    }

    @Test
    fun recentSuccessfulChallenge_isSharedByConcurrentRetries() {
        val lastSuccess = 100_000L

        assertEquals(true, WafCookieRefreshPolicy.hasRecentSuccess(lastSuccess, 114_999L))
        assertEquals(false, WafCookieRefreshPolicy.hasRecentSuccess(lastSuccess, 115_001L))
    }

    @Test
    fun rejectedGet_triggersChallengeForBothKnownWafStatuses() {
        assertEquals(true, WafCookieRefreshPolicy.shouldRefreshForResponse(444, "GET"))
        assertEquals(true, WafCookieRefreshPolicy.shouldRefreshForResponse(405, "GET"))
    }

    @Test
    fun methodNotAllowedPost_isNotAutomaticallyReplayed() {
        assertEquals(false, WafCookieRefreshPolicy.shouldRefreshForResponse(405, "POST"))
    }
}
