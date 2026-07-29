package org.shirakawatyu.yamibo.novel.util.blog

import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.shirakawatyu.yamibo.novel.global.YamiboRetrofit
import org.shirakawatyu.yamibo.novel.util.YamiboSession
import java.io.IOException

internal data class BlogReactionOption(
    val clickId: String,
    val label: String,
    val iconUrl: String,
    val count: Int,
    val barClass: String,
    val actionUrl: String
)

internal data class BlogReactionUser(
    val uid: String,
    val username: String,
    val avatarUrl: String,
    val reaction: String
)

internal data class BlogReactionSnapshot(
    val options: List<BlogReactionOption>,
    val users: List<BlogReactionUser>,
    val totalCount: Int
)

internal data class BlogReactionUpdate(
    val snapshot: BlogReactionSnapshot,
    val message: String
)

internal object BlogReactionRemoteClient {
    private const val FORUM_ROOT = "https://bbs.yamibo.com/"
    private val numericId = Regex("[1-9]\\d*")

    fun fetchSnapshot(ownerUid: String, blogId: String): BlogReactionSnapshot {
        val blogUrl = desktopBlogUrl(ownerUid, blogId)
        return parseSnapshot(execute(Request.Builder().url(blogUrl).get()), blogUrl.toString())
            ?: throw IOException("手机版表态信息暂时无法加载")
    }

    fun addReaction(ownerUid: String, blogId: String, clickId: String): BlogReactionUpdate {
        val normalizedClickId = clickId.takeIf { it.matches(numericId) }
            ?: throw IOException("无效的表态类型")
        val blogUrl = desktopBlogUrl(ownerUid, blogId)
        val before = fetchSnapshot(ownerUid, blogId)
        val option = before.options.firstOrNull { it.clickId == normalizedClickId }
            ?: throw IOException("该表态选项已失效，请刷新后重试")
        val cookie = YamiboSession.cookieFor(FORUM_ROOT)
        if (!Regex("(?:^|;\\s*)[^=;]*_auth=").containsMatchIn(cookie)) {
            throw IOException("请先登录后再表态")
        }

        val actionUrl = validatedActionUrl(option.actionUrl, blogId, normalizedClickId)
        val ajaxUrl = actionUrl.newBuilder()
            .apply {
                if (actionUrl.queryParameter("inajax").isNullOrBlank()) {
                    addQueryParameter("inajax", "1")
                }
                if (actionUrl.queryParameter("ajaxtarget").isNullOrBlank()) {
                    addQueryParameter("ajaxtarget", "fwin_content_clickhandle")
                }
            }
            .build()
        val responseHtml = execute(
            Request.Builder()
                .url(ajaxUrl)
                .header("Referer", blogUrl.toString())
                .header("X-Requested-With", "XMLHttpRequest")
                .get()
        )
        val after = fetchSnapshot(ownerUid, blogId)
        val changed = reactionState(before) != reactionState(after)
        val responseMessage = extractResponseMessage(responseHtml)
        val message = when {
            changed -> responseMessage.ifBlank { "表态成功" }
            responseMessage.isNotBlank() -> responseMessage
            else -> "表态未更新，可能已经表态过"
        }
        return BlogReactionUpdate(after, message)
    }

    private fun desktopBlogUrl(ownerUid: String, blogId: String): HttpUrl {
        val safeUid = ownerUid.takeIf { it.matches(numericId) }
            ?: throw IOException("无效的日志作者")
        val safeBlogId = blogId.takeIf { it.matches(numericId) }
            ?: throw IOException("无效的日志编号")
        return "$FORUM_ROOT/home.php".toHttpUrl().newBuilder()
            .addQueryParameter("mod", "space")
            .addQueryParameter("uid", safeUid)
            .addQueryParameter("do", "blog")
            .addQueryParameter("id", safeBlogId)
            .addQueryParameter("mobile", "no")
            .build()
    }

    private fun validatedActionUrl(
        rawUrl: String,
        blogId: String,
        clickId: String
    ): HttpUrl {
        val url = rawUrl.toHttpUrl()
        val valid = url.host.equals("bbs.yamibo.com", ignoreCase = true) &&
                url.encodedPath == "/home.php" &&
                url.queryParameter("mod") == "spacecp" &&
                url.queryParameter("ac") == "click" &&
                url.queryParameter("op") == "add" &&
                url.queryParameter("idtype") == "blogid" &&
                url.queryParameter("id") == blogId &&
                url.queryParameter("clickid") == clickId &&
                !url.queryParameter("hash").isNullOrBlank()
        if (!valid) throw IOException("表态链接校验失败，请刷新后重试")
        return url
    }

