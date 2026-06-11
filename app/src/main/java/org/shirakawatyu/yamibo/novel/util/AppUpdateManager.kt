package org.shirakawatyu.yamibo.novel.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.alibaba.fastjson2.JSON
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.shirakawatyu.yamibo.novel.BuildConfig
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
    const val LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/KrelinnBios/YamiboReaderLite/releases/latest"

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val MAX_APK_SIZE_BYTES = 512L * 1024L * 1024L

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

    suspend fun checkForUpdate(): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.APP_UPDATE_URL.trim().ifBlank { LATEST_RELEASE_API_URL }
        val request = Request.Builder()
            .url(endpoint)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "YamiboReaderLite/${BuildConfig.VERSION_NAME}")
            .header("Cache-Control", "no-cache")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppUpdateCheckResult.Failed(
                        "GitHub 返回 HTTP ${response.code}"
                    )
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
        val root = runCatching { JSON.parseObject(json) }.getOrNull() ?: return null
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

        val targetFile = File(updateDir, "300 Lite.apk")
        val tempFile = File(updateDir, "${targetFile.name}.download")
        tempFile.delete()

        val request = Request.Builder()
            .url(info.apkUrl)
            .header("Accept", APK_MIME_TYPE)
            .header("User-Agent", "YamiboReaderLite/${BuildConfig.VERSION_NAME}")
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
