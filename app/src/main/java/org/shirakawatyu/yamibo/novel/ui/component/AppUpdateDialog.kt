package org.shirakawatyu.yamibo.novel.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import org.shirakawatyu.yamibo.novel.util.AppUpdateInfo

@Composable
fun AppUpdateDialog(
    info: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("发现新版本 ${info.versionName}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = info.releaseNotes.ifBlank { "新版本已经发布。" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching { uriHandler.openUri(info.releaseUrl) }
                    onDismiss()
                }
            ) {
                Text("前往更新")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}
