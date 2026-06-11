package org.shirakawatyu.yamibo.novel.ui.page

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.BorderStroke
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.shirakawatyu.yamibo.novel.bean.MangaHomeItem
import org.shirakawatyu.yamibo.novel.global.GlobalData
import org.shirakawatyu.yamibo.novel.ui.vm.BottomNavBarVM
import org.shirakawatyu.yamibo.novel.ui.vm.MangaHomeVM
import org.shirakawatyu.yamibo.novel.util.DarkThemeColors
import org.shirakawatyu.yamibo.novel.util.manga.MangaProber
import java.net.URLEncoder

@Composable
fun MangaHomePage(
    navController: NavController,
    mangaHomeVM: MangaHomeVM = viewModel()
) {
    val state by mangaHomeVM.uiState.collectAsState()
    val isDarkMode by GlobalData.isDarkMode.collectAsState()
    val context = LocalContext.current
    val bottomNavBarVM: BottomNavBarVM =
        viewModel(viewModelStoreOwner = context as ComponentActivity)
    val listState = rememberLazyListState()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()
    var openingTid by remember { mutableStateOf<String?>(null) }
    val classicDarkColors = DarkThemeColors.CLASSIC
    val headerContainerColor =
        if (isDarkMode) classicDarkColors.statusBar else MaterialTheme.colorScheme.primary
    val headerContentColor =
        if (isDarkMode) classicDarkColors.onPrimary else MaterialTheme.colorScheme.onPrimary
    val sectionContainerColor =
        if (isDarkMode) classicDarkColors.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
    val sectionOutlineColor =
        if (isDarkMode) classicDarkColors.outline.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val selectedSectionColor =
        if (isDarkMode) classicDarkColors.surface else MaterialTheme.colorScheme.surface
    val selectedSectionContentColor =
        if (isDarkMode) classicDarkColors.onSurface else MaterialTheme.colorScheme.onSurface
    val unselectedSectionContentColor =
        if (isDarkMode) classicDarkColors.onSurfaceVariant
        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
    val selectedSectionBorderColor =
        if (isDarkMode) classicDarkColors.primary.copy(alpha = 0.95f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)

    LaunchedEffect(state.isLoading, state.isLoadingMore) {
        if (!state.isLoading && !state.isLoadingMore) {
            bottomNavBarVM.finishRefresh("MangaHomePage")
        }
    }
    LaunchedEffect(bottomNavBarVM) {
        launch {
            bottomNavBarVM.refreshEvent.collect { route ->
                if (route == "MangaHomePage") mangaHomeVM.refresh()
            }
        }
        launch {
            bottomNavBarVM.goHomeEvent.collect { route ->
                if (route == "MangaHomePage") {
                    mangaHomeVM.clearSearch()
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = navBottom + 50.dp)
    ) {
        Surface(
            color = headerContainerColor,
            contentColor = headerContentColor
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .width(220.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(sectionContainerColor)
                        .border(
                            width = 1.dp,
                            color = sectionOutlineColor,
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(3.dp)
                        .align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MangaHomeVM.sections.forEach { (fid, label) ->
                        val selected = state.selectedFid == fid
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { mangaHomeVM.setSection(fid) },
                            color = if (selected) selectedSectionColor
                            else Color.Transparent,
                            contentColor = if (selected) selectedSectionContentColor
                            else unselectedSectionContentColor,
                            border = if (selected) {
                                BorderStroke(
                                    width = 1.dp,
                                    color = selectedSectionBorderColor
                                )
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = mangaHomeVM::updateQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(52.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = {
                        Text(
                            text = "搜索当前版区漫画",
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotBlank()) {
                            IconButton(
                                onClick = mangaHomeVM::clearSearch,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "清空搜索",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { mangaHomeVM.submitSearch() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null && state.items.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = mangaHomeVM::refresh) { Text("重试") }
                    }
                }
                state.items.isEmpty() -> {
                    Text(
                        text = if (state.query.isBlank()) "暂无漫画" else "没有精确匹配的漫画",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        itemsIndexed(
                            items = state.items,
                            key = { _, item -> item.tid }
                        ) { index, item ->
                            MangaHomeRow(
                                item = item,
                                alternate = index % 2 == 1,
                                isOpening = openingTid == item.tid,
                                onClick = {
                                    if (openingTid == null) {
                                        openingTid = item.tid
                                        scope.launch {
                                            MangaProber().probeUrl(
                                                context = context,
                                                url = item.url,
                                                forceRefresh = true,
                                                onSuccess = { urls, title, html ->
                                                    val normalizedUrls = urls
                                                        .map(String::trim)
                                                        .filter(String::isNotBlank)
                                                        .distinct()
                                                    GlobalData.tempMangaUrls = normalizedUrls
                                                    GlobalData.tempMangaIndex = 0
                                                    GlobalData.tempHtml = html
                                                    GlobalData.tempTitle = title
                                                    val encoded =
                                                        URLEncoder.encode(item.url, "utf-8")
                                                    navController.navigate(
                                                        "NativeMangaPage?url=$encoded&originalUrl=$encoded"
                                                    )
                                                    openingTid = null
                                                },
                                                onFallback = {
                                                    val encoded =
                                                        URLEncoder.encode(item.url, "utf-8")
                                                    navController.navigate(
                                                        "MangaWebPage/$encoded/$encoded?fastForward=false&initialPage=0"
                                                    )
                                                    openingTid = null
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                            if (index >= state.items.lastIndex - 4) {
                                LaunchedEffect(state.page, state.items.size) {
                                    mangaHomeVM.loadMore()
                                }
                            }
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaHomeRow(
    item: MangaHomeItem,
    alternate: Boolean,
    isOpening: Boolean,
    onClick: () -> Unit
) {
    val rowColor = if (alternate) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .width(70.dp)
                .height(94.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (item.coverUrl != null) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = listOf(item.authorName, item.date)
                    .filter(String::isNotBlank)
                    .joinToString("  "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isOpening) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
