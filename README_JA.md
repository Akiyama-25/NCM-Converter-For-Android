[简体中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README.md) | [繁體中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_TC.md) | [English](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_EN.md)

# NCM コンバーター

NetEase Cloud Music で暗号化された `.ncm` ファイルを標準的な MP3 または FLAC 形式に復号する Android アプリです。

## 機能

- ファイル数制限なしのバッチ変換
- メタデータ自動書き込み（タイトル、アーティスト、アルバム、カバーアート）
- 歌詞の自動検索・埋め込み、原文と翻訳を独立したスイッチで制御
- システムフォルダ選択器による出力先設定、DocumentTree 永続権限対応
- バックグラウンド変換：自動保存を有効にすると、フォアグラウンドサービスで変換を実行し、通知バーに進捗を表示。アプリ切り替えしても中断しません
- Material You ダイナミックカラー（Android 12+）
- HSL スライダーによるアクセントカラー・背景色のカスタマイズ
- マルチカラムグリッドレイアウト、折りたたみ端末・タブレット対応
- 予測型バックジェスチャー対応（Android 16+）
- ダブルタップで終了
- プロセス異常終了後のファイルリスト自動復元
- 多言語対応：簡体字中国語、繁体字中国語、English、日本語

## 動作要件

- Android 8.0（API 26）以上
- Android 16（API 36）対応済み

## 技術スタック

- Kotlin + Jetpack Compose + Material 3
- Retrofit2 + OkHttp4
- AES-128-ECB / RC4 復号
- MediaStore API / DocumentFile

## ビルド方法

```
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

## 歌詞機能

歌詞自動埋め込み機能を利用するには、自前で構築した NetEase Cloud Music API サービスが必要です。

1. [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) をデプロイする
2. アプリの設定画面でダイアログから API アドレスを入力し、保存後に歌詞関連オプションが表示される
3. 歌詞機能を有効にすると、「原文歌詞」と「翻訳歌詞」の埋め込みを独立して制御可能
4. 地域を指定する必要がある場合は、Real IP パラメータを入力する

> 注：翻訳歌詞機能は簡体中国語の翻訳結果のみ対応しています。アプリの言語が簡体中国語以外の場合、翻訳スイッチに対応する注記が表示されます。

## 参考プロジェクト

本プロジェクトの開発にあたり、以下のプロジェクトを参考にしました：

- [NLyric](https://github.com/wwh1004/NLyric) — 歌詞マッチング戦略の参考
- [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) — 歌詞／メタデータ API サービス
- [openyyy.com](https://openyyy.com/) — NCM 形式解析の参考

## ライセンス

本プロジェクトは [GPL v3](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/LICENSE) ライセンスの下でオープンソース化されています。
