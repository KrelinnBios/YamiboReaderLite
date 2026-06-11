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

- 浏览百合会论坛，支持账号登录、自动签到和 DNS 网络优化。
- 浏览和搜索中文漫画区、漫画图源区，整理并更新漫画目录。
- 原生漫画阅读，支持章节切换、进度记录、亮度调节和三种阅读方向。
- 原生小说阅读，支持字号、行距、页边距、翻页方式、正文图片和简繁转换。
- 收藏分类、搜索、排序、置顶、隐藏、更新检查和缓存清理。
- 浏览历史搜索、日期筛选和删除。
- 小说页面与漫画图片缓存、定期清理和版本更新检查。

## 下载

从 [Releases](https://github.com/KrelinnBios/YamiboReaderLite/releases) 下载 APK。

系统要求：Android 7.0（API 24）及以上。

## 截图

<p align="center">
  <img src="screenshots/1.png" width="19%" alt="漫画首页">
  <img src="screenshots/2.png" width="19%" alt="收藏页面">
  <img src="screenshots/3.png" width="19%" alt="收藏菜单">
  <img src="screenshots/4.png" width="19%" alt="论坛首页">
  <img src="screenshots/5.png" width="19%" alt="应用设置">
</p>

<p align="center">
  <img src="screenshots/6.png" width="19%" alt="浏览历史">
  <img src="screenshots/7.png" width="19%" alt="漫画阅读">
  <img src="screenshots/8.png" width="19%" alt="漫画信息更新">
  <img src="screenshots/9.png" width="19%" alt="小说阅读">
  <img src="screenshots/10.png" width="19%" alt="小说阅读设置">
</p>

## 构建

需要 JDK 17 和 Android SDK 34。

```bash
./gradlew assembleDebug
```

## 内容边界

本项目与百合会论坛运营方无隶属关系，请遵守论坛规则及所在地法律法规。

## 许可协议

本项目依据 [GNU AGPL-3.0](LICENSE) 发布，版权信息见 [NOTICE](NOTICE)。

## 反馈与贡献

欢迎通过 [GitHub Issue](https://github.com/KrelinnBios/YamiboReaderLite/issues) 提交使用问题、兼容性问题、功能建议或其他改进建议。
