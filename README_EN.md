[简体中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README.md) | [繁體中文](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_TC.md) | [日本語](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/README_JA.md)

# NCM Converter

An Android app that decrypts NetEase Cloud Music's encrypted `.ncm` files into standard MP3 or FLAC format.

## Features

- Batch conversion, up to 50 files at once
- Automatic metadata writing (title, artist, album, cover art)
- Automatic lyric search and embedding (original / translated / merged)
- Material You dynamic colors (Android 12+)
- Custom accent and background colors via HSL sliders
- Multi-column grid layout, optimized for foldables and tablets
- Multi-language support: Simplified Chinese, Traditional Chinese, English, Japanese

## Requirements

- Android 8.0 (API 26) or higher

## Tech Stack

- Kotlin + Jetpack Compose
- Retrofit2 + OkHttp4
- AES-128-ECB / RC4 decryption
- MediaStore API

## Build

```
git clone https://github.com/Akiyama-25/NCM-Converter-For-Android.git
cd NCM-Converter-For-Android
./gradlew assembleDebug
```

## Lyrics

The automatic lyrics embedding feature requires a self-hosted NetEase Cloud Music API service.

1. Deploy [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced)
2. Enter the API address in the app settings
3. To specify a region, you can provide a Real IP parameter

## Reference Projects

The following projects were referenced during development:

- [NLyric](https://github.com/wwh1004/NLyric) — reference for lyrics matching strategy
- [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) — lyrics/metadata API service
- [openyyy.com](https://openyyy.com/) — reference for NCM format parsing

## License

This project is open-sourced under the [GPL v3](https://github.com/Akiyama-25/NCM-Converter-For-Android/blob/main/LICENSE) license.
