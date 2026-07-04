# NCM Converter

An Android tool for decrypting NetEase CloudMusic encrypted `.ncm` files back to standard MP3/FLAC audio files, with support for embedding album art and lyrics (API required — see [Lyrics](#lyrics) section).

\*Built by AI — probably made up with shit and shit, just take a look.

## Features

- **NCM Decryption** — Decrypts `.ncm` files to MP3 or FLAC format
- **Batch Conversion** — Select up to 50 files for batch processing at once
- **Metadata Embedding** — Automatically writes song title, artist, album, cover art, and more
- **Lyrics Embedding** — Fetches lyrics from NetEase CloudMusic and embeds them into audio files. Supports original / translated / mixed modes (requires that the corresponding lyrics/translations already exist on NetEase CloudMusic)
- **Large File Support** — Streaming decryption architecture with no memory cap, capable of handling files of any size
- **Theme Switching** — Supports System / Light / Dark themes

## Requirements

- Android 8.0 (API 26) or above

## Tech Stack

- Kotlin + Jetpack Compose
- Retrofit2 + OkHttp4
- AES-128-ECB / RC4 Decryption
- MediaStore API

## Lyrics

The automatic lyrics embedding feature requires a self-hosted NetEase CloudMusic API service.

1. Deploy [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced)
2. Enter the API address in the app's settings
3. If you need to specify a region, fill in the Real IP parameter

## Build

```shell
./gradlew assembleRelease
```

## References

The following projects were referenced during development:

- [NLyric](https://github.com/wwh1004/NLyric) — Lyric matching strategy reference
- [NeteaseCloudMusicApiEnhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) — Lyrics / metadata API service
- [openyyy.com](https://openyyy.com/) — NCM format parsing reference

## Disclaimer

This tool is for learning and educational purposes only. Decrypted files must not be used for commercial purposes or illegal distribution. All file decryption is performed locally on-device; the app does not upload any files to the internet.

## License

This project is for personal learning use only.
