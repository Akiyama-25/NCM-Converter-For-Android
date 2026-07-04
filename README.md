# NCM 转换器

一款 Android 工具，用于将网易云音乐加密的 `.ncm` 文件解密还原为标准 MP3/FLAC 音频文件。

## 功能

- **NCM 解密**：将 `.ncm` 文件解密为 MP3 或 FLAC 格式
- **批量转换**：支持同时选择最多 50 个文件批量处理
- **元数据写入**：自动写入歌曲标题、艺术家、专辑、封面等信息
- **歌词嵌入**：自动从网易云获取歌词并嵌入音频文件 支持原文/翻译/混合模式 前提是网易云中已经有对应歌曲的歌词/翻译
- **大文件支持**：流式解密架构，无内存上限，可处理任意大小的文件
- **主题切换**：支持跟随系统 / 浅色 / 深色三种主题

## 系统要求

- Android 8.0 (API 26) 及以上

## 技术栈

- Kotlin + Jetpack Compose
- Retrofit2 + OkHttp4
- AES-128-ECB / RC4 解密
- MediaStore API

## 歌词功能

歌词自动嵌入功能需要配合自建的网易云音乐 API 服务使用。

1. 部署 [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced)
2. 在 APP 设置中填入 API 地址
3. 如需指定地区，可填入 Real IP 参数

## 构建

```bash
./gradlew assembleRelease
```

## 参考项目

本项目开发过程中参考了以下项目：

- [NLyric](https://github.com/wwh1004/NLyric) — 歌词匹配策略参考
- [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) — 歌词/元数据 API 服务
- [openyyy.com](https://openyyy.com/) — NCM 格式解析参考

## 免责声明

本工具仅供学习交流使用。解密后的文件请勿用于商业用途或非法传播。所有文件解密均在设备本地完成，应用不会上传任何文件到互联网。

## 许可

本项目仅供个人学习使用。
