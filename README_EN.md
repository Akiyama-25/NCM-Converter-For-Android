[简体中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README.md) | [繁體中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_TC.md) | [日本語](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_JA.md)

# NCM Converter

An Android app that decrypts NetEase Cloud Music's encrypted `.ncm` files into standard MP3 or FLAC format.

## Features

- Batch conversion with no file limit
- Automatic metadata writing (title, artist, album, cover art)
- Automatic lyric search and embedding with independent original/translated switches
- System folder picker for export directory with DocumentTree persistent permission
- Background conversion: when auto-save is enabled, conversion runs in a foreground service with notification progress — switching apps won't interrupt it
- Material You dynamic colors (Android 12+)
- Custom accent and background colors via HSL sliders
- Multi-column grid layout, optimized for foldables and tablets
- Predictive back gesture support (Android 16+)
- Double-tap back to exit
- Automatic file list recovery after unexpected process death
- Multi-language support: Simplified Chinese, Traditional Chinese, English, Japanese

## Requirements

- Android 8.0 (API 26) or higher
- Android 16 (API 36) supported

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Retrofit2 + OkHttp4
- AES-128-ECB / RC4 decryption
- MediaStore API / DocumentFile

## Build

```
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

## Lyrics

The automatic lyrics embedding feature requires a self-hosted NetEase Cloud Music API service.

1. Deploy [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced)
2. Enter the API address via the dialog in app settings; lyric options will only appear after saving
3. Once lyrics are enabled, you can independently control whether to embed "original lyrics" and "translated lyrics"
4. To specify a region, you can provide a Real IP parameter

> Note: The translated lyrics feature only supports Simplified Chinese translation results. When the app language is not Simplified Chinese, the translation switch will display a corresponding note.

## Reference Projects

The following projects were referenced during development:

- [NLyric](https://github.com/wwh1004/NLyric) — reference for lyrics matching strategy
- [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) — lyrics/metadata API service
- [openyyy.com](https://openyyy.com/) — reference for NCM format parsing

## License

This project is open-sourced under the [GPL v3](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/LICENSE) license.
