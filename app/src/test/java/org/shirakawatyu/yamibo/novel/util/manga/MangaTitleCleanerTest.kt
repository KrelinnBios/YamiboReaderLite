package org.shirakawatyu.yamibo.novel.util.manga

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun isTruncatedCleanBookName_detectsMissingCollectionSuffix() {
        assertTrue(
            MangaTitleCleaner.isTruncatedCleanBookName(
                "沉溺于罪恶的犯罪百合",
                "沉溺于罪恶的犯罪百合短篇集"
            )
        )
    }

    @Test
    fun isTruncatedCleanBookName_ignoresUnrelatedOrManualNames() {
        assertFalse(MangaTitleCleaner.isTruncatedCleanBookName("某漫画", "某漫画"))
        assertFalse(MangaTitleCleaner.isTruncatedCleanBookName("某漫画(完结)", "某漫画"))
        assertFalse(MangaTitleCleaner.isTruncatedCleanBookName("某漫画", "另一部漫画短篇集"))
    }

    @Test
    fun isIndividualRelease_matchesPersonalOrAdHocCredits() {
        assertTrue(
            MangaTitleCleaner.isIndividualRelease(
                "[个人汉化](犬兎ねこ)となりの席の地味な奴(邻座那个朴素的女孩)第53话"
            )
        )
        assertTrue(
            MangaTitleCleaner.isIndividualRelease(
                "【个人翻译】《上伊那牡丹，酒醉身姿似百合花般》动漫 —— 《谈话室笔记9-12》"
            )
        )
        assertTrue(MangaTitleCleaner.isIndividualRelease("【個人漢化】某漫画"))
        assertTrue(MangaTitleCleaner.isIndividualRelease("【自翻】某漫画"))
        assertTrue(MangaTitleCleaner.isIndividualRelease("【代发】某漫画"))
        assertTrue(MangaTitleCleaner.isIndividualRelease("【渣翻】某漫画"))
        assertTrue(MangaTitleCleaner.isIndividualRelease("【中字渣翻】某漫画"))
        assertTrue(MangaTitleCleaner.isIndividualRelease("【个人渣翻】某漫画"))
        assertTrue(MangaTitleCleaner.isIndividualRelease("【个人渣改翻】某漫画"))
        assertTrue(MangaTitleCleaner.isIndividualRelease("【转载】某漫画"))
        assertTrue(MangaTitleCleaner.isIndividualRelease("【授权转载】某漫画"))
        assertTrue(MangaTitleCleaner.isIndividualRelease("【授權轉載】某漫画"))
    }

    @Test
    fun isIndividualRelease_doesNotMatchNamedGroups() {
        assertFalse(
            MangaTitleCleaner.isIndividualRelease(
                "[短篇漫畫]【提灯喵汉化组】沉溺于罪恶的犯罪百合短篇集[原作：るぅ1mm]天使的抉择"
            )
        )
    }
}
