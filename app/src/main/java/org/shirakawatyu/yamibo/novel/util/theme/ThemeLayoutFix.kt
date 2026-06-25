package org.shirakawatyu.yamibo.novel.util.theme

/**
 * 与主题无关的「结构性布局修复」CSS。
 *
 * 站点自身 CSS 在窄屏（WebView 把电脑版以 width=device-width 渲染）下会布局错乱，这类修复不分
 * 暗黑/浅色——必须在两个模式都注入，否则同一页面在暗黑与浅色下布局不一致（如 blog/个人主页：
 * 暗黑下已修好、浅色下仍重叠）。这里只改布局（float/width/margin），绝不碰颜色与 background-image。
 *
 * 暗黑列表 [DARK_MODE_CSS_RULES_CLASSIC] 与浅色列表 [LIGHT_MODE_CSS_RULES_CLASSIC] 都会拼接本列表。
 */
val STRUCTURAL_LAYOUT_FIX_CSS_RULES = listOf(
    "/* spacecp 控制面板页（发布日志/上传/资料等个人主页相关页，#ct.ct3_a）窄屏 reflow：站点把 .mn 写死 775px、.sd float right 220px，而 .wp 在窄屏是 width:auto，导致主表单溢出、侧栏(资料完成度/最近来访)浮到右沿压住「发布」按钮。取消浮动、宽度自适应，让主表单与侧栏纵向堆叠。与主题无关，暗黑/浅色都注入以保持一致。 */",
    ".pg_space .ct3_a .mn, .pg_space .ct3_a .sd { float: none !important; width: auto !important; margin-left: 0 !important; }"
)
