# CLAUDE.md

本文件记录 **YamiboReaderLite** 最终代码状态下的项目结构、实现约束与历史决策，供 AI 编码代理或开发者审阅、理解和追溯仓库时参考。默认使用**中文**回复和说明。

## 仓库状态：停止维护，准备归档

这是理解本仓库的最高优先级前提。

- **YamiboReaderLite 已停止维护，当前仓库用于保留项目最终实现状态，后续计划直接 Archive。**
- 本仓库不再承担持续开发、功能迭代、架构演进或依赖现代化任务；不要根据“以后可能继续维护”的假设提出改造方案。
- 不要把本仓库与任何后续项目建立代码同步、兼容、回移植或迁移关系。其他仓库的实现、架构和路线不构成本仓库的设计依据。
- README 顶部的停止维护说明属于当前项目状态，应保留；README 中指向其他项目的文字仅是面向用户的去向提示，不代表两个仓库存在开发联动。
- 如果用户明确要求再次修改本仓库，则以该次明确要求为准；除此之外，默认把源码视为最终快照，不主动提出新增功能、大规模重构、依赖升级或“顺便清理”。
- 本项目基于 `prprbell/YamiboReaderPro` 修改并延续 `flben233/YamiboReader` 的上游代码，不是从零开发。涉及归属、许可证或再分发时先看 `NOTICE`、`LICENSE`、`THIRD-PARTY-NOTICES.md`。

## 信息优先级

遇到描述冲突时按以下顺序判断：

1. 用户当前明确要求。
2. 当前仓库源码、测试、Gradle 配置和 `.github/workflows/`。
3. 本文件记录的实现约束和历史决定。
4. README 等面向用户的说明文字。

因此：

- **源码和配置是最终事实。** 本文件用于帮助理解它们，不应反过来覆盖已经存在的实现。
- 不要把本文件中的版本号、Action 版本或实现描述当成永久真值；如果仓库实际文件不同，以当前文件内容为准。
- 阅读代码时先搜索现有实现、调用链、状态容器和测试。项目中有不少针对百合会论坛、Discuz、WebView 和真实设备行为形成的定向修复，不能只凭常规 Android 经验判断“应该怎样”。
- 对依赖论坛 HTML、Discuz 模板、WebView 生命周期或真实网络状态的问题，没有真实样本时不要猜。
- 编译或单测通过只能证明对应静态/JVM 检查通过，不能反推真实论坛页面和设备行为一定正确。

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
- `AGENTS.md` 仅指向本文件。

## 代码地图

主源码位于：

`app/src/main/java/org/shirakawatyu/yamibo/novel/`

重点目录/入口：

- `MainActivity.kt`：主导航和大量页面级协调逻辑。文件很大，这是最终快照的一部分，不应仅为了“拆文件”进行结构重写。
- `YamiboApplication.kt`：进程级初始化，`CrashHandler` 很早安装。
- `ui/page/`：Compose / WebView 页面。
- `ui/vm/`：页面 ViewModel 和主要 UI 状态协调。
- `ui/state/`：页面状态对象。
- `ui/component/`、`ui/widget/`：复用 UI。
- `network/`：Retrofit / OkHttp / DNS / 网络代理相关逻辑。
- `parser/`：论坛 HTML、帖子和目录解析。
- `repository/`：目录等持久化/聚合逻辑。
- `util/`：缓存、设置、更新、签到、链接、WebView 脚本等跨页面工具。
- `util/theme/`：论坛网页深浅色 CSS 与会员空间守卫。
- `app/src/test/`：JVM 单元测试，覆盖解析、URL、会话、更新、图片策略等部分逻辑。
- `.github/workflows/ci.yml`：日常编译 + JVM 单测门禁。
- `.github/workflows/build-apk.yml`：手动/Release 的签名 APK 构建。

不要仅凭目录名判断职责；需要追溯行为时先搜索目标 symbol 的引用。

## 最终功能范围

以下描述的是 Lite 最终保留的能力，不是未来路线图：

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

## 构建与验证

如果只是阅读/审阅仓库，无需执行构建。只有在用户明确要求再次修改代码时，按修改范围选择验证。

Windows：

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat clean assembleDebug
```

Linux/macOS/CI 对应使用 `./gradlew`。

验证约定：

- Kotlin 改动至少运行 `compileDebugKotlin`。
- 改到解析、URL、会话、更新、图片策略、论坛屏蔽数据等已有测试覆盖的模块时，同时运行 `testDebugUnitTest`。
- 纯文档修改无需运行 Gradle。
- 增量构建出现 `TaskOutputsBackup` / `IOException` 等缓存损坏时再用 `clean assembleDebug`，不要把 `clean` 当默认命令。
- UI、WebView 生命周期、论坛模板和网络恢复无法由 JVM 测试完整覆盖，需要真实设备/页面样本才能确认。

## 架构速览

### 页面与导航

- 四个底栏主页面：`MangaHomePage`（原生）、`FavoritePage`（原生）、`BBSPage`（WebView）、`MinePage`（WebView）。
- BBS/Mine 使用常驻组合并通过 `isSelected` 切换；Manga/Favorite 走 NavHost 的 saveState/restoreState。
- 主要子页面包括 `ReaderPage`、`NativeMangaPage`、`MangaWebPage`、`ReaderWebPage`、`OtherWebPage`、`HistoryPage`、`MineHistoryPostPage`。
- 历史帖子详情使用 route-scoped `MinePageVM` 和独立 WebView；不得把历史详情状态写回底栏“我的”常驻页面。

### WebView

- `WebViewPool` 负责实例复用；页面切走后通过 `schedulePause` 延迟暂停。
- WebView 暂停期间 `evaluateJavascript` 可能丢失。恢复路径（如 `resumeBbsWebViewAfterChildPage`、`resumeMineWebViewAfterChildPage`）需要重新注入主题和页面交互 JS。
- 从原生阅读器返回原帖时优先恢复现有 WebView，不无条件 reload，否则会丢滚动位置并重新出现加载遮罩。
- `BBSPageState` 管理论坛页加载、超时、错误和前后台恢复；不要把现有行为误解成可以由另一套独立状态标志替代。

### 阅读器

- 小说：`ReaderPage` + `ReaderVM`，加载链为 `AuthenticatedThreadPageLoader` + `ThreadHtmlParser`，缓存由 `LocalCacheUtil`、`CacheUtil` 和内存预热组件共同维护。
- 漫画：优先 `NativeMangaPage`，由 `MangaReaderManager`、`MangaImagePipeline`、`DirectoryRepository` 管理章节、图片和目录；`MangaWebPage` 兜底。
- 漫画识别链为 `MangaProber.fastApiProbe` → `fastHtmlProbe` → WebView 兜底。
- `ReaderReturnBridge` 维护阅读器与原帖 WebView 的一次性返回状态。

### 数据与后台功能

- 新手引导：`OnboardingUtil` 按 `OnboardingUtil.Page` 记录 DataStore 状态；`OnboardingOverlay` 复用，只覆盖现有原生页面和底栏。
- 收藏主状态在 `FavoriteVM`；本地数据、删除同步、墓碑队列分别由 `FavoriteUtil`、`FavoriteDeleteUtil`、`TombstoneQueueUtil` 管理。
- 收藏手动更新检查统一经过 `UpdateCheckEngine`；小说、漫画、其他帖子各有 profile/util。
- 漫画收藏版块固定 fid 30「中文百合漫画区」、fid 37「百合漫画图源区」；小说版块固定 fid 49「文學區」、fid 55「轻小说/译文区」、fid 60「TXT小说区」。未识别收藏允许手动选小说/漫画/其他；选择其他后从收藏页移出。
- 浏览历史由 `HistoryUtil` 管理；漫画目录由 `DirectoryRepository` 管理。
- 全局同类设置统一走 DataStore（`SettingsUtil` / `DataStoreUtil`）；阅读器另有现存兼容存储。
- 应用更新统一走 `AppUpdateManager`；自动签到走 `AutoSignManager` / `AccountSyncManager`；缓存清理由 `CacheMaintenance` 协调。
- 论坛屏蔽数据在 `ForumBlocklistManager`；注入在 `PageJsScripts.getForumBlockerJs`；JS ↔ 原生桥为 `ForumBlocklistJSInterface`（`AndroidForumBlocklist`）；管理 UI 为 `ForumBlocklistDialog`。
- 当前 uid：`CurrentUserUtil` 持久化，`GlobalData.currentUid` 做内存缓存。来源包括收藏接口 `member_uid`、桌面页 `discuz_uid`、手机版 `mycenter=1` 链接探测。
- 链接直达统一走 `YamiboPostLinkUtil`；剪贴板和 deep link 通过 `GlobalData.pendingClipboardUrl` / `pendingDeepLinkUrl` 交给 `BBSPage`。
- `CrashHandler` 在 `YamiboApplication.onCreate` 尽早安装。

### 网络

- `YamiboRetrofit.okHttpClient` 用于接口、HTML 代理和主框架；`threadOkHttpClient` 主要用于论坛图片。
- 两者共享 `sharedConnectionPool` 与 `TtlDnsCache`；`DynamicDns` 在阿里/腾讯 DoH、手动 DoH 和系统 DNS 间切换。
- WebView HTML 主题代理、静态资源代理、Cookie 同步和 Coil 缓存策略也集中在这条网络链路。

## 已确立的行为与历史约束

以下内容主要用于解释最终代码为什么这样写。它们通常来自真实论坛页面、实机问题或已经发生过的回归，不应仅凭“常规最佳实践”判断为多余逻辑。

### 网络

- **禁止强制 HTTP/1.1**（`protocols(HTTP_1_1)`）。这曾导致 App 无法连接论坛，并与共享连接池中的 h2 连接冲突。
- `proceedWithDnsRecovery` 处理 GET 的瞬时流重置/建连失败和 444 WAF 限流：建连类问题最多重试 3 次，444 最多重试 2 次；重试前清连接池并递增退避。
- `TtlDnsCache` 的约 30 分钟 IP 缓存与“DNS 优化”开关不是一回事；开关决定解析器，结果仍进入同一缓存。建连失败时会在下一次重试前使当前 host 缓存失效。
- 连接池 keepalive 固定约 **50 秒**，短于论坛服务器约 60～75 秒的空闲超时，避免切回 App 时复用半死连接。

### 暗黑模式 / Web CSS

- 只有经典蓝黑一套深色主题：`DarkThemeColors.CLASSIC`，主色 `#4EA1FF`、背景 `#0D141D`、面板 `#182332`。
- 深色论坛规则集中在 `util/theme/DarkClassic.kt`；HTML 代理注入与运行时 JS 注入复用同一份 CSS。
- 浅色模式不是零注入：`util/theme/LightClassic.kt` 只做必要覆盖，核心用途是统一正文链接颜色。
- 电脑版空间/家园页的 viewport 通过 `PageJsScripts.shouldUseResponsiveSpaceViewport` 决定。`#ct.ct3_a` 和 `pg_space+ct2_a+tl` 使用 1200 宽度缩放保持多栏，而不是 reflow 成单栏。
- HTML 代理与运行时 JS 对 viewport 的判断需要一致。
- CSS 处理会把 `background:` 规范成 `background-color:`；不能覆盖站点 `background-image`，轮播、头像、会员自定义背景依赖它。
- 注入 CSS 字符串不能出现会破坏现有 JS 拼接方式的单引号。
- 只有真实的自定义 DIY 会员空间跳过主题注入。判断统一走 `util/theme/MemberSpaceGuard`：`body#space` + `data/attachment` 自定义背景，而不是仅按空间 URL 判断。
- 投票和用户侧栏经验/积分彩条依赖内联颜色；大范围 `.plc div` / `.pls div` 规则会误伤 `.pbr` / `.pbg` / `.pbr2` / `.pbg2`。
- 深色正文链接统一浅蓝 `#7dbdf2`；浅色正文链接统一站点默认 `#6E2B19`。链接内部 `font[color]` / inline color 只改文字色，不碰背景图。
- 系统 SplashScreen / 窗口开屏背景固定浅色 `@color/splash_background`（`#FCF4CF`），不随暗黑模式切换。曾尝试的 SharedPreferences + `UiModeManager.setApplicationNightMode` + `values-night` 方案已回退。

