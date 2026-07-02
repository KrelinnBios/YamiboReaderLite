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

    /**
     * 判断 Cookie 串是否处于「电脑版」会话：用户点过论坛底部的电脑版切换后，
     * Discuz 会写入 mobile=no（含站点前缀如 EeqY_2132_mobile）cookie。
     */
    fun isDesktopSessionCookie(cookies: String?): Boolean {
        if (cookies.isNullOrBlank()) return false
        return cookies.split(';').any { pair ->
            val trimmed = pair.trim()
            val name = trimmed.substringBefore('=')
            val value = trimmed.substringAfter('=', "")
            value.equals("no", ignoreCase = true) &&
                    (name.equals("mobile", ignoreCase = true) ||
                            name.endsWith("_mobile", ignoreCase = true))
        }
    }

    /** 读取 WebView 会话 cookie 判断是否电脑版会话；单元测试等无 WebView 环境按手机版处理。 */
    private fun isDesktopWebSession(): Boolean = try {
        isDesktopSessionCookie(
            android.webkit.CookieManager.getInstance().getCookie("https://bbs.yamibo.com")
        )
    } catch (_: Throwable) {
        false
    }

    /**
     * 电脑版专属页（标签页 misc.php?mod=tag）在手机版会话下直接打开会变成
     * 「提示信息」并自动跳回论坛首页（且手机模板禁用缩放）。这里把这类链接
     * 强制加上 mobile=no，让服务器返回电脑版标签页。无需改写时返回 null。
     * 用户主动切到电脑版会话时不改写：电脑版本就能直接渲染标签页，且改写后的
     * mobile=no URL 会触发加载完成时的 cookie 恢复逻辑，把用户主动选择的
     * 电脑版会话误切回手机版。
     */
    fun normalizePcOnlyPageUrl(url: String?): String? {
        val parsed = url?.toHttpUrlOrNull() ?: return null
        if (parsed.host.lowercase() !in validHosts) return null
        if (!parsed.encodedPath.equals("/misc.php", ignoreCase = true)) return null
        if (!parsed.queryParameter("mod").equals("tag", ignoreCase = true)) return null
        if (parsed.queryParameter("mobile") == "no") return null
        if (isDesktopWebSession()) return null
        return parsed.newBuilder()
            .scheme("https")
            .host("bbs.yamibo.com")
            .removeAllQueryParameters("mobile")
            .addQueryParameter("mobile", "no")
            .build()
            .toString()
    }

    /**
     * 帖子链接兜底补 mobile=2：mobile cookie 一旦被电脑版页面（如 mobile=no 的标签页）
     * 污染成 no，之后所有不带 mobile 参数的帖子跳转（我的主题/我的回复/历史记录/原帖）
     * 都会渲染成电脑版，表现为"没有内容或被弹回"。给帖子链接显式带上 mobile=2 后，
     * 服务器会渲染手机版并顺带清掉污染的 mobile cookie（实测 Set-Cookie deleted）。
     * 仅处理站内帖子链接；已带 mobile 参数时返回 null 不改写。
     * 用户主动切到电脑版会话时也不改写：手机版会话点帖子开手机版、
     * 电脑版会话点帖子开电脑版，保持模板一致。
     */
    fun forceMobilePostUrl(url: String?): String? {
        val parsed = url?.toHttpUrlOrNull() ?: return null
        if (parsed.host.lowercase() !in validHosts) return null
        if (!isPostUrl(parsed)) return null
        if (!parsed.queryParameter("mobile").isNullOrBlank()) return null
        if (isDesktopWebSession()) return null
        return parsed.newBuilder()
            .addQueryParameter("mobile", "2")
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
