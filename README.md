[English](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_EN.md) | [繁體中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_TC.md) | [日本語](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_JA.md)

# NCM 转换器

将网易云音乐加密的 `.ncm` 文件解密为标准 MP3 或 FLAC 格式的 Android 应用。

## 功能

- 批量转换，无文件数量限制
- 自动写入元数据（标题、艺术家、专辑、封面）
- 自动搜索并嵌入歌词（原文 / 翻译 / 混合）
- 后台转换：开启自动保存后，转换在前台服务中进行，通知栏显示进度，切换应用不中断
- Material You 动态取色（Android 12+）
- 自定义强调色与背景色（HSL 滑条）
- 多列网格布局，适配折叠屏与平板
- 预测性返回手势支持（Android 16+）
- 双击返回键退出
- 进程意外终止后自动恢复文件列表
- 多语言支持：简体中文、繁体中文、English、日本語

## 系统要求

- Android 8.0（API 26）及以上
- 已适配 Android 16（API 36）

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Retrofit2 + OkHttp4
- AES-128-ECB / RC4 解密
- MediaStore API

## 构建

```
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

## 歌词功能

歌词自动嵌入功能需要配合自建的网易云音乐 API 服务使用。

1. 部署 [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced)
2. 在 APP 设置中填入 API 地址
3. 如需指定地区，可填入 Real IP 参数

## 参考项目

本项目开发过程中参考了以下项目：

- [NLyric](https://github.com/wwh1004/NLyric) — 歌词匹配策略参考
- [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) — 歌词/元数据 API 服务
- [openyyy.com](https://openyyy.com/) — NCM 格式解析参考

## 许可证

本项目基于 [GPL v3](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/LICENSE) 许可证开源。
