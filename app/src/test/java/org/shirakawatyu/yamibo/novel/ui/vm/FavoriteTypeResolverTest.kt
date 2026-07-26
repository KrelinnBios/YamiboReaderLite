package org.shirakawatyu.yamibo.novel.ui.vm

import org.junit.Assert.assertEquals
import org.junit.Test
import org.shirakawatyu.yamibo.novel.bean.Favorite

class FavoriteTypeResolverTest {
    @Test
    fun legacyOtherWithoutSourceFidNeedsReprobe() {
        val favorite = Favorite(
            title = "旧版误判漫画",
            url = "forum.php?mod=viewthread&tid=1",
            type = 3,
            sourceFid = null
        )

        assertEquals(0, FavoriteTypeResolver.reliableType(favorite))
    }

    @Test
    fun manuallySelectedOtherRemainsReliable() {
        val favorite = Favorite(
            title = "手动设为其他",
            url = "forum.php?mod=viewthread&tid=2",
            type = 3,
            sourceFid = FavoriteTypeResolver.MANUAL_SOURCE_FID
        )

        assertEquals(3, FavoriteTypeResolver.reliableType(favorite))
    }

    @Test
    fun mangaFidOverridesLegacyType() {
        val favorite = Favorite(
            title = "漫画",
            url = "forum.php?mod=viewthread&tid=3",
            type = 3,
            sourceFid = "30"
        )

        assertEquals(2, FavoriteTypeResolver.reliableType(favorite))
    }
}
