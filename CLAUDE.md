# CLAUDE.md

本文件是 AI 编码代理在 **YamiboReaderLite** 仓库工作的项目级规范。默认使用**中文**回复、说明和提交信息。

## 仓库状态：已停止维护

这是当前项目最重要的前提。

- **YamiboReaderLite 已停止维护，不再作为后续功能开发主线。** 后续开发迁移到 [KrelinnBios/YamiboPlus](https://github.com/KrelinnBios/YamiboPlus)。README 顶部的停止维护警告必须保留。
- 在没有明确要求的情况下，不要在 Lite 中主动新增功能、做大规模重构、架构迁移、UI 重设计或依赖升级；这类工作原则上属于 YamiboPlus。
- 如果用户**明确要求修改 YamiboReaderLite 本仓库**，则按要求在本仓库完成，不要擅自把任务改去 YamiboPlus。此时优先采取最小、兼容、可回退的维护式改动。
- Lite 中仍可进行明确要求的 bug 修复、兼容修复、文档修正、CI/发布维护、必要的安全修复和定向回移植；不要借机扩展任务范围。
- 应用内更新、GitHub Release 和签名构建代码仍保留在仓库中；“停止维护”不等于可以随意删除这些现有能力。
- 本项目基于 `prprbell/YamiboReaderPro` 修改并延续 `flben233/YamiboReader` 的上游代码，不是从零开发。涉及归属、许可证或再分发时先看 `NOTICE`、`LICENSE`、`THIRD-PARTY-NOTICES.md`。

## 信息优先级与工作方式

遇到冲突时按以下顺序判断：

1. 用户当前明确要求。
2. 当前仓库源码、测试、Gradle 配置和 `.github/workflows/`。
3. 本文件记录的项目约束和历史决定。
4. README 等面向用户的说明文字。

因此：

- **不要把本文件中的版本号、Action 版本或实现细节当成比源码更高的真相。** 如果实际 Gradle/workflow 已变化，以当前文件为准，并同步修正本文件中已经过时的描述。
- 动手前先搜索现有实现、调用链、状态容器和测试。这个项目已有较多针对论坛异常行为的补丁，禁止在没看现有逻辑时另起一套平行实现。
- 优先修根因，不为“看起来更整洁”做无关重构。Lite 已冻结，最小 diff 比架构洁癖更重要。
- 对依赖论坛 HTML、Discuz 模板、WebView 生命周期或真实网络状态的问题，没有真实样本时不要猜；需要页面结构时以实际 HTML/URL/日志为依据。
- 不把“编译通过”写成“问题已修复”。网络、WebView、页面注入和设备行为通常仍需要实机确认。

## 项目概况

- **YamiboReaderLite（300 Lite）**：面向百合会论坛 `bbs.yamibo.com` 的非官方 Android 阅读客户端。
- 单模块 Android 工程：根项目 `YamiboReaderLite`，仅 `:app`。
- Kotlin + Jetpack Compose + Material 3；JDK 17。
- `compileSdk = 34`、`targetSdk = 34`、`minSdk = 24`。
- namespace：`org.shirakawatyu.yamibo.novel`。
- applicationId：`com.krelinnbios.yamiboreaderlite`。
- 仅构建 `arm64-v8a`、`armeabi-v7a`。
- APK 文件名固定为 `300-Lite.apk`。
- 应用内更新源默认指向 `KrelinnBios/YamiboReaderLite` 的 GitHub Releases。
- `AGENTS.md` 仅指向本文件；本文件同时作为其他编码代理的仓库规范。

## 代码地图

主源码位于：

`app/src/main/java/org/shirakawatyu/yamibo/novel/`

重点目录/入口：

- `MainActivity.kt`：主导航和大量页面级协调逻辑。文件很大，但在 Lite 中不要为了“拆文件”单独做大重构。
- `YamiboApplication.kt`：进程级初始化，`CrashHandler` 必须很早安装。
- `ui/page/`：Compose / WebView 页面。
- `ui/vm/`：页面 ViewModel 和主要 UI 状态协调。
- `ui/state/`：页面状态对象。
- `ui/component/`、`ui/widget/`：复用 UI。
- `network/`：Retrofit / OkHttp / DNS / 网络代理相关逻辑。
- `parser/`：论坛 HTML、帖子和目录解析。
- `repository/`：目录等持久化/聚合逻辑。
- `util/`：缓存、设置、更新、签到、链接、WebView 脚本等大量跨页面工具。
- `util/theme/`：论坛网页深浅色 CSS 与会员空间守卫。
- `app/src/test/`：JVM 单元测试；解析、URL、会话、更新、图片策略等改动优先在这里补覆盖。
- `.github/workflows/ci.yml`：日常编译 + JVM 单测门禁。
- `.github/workflows/build-apk.yml`：手动/Release 的签名 APK 构建。

不要仅凭目录名判断职责；修改前先搜索目标 symbol 的引用。

## 当前功能边界

以下是 Lite 当前已经存在的主要能力，不代表还要继续扩展：

- 论坛浏览：登录、WebView 浏览、网页暗黑模式、自动签到、DNS 优化。
- 链接直达：识别剪贴板或外部应用打开的百合会帖子链接，并跳到论坛页。
- 论坛屏蔽：帖子/楼层/列表屏蔽与本地黑名单管理。
- 漫画发现：浏览/搜索中文漫画区和漫画图源区，生成、更新和管理本地目录。
- 漫画阅读：原生阅读器、章节切换、进度、缓存、亮度、纵向/LTR/RTL；识别失败时 WebView 兜底。
- 小说阅读：字号、行距、页边距、横向/纵向翻页、正文图片、简繁转换、章节、进度和页面缓存。
- 收藏管理：论坛收藏同步、分类、搜索、排序、隐藏、删除、置顶、缓存和手动更新检查。
- 浏览历史：日期筛选、批量删除、独立 WebView 打开历史帖子。
- 缓存维护：小说页面缓存、漫画图片缓存、统计、单项/全量清理和定期维护。
- 数据备份：`BackupUtil` 导入/导出 zip；不备份页面/图片缓存和 WebView 登录态；导入后必须重启进程，避免运行中的 DataStore 把旧值写回。
- 应用更新：GitHub Release 检查、APK 下载/校验、系统安装器及 Releases 兜底。
- 新手引导：登录后首次进入指定原生页面和首次显示底栏时展示一次性提示。
- 崩溃兜底：记录未捕获异常；后台线程异常按现有策略吞掉，主线程异常交给系统默认处理。

## 常用命令与验证

Windows：

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat clean assembleDebug
```

Linux/macOS/CI 对应使用 `./gradlew`。

验证原则：

- Kotlin 改动至少运行 `compileDebugKotlin`。
- 改到解析、URL、会话、更新、图片策略、论坛屏蔽数据等已有测试覆盖的模块时，同时运行 `testDebugUnitTest`。
- 纯文档修改无需运行 Gradle；本次只改 `CLAUDE.md` 时不需要为了形式跑构建。
- 增量构建出现 `TaskOutputsBackup` / `IOException` 等缓存损坏时再用 `clean assembleDebug`，不要把 `clean` 作为日常默认。
- UI、WebView 生命周期、论坛模板和网络恢复无法由 JVM 测试完整覆盖；完成后应明确列出需要实机确认的行为。
- 不假定本地存在设备/模拟器或 adb 环境。

## 架构速览

### 页面与导航

- 四个底栏主页面：`MangaHomePage`（原生）、`FavoritePage`（原生）、`BBSPage`（WebView）、`MinePage`（WebView）。
- BBS/Mine 使用常驻组合并通过 `isSelected` 切换；Manga/Favorite 走 NavHost 的 saveState/restoreState。
- 主要子页面包括 `ReaderPage`、`NativeMangaPage`、`MangaWebPage`、`ReaderWebPage`、`OtherWebPage`、`HistoryPage`、`MineHistoryPostPage`。
- 历史帖子详情使用 route-scoped `MinePageVM` 和独立 WebView；不得把历史详情状态写回底栏“我的”常驻页面。

### WebView

- `WebViewPool` 负责实例复用；页面切走后通过 `schedulePause` 延迟暂停。
- WebView 暂停期间 `evaluateJavascript` 可能丢失。任何恢复路径（如 `resumeBbsWebViewAfterChildPage`、`resumeMineWebViewAfterChildPage`）都要重新注入主题和页面交互 JS。
- 从原生阅读器返回原帖时优先恢复现有 WebView，不要无条件 reload，否则会丢滚动位置并重新出现加载遮罩。
- `BBSPageState` 管理论坛页加载、超时、错误和前后台恢复；不要绕过它再造第二套状态标志。

### 阅读器

- 小说：`ReaderPage` + `ReaderVM`，加载链为 `AuthenticatedThreadPageLoader` + `ThreadHtmlParser`，缓存由 `LocalCacheUtil`、`CacheUtil` 和内存预热组件共同维护。
- 漫画：优先 `NativeMangaPage`，由 `MangaReaderManager`、`MangaImagePipeline`、`DirectoryRepository` 管理章节、图片和目录；`MangaWebPage` 兜底。
- 漫画识别链固定为 `MangaProber.fastApiProbe` → `fastHtmlProbe` → WebView 兜底；没有真实证据不要改变顺序。
- `ReaderReturnBridge` 维护阅读器与原帖 WebView 的一次性返回状态。

### 数据与后台功能

- 新手引导：`OnboardingUtil` 按 `OnboardingUtil.Page` 记录 DataStore 状态；`OnboardingOverlay` 复用。只覆盖现有原生页面和底栏，不扩展到 WebView 页。
- 收藏主状态在 `FavoriteVM`；本地数据、删除同步、墓碑队列分别由 `FavoriteUtil`、`FavoriteDeleteUtil`、`TombstoneQueueUtil` 管理。
- 收藏手动更新检查统一经过 `UpdateCheckEngine`；小说、漫画、其他帖子各有 profile/util。
- 漫画收藏版块固定 fid 30「中文百合漫画区」、fid 37「百合漫画图源区」；小说版块固定 fid 49「文學區」、fid 55「轻小说/译文区」、fid 60「TXT小说区」。未识别收藏允许手动选小说/漫画/其他；选择其他后从收藏页移出。
- 浏览历史由 `HistoryUtil` 管理；漫画目录由 `DirectoryRepository` 管理。
- 全局同类设置统一走 DataStore（`SettingsUtil` / `DataStoreUtil`）；不要新增 SharedPreferences 平行保存。阅读器现有兼容存储除外。
- 应用更新统一走 `AppUpdateManager`；自动签到走 `AutoSignManager` / `AccountSyncManager`；缓存清理由 `CacheMaintenance` 协调。
- 论坛屏蔽数据在 `ForumBlocklistManager`；注入在 `PageJsScripts.getForumBlockerJs`；JS ↔ 原生桥为 `ForumBlocklistJSInterface`（`AndroidForumBlocklist`）；管理 UI 为 `ForumBlocklistDialog`。
- 当前 uid：`CurrentUserUtil` 持久化，`GlobalData.currentUid` 做内存缓存。来源包括收藏接口 `member_uid`、桌面页 `discuz_uid`、手机版 `mycenter=1` 链接探测。
- 链接直达统一走 `YamiboPostLinkUtil`；剪贴板和 deep link 通过 `GlobalData.pendingClipboardUrl` / `pendingDeepLinkUrl` 交给 `BBSPage`。
- `CrashHandler` 在 `YamiboApplication.onCreate` 尽早安装。

### 网络

- `YamiboRetrofit.okHttpClient` 用于接口、HTML 代理和主框架；`threadOkHttpClient` 主要用于论坛图片。
- 两者共享 `sharedConnectionPool` 与 `TtlDnsCache`；`DynamicDns` 在阿里/腾讯 DoH、手动 DoH 和系统 DNS 间切换。
- WebView HTML 主题代理、静态资源代理、Cookie 同步和 Coil 缓存策略也集中在这条网络链路；修改时同时考虑原生请求与 WebView 行为。

## 已确立的行为与历史约束

这些约束通常源于已发生的实机问题。除非当前任务就是修正它，或者已有新证据证明约束过时，否则不要擅自推翻。

### 网络

- **禁止强制 HTTP/1.1**（`protocols(HTTP_1_1)`）。这曾导致 App 无法连接论坛，并与共享连接池中的 h2 连接冲突。
- `proceedWithDnsRecovery` 已处理 GET 的瞬时流重置/建连失败和 444 WAF 限流：建连类问题最多重试 3 次，444 最多重试 2 次；重试前清连接池并递增退避。不要再从协议协商层“修”同一问题。
- `TtlDnsCache` 的约 30 分钟 IP 缓存与“DNS 优化”开关不是一回事；开关决定解析器，结果仍进入同一缓存。建连失败时要在下一次重试前使当前 host 缓存失效。
- 连接池 keepalive 固定约 **50 秒**，要短于论坛服务器约 60～75 秒的空闲超时；不要改回分钟级。

### 暗黑模式 / Web CSS

- 只有经典蓝黑一套深色主题：`DarkThemeColors.CLASSIC`，主色 `#4EA1FF`、背景 `#0D141D`、面板 `#182332`；不要扩成多主题系统。
- 深色论坛规则集中在 `util/theme/DarkClassic.kt`；HTML 代理注入与运行时 JS 注入必须复用同一份 CSS。
- 浅色模式不是零注入：`util/theme/LightClassic.kt` 的规则只做必要覆盖，目前核心用途是统一正文链接颜色；不要把浅色模式改造成二次主题。
- 电脑版空间/家园页的 viewport 统一通过 `PageJsScripts.shouldUseResponsiveSpaceViewport` 决定。`#ct.ct3_a` 和 `pg_space+ct2_a+tl` 使用 1200 宽度缩放保持多栏，不要用 `float:none` 等 reflow 把它强行改单栏。
- HTML 代理与运行时 JS 对 viewport 的判断必须一致，不能一边写 1200、一边又改回 `device-width`。
- CSS 处理会把 `background:` 规范成 `background-color:`；**不要覆盖站点 `background-image`**，轮播、头像、会员自定义背景依赖它。
- 注入 CSS 字符串不要出现会破坏现有 JS 拼接方式的单引号。
- 只有真实的自定义 DIY 会员空间才跳过主题注入。判断统一走 `util/theme/MemberSpaceGuard`：`body#space` + `data/attachment` 自定义背景。不要仅按 `space-uid-N` / `mod=space&uid=` URL 排除全部空间页。
- 如果发现外链背景等新 DIY 形态，先拿真实页面样本再扩展 `MemberSpaceGuard`，不要猜。
- 投票和用户侧栏经验/积分彩条依赖内联颜色；不要用 `.plc div` / `.pls div` 这类大范围规则覆盖 `.pbr` / `.pbg` / `.pbr2` / `.pbg2`。
- 深色正文链接统一浅蓝 `#7dbdf2`；浅色正文链接统一站点默认 `#6E2B19`。链接内部 `font[color]` / inline color 只改文字色，不碰背景图。
- 新页面未适配时先拿真实 HTML，再做精确选择器修复。
- 系统 SplashScreen / 窗口开屏背景固定浅色 `@color/splash_background`（`#FCF4CF`），不随暗黑模式切换。曾尝试的 SharedPreferences + `UiModeManager.setApplicationNightMode` + `values-night` 方案已回退，不要恢复。

### 交互

- 底栏**单击 = 切换板块**；已经在本板块时不重载。**长按 = 回该板块主页**，通过 `returnToHome(notifyHome = true)` / `goHomeEvent`。不要把回主页改回单击。
- 刷新统一使用下拉手势：原生页 `PullToRefreshBox`，WebView 页 `SwipeRefreshLayout`。长按刷新已删除，不要恢复。
- 深色下拉刷新指示器背景 `#223247`、箭头 `#4EA1FF`。
- 长按底栏漫画键回 `MangaHomePage` 时只清搜索词并回顶部，不触发网络刷新。
- 小说阅读器进度只显示 `当前/总数`，不显示百分比。
- 小说阅读器标题放在点击正文后出现的菜单中，不常驻顶部。
- 从阅读器返回原帖的 URL 必须经 `ReaderReturnBridge.forceMobileTemplate` 并带 `mobile=2`。

### 权限贴（readperm）

- API 的 `readperm > 0` 只是帖子元数据。只要 API 已返回图片 URL，就说明当前账号可见，`MangaProber.fastApiProbe` 不得据此直接拦截。
- 访问拒绝检测只看 Discuz 错误元素：`#messagetext, .showmessage, .alert_error, .nfl .f_c`。
- 不要退回 body 全文关键词匹配；“阅读权限: 50”等正常帖子属性会误判。

### 漫画目录（DirectoryRepository / MangaTitleCleaner）

- 自动归并默认依据**作品名 + 汉化组**，不默认按发布账号拆目录。只有标题明确表示个人/非固定团队发布（`MangaTitleCleaner.isIndividualRelease`）时才用发布者兜底；用户手动发布者设置始终优先保留。
- “短篇集/合集/选集/总集/精选集”等集合后缀属于书名的一部分，`getCleanBookName` 需要优先保留；不要因章节清洗把集合名截残。
- 标题开头括号可能是原作/出处标注；只有括号后仍有真实标题时才剥离，避免把同 parody 的不同作品并到一个目录。
- 组名提取统一走 `extractReleaseGroup`：优先明确汉化组；必要时按论坛惯例用第一个 `【】` 制作组兜底；`[]` 不参与；组名比较做繁简归一。
- 汉化组过滤是**硬过滤**，统一复用 `DirectoryRepository.filterChaptersByDirectoryConstraints`；展示层不要另写一套。
- 首楼如果提供编号式跨帖子链接列表，视为**权威目录**：按列表顺序和标题保存，`MangaDirectory.authoritativeLinks` 标记；不要自动搜索扩展未列出的帖子。`manuallyUpdateDirectory` 对权威目录整体跳过，列表只在打开原帖时按首楼刷新。
- 已存组与当前帖子组不一致时：当前帖能识别组名则切当前组；识别不出才沿用旧组。
- 旧版标题清洗残次目录的自动迁移依赖 `isStaleCleanBookName`、`isTruncatedCleanBookName`、`isParenResidueCleanBookName`、`isParodyResidueCleanBookName` 等保护条件；不要把迁移改成无条件重命名，避免覆盖用户手工目录名。
- 两个汉化组对同一作品使用完全不同名称时，当前方案不做模糊自动归并；交给用户手动目录名/搜索关键词处理。

### 收藏、历史与缓存

- 删除收藏同时涉及论坛端、本地排序、缓存和 `TombstoneQueueUtil` 延迟同步；不能只删 UI 或只调论坛接口。
- 历史帖子详情用独立 WebView；退出后清 route-scoped 状态，不能污染 `MinePage` 常驻 WebView。
- 小说缓存以规范化 URL 和兼容别名为索引；清单本缓存要覆盖同帖 URL 兼容形式。
- 漫画缓存统计依赖实际图片 URL 集合；清单项优先精确淘汰对应 URL，不要无条件清空全局 Coil 缓存。

### 论坛屏蔽

- 只屏蔽**别人**的内容。当前登录 uid 与作者 uid 相同的主题/楼层（含一楼）不显示“屏蔽”。`view=me` 或自己的 `mod=space&do=thread/reply/favorite` 列表整页跳过。
- uid 要登录后尽早获取并通过 `CurrentUserUtil` 持久化；手机版帖子页本身不能可靠提供自身 uid，不能临时现取现用。
- 手机版列表页按钮继续复用 `.threadlist_foot li` 布局；帖子页按钮和用户名的既有间距策略不要随意改。
- 电脑版页面只隐藏、不注入屏蔽按钮；列表行由 `syncPcListPage` 处理。不要往 `<table>` 塞占位节点；电脑版帖子楼层继续复用 `syncPostPage`。

### 链接直达

- 帖子识别/归一化统一走 `YamiboPostLinkUtil`：限定 `bbs.yamibo.com`，强制移动模板 `mobile=2`，排除图片、首页等非帖子链接。
- 跳转必须经 `BBSPage.startLoading`，不要裸 `webView.loadUrl`；否则会与初始首页加载竞争，出现“停在论坛首页没反应”。
- 剪贴板只在启动/切回前台等现有时机读取，不要增加持续后台监听。

### 崩溃兜底

- `CrashHandler` 在 `YamiboApplication.onCreate` 尽早安装。
- 后台线程未捕获异常按现有策略记录后吞掉以减少整体闪退；主线程异常交给系统默认处理器。
- 日志写入 `getExternalFilesDir/crash`，维持现有保留数量策略。

## CI / 发布

以 `.github/workflows/ci.yml` 和 `.github/workflows/build-apk.yml` 的**当前内容为唯一准绳**，不要在本文件硬编码 GitHub Action 的 major 版本；Action 版本升级后无需再维护一份容易过期的副本。

当前流程语义：

- `ci.yml`：push 到 `main` 和 PR 时，在 Ubuntu + JDK 17 + Android SDK 34 环境执行 `compileDebugKotlin` 和 `testDebugUnitTest`，并显式关闭 configuration cache。
- `build-apk.yml`：`workflow_dispatch` 或 Release 发布时构建 release APK；要求完整正式签名 secrets。
- Release 触发时，`APP_VERSION_NAME` 从 tag 去掉可选 `v` 前缀；`APP_VERSION_CODE` 使用 `github.run_number`。tag 必须是版本号格式，否则构建失败。
- Release 构建先跑 `testReleaseUnitTest`，再 `assembleRelease`，之后用 `apksigner` 校验签名。
- release 产物固定为 `app/build/outputs/apk/release/300-Lite.apk`，最终附件名 `300-Lite.apk`。
- 不要让 Release 包静默回落到 `build.gradle.kts` 的默认 `versionName/versionCode`，否则应用内更新判断会出错。
- 签名材料只来自 secrets/构建环境；不要提交 keystore、密码或临时解码文件。
- 仓库虽已停止维护，但不要主动删除 workflow；只有用户明确要求调整/下线发布链路时才改。

## 修改原则

- 优先沿用现有类、状态流和工具函数，不在 Composable 中复制网络、缓存、解析或持久化逻辑。
- Lite 的默认目标是“稳定保持现状”，不是继续扩架构。修 bug 时尽量局部，不顺手抽象整个子系统。
- 修改 URL 归一化、Cookie、会话、HTML 解析、阅读器返回链接、图片策略、应用更新解析、`YamiboPostLinkUtil`、`ForumBlocklistManager` 等可测试逻辑时，补充/更新 `app/src/test` 对应测试。
- UI、WebView 生命周期、网络恢复和论坛页面兼容问题，即使单测/编译通过，也要说明实机验证点。
- 不要顺手升级 Gradle、AGP、Kotlin、Compose、OkHttp、Retrofit 等依赖；依赖升级必须是明确任务。
- 不要引入新的 DI 框架、数据库、模块化方案或导航框架来“现代化” Lite，除非用户明确要求。
- 不要提交构建产物、签名材料、`.env`、`local.properties`、临时 HTML、抓包文件或设备日志。
- 修改论坛解析/CSS/JS 时，优先加精确条件和回归测试，不用大范围 selector / regex 一次兜所有模板。

## 文档与提交约定

- README 顶部的“YamiboReaderLite 已停止维护、后续前往 YamiboPlus”警告属于当前项目状态，禁止在普通文档整理中删除或弱化。
- README 面向用户描述 Lite **现有**能力，不写成未来路线图。
- README 主体结构维持：项目简介 → 功能概览 → 界面预览 → 使用方式 → 数据与安全 → 内容边界 → 许可协议 → 反馈与贡献；不额外添加“技术信息”式开发者章节，技术细节留在本文件/源码。
- README 功能列表保持 `- 四字标签：描述。` 的扁平风格，不加粗标签、不按功能再拆三级标题。
- README 项目简介保留“以下说明仅描述 YamiboReaderLite 当前实际提供的功能”这类范围说明。
- 顶部图标使用 `icon/icon.svg`。
- 中文文档统一 UTF-8，尤其避免 PowerShell 默认编码造成乱码。
- 提交信息沿用仓库现有简短中文风格。文档类提交可直接写 `更新 CLAUDE.md 项目规范` 这类描述。

## 完成任务时的输出

完成仓库修改后，回复中至少说明：

- 改了什么，为什么这样改。
- 修改了哪些文件。
- 做了哪些验证；如果没跑 Gradle，要明确说明原因（例如纯文档改动）。
- 哪些行为仍需要实机/真实论坛页面确认。
- 如果工作属于 Lite 停止维护后的例外维护，保持说明简洁，不需要反复提醒用户迁移到 YamiboPlus。