package org.shirakawatyu.yamibo.novel.ui.vm

import org.shirakawatyu.yamibo.novel.bean.Favorite

/**
 * 收藏类型的可信来源判定。
 *
 * 旧版本曾在类型探测临时失败时把条目持久化为 type=3 且没有 sourceFid。
 * 这种数据不能继续当作可靠的“其他”，否则真实漫画会永久绕过漫画打开和更新链路。
 */
internal object FavoriteTypeResolver {
    const val MANUAL_SOURCE_FID = "__manual__"
    const val DETECTED_OTHER_SOURCE_FID = "__other__"

    val MANGA_FIDS = setOf("30", "37")
    val NOVEL_FIDS = setOf("49", "55", "60")

    fun reliableType(favorite: Favorite): Int = when {
        favorite.sourceFid == MANUAL_SOURCE_FID && favorite.type in 1..3 -> favorite.type
        favorite.sourceFid in MANGA_FIDS -> 2
        favorite.sourceFid in NOVEL_FIDS -> 1
        !favorite.sourceFid.isNullOrBlank() -> 3
        favorite.type in 1..2 -> favorite.type
        // 没有版区依据的旧 type=3 可能是历史误判，需要重新探测一次。
        else -> 0
    }
}
