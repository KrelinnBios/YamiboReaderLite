package org.shirakawatyu.yamibo.novel.util.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderReturnBridgeTest {
    @Test
    fun replacesDesktopMobileParameter() {
        assertEquals(
            "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=123&mobile=2#post",
            ReaderReturnBridge.forceMobileTemplate(
                "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=123&mobile=no#post"
            )
        )
    }

    @Test
    fun appendsMobileParameterWhenMissing() {
        assertEquals(
            "https://bbs.yamibo.com/thread-123-1-1.html?mobile=2",
            ReaderReturnBridge.forceMobileTemplate(
                "https://bbs.yamibo.com/thread-123-1-1.html"
            )
        )
    }
}
