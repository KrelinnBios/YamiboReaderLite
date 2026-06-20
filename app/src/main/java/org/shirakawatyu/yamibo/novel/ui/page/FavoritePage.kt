package org.shirakawatyu.yamibo.novel.ui.page

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.shirakawatyu.yamibo.novel.R
import org.shirakawatyu.yamibo.novel.bean.Favorite
import org.shirakawatyu.yamibo.novel.bean.MangaUpdateCheckStrategy
import org.shirakawatyu.yamibo.novel.global.GlobalData
import org.shirakawatyu.yamibo.novel.item.FavoriteItem
import org.shirakawatyu.yamibo.novel.ui.theme.YamiboColors
import org.shirakawatyu.yamibo.novel.ui.theme.YellowLightLight
import org.shirakawatyu.yamibo.novel.ui.vm.BottomNavBarVM
import org.shirakawatyu.yamibo.novel.ui.vm.FavoriteVM
import org.shirakawatyu.yamibo.novel.ui.vm.ViewModelFactory
import org.shirakawatyu.yamibo.novel.ui.widget.TopBar
import org.shirakawatyu.yamibo.novel.ui.widget.YamiboToast
import org.shirakawatyu.yamibo.novel.ui.widget.favorite.AutoCheckSection
import org.shirakawatyu.yamibo.novel.ui.widget.favorite.FavoriteManageDoneButton
import org.shirakawatyu.yamibo.novel.ui.widget.favorite.FavoriteTopSearchField
import org.shirakawatyu.yamibo.novel.util.darkModeColor
import org.shirakawatyu.yamibo.novel.util.darkThemeColor
import org.shirakawatyu.yamibo.novel.util.manga.MangaImagePipeline
import org.shirakawatyu.yamibo.novel.util.manga.MangaProber
import org.shirakawatyu.yamibo.novel.util.manga.MangaTitleCleaner
import org.shirakawatyu.yamibo.novel.util.updateCheck.AutoUpdateCheckScheduler


// 非搜索场景下，确认"暂无收藏"前的等待时间。
// 调大以覆盖冷启动时 DataStore/Flow 首次发射前的空窗，避免闪现"暂无收藏"。
private const val EMPTY_STATE_CONFIRM_DELAY_MS = 1500L

