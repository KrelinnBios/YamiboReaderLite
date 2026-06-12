# CLAUDE.md

本文件为 Claude Code 在本仓库工作时的项目规范。请始终用**中文**回复。

## 项目概况

- **YamiboReaderLite（300 Lite）**：面向百合会论坛（bbs.yamibo.com）的非官方 Android 阅读客户端，基于 [prprbell/YamiboReaderPro](https://github.com/prprbell/YamiboReaderPro) 修改而来。
- Kotlin + Jetpack Compose + Material 3；minSdk 24 / targetSdk 34 / JDK 17。
- 包名 `org.shirakawatyu.yamibo.novel`，applicationId `com.krelinnbios.yamiboreaderlite`。
- 产物固定命名 `300 Lite.apk`；应用内更新绑定 GitHub Releases（KrelinnBios/YamiboReaderLite）。

## 常用命令

```powershell
.\gradlew.bat compileDebugKotlin   # 快速编译检查（每次改完 Kotlin 必跑）
.\gradlew.bat assembleDebug        # 出可安装 APK：app\build\outputs\apk\debug\300 Lite.apk
.\gradlew.bat clean assembleDebug  # 遇到 TaskOutputsBackup/IOException 等增量构建损坏时用
```

- 本地一般没有连接设备/模拟器，无法跑 instrumented test 或 adb；运行验证靠用户实机安装反馈。
- 间歇性、网络相关的 bug 无法在本机复现，修复后必须明确请用户实测确认，不要宣称"已修复"。

## 架构速览

- **四个主页面**（底栏标签）：`MangaHomePage`（原生列表）、`FavoritePage`（原生列表）、`BBSPage`（WebView）、`MinePage`（WebView）。BBS/Mine 常驻组合并用 `isSelected` 切换；Manga/Favorite 走 NavHost saveState/restoreState。
- **WebView 体系**：`WebViewPool` 复用实例；页面切走后 `schedulePause` 延迟暂停。**WebView 暂停期间 `evaluateJavascript` 可能被丢弃**——任何"恢复页面"路径（`resumeBbsWebViewAfterChildPage` / `resumeMineWebViewAfterChildPage`）都要补注入主题 JS。
- **阅读器**：小说 `ReaderPage`（原生，`AuthenticatedThreadPageLoader` + `ThreadHtmlParser`）；漫画 `NativeMangaPage`（原生）/ `MangaWebPage`（WebView 兜底）。漫画识别链：`fastApiProbe` → `fastHtmlProbe` → WebView 兜底（`MangaProber`）。
- **网络**：`YamiboRetrofit` 两个客户端——`okHttpClient`（接口/HTML 代理/主框架）和 `threadOkHttpClient`（论坛图片）。两者共享 `sharedConnectionPool` 和 DNS 缓存。

## 已确立的决定（不要反复商量，不要推翻）

### 网络
- **禁止强制 HTTP/1.1**（`protocols(HTTP_1_1)`）：曾导致整个 App 无法连接论坛（与共享连接池的 h2 连接冲突）。
- 服务器偶发 `stream was reset: PROTOCOL_ERROR`：已在应用级拦截器对 GET 做**重试一次**处理（`proceedWithDnsRecovery`），这是认可的方案；不要再动协议协商层。

### 暗黑模式
- 只有一套深色主题：经典蓝黑（`DarkThemeColors.CLASSIC`，主色 `#4EA1FF`，底 `#0D141D`，面板 `#182332`）。不引入多主题。
- 论坛网页深色靠 CSS 注入，规则全部在 `util/theme/DarkClassic.kt`，两条注入路径共用同一份规则：加载时 HTML 代理注入（`proxyHtmlForDarkMode` + `injectThemeCssIntoHtml`）和运行时 JS 注入（`getThemeSetJs`）。
- **写规则的硬约束**：
  - 文件末尾有全局 `background:` → `background-color:` 重写，所以规则里写 `background:` 即可，**绝不能盖掉 `background-image`**（轮播图、头像、会员自定义背景都靠它）。
  - 规则字符串里**不能出现单引号**（会炸掉 JS 注入的字符串拼接）。
  - 会员个人主页（`body#space`）是会员自己设计的：背景色全部置透明、文字 inherit 会员自己的配色，**不强制深色面板**。
  - 投票区 `#poll` 保留原彩色（彩条和彩色计数都来自内联样式，`em` 全局规则已用 `:not(#poll em)` 排除）。
  - 链接深色统一浅蓝 `#7dbdf2`，不允许棕色。
- 新页面发现没适配时：让用户提供该页 HTML 片段，按选择器精准补规则，**不要写大范围通配**。

### 交互
- 底栏：**单击 = 回该板块主页**；刷新一律下拉（原生页 `PullToRefreshBox`，WebView 页 `SwipeRefreshLayout`）。长按刷新已删除，不要恢复。
- 下拉刷新指示器必须跟随暗黑模式配色（深色：底 `#223247`、箭头 `#4EA1FF`）。
- 切回 MangaHomePage **不触发网络刷新**（仅清搜索词 + 回顶部），避免网络波动把列表刷坏。
- 小说阅读器：进度只显示**页数**（`当前/总数`），不显示百分比；标题不常驻顶部，放在点击弹出的菜单中间（与漫画阅读器一致）。
- 从阅读器回"原帖"的 URL 必须带 `mobile=2`（`ReaderReturnBridge.forceMobileTemplate`），否则论坛渲染成电脑版。

### 权限贴（readperm）
- API 返回 `readperm > 0` 只是元数据，**只要返回了图片 URL 就说明有权限**，不得据此拦截（`MangaProber.fastApiProbe`）。
- 访问拒绝检测只看 Discuz 错误元素（`#messagetext, .showmessage, .alert_error, .nfl .f_c`），**绝不回退到 body 全文匹配**（"阅读权限: 50" 这类帖子属性会误判）。

### CI / 发布
- workflow 只出 release 签名包（`assembleRelease`），四个签名 secrets 缺一即 fail。
- Actions 已全部升级到 Node 24 版本（checkout@v5 / setup-java@v5 / setup-gradle@v5 / setup-android@v4 / upload-artifact@v5），并设 `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24`。不要降级。

## 其他约定

- 提交信息沿用现有风格：`更新 XXX.kt` 一类的简短中文。
- 用户会把论坛页面「另存为」的 HTML（连同 `_files/` 目录）放进仓库根目录或 Downloads 作为适配样本——它们是临时测试材料，**不要提交进 git**。
- 修 UI/CSS 前先要样本：没有对应页面的真实 HTML 时不要凭空猜选择器。
- README 功能列表格式：`- 四字标签：描述。`，不加粗。
