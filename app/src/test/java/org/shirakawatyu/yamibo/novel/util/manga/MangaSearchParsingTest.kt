package org.shirakawatyu.yamibo.novel.util.manga

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.shirakawatyu.yamibo.novel.parser.MangaHtmlParser

class MangaSearchParsingTest {
    private val fullTitle = "【提灯喵汉化组】后续任凭想象[やナい]把我弄得乱七八糟吧！"

    @Test
    fun searchKeywordUsesContinuousDistinctiveTitleSegment() {
        assertEquals("把我弄得乱七八糟吧", MangaTitleCleaner.getSearchKeyword(fullTitle))
    }

    @Test
    fun matchingIgnoresBracketedAuthorInsertedInsideTitle() {
        assertTrue(
            MangaTitleCleaner.matchesSearchQuery(
                fullTitle,
                "后续任凭想象把我弄得乱七八糟吧"
            )
        )
    }

    @Test
    fun matchingSupportsMultipleFieldsAndCommonCharacterVariants() {
        val searchableText = "【犬山あむ】柊酱与富川酱 第10話 Kakukuroi汉化组 犬山あむ"
        assertTrue(
            MangaTitleCleaner.matchesSearchQuery(
                searchableText,
                "柊酱 富川酱 Kakukuroi"
            )
        )
        assertTrue(MangaTitleCleaner.matchesSearchQuery(searchableText, "第10话"))
    }

    @Test
    fun forumKeywordPrefersBookTitleOverChapterAndTranslationGroup() {
        assertEquals(
            "柊酱与富川酱",
            MangaTitleCleaner.getForumSearchKeyword(
                "柊酱与富川酱 第10话 Kakukuroi汉化组"
            )
        )
        assertEquals(
            "Kakukuroi汉化组",
            MangaTitleCleaner.getForumSearchKeyword("Kakukuroi汉化组")
        )
    }

    @Test
    fun directorySearchUsesBookNameInsteadOfCombinedFilters() {
        assertEquals(
            "DrunkenMyBoss",
            MangaTitleCleaner.getDirectoryForumSearchKeyword(
                cleanBookName = "DrunkenMyBoss",
                rawTitle = "[个人汉化][BBTan] DrunkenMyBoss 第一话",
                configuredKeywords = "BBTan 个人汉化"
            )
        )
    }

    @Test
    fun directoryCandidateKeepsAllBookMatchesAndSupportsAliases() {
        assertTrue(
            MangaTitleCleaner.matchesDirectoryCandidate(
                rawText = "[合作汉化][BBTan] DrunkenMyBoss 第四十七话+后记",
                cleanBookName = "DrunkenMyBoss",
                configuredKeywords = "个人汉化 BBTan"
            )
        )
        assertTrue(
            MangaTitleCleaner.matchesDirectoryCandidate(
                rawText = "[某汉化组] 酒醉上司 第2话",
                cleanBookName = "DrunkenMyBoss",
                configuredKeywords = "酒醉上司"
            )
        )
        assertFalse(
            MangaTitleCleaner.matchesDirectoryCandidate(
                rawText = "[BBTan] 完全不同的作品 第1话",
                cleanBookName = "DrunkenMyBoss",
                configuredKeywords = "酒醉上司"
            )
        )
    }

    @Test
    fun threadStyleFavoriteUrlExtractsTid() {
        assertEquals(
            "546273",
            MangaTitleCleaner.extractTidFromUrl(
                "https://bbs.yamibo.com/thread-546273-1-1.html"
            )
        )
    }

    @Test
    fun koreanTitleWithUpperPartSuffixKeepsBookNameAndChapterOrder() {
        val title =
            "[个人汉化][Willow（윌로우）]장마에서 살아남는 방법（从梅雨中活下来的方法）上篇"

        assertEquals(
            "장마에서 살아남는 방법（从梅雨中活下来的方法）",
            MangaTitleCleaner.getCleanBookName(title)
        )
        assertEquals(0.1f, MangaTitleCleaner.extractChapterNum(title))
    }

    @Test
    fun bareDashChapterNumberKeepsLeadingChapterDigit() {
        // 无"第"无"话"的纯 X-Y 格式（如"向笨蛋告白 2-1"）曾被规则4只截取横杠后半段，
        // 算出 1 而不是 2.01，导致同书多章节号倒退（2,1,2,1,2...）。
        assertEquals(2.01f, MangaTitleCleaner.extractChapterNum("向笨蛋告白 2-1"))
        assertEquals(2.02f, MangaTitleCleaner.extractChapterNum("向笨蛋告白2-2"))
        assertEquals(5.01f, MangaTitleCleaner.extractChapterNum("向笨蛋告白5-1"))
    }

    @Test
    fun combinedChapterNumberKeepsDisplayLabel() {
        val title =
            "[長篇連載] 【大友同好會】 [ドスコイ]特别な中途半端(特别的半吊子) 52+52.5"

        assertEquals("特别な中途半端(特别的半吊子)", MangaTitleCleaner.getCleanBookName(title))
        assertEquals("52+52.5", MangaTitleCleaner.extractChapterLabel(title))
        assertEquals(52f, MangaTitleCleaner.extractChapterNum(title))
        assertEquals(
            "52+52.5",
            MangaTitleCleaner.formatChapterDisplayNumber(
                title,
                MangaTitleCleaner.extractChapterNum(title),
                1
            )
        )
    }

