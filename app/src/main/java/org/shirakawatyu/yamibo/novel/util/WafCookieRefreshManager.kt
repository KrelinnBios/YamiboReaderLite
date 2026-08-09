package org.shirakawatyu.yamibo.novel.util

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import org.shirakawatyu.yamibo.novel.YamiboApplication
import org.shirakawatyu.yamibo.novel.constant.RequestConfig
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal object WafCookieRefreshPolicy {
    const val REFRESH_INTERVAL_MS = 25 * 60 * 1000L
    const val RETRY_INTERVAL_MS = 2 * 60 * 1000L
    const val CHALLENGE_TIMEOUT_MS = 18_000L
    const val RECENT_SUCCESS_GRACE_MS = 15_000L

    fun nextDelayMs(succeeded: Boolean): Long =
        if (succeeded) REFRESH_INTERVAL_MS else RETRY_INTERVAL_MS

    fun hasRecentSuccess(lastSuccessMs: Long, nowMs: Long): Boolean =
        lastSuccessMs > 0L && nowMs - lastSuccessMs in 0..RECENT_SUCCESS_GRACE_MS
}

/**
 * 在应用前台保留一个屏幕外 WebView，让论坛注入的百度 WAF JavaScript 组件持续刷新挑战 Cookie。
 *
 * WebView CookieManager 是进程内共享的，因此这里拿到的新 Cookie 会同时供可见论坛页和原生
 * OkHttp 请求使用。隐藏页只加载主框架与脚本，图片被禁用；应用进入后台后立即销毁。
 */
object WafCookieRefreshManager {
    private const val FORUM_CHALLENGE_URL = "https://bbs.yamibo.com/forum.php?mobile=2"
    private const val READY_POLL_INTERVAL_MS = 500L

    private const val FORUM_READY_JS = """
        (function() {
            if (!document || !document.documentElement) return false;
            return !!(
                document.querySelector('meta[name="generator"][content*="Discuz"]') ||
                document.querySelector('input[name="formhash"]') ||
                document.getElementById('wp') ||
                document.getElementById('ct') ||
                document.querySelector('[id^="category_"]') ||
                document.querySelector('.threadlist')
            );
        })();
    """

    private class RefreshSignal {
        val latch = CountDownLatch(1)

        @Volatile
        var succeeded = false
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val signalLock = Any()

    private var ownerRef: WeakReference<Activity>? = null
    @Volatile
    private var webView: WebView? = null
    private var periodicRunnable: Runnable? = null
    private var readinessRunnable: Runnable? = null
    private var timeoutRunnable: Runnable? = null
    private var pageGeneration = 0
    @Volatile
    private var started = false

    @Volatile
    private var lastSuccessfulRefreshMs = 0L

    @Volatile
    private var activeSignal: RefreshSignal? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun start(activity: Activity) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { start(activity) }
            return
        }

        if (started && ownerRef?.get() === activity && webView != null) return
        stopInternal()

