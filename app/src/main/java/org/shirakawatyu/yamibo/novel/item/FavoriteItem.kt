package org.shirakawatyu.yamibo.novel.item

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.shirakawatyu.yamibo.novel.R
import org.shirakawatyu.yamibo.novel.ui.theme.YamiboColors
import org.shirakawatyu.yamibo.novel.ui.vm.FavoriteVM
import org.shirakawatyu.yamibo.novel.util.darkModeColor
import org.shirakawatyu.yamibo.novel.util.darkThemeColor

private val PREFIX_REGEX = Regex("^(?:[【\\[].*?[】\\]]|[\\s\\u00A0\\u3000])+")
private val TAG_REGEX = Regex("[【\\[].*?[】\\]]")

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

/**
 * 收藏项组件，用于展示收藏的小说条目信息。
 *
 * @param title 小说标题
 * @param lastView 上次阅读的网页页码
 * @param lastPage 上次阅读的阅读器页码
 * @param lastChapter 上次阅读的章节名称，可能为空
 * @param onClick 点击该项时触发的回调函数
 * @param modifier 组件修饰符，默认为Modifier
 * @param isDragging 是否正在拖拽状态，影响动画效果，默认为false
 * @param dragHandle 拖拽手柄的可组合项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteItem(
    title: String,
    lastView: Int,
    lastPage: Int,
    lastChapter: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    dragHandle: @Composable (() -> Unit) = {},
    isManageMode: Boolean = false,
    isSelected: Boolean = false,
    isHidden: Boolean = false,
    type: Int = 0,
    cacheInfo: FavoriteVM.CacheInfo? = null,
    mangaCachedPages: Int = 0,
    hasUpdate: Boolean = false,
    isCheckingUpdate: Boolean = false,
    autoCheckEnabled: Boolean = false,
    isPinned: Boolean = false
) {
    val tagColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

    val displayTitle = remember(title, tagColor) {
        buildAnnotatedString {
            append(title)
            val prefixMatch = PREFIX_REGEX.find(title)

            if (prefixMatch != null) {
                val tags = TAG_REGEX.findAll(prefixMatch.value)
                for (tag in tags) {
                    addStyle(
                        style = SpanStyle(
                            fontSize = 14.sp,
                            color = tagColor
                        ),
                        start = tag.range.first,
                        end = tag.range.last + 1
                    )
                }
            }
        }
    }

    // 拖拽动画：根据是否处于拖拽状态动态调整卡片的阴影、缩放和颜色
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 1.dp,
        label = "elevation_animation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1.0f,
        label = "scale_animation"
    )
    val color by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.surfaceVariant
            isManageMode && isSelected -> MaterialTheme.colorScheme.primaryContainer
            isManageMode && isHidden -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.tertiary
        },
        label = "color_animation"
    )

    val typeColor = when (type) {
        1 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        2 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.outline
    }

    // 小说标记浅色保持原本棕红，暗黑模式改为青色，和顶部分类菜单样例保持一致。
    val middleColor = if (type == 1) {
        darkModeColor(
            light = YamiboColors.secondary,
            dark = Color(0xFF3FC1B0)
        )
    } else {
        lerp(typeColor.first, typeColor.second, 0.75f)
    }
    val pinColor = darkModeColor(
        light = YamiboColors.primary.copy(alpha = 0.8f),
        dark = Color(0xFF8CCBFF)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = middleColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(3.dp.toPx(), size.height)
                    )
                }
                .padding(start = 15.dp, end = 15.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = if (isPinned) 22.dp else 0.dp),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Clip,
                        text = displayTitle,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(
                            lineBreak = LineBreak.Simple
                        )
                    )
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isPinned,
                        modifier = Modifier.align(Alignment.TopEnd),
                        enter = fadeIn(animationSpec = tween(160, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(120))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "已置顶",
                            tint = pinColor,
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer {
                                    rotationZ = 18f
                                }
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    if (!lastChapter.isNullOrBlank()) {
                        Text(
                            modifier = Modifier.padding(0.dp, 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            text = lastChapter,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (type == 1) {
                        Text(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            text = "上次读到第${lastPage + 1}页, 对应网页第${lastView}页"
                        )
                    }
                    if (cacheInfo != null && cacheInfo.totalPages > 0) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cache),
                                contentDescription = "已缓存",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                text = "已缓存 ${cacheInfo.totalPages} 页 (${
                                    formatFileSize(
                                        cacheInfo.totalSize
                                    )
                                })"
                            )
                        }
                    }
                    if (type == 2 && mangaCachedPages > 0) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cache),
                                contentDescription = "已缓存",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                text = "已缓存 $mangaCachedPages 页"
                            )
                        }
                    }
                }

                if (isManageMode && isHidden) {
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp,
                        text = "[已隐藏]"
                    )
                }
            }

            // 更新状态直接坐在右侧 40dp 把手上，不挤占标题和正文。
            val handle = @Composable {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    dragHandle()
                }
            }

            UpdateStatusHandle(
                isCheckingUpdate = isCheckingUpdate,
                hasUpdate = hasUpdate,
                autoCheckEnabled = autoCheckEnabled,
                modifier = Modifier.padding(start = 8.dp)
            ) { handle() }
        }
    }
}


private enum class HandleStatus {
    CHECKING,
    UPDATED
}

/**
 * 卡片右侧状态把手：状态胶囊作为浮层坐在最右侧 40dp 图标区域上。
 *
 * 状态贴在右侧把手上，不占标题宽度，也不改变卡片高度。
 * 优先级：检查中 > 有更新 > 自动检查已开启。
 *
 * 退出动画期间冻结 lastVisibleStatus，防止 AnimatedVisibility fade-out 时
 * isCheckingUpdate 已变 false 但内容切到 else 分支，闪现"新"胶囊。
 */
