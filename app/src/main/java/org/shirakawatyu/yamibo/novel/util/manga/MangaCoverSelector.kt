package org.shirakawatyu.yamibo.novel.util.manga

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object MangaCoverSelector {
    private const val BASE_URL = "https://bbs.yamibo.com/"
    private val imageAttributes = listOf("zoomfile", "file", "zsrc", "data-src", "src")
    private val ignoredImageParts = listOf(
        "smiley",
        "smilie",
        "static/image",
        "template/",
        "/common/",
        "/block/",
        "avatar"
    )

    fun firstCoverUrl(messages: Iterable<String>): String? {
        messages.forEach { message ->
            val document = Jsoup.parse(message, BASE_URL)
            document.select("img").forEach { image ->
                val url = imageUrl(image) ?: return@forEach
                if (isCoverCandidate(url, image)) return url
            }
        }
        return null
    }

    private fun imageUrl(image: Element): String? {
        val attribute = imageAttributes
            .firstOrNull { image.hasAttr(it) && image.attr(it).isNotBlank() }
            ?: return null
        val raw = image.attr(attribute).trim()
        if (raw.startsWith("data:") || raw.startsWith("blob:")) return null
        return image.absUrl(attribute).ifBlank { raw }
    }

    private fun isCoverCandidate(url: String, image: Element): Boolean {
        if (image.hasAttr("smilieid")) return false
        val normalizedUrl = url.replace('\\', '/')
        val markerText = "$normalizedUrl ${image.className()} ${image.id()}"
        return ignoredImageParts.none { markerText.contains(it, ignoreCase = true) }
    }
}
