package org.shirakawatyu.yamibo.novel.global

import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import org.shirakawatyu.yamibo.novel.YamiboApplication
import org.shirakawatyu.yamibo.novel.constant.RequestConfig
import org.shirakawatyu.yamibo.novel.util.manga.ImageCheckerUtil
import org.shirakawatyu.yamibo.novel.util.network.RateLimitInterceptor
import org.shirakawatyu.yamibo.novel.util.network.TtlDnsCache
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DynamicDns(private val bootstrapClient: OkHttpClient) : okhttp3.Dns {

    private val aliDns by lazy {
        DnsOverHttps.Builder().client(bootstrapClient)
            .url("https://dns.alidns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf(
                    InetAddress.getByName("223.5.5.5"),
                    InetAddress.getByName("223.6.6.6")
                )
            )
            .includeIPv6(false).build()
    }

    private val tencentDns by lazy {
        DnsOverHttps.Builder().client(bootstrapClient)
            .url("https://doh.pub/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                listOf(
                    InetAddress.getByName("1.12.12.12"),
                    InetAddress.getByName("120.53.53.53")
                )
            )
            .includeIPv6(false).build()
    }

    private val systemDns = Dns.SYSTEM

    private val manualDnsCache = ConcurrentHashMap<String, Dns>()

    private val raceExecutor = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "DnsRace").apply { isDaemon = true }
    }

    private fun getBootstrapHostsForDoHUrl(url: HttpUrl): List<InetAddress> {
        val host = url.host.lowercase()
        return when {
            host.contains("alidns") || host.contains("dns.aliyun") ->
                listOf(InetAddress.getByName("223.5.5.5"), InetAddress.getByName("223.6.6.6"))

            host.contains("doh.pub") || host.contains("dnspod") ->
                listOf(InetAddress.getByName("1.12.12.12"), InetAddress.getByName("120.53.53.53"))

            host.contains("cloudflare") || host.contains("1.1.1.1") ->
                listOf(InetAddress.getByName("1.1.1.1"), InetAddress.getByName("1.0.0.1"))

            host.contains("google") || host.contains("dns.google") ->
                listOf(InetAddress.getByName("8.8.8.8"), InetAddress.getByName("8.8.4.4"))

            else -> listOf(InetAddress.getByName("223.5.5.5"))
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val enabled = GlobalData.isDnsOptimizationEnabled.value
        if (!enabled) return systemDns.lookup(hostname)

        val mode = GlobalData.dnsOptimizationMode.value
        if (mode == "manual") {
            val url = GlobalData.customDnsUrl.value
            if (url.isBlank() || !url.startsWith("https://")) return systemDns.lookup(hostname)

            return try {
                val manualDns = manualDnsCache.getOrPut(url) {
                    DnsOverHttps.Builder().client(bootstrapClient)
                        .url(url.toHttpUrl())
                        .bootstrapDnsHosts(getBootstrapHostsForDoHUrl(url.toHttpUrl()))
                        .includeIPv6(false)
                        .build()
                }
                manualDns.lookup(hostname)
            } catch (_: Exception) {
                systemDns.lookup(hostname)
            }
        }

        val ecs = ExecutorCompletionService<List<InetAddress>>(raceExecutor)
        val futures = listOf(
            ecs.submit { aliDns.lookup(hostname) },
            ecs.submit { tencentDns.lookup(hostname) }
        )
        return try {
            ecs.poll(1500, TimeUnit.MILLISECONDS)?.get()
                ?: systemDns.lookup(hostname)
        } catch (_: Exception) {
            systemDns.lookup(hostname)
        }
    }
}

class YamiboRetrofit {

    companion object {
        private val pcUaList = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0"
        )

        private val currentPcUa = pcUaList.random()

        private val staticResourceRegex =
            Regex("\\.(jpg|jpeg|png|webp|gif|js|css|woff2?|ttf|eot|svg|ico)(\\?.*)?\$", RegexOption.IGNORE_CASE)

        private val acceptLanguage by lazy {
            val locale = Locale.getDefault()
            "${locale.language}-${locale.country},${locale.language};q=0.9,en-US;q=0.8,en;q=0.7"
        }

