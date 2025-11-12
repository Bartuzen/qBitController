# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

qBitController 是一个免费开源的跨平台应用程序,用于远程控制 qBittorrent 客户端。支持 Android、iOS、Windows、Linux 和 macOS。

**技术栈**: Kotlin Multiplatform (KMP) + Compose Multiplatform + Ktor + Koin

## 开发重点

**⚠️ 重要**: 当前开发重点为 iOS 平台。所有新功能、优化和修复应优先考虑 iOS 平台，暂不考虑其他平台的兼容性。代码修改应集中在 `iosMain` 源集和 iOS 相关实现。

## 开发命令

### Android
```bash
./gradlew assembleFreeDebug          # 构建 Debug 版本
./gradlew assembleFreeRelease        # 构建 Release 版本
./gradlew installFreeDebug           # 安装到设备
./gradlew generateBaselineProfile    # 生成性能基线配置
```

### Desktop (JVM)
```bash
./gradlew :composeApp:run            # 运行应用
./gradlew :composeApp:packageMsi     # Windows 打包
./gradlew :composeApp:packageDmg     # macOS 打包
./gradlew :composeApp:packageAppImage # Linux 打包
./gradlew prepareFlatpak             # Flatpak 打包准备
./gradlew bundleFlatpak              # Flatpak 打包
```

### iOS
在 Xcode 中打开: `iosApp/iosApp.xcodeproj`

### 代码质量
```bash
./gradlew formatKotlin               # 格式化代码
./gradlew lintKotlin                 # Lint 检查
./gradlew dependencyUpdates          # 检查依赖更新
```

## 代码架构

### MVVM 架构模式
- **Model**: `composeApp/src/commonMain/kotlin/.../model/` - 数据模型
- **View**: `composeApp/src/commonMain/kotlin/.../ui/` - Compose UI 组件
- **ViewModel**: `composeApp/src/commonMain/kotlin/.../ui/**/ViewModel.kt` - 状态管理
- **Repository**: `composeApp/src/commonMain/kotlin/.../data/repositories/` - 数据层
- **Network**: `composeApp/src/commonMain/kotlin/.../network/` - 网络服务

### 依赖注入
使用 Koin,配置位于 `composeApp/src/commonMain/kotlin/.../di/AppModule.kt`

### Multiplatform 源集结构
- `commonMain`: 跨平台共享代码(核心业务逻辑)
- `jvmMain`: Android + Desktop 共享代码
- `nonAndroidMain`: Desktop + iOS 共享代码
- `androidMain`: Android 特定实现
- `desktopMain`: Desktop 特定实现
- `iosMain`: iOS 特定实现

使用 `expect/actual` 机制实现平台特定功能。

### Product Flavors (Android)
- `free`: 默认版本,无 Firebase
- `firebase`: 包含 Firebase Analytics 和 Crashlytics

## 版本管理

- 应用版本和版本号在 `buildSrc/src/main/java/dev/bartuzen/qbitcontroller/Versions.kt` 中集中管理
- 依赖版本在 `gradle/libs.versions.toml` 中管理

## 自定义 Gradle 插件

位于 `buildSrc/src/main/java/dev/bartuzen/qbitcontroller/plugin/`:
- `language`: 处理多语言资源
- `ios`: 生成 iOS 配置文件

## 网络层架构

- 使用 Ktor 客户端进行 HTTP 请求
- JVM 平台使用 OkHttp 引擎
- iOS 平台使用 Darwin 引擎
- 主要 API 服务类: `TorrentService.kt`
- 请求管理: `RequestManager.kt`

## UI 组件结构

- `ui/torrentlist/`: Torrent 列表页面
- `ui/torrent/tabs/`: Torrent 详情标签页(概览、文件、跟踪器等)
- `ui/addtorrent/`: 添加 Torrent 功能
- `ui/rss/`: RSS 订阅管理
- `ui/search/`: 搜索功能
- `ui/settings/`: 应用设置
- `ui/components/`: 可复用 UI 组件

## 重要管理类

- `ServerManager.kt`: 服务器配置管理
- `SettingsManager.kt`: 应用设置管理
- `ConfigMigrator.kt`: 配置版本迁移

## 国际化

通过 Weblate 管理翻译: https://hosted.weblate.org/engage/qbitcontroller

## 编码约定

- 使用 Kotlinter 进行代码格式化和 Lint
- 遵循 Kotlin 官方编码风格
- 使用 Compose 声明式 UI 模式
- 使用 Kotlin Coroutines 进行异步操作
- 使用 kotlinx-serialization 进行 JSON 序列化
