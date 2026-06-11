package org.shirakawatyu.yamibo.novel.ui.widget.favorite

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun SwipeToFavoriteActionsRow(
    enabled: Boolean,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier) { content() }
        return
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val actionWidthPx = with(density) { 72.dp.toPx() }
    val maxOffsetPx = actionWidthPx * 2f
    val currentOnPin by rememberUpdatedState(onPin)
    val currentOnDelete by rememberUpdatedState(onDelete)

    fun closeActions() {
        scope.launch {
            offsetX.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        ) {
            FavoriteSwipeAction(
                label = "置顶",
                icon = { tint ->
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                onClick = {
                    closeActions()
                    currentOnPin()
                }
            )
            FavoriteSwipeAction(
                label = "删除",
                icon = { tint ->
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    closeActions()
                    currentOnDelete()
                }
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(maxOffsetPx) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val target = (offsetX.value + dragAmount)
                                .coerceIn(-maxOffsetPx, 0f)
                            scope.launch { offsetX.snapTo(target) }
                        },
                        onDragEnd = {
                            val target = if (offsetX.value <= -actionWidthPx * 0.65f) {
                                -maxOffsetPx
                            } else {
                                0f
                            }
                            scope.launch {
                                offsetX.animateTo(
                                    target,
                                    tween(220, easing = FastOutSlowInEasing)
                                )
                            }
                        },
                        onDragCancel = { closeActions() }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
private fun FavoriteSwipeAction(
    label: String,
    icon: @Composable (androidx.compose.ui.graphics.Color) -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon(contentColor)
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
