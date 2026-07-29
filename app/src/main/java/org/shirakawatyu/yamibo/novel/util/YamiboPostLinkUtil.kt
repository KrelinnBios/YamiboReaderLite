package org.shirakawatyu.yamibo.novel.util

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object YamiboPostLinkUtil {
    private val candidateRegex = Regex(
        """(?i)(?:https?://)?(?:(?:bbs|m|www)\.)?yamibo\.com/[^\s<>"']+"""
    )
    private val threadPathRegex = Regex("""^/thread-\d+(?:-\d+){0,2}\.html$""", RegexOption.IGNORE_CASE)
    private val forumPathRegex = Regex("""^/forum-\d+(?:-\d+){0,2}\.html$""", RegexOption.IGNORE_CASE)
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
     * App 默认保持当前模板：尚未主动切到电脑版时，Discuz 的分页或跳转链接如果没有
     * mobile 参数，就补上 mobile=2，避免版块/帖子下一页因 cookie 或链接缺失突然变成电脑版。
     * 只有明确的论坛模板切换入口才改变用户选择；普通分页即使错误带有 mobile=no，在用户
     * 尚未切换前也会纠正为手机版。当前已经是电脑版会话时，后续跳转保持电脑版。
     *
     * 标签页和明确进入的个人空间沿用现有电脑版流程；静态资源、附件、搜索等也不属于
     * 常规论坛页面。无需改写时返回 null，避免 loadUrl 循环。
     */
    fun forceMobileForumPageUrl(url: String?, desktopSession: Boolean): String? {
        val parsed = url?.toHttpUrlOrNull() ?: return null
        if (parsed.host.lowercase() !in validHosts) return null
        if (!isMobileForumPage(parsed)) return null
        if (explicitDesktopTemplateSelection(url) != null) return null
        if (desktopSession) return null
        val mobileValues = parsed.queryParameterValues("mobile")
        if (mobileValues.size == 1 &&
            (mobileValues.single().equals("2", ignoreCase = true) ||
                    mobileValues.single().equals("yes", ignoreCase = true))
        ) {
            return null
        }
        return parsed.newBuilder()
            .scheme("https")
            .host("bbs.yamibo.com")
            .removeAllQueryParameters("mobile")
            .addQueryParameter("mobile", "2")
            .build()
            .toString()
    }
    /**
     * 识别明确的模板选择。任何 mobile=2/yes 页面都能确认当前已回到手机版；只有论坛
     * 首页的纯 mobile=no 切换入口才记为用户主动选择电脑版，分页或特殊页面的 no 不算。
     */
    fun explicitDesktopTemplateSelection(url: String?): Boolean? {
        val parsed = url?.toHttpUrlOrNull() ?: return null
        if (parsed.host.lowercase() !in validHosts) return null
        val values = parsed.queryParameterValues("mobile")
        if (values.size != 1) return null
        return when (values.single()?.lowercase()) {
            "2", "yes" -> false
            "no" -> {
                val isForumHome =
                    parsed.encodedPath.lowercase() in setOf("/", "/index.php", "/forum.php")
                val hasOnlyMobile =
                    parsed.queryParameterNames.all { it.equals("mobile", ignoreCase = true) }
                true.takeIf { isForumHome && hasOnlyMobile }
            }
            else -> null
        }
    }
    private fun isMobileForumPage(url: HttpUrl): Boolean {
        if (threadPathRegex.matches(url.encodedPath) || forumPathRegex.matches(url.encodedPath)) {
            return true
        }
        return when (url.encodedPath.lowercase()) {
            "/", "/index.php", "/forum.php", "/member.php" -> true
            "/home.php" -> {
                val isDesktopSpace =
                    url.queryParameter("mod").equals("space", ignoreCase = true) &&
                            url.queryParameter("mobile").equals("no", ignoreCase = true)
                !isDesktopSpace
            }
            "/misc.php" -> !url.queryParameter("mod").equals("tag", ignoreCase = true)
            else -> false
        }
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
