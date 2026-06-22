# CLAUDE.md

本文件为 AI 编码代理在本仓库工作时的项目规范。请始终用**中文**回复。

## 项目概况

- **YamiboReaderLite（300 Lite）**：面向百合会论坛（bbs.yamibo.com）的非官方 Android 阅读客户端，基于 [prprbell/YamiboReaderPro](https://github.com/prprbell/YamiboReaderPro) 修改而来。
- Kotlin + Jetpack Compose + Material 3；minSdk 24 / targetSdk 34 / compileSdk 34 / JDK 17。
- 包名 `org.shirakawatyu.yamibo.novel`，applicationId `com.krelinnbios.yamiboreaderlite`。
- 仅构建 `arm64-v8a` 和 `armeabi-v7a`；APK 固定命名为 `300 Lite.apk`。
- 应用内更新绑定 GitHub Releases（KrelinnBios/YamiboReaderLite）。

## 当前功能

描述当前实际提供的功能；具体约束与“不要做什么”属于[已确立的决定](#已确立的决定)。

- 论坛浏览：账号登录、论坛 WebView 浏览、网页暗黑模式、自动签到、DNS 优化。
- 链接直达：识别剪贴板或从外部应用打开的百合会帖子链接，一键在论坛 WebView 跳转到对应帖子。
- 论坛屏蔽：在帖子/楼层/列表注入「屏蔽」按钮，按主题或楼层屏蔽他人内容（自己的内容不显示按钮），黑名单弹窗支持搜索与筛选。
- 漫画发现：浏览和搜索中文漫画区、漫画图源区，生成、更新和管理本地漫画目录。
- 漫画阅读：原生阅读器支持章节切换、进度记录、缓存、亮度调节和纵向/从左到右/从右到左三种阅读方向；识别失败时使用 WebView 兜底。
- 小说阅读：原生阅读器支持字号、行距、页边距、横向/纵向翻页、正文图片、简繁转换、章节跳转、进度记录和页面缓存。
- 收藏管理：同步论坛收藏，支持分类、搜索、拖动排序、隐藏、删除、置顶、缓存清理，以及小说/漫画/其他帖子的手动和自动更新检查。
- 浏览历史：记录帖子浏览历史，支持日期筛选、批量删除，并通过独立 WebView 打开历史帖子。
- 缓存维护：维护小说页面缓存、漫画图片缓存、缓存统计、单项清理、全量清理和定期自动清理。
- 应用更新：启动或手动检查 GitHub Release，支持下载、校验 APK、调起系统安装器，失败时提供 Releases 页面。
- 崩溃兜底：全局崩溃处理器记录未捕获异常日志，并吞掉后台线程异常以减少整体闪退。

## 常用命令

```powershell
.\gradlew.bat compileDebugKotlin   # Kotlin 改动后的最低检查，必须执行
.\gradlew.bat testDebugUnitTest    # 运行本地单元测试
.\gradlew.bat assembleDebug        # 生成 app\build\outputs\apk\debug\300 Lite.apk
.\gradlew.bat clean assembleDebug  # 增量构建出现 TaskOutputsBackup/IOException 等损坏时使用
```

- 修改 Kotlin 后至少运行 `compileDebugKotlin`；改到解析、URL、会话、更新或图片策略等已有测试覆盖的模块时，同时运行 `testDebugUnitTest`。
- 纯文档、图片或资源说明修改无需运行 Gradle 构建。
- 本地一般没有连接设备/模拟器，无法跑 instrumented test 或 adb；运行验证依赖用户实机安装反馈。
- 间歇性、网络相关的 bug 无法在本机稳定复现，修改后必须明确请用户实测确认，不要直接宣称“已修复”。

## 架构速览

### 页面与导航

- 四个底栏主页面：`MangaHomePage`（原生列表）、`FavoritePage`（原生列表）、`BBSPage`（WebView）、`MinePage`（WebView）。
- BBS/Mine 使用常驻组合并通过 `isSelected` 切换；Manga/Favorite 走 NavHost 的 saveState/restoreState。
- 主要子页面包括 `ReaderPage`、`NativeMangaPage`、`MangaWebPage`、`ReaderWebPage`、`OtherWebPage`、`HistoryPage` 和 `MineHistoryPostPage`。
- 历史帖子详情复用 `MinePage` 的页面能力，但使用 route-scoped `MinePageVM` 和独立 WebView；不得把历史帖子状态写回底栏“我的”页面。

### WebView

- `WebViewPool` 负责复用实例；页面切走后通过 `schedulePause` 延迟暂停。
- WebView 暂停期间 `evaluateJavascript` 可能被丢弃。任何“恢复页面”路径（如 `resumeBbsWebViewAfterChildPage`、`resumeMineWebViewAfterChildPage`）都必须重新注入主题和页面交互 JS。
- 从原生阅读器返回原帖时应恢复现有 WebView，不要无条件重新加载，否则会丢失滚动位置并重新出现加载遮罩。
- `BBSPageState` 管理论坛页加载、超时、错误和前后台恢复；修改恢复逻辑时不要绕过其状态机另起一套标志。

### 阅读器

- 小说：`ReaderPage` + `ReaderVM`，页面加载链为 `AuthenticatedThreadPageLoader` + `ThreadHtmlParser`，并由 `LocalCacheUtil`、`CacheUtil` 和内存预热组件维护缓存。
- 漫画：优先进入 `NativeMangaPage`，由 `MangaReaderManager`、`MangaImagePipeline` 和 `DirectoryRepository` 管理章节、图片和目录；`MangaWebPage` 负责 WebView 兜底。
- 漫画识别链固定为 `MangaProber.fastApiProbe` → `fastHtmlProbe` → WebView 兜底，不要在没有证据时改变顺序。
- `ReaderReturnBridge` 维护阅读器与原帖 WebView 的一次性返回状态。

### 数据与后台功能

- 收藏主状态在 `FavoriteVM`；本地数据、删除同步和墓碑队列分别由 `FavoriteUtil`、`FavoriteDeleteUtil`、`TombstoneQueueUtil` 管理。
- 收藏更新检查统一经过 `UpdateCheckEngine`，自动调度由 `AutoUpdateCheckScheduler` 负责；小说、漫画和其他帖子各有独立 profile/util。
- 浏览历史由 `HistoryUtil` 管理；漫画目录由 `DirectoryRepository` 管理。
- 设置使用 DataStore（`SettingsUtil` / `DataStoreUtil`）；不要另建 SharedPreferences 保存同类全局设置。阅读器自身已有兼容存储除外。
- 应用更新统一走 `AppUpdateManager`；自动签到统一走 `AutoSignManager` / `AccountSyncManager`；缓存清理由 `CacheMaintenance` 统一协调。
- 论坛屏蔽：数据与持久化在 `ForumBlocklistManager`；页面注入逻辑在 `PageJsScripts.getForumBlockerJs`（向论坛/个人中心 WebView 注入「屏蔽」按钮与隐藏占位）；JS↔原生桥接是 `ForumBlocklistJSInterface`（`AndroidForumBlocklist`，含 `block` / `unblock` / `setUid`）；管理弹窗是 `ForumBlocklistDialog`。
- 当前登录 uid：`CurrentUserUtil` 持久化、`GlobalData.currentUid` 内存缓存，供屏蔽功能排除自己的内容。来源：收藏接口 `member_uid`（`FavoriteVM`）、桌面页 `discuz_uid`、手机版 `mycenter=1` 链接（页面脚本探测后经 `setUid` 回传）。
- 链接直达：`YamiboPostLinkUtil` 统一识别/归一化帖子链接；剪贴板与外部 deep link 经 `GlobalData.pendingClipboardUrl` / `pendingDeepLinkUrl` 传给 `BBSPage` 加载。
- 崩溃兜底：`CrashHandler` 在 `YamiboApplication.onCreate` 最先安装。

### 网络

- `YamiboRetrofit` 有两个主要客户端：`okHttpClient` 用于接口、HTML 代理和主框架，`threadOkHttpClient` 用于论坛图片。
- 两个客户端共享 `sharedConnectionPool` 与 `TtlDnsCache`；DNS 优化由 `DynamicDns` 在阿里/腾讯 DoH、手动 DoH 和系统 DNS 之间处理。
- WebView HTML 暗黑代理、静态资源代理、Cookie 同步和 Coil 图片缓存判断也集中在 `YamiboRetrofit`，修改时注意 WebView 与原生请求行为必须保持一致。

## 已确立的决定

以下决定已有实机问题和现有实现作为依据，不要反复商量或擅自推翻。

### 网络

- **禁止强制 HTTP/1.1**（`protocols(HTTP_1_1)`）。这曾导致整个 App 无法连接论坛，并与共享连接池中的 h2 连接冲突。
- 服务器偶发 `stream was reset: PROTOCOL_ERROR` 已在应用级拦截器 `proceedWithDnsRecovery` 处理：GET 遇瞬时流重置最多重试 3 次、444 WAF 限流最多重试 2 次（限流多打反而加剧，故更保守），每次重试前执行 `connectionPool.evictAll()` 清理坏连接并递增退避。不要再改协议协商层。
- 连接池 keepalive 固定为 **50 秒**，必须短于论坛服务器约 60～75 秒的空闲超时。不要改回分钟级，否则切回 App 时可能复用半死连接。

### 暗黑模式

- 只有一套深色主题：经典蓝黑 `DarkThemeColors.CLASSIC`，主色 `#4EA1FF`、背景 `#0D141D`、面板 `#182332`。不引入多套深色主题。
- 论坛网页深色规则全部位于 `util/theme/DarkClassic.kt`，加载时 HTML 代理注入（`proxyHtmlForDarkMode` + `injectThemeCssIntoHtml`）与运行时 JS 注入（`getThemeSetJs`）必须共用同一份 CSS。
- 原色（浅色）模式**不再是零注入**：会注入一份**极小**的覆盖（`util/theme/LightClassic.kt` 的 `LIGHT_MODE_CSS_RULES_CLASSIC`），目前**仅**统一正文链接颜色。同样走 `injectThemeCssIntoHtml` / `getThemeSetJs` 两条路径，不要往里塞与"链接统一"无关的规则，浅色模式整体应保持论坛原样。
- CSS 规则字符串末尾会统一执行 `background:` → `background-color:` 重写。规则中可以写 `background:`，但**绝不能覆盖站点的 `background-image`**，轮播图、头像和会员自定义背景依赖它。
- CSS 规则字符串中**不能出现单引号**，否则会破坏 JS 注入字符串拼接。
- 只有**自定义 DIY 会员空间**才不启用暗黑模式（保留作者亲手设计的版面）；判定改为**按页面内容**：页面是 `body#space` 且使用了 `data/attachment` 的自定义背景图。普通空间（如「xxx的空间」，只有 `static/image` 默认图、无自定义背景）以及帖子、版块、手机版个人中心等所有其它页面都照常启用暗黑。不要再用 URL（`space-uid-N`/`mod=space&uid=` 等）来排除——那会误伤无自定义的普通空间，让它们也变不了深色。
- 底栏“我的”是手机版个人中心（`mobile=2` / `mobile=yes` / `mycenter=1`），非 `body#space`，照常暗黑。
- 会员空间守卫统一由 `util/theme/MemberSpaceGuard` 提供：`isMemberSpaceHtml()` 给 HTML 代理判断、`jsExpression()` 给运行时 JS 判断，两者共用「`body#space` + `data/attachment` 背景」这同一组内容规则。`YamiboRetrofit.proxyHtmlForDarkMode` 不再按 URL 跳过空间页（所有 bbs 主框架页都进代理），由 `injectDarkModeCssIntoHtml` / `injectLightModeCssIntoHtml` 调 `isMemberSpaceHtml()` 决定注不注入；深色/浅色 JS 注入（`getDarkModeSetJs` / `getLightModeSetJs`）调 `jsExpression()`。改判断逻辑要一处改、各处一致。
- 当前 DIY 检测认 `data/attachment` 自定义背景图。若遇到只用纯外链图（无 `data/attachment`）做 DIY 的空间会漏判，按真实样本再扩展规则，不要凭空猜。
- 投票区 `#poll` 的彩条以及用户侧栏的经验/积分彩条依赖内联颜色，必须保留原色；不要用大范围 `.plc div` / `.pls div` 规则覆盖 `.pbr`、`.pbg`、`.pbr2`、`.pbg2`。
- 深色链接统一使用浅蓝 `#7dbdf2`，不允许改成棕色；浅色（原色）模式下正文链接统一为站点默认链接色 `#6E2B19`。
- 链接颜色统一要覆盖到链接内部的 `font[color]` / 内联 `color` 子元素（楼主常把链接套多种颜色），但只改文字色、绝不动 `background-image`。
- 新页面未适配时，先让用户提供真实 HTML 片段，再按选择器精准补规则；不要凭空猜测或添加大范围通配。
- 开屏（系统 SplashScreen + 窗口背景）固定用浅色 `@color/splash_background`(#FCF4CF)，**不跟随暗黑模式**。曾尝试用 SharedPreferences 引导缓存 + `UiModeManager.setApplicationNightMode` + `values-night` 让开屏变暗，但带来点击闪原色、漫画首页加载异常等问题，已整体回退；不要再加这套开屏换色逻辑。

### 交互

- 底栏**单击表示“切换板块”**：跳到对应板块；已在本板块内时不做任何事，避免误触重载。**长按表示“回该板块主页”**（论坛首页 / 个人资料 / 漫画首页 / 收藏首页），即 `returnToHome(notifyHome = true)` 发出的 `goHomeEvent`；单击走 `notifyHome = false`。不要把回主页改回单击。
- 刷新统一使用下拉手势，原生页用 `PullToRefreshBox`，WebView 页用 `SwipeRefreshLayout`。长按刷新已删除，不要恢复（长按现用于回主页，不是刷新）。
- 下拉刷新指示器必须跟随暗黑模式配色：深色背景 `#223247`、箭头 `#4EA1FF`。
- 切回 `MangaHomePage`（长按底栏漫画键）不触发网络刷新，只清空搜索词并回到顶部，避免网络波动破坏现有列表。
- 小说阅读器进度只显示页数 `当前/总数`，不显示百分比。
- 小说阅读器标题不常驻顶部，放在点击正文后弹出的菜单中间，与漫画阅读器保持一致。
- 从阅读器返回“原帖”的 URL 必须经过 `ReaderReturnBridge.forceMobileTemplate` 并带 `mobile=2`，否则论坛会渲染电脑版。

### 权限贴（readperm）

- API 返回 `readperm > 0` 只是帖子元数据。只要 API 已返回图片 URL，就说明当前账号有权限，不得在 `MangaProber.fastApiProbe` 中据此拦截。
- 访问拒绝检测只看 Discuz 错误元素：`#messagetext, .showmessage, .alert_error, .nfl .f_c`。
- 绝不回退到 body 全文匹配；“阅读权限: 50”等正常帖子属性会造成误判。

### 收藏、历史与缓存

- 收藏删除同时涉及论坛端、本地排序数据、缓存和 `TombstoneQueueUtil` 的延迟同步；不要只删除 UI 列表或只调用论坛接口。
- 历史帖子详情必须使用独立 WebView，退出后清理 route-scoped 状态，不能污染 `MinePage` 的常驻 WebView。
- 小说缓存以规范化 URL 及兼容别名为索引；清理单本缓存时必须覆盖同帖 URL 的兼容形式。
- 漫画缓存统计依赖实际图片 URL 集合；清理单项时优先精确淘汰对应 URL，不要无条件清空全局 Coil 缓存。

### 论坛屏蔽

- 只屏蔽**别人**的内容：自己发布的主题/楼层（含 1 楼）不显示「屏蔽」按钮。判断依据是当前登录 uid 与该主题/楼层作者 uid 比对；`view=me` 或自己 `mod=space&do=thread/reply/favorite` 的"我的空间列表页"整页跳过，不依赖行内作者链接（部分模板该页不带作者头像链接）。
- uid 必须**登录后尽早拿到并本地持久化**（`CurrentUserUtil`），注入屏蔽脚本时作为 `selfUid` 回传给页面。手机版帖子页本身不带任何自身 uid 标识，必须靠提前存好的值。
- 列表页「屏蔽」按钮用普通的 `.threadlist_foot li` 容器，与浏览/回复数按钮对齐，不要单独重置样式；帖子页按钮与用户名之间用四个不可断空格（`String.fromCharCode(160,...)`）保持间距。

### 链接直达

- 帖子链接的识别与归一化统一走 `YamiboPostLinkUtil`：强制 `bbs.yamibo.com` + `mobile=2`，排除图片、首页等非帖子链接；剪贴板检测只在切回前台/启动时读当前剪贴板。
- 跳转必须经 `BBSPage` 的 `startLoading`（会置 `hasRequestedInitialLoad` 等状态），**不要用裸 `webView.loadUrl`**，否则会与首页初始加载抢加载、表现为"停在论坛首页没反应"。

### 崩溃兜底

- `CrashHandler` 在 `YamiboApplication.onCreate` **最先**安装。后台线程未捕获异常记录后吞掉以避免整体闪退；主线程异常仍交还系统默认处理器（此时无法安全恢复）。崩溃日志写入 `getExternalFilesDir/crash`，仅保留最新若干份。

### CI / 发布

- workflow 只构建 release 签名包（`assembleRelease`），四个签名 secrets 缺一即失败。
- release 触发时版本号从 tag 推导：`APP_VERSION_NAME` 为 tag 去掉 `v` 前缀，`APP_VERSION_CODE` 为 `github.run_number`；tag 必须符合 `v数字.数字…` 格式，否则构建直接失败。发布包不得回落到 `build.gradle.kts` 的默认版本号，否则会导致应用内更新循环。
- GitHub Actions 固定使用 Node 24 兼容版本：checkout@v5、setup-java@v5、setup-gradle@v5、setup-android@v4、upload-artifact@v5，并设置 `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24`。不要降级。
- 发布产物路径和名称固定为 `app/build/outputs/apk/release/300 Lite.apk` / `300 Lite.apk`。

## 测试与修改原则

- 优先沿用现有类、状态流和工具函数，不要在 Composable 中复制网络、缓存或持久化逻辑。
- 修改 URL 归一化、Cookie、会话、HTML 解析、阅读器返回链接、图片加载策略、应用更新解析、帖子链接识别（`YamiboPostLinkUtil`）或屏蔽数据（`ForumBlocklistManager`）时，应补充或更新 `app/src/test` 下的对应单元测试。
- UI、WebView 生命周期和网络恢复通常无法由 JVM 单元测试完整覆盖，编译通过后仍需说明实机验证点。
- 不要顺手升级 Gradle、AGP、Kotlin、Compose 或网络依赖；依赖升级必须是明确任务，并单独验证兼容性。
- 不要提交构建产物、签名材料、`.env`、`local.properties` 或临时抓取页面。

## 其他约定

- 提交信息沿用现有简短中文风格，如 `更新 XXX.kt`。
- 用户会把论坛页面“另存为”的 HTML 及其 `_files/` 目录放到仓库根目录或 Downloads 作为适配样本；它们是临时测试材料，**不要提交进 git**。
- 修 UI/CSS 前先获取真实页面样本；没有对应 HTML 时不要凭空猜选择器。
- README 功能列表格式为 `- 四字标签：描述。`，不加粗。
- 文档使用 UTF-8 编码；修改中文文件时避免因 PowerShell 默认编码造成乱码。
