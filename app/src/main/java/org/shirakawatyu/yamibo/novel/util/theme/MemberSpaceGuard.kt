package org.shirakawatyu.yamibo.novel.util.theme

object MemberSpaceGuard {
    private val mobileCenterRegex = Regex("[?&](mobile=(2|yes)|mycenter=1)(?:[&#]|$)", RegexOption.IGNORE_CASE)
    private val rewrittenMemberSpaceRegex =
        Regex("/(?:space-uid-\\d+|blog-\\d+)(?:[-./?&#]|$)", RegexOption.IGNORE_CASE)
    private val homeModuleRegex =
        Regex("/home\\.php\\?[^#]*\\bmod=(space|blog)(?:[&#]|$)", RegexOption.IGNORE_CASE)
    private val memberIdentityRegex = Regex("[?&](uid|username)=", RegexOption.IGNORE_CASE)
    private val blogModuleRegex =
        Regex("/home\\.php\\?[^#]*\\bmod=blog(?:[&#]|$)", RegexOption.IGNORE_CASE)

    fun isMemberSpaceUrl(url: String): Boolean {
        if (mobileCenterRegex.containsMatchIn(url)) return false
        if (rewrittenMemberSpaceRegex.containsMatchIn(url)) return true
        if (blogModuleRegex.containsMatchIn(url)) return true
        return homeModuleRegex.containsMatchIn(url) && memberIdentityRegex.containsMatchIn(url)
    }

    fun isMemberSpaceHtml(html: String): Boolean {
        return Regex(
            """<body\b[^>]*\bid\s*=\s*(["'])space\1""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).containsMatchIn(html)
    }

    /**
     * 与 isMemberSpaceUrl 使用同一组规则；body#space 仅是运行时 DOM 兜底。
     */
    fun jsExpression(urlExpression: String = "location.href"): String {
        return """
            (!/[?&](?:mobile=(?:2|yes)|mycenter=1)(?:[&#]|$)/i.test($urlExpression) && (
                /\/(?:space-uid-\d+|blog-\d+)(?:[-./?&#]|$)/i.test($urlExpression) ||
                /\/home\.php\?[^#]*\bmod=blog(?:[&#]|$)/i.test($urlExpression) ||
                (/\/home\.php\?[^#]*\bmod=(?:space|blog)(?:[&#]|$)/i.test($urlExpression) &&
                 /[?&](?:uid|username)=/i.test($urlExpression))
            ))
        """.trimIndent()
    }
}
