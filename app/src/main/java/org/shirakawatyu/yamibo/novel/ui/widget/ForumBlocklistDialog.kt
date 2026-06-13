package org.shirakawatyu.yamibo.novel.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.shirakawatyu.yamibo.novel.util.forum.ForumBlockedItem
import org.shirakawatyu.yamibo.novel.util.forum.ForumBlocklistManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumBlocklistDialog(onDismiss: () -> Unit) {
    val blockedItems by ForumBlocklistManager.items.collectAsState()
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }

    val visibleItems = remember(blockedItems, search, filter) {
        val keyword = search.trim()
        blockedItems.filter { item ->
            (filter == "all" || item.type == filter) &&
                    (keyword.isBlank() ||
                            item.id.contains(keyword, ignoreCase = true) ||
                            item.title.contains(keyword, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("黑名单", fontSize = 18.sp) },
        text = {
            // 搜索框与筛选按钮共用同一高度，保证视觉对齐。
            val controlHeight = 42.dp
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 紧凑搜索框：用 DecorationBox 自定义更小的内边距，整体比默认输入框矮一截。
                val searchInteraction = remember { MutableInteractionSource() }
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(controlHeight),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    interactionSource = searchInteraction,
                    decorationBox = { innerTextField ->
                        OutlinedTextFieldDefaults.DecorationBox(
                            value = search,
                            innerTextField = innerTextField,
                            enabled = true,
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = searchInteraction,
                            placeholder = {
                                Text(
                                    "搜索标题或 ID",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                )

                // 全部 / 主题 / 楼层：等宽按钮，平铺占满整行，高度与搜索框一致。
                // 未选中无背景，选中项变蓝色。
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterButton(
                        "全部",
                        filter == "all",
                        Modifier.weight(1f).height(controlHeight)
                    ) { filter = "all" }
                    FilterButton(
                        "主题",
                        filter == ForumBlockedItem.TYPE_THREAD,
                        Modifier.weight(1f).height(controlHeight)
                    ) { filter = ForumBlockedItem.TYPE_THREAD }
                    FilterButton(
                        "楼层",
                        filter == ForumBlockedItem.TYPE_POST,
                        Modifier.weight(1f).height(controlHeight)
                    ) { filter = ForumBlockedItem.TYPE_POST }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                ) {
                    if (visibleItems.isEmpty()) {
                        item {
                            Text(
                                text = if (search.isBlank()) "暂无屏蔽项" else "未找到匹配项",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(
                            items = visibleItems,
                            key = { "${it.type}:${it.id}" }
                        ) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (item.type == ForumBlockedItem.TYPE_THREAD) "主题" else "楼层",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.ifBlank { item.id },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ID ${item.id}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "移除",
                                    modifier = Modifier
                                        .clickable {
                                            ForumBlocklistManager.remove(item.type, item.id)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {
            if (blockedItems.isNotEmpty()) {
                TextButton(onClick = ForumBlocklistManager::clear) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
private fun FilterButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // 选中项的主题色底色在三个按钮之间切换；未选中无底色。
    // 选中时文字用 onPrimary（与“清理缓存”等填充按钮一致），保证在主题色底色上清晰可读；
    // 未选中时为普通文字色。
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}
