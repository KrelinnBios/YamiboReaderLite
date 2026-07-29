package org.shirakawatyu.yamibo.novel.util.blog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileBlogJsScriptsTest {
    @Test
    fun enhancementIsGuardedToMobileBlogPages() {
        val script = MobileBlogJsScripts.ENHANCEMENTS_JS

        assertTrue(script.contains("body.id !== 'home'"))
        assertTrue(script.contains("body.classList.contains('pg_space')"))
        assertTrue(script.contains("searchParams.get('do')"))
        assertTrue(script.contains("!== 'blog'"))
        assertTrue(script.contains("searchParams.get('id')"))
    }

    @Test
    fun invitationUsesDirectMobileNavigation() {
        val script = MobileBlogJsScripts.ENHANCEMENTS_JS

        assertTrue(script.contains("closest('#a_invite[href]')"))
        assertTrue(script.contains("event.stopImmediatePropagation()"))
        assertTrue(script.contains("inviteUrl.searchParams.set('mobile', '2')"))
        assertTrue(script.contains("location.assign(inviteUrl.href)"))
    }

    @Test
    fun reactionAreaLoadsAndSubmitsThroughNativeBridge() {
        val script = MobileBlogJsScripts.ENHANCEMENTS_JS

        assertTrue(script.contains("window.AndroidBlogReaction.load(ownerUid, blogId, activeRequest)"))
        assertTrue(script.contains("window.AndroidBlogReaction.react(ownerUid, blogId, clickId, activeRequest)"))
        assertTrue(script.contains("window.__yamiboBlogReactionReceive"))
        assertTrue(script.contains("document.querySelector('.threadlist_foot')"))
        assertTrue(script.contains("foot.parentNode.insertBefore(section, foot)"))
    }

    @Test
    fun reactionAreaUsesFixedTextBarsWithoutImagesOrUserList() {
        val script = MobileBlogJsScripts.ENHANCEMENTS_JS

        assertTrue(script.contains("给帖主表态"))
        assertTrue(script.contains("{ clickId: '1', label: '路过' }"))
        assertTrue(script.contains("{ clickId: '5', label: '鸡蛋' }"))
        assertTrue(script.contains("renderOptions();"))
        assertFalse(script.contains("ybr-icon"))
        assertFalse(script.contains("ybr-users"))
        assertFalse(script.contains("ybr-avatar"))
        assertFalse(script.contains("avatarUrl"))
    }
}
