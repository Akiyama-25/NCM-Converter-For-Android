# NCM Converter for Android

<p align="center">
  <a href="#简体中文">简体中文</a> ·
  <a href="#繁體中文">繁體中文</a> ·
  <a href="#english">English</a> ·
  <a href="#日本語">日本語</a>
</p>

---

## 简体中文

### 简介

将网易云音乐加密的 `.ncm` 文件解密为标准 MP3 或 FLAC 格式的 Android 应用。

### 功能

- 批量转换，最多同时处理 50 个文件
- 自动写入元数据（标题、艺术家、专辑、封面）
- 自动搜索并嵌入歌词（原文 / 翻译 / 混合）
- Material You 动态取色（Android 12+）
- 自定义强调色与背景色（HSL 滑条）
- 多列网格布局，适配折叠屏与平板
- 多语言支持：简体中文、繁体中文、English、日本語

### 系统要求

- Android 8.0（API 26）及以上

### 构建

```bash
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

### 歌词功能

歌词功能依赖自建的 NeteaseCloudMusicApiEnhanced 服务。部署后在设置中配置 API 地址即可使用。

参考：[NeteaseCloudMusicApiEnhanced](https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced)

---

## 繁體中文

### 簡介

將網易雲音樂加密的 `.ncm` 檔案解密為標準 MP3 或 FLAC 格式的 Android 應用。

### 功能

- 批次轉換，最多同時處理 50 個檔案
- 自動寫入中繼資料（標題、藝術家、專輯、封面）
- 自動搜尋並嵌入歌詞（原文 / 翻譯 / 混合）
- Material You 動態取色（Android 12+）
- 自訂強調色與背景色（HSL 滑條）
- 多欄格線佈局，適用於摺疊螢幕與平板
- 多語言支援：簡體中文、繁體中文、English、日本語

### 系統需求

- Android 8.0（API 26）及以上

### 建置

```bash
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

### 歌詞功能

歌詞功能依賴自建的 NeteaseCloudMusicApiEnhanced 服務。部署後在設定中配置 API 網址即可使用。

參考：[NeteaseCloudMusicApiEnhanced](https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced)

---

## English

### Overview

An Android app that decrypts NetEase Cloud Music's encrypted `.ncm` files into standard MP3 or FLAC format.

### Features

- Batch conversion, up to 50 files at once
- Automatic metadata writing (title, artist, album, cover art)
- Automatic lyric search and embedding (original / translated / merged)
- Material You dynamic colors (Android 12+)
- Custom accent and background colors via HSL sliders
- Multi-column grid layout, optimized for foldables and tablets
- Multi-language support: Simplified Chinese, Traditional Chinese, English, Japanese

### Requirements

- Android 8.0 (API 26) or higher

### Build

```bash
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

### Lyrics

Lyrics require a self-hosted NeteaseCloudMusicApiEnhanced instance. Deploy it and configure the API URL in Settings.

Reference: [NeteaseCloudMusicApiEnhanced](https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced)

---

## 日本語

### 概要

NetEase Cloud Music で暗号化された `.ncm` ファイルを標準的な MP3 または FLAC 形式に復号する Android アプリです。

### 機能

- バッチ変換、最大50ファイル同時処理
- メタデータ自動書き込み（タイトル、アーティスト、アルバム、カバーアート）
- 歌詞の自動検索・埋め込み（原文 / 翻訳 / 混合）
- Material You ダイナミックカラー（Android 12+）
- HSL スライダーによるアクセントカラー・背景色のカスタマイズ
- マルチカラムグリッドレイアウト、折りたたみ端末・タブレット対応
- 多言語対応：簡体字中国語、繁体字中国語、English、日本語

### 動作要件

- Android 8.0（API 26）以上

### ビルド方法

```bash
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

### 歌詞機能

歌詞機能はセルフホストの NeteaseCloudMusicApiEnhanced サービスに依存します。デプロイ後、設定画面で API URL を設定してください。

参考：[NeteaseCloudMusicApiEnhanced](https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced)
