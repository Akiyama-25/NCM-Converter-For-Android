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
- 内蔵 Resource Han Round SC フォント、6 段階のフォントウェイトのカスタマイズに対応
- 予測型バックジェスチャー対応（Android 16+）
- ダブルタップで終了
- プロセス異常終了後のファイルリスト自動復元
- 多言語対応：簡体字中国語、繁体字中国語、English、日本語

<p align="center">
  <img src="docs/screenshots/screenshot_1.jpg" alt="Screenshot 1" width="30%" />
  <img src="docs/screenshots/screenshot_2.jpg" alt="Screenshot 2" width="30%" />
  <img src="docs/screenshots/screenshot_3.jpg" alt="Screenshot 3" width="30%" />
  <br><br>
  <img src="docs/screenshots/screenshot_4.jpg" alt="Screenshot 4" width="30%" />
  <img src="docs/screenshots/screenshot_5.png" alt="Screenshot 5" width="30%" />
  <img src="docs/screenshots/screenshot_6.png" alt="Screenshot 6" width="30%" />
</p>

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

歌詞自動埋め込み機能には NetEase 公式直接接続 API が内蔵されており、外部サーバーを構築することなくそのまま利用可能です。

1. アプリはデフォルトで内蔵公式直接接続 API を使用するため、サーバー設定不要で歌詞とカバー画像を取得・埋め込みできます。
2. 歌詞機能を有効にすると、「原文歌詞」と「翻訳歌詞」の埋め込みを個別に制御可能（両方有効時は行ごとに自動結合）。

> 注：翻訳歌詞機能は簡体中国語の翻訳結果のみ対応しています。アプリの言語が簡体中国語以外の場合、翻訳スイッチに対応する注記が表示されます。

## 参考プロジェクト

本プロジェクトの開発にあたり、以下のプロジェクトを参考にしました：

- [rinchao0721/Melodia](https://github.com/rinchao0721/Melodia) — アプリ内蔵の純粋な Kotlin 暗号化 NetEase 公式直接接続 API の設計方針およびプロトコル参考
- [Ame-Player](https://github.com/Akiyama-25/Ame-Player) — クライアント内蔵 NetEase 暗号化直接接続および署名付きビルドワークフローの参考
- [NLyric](https://github.com/wwh1004/NLyric) — 歌詞マッチング戦略の参考
- [openyyy.com](https://openyyy.com/) — NCM 形式解析の参考

## ライセンス

本プロジェクトは [GPL v3](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/LICENSE) ライセンスの下でオープンソース化されています。