/**
 * 收藏页面，展示用户的收藏列表，支持刷新和拖拽排序。
 *
 * @param favoriteVM 用于管理收藏数据的 ViewModel，默认通过 viewModel() 获取实例。
 * @param navController 导航控制器，用于跳转到其他页面。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FavoritePage(
    favoriteVM: FavoriteVM = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext)),
    navController: NavController
) {
    val uiState by favoriteVM.uiState.collectAsState()
    val favoriteList = uiState.favoriteList
    val isRefreshing = uiState.isRefreshing
    val isInManageMode = uiState.isInManageMode
    val selectedItems = uiState.selectedItems
    val updateCheckNovels = uiState.updateCheckNovels
    val updateCheckMangas = uiState.updateCheckMangas
    val updateCheckOthers = uiState.updateCheckOthers
    val novelCheckMap = remember(updateCheckNovels) { updateCheckNovels.associateBy { it.url } }
    val mangaCheckMap = remember(updateCheckMangas) { updateCheckMangas.associateBy { it.url } }
    val otherCheckMap = remember(updateCheckOthers) { updateCheckOthers.associateBy { it.url } }
    val autoEnabledCount = remember(updateCheckNovels, updateCheckMangas, updateCheckOthers) {
        updateCheckNovels.count { it.autoCheckEnabled } +
                updateCheckMangas.count { it.autoCheckEnabled } +
                updateCheckOthers.count { it.autoCheckEnabled }
    }
    var cacheInfoMap = uiState.cacheInfoMap
    LaunchedEffect(Unit) { favoriteVM.refreshCacheInfo() }
    DisposableEffect(Unit) {
        favoriteVM.isFavoritePageVisible = true
        onDispose {
            favoriteVM.isFavoritePageVisible = false
        }
    }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemActionTarget by remember { mutableStateOf<Favorite?>(null) }
    var singleDeleteTarget by remember { mutableStateOf<Favorite?>(null) }
    var mangaUpdateCheckTarget by remember { mutableStateOf<Favorite?>(null) }
    var showMangaConfigDialog by remember { mutableStateOf(false) }
    var mangaConfigPresetStrategy by remember { mutableStateOf(MangaUpdateCheckStrategy.TAG) }
    var mangaConfigPresetKeyword by remember { mutableStateOf("") }
    var mangaConfigPresetBookName by remember { mutableStateOf("") }
    var mangaConfigTagAvailable by remember { mutableStateOf(false) }
    var novelUpdateCheckTarget by remember { mutableStateOf<Favorite?>(null) }
    var showNovelConfigDialog by remember { mutableStateOf(false) }
    var novelConfigAutoCheck by remember { mutableStateOf(false) }
    var novelConfigInterval by remember { mutableIntStateOf(12) }
    var otherUpdateCheckTarget by remember { mutableStateOf<Favorite?>(null) }
    var showOtherConfigDialog by remember { mutableStateOf(false) }
    var otherConfigAutoCheck by remember { mutableStateOf(false) }
    var otherConfigInterval by remember { mutableIntStateOf(12) }
    val openMangaConfig: (Favorite) -> Unit = { fav ->
        mangaUpdateCheckTarget = fav
        favoriteVM.getDirectoryList { dirs ->
            val t = Regex("tid=(\\d+)").find(fav.url)?.groupValues?.get(1)
            val existing = mangaCheckMap[fav.url]
            val matchedDir = if (existing != null) {
                dirs.find { it.cleanBookName == existing.cleanBookName }
                    ?: dirs.find { dir -> dir.chapters.any { it.tid == t } }
            } else {
                dirs.find { dir -> dir.chapters.any { it.tid == t } }
            }

            val tagAvailable =
                matchedDir?.strategy == org.shirakawatyu.yamibo.novel.bean.DirectoryStrategy.TAG
            mangaConfigTagAvailable = tagAvailable

            mangaConfigPresetStrategy = when {
                !tagAvailable -> MangaUpdateCheckStrategy.SEARCH
                existing != null -> existing.strategy
                else -> MangaUpdateCheckStrategy.TAG
            }
            val derivedKeyword = matchedDir?.chapters?.find { it.tid == t }
                ?.let { MangaTitleCleaner.extractAuthorPrefix(it.rawTitle) }
                ?: matchedDir?.chapters?.lastOrNull()
                    ?.let { MangaTitleCleaner.extractAuthorPrefix(it.rawTitle) } ?: ""
            mangaConfigPresetKeyword = existing?.searchKeyword ?: matchedDir?.searchKeyword ?: derivedKeyword
            mangaConfigPresetBookName = existing?.cleanBookName ?: matchedDir?.cleanBookName ?: ""
            showMangaConfigDialog = true
        }
    }
    val context = LocalContext.current
    val bottomNavBarVM: BottomNavBarVM =
        viewModel(viewModelStoreOwner = context as ComponentActivity)
    var probingUrl by remember { mutableStateOf<String?>(null) }
    var probingJob by remember { mutableStateOf<Job?>(null) }
    BackHandler(enabled = probingUrl != null) {
        probingJob?.cancel()
        probingJob = null
        probingUrl = null
    }
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    var previousListSize by remember { mutableIntStateOf(favoriteList.size) }
    var wasAtTop by remember { mutableStateOf(true) }
    var keepTopAfterUnpin by remember { mutableStateOf(false) }
    var showTopToast by remember { mutableStateOf(false) }
    var newItemsCount by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val openMangaFavorite: (Favorite) -> Unit =
        remember(context, navController, coroutineScope) {
            { favorite ->
                val targetUrl = favorite.lastMangaUrl
                    ?.takeIf { MangaTitleCleaner.extractTidFromUrl(it) != null }
                    ?: favorite.url
                val encodedTarget = java.net.URLEncoder.encode(targetUrl, "utf-8")
                val encodedOriginal = java.net.URLEncoder.encode(favorite.url, "utf-8")
                probingUrl = targetUrl
                probingJob = coroutineScope.launch {
                    var navigationHandled = false
                    val openFallback = {
                        if (!navigationHandled) {
                            navigationHandled = true
                            navController.navigate(
                                "MangaWebPage/$encodedTarget/$encodedOriginal" +
                                        "?fastForward=false&initialPage=${favorite.lastPage}"
                            )
                            probingUrl = null
                            probingJob = null
                        }
                    }

                    try {
                        MangaProber().probeUrl(
                            context = context,
                            url = targetUrl,
                            forceRefresh = true,
                            onSuccess = probeSuccess@{ urls, title, html ->
                                if (navigationHandled) return@probeSuccess
                                val normalizedUrls = urls
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                if (normalizedUrls.isEmpty()) {
                                    openFallback()
                                    return@probeSuccess
                                }

                                navigationHandled = true
                                val targetIndex = favorite.lastPage.coerceIn(
                                    0,
                                    normalizedUrls.lastIndex
                                )

                                MangaImagePipeline.handoffPrefetch(
                                    context = context.applicationContext,
                                    urls = normalizedUrls,
                                    clickedIndex = targetIndex
                                )

                                GlobalData.tempMangaUrls = normalizedUrls
                                GlobalData.tempHtml = html
                                GlobalData.tempTitle = title
                                GlobalData.tempMangaIndex = targetIndex

                                navController.navigate(
                                    "NativeMangaPage?url=$encodedTarget&originalUrl=$encodedOriginal"
                                )
                                probingUrl = null
                                probingJob = null
                            },
                            onFallback = openFallback
                        )
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Throwable) {
                        openFallback()
                    } finally {
                        if (!navigationHandled) {
                            probingUrl = null
                            probingJob = null
                        }
                    }
                }
            }
        }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                favoriteVM.lastPauseTime = System.currentTimeMillis()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                val isQuickReturn = favoriteVM.lastPauseTime != 0L &&
                        (System.currentTimeMillis() - favoriteVM.lastPauseTime < 2400L)
                // 进入前台：尝试跑一轮自动更新检查（调度器自带最小间隔 + 错峰 + 登录守卫）
                AutoUpdateCheckScheduler.onAppForeground(context.applicationContext)

                coroutineScope.launch {
                    delay(350)

                    if (!isQuickReturn) {
                        when (favoriteVM.getEffectiveResumeStrategy()) {
                            FavoriteVM.RefreshStrategy.SKIP -> {
                            }

                            FavoriteVM.RefreshStrategy.SMART -> {
                                favoriteVM.refreshList(showLoading = false, isSmartSync = true)
                            }

                            FavoriteVM.RefreshStrategy.FULL -> {
                                favoriteVM.refreshList(showLoading = false, isSmartSync = false)
                            }
                        }
                    }

                    favoriteVM.nextResumeStrategy = FavoriteVM.RefreshStrategy.FULL

                    if (!isQuickReturn) {
                        favoriteVM.getCacheInfo { info -> cacheInfoMap = info }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    LaunchedEffect(lazyListState) {
        androidx.compose.runtime.snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            wasAtTop = (index == 0 && offset <= 50)
        }
    }

    var previousCategory by remember { mutableIntStateOf(favoriteVM.currentCategory) }
    var previousManageMode by remember { mutableStateOf(uiState.isInManageMode) }


    LaunchedEffect(favoriteList, favoriteVM.currentCategory, uiState.isInManageMode) {
        if (keepTopAfterUnpin) {
            lazyListState.scrollToItem(0)
            keepTopAfterUnpin = false
        }

        val addedCount = favoriteList.size - previousListSize

        val isSameCategory = favoriteVM.currentCategory == previousCategory
        val isSameManageMode = uiState.isInManageMode == previousManageMode
        val isNotInitialLoad = previousListSize > 0

        if (isSameCategory && isSameManageMode && isNotInitialLoad && addedCount > 0) {
            if (wasAtTop) {
                lazyListState.animateScrollToItem(0)
            } else {
                newItemsCount = addedCount
                showTopToast = true
            }
        }
        previousListSize = favoriteList.size
        previousCategory = favoriteVM.currentCategory
        previousManageMode = uiState.isInManageMode
    }
    LaunchedEffect(showTopToast) {
        if (showTopToast) {
            delay(2500)
            showTopToast = false
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchBarExpanded by rememberSaveable { mutableStateOf(false) }
    val isSearching = searchQuery.isNotBlank()

    LaunchedEffect(bottomNavBarVM, lazyListState) {
        bottomNavBarVM.goHomeEvent.collect { route ->
            if (route == "FavoritePage") {
                searchQuery = ""
                isSearchBarExpanded = false
                lazyListState.animateScrollToItem(0)
            }
        }
    }

    BackHandler(enabled = isSearchBarExpanded && probingUrl == null) {
        searchQuery = ""
        isSearchBarExpanded = false
    }

    // 分类数据
    val novelCategoryColor = darkModeColor(
        light = YamiboColors.secondary,
        dark = Color(0xFF3FC1B0)
    )
    val categoryOptions = listOf(
        Triple(-1, "全部", Color.Transparent),
        Triple(1, "小说", novelCategoryColor),
        Triple(2, "漫画", MaterialTheme.colorScheme.primary)
    )

    var currentCategoryId by rememberSaveable { mutableIntStateOf(favoriteVM.currentCategory) }
    val currentCat = categoryOptions.find { it.first == currentCategoryId } ?: categoryOptions[0]

    val searchTerms = remember(searchQuery) {
        searchQuery
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
    }

    val searchedFavoriteList = remember(favoriteList, searchTerms) {
        if (searchTerms.isEmpty()) {
            favoriteList
        } else {
            favoriteList.filter { fav ->
                val cleanTitle = fav.title
                    .replace(Regex("^(?:【.*?】|\\[.*?\\]|\\s)+"), "")
                    .ifBlank { fav.title }

                // 只搜索收藏标题，避免数字、链接、作者 ID、章节名等隐藏/辅助字段造成误命中。
                searchTerms.all { term ->
                    fav.title.contains(term, ignoreCase = true) ||
                            cleanTitle.contains(term, ignoreCase = true)
                }
            }
        }
    }

    var shouldShowEmptyState by remember { mutableStateOf(false) }

    LaunchedEffect(
        searchedFavoriteList.isEmpty(),
        isRefreshing,
        isSearching,
        favoriteList.size,
        currentCategoryId
    ) {
        shouldShowEmptyState = false
        if (searchedFavoriteList.isNotEmpty() || isRefreshing) return@LaunchedEffect

        if (isSearching) {
            // 搜索结果为空应该及时反馈；非搜索的“暂无收藏”则延迟确认，
            // 避免进入页面时 DataStore / Flow 初始空列表造成一瞬间闪屏。
            shouldShowEmptyState = true
        } else {
            delay(EMPTY_STATE_CONFIRM_DELAY_MS)
            shouldShowEmptyState = true
        }
    }

    LaunchedEffect(searchQuery, currentCategoryId) {
        if (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0) {
            lazyListState.scrollToItem(0)
        }
    }

    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var lockedNavHeightValue by rememberSaveable { mutableFloatStateOf(0f) }
    if (navBarsPadding.value > lockedNavHeightValue) lockedNavHeightValue = navBarsPadding.value
    val lockedNavHeight = lockedNavHeightValue.dp

    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var lockedStatusHeightValue by rememberSaveable { mutableFloatStateOf(0f) }
    if (statusBarsPadding.value > lockedStatusHeightValue) lockedStatusHeightValue =
        statusBarsPadding.value
    val lockedStatusHeight = lockedStatusHeightValue.dp
    val topBarContentColor = darkThemeColor(Color.Black) { onPrimary }
    LaunchedEffect(Unit) {
        favoriteVM.refreshCacheInfo()
    }

    @Composable
    fun MoreOptionsButton() {
        val enabled = favoriteList.isNotEmpty()
        IconButton(
            onClick = { if (enabled) favoriteVM.toggleManageMode() },
            enabled = enabled,
            modifier = Modifier
                .size(44.dp)
                .padding(end = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Checklist,
                contentDescription = "管理收藏",
                modifier = Modifier.size(23.dp),
                tint = if (enabled) topBarContentColor
                else topBarContentColor.copy(alpha = 0.38f)
            )
        }
    }

    Column(
        modifier = Modifier
            .padding(bottom = lockedNavHeight + 50.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (lockedStatusHeight > 0.dp) lockedStatusHeight else 28.dp)
                .background(darkThemeColor(YamiboColors.onSurface) { statusBar })
        )
        TopBar(title = "") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchBarExpanded) {
                    FavoriteTopSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        onClose = {
                            searchQuery = ""
                            isSearchBarExpanded = false
                        },
                        resultText = if (isSearching) "${searchedFavoriteList.size}项" else null,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp, end = if (isInManageMode) 6.dp else 2.dp)
                    )
                    if (isInManageMode) {
                        FavoriteManageDoneButton(
                            onClick = { favoriteVM.toggleManageMode() }
                        )
                    } else {
                        MoreOptionsButton()
                    }
                } else {
                    // 左半部：标题或分类切换
                    if (isInManageMode) {
                        Text(
                            text = "管理收藏 (${selectedItems.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = topBarContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f)
                        )
                    } else {
                        var categoryMenuExpanded by remember { mutableStateOf(false) }
                        val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (categoryMenuExpanded) 180f else 0f,
                            animationSpec = tween(
                                durationMillis = 250,
                                easing = FastOutSlowInEasing
                            ),
                            label = "arrow_rotation_animation"
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .height(40.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { categoryMenuExpanded = true }
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = currentCat.second,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = topBarContentColor
                                )
                                Icon(
                                    painterResource(R.drawable.ic_arrow_down),
                                    contentDescription = if (categoryMenuExpanded) "收起分类" else "展开分类",
                                    tint = topBarContentColor,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .offset(y = 2.dp)
                                        .graphicsLayer {
                                            transformOrigin = TransformOrigin(
                                                pivotFractionX = 0.5f,
                                                pivotFractionY = 0.5f
                                            )
                                            rotationZ = arrowRotation
                                        }
                                )
                            }

                            // 下拉菜单
                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false },
                                offset = DpOffset(x = 0.dp, y = 16.dp),
                                modifier = Modifier
                                    .width(140.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                categoryOptions.forEach { (typeId, name, color) ->
                                    DropdownMenuItem(
                                        contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                                        text = {
                                            Text(
                                                text = name,
                                                fontSize = 15.sp,
                                                modifier = Modifier.padding(start = 16.dp)
                                            )
                                        },
                                        onClick = {
                                            currentCategoryId = typeId
                                            favoriteVM.setCategory(typeId)
                                            categoryMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier.size(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (typeId == -1) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.List,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .background(
                                                                color,
                                                                androidx.compose.foundation.shape.CircleShape
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // 右半部：操作菜单区域
                    if (isInManageMode) {
                        IconButton(
                            onClick = { isSearchBarExpanded = true },
                            modifier = Modifier
                                .size(44.dp)
                                .padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索收藏",
                                modifier = Modifier.size(24.dp),
                                tint = if (isSearching) MaterialTheme.colorScheme.primary else topBarContentColor
                            )
                        }
                        FavoriteManageDoneButton(
                            onClick = { favoriteVM.toggleManageMode() }
                        )
                    } else {
                        IconButton(
                            onClick = { isSearchBarExpanded = true },
                            modifier = Modifier
                                .size(44.dp)
                                .padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索收藏",
                                modifier = Modifier.size(24.dp),
                                tint = if (isSearching) MaterialTheme.colorScheme.primary else topBarContentColor
                            )
                        }
                        MoreOptionsButton()
                    }

                }
            }
        }

        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { favoriteVM.refreshList(showLoading = true, isSmartSync = false) },
            state = pullState,
            modifier = Modifier.weight(1f),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = darkThemeColor(YellowLightLight) { surfaceVariant },
                    color = darkThemeColor(YamiboColors.primary) { primary }
                )
            }
        ) {

            // 收藏列表
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 40.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp, 3.dp)
            ) {
                itemsIndexed(
                    items = searchedFavoriteList,
                    key = { _, item -> item.url }
                ) { _, item ->
                    val currentOnClick = remember(item, isInManageMode, openMangaFavorite) {
                        {
                            if (isInManageMode) {
                                favoriteVM.toggleItemSelection(item.url)
                            } else {
                                val encodedUrl = java.net.URLEncoder.encode(item.url, "utf-8")
                                val openByType: (Int) -> Unit = { type ->
                                    favoriteVM.updateStrategyBeforeNavigation(type)
                                    when (type) {
                                        1 -> {
                                            favoriteVM.clearNovelUpdateCheckFlag(item.url)
                                            navController.navigate("ReaderPage/$encodedUrl")
                                        }
                                        2 -> {
                                            favoriteVM.clearMangaUpdateCheckFlag(item.url)
                                            openMangaFavorite(item)
                                        }
                                        else -> navController.navigate("OtherWebPage/$encodedUrl")
                                    }
                                }
                                val knownType = when {
                                    item.type != 0 -> item.type
                                    item.sourceFid in setOf("30", "37") -> 2
                                    item.sourceFid in setOf("49", "55", "60") -> 1
                                    else -> 0
                                }

                                if (knownType != 0) {
                                    openByType(knownType)
                                } else {
                                    probingUrl = item.url
                                    probingJob = coroutineScope.launch {
                                        val resolvedType = runCatching {
                                            favoriteVM.resolveFavoriteTypeForOpen(item)
                                        }.getOrDefault(3)
                                        probingUrl = null
                                        probingJob = null
                                        openByType(resolvedType)
                                    }
                                }
                            }
                        }
                    }
                    val isSelected = selectedItems.contains(item.url)
                    val hasUpdate = novelCheckMap[item.url]?.hasUpdate == true ||
                            mangaCheckMap[item.url]?.hasUpdate == true ||
                            otherCheckMap[item.url]?.hasUpdate == true
                    val isCheckingUpdate = uiState.checkingUpdateUrls.contains(item.url)
                    val autoCheckEnabled = novelCheckMap[item.url]?.autoCheckEnabled == true ||
                            mangaCheckMap[item.url]?.autoCheckEnabled == true ||
                            otherCheckMap[item.url]?.autoCheckEnabled == true
                    FavoriteItem(
                        item.title,
                        item.lastView,
                        item.lastPage,
                        item.lastChapter,
                        onClick = currentOnClick,
                        onLongClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            itemActionTarget = item
                        },
                        modifier = Modifier.animateItem(),
                        isManageMode = isInManageMode,
                        isSelected = isSelected,
                        isHidden = item.isHidden,
                        type = item.type,
                        cacheInfo = cacheInfoMap[item.url],
                        mangaCachedPages = item.mangaCachedPages,
                        mangaCacheBytes = item.mangaCacheBytes,
                        hasUpdate = hasUpdate,
                        isCheckingUpdate = isCheckingUpdate,
                        autoCheckEnabled = autoCheckEnabled,
                        isPinned = item.pinAnchorUrl != null
                    )
                }
            }

            if (shouldShowEmptyState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isSearching) "没有匹配的收藏" else "暂无收藏",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            // 悬浮删除操作栏
            androidx.compose.animation.AnimatedVisibility(
                visible = isInManageMode,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it + 100 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { it + 100 },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = darkThemeColor(YellowLightLight) { surface },
                    shadowElevation = 3.dp,
                    border = BorderStroke(
                        width = 2.dp,
                        color = darkThemeColor(
                            YamiboColors.primary.copy(alpha = 0.4f)
                        ) { primary.copy(alpha = 0.5f) }
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allSelected = searchedFavoriteList.isNotEmpty() &&
                                selectedItems.size >= searchedFavoriteList.size &&
                                searchedFavoriteList.all { selectedItems.contains(it.url) }
                        TextButton(
                            onClick = {
                                favoriteVM.setSelectedItems(
                                    if (allSelected) emptySet()
                                    else searchedFavoriteList.map { it.url }.toSet()
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(if (allSelected) "取消" else "全选")
                        }

                        TextButton(
                            onClick = { favoriteVM.hideSelectedItems() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_visibility_off),
                                "隐藏",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("隐藏")
                        }

                        TextButton(
                            onClick = { favoriteVM.unhideSelectedItems() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_visibility),
                                "显示",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("显示")
                        }

                        Spacer(Modifier.width(4.dp))

                        TextButton(
                            onClick = {
                                if (selectedItems.isNotEmpty()) showDeleteConfirmDialog = true
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("删除")
                        }
                    }
                }
            }

            // 悬浮气泡
            androidx.compose.animation.AnimatedVisibility(
                visible = showTopToast,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) +
                        androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) +
                        androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.clickable {
                        showTopToast = false
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Text(
                        text = "发现了 $newItemsCount 条新收藏 (点击查看)",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (itemActionTarget != null) {
            val target = itemActionTarget!!
            Dialog(
                onDismissRequest = { itemActionTarget = null },
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            target.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        // 同一个按钮根据是否已置顶切换：未置顶=上箭头「置顶」，已置顶=下箭头「取消置顶」回到原位。
                        val isPinned = target.pinAnchorUrl != null
                        TextButton(
                            onClick = {
                                if (isPinned) {
                                    keepTopAfterUnpin = true
                                    favoriteVM.unpinToOriginal(target.url)
                                    itemActionTarget = null
                                } else {
                                    favoriteVM.moveToTop(target.url)
                                    itemActionTarget = null
                                    coroutineScope.launch {
                                        delay(120)
                                        lazyListState.animateScrollToItem(0)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isPinned) Icons.Default.KeyboardArrowDown
                                    else Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(if (isPinned) "取消置顶" else "置顶")
                            }
                        }
                        TextButton(
                            onClick = {
                                when (target.type) {
                                    0 -> favoriteVM.probeFavoriteTypeInBackground(target)
                                    1 -> favoriteVM.checkNovelUpdate(target)
                                    2 -> {
                                        if (mangaCheckMap[target.url] != null) {
                                            favoriteVM.checkMangaUpdate(target)
                                        } else {
                                            openMangaConfig(target)
                                        }
                                    }
                                    3 -> favoriteVM.checkOtherUpdate(target)
                                }
                                itemActionTarget = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("检查更新")
                            }
                        }
                        TextButton(
                            onClick = {
                                itemActionTarget = null
                                favoriteVM.clearFavoriteCache(target) { message ->
                                    YamiboToast.show(context = context, message = message)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_cache),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("清理缓存")
                            }
                        }
                        TextButton(
                            onClick = {
                                itemActionTarget = null
                                singleDeleteTarget = target
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("删除")
                            }
                        }
                    }
                }
            }
        }
        if (singleDeleteTarget != null) {
            val target = singleDeleteTarget!!
            AlertDialog(
                onDismissRequest = { singleDeleteTarget = null },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                title = { Text("删除收藏", fontSize = 18.sp) },
                text = {
                    Text(
                        "确定删除“${target.title}”吗？",
                        fontSize = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            singleDeleteTarget = null
                            favoriteVM.deleteFavorite(target) { message ->
                                YamiboToast.show(context = context, message = message)
                            }
                        }
                    ) {
                        Text(
                            "删除",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { singleDeleteTarget = null }) {
                        Text("取消", fontSize = 15.sp)
                    }
                }
            )
        }
        // 多选删除确认对话框
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                title = {
                    Text(
                        "删除收藏",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        "确定要删除这 ${selectedItems.size} 项收藏吗？",
                        fontSize = 15.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            favoriteVM.deleteSelectedFavorites { msg ->
                                YamiboToast.show(context = context, message = msg)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("确认删除", fontSize = 15.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("取消", fontSize = 15.sp)
                    }
                }
            )
        }
        // 漫画手动更新检查配置对话框
        if (showMangaConfigDialog && mangaUpdateCheckTarget != null) {
            val fav = mangaUpdateCheckTarget!!
            val initStrategy = mangaConfigPresetStrategy
            val initKeyword = mangaConfigPresetKeyword
            val initBookName = mangaConfigPresetBookName
            var selectedStrategy by remember(fav.url, initStrategy) { mutableStateOf(initStrategy) }
            var bookName by remember(fav.url, initBookName) { mutableStateOf(initBookName) }
            var keyword1 by remember(fav.url, initKeyword) { mutableStateOf(initKeyword) }
            var keyword2 by remember(fav.url) { mutableStateOf("") }
            var showKeyword2 by remember(fav.url) { mutableStateOf(false) }
            val existingManga = mangaCheckMap[fav.url]
            var mangaConfigAutoCheck by remember(fav.url, existingManga) { mutableStateOf(existingManga?.autoCheckEnabled ?: false) }
            var mangaConfigInterval by remember(fav.url, existingManga) {
                mutableIntStateOf(existingManga?.autoCheckIntervalHours ?: 12)
            }
            var searchCooldownSec by remember { mutableIntStateOf(0) }

            LaunchedEffect(selectedStrategy, showMangaConfigDialog) {
                while (selectedStrategy == MangaUpdateCheckStrategy.SEARCH && showMangaConfigDialog) {
                    val remaining = favoriteVM.getSearchCooldownRemainingMs()
                    searchCooldownSec = if (remaining > 0) ((remaining + 999) / 1000).toInt() else 0
                    if (searchCooldownSec == 0) break
                    delay(1000)
                }
                searchCooldownSec = 0
            }

            val searchOnCooldown = selectedStrategy == MangaUpdateCheckStrategy.SEARCH && searchCooldownSec > 0

            AlertDialog(
                onDismissRequest = { showMangaConfigDialog = false; mangaUpdateCheckTarget = null },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                title = { Text("漫画更新检查") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        val tagAvailable = mangaConfigTagAvailable

                        if (tagAvailable) {
                            Text("更新策略", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedStrategy == MangaUpdateCheckStrategy.TAG,
                                    onClick = { selectedStrategy = MangaUpdateCheckStrategy.TAG }
                                )
                                Text("标签页拉取", modifier = Modifier.clickable { selectedStrategy = MangaUpdateCheckStrategy.TAG })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedStrategy == MangaUpdateCheckStrategy.SEARCH,
                                    onClick = { selectedStrategy = MangaUpdateCheckStrategy.SEARCH }
                                )
                                Text("全局搜索", modifier = Modifier.clickable { selectedStrategy = MangaUpdateCheckStrategy.SEARCH })
                            }
                        } else {
                            Text(
                                "此漫画无标签，将使用「搜索」方式检查更新",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        if (selectedStrategy == MangaUpdateCheckStrategy.SEARCH) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = keyword1,
                                    onValueChange = { keyword1 = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    label = { Text("关键词 1") }
                                )
                                if (showKeyword2) {
                                    OutlinedTextField(
                                        value = keyword2,
                                        onValueChange = { keyword2 = it },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        label = { Text("关键词 2") }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showKeyword2 = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Light)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = bookName,
                                onValueChange = { bookName = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("漫画名称") }
                            )
                        }

                        if (searchOnCooldown) {
                            Text(
                                "搜索冷却中，请等待 $searchCooldownSec 秒",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        AutoCheckSection(
                            enabled = mangaConfigAutoCheck,
                            intervalHours = mangaConfigInterval,
                            onEnabledChange = { mangaConfigAutoCheck = it },
                            onIntervalChange = { mangaConfigInterval = it },
                            enabledCount = autoEnabledCount + when {
                                mangaConfigAutoCheck && existingManga?.autoCheckEnabled != true -> 1
                                !mangaConfigAutoCheck && (existingManga?.autoCheckEnabled == true) -> -1
                                else -> 0
                            },
                            maxCount = FavoriteVM.MAX_AUTO_CHECK,
                            isCurrentlyEnabled = existingManga?.autoCheckEnabled == true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showMangaConfigDialog = false
                            mangaUpdateCheckTarget?.let { target ->
                                val isSearch = selectedStrategy == MangaUpdateCheckStrategy.SEARCH
                                val combinedKeyword = if (isSearch) {
                                    listOf(keyword1.trim(), keyword2.trim())
                                        .filter { it.isNotEmpty() }
                                        .joinToString(" ")
                                } else ""
                                favoriteVM.checkMangaUpdateAndSaveAutoCheck(
                                    target,
                                    overrideStrategy = selectedStrategy,
                                    overrideSearchKeyword = combinedKeyword.ifBlank { null },
                                    overrideCleanBookName = if (isSearch) bookName.ifBlank { null } else null,
                                    autoEnabled = mangaConfigAutoCheck,
                                    intervalHours = mangaConfigInterval
                                )
                            }
                            mangaUpdateCheckTarget = null
                        },
                        enabled = !searchOnCooldown
                    ) { Text(if (searchOnCooldown) "冷却中..." else "开始查询") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showMangaConfigDialog = false; mangaUpdateCheckTarget = null
                    }) { Text("取消") }
                }
            )
        }
        // 小说自动检查配置对话框
        if (showNovelConfigDialog && novelUpdateCheckTarget != null) {
            val fav = novelUpdateCheckTarget!!
            AlertDialog(
                onDismissRequest = { showNovelConfigDialog = false; novelUpdateCheckTarget = null },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                title = { Text("小说更新检查") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AutoCheckSection(
                            enabled = novelConfigAutoCheck,
                            intervalHours = novelConfigInterval,
                            onEnabledChange = { novelConfigAutoCheck = it },
                            onIntervalChange = { novelConfigInterval = it },
                            enabledCount = autoEnabledCount + when {
                                novelConfigAutoCheck && novelCheckMap[fav.url]?.autoCheckEnabled != true -> 1
                                !novelConfigAutoCheck && (novelCheckMap[fav.url]?.autoCheckEnabled == true) -> -1
                                else -> 0
                            },
                            maxCount = FavoriteVM.MAX_AUTO_CHECK,
                            isCurrentlyEnabled = novelCheckMap[fav.url]?.autoCheckEnabled == true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showNovelConfigDialog = false
                        novelUpdateCheckTarget?.let { target ->
                            favoriteVM.saveNovelAutoCheck(target.url, novelConfigAutoCheck, novelConfigInterval)
                        }
                        novelUpdateCheckTarget = null
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showNovelConfigDialog = false; novelUpdateCheckTarget = null
                    }) { Text("取消") }
                }
            )
        }
        if (showOtherConfigDialog && otherUpdateCheckTarget != null) {
            val fav = otherUpdateCheckTarget!!
            AlertDialog(
                onDismissRequest = { showOtherConfigDialog = false; otherUpdateCheckTarget = null },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                title = { Text("帖子更新检查") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        AutoCheckSection(
                            enabled = otherConfigAutoCheck,
                            intervalHours = otherConfigInterval,
                            onEnabledChange = { otherConfigAutoCheck = it },
                            onIntervalChange = { otherConfigInterval = it },
                            enabledCount = autoEnabledCount + when {
                                otherConfigAutoCheck && otherCheckMap[fav.url]?.autoCheckEnabled != true -> 1
                                !otherConfigAutoCheck && (otherCheckMap[fav.url]?.autoCheckEnabled == true) -> -1
                                else -> 0
                            },
                            maxCount = FavoriteVM.MAX_AUTO_CHECK,
                            isCurrentlyEnabled = otherCheckMap[fav.url]?.autoCheckEnabled == true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showOtherConfigDialog = false
                        otherUpdateCheckTarget?.let { target ->
                            favoriteVM.saveOtherAutoCheck(target.url, otherConfigAutoCheck, otherConfigInterval)
                        }
                        otherUpdateCheckTarget = null
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showOtherConfigDialog = false; otherUpdateCheckTarget = null
                    }) { Text("取消") }
                }
            )
        }
    }
}
