package org.shirakawatyu.yamibo.novel.util

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object YamiboPostLinkUtil {
    private val candidateRegex = Regex(
        """(?i)(?:https?://)?(?:(?:bbs|m|www)\.)?yamibo\.com/[^\s<>"']+"""
    )
    private val threadPathRegex = Regex("""^/thread-\d+(?:-\d+){0,2}\.html$""", RegexOption.IGNORE_CASE)
    private val imagePathRegex = Regex(
        """(?:^|/)(?:data/attachment/.*|\S+\.(?:jpg|jpeg|png|webp|gif|bmp))$""",
        RegexOption.IGNORE_CASE
    )
    private val validHosts = setOf(
        "bbs.yamibo.com",
        "m.yamibo.com",
        "www.yamibo.com",
        "yamibo.com"
    )
    private val trailingPunctuation = setOf(
        '.', ',', ';', ':', '!', '?',
        '。', '，', '；', '：', '！', '？',
        ')', ']', '}', '）', '】', '》', '"'
    )

    fun extractPostUrl(text: CharSequence?): String? {
        val rawText = text?.toString()?.replace("&amp;", "&").orEmpty()
        return candidateRegex.findAll(rawText)
            .mapNotNull { normalizeCandidate(it.value) }
            .firstOrNull()
    }

    fun normalizePostUrl(url: String?): String? {
        return extractPostUrl(url)
    }

    private fun normalizeCandidate(candidate: String): String? {
        val trimmed = candidate.trimEnd { it in trailingPunctuation }
        val withScheme = if (trimmed.startsWith("http", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        if (parsed.host.lowercase() !in validHosts) return null
        if (imagePathRegex.matches(parsed.encodedPath)) return null
        if (!isPostUrl(parsed)) return null

        return parsed.newBuilder()
            .scheme("https")
            .host("bbs.yamibo.com")
            .removeAllQueryParameters("highlight")
            .apply {
                if (parsed.queryParameter("mobile").isNullOrBlank()) {
                    addQueryParameter("mobile", "2")
                }
            }
            .build()
            .toString()
    }

    private fun isPostUrl(url: HttpUrl): Boolean {
        if (threadPathRegex.matches(url.encodedPath)) return true
        if (!url.encodedPath.equals("/forum.php", ignoreCase = true)) return false

        val mod = url.queryParameter("mod").orEmpty()
        val tid = url.queryParameter("tid")
        val pid = url.queryParameter("pid")
        return mod.equals("viewthread", ignoreCase = true) && tid.isPositiveId() ||
                mod.equals("redirect", ignoreCase = true) && pid.isPositiveId()
    }

    private fun String?.isPositiveId(): Boolean {
        return this?.matches(Regex("""[1-9]\d*""")) == true
    }
}