### 交互

- 底栏**单击 = 切换板块**；已经在本板块时不重载。**长按 = 回该板块主页**，通过 `returnToHome(notifyHome = true)` / `goHomeEvent`。
- 刷新统一使用下拉手势：原生页 `PullToRefreshBox`，WebView 页 `SwipeRefreshLayout`。长按刷新已删除。
- 深色下拉刷新指示器背景 `#223247`、箭头 `#4EA1FF`。
- 长按底栏漫画键回 `MangaHomePage` 时只清搜索词并回顶部，不触发网络刷新。
- 小说阅读器进度只显示 `当前/总数`，不显示百分比。
- 小说阅读器标题放在点击正文后出现的菜单中，不常驻顶部。
- 从阅读器返回原帖的 URL 经 `ReaderReturnBridge.forceMobileTemplate` 并带 `mobile=2`。

### 权限贴（readperm）

- API 的 `readperm > 0` 只是帖子元数据。只要 API 已返回图片 URL，就说明当前账号可见，`MangaProber.fastApiProbe` 不据此直接拦截。
- 访问拒绝检测只看 Discuz 错误元素：`#messagetext, .showmessage, .alert_error, .nfl .f_c`。
- 不使用 body 全文关键词匹配；“阅读权限: 50”等正常帖子属性会误判。

