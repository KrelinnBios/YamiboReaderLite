package org.shirakawatyu.yamibo.novel.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.shirakawatyu.yamibo.novel.util.AppUpdateInfo
import org.shirakawatyu.yamibo.novel.util.AppUpdateManager

private enum class UpdateDownloadState {
    READY,
    DOWNLOADING,
    INSTALLER_OPENED,
    FAILED
}

@Composable
fun AppUpdateDialog(
    info: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(info.versionName) { mutableStateOf(UpdateDownloadState.READY) }
    var failureMessage by remember(info.versionName) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            if (state != UpdateDownloadState.DOWNLOADING) onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("发现新版本 ${info.versionName}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state) {
                    UpdateDownloadState.DOWNLOADING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text("正在从 GitHub 下载并校验 APK…")
                        }
                    }

                    UpdateDownloadState.INSTALLER_OPENED -> {
                        Text("系统安装器已打开。若安装失败，可返回此处选择手动下载。")
                    }

                    UpdateDownloadState.FAILED -> {
                        Text(
                            text = "自动更新失败：$failureMessage\n\n请使用“手动下载”前往 GitHub Releases。",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    UpdateDownloadState.READY -> {
                        Text(
                            text = buildString {
                                appendLine("目标版本：v${info.versionName}")
                                appendLine()
                                append(info.releaseNotes.ifBlank { "新版本已经发布。" })
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = state != UpdateDownloadState.DOWNLOADING,
                onClick = {
                    scope.launch {
                        state = UpdateDownloadState.DOWNLOADING
                        failureMessage = ""
                        AppUpdateManager.downloadAndOpenInstaller(context, info)
                            .onSuccess {
                                state = UpdateDownloadState.INSTALLER_OPENED
                            }
                            .onFailure { error ->
                                failureMessage = error.message ?: "未知错误"
                                state = UpdateDownloadState.FAILED
                            }
                    }
                }
            ) {
                Text(
                    when (state) {
                        UpdateDownloadState.DOWNLOADING -> "下载中"
                        UpdateDownloadState.INSTALLER_OPENED -> "重新打开安装器"
                        UpdateDownloadState.FAILED -> "重试自动更新"
                        UpdateDownloadState.READY -> "下载并安装"
                    }
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = state != UpdateDownloadState.DOWNLOADING,
                    onClick = {
                        AppUpdateManager.openReleasesPage(context)
                        onDismiss()
                    }
                ) {
                    Text("手动下载")
                }
                TextButton(
                    enabled = state != UpdateDownloadState.DOWNLOADING,
                    onClick = onDismiss
                ) {
                    Text("稍后")
                }
            }
        }
    )
}

@Composable
fun AppUpdateFailureDialog(
    reason: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("自动检查更新失败") },
        text = {
            Text("$reason\n\n可以前往 GitHub Releases 手动检查并下载最新版本。")
        },
        confirmButton = {
            Button(
                onClick = {
                    AppUpdateManager.openReleasesPage(context)
                    onDismiss()
                }
            ) {
                Text("打开下载页")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