        started = true
        ownerRef = WeakReference(activity)
        val challengeUserAgent = runCatching {
            WebSettings.getDefaultUserAgent(activity)
        }.getOrDefault(RequestConfig.UA)
        YamiboApplication.systemUserAgent = challengeUserAgent
        val hiddenWebView = WebView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(1, 1).apply {
                leftMargin = -10_000
                topMargin = -10_000
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = false
                blockNetworkImage = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                userAgentString = challengeUserAgent
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: android.graphics.Bitmap?
                ) {
                    pageGeneration++
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pollUntilForumPageReady(pageGeneration)
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    if (view === webView) {
                        (view?.parent as? ViewGroup)?.removeView(view)
                        webView = null
                        completeRefresh(activeSignal, succeeded = false)
                    }
                    return true
                }
            }
        }

        webView = hiddenWebView
        (activity.window.decorView as? ViewGroup)?.addView(hiddenWebView)
        beginRefresh(newSignal())
    }

    fun stop(activity: Activity) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stop(activity) }
            return
        }
        if (ownerRef?.get() !== activity) return
        stopInternal()
    }

    /** 等待正在进行的首次/定时挑战，避免原生请求比新 Cookie 更早发出。 */
    fun awaitRefreshIfRunning(timeoutMs: Long = WafCookieRefreshPolicy.CHALLENGE_TIMEOUT_MS): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) return false
        val signal = activeSignal ?: return true
        return awaitSignal(signal, timeoutMs)
    }

    /** 收到 WAF 444 后立刻重新挑战；多个并发请求会合并为同一次隐藏页刷新。 */
    fun refreshAndWait(timeoutMs: Long = WafCookieRefreshPolicy.CHALLENGE_TIMEOUT_MS): Boolean {
        if (Looper.myLooper() == Looper.getMainLooper()) return false
        if (WafCookieRefreshPolicy.hasRecentSuccess(
                lastSuccessfulRefreshMs,
                SystemClock.elapsedRealtime()
            )
        ) {
            return true
        }
        if (!started || webView == null) return false

        val signal = synchronized(signalLock) {
            activeSignal ?: RefreshSignal().also {
                activeSignal = it
                mainHandler.post { beginRefresh(it) }
            }
        }
        return awaitSignal(signal, timeoutMs)
    }

    private fun awaitSignal(signal: RefreshSignal, timeoutMs: Long): Boolean {
        return try {
            signal.latch.await(timeoutMs, TimeUnit.MILLISECONDS) && signal.succeeded
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    private fun newSignal(): RefreshSignal = synchronized(signalLock) {
        activeSignal ?: RefreshSignal().also { activeSignal = it }
    }

    private fun beginRefresh(signal: RefreshSignal) {
        if (!started || signal !== activeSignal) {
            completeRefresh(signal, succeeded = false)
            return
        }
        val target = webView ?: run {
            completeRefresh(signal, succeeded = false)
            return
        }

        cancelRefreshCallbacks()
        YamiboSession.syncToWebView(FORUM_CHALLENGE_URL)
        runCatching {
            target.onResume()
            target.resumeTimers()
            target.loadUrl(
                FORUM_CHALLENGE_URL,
                mapOf("Cache-Control" to "no-cache")
            )
        }.onFailure {
            completeRefresh(signal, succeeded = false)
            return
        }

        timeoutRunnable = Runnable {
            completeRefresh(signal, succeeded = false)
        }.also {
            mainHandler.postDelayed(it, WafCookieRefreshPolicy.CHALLENGE_TIMEOUT_MS)
        }
        pollUntilForumPageReady(pageGeneration)
    }

    private fun pollUntilForumPageReady(expectedGeneration: Int) {
        val signal = activeSignal ?: return
        val target = webView ?: return
        readinessRunnable?.let(mainHandler::removeCallbacks)
        readinessRunnable = Runnable {
            if (signal !== activeSignal || target !== webView) return@Runnable
            if (expectedGeneration != pageGeneration) {
                pollUntilForumPageReady(pageGeneration)
                return@Runnable
            }
            runCatching {
                target.evaluateJavascript(FORUM_READY_JS) { result ->
                    if (signal !== activeSignal || target !== webView) return@evaluateJavascript
                    if (result.equals("true", ignoreCase = true)) {
                        YamiboSession.persistWebViewCookies(FORUM_CHALLENGE_URL)
                        completeRefresh(signal, succeeded = true)
                    } else {
                        pollUntilForumPageReady(pageGeneration)
                    }
                }
            }.onFailure {
                pollUntilForumPageReady(pageGeneration)
            }
        }.also {
            mainHandler.postDelayed(it, READY_POLL_INTERVAL_MS)
        }
    }

    private fun completeRefresh(signal: RefreshSignal?, succeeded: Boolean) {
        if (signal == null) return
        synchronized(signalLock) {
            if (signal !== activeSignal) return
            signal.succeeded = succeeded
            if (succeeded) {
                lastSuccessfulRefreshMs = SystemClock.elapsedRealtime()
            }
            activeSignal = null
        }
        cancelRefreshCallbacks()
        signal.latch.countDown()
        scheduleNextRefresh(WafCookieRefreshPolicy.nextDelayMs(succeeded))
    }

    private fun scheduleNextRefresh(delayMs: Long) {
        periodicRunnable?.let(mainHandler::removeCallbacks)
        if (!started) return
        periodicRunnable = Runnable {
            if (!started) return@Runnable
            if (webView != null) {
                beginRefresh(newSignal())
            } else {
                ownerRef?.get()?.takeUnless { it.isFinishing || it.isDestroyed }?.let(::start)
            }
        }.also { mainHandler.postDelayed(it, delayMs) }
    }

    private fun cancelRefreshCallbacks() {
        readinessRunnable?.let(mainHandler::removeCallbacks)
        timeoutRunnable?.let(mainHandler::removeCallbacks)
        readinessRunnable = null
        timeoutRunnable = null
    }

    private fun stopInternal() {
        started = false
        periodicRunnable?.let(mainHandler::removeCallbacks)
        periodicRunnable = null
        cancelRefreshCallbacks()

        val signal = synchronized(signalLock) {
            activeSignal.also { activeSignal = null }
        }
        signal?.let {
            it.succeeded = false
            it.latch.countDown()
        }

        webView?.let { target ->
            runCatching {
                target.stopLoading()
                (target.parent as? ViewGroup)?.removeView(target)
                target.removeAllViews()
                target.destroy()
            }
        }
        webView = null
        ownerRef = null
        pageGeneration = 0
    }
}