    private fun execute(builder: Request.Builder): String {
        val cookie = YamiboSession.cookieFor(FORUM_ROOT)
        val request = builder
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            )
            .apply {
                if (cookie.isNotBlank()) {
                    header("Cookie", YamiboSession.desktopCookie(cookie))
                }
            }
            .build()
        return YamiboRetrofit.okHttpClient.newCall(request).execute().use { response ->
            YamiboSession.storeSetCookies(
                response.request.url.toString(),
                response.headers("Set-Cookie").filterNot { header ->
                    isMobileCookieName(header.substringBefore('=').trim())
                }
            )
            if (!response.isSuccessful) {
                throw IOException("表态请求失败：HTTP ${response.code}")
            }
            response.body?.string().orEmpty()
        }
    }

    private fun isMobileCookieName(name: String): Boolean =
        name.equals("mobile", ignoreCase = true) ||
                name.endsWith("_mobile", ignoreCase = true)

    internal fun parseSnapshot(html: String, baseUrl: String): BlogReactionSnapshot? {
        if (html.isBlank()) return null
        val document = Jsoup.parse(html, baseUrl)
        val clickDiv = document.selectFirst("#click_div") ?: return null
        val options = clickDiv.select(".atd a[id^=click_]").mapNotNull { link ->
            val actionUrl = link.absUrl("href").ifBlank { link.attr("href") }
            val parsedAction = runCatching { actionUrl.toHttpUrl() }.getOrNull()
                ?: return@mapNotNull null
            val clickId = parsedAction.queryParameter("clickid")
                ?.takeIf { it.matches(numericId) }
                ?: return@mapNotNull null
            val iconUrl = link.selectFirst("img[src*=/static/image/click/]")
                ?.absUrl("src")
                .orEmpty()
            val label = link.ownText().trim()
            if (iconUrl.isBlank() || label.isBlank()) return@mapNotNull null
            val bar = link.selectFirst(".atdc > div")
            BlogReactionOption(
                clickId = clickId,
                label = label,
                iconUrl = iconUrl,
                count = link.selectFirst(".atdc em")?.text()?.trim()?.toIntOrNull() ?: 0,
                barClass = bar?.classNames()
                    ?.firstOrNull { it.matches(Regex("ac[1-4]")) }
                    .orEmpty(),
                actionUrl = actionUrl
            )
        }.distinctBy { it.clickId }
        if (options.isEmpty()) return null

        val users = clickDiv.select("#trace_ul > li").mapNotNull { item ->
            val profileLink = item.selectFirst(".avt a[href], p a[href]") ?: return@mapNotNull null
            val uid = extractUid(profileLink.attr("href")) ?: return@mapNotNull null
            val nameLink = item.selectFirst("p a")
            val username = nameLink?.attr("title")?.trim()
                ?.takeIf(String::isNotBlank)
                ?: nameLink?.text()?.trim()?.takeIf(String::isNotBlank)
                ?: "UID $uid"
            BlogReactionUser(
                uid = uid,
                username = username,
                avatarUrl = item.selectFirst(".avt img[src]")?.absUrl("src").orEmpty(),
                reaction = item.selectFirst(".avt a[title]")?.attr("title")?.trim().orEmpty()
            )
        }.distinctBy { it.uid }
        val totalCount = Regex("(\\d+)\\s*人")
            .find(clickDiv.selectFirst("h3")?.text().orEmpty())
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: options.sumOf { it.count }
        return BlogReactionSnapshot(options, users, totalCount)
    }

    internal fun extractResponseMessage(html: String): String {
        if (html.isBlank()) return ""
        val document = Jsoup.parse(html)
        val elementMessage = document.selectFirst(
            "#messagetext, .showmessage, .alert_error, .alert_info, .alert_right"
        )?.text()?.trim().orEmpty()
        if (elementMessage.isNotBlank()) return elementMessage

        val decoded = Parser.unescapeEntities(html, false)
        return Regex("""showDialog\(\s*['"]([^'"]+)['"]""")
            .find(decoded)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\'", "'")
            ?.replace("\\\"", "\"")
            ?.trim()
            .orEmpty()
    }

    private fun reactionState(snapshot: BlogReactionSnapshot): Pair<List<Pair<String, Int>>, List<String>> =
        snapshot.options.map { it.clickId to it.count } to snapshot.users.map { it.uid }

    private fun extractUid(href: String): String? =
        Regex("space-uid-([1-9]\\d*)", RegexOption.IGNORE_CASE)
            .find(href)
            ?.groupValues
            ?.getOrNull(1)
            ?: Regex("[?&]uid=([1-9]\\d*)", RegexOption.IGNORE_CASE)
                .find(href)
                ?.groupValues
                ?.getOrNull(1)
}
