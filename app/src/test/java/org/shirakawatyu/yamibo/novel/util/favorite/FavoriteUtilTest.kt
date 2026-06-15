package org.shirakawatyu.yamibo.novel.util.favorite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FavoriteUtilTest {
    @Test
    fun jsonToHashMapKeepsPinAnchorUrl() {
        val json = """
            {
              "forum.php?mod=viewthread&tid=123": {
                "title": "测试小说",
                "url": "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=123&mobile=2",
                "lastPage": 5,
                "lastView": 2,
                "type": 1,
                "pinAnchorUrl": "forum.php?mod=viewthread&tid=88"
              }
            }
        """.trimIndent()

        val result = FavoriteUtil.jsonToHashMap(json)
        val favorite = result["forum.php?mod=viewthread&tid=123"]

        assertNotNull(favorite)
        assertEquals("forum.php?mod=viewthread&tid=88", favorite?.pinAnchorUrl)
        assertEquals("forum.php?mod=viewthread&tid=123", favorite?.url)
    }
}