@Composable
private fun UpdateStatusHandle(
    isCheckingUpdate: Boolean,
    hasUpdate: Boolean,
    autoCheckEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val primary = darkThemeColor(YamiboColors.primary) { primary }
    val updateAccent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) { content() }

        // 检查更新：卡片右侧上下居中的主题色普通刷新转圈（替代原来顶部的「查」胶囊）。
        androidx.compose.animation.AnimatedVisibility(
            visible = isCheckingUpdate,
            enter = fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(120))
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = primary
            )
        }

        // 有更新 胶囊（顶部）
        androidx.compose.animation.AnimatedVisibility(
            visible = hasUpdate && !isCheckingUpdate,
            enter = fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(120)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
        ) {
            HandleStatusCapsule(
                status = HandleStatus.UPDATED,
                accent = updateAccent
            )
        }

        // 自动检查已开启 胶囊（仅在无查/新时展示）
        androidx.compose.animation.AnimatedVisibility(
            visible = autoCheckEnabled && !isCheckingUpdate && !hasUpdate,
            enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(120)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
        ) {
            AutoCheckCapsule()
        }
    }
}

/** 贴在把手上的迷你胶囊：保留胶囊质感，但被限制在右侧图标区域内。 */
@Composable
private fun HandleStatusCapsule(
    status: HandleStatus,
    accent: Color
) {
    val isChecking = status == HandleStatus.CHECKING
    val shape = RoundedCornerShape(50)
    val containerAlpha = if (isChecking) 0.14f else 0.16f
    val borderAlpha = if (isChecking) 0.34f else 0.40f
    val textOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { (-3).dp.toPx() }

    Row(
        modifier = Modifier
            .width(38.dp)
            .height(18.dp)
            .clip(shape)
            .background(accent.copy(alpha = containerAlpha))
            .border(1.dp, accent.copy(alpha = borderAlpha), shape)
            .padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.45.dp,
                color = accent
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "查",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { translationY = textOffsetPx }
            )
        } else {
            UpdateCapsuleGlyph(accent = accent)
            Spacer(Modifier.width(3.dp))
            Text(
                text = "新",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { translationY = textOffsetPx }
            )
        }
    }
}

/** 自动检查已开启的胶囊：比”查”/”新”更低调，表明该项已纳入后台自动巡检。 */
@Composable
private fun AutoCheckCapsule() {
    val primary = darkThemeColor(YamiboColors.primary) { primary }
    val shape = RoundedCornerShape(50)
    val textOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { (-3).dp.toPx() }

    Row(
        modifier = Modifier
            .width(38.dp)
            .height(18.dp)
            .clip(shape)
            .background(primary.copy(alpha = 0.08f))
            .border(1.dp, primary.copy(alpha = 0.28f), shape)
            .padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "自动",
            color = primary.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.graphicsLayer { translationY = textOffsetPx }
        )
    }
}

/** 胶囊内的柔和脉冲点：让”有更新”有生命感，但不再是孤零零廉价红点。 */
@Composable
private fun UpdateCapsuleGlyph(accent: Color) {
    val transition = rememberInfiniteTransition(label = "update_capsule_pulse")
    val haloScale by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "update_capsule_halo_scale"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.34f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "update_capsule_halo_alpha"
    )

    Box(modifier = Modifier.size(10.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(7.dp)
                .graphicsLayer {
                    scaleX = haloScale
                    scaleY = haloScale
                    alpha = haloAlpha
                }
                .clip(CircleShape)
                .background(accent)
        )
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(accent)
        )
    }
}