### 漫画目录（DirectoryRepository / MangaTitleCleaner）

- 自动归并默认依据**作品名 + 汉化组**，不默认按发布账号拆目录。只有标题明确表示个人/非固定团队发布（`MangaTitleCleaner.isIndividualRelease`）时才用发布者兜底；用户手动发布者设置始终优先保留。
- “短篇集/合集/选集/总集/精选集”等集合后缀属于书名的一部分，`getCleanBookName` 需要优先保留。
- 标题开头括号可能是原作/出处标注；只有括号后仍有真实标题时才剥离，避免把同 parody 的不同作品并到一个目录。
- 组名提取统一走 `extractReleaseGroup`：优先明确汉化组；必要时按论坛惯例用第一个 `【】` 制作组兜底；`[]` 不参与；组名比较做繁简归一。
- 汉化组过滤是**硬过滤**，统一复用 `DirectoryRepository.filterChaptersByDirectoryConstraints`。
- 首楼如果提供编号式跨帖子链接列表，视为**权威目录**：按列表顺序和标题保存，`MangaDirectory.authoritativeLinks` 标记；不自动搜索扩展未列出的帖子。`manuallyUpdateDirectory` 对权威目录整体跳过，列表只在打开原帖时按首楼刷新。
- 已存组与当前帖子组不一致时：当前帖能识别组名则切当前组；识别不出才沿用旧组。
- 旧版标题清洗残次目录的自动迁移依赖 `isStaleCleanBookName`、`isTruncatedCleanBookName`、`isParenResidueCleanBookName`、`isParodyResidueCleanBookName` 等保护条件，以免覆盖用户手工目录名。
- 两个汉化组对同一作品使用完全不同名称时，最终实现不做模糊自动归并，交给用户手动目录名/搜索关键词处理。

### 收藏、历史与缓存

- 删除收藏同时涉及论坛端、本地排序、缓存和 `TombstoneQueueUtil` 延迟同步。
- 历史帖子详情用独立 WebView；退出后清 route-scoped 状态，不能污染 `MinePage` 常驻 WebView。
- 小说缓存以规范化 URL 和兼容别名为索引；清单本缓存覆盖同帖 URL 兼容形式。
- 漫画缓存统计依赖实际图片 URL 集合；清单项优先精确淘汰对应 URL，而不是无条件清空全局 Coil 缓存。

### 论坛屏蔽

- 只屏蔽**别人**的内容。当前登录 uid 与作者 uid 相同的主题/楼层（含一楼）不显示“屏蔽”。`view=me` 或自己的 `mod=space&do=thread/reply/favorite` 列表整页跳过。
- uid 登录后尽早获取并通过 `CurrentUserUtil` 持久化；手机版帖子页本身不能可靠提供自身 uid。
- 手机版列表页按钮复用 `.threadlist_foot li` 布局；帖子页按钮和用户名使用既有间距策略。
- 电脑版页面只隐藏、不注入屏蔽按钮；列表行由 `syncPcListPage` 处理，不往 `<table>` 塞占位节点；电脑版帖子楼层复用 `syncPostPage`。

### 链接直达