        // keepalive 必须短于论坛服务器的空闲超时（nginx 通常 60~75 秒），
        // 否则切回 App 时会复用已被服务器掐掉的半死连接，
        // 表现为 stream was reset: PROTOCOL_ERROR / 断联需手动刷新。
        private val sharedConnectionPool = ConnectionPool(
            maxIdleConnections = 8,
            keepAliveDuration = 50,
            timeUnit = TimeUnit.SECONDS
        )

        private val sharedBootstrapClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .build()
        }

        private val sharedDns by lazy {
            TtlDnsCache(delegate = DynamicDns(sharedBootstrapClient))
        }

        // 基础客户端
        val okHttpClient: OkHttpClient by lazy {
            createOkHttpClient(
                "http_cache_default",
                128L * 1024 * 1024,
                enableCache = true,
                enableImageChecker = false
            )
        }

        val threadOkHttpClient: OkHttpClient by lazy {
            createOkHttpClient(
                "http_cache_thread",
                0L,
                enableCache = false,
                enableImageChecker = true,
                maxRequestsPerHost = 6
            )
        }

        private val YamiboInstance: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(RequestConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
        }

        fun getInstance(): Retrofit {
            return YamiboInstance
        }

        private fun createOkHttpClient(
            cacheDirName: String,
            cacheSize: Long,
            enableCache: Boolean,
            enableImageChecker: Boolean,
            maxRequestsPerHost: Int = 5
        ): OkHttpClient {
            val customDispatcher = okhttp3.Dispatcher().apply {
                this.maxRequestsPerHost = maxRequestsPerHost
            }
            val builder = OkHttpClient.Builder()
                .dispatcher(customDispatcher)
                .dns(sharedDns)
                .connectionPool(sharedConnectionPool)
                .pingInterval(20, TimeUnit.SECONDS)

            if (enableCache) {
                val cacheDir = File(YamiboApplication.globalCacheDir, cacheDirName)
                builder.cache(okhttp3.Cache(cacheDir, cacheSize))
            }

            // 1. 应用拦截器
            builder.addInterceptor { chain ->
                val original = chain.request()
                if (!original.url.host.contains("yamibo.com")) {
                    return@addInterceptor chain.proceed(original)
                }

                // 若请求已携带 Cookie，
                // 优先使用它而非 GlobalData 中的缓存值，确保登录/登出后 cookie 及时生效
                val cookie = original.header("Cookie") ?: GlobalData.currentCookie
                val existingUa = original.header("User-Agent")
                val isPcPseudoRequest =
                    existingUa?.contains("Windows NT") == true || existingUa?.contains("Macintosh") == true

                val finalUa = if (isPcPseudoRequest) {
                    currentPcUa
                } else {
                    YamiboApplication.systemUserAgent.ifEmpty { RequestConfig.UA }
                }

                val requestBuilder = original.newBuilder()
                    .header("User-Agent", finalUa)
                    .header("Accept", RequestConfig.ACCEPT)
                    .header("Accept-Language", acceptLanguage)
                    .header("Cookie", cookie)

                if (
                    original.url.host.contains("yamibo.com") &&
                    original.header("Referer").isNullOrBlank()
                ) {
                    requestBuilder.header("Referer", "https://bbs.yamibo.com/")
                }

                val request = requestBuilder
                    .method(original.method, original.body)
                    .build()

                proceedWithDnsRecovery(chain, request)
            }
            if (enableImageChecker) {
                builder.addNetworkInterceptor(RateLimitInterceptor(100L))
            }
            // 2. 网络拦截器
            builder.addNetworkInterceptor { chain ->
                val request = chain.request()
                val rawResponse = chain.proceed(request)
                val urlStr = request.url.toString()

                val checkedResponse = if (enableImageChecker) {
                    try {
                        ImageCheckerUtil.interceptAndCheckImageStream(rawResponse, urlStr)
                    } catch (e: Exception) {
                        throw IOException("Blocked by ImageChecker: ${e.message}", e)
                    }
                } else {
                    rawResponse
                }

                val isForumImage = urlStr.contains("attachment/forum", ignoreCase = true)

                if (checkedResponse.isSuccessful) {
                    val isStaticResource = urlStr.contains(staticResourceRegex)

                    if (isStaticResource) {
                        if (isForumImage) {
                            val maxAge = 60 * 60 * 2
                            return@addNetworkInterceptor checkedResponse.newBuilder()
                                .header("Cache-Control", "public, max-age=$maxAge")
                                .removeHeader("Pragma")
                                .build()
                        } else {
                            val baseMaxAge = 60 * 60 * 24 * 7
                            val maxAge = baseMaxAge + kotlin.random.Random.nextInt(-86400, 86400)
                            val swr = 60 * 60 * 24 * 1
                            return@addNetworkInterceptor checkedResponse.newBuilder()
                                .header(
                                    "Cache-Control",
                                    "public, max-age=$maxAge, stale-while-revalidate=$swr"
                                )
                                .removeHeader("Pragma")
                                .build()
                        }
                    }
                }
                checkedResponse
            }

            return builder.build()
        }

        /**
         * 瞬时故障：HTTP/2 流被服务器重置（stream was reset: PROTOCOL_ERROR 等），
         * 或连接被对端在收发途中掐断（unexpected end of stream / connection reset 等）。
         * 这类错误重试一次通常即可恢复。
         */
        private fun isTransientStreamReset(e: IOException): Boolean {
            val msg = e.message ?: return false
            return msg.contains("stream was reset", ignoreCase = true) ||
                    msg.contains("PROTOCOL_ERROR", ignoreCase = true) ||
                    msg.contains("REFUSED_STREAM", ignoreCase = true) ||
                    msg.contains("unexpected end of stream", ignoreCase = true) ||
                    msg.contains("Connection reset", ignoreCase = true) ||
                    msg.contains("connection abort", ignoreCase = true) ||
                    msg.contains("connection closed", ignoreCase = true)
        }

        /**
         * nginx/WAF 反爬限流时会直接回 444（关闭连接、无正常响应体）。
         * 这是“请求被拒/限流”的临时信号，退避后换一条新连接重试通常即可放行。
         */
        private fun isRateLimitedResponse(response: okhttp3.Response): Boolean {
            return response.code == 444
        }

        private fun proceedWithDnsRecovery(
            chain: okhttp3.Interceptor.Chain,
            request: Request
        ): okhttp3.Response {
            var lastError: IOException? = null
            // GET 请求遇到瞬时流重置最多重试 3 次（稍弱网络下 stream was reset 偶发更频繁，
            // 多给一次重试明显提升头像/表情/封面这类小图的成活率）；444 WAF 限流仍只重试 2 次，
            // 避免反复打它加剧限流。每次重试前清空空闲连接换新连接，并做递增退避。
            repeat(4) { attempt ->
                try {
                    val response = chain.proceed(request)
                    val canRetryResponse = attempt < 2 &&
                            request.method == "GET" &&
                            isRateLimitedResponse(response)
                    if (!canRetryResponse) return response
                    // 444 没有可用响应体，丢弃后退避换连接重试。
                    response.close()
                    sharedConnectionPool.evictAll()
                    backoff(attempt)
                } catch (e: IOException) {
                    lastError = e
                    val canRetry = attempt < 3 &&
                            request.method == "GET" &&
                            isTransientStreamReset(e)
                    if (!canRetry) {
                        request.url.host.takeIf { it.contains("yamibo.com") }
                            ?.let(sharedDns::invalidate)
                        throw e
                    }
                    sharedConnectionPool.evictAll()
                    backoff(attempt)
                }
            }
            throw lastError ?: IOException("request failed after retries")
        }

        /** 递增退避：第 1 次重试 ~300ms，第 2 次 ~600ms（带少量抖动，避免与并发请求同时重试）。 */
        private fun backoff(attempt: Int) {
            try {
                val base = 300L * (attempt + 1)
                Thread.sleep(base + kotlin.random.Random.nextLong(0, 150))
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        fun proxyWebViewResource(request: android.webkit.WebResourceRequest): android.webkit.WebResourceResponse? {
            val url = request.url.toString()
            if (request.method != "GET" || url.startsWith("data:")) return null
            val isForumImage = url.contains("attachment/forum", ignoreCase = true)
            val client = if (isForumImage) threadOkHttpClient else okHttpClient

            try {
                val reqBuilder = okhttp3.Request.Builder().url(url)
                request.requestHeaders?.forEach { (key, value) -> reqBuilder.header(key, value) }

                if (
                    url.contains("yamibo.com", ignoreCase = true) &&
                    request.requestHeaders?.keys?.none {
                        it.equals("Referer", ignoreCase = true)
                    } != false
                ) {
                    reqBuilder.header("Referer", "https://bbs.yamibo.com/")
                }

                val response = client.newCall(reqBuilder.build()).execute()
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type", "") ?: ""
                    val mimeType =
                        contentType.substringBefore(";").trim().takeIf { it.isNotEmpty() }
                            ?: "application/octet-stream"
                    val encoding = if (contentType.contains("charset=")) {
                        contentType.substringAfter("charset=").replace("\"", "").trim()
                    } else null

                    val inputStream = response.body?.byteStream()
                    if (inputStream != null) {
                        val webResourceResponse =
                            android.webkit.WebResourceResponse(mimeType, encoding, inputStream)
                        webResourceResponse.setStatusCodeAndReasonPhrase(
                            response.code,
                            response.message.ifBlank { "OK" })
                        val responseHeaders = mutableMapOf<String, String>()
                        response.headers.forEach { (name, value) ->
                            responseHeaders[name] = value
                        }
                        webResourceResponse.responseHeaders = responseHeaders
                        return webResourceResponse
                    }
                }
            } catch (e: IOException) {
                request.url.host?.let { sharedDns.invalidate(it) }
                return null
            } catch (_: Exception) {
                return null
            }
            return null
        }

        @OptIn(ExperimentalCoilApi::class)
        fun isImageCachedInCoilDisk(cacheKey: String): Boolean {
            return try {
                val diskCache = YamiboApplication.application.imageLoader.diskCache ?: return false
                diskCache.openSnapshot(cacheKey)?.use {
                    true
                } ?: false
            } catch (_: Exception) {
                false
            }
        }
        // 是否排除暗黑改为按页面内容判断（只排除自定义 DIY 空间）：所有 bbs 主框架页都照常
        // 经代理抓取，再由 injectThemeCssIntoHtml → MemberSpaceGuard.isMemberSpaceHtml 决定
            // 注不注入。普通空间也能变深色，只有用了自定义背景图的 DIY 空间保持原样。
            fun proxyHtmlForDarkMode(request: android.webkit.WebResourceRequest): String? {
                val urlStr = request.url.toString()
                if (request.method != "GET" || !urlStr.startsWith("https://bbs.yamibo.com")) return null
                // mod=redirect（如「我的回复」的 goto=findpost）不走代理：让 WebView 像原色模式那样原生跟随
                // 302 落到真正的帖子 URL（#pidXXX 锚点由服务器重定向保留），那一跳的帖子页再被本代理正常注入
                // 深色样式。曾用「代理内容替换：URL 停在 findpost、内容换成帖子」实现首屏即深色，但暗黑下点
                // 「我的回复」会瞬间被弹回列表、甚至到不了楼层（URL 与内容不一致与 MinePage 状态机冲突）。
                // 改为原生跳转后行为与原色一致，只是多一跳重定向（可能有极短闪烁），换来导航正确。
                if (urlStr.contains("mod=redirect", ignoreCase = true) ||
                    urlStr.contains("goto=findpost", ignoreCase = true)
                ) {
                    return null
                }
                //
                // okHttpClient 没有 CookieJar：若让它自动跟随重定向，中途响应的 Set-Cookie 不会带到后续
                // 请求上。Discuz 的「电脑版/手机版」切换正是「Set-Cookie: mobile=... + 302」，自动跟随会
                // 让跟随后的请求仍用旧 cookie，服务器返回切换前的模板——表现为暗黑下点「电脑版」要点两次
                // （第一次只把 cookie 同步进了 CookieManager，第二次才生效）。亮色模式由 WebView 原生导航，
                // 其自带 cookie 处理会跨重定向带上新 cookie，故一次到位。
                // 这里关闭自动跟随，手动逐跳把 Set-Cookie 同步进 CookieManager，并用其最新值作为下一跳的
                // Cookie，使切换一次到位；普通页面无重定向，循环只跑一跳，行为与原先一致。
                return try {
                    val cm = android.webkit.CookieManager.getInstance()
                    val manualRedirectClient = okHttpClient.newBuilder()
                        .followRedirects(false)
                        .followSslRedirects(false)
                        .build()

                    var currentUrl = urlStr
                    var anchor: String? = null
                    var hops = 0
                    while (true) {
                        val reqBuilder = okhttp3.Request.Builder().url(currentUrl)
                        // 仅首跳沿用 WebView 原请求头；后续跳为服务器重定向目标，用干净请求头即可。
                        if (hops == 0) {
                            request.requestHeaders?.forEach { (k, v) -> reqBuilder.header(k, v) }
                        }
                        // 始终用 CookieManager 中的最新 cookie（含上一跳刚同步进去的切换 cookie）
                        val cmCookie = cm.getCookie(currentUrl)
                        if (!cmCookie.isNullOrEmpty()) reqBuilder.header("Cookie", cmCookie)
                        if (reqBuilder.build().header("Referer").isNullOrBlank()) {
                            reqBuilder.header("Referer", "https://bbs.yamibo.com/")
                        }
                        // 签到页是动态状态页：强制向服务器重新校验，避免 OkHttp 磁盘缓存命中旧响应，
                        // 导致显示的签到状态与后台自动签到后的真实状态不一致。
                        if (currentUrl.contains("zqlj_sign", ignoreCase = true)) {
                            reqBuilder.header("Cache-Control", "no-cache")
                        }

                        val response = manualRedirectClient.newCall(reqBuilder.build()).execute()
                        // 先把本跳 Set-Cookie 同步进 CookieManager，供下一跳与 WebView 使用
                        syncSetCookieToWebView(response)

                        if (response.isRedirect) {
                            val location = response.header("Location")
                            val resolved = location?.let { response.request.url.resolve(it) }
                            response.close()
                            if (resolved == null || hops >= 8) return null
                            if (anchor == null && location.contains('#')) {
                                anchor = location.substring(location.indexOf('#'))
                            }
                            currentUrl = resolved.toString()
                            hops++
                            continue
                        }

                        if (!response.isSuccessful) {
                            response.close()
                            return null
                        }
                        val body = response.body?.string()
                        response.close()
                        return if (body != null && anchor != null) {
                            injectAnchorScrollScript(body, anchor)
                        } else {
                            body
                        }
                    }
                    @Suppress("UNREACHABLE_CODE")
                    null
                } catch (_: Exception) {
                    null
                }
        }

        /** 在 HTML 中注入锚点滚动脚本，保留 #pidXXX 跳转。*/
        private fun injectAnchorScrollScript(html: String, anchor: String): String {
            val script = "<script>setTimeout(function(){location.hash='${anchor.substringAfter("#")}';},80);</script>"
            return if (html.contains("</body>")) {
                html.replace("</body>", "$script</body>")
            } else {
                html + script
            }
        }

        /** 将 OkHttp 响应链中的 Set-Cookie 同步到 WebView 的 CookieManager */
        private fun syncSetCookieToWebView(response: okhttp3.Response) {
            try {
                val cm = android.webkit.CookieManager.getInstance()
                var resp: okhttp3.Response? = response
                while (resp != null) {
                    val url = resp.request.url.toString()
                    for (header in resp.headers("Set-Cookie")) {
                        cm.setCookie(url, header)
                    }
                    resp = resp.priorResponse
                }
                cm.flush()
            } catch (_: Exception) {}
        }

        fun getPcUserAgent(): String = pcUaList.random()
    }
}
