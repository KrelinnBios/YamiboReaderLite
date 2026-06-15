package org.shirakawatyu.yamibo.novel.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.shirakawatyu.yamibo.novel.BuildConfig
import org.shirakawatyu.yamibo.novel.global.GlobalData
import org.shirakawatyu.yamibo.novel.global.YamiboRetrofit
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Long?,
    val releaseNotes: String,
    val apkUrl: String,
    val releasePageUrl: String
)

sealed interface AppUpdateCheckResult {
    data object NoUpdate : AppUpdateCheckResult
    data class UpdateAvailable(val info: AppUpdateInfo) : AppUpdateCheckResult
    data class Failed(val reason: String) : AppUpdateCheckResult
}

object AppUpdateManager {
    const val RELEASES_PAGE_URL =
        "https://github.com/KrelinnBios/YamiboReaderLite/releases"
    const val RELEASES_API_URL =
        "https://api.github.com/repos/KrelinnBios/YamiboReaderLite/releases?per_page=20"

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val MAX_APK_SIZE_BYTES = 512L * 1024L * 1024L

    // 自动检查的最小间隔：GitHub 未认证接口限流约每小时 60 次，频繁启动会触发 403。
    private val lastAutoCheckKey = longPreferencesKey("last_auto_update_check_ms")
    private const val AUTO_CHECK_MIN_INTERVAL_MS = 6L * 60 * 60 * 1000

    private val client by lazy {
        OkHttpClient.Builder()
            .dns(YamiboRetrofit.okHttpClient.dns)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .callTimeout(5, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * 启动时的自动检查：带节流。距上次自动检查不足 [AUTO_CHECK_MIN_INTERVAL_MS] 时返回 null（跳过），
     * 避免频繁启动把 GitHub 未认证接口的限流额度用尽（403）。无论成功失败都记录检查时间。
     * 失败由调用方静默处理；需要明确反馈时走用户手动的 [checkForUpdate]。
     */
    suspend fun checkForUpdateAuto(): AppUpdateCheckResult? = withContext(Dispatchers.IO) {
        val store = GlobalData.dataStore
        if (store != null) {
            val last = runCatching {
                store.data.firstOrNull()?.get(lastAutoCheckKey)
            }.getOrNull() ?: 0L
            if (System.currentTimeMillis() - last < AUTO_CHECK_MIN_INTERVAL_MS) {
                return@withContext null
            }
            runCatching { store.edit { it[lastAutoCheckKey] = System.currentTimeMillis() } }
        }
        checkForUpdate()
    }

    suspend fun checkForUpdate(): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.APP_UPDATE_URL.trim().ifBlank { RELEASES_API_URL }
        val requestUrl = if ('?' in endpoint) {
            "$endpoint&_=${System.currentTimeMillis()}"
        } else {
            "$endpoint?_=${System.currentTimeMillis()}"
        }

        val request = Request.Builder()
            .url(requestUrl)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "YamiboReaderLite/${BuildConfig.VERSION_NAME}")
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val reason = when (response.code) {
                        403, 429 -> "GitHub 接口暂时不可用（${response.code}，通常是短时间内请求过于频繁触发限流），请稍后再试或前往 Releases 页面手动查看"
                        404 -> "仓库暂无正式发布版本"
                        else -> "GitHub 返回 HTTP ${response.code}"
                    }
                    return@withContext AppUpdateCheckResult.Failed(reason)
                }

                val json = response.body?.string().orEmpty()
                val info = parseReleaseJson(json)
                    ?: return@withContext AppUpdateCheckResult.Failed(
                        "最新版本信息中没有可下载的 APK"
                    )

