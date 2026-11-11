# NCM Converter — User Guide

## 1. Overview
**NCM Converter** is a free Android utility that decrypts and restores encrypted NetEase Cloud Music (`.ncm`) files into standard **MP3** or **FLAC** audio files.

> All decryption calculations are performed locally on your device. None of your files are ever uploaded to the internet.

---

## 2. Supported Format
- **NetEase Cloud Music**: `.ncm`

---

## 3. How to Use
1. Tap the **Select NCM Files** button and choose one or more `.ncm` files from your file manager.
2. Tap the play button on the right side of a file to decrypt it individually, or tap **Convert All** at the bottom for batch processing.
3. Once decryption completes, tap the save button to write the file to storage (or wait for **Auto Save** if enabled).
4. Tap the gear icon in the top bar to open **Settings** and customize the export path, theme, font, lyrics, and more.

---

## 4. Settings Guide
- **Save Path**: Choose where decrypted files are saved (defaults to `Music/NCMConverter`).
- **Auto Save**: When enabled, files are saved automatically after conversion and background conversion with notification progress is supported; when disabled, tap the save button manually after conversion.
- **Auto Fetch Cover**: Automatically download album cover art from the network during conversion.
- **Theme & Colors**: Supports **System / Light / Dark** modes, **Material You** dynamic colors (Android 12+), and custom **HSL** accent/background colors.
- **System Font Family**: Scan and apply fonts installed on your device with adjustable global font weight (`100`–`900`).

---

## 5. Lyrics Feature (Built-in Direct API)
Automatic lyrics embedding includes built-in direct connection support for NetEase Cloud Music's official API. It works out of the box without needing an external server.

### Lyrics Settings
- **Auto Embed Lyrics**: Automatically search for and embed lyrics during conversion.
- **Embed Original Lyrics**: Include original-language lyrics in the output file.
- **Embed Translated Lyrics**: Include translated lyrics in the output file (aligned and merged line-by-line when both are enabled).

### Embedded Lyrics Format
- **MP3 files**: Written to the `USLT` (Unsynchronized Lyrics) ID3v2 tag.
- **FLAC files**: Written to the `LYRICS` Vorbis Comment field.

---

## 6. Technical Details
- Uses `AES-128-ECB` to decrypt the key block and metadata.
- Uses a modified `RC4` stream cipher to decrypt audio data.
- Automatically detects output audio format (`MP3` / `FLAC`).
- Automatically embeds track metadata (title, artist, album, cover art).
- Supports multiple lyrics matching strategies (direct `musicId` lookup and title + artist search).

---

## 7. Notes
- Up to **50** `.ncm` files can be added at a time; non-`.ncm` files are filtered out automatically.
- Decrypting large lossless files may take a moment—please wait patiently.
- The output format (`MP3` or `FLAC`) is determined by the original audio stream inside the `.ncm` file.
- If conversion fails, please verify that the file is not corrupted and is a valid `.ncm` file.
- Uses `MediaStore` / `DocumentFile` APIs on Android 10+ for safe storage access (fully adapted through Android 17 / API 37).
- Fetching lyrics and high-res covers requires an internet connection; network failures will not affect audio decryption.

---

## 8. Privacy Statement
- All audio file decryption is performed strictly locally on your device.
- The app never uploads your files or audio data to the internet.
- Network access is used only when fetching cover art and lyrics.
- The app does not collect any personal information.

---

## 9. FAQ
**Q: Why is the song title missing on the converted file?**
A: Some older `.ncm` files have incomplete embedded metadata. The audio stream itself is unaffected.

**Q: Is batch conversion supported?**
A: Yes. You can select up to 50 files at once and tap **Convert All**.

**Q: Where are converted files saved?**
A: By default they are saved under `Music/NCMConverter`. You can change the save folder in **Settings**.

**Q: Why isn't the lyrics feature working?**
A: The app uses a built-in encrypted connection to NetEase's official API. Please check your device's internet connection.

**Q: Why were no lyrics found for a song?**
A: Possible reasons:
1. The track has no lyrics on NetEase Cloud Music.
2. Network connection error.
3. Regional network restrictions.

---

## 10. Reference Projects
- Built-in Client API Reference: [rinchao0721/Melodia](https://github.com/rinchao0721/Melodia)
- Lyrics Matching Reference: [wwh1004/NLyric](https://github.com/wwh1004/NLyric)
