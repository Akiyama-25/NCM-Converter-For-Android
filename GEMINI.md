# 温馨提醒：无论有没有项目级 CLAUDE.md 或 GEMINI.md，都必须查看用户在 .claude 或 .gemini 目录下的全局提示词。

# NCM Converter for Android

## 项目概述

将网易云音乐加密的 `.ncm` 文件解密为标准 MP3 或 FLAC 格式的 Android 应用。支持批量转换，自动写入元数据（标题、艺术家、专辑、封面）和歌词。

- **包名**: `com.example.ncmconverter`
- **applicationId**: `Akari.Said.YuriBest`
- **当前版本**: release-0.6 (versionCode 6)
- **GitHub**: https://github.com/Akiyama-25/NCM-Converter-For-Android

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.2.10 |
| UI 框架 | Jetpack Compose + Material 3 | BOM 2025.05.01 |
| 构建工具 | AGP | 9.3.0 |
| Gradle | Wrapper | 9.5.0 |
| JDK | Azul Toolchain | 21 (target 17) |
| 异步 | Kotlin Coroutines | 1.10.2 |
| 网络 | Retrofit + OkHttp + Gson | 2.9.0 / 4.12.0 |
| 图片加载 | Coil Compose | 2.7.0 |
| Activity | activity-compose | 1.10.1 |
| Lifecycle | viewmodel-compose / runtime-compose | 2.9.1 |
| compileSdk / targetSdk | Android SDK | 36 (Android 16) |
| minSdk | | 26 (Android 8.0) |

## 架构

MVVM-lite（手动实现），无依赖注入、无 Repository 层。

```
UI Layer (Jetpack Compose)
  HomeScreen / SettingsScreen / FileItemView
  ↓ collect StateFlow
ViewModel Layer
  ConvertViewModel — 管理文件列表，协调解密/歌词/元数据
  ↓ autoSave=false: 直接转换    autoSave=true: 委托 Service
Service Layer (条件启用)
  DecryptService — 前台服务，通知栏进度，后台保活
  ↓ 直接调用
Business Logic Layer
  NcmDecryptor        — NCM 文件解密 (AES-ECB + RC4 + XOR)
  LyricMatcher         — 歌词搜索与匹配
  Mp3MetadataWriter    — ID3v2.3 标签写入
  FlacMetadataWriter   — FLAC Vorbis Comment + Picture block
  ↓
Data Layer
  Retrofit (NcmApiService) — 歌词 API 调用
  SharedPreferences (AppPrefs) — 用户偏好设置
  MediaStore / DocumentFile (FileUtils) — 文件保存（公共存储 / 用户选定目录）
```

## 核心自述与变更说明

自述文件列表：
- `README.md` (简体中文)
- `README_EN.md` (English)
- `README_JA.md` (日本語)
- `README_TC.md` (繁體中文)
- `task.md` (待办事项与完成记录)
- `CLAUDE.md` / `GEMINI.md` (Agent 指南与项目架构文档)
