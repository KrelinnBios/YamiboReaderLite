package org.shirakawatyu.yamibo.novel.ui.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.shirakawatyu.yamibo.novel.global.GlobalData
import org.shirakawatyu.yamibo.novel.util.DarkThemeColors
import org.shirakawatyu.yamibo.novel.util.YamiboPostLinkUtil
import org.shirakawatyu.yamibo.novel.util.forum.ForumBlockedItem
import org.shirakawatyu.yamibo.novel.util.forum.ForumBlocklistManager

/** 由屏蔽项构造可跳转的原帖链接（统一经 YamiboPostLinkUtil 归一化为 bbs + mobile=2）。 */
private fun blockedItemPostUrl(item: ForumBlockedItem): String? {
    if (item.type == ForumBlockedItem.TYPE_USER) {
        return "https://bbs.yamibo.com/home.php?mod=space&uid=${item.id}&do=profile&mobile=2"
    }
    val raw = when (item.type) {
        ForumBlockedItem.TYPE_THREAD ->
            "https://bbs.yamibo.com/forum.php?mod=viewthread&tid=${item.id}"

        ForumBlockedItem.TYPE_POST ->
            "https://bbs.yamibo.com/forum.php?mod=redirect&goto=findpost&pid=${item.id}"

        else -> return null
    }
    return YamiboPostLinkUtil.normalizePostUrl(raw)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumBlocklistDialog(
    onDismiss: () -> Unit,
    onOpenPost: (url: String) -> Unit = {}
) {
    val blockedItems by ForumBlocklistManager.items.collectAsState()
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }

    val visibleItems = remember(blockedItems, search, filter) {
        val keyword = search.trim()
        blockedItems.filter { item ->
            (filter == "all" || item.type == filter) &&
                    (keyword.isBlank() ||
                            item.id.contains(keyword, ignoreCase = true) ||
                            item.title.contains(keyword, ignoreCase = true) ||
                            item.authorName.contains(keyword, ignoreCase = true) ||
                            item.authorUid.contains(keyword, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("黑名单", fontSize = 18.sp) },
        text = {
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
                                    "输入帖子标题、用户名或用户 ID",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                )

                // 全部 / 主题 / 楼层 / 用户：与漫画首页一致的胶囊分段选择器。
                SectionSegmentedTabs(
                    selected = filter,
                    onSelect = { filter = it },
                    height = controlHeight
                )

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
                            val authorLine = when {
                                item.type == ForumBlockedItem.TYPE_USER -> "UID ${item.id}"
                                item.authorName.isNotBlank() && item.authorUid.isNotBlank() ->
                                    "${item.authorName}（UID ${item.authorUid}）"
                                item.authorName.isNotBlank() -> item.authorName
                                item.authorUid.isNotBlank() -> "UID ${item.authorUid}"
                                else -> "ID ${item.id}"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        blockedItemPostUrl(item)?.let(onOpenPost)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (item.type) {
                                        ForumBlockedItem.TYPE_THREAD -> "主题"
                                        ForumBlockedItem.TYPE_POST -> "楼层"
                                        ForumBlockedItem.TYPE_USER -> "用户"
                                        else -> ""
                                    },
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
                                        text = authorLine,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

/** 全部 / 主题 / 楼层 / 用户胶囊分段选择器，配色与漫画首页版区切换一致。 */
@Composable
private fun SectionSegmentedTabs(
    selected: String,
    onSelect: (String) -> Unit,
    height: androidx.compose.ui.unit.Dp
) {
    val isDarkMode by GlobalData.isDarkMode.collectAsState()
    val classic = DarkThemeColors.CLASSIC

    val containerColor =
        if (isDarkMode) classic.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
    val outlineColor =
        if (isDarkMode) classic.outline.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val selectedColor =
        if (isDarkMode) classic.primary.copy(alpha = 0.32f) else MaterialTheme.colorScheme.surface
    val selectedContentColor =
        if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
    val unselectedContentColor =
        if (isDarkMode) classic.onSurfaceVariant
        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
    val selectedBorderColor =
        if (isDarkMode) classic.primary.copy(alpha = 0.95f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

    val tabs = listOf(
        "全部" to "all",
        "主题" to ForumBlockedItem.TYPE_THREAD,
        "楼层" to ForumBlockedItem.TYPE_POST,
        "用户" to ForumBlockedItem.TYPE_USER
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .border(1.dp, outlineColor, RoundedCornerShape(999.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (label, value) ->
            val isSelected = selected == value
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(value) },
                color = if (isSelected) selectedColor else Color.Transparent,
                contentColor = if (isSelected) selectedContentColor else unselectedContentColor,
                border = if (isSelected) BorderStroke(1.dp, selectedBorderColor) else null,
                shape = RoundedCornerShape(999.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