    @Test
    fun chapterDisplayNumberMatchesDirectoryFormat() {
        // 纯小数编号（13.2）保留小数点，不转成"13-2"
        assertEquals(
            "13.2",
            MangaTitleCleaner.formatChapterDisplayNumber("魔法少女奈叶 EXCEEDS 13.2", 13.2f, 5)
        )
        // 分段标题（X-Y）原样展示为"12-2"
        assertEquals(
            "12-2",
            MangaTitleCleaner.formatChapterDisplayNumber("某漫画 12-2", 12.02f, 5)
        )
        // 整数编号
        assertEquals(
            "4",
            MangaTitleCleaner.formatChapterDisplayNumber("就像你一样 4", 4f, 9)
        )
        // 识别失败（chapterNum<=0）时用列表位置兜底，而非"Ex"
        assertEquals(
            "4",
            MangaTitleCleaner.formatChapterDisplayNumber("【提灯喵汉化组】就像你一样", 0f, 4)
        )
    }

    @Test
    fun administrativeThreadsAreExcluded() {
        assertTrue(MangaTitleCleaner.isAdministrativeThread("百合会新人须知/论坛规则"))
        assertFalse(MangaTitleCleaner.isAdministrativeThread(fullTitle))
    }

    @Test
    fun translationGroupOnlyAcceptsExplicitGroupFormats() {
        assertEquals(
            "提灯喵汉化组",
            MangaTitleCleaner.extractTranslationGroup("【提灯喵汉化组】作品名 第1话")
        )
        assertEquals(
            "Kakukuroi汉化组",
            MangaTitleCleaner.extractTranslationGroup(
                "【犬山あむ】柊酱与富川酱 第10话 Kakukuroi汉化组"
            )
        )
        assertEquals(
            "汉化工房九九组",
            MangaTitleCleaner.extractTranslationGroup(
                "【汉化工房九九组】[工藤える]江口家的纯情魅魔 2卷彩页"
            )
        )
        assertEquals("", MangaTitleCleaner.extractTranslationGroup("[やナい]作品名 第3话"))
        assertEquals("", MangaTitleCleaner.extractTranslationGroup("[A社×B组 联合汉化]作品名"))
    }

    @Test
    fun translationGroupMatchingRequiresAnExplicitGroupName() {
        assertTrue(
            MangaTitleCleaner.matchesTranslationGroup(
                "【提灯喵汉化组】作品名 第2话",
                "提灯喵汉化组"
            )
        )
        assertFalse(
            MangaTitleCleaner.matchesTranslationGroup(
                "【另一汉化组】作品名 第2话",
                "提灯喵汉化组"
            )
        )
    }

    @Test
    fun publisherMatchingUsesUidOrExactName() {
        assertTrue(
            MangaTitleCleaner.matchesPublisher(
                authorUid = "489445",
                authorName = "发布者A",
                publisherUid = "489445",
                publisherName = null
            )
        )
        assertTrue(
            MangaTitleCleaner.matchesPublisher(
                authorUid = null,
                authorName = "发布者B",
                publisherUid = null,
                publisherName = "发布者B"
            )
        )
        assertFalse(
            MangaTitleCleaner.matchesPublisher(
                authorUid = "123",
                authorName = "发布者A",
                publisherUid = null,
                publisherName = "发布者B"
            )
        )
    }
    @Test
    fun mobileSearchParserReturnsCompleteTitleInsteadOfHighlightOnly() {
        val html = """
            <html><body id="search">
              <ul class="threadlist">
                <li class="list">
                  <a href="forum.php?mod=viewthread&amp;tid=123">
                    <div class="threadlist_tit">后续任凭<em>想象</em>[やナい]把我弄乱吧</div>
                  </a>
                </li>
              </ul>
            </body></html>
        """.trimIndent()

        val parsed = MangaHtmlParser.parseListHtml(html)
        assertEquals(1, parsed.size)
        assertEquals("后续任凭想象[やナい]把我弄乱吧", parsed.single().rawTitle)
    }

    @Test
    fun pcSearchParserIsSupported() {
        val html = """
            <html><body class="pg_search">
              <div class="slst"><ul><li class="pbw">
                <h3 class="xs3">
                  <a href="forum.php?mod=viewthread&amp;tid=456">
                    【提灯喵汉化组】后续任凭<em>想象</em>[やナい]把我弄得乱七八糟吧！
                  </a>
                </h3>
              </li></ul></div>
            </body></html>
        """.trimIndent()

        val parsed = MangaHtmlParser.parseListHtml(html)
        assertEquals(1, parsed.size)
        assertEquals("456", parsed.single().tid)
        assertTrue(parsed.single().rawTitle.contains("把我弄得乱七八糟吧"))
    }

    @Test
    fun searchPaginationReadsPageNumbersFromLinks() {
        val html = """
            <div class="pg">
              <a href="search.php?mod=forum&amp;searchid=321&amp;page=2">2</a>
              <a class="nxt" href="search.php?mod=forum&amp;searchid=321&amp;page=8">下一页</a>
            </div>
        """.trimIndent()

        assertEquals("321", MangaHtmlParser.extractSearchId(html))
        assertEquals(8, MangaHtmlParser.extractTotalPages(html))
    }
}
