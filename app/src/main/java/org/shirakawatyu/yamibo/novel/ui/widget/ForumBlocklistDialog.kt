package org.shirakawatyu.yamibo.novel.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.shirakawatyu.yamibo.novel.util.forum.ForumBlockedItem
import org.shirakawatyu.yamibo.novel.util.forum.ForumBlocklistManager

@Composable
fun ForumBlocklistDialog(onDismiss: () -> Unit) {
    val blockedItems by ForumBlocklistManager.items.collectAsState()
    var input by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var inputError by remember { mutableStateOf(false) }

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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            inputError = false
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = inputError,
                        label = { Text("帖子/楼层链接或 ID") }
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val item = ForumBlocklistManager.parseInput(input)
                            if (item == null) {
                                inputError = true
                            } else {
                                ForumBlocklistManager.add(item.type, item.id, item.title)
                                input = ""
                            }
                        }
                    ) {
                        Text("添加")
                    }
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("搜索标题或 ID") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterButton("全部", filter == "all") { filter = "all" }
                    FilterButton("主题", filter == ForumBlockedItem.TYPE_THREAD) {
                        filter = ForumBlockedItem.TYPE_THREAD
                    }
                    FilterButton("楼层", filter == ForumBlockedItem.TYPE_POST) {
                        filter = ForumBlockedItem.TYPE_POST
                    }
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
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        fontSize = 13.sp
    )
}
