package org.shirakawatyu.yamibo.novel.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import org.shirakawatyu.yamibo.novel.YamiboApplication
import org.shirakawatyu.yamibo.novel.bean.ReaderSettings
import org.shirakawatyu.yamibo.novel.global.GlobalData

/**
 * 设置管理工具
 * 负责保存和读取阅读器设置、省流模式开关以及折叠模式状态
 */
class SettingsUtil {
    companion object {
        private val key = stringPreferencesKey("settings")
        private val collapseModeKey = stringPreferencesKey("favorite_collapse_mode")
        private val homePageKey = stringPreferencesKey("home_page")
        private val customDnsKey = stringPreferencesKey("custom_dns_mode")
        private val clickToTopKey = stringPreferencesKey("click_to_top_mode")
        private val autoSignInKey = stringPreferencesKey("auto_sign_in")
        private val autoVersionUpdateKey = stringPreferencesKey("auto_version_update")
        private val autoClearCacheKey = stringPreferencesKey("auto_clear_cache")
        private val dnsEnabledKey = stringPreferencesKey("dns_optimization_enabled")
        private val dnsModeKey = stringPreferencesKey("dns_optimization_mode")
        private val darkModeKey = stringPreferencesKey("dark_mode")
        private val customDnsUrlKey = stringPreferencesKey("custom_dns_url")
        private val skipVersionKey = stringPreferencesKey("skip_version")
        fun saveSettings(settings: ReaderSettings) {
            DataStoreUtil.addData(JSON.toJSONString(settings), key)
        }

        fun getSettings(callback: (settings: ReaderSettings) -> Unit, onNull: () -> Unit) {
            DataStoreUtil.getData(key, callback = {
                try {
                    val settings = JSON.parseObject(it, ReaderSettings::class.java)
                    callback(settings)
                } catch (_: JSONException) {
                    onNull()
                }
            }, onNull = onNull)
        }


        fun saveFavoriteCollapseMode(isCollapsed: Boolean) {
            DataStoreUtil.addData(isCollapsed.toString(), collapseModeKey)
        }

        fun getFavoriteCollapseMode(callback: (isCollapsed: Boolean) -> Unit) {
            DataStoreUtil.getData(collapseModeKey, callback = {
                callback(it.toBooleanStrictOrNull() ?: false)
            }, onNull = {
                callback(false)
            })
        }
        fun saveHomePage(route: String) {
            DataStoreUtil.addData(route, homePageKey)
        }

        fun getHomePage(callback: (route: String) -> Unit) {
            DataStoreUtil.getData(homePageKey, callback = {
                callback(it.ifBlank { "BBSPage" })
            }, onNull = {
                callback("BBSPage")
            })
        }
        fun saveCustomDnsMode(isEnabled: Boolean) {
            DataStoreUtil.addData(isEnabled.toString(), customDnsKey)
        }

        fun getCustomDnsMode(callback: (isEnabled: Boolean) -> Unit) {
            DataStoreUtil.getData(customDnsKey, callback = {
                callback(it.toBooleanStrictOrNull() ?: false)
            }, onNull = {
                callback(false)
            })
        }
        fun saveClickToTopMode(isEnabled: Boolean) {
            DataStoreUtil.addData(isEnabled.toString(), clickToTopKey)
        }

        fun getClickToTopMode(callback: (isEnabled: Boolean) -> Unit) {
            DataStoreUtil.getData(clickToTopKey, callback = {
                callback(it.toBooleanStrictOrNull() ?: false)
            }, onNull = {
                callback(false)
            })
        }
        fun saveAutoSignInMode(isEnabled: Boolean) {
            DataStoreUtil.addData(isEnabled.toString(), autoSignInKey)
        }

        fun getAutoSignInMode(callback: (Boolean) -> Unit) {
            DataStoreUtil.getData(autoSignInKey, callback = {
                callback(it.toBooleanStrictOrNull() ?: true)
            }, onNull = {
                callback(true)
            })
        }
        fun saveAutoClearCacheMode(isEnabled: Boolean) {
            DataStoreUtil.addData(isEnabled.toString(), autoClearCacheKey)
        }

        fun saveAutoVersionUpdateMode(isEnabled: Boolean) {
            DataStoreUtil.addData(isEnabled.toString(), autoVersionUpdateKey)
        }

        fun getAutoVersionUpdateMode(callback: (Boolean) -> Unit) {
            DataStoreUtil.getData(autoVersionUpdateKey, callback = {
                callback(it.toBooleanStrictOrNull() ?: true)
            }, onNull = {
                callback(true)
            })
        }

        suspend fun getAutoVersionUpdateMode(): Boolean {
            val preferences = GlobalData.dataStore?.data?.first() ?: return true
            return preferences[autoVersionUpdateKey]?.toBooleanStrictOrNull() ?: true
        }

        fun getAutoClearCacheMode(callback: (Boolean) -> Unit) {
            DataStoreUtil.getData(autoClearCacheKey, callback = {
                callback(it.toBooleanStrictOrNull() ?: true)
            }, onNull = {
                callback(true)
            })
        }
        fun saveDnsOptimizationEnabled(enabled: Boolean) {
            DataStoreUtil.addData(enabled.toString(), dnsEnabledKey)
        }
        fun getDnsOptimizationEnabled(callback: (Boolean) -> Unit) {
            DataStoreUtil.getData(dnsEnabledKey, callback = { value ->
                callback(value.toBooleanStrictOrNull() ?: true)
            }, onNull = {
                DataStoreUtil.getData(customDnsKey, callback = { oldValue ->
                    callback(oldValue.toBooleanStrictOrNull() ?: true)
                }, onNull = { callback(true) })
            })
        }
        fun saveDnsOptimizationMode(mode: String) {
            DataStoreUtil.addData(mode, dnsModeKey)
        }
        fun getDnsOptimizationMode(callback: (String) -> Unit) {
            DataStoreUtil.getData(dnsModeKey, callback = { value ->
                callback(value.ifBlank { "auto" })
            }, onNull = { callback("auto") })
        }
        fun saveCustomDnsUrl(url: String) {
            DataStoreUtil.addData(url, customDnsUrlKey)
        }
        fun getCustomDnsUrl(callback: (String) -> Unit) {
            DataStoreUtil.getData(customDnsUrlKey, callback = { value ->
                callback(value)
            }, onNull = {
                callback("")
            })
        }
        // 冷启动开屏引导缓存：DataStore 只能异步读，进程启动那一刻拿不到暗黑开关的值，
        // 导致开屏永远按浅色画。这里把暗黑标记同步镜像到 SharedPreferences，供 onCreate
        // 同步读取并立即套用深色开屏。真值仍以 DataStore 为准，这份仅作启动引导缓存。
        private const val bootstrapPrefs = "launch_bootstrap"
        private const val bootstrapDarkKey = "dark_mode"

        fun saveDarkMode(enabled: Boolean) {
            DataStoreUtil.addData(enabled.toString(), darkModeKey)
            runCatching {
                YamiboApplication.application
                    .getSharedPreferences(bootstrapPrefs, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(bootstrapDarkKey, enabled)
                    .apply()
            }
        }

        fun readDarkModeBootstrap(): Boolean {
            return runCatching {
                YamiboApplication.application
                    .getSharedPreferences(bootstrapPrefs, Context.MODE_PRIVATE)
                    .getBoolean(bootstrapDarkKey, false)
            }.getOrDefault(false)
        }
        fun getDarkMode(callback: (Boolean) -> Unit) {
            DataStoreUtil.getData(darkModeKey, callback = {
                callback(it.toBooleanStrictOrNull() ?: false)
            }, onNull = {
                callback(false)
            })
        }
        fun saveSkipVersion(version: String) {
            DataStoreUtil.addData(version, skipVersionKey)
        }
        fun getSkipVersion(callback: (String) -> Unit) {
            DataStoreUtil.getData(skipVersionKey, callback = {
                callback(it)
            }, onNull = {
                callback("")
            })
        }

        fun saveLastUpdateCheckTime(millis: Long) {
            DataStoreUtil.addData(millis.toString(), stringPreferencesKey("last_update_check"))
        }
        fun getLastUpdateCheckTime(callback: (Long) -> Unit) {
            DataStoreUtil.getData(stringPreferencesKey("last_update_check"), callback = {
                callback(it.toLongOrNull() ?: 0L)
            }, onNull = {
                callback(0L)
            })
        }
    }
}
