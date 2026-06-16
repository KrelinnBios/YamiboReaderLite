# YamiboReaderLite

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/ic_launcher_playstore.png" width="120" alt="300 Lite">
</p>


<p align="center">
  <strong>300 Lite</strong><br>
  面向百合会论坛的非官方 Android 阅读客户端
</p>


> [!IMPORTANT]
> YamiboReaderLite 基于 [prprbell/YamiboReaderPro](https://github.com/prprbell/YamiboReaderPro) 修改和继续开发，并非从零开发的独立项目。感谢原作者及所有上游贡献者。

当前 Lite 版本将百合会论坛浏览、收藏管理、浏览历史、小说阅读和漫画阅读整合在同一个应用中。其界面、功能和行为已经过调整，与上游项目并不完全相同；以下说明仅描述 YamiboReaderLite 当前实际提供的功能。

## 内容与功能

- 暗黑模式：经典蓝黑配色，原生界面与论坛网页（含电脑版帖子页）同步切换深色，可在设置中一键开关。
- 底栏交互：下拉刷新当前页面；点击底栏图标可返回对应板块，长按则直接回到板块主页。
- 论坛浏览：登录账号浏览百合会论坛，支持自动签到和 DNS 网络优化。
- 链接直达：复制百合会帖子链接后回到应用即可一键打开，也支持从其他应用直接用本应用打开帖子链接。
- 论坛屏蔽：屏蔽指定主题、楼层或用户，黑名单支持搜索与筛选。
- 漫画发现：浏览和搜索中文漫画区、漫画图源区，整理并更新漫画目录。
- 漫画阅读：支持章节切换、进度记录、亮度调节和三种阅读方向。
- 小说阅读：支持字号、行距、页边距、翻页方式、正文图片和简繁转换。
- 收藏管理：分类、搜索、置顶、更新检查和缓存清理。
- 浏览历史：记录浏览历史，支持日期筛选、组合搜索和删除。
- 缓存维护：小说页面与漫画图片缓存、定期清理和版本更新检查。
- 崩溃兜底：拦截后台线程异常以减少闪退，并记录崩溃日志便于排查问题。

## 下载

从 [Releases](https://github.com/KrelinnBios/YamiboReaderLite/releases) 下载 APK。

应用启动时会通过 GitHub Releases API 检查新版本，也可以在设置页随时手动检查更新。检测到更新后可以在应用内下载并调起系统安装器；如果自动检查、下载或安装器启动失败，应用会提供 Releases 手动下载入口。

系统要求：Android 7.0（API 24）及以上。

## 截图

<p align="center">
  <img src="screenshots/1.png" width="19%" alt="漫画发现">
  <img src="screenshots/2.png" width="19%" alt="漫画目录编辑">
  <img src="screenshots/3.png" width="19%" alt="收藏管理">
  <img src="screenshots/4.png" width="19%" alt="小说阅读">
  <img src="screenshots/5.png" width="19%" alt="浏览历史">
</p>


<p align="center">
  <img src="screenshots/6.png" width="19%" alt="论坛浏览">
  <img src="screenshots/7.png" width="19%" alt="论坛屏蔽">
  <img src="screenshots/8.png" width="19%" alt="个人中心">
  <img src="screenshots/9.png" width="19%" alt="应用设置">
  <img src="screenshots/10.png" width="19%" alt="黑名单">
</p>


## 内容边界

本项目与百合会论坛运营方无隶属关系，请遵守论坛规则及所在地法律法规。

## 许可协议

本项目依据 [GNU AGPL-3.0](LICENSE) 发布。

相关项目：

- [prprbell/YamiboReaderPro](https://github.com/prprbell/YamiboReaderPro)
- [flben233/YamiboReader](https://github.com/flben233/YamiboReader)
- [duck123ducker/yamibo_manga_reader](https://github.com/duck123ducker/yamibo_manga_reader)（参考）

## 反馈与贡献

欢迎通过 [GitHub Issue](https://github.com/KrelinnBios/YamiboReaderLite/issues) 提交使用问题、兼容性问题、功能建议或其他改进建议。
