package org.shirakawatyu.yamibo.novel.global

import android.util.DisplayMetrics
import android.webkit.WebChromeClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.shirakawatyu.yamibo.novel.module.YamiboWebViewClient
import org.shirakawatyu.yamibo.novel.util.CookieUtil

class GlobalData {

    companion object {
        val webViewClient = YamiboWebViewClient()
        val webChromeClient = WebChromeClient()
        var dataStore: DataStore<Preferences>? = null
        var displayMetrics: DisplayMetrics? = null
        var currentCookie: String = ""
        var isAppInitialized by mutableStateOf(false)
        val cookieFlow: Flow<String> by lazy {
            CookieUtil.getCookieFlow()
        }
        var tempMangaUrls: List<String> = emptyList()
        var tempMangaIndex: Int = 0
        var tempHtml: String = ""
        var tempTitle: String = ""

        val isAutoSignInEnabled = mutableStateOf(true)
        val isFavoriteCollapsed = MutableStateFlow(false)
        val webProgress = MutableStateFlow(0)
        val homePageRoute = MutableStateFlow("BBSPage")
        val isDarkMode = MutableStateFlow(false)
        val darkModeTheme = MutableStateFlow(0)
        val lightModeTheme = MutableStateFlow(0)
        val isCustomDnsEnabled = MutableStateFlow(false)
        val isClickToTopEnabled = MutableStateFlow(false)
        val isAutoVersionUpdateEnabled = MutableStateFlow(true)
        val isAutoClearCacheEnabled = MutableStateFlow(true)
        val isDnsOptimizationEnabled = MutableStateFlow(true)
        val dnsOptimizationMode = MutableStateFlow("auto")
        val customDnsUrl = MutableStateFlow("")

        val pendingClipboardUrl = MutableStateFlow<String?>(null)
        var lastClipboardUrl: String? = null
        val pendingDeepLinkUrl = MutableStateFlow<String?>(null)
    }
}
