package org.shirakawatyu.yamibo.novel.util.manga

import org.junit.Assert.assertEquals
import org.junit.Test

class MangaTitleCleanerTest {
    @Test
    fun getCleanBookName_keepsJiWhenPartOfCollectionSuffix() {
        assertEquals(
            "沉溺于罪恶的犯罪百合短篇集",
            MangaTitleCleaner.getCleanBookName(
                "[短篇漫畫]【提灯喵汉化组】沉溺于罪恶的犯罪百合短篇集[原作：るぅ1mm]天使的抉择"
            )
        )
    }

    @Test
    fun getDisplayChapterTitle_dropsOnlyBookNamePrefixNotCollectionSuffix() {
        val bookName = "沉溺于罪恶的犯罪百合短篇集"
        assertEquals(
            "天使的抉择",
            MangaTitleCleaner.getDisplayChapterTitle(
                "[短篇漫畫]【提灯喵汉化组】沉溺于罪恶的犯罪百合短篇集[原作：るぅ1mm]天使的抉择",
                bookName,
                "2"
            )
        )
        assertEquals(
            "崇拜的尽头",
            MangaTitleCleaner.getDisplayChapterTitle(
                "[短篇漫畫]【提灯喵汉化组】沉溺于罪恶的犯罪百合短篇集[原作：瀬尾みいのすけ]崇拜的尽头",
                bookName,
                "1"
            )
        )
    }

    @Test
    fun getCleanBookName_stillTruncatesGenuineChapterMarkers() {
        assertEquals(
            "某漫画",
            MangaTitleCleaner.getCleanBookName("【汉化组】某漫画 番外1")
        )
        assertEquals(
            "某漫画",
            MangaTitleCleaner.getCleanBookName("【汉化组】某漫画 第3话")
        )
    }
}
