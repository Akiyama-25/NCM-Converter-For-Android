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
- 內建 Resource Han Round SC 字型，支援 6 檔字重自訂調節
- 全域 AOSP 預見性返回手勢跟手縮放預覽（返回透出底層頁面，支援手勢跟手縮放、動態圓角卡片與彈性回彈）
- 雙擊返回鍵退出
- 行程意外終止後自動復原檔案列表
- 多語言支援：簡體中文、繁體中文、English、日本語

<p align="center">
  <img src="docs/screenshots/screenshot_1.jpg" alt="Screenshot 1" width="30%" />
  <img src="docs/screenshots/screenshot_2.jpg" alt="Screenshot 2" width="30%" />
  <img src="docs/screenshots/screenshot_3.jpg" alt="Screenshot 3" width="30%" />
  <br><br>
  <img src="docs/screenshots/screenshot_4.jpg" alt="Screenshot 4" width="30%" />
  <img src="docs/screenshots/screenshot_5.png" alt="Screenshot 5" width="30%" />
  <img src="docs/screenshots/screenshot_6.png" alt="Screenshot 6" width="30%" />
</p>

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

歌詞自動嵌入功能已內建網易雲官方介面直連，開箱即用，無需額外架設外部雲端服務。

1. APP 預設使用內建官方直連 API，無需額外設定即可自動取得並嵌入歌詞與封面。
2. 歌詞功能開啟後，可獨立控制是否嵌入「原文歌詞」與「翻譯歌詞」（若同時開啟將自動逐行對齊合併）。

> 註：翻譯歌詞功能僅支援簡體中文翻譯結果。當 APP 語言不是簡體中文時，翻譯開關會顯示相應提示。

## 參考專案

本專案開發過程中參考了以下專案：

- [rinchao0721/Melodia](https://github.com/rinchao0721/Melodia) — 提供了 App 內嵌純 Kotlin 加密直連網易雲官方 API 的架構思路與協定參考
- [Ame-Player](https://github.com/Akiyama-25/Ame-Player) — 客戶端內建 NetEase 官方加密直連與建置簽名工作流程參考
- [NLyric](https://github.com/wwh1004/NLyric) — 歌詞比對策略參考
- [openyyy.com](https://openyyy.com/) — NCM 格式解析參考

## 授權條款

本專案基於 [GPL v3](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/LICENSE) 授權條款開源。
