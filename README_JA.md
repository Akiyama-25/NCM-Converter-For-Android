[简体中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README.md) | [繁體中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_TC.md) | [English](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_EN.md)

この文書は「**Claude**」によって翻訳されました。

# NCM コンバーター

NetEase Cloud Music で暗号化された `.ncm` ファイルを標準的な MP3 または FLAC 形式に復号する Android アプリです。

## 機能

- バッチ変換、最大50ファイル同時処理
- メタデータ自動書き込み（タイトル、アーティスト、アルバム、カバーアート）
- 歌詞の自動検索・埋め込み（原文 / 翻訳 / 混合）
- Material You ダイナミックカラー（Android 12+）
- HSL スライダーによるアクセントカラー・背景色のカスタマイズ
- マルチカラムグリッドレイアウト、折りたたみ端末・タブレット対応
- 多言語対応：簡体字中国語、繁体字中国語、English、日本語

## 動作要件

- Android 8.0（API 26）以上

## 技術スタック

- Kotlin + Jetpack Compose
- Retrofit2 + OkHttp4
- AES-128-ECB / RC4 復号
- MediaStore API

## ビルド方法

```
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

## 歌詞機能

歌詞自動埋め込み機能を利用するには、自前で構築した NetEase Cloud Music API サービスが必要です。

1. [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) をデプロイする
2. アプリの設定画面に API アドレスを入力する
3. 地域を指定する必要がある場合は、Real IP パラメータを入力する

## 参考プロジェクト

本プロジェクトの開発にあたり、以下のプロジェクトを参考にしました：

- [NLyric](https://github.com/wwh1004/NLyric) — 歌詞マッチング戦略の参考
- [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) — 歌詞／メタデータ API サービス
- [openyyy.com](https://openyyy.com/) — NCM 形式解析の参考

## ライセンス

本プロジェクトは [GPL v3](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/LICENSE) ライセンスの下でオープンソース化されています。
