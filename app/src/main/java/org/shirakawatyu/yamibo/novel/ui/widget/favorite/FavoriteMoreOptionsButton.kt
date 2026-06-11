package org.shirakawatyu.yamibo.novel.ui.widget.favorite

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun FavoriteDeleteAllButton(
    enabled: Boolean,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onDeleteAll,
        enabled = enabled,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "删除所有收藏",
            modifier = Modifier.size(23.dp),
            tint = if (enabled) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            }
        )
    }
}
