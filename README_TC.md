[English](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_EN.md) | [简体中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README.md) | [日本語](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_JA.md)

# NCM 轉換器

將網易雲音樂加密的 `.ncm` 檔案解密為標準 MP3 或 FLAC 格式的 Android 應用。

## 功能

- 批次轉換，最多同時處理 50 個檔案
- 自動寫入中繼資料（標題、藝術家、專輯、封面）
- 自動搜尋並嵌入歌詞（原文 / 翻譯 / 混合）
- Material You 動態取色（Android 12+）
- 自訂強調色與背景色（HSL 滑桿）
- 多欄格線佈局，適用於摺疊螢幕與平板
- 多語言支援：簡體中文、繁體中文、English、日本語

## 系統需求

- Android 8.0（API 26）及以上

## 技術棧

- Kotlin + Jetpack Compose
- Retrofit2 + OkHttp4
- AES-128-ECB / RC4 解密
- MediaStore API

## 建置

```
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

## 歌詞功能

歌詞自動嵌入功能需要搭配自建的網易雲音樂 API 服務使用。

1. 部署 [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced)
2. 在 APP 設定中填入 API 位址
3. 如需指定地區，可填入 Real IP 參數

## 參考專案

本專案開發過程中參考了以下專案：

- [NLyric](https://github.com/wwh1004/NLyric) — 歌詞比對策略參考
- [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) — 歌詞/中繼資料 API 服務
- [openyyy.com](https://openyyy.com/) — NCM 格式解析參考

## 授權條款

本專案基於 [GPL v3](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/LICENSE) 授權條款開源。
