[English](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_EN.md) | [简体中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README.md) | [日本語](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_JA.md)

# NCM 轉換器

將網易雲音樂加密的 `.ncm` 檔案解密為標準 MP3 或 FLAC 格式的 Android 應用。

## 功能

- 批次轉換，無檔案數量限制
- 自動寫入中繼資料（標題、藝術家、專輯、封面）
- 自動搜尋並嵌入歌詞，支援原文與翻譯獨立開關控制
- 系統資料夾選擇器設定輸出目錄，使用 DocumentTree 持久權限
- 背景轉換：開啟自動儲存後，轉換在前景服務中進行，通知欄顯示進度，切換應用不會中斷
- Material You 動態取色（Android 12+）
- 自訂強調色與背景色（HSL 滑桿）
- 多欄格線佈局，適用於摺疊螢幕與平板
- 預測型返回手勢支援（Android 16+）
- 雙擊返回鍵退出
- 行程意外終止後自動復原檔案列表
- 多語言支援：簡體中文、繁體中文、English、日本語

## 系統需求

- Android 8.0（API 26）及以上
- 已適配 Android 16（API 36）

## 技術棧

- Kotlin + Jetpack Compose + Material 3
- Retrofit2 + OkHttp4
- AES-128-ECB / RC4 解密
- MediaStore API / DocumentFile

## 建置

```
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

## 歌詞功能

歌詞自動嵌入功能需要搭配自建的網易雲音樂 API 服務使用。

1. 部署 [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced)
2. 在 APP 設定中通過對話框填入 API 位址，儲存後才會顯示歌詞相關選項
3. 歌詞功能開啟後，可獨立控制是否嵌入「原文歌詞」與「翻譯歌詞」
4. 如需指定地區，可填入 Real IP 參數

> 註：翻譯歌詞功能僅支援簡體中文翻譯結果。當 APP 語言不是簡體中文時，翻譯開關會顯示相應提示。

## 參考專案

本專案開發過程中參考了以下專案：

- [NLyric](https://github.com/wwh1004/NLyric) — 歌詞比對策略參考
- [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) — 歌詞/中繼資料 API 服務
- [openyyy.com](https://openyyy.com/) — NCM 格式解析參考

## 授權條款

本專案基於 [GPL v3](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/LICENSE) 授權條款開源。
