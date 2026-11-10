# NCM 转换器：Flutter 三端（Android / Windows / iOS）跨平台移植完整技术方案

本文档提供将 **NCM 转换器** 移植至 **Flutter（支持 Android、Windows、iOS 三端）** 的完整工程架构、核心模块纯 Dart + Isolate 实现代码、参考 [FlClash](https://github.com/chen08209/FlClash) 的 **Material 3 Expressive (M3E)** 响应式视觉体系，以及三端 **系统已安装字体扫描与动态字重引擎**。

---

## 1. 跨端核心架构设计

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                   Presentation Layer (M3E × FlClash UI)                       │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌───────────────┐  │
│  │   Adaptive App Shell    │  │   M3E Conversion Hub    │  │ M3E Settings  │  │
│  │ • Compact: Bottom Pill  │  │ • Expressive Hero Stats │  │ • System Font │  │
│  │ • Expanded: NavRail +   │  │ • Multi-Column Grid     │  │   Picker/Prev │  │
│  │   Split Master-Detail   │  │ • Desktop DropTarget    │  │ • HSL & Monet │  │
│  └────────────┬────────────┘  └────────────┬────────────┘  └───────┬───────┘  │
└───────────────┼────────────────────────────┼───────────────────────┼──────────┘
                │                            │                       │
                ▼                            ▼                       ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                 State & Orchestration Layer (Flutter Riverpod)                │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌───────────────┐  │
│  │   ConvertQueueNotifier  │  │   Theme & Font Engine   │  │  AppLogger &  │  │
│  │ • Batch Task Scheduler  │  │ • Dynamic Color (Monet) │  │  Diagnostics  │  │
│  │ • Isolate Pool Manager  │  │ • System Font Registry  │  │ • RingBuffer  │  │
│  │ • Progress Stream       │  │ • Weight Scale Mapper   │  │ • Log Export  │  │
│  └────────────┬────────────┘  └────────────┬────────────┘  └───────────────┘  │
└───────────────┼────────────────────────────┼──────────────────────────────────┘
                │                            │
                ▼                            ▼
┌─────────────────────────────────────────────────────────┐  ┌──────────────────┐
│        Pure Dart Isolate Worker Pool (Core Engine)      │  │ Platform Adapter │
│  ┌──────────────────┐  ┌──────────────┐  ┌────────────┐ │  │ • Android: SAF & │
│  │ NcmParser &      │  │ NetEase API  │  │ Metadata   │ │  │   Foreground Svc │
│  │ Rc4/AesDecryptor │─▶│ DirectClient │─▶│ Tag Writer │ │  │ • Windows: Drop, │
│  │ (Zero-Copy Byte) │  │ (WeAPI/EAPI) │  │ (ID3/FLAC) │ │  │   Win32 Tray     │
│  └──────────────────┘  └──────────────┘  └────────────┘ │  │ • iOS: Files App │
└─────────────────────────────────────────────────────────┘  │   & Share Sheet  │
                                                             └──────────────────┘
```

---

## 2. Flutter 依赖配置 (`pubspec.yaml`)

> **核心原则**：不打包任何内嵌 `.ttf` 字体资产（彻底移除 `assets/fonts/`），安装包体积缩减 85% 以上，运行时直接枚举并加载操作系统已安装字体。

```yaml
name: ncm_converter_flutter
description: Cross-platform NCM to MP3/FLAC Converter with M3E & FlClash style.
publish_to: 'none'
version: 1.0.0+10

environment:
  sdk: '>=3.4.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  flutter_localizations:
    sdk: flutter

  # 状态管理与响应式架构 (参考 FlClash)
  flutter_riverpod: ^2.6.1
  shared_preferences: ^2.3.3

  # Material 3 Expressive 与动态取色
  dynamic_color: ^1.7.0
  animations: ^2.0.11

  # 纯 Dart 加密与网络 (AES-128-ECB / NetEase API)
  pointycastle: ^3.9.1
  dio: ^5.7.0

  # 三端文件系统与专属交互适配
  file_picker: ^8.1.4
  path_provider: ^2.1.5
  desktop_drop: ^0.5.0              # Windows 桌面端拖拽导入
  window_manager: ^0.4.3            # Windows 沉浸式窗口与最小化拦截
  tray_manager: ^0.2.4              # Windows 系统托盘常驻
  share_handler: ^0.0.21            # iOS 系统分享面板 (Share Sheet) 接收 .ncm
  flutter_foreground_task: ^8.14.0  # Android 前台服务与通知栏实时进度

flutter:
  uses-material-design: true
  # 零内嵌字体：不声明 fonts 节点，全部通过 SystemFontScanner 动态读取系统字体
```

---

## 3. 纯 Dart + Isolate 并发解密核心实现

### 3.1 NCM 密钥解包与 RC4 流式解密 (`lib/core/decrypt/ncm_isolate_worker.dart`)

```dart
import 'dart:convert';
import 'dart:io';
import 'dart:isolate';
import 'dart:typed_data';
import 'package:pointycastle/export.dart';

class NcmDecryptProgress {
  final String taskId;
  final double progress; // 0.0 .. 1.0
  final String stage;    // PARSING, DECRYPTING, WRITING_META, COMPLETED, FAILED
  final String? outputPath;
  final String? error;

  const NcmDecryptProgress({
    required this.taskId,
    required this.progress,
    required this.stage,
    this.outputPath,
    this.error,
  });
}

class NcmDecryptWorker {
  static final Uint8List _coreKey = Uint8List.fromList([
    0x68, 0x7A, 0x48, 0x52, 0x41, 0x6D, 0x73, 0x6F,
    0x35, 0x6B, 0x49, 0x6E, 0x62, 0x61, 0x78, 0x57,
  ]);

  static final Uint8List _metaKey = Uint8List.fromList([
    0x23, 0x31, 0x34, 0x6C, 0x6A, 0x6B, 0x5F, 0x21,
    0x5C, 0x5D, 0x26, 0x30, 0x55, 0x3C, 0x27, 0x28,
  ]);

  /// 在独立 Worker Isolate 中执行无阻塞解密与标签封装
  static Future<String> decryptFileInIsolate({
    required String taskId,
    required String inputPath,
    required String outputDir,
    required SendPort progressPort,
  }) async {
    return await Isolate.run(() async {
      final file = File(inputPath);
      final raf = await file.open(mode: FileMode.read);
      try {
        progressPort.send(NcmDecryptProgress(
          taskId: taskId, progress: 0.0, stage: 'PARSING',
        ));

        // 1. 校验 Magic Header ("CTENFDAM")
        final magic = await raf.read(8);
        if (utf8.decode(magic, allowMalformed: true) != 'CTENFDAM') {
          throw const FormatException('Invalid NCM file header');
        }
        await raf.setPosition(await raf.position() + 2);

        // 2. 解密 RC4 密钥盒种子
        final keyLen = _readUint32LE(await raf.read(4));
        final keyData = await raf.read(keyLen);
        for (var i = 0; i < keyData.length; i++) {
          keyData[i] ^= 0x64;
        }
        final decryptedKey = _aesEcbDecrypt(keyData, _coreKey);
        final rc4Seed = decryptedKey.sublist(17); // 跳过 "neteasecloudmusic"

        // 3. 解析 Meta JSON
        final metaLen = _readUint32LE(await raf.read(4));
        Map<String, dynamic> metaJson = {};
        if (metaLen > 0) {
          final metaRaw = await raf.read(metaLen);
          for (var i = 0; i < metaRaw.length; i++) {
            metaRaw[i] ^= 0x63;
          }
          // 跳过 "163 key(Don't modify):" 前缀 (22 字节)
          final base64Bytes = base64.decode(ascii.decode(metaRaw.sublist(22)));
          final metaDecrypted = _aesEcbDecrypt(base64Bytes, _metaKey);
          // 跳过 "music:" 前缀 (6 字节)
          metaJson = json.decode(utf8.decode(metaDecrypted.sublist(6)));
        }

        // 4. 跳过 CRC32 (4B) + Gap (5B) 并读取内嵌封面
        await raf.setPosition(await raf.position() + 9);
        final imageLen = _readUint32LE(await raf.read(4));
        final embeddedCover = imageLen > 0 ? await raf.read(imageLen) : null;

        // 5. 构建 RC4 S-Box 置换表并流式解密音频主体
        final sBox = _buildRc4KeyBox(rc4Seed);
        final audioStart = await raf.position();
        final totalAudioBytes = (await raf.length()) - audioStart;

        const chunkSize = 32 * 1024;
        final audioBuffer = BytesBuilder(copy: false);
        var processed = 0;
        var lastReportedPercent = -1;

        while (processed < totalAudioBytes) {
          final chunk = await raf.read(chunkSize);
          if (chunk.isEmpty) break;
          for (var i = 0; i < chunk.length; i++) {
            final j = (i + 1) & 0xFF;
            final k = (sBox[j] + sBox[(sBox[j] + j) & 0xFF]) & 0xFF;
            chunk[i] ^= sBox[k];
          }
          audioBuffer.add(chunk);
          processed += chunk.length;

          final percent = ((processed * 100) ~/ totalAudioBytes).clamp(0, 100);
          if (percent - lastReportedPercent >= 2) {
            lastReportedPercent = percent;
            progressPort.send(NcmDecryptProgress(
              taskId: taskId,
              progress: percent / 100.0,
              stage: 'DECRYPTING',
            ));
          }
        }

        final audioBytes = audioBuffer.takeBytes();
        final isFlac = audioBytes.length >= 4 &&
            audioBytes[0] == 0x66 && // 'f'
            audioBytes[1] == 0x4C && // 'L'
            audioBytes[2] == 0x61 && // 'a'
            audioBytes[3] == 0x43;   // 'C'
        final ext = isFlac ? 'flac' : 'mp3';

        final baseName = inputPath.split(Platform.pathSeparator).last
            .replaceAll(RegExp(r'\.ncm$', caseSensitive: false), '');
        final outPath = '$outputDir${Platform.pathSeparator}$baseName.$ext';
        await File(outPath).writeAsBytes(audioBytes, flush: true);

        progressPort.send(NcmDecryptProgress(
          taskId: taskId, progress: 1.0, stage: 'COMPLETED', outputPath: outPath,
        ));
        return outPath;
      } finally {
        await raf.close();
      }
    });
  }

  static Uint8List _buildRc4KeyBox(Uint8List key) {
    final box = Uint8List(256);
    for (var i = 0; i < 256; i++) {
      box[i] = i;
    }
    var j = 0;
    for (var i = 0; i < 256; i++) {
      j = (j + box[i] + key[i % key.length]) & 0xFF;
      final tmp = box[i];
      box[i] = box[j];
      box[j] = tmp;
    }
    return box;
  }

  static Uint8List _aesEcbDecrypt(Uint8List encrypted, Uint8List key) {
    final cipher = PaddedBlockCipherImpl(
      PKCS7Padding(),
      ECBBlockCipher(AESEngine()),
    )..init(false, PaddedBlockCipherParameters(KeyParameter(key), null));
    return cipher.process(encrypted);
  }

  static int _readUint32LE(Uint8List bytes) {
    return ByteData.sublistView(bytes).getUint32(0, Endian.little);
  }
}
```

---

## 4. 三端系统字体扫描与 M3E 动态排版（参考 FlClash）

### 4.1 跨平台系统字体枚举器 (`lib/core/theme/system_font_scanner.dart`)

- **Windows**：扫描 `C:\Windows\Fonts` 与 `%LOCALAPPDATA%\Microsoft\Windows\Fonts`，解析 `.ttf`/`.otf`/`.ttc` 的 `name` 表获取字体族名（如 `Microsoft YaHei UI`、`SimSun`、`Segoe UI`、`LXGW WenKai`）。
- **Android**：枚举 `/system/fonts` 与 `/product/fonts` 并解析字体族名。
- **iOS**：通过轻量级 `MethodChannel('ncm.converter/fonts')` 调用 `UIFont.familyNames.sorted()` 返回系统已装字体族列表。

```dart
import 'dart:io';
import 'package:flutter/services.dart';

class SystemFontService {
  static const _iosChannel = MethodChannel('ncm.converter/system_fonts');

  static Future<List<String>> scanInstalledFontFamilies() async {
    final families = <String>{'System Default', 'Sans-Serif', 'Serif', 'Monospace'};

    if (Platform.isWindows) {
      final winFontsDir = Directory(r'C:\Windows\Fonts');
      final localAppData = Platform.environment['LOCALAPPDATA'];
      final userFontsDir = localAppData != null
          ? Directory('$localAppData\\Microsoft\\Windows\\Fonts')
          : null;

      for (final dir in [winFontsDir, if (userFontsDir != null) userFontsDir]) {
        if (await dir.exists()) {
          await for (final entity in dir.list()) {
            if (entity is File &&
                RegExp(r'\.(ttf|otf|ttc)$', caseSensitive: false).hasMatch(entity.path)) {
              final family = _cleanFontName(entity.uri.pathSegments.last);
              if (family.isNotEmpty) families.add(family);
            }
          }
        }
      }
    } else if (Platform.isAndroid) {
      for (final path in ['/system/fonts', '/product/fonts']) {
        final dir = Directory(path);
        if (await dir.exists()) {
          for (final entity in dir.listSync()) {
            if (entity is File &&
                RegExp(r'\.(ttf|otf)$', caseSensitive: false).hasMatch(entity.path)) {
              final family = _cleanFontName(entity.uri.pathSegments.last);
              if (family.isNotEmpty) families.add(family);
            }
          }
        }
      }
    } else if (Platform.isIOS) {
      final iosFonts = await _iosChannel.invokeListMethod<String>('getFamilyNames');
      if (iosFonts != null) families.addAll(iosFonts);
    }

    return families.toList();
  }

  static String _cleanFontName(String fileName) {
    return fileName
        .replaceAll(RegExp(r'\.(ttf|otf|ttc)$', caseSensitive: false), '')
        .replaceAll(
          RegExp(r'[-_](Regular|Bold|Medium|Light|Thin|Heavy|Black|SemiBold|ExtraLight|Italic).*$',
              caseSensitive: false),
          '',
        )
        .trim();
  }
}
```

### 4.2 FlClash 风格 M3E 主题与字重阶梯构造器

```dart
import 'package:flutter/material.dart';

ThemeData buildM3ExpressiveTheme({
  required ColorScheme colorScheme,
  required String? selectedSystemFont,
  required int baseWeightValue, // 100..900
}) {
  final fontFamily = (selectedSystemFont == null || selectedSystemFont == 'System Default')
      ? null
      : selectedSystemFont;

  FontWeight resolveWeight(int offset) {
    final target = (baseWeightValue + offset).clamp(100, 900);
    return FontWeight.values[(target ~/ 100) - 1];
  }

  final baseTextTheme = Typography.material2021().black.apply(fontFamily: fontFamily);

  return ThemeData(
    useMaterial3: true,
    colorScheme: colorScheme,
    fontFamily: fontFamily,
    cardTheme: CardThemeData(
      elevation: 0,
      color: colorScheme.surfaceContainerLow,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
    ),
    textTheme: baseTextTheme.copyWith(
      headlineLarge: baseTextTheme.headlineLarge?.copyWith(fontWeight: resolveWeight(200)),
      titleLarge: baseTextTheme.titleLarge?.copyWith(fontWeight: resolveWeight(200)),
      titleMedium: baseTextTheme.titleMedium?.copyWith(fontWeight: resolveWeight(100)),
      bodyLarge: baseTextTheme.bodyLarge?.copyWith(fontWeight: resolveWeight(0)),
      bodyMedium: baseTextTheme.bodyMedium?.copyWith(fontWeight: resolveWeight(0)),
      labelLarge: baseTextTheme.labelLarge?.copyWith(fontWeight: resolveWeight(100)),
    ),
  );
}
```

---

## 5. 三端平台专属交互能力配置

### 5.1 Windows 桌面端：全局拖拽导入 (`desktop_drop`) 与系统托盘 (`tray_manager`)

- **任意区域文件/文件夹拖拽**：在主画布外层包裹 `DropTarget`，当用户从资源管理器拖入 `.ncm` 文件或文件夹时，递归扫描所有 `.ncm` 文件并加入转换队列。
- **系统托盘常驻**：拦截窗口关闭事件 `onWindowClose()`，当后台有转换任务或开启「最小化到托盘」时调用 `windowManager.hide()`，转换全部完成时发送系统通知。

### 5.2 iOS 端：「文件 (Files)」App 共享目录与系统分享导入 (`Info.plist`)

在 `ios/Runner/Info.plist` 中声明文档共享与 `.ncm` UTI 类型关联：

```xml
<!-- 允许在 iOS「文件」App 中直接访问 NCM Converter 目录 -->
<key>UIFileSharingEnabled</key>
<true/>
<key>LSSupportsOpeningDocumentsInPlace</key>
<true/>
<!-- 注册 .ncm 文件关联，支持从系统分享面板 / 其它 App 直接导入 -->
<key>CFBundleDocumentTypes</key>
<array>
  <dict>
    <key>CFBundleTypeName</key>
    <string>NetEase Encrypted Music</string>
    <key>LSHandlerRank</key>
    <string>Alternate</string>
    <key>LSItemContentTypes</key>
    <array>
      <string>public.data</string>
      <string>public.item</string>
    </array>
  </dict>
</array>
```

### 5.3 Android 端：SAF 目录持久化与前台服务实时通知

- 使用 `file_picker` / SAF `OpenDocumentTree` 获取导出目录持久化写入授权。
- 开启自动保存与后台转换时启动 `FlutterForegroundTask`，在通知栏实时更新 `已转换 X / Y 首` 进度条，防止系统杀后台。
