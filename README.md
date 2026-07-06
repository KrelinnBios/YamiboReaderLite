# YamiboReaderLite

<p align="center">
  <img src="icon/logo.svg" width="128" alt="300 Lite logo">
</p>

<p align="center">
  <strong>300 Lite</strong><br>
  面向百合会论坛的非官方 Android 阅读客户端
</p>

<p align="center">
  <a href="https://github.com/KrelinnBios/YamiboReaderLite/releases"><img src="https://img.shields.io/github/v/release/KrelinnBios/YamiboReaderLite?style=flat-square&label=%E4%B8%8B%E8%BD%BD&color=2f6f73" alt="最新版本"></a>
  <img src="https://img.shields.io/badge/平台-Android%207.0%2B-247344?style=flat-square" alt="Android 7.0+">
  <img src="https://img.shields.io/badge/许可-AGPL--3.0-1f5f9c?style=flat-square" alt="AGPL-3.0 License">
</p>

> [!IMPORTANT]
> YamiboReaderLite 基于 [prprbell/YamiboReaderPro](https://github.com/prprbell/YamiboReaderPro) 修改和继续开发，并非从零开发的独立项目。感谢原作者及所有上游贡献者。

## 项目简介

YamiboReaderLite 是面向百合会论坛的非官方 Android 阅读客户端。Lite 版本围绕移动端阅读体验，整合论坛浏览、收藏管理、浏览历史、小说阅读和漫画阅读等功能。

以下说明仅描述 YamiboReaderLite 当前实际提供的功能。

## 功能概览

- 论坛浏览：登录账号浏览百合会论坛，支持自动签到、深色显示和简繁切换。
- 漫画发现：浏览和搜索中文漫画区、漫画图源区，并管理本地漫画目录。
- 漫画阅读：支持章节切换、进度记录、图片缓存、图片缩放、亮度调节和多种阅读方向。
- 小说阅读：支持字号、行距、页边距、翻页方式、正文图片、章节目录和进度记录。
- 收藏管理：同步论坛收藏，支持分类、搜索、置顶、隐藏、删除、更新检查和缓存清理。
- 浏览历史：记录浏览过的帖子，支持日期筛选、组合搜索和删除。
- 链接直达：可识别百合会帖子链接，并从剪贴板或其他应用直接打开对应帖子。
- 论坛屏蔽：可屏蔽指定主题、楼层或用户，并管理黑名单。
- 数据备份：支持导出和导入收藏排序、漫画目录、阅读进度、浏览历史、黑名单和设置。
- 应用设置：提供主题、语言、缓存、更新检查、问题反馈等常用入口。

## 界面预览

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

## 使用方式

### 安装使用

从 [Releases](https://github.com/KrelinnBios/YamiboReaderLite/releases) 下载 APK 后安装。

### 系统要求

Android 7.0（API 24）及以上。

当前 APK 仅提供 `arm64-v8a` 和 `armeabi-v7a` 架构版本。

### 更新方式

应用可在启动时或设置页检查新版本。检测到更新后，可在应用内下载并安装，也可以前往 Releases 页面手动下载。

若系统提示无法覆盖安装，通常需要先卸载旧版，再从 Releases 重新安装。

## 数据与安全

- 登录状态、收藏、历史和缓存等数据保存在设备本地或来自百合会论坛账号本身。
- 请只从本仓库 Releases 或可信来源安装 APK，避免使用来源不明的改包版本。
- 清理应用数据、卸载应用或更换设备可能导致本地历史、缓存和设置丢失。

## 内容边界

- 本项目为非官方客户端，与百合会论坛运营方无隶属关系。
- 请遵守目标论坛规则、版权要求以及所在地法律法规。
- 论坛内容、图片和用户发表的信息来自原站点，其版权与内容责任归原始来源所有。
- 本项目基于上游项目继续开发，相关来源与许可证信息请同时参考本仓库的 [LICENSE](./LICENSE) 和 [NOTICE](./NOTICE)。

## 许可协议

本项目依据 [GNU AGPL-3.0](./LICENSE) 发布。

第三方资源以其原作者或原项目的许可证声明为准。

相关项目：

- [prprbell/YamiboReaderPro](https://github.com/prprbell/YamiboReaderPro)
- [flben233/YamiboReader](https://github.com/flben233/YamiboReader)
- [duck123ducker/yamibo_manga_reader](https://github.com/duck123ducker/yamibo_manga_reader)（参考）
- [LittleSurvival/yamibo-app](https://github.com/LittleSurvival/yamibo-app)（Android 客户端）
- [Arkalin/YamiboReaderSwift](https://github.com/Arkalin/YamiboReaderSwift)（SwiftUI iOS 客户端）

## 反馈与贡献

欢迎通过 [GitHub Issue](https://github.com/KrelinnBios/YamiboReaderLite/issues) 提交使用问题、兼容性问题、功能建议或其他改进建议。