- 帖子识别/归一化统一走 `YamiboPostLinkUtil`：限定 `bbs.yamibo.com`，强制移动模板 `mobile=2`，排除图片、首页等非帖子链接。
- 跳转经 `BBSPage.startLoading`，而不是裸 `webView.loadUrl`；否则会与初始首页加载竞争，出现“停在论坛首页没反应”。
- 剪贴板只在启动/切回前台等现有时机读取，没有持续后台监听。

### 崩溃兜底

- `CrashHandler` 在 `YamiboApplication.onCreate` 尽早安装。
- 后台线程未捕获异常按现有策略记录后吞掉以减少整体闪退；主线程异常交给系统默认处理器。
- 日志写入 `getExternalFilesDir/crash`，按现有数量策略保留。

## CI / 发布快照

以 `.github/workflows/ci.yml` 和 `.github/workflows/build-apk.yml` 的**当前内容为准**。本节只记录最终仓库中这两条流程的语义，不为未来版本升级制定规则。

- `ci.yml`：push 到 `main` 和 PR 时，在 Ubuntu + JDK 17 + Android SDK 34 环境执行 `compileDebugKotlin` 和 `testDebugUnitTest`，并显式关闭 configuration cache。
- `build-apk.yml`：`workflow_dispatch` 或 Release 发布时构建 release APK；要求完整正式签名 secrets。
- Release 触发时，`APP_VERSION_NAME` 从 tag 去掉可选 `v` 前缀；`APP_VERSION_CODE` 使用 `github.run_number`。tag 必须是版本号格式。
- Release 构建先跑 `testReleaseUnitTest`，再 `assembleRelease`，之后用 `apksigner` 校验签名。
- release 产物固定为 `app/build/outputs/apk/release/300-Lite.apk`，最终附件名 `300-Lite.apk`。
- 签名材料来自 secrets/构建环境，不存在于仓库源码中。

## 如需再次修改本仓库

正常情况下不再修改；本节只规定用户明确要求再次改动时的边界。

- 优先沿用现有类、状态流和工具函数，不在 Composable 中复制网络、缓存、解析或持久化逻辑。
- 以最小 diff 为原则，不把一次定向修改扩展成架构清理。
- 修改 URL 归一化、Cookie、会话、HTML 解析、阅读器返回链接、图片策略、应用更新解析、`YamiboPostLinkUtil`、`ForumBlocklistManager` 等可测试逻辑时，补充/更新 `app/src/test` 对应测试。
- UI、WebView 生命周期、网络恢复和论坛页面兼容问题，即使单测/编译通过，也需要真实设备或页面样本确认。
- 不顺手升级 Gradle、AGP、Kotlin、Compose、OkHttp、Retrofit 等依赖。
- 不为了“现代化”引入新的 DI 框架、数据库、模块化方案或导航框架。
- 不提交构建产物、签名材料、`.env`、`local.properties`、临时 HTML、抓包文件或设备日志。
- 修改论坛解析/CSS/JS 时，优先使用真实页面样本和精确条件，不用大范围 selector / regex 猜测所有模板。

## 文档约定

- README 顶部停止维护警告属于项目最终状态，不在普通文档整理中删除或弱化。
- README 面向用户描述 Lite **已经存在的能力**，不写未来路线图。
- README 主体结构维持：项目简介 → 功能概览 → 界面预览 → 使用方式 → 数据与安全 → 内容边界 → 许可协议 → 反馈与贡献；技术细节留在源码和本文件。
- README 功能列表保持 `- 四字标签：描述。` 的扁平风格，不加粗标签、不按功能再拆三级标题。
- README 项目简介保留“以下说明仅描述 YamiboReaderLite 当前实际提供的功能”这类范围说明。
- 顶部图标使用 `icon/icon.svg`。
- 中文文档统一 UTF-8，尤其避免 PowerShell 默认编码造成乱码。
- 提交信息沿用仓库现有简短中文风格。

## 回答本仓库相关问题时

- 把 YamiboReaderLite 当作**已经完成并停止维护的独立历史项目**讨论。
- 解释设计时以本仓库自身代码和历史问题为依据，不拿其他项目的现状反推 Lite 应该怎样设计。
- 除非用户主动询问，不需要讨论其他仓库、迁移计划或后续开发。
- 如果只是代码审阅、历史追溯或功能解释，直接回答当前实现，不额外提出“下一步重构/升级”建议。