                if (compareVersions(info.versionName, BuildConfig.VERSION_NAME) <= 0) {
                    AppUpdateCheckResult.NoUpdate
                } else {
                    AppUpdateCheckResult.UpdateAvailable(info)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppUpdateCheckResult.Failed(error.message ?: "无法连接 GitHub")
        }
    }

    suspend fun downloadAndOpenInstaller(
        context: Context,
        info: AppUpdateInfo
    ): Result<Unit> {
        val appContext = context.applicationContext
        return try {
            val apkFile = withContext(Dispatchers.IO) {
                downloadAndValidateApk(appContext, info)
            }
            withContext(Dispatchers.Main) {
                openInstaller(appContext, apkFile)
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun openReleasesPage(context: Context): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    internal fun parseReleaseJson(json: String): AppUpdateInfo? {
        val parsed = runCatching { JSON.parse(json) }.getOrNull() ?: return null

        return when (parsed) {
            is JSONArray -> {
                val releases = buildList {
                    for (index in 0 until parsed.size) {
                        val release = parsed.getJSONObject(index) ?: continue
                        parseReleaseObject(release)?.let(::add)
                    }
                }

                releases.maxWithOrNull { left, right ->
                    compareVersions(left.versionName, right.versionName)
                }
            }

            is JSONObject -> parseReleaseObject(parsed)

            else -> null
        }
    }

    private fun parseReleaseObject(root: JSONObject): AppUpdateInfo? {
        if (root.getBooleanValue("draft") || root.getBooleanValue("prerelease")) return null

        val latestVersion = (
            root.getString("tag_name")
                ?: root.getString("versionName")
                ?: root.getString("version")
            )
            ?.removePrefix("v")
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: return null

        val assets = root.getJSONArray("assets")
        var apkUrl: String? = null
        var versionCode: Long? = null

        if (assets != null) {
            for (index in 0 until assets.size) {
                val asset = assets.getJSONObject(index) ?: continue
                val assetName = asset.getString("name").orEmpty()
                val contentType = asset.getString("content_type").orEmpty()

                if (assetName.endsWith(".apk", ignoreCase = true) ||
                    contentType.equals(APK_MIME_TYPE, ignoreCase = true)
                ) {
                    apkUrl = asset.getString("browser_download_url")
                    versionCode = asset.getLong("versionCode")
                    break
                }
            }
        }

        apkUrl = apkUrl
            ?: root.getString("downloadUrl")?.takeIf { it.endsWith(".apk", true) }
            ?: return null

        return AppUpdateInfo(
            versionName = latestVersion,
            versionCode = versionCode ?: root.getLong("versionCode"),
            releaseNotes = root.getString("body")
                ?: root.getString("releaseNotes")
                ?: "",
            apkUrl = apkUrl,
            releasePageUrl = root.getString("html_url")
                ?: root.getString("releaseUrl")
                ?: RELEASES_PAGE_URL
        )
    }

    private fun downloadAndValidateApk(context: Context, info: AppUpdateInfo): File {
        val updateDir = File(context.externalCacheDir ?: context.cacheDir, "update")
        if (!updateDir.exists() && !updateDir.mkdirs()) {
            throw IOException("无法创建更新缓存目录")
        }

        updateDir.listFiles()
            ?.filter { it.name.startsWith("300 Lite-") || it.name == "300 Lite.apk" }
            ?.forEach { it.delete() }

        val safeVersionName = info.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val targetFile = File(updateDir, "300 Lite-$safeVersionName.apk")
        val tempFile = File(updateDir, "${targetFile.name}.download")
        tempFile.delete()
        targetFile.delete()

        val apkDownloadUrl = if ('?' in info.apkUrl) {
            "${info.apkUrl}&_=${System.currentTimeMillis()}"
        } else {
            "${info.apkUrl}?_=${System.currentTimeMillis()}"
        }

        val request = Request.Builder()
            .url(apkDownloadUrl)
            .header("Accept", APK_MIME_TYPE)
            .header("User-Agent", "YamiboReaderLite/${BuildConfig.VERSION_NAME}")
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("APK 下载失败：HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("APK 下载内容为空")
                val contentLength = body.contentLength()
                if (contentLength > MAX_APK_SIZE_BYTES) {
                    throw IOException("APK 文件大小异常")
                }

                body.byteStream().use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var totalBytes = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            if (totalBytes > MAX_APK_SIZE_BYTES) {
                                throw IOException("APK 文件大小异常")
                            }
                            output.write(buffer, 0, count)
                        }
                        if (totalBytes <= 0L || contentLength > 0L && totalBytes != contentLength) {
                            throw IOException("APK 下载不完整")
                        }
                    }
                }
            }

            validateApk(context, tempFile, info)
            targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            return targetFile
        } catch (error: Exception) {
            tempFile.delete()
            throw error
        }
    }

    private fun validateApk(context: Context, apkFile: File, info: AppUpdateInfo) {
        val packageInfoFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(packageInfoFlags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, packageInfoFlags)
        } ?: throw IOException("下载的文件不是有效 APK")

        if (packageInfo.packageName != context.packageName) {
            throw IOException("APK 包名不匹配")
        }

        val downloadedVersionName = packageInfo.versionName.orEmpty()
        if (compareVersions(downloadedVersionName, info.versionName) != 0) {
            throw IOException(
                "APK versionName 与发布信息不一致：下载到 v$downloadedVersionName，预期 v${info.versionName}"
            )
        }

        val downloadedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        if (downloadedVersionCode <= BuildConfig.VERSION_CODE.toLong()) {
            throw IOException("APK versionCode 未高于当前版本")
        }
        if (info.versionCode != null && downloadedVersionCode != info.versionCode) {
            throw IOException("APK versionCode 与发布信息不一致")
        }

        val installedPackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(packageInfoFlags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, packageInfoFlags)
        }
        val installedCertificates = signingCertificateDigests(installedPackageInfo)
        val downloadedCertificates = signingCertificateDigests(packageInfo)
        if (installedCertificates.isNotEmpty() &&
            downloadedCertificates.isNotEmpty() &&
            installedCertificates.intersect(downloadedCertificates).isEmpty()
        ) {
            throw IOException(
                "APK 签名与当前安装版本不一致，请卸载旧版后从下载页重新安装"
            )
        }
    }

    private fun signingCertificateDigests(
        packageInfo: android.content.pm.PackageInfo
    ): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        val digest = MessageDigest.getInstance("SHA-256")
        return signatures.orEmpty().mapTo(linkedSetOf()) { signature ->
            digest.digest(signature.toByteArray()).joinToString("") { byte ->
                "%02x".format(byte)
            }
        }
    }

    private fun openInstaller(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            throw IOException("系统中没有可用的 APK 安装器")
        }
        context.startActivity(intent)
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
