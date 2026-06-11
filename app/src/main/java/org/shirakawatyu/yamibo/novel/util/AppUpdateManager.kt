package org.shirakawatyu.yamibo.novel.util

import com.alibaba.fastjson2.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.shirakawatyu.yamibo.novel.BuildConfig
import org.shirakawatyu.yamibo.novel.global.YamiboRetrofit

data class AppUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val releaseUrl: String
)

object AppUpdateManager {
    private val client by lazy {
        OkHttpClient.Builder()
            .dns(YamiboRetrofit.okHttpClient.dns)
            .build()
    }

    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.APP_UPDATE_URL.trim()
        if (endpoint.isBlank()) return@withContext null

        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/vnd.github+json")
            .header("Cache-Control", "no-cache")
            .build()

        val response = runCatching {
            client.newCall(request).execute()
        }.getOrNull() ?: return@withContext null

        response.use {
            if (!it.isSuccessful) return@withContext null
            val root = runCatching {
                JSON.parseObject(it.body?.string().orEmpty())
            }.getOrNull() ?: return@withContext null
            val latestVersion = (
                root.getString("tag_name")
                    ?: root.getString("versionName")
                    ?: root.getString("version")
                )
                ?.removePrefix("v")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return@withContext null

            if (compareVersions(latestVersion, BuildConfig.VERSION_NAME) <= 0) {
                return@withContext null
            }

            val assets = root.getJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (index in 0 until assets.size) {
                    val asset = assets.getJSONObject(index) ?: continue
                    if (asset.getString("name")
                            ?.endsWith(".apk", ignoreCase = true) == true
                    ) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
            }

            AppUpdateInfo(
                versionName = latestVersion,
                releaseNotes = root.getString("body")
                    ?: root.getString("releaseNotes")
                    ?: "",
                releaseUrl = apkUrl
                    ?: root.getString("html_url")
                    ?: root.getString("releaseUrl")
                    ?: root.getString("downloadUrl")
                    ?: endpoint
            )
        }
    }

    internal fun compareVersions(left: String, right: String): Int {
        val leftParts = left.removePrefix("v").split('.', '-', '_')
        val rightParts = right.removePrefix("v").split('.', '-', '_')
        val size = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until size) {
            val leftValue = leftParts.getOrNull(index)?.toIntOrNull() ?: 0
            val rightValue = rightParts.getOrNull(index)?.toIntOrNull() ?: 0
            if (leftValue != rightValue) return leftValue.compareTo(rightValue)
        }
        return 0
    }
}
