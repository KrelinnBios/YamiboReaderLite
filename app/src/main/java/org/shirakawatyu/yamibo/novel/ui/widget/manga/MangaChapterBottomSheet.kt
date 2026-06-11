package org.shirakawatyu.yamibo.novel.ui.widget.manga

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.shirakawatyu.yamibo.novel.bean.MangaSettings

data class MangaChapter(
    val index: Float,
    val title: String,
    val url: String,
    val isRead: Boolean = false,
    val isNew: Boolean = false,
    val isCurrent: Boolean = false
)

@Composable
fun MangaChapterPanel(
    modifier: Modifier = Modifier,
    title: String,
    initialAuthor: String,
    initialTranslationGroup: String,
    chapters: List<MangaChapter>,
    isUpdating: Boolean = false,
    onDismiss: () -> Unit,
    onChapterClick: (MangaChapter) -> Unit,
    onTitleEdit: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var visible by remember { mutableStateOf(false) }
    var ascending by remember { mutableStateOf(MangaSettings.getSettings(context).isAscending) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitleText by remember(title) { mutableStateOf(title) }
    var editKeyword1 by remember(initialAuthor) { mutableStateOf(initialAuthor) }
    var editKeyword2 by remember { mutableStateOf("") }
    var editTranslationGroup by remember(initialTranslationGroup) {
        mutableStateOf(initialTranslationGroup)
    }
    var showSecondKeyword by remember { mutableStateOf(false) }
    var isTitleExpanded by remember { mutableStateOf(false) }

    val sorted = remember(chapters, ascending) {
        if (ascending) chapters else chapters.reversed()
    }
    fun dismiss() {
        scope.launch {
            visible = false
            delay(240)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }
    LaunchedEffect(sorted, visible) {
        if (!visible) return@LaunchedEffect
        val currentIndex = sorted.indexOfFirst { it.isCurrent }
        if (currentIndex >= 0) {
            listState.scrollToItem((currentIndex - 3).coerceAtLeast(0))
        }
    }
    BackHandler(enabled = visible) {
        dismiss()
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("更新漫画信息") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "确保书名不包含单章编号等信息",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editKeyword1,
                            onValueChange = { editKeyword1 = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            label = { Text("关键词 1") }
                        )
                        if (showSecondKeyword) {
                            OutlinedTextField(
                                value = editKeyword2,
                                onValueChange = { editKeyword2 = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                label = { Text("关键词 2") }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { showSecondKeyword = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editTitleText,
                        onValueChange = { editTitleText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("漫画名称") },
                        minLines = 1,
                        maxLines = 2
                    )
                    OutlinedTextField(
                        value = editTranslationGroup,
                        onValueChange = { editTranslationGroup = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("汉化组（留空显示全部）") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitleText.isNotBlank()) {
                            val keywords = listOf(editKeyword1.trim(), editKeyword2.trim())
                                .filter(String::isNotEmpty)
                                .joinToString(" ")
                            onTitleEdit(
                                editTitleText.trim(),
                                keywords,
                                editTranslationGroup.trim()
                            )
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("保存并更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { dismiss() }
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(260)) { -it },
            exit = slideOutHorizontally(tween(220)) { -it },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize()
                                .clickable { isTitleExpanded = !isTitleExpanded },
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = if (isTitleExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { dismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭目录")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                ascending = !ascending
                                MangaSettings.saveIsAscending(context, ascending)
                            }
                        ) {
                            Text(if (ascending) "正序" else "倒序")
                        }
                        TextButton(
                            onClick = {
                                editTitleText = title
                                showEditDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("更新")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (isUpdating) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(
                            items = sorted,
                            key = { _, chapter -> chapter.url }
                        ) { listIndex, chapter ->
                            val fallbackNumber =
                                if (ascending) listIndex + 1 else sorted.size - listIndex
                            MangaChapterDrawerItem(
                                chapter = chapter,
                                chapterNumber = formatChapterNumber(chapter.index, fallbackNumber),
                                onClick = { onChapterClick(chapter) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaChapterDrawerItem(
    chapter: MangaChapter,
    chapterNumber: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        selected = chapter.isCurrent,
        onClick = {
            if (!chapter.isCurrent) onClick()
        },
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chapterNumber,
                    modifier = Modifier.width(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = chapter.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (chapter.isRead && !chapter.isCurrent) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        badge = {
            when {
                chapter.isNew -> Text(
                    "NEW",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                chapter.isCurrent -> Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

private fun formatChapterNumber(index: Float, fallbackNumber: Int): String {
    if (!index.isFinite() || index <= 0f) return fallbackNumber.toString()
    val value = if (index % 1f == 0f) {
        index.toInt().toString()
    } else {
        index.toString().trimEnd('0').trimEnd('.')
    }
    return value
}
