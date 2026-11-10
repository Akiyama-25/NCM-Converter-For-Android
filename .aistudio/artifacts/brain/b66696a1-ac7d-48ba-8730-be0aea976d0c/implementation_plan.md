# NCM 转换器：Flutter 三端（Android / Windows / iOS）移植架构与 M3E 系统字体升级方案

本方案旨在将现有 NCM 转换器完整移植为覆盖 **Android、Windows、iOS** 三端通用的跨平台 **Flutter** 应用，同时全面引入参考 **FlClash** 美学风格的 **Material 3 Expressive (M3E)** 表现力设计语言；字体系统彻底告别内嵌大体积字体包，改为实时扫描并读取操作系统已安装字体族与字重调节，并在本工作区同步将现有原生端升级为 M3E 视觉与系统字体动态加载架构。

---

### User Review & Critical Decisions

> [!IMPORTANT]
> 以下为您在第一阶段确认的核心架构与交互决策，将作为后续跨端架构落地与当前工程升级的最高准则：

- **Confirmed Decision 1 — 纯 Dart + Isolate 并发解密核心**：Flutter 端摒弃重度 C++/Rust 编译链依赖，采用纯 Dart 实现 AES-128-ECB 密钥解包、RC4 流式音频解密、MP3 ID3v2 / FLAC Vorbis Comment & Picture 元数据写入及网易云官方 EAPI/WeAPI 加密直连，通过 `Isolate.run` 与常驻 Worker Isolate 池实现多核无阻塞批量转换。
- **Confirmed Decision 2 — 系统已安装字体扫描、预览切换与字重调节（FlClash 风格）**：完全移除内嵌 `Resource Han Round SC` 字体文件（立减约 45MB 包体积），改为枚举系统已安装字体族列表（如 Windows DirectWrite/Fonts 注册表、Android `/system/fonts` & `SystemFonts` API、iOS `UIFont.familyNames`），提供可视化字体预览选择器、实时搜索过滤与 100–900 多档字重动态调节。
- **Confirmed Decision 3 — 三端原生专属交互能力全覆盖**：
  - **Windows 桌面端**：支持窗口任意区域文件/文件夹拖拽（Drag & Drop）批量导入 `.ncm`、自定义无边框沉浸式标题栏，以及系统托盘（System Tray）最小化常驻与后台转换完成气泡通知。
  - **iOS 移动端**：开启文档共享沙盒，深度集成系统「文件 (Files)」App 目录访问与系统分享面板（Share Sheet / Action Extension）直接导入 `.ncm` 文件，适配后台音频处理任务续期。
  - **Android 移动端**：完整保留 SAF（Storage Access Framework）目录授权持久化与前台服务（Foreground Service）后台不中断转换及通知栏实时进度。
- **Confirmed Decision 4 — 交付范围与同步升级**：输出完整详尽的 Flutter 三端跨平台架构与核心模块移植指南，并同步将当前工作区内的 Android 应用重构升级为 **Material 3 Expressive (M3E)** 视觉体系与 **系统已装字体扫描/预览/字重调节** 架构。

---

### 1. Overview & Core Concept

- **What It Does**：提供一站式、高吞吐量的网易云音乐 `.ncm` 加密音频文件无损解密与元数据封装工具。自动解析内嵌歌曲信息、从网易云官方加密接口直连拉取高清专辑封面与双语逐行对齐 LRC 歌词，并封装进目标 MP3 或 FLAC 音频文件中。
- **Target Audience / Persona**：拥有跨设备（Windows PC 桌面端、Android 手机/平板、iPhone/iPad）本地音乐管理需求的音乐爱好者，追求精致现代的 UI 动效、轻量化安装包体积与零配置开箱即用体验。
- **Key Value**：
  1. **极致瘦身与原生字体渲染**：剥离内嵌字体包后安装包体积缩减 85% 以上，同时直接调用本机安装的思源黑体、霞鹜文楷、MiSans、HarmonyOS Sans、PingFang SC、Microsoft YaHei UI 等系统字体。
  2. **M3E + FlClash 跨端自适应体验**：融合 Material 3 Expressive 的弹簧物理动效、丰富色彩层级与 FlClash 标志性的大圆角分组卡片、自适应侧边栏/底栏导航及丝滑手势交互。
  3. **三端一致的高性能纯异步流水线**：解密、歌词匹配、封面下载与标签写入全链路流式并发处理，大体积无损 FLAC 转换同样保持 UI 满帧响应。

---

### 2. User Experience & Visual Design

#### Key User Flows

1. **多端统一导入与拖拽流**：
   - **移动端（Android / iOS）**：点击顶部/悬浮 M3E 大型扩展操作按钮（Extended FAB / Filled Hero Card）调起系统文件选择器，或从系统文件管理器通过「分享/打开方式」直接将多个 `.ncm` 送入转换队列。
   - **桌面端（Windows）**：支持直接将文件或整个文件夹拖入主窗口画布；拖入瞬间触发 M3E 弹性高亮边框与呼吸微动效，自动递归过滤出所有 `.ncm` 文件。
2. **批量并发转换与实时状态流**：
   - 队列中每个音频卡片实时展示解析状态（等待中 → 解析头部 → RC4 解密百分比进度条 → 网易云双语歌词/封面匹配 → 写入 ID3v2/Vorbis 标签 → 完成/自动落盘）。
   - 顶部 M3E 状态胶囊（Expressive Status Pill）实时汇总总体进度、成功/失败计数与估算剩余时间。
3. **系统字体扫描与实时预览流（参考 FlClash）**：
   - 进入「设置 → 外观与排版 → 系统字体」，应用自动扫描当前设备已安装的所有可用字体族。
   - 弹出分组搜索式字体选择面板，每一项均以该字体本身渲染中英日文示例文案（如 `字体预览 AaBb 永 123`），点击即刻全局无感热切换，配合字重滑块（Thin 100 至 Black 900）实时调整全局视觉粗细。

#### Visual Identity & Theme (Material 3 Expressive × FlClash Style)

- **Aesthetic Direction**：参考 **FlClash** 的 **Material 3 meets Apple HIG** 现代精致工具美学，结合 **Material 3 Expressive (M3E)** 规范，强调层次分明的容器嵌套（Surface Container Lowest → Highest）、连续平滑大圆角（20dp–28dp）、富有弹性的物理弹簧过渡以及高信息密度的仪表盘卡片布局。
- **Color Palette & Mood**：
  - 支持 **Monet 动态取色**（Android 12+ 壁纸取色 / Windows 系统强调色提取）与 **自定义 HSL 调色盘**。
  - 采用 M3E 多级表面容器色阶：背景底层使用深邃或温润的 `surfaceContainerLowest`，功能区块与文件卡片采用 `surfaceContainerLow` / `surfaceContainer`，高亮状态胶囊与激活筛选器采用高饱和度 `primaryContainer` 与 `secondaryContainer`。
- **Typography & Hierarchy**：
  - **零内嵌字体**：默认跟随系统 UI 字体，支持用户在扫描出的系统已装字体列表中自由指定全局字体族（例如 Roboto、Noto Sans CJK、DroidSansFallback、Serif、Monospace 或 OEM 系统预装字体）。
  - **M3E 阶梯字重映射**：当用户调节全局基准字重（如 400 Regular 或 500 Medium）时，标题（Headline/Title）自动向上偏移 +100~200 字重以保持视觉层级对比，正文与说明文字保持清晰易读。
- **Component Styling & Adaptive Layout**：
  - **三端自适应导航架构（FlClash 模式）**：
    - 窄屏（手机竖屏 `Compact`）：采用 M3E 悬浮胶囊底栏或顶部聚合仪表盘 + 预见性返回叠层。
    - 宽屏（平板/折叠屏/Windows 桌面 `Medium` & `Expanded`）：自动切换为左侧 `NavigationRail` / 侧边导航栏 + 右侧多列自适应网格画布（1~5 列动态响应）及双栏设置视图。
  - **M3E 卡片设计**：音频条目卡片配备专辑封面圆角缩略图、格式徽章（`FLAC` / `MP3` 胶囊标签）、双语歌词内嵌状态徽标及圆角药丸形（Pill-shaped）操作按钮。
- **Interactive Feedback & Motion**：
  - 全面采用弹簧物理曲线（`spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)`）替代线性过渡。
  - 列表项入场、移除、状态切换及设置面板折叠展开均具备顺滑的位移与缩放回弹反馈；保留并强化 AOSP 预见性返回手势（Predictive Back）跟手圆角卡片缩放预览。

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1：纯 Dart + Isolate 池 vs. Rust FFI 跨端解密引擎**
  - *Chosen Approach*：采用 **纯 Dart 结合 Worker Isolate 并发池** 实现 `.ncm` 解密与音频标签写入。
  - *Why*：`.ncm` 核心算法为头部 AES-128-ECB 解密（仅数百字节）与音频主体 RC4 异或流（纯字节数组位运算）。在 Dart AOT 编译优化下，配合 `Uint8List` 零拷贝视图与多 Isolate 并发，单曲 30MB FLAC 解密耗时仅约 80–150ms，且完全免去了 Windows Visual Studio C++ 工具链、iOS Xcode XCFramework 交叉编译与签名维护成本。
  - *Alternatives Considered*：Rust FFI (`flutter_rust_bridge`) 虽然单核吞吐略高，但引入了三端原生动态库编译与 CI 打包复杂度，对本应用的 I/O 密集型场景收益有限。
- **Decision 2：动态读取系统已装字体 vs. 内嵌开源字体包**
  - *Chosen Approach*：彻底移除内嵌 `.ttf` 字体文件，构建跨平台 **System Font Scanner & Loader（系统字体扫描与动态加载器）**。
  - *Why*：原先内嵌 6 个字重的 `Resource Han Round SC` 占用高达 ~45MB 体积。改为扫描系统字体（Android 通过 `android.graphics.fonts.SystemFonts.getAvailableFonts()` 与 `/system/fonts` 解析字体族名称及文件；Windows 枚举系统 `C:\Windows\Fonts` 与 DirectWrite 字体名；iOS 调用 `UIFont.familyNames`）后，安装包体积大幅缩减，且用户可直接选用系统中已安装的任意字体并实时调节字重。
  - *Alternatives Considered*：在线下载 Google Fonts 在国内网络环境下不稳定且增加缓存开销，不如直接枚举本机系统字体可靠。
- **Decision 3：FlClash 风格单窗口自适应架构 vs. 多端独立 UI 代码**
  - *Chosen Approach*：参考 FlClash 的 `AdaptiveScaffold` 响应式骨架，一套 Widget / Composable 树根据窗口宽度断点（`<600dp`、`600–840dp`、`>840dp`）自动切换布局形态与交互控件。
  - *Why*：保证 Android 手机、折叠屏、iPad 与 Windows 桌面窗口自由拖拽缩放时无缝衔接，同时在桌面端叠加 `desktop_drop` 拖拽层与 `tray_manager` 托盘控制。

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

#### 4.1 Flutter 三端（Android / Windows / iOS）跨平台总体架构图

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                   Presentation Layer (M3E × FlClash UI)                       │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌───────────────┐  │
│  │   Adaptive App Shell    │  │   M3E Conversion Hub    │  │ M3E Settings  │  │
│  │ • Compact: Top/Bottom   │  │ • Expressive Hero Stats │  │ • System Font │  │
│  │ • Expanded: NavRail +   │  │ • Multi-Column Grid     │  │   Picker/Prev │  │
│  │   Split Master-Detail   │  │ • Desktop Drop Zone     │  │ • HSL & Monet │  │
│  └────────────┬────────────┘  └────────────┬────────────┘  └───────┬───────┘  │
└───────────────┼────────────────────────────┼───────────────────────┼──────────┘
                │                            │                       │
                ▼                            ▼                       ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                 State & Orchestration Layer (Riverpod / Flow)                 │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌───────────────┐  │
│  │   QueueQueueNotifier    │  │   Theme & Font Engine   │  │  AppLogger &  │  │
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

#### 4.2 当前工作区同步升级架构（Android M3E 视觉重构 + 系统字体动态扫描器）

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                  Upgraded Android App (M3E + System Fonts)                    │
│                                                                               │
│  ┌──────────────────────────────┐       ┌──────────────────────────────────┐  │
│  │   SystemFontScanner (New)    │       │   M3E Expressive Theme Engine    │  │
│  │ • SystemFonts API (API 29+)  │──────▶│ • Dynamic Typography Builder     │  │
│  │ • /system/fonts & /product   │       │   (Selected Family + Weight)     │  │
│  │   TTF/OTF Name Table Parser  │       │ • M3E Surface Container Tokens   │  │
│  │ • FontFamily Cache & Preview │       │ • Expressive Shapes & Springs    │  │
│  └──────────────────────────────┘       └─────────────────┬────────────────┘  │
│                                                           │                   │
│                                                           ▼                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                 M3E Redesigned Screens & Components                     │  │
│  │  • HomeScreen: M3E Summary Dashboard Banner + Expressive Action Bar     │  │
│  │  • FileItemView: M3E Tonal Card + Format Badge + Animated Progress Pill │  │
│  │  • SettingsScreen: FlClash-style Grouped Card Sections + System Font    │  │
│  │    Search/Preview Modal Sheet + 100..900 Weight Slider                  │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────────┘
```

#### 4.3 核心模块跨平台实现与移植策略

1. **三端系统已安装字体扫描与动态加载机制（参考 FlClash）**：
   - **字体发现与元数据解析**：
     - **Android 端**：通过 `android.graphics.fonts.SystemFonts.getAvailableFonts()`（Android 10+）及扫描 `/system/fonts`、`/product/fonts`、`/system/etc/fonts.xml`，结合轻量级 TTF/OTF `name` 表解析器提取字体的真实字体族名称（Family Name，支持中英文显示名去重与排序）及对应的字体文件路径，并提供平台内置标准字体族（系统默认 `Sans-Serif`、`Serif`、`Monospace` 以及扫描到的全部系统字体文件）。
     - **Windows 端（Flutter）**：读取 `C:\Windows\Fonts` 与用户字体目录 `%LOCALAPPDATA%\Microsoft\Windows\Fonts` 或通过 `system_fonts` / Win32 DirectWrite 枚举已安装字体族名，直接将选中的 `fontFamily` 注入 Flutter `ThemeData(fontFamily: selectedFont)`。
     - **iOS 端（Flutter）**：通过 MethodChannel 调用 `UIFont.familyNames` 获取 iOS 系统预装与通过配置描述文件安装的字体族名列表。
   - **字体预览与字重动态映射**：
     - 在设置页提供「系统字体选择器」弹窗，支持关键词搜索、字体族列表实时渲染各自字体的预览文本，以及 `100 (Thin)` 到 `900 (Black)` 的字重滑块与复位按钮。

2. **纯 Dart + Isolate 解密与标签写入流水线**：
   - **加密算法 1:1 移植**：
     - `NcmFileParser`：校验 `CTENFDAM` 魔数，跳过 2 字节间距，按小端序读取密钥长度并用固定 Core Key (`0x687A4852416D736F356B496E62617857`) 执行 AES-128-ECB 解密获取 RC4 密钥盒种子；随后用 Meta Key (`0x2331346C6A6B5F215C5D2630553C2728`) 解密 Base64 编码的 JSON 元数据与内嵌封面。
     - `Rc4Engine`：构建 256 字节 S-Box 置换表，在 Worker Isolate 中以 32KB 分块流式解密音频载荷，通过 `SendPort` 节流回传进度百分比（每变化 ≥2% 通知一次 UI，避免主线程消息队列拥塞）。
     - `MetadataWriter`：纯字节操作构建 MP3 `ID3v2.3`（`TIT2`, `TPE1`, `TALB`, `USLT` 歌词帧, `APIC` 封面帧）与 FLAC `METADATA_BLOCK_VORBIS_COMMENT` / `METADATA_BLOCK_PICTURE`。

3. **三端平台专属交互适配层（Platform Adapter）**：
   - **Windows 桌面端**：
     - 集成 `desktop_drop` 监听窗口全局文件拖放，支持拖入单个或多个 `.ncm` 文件及包含 `.ncm` 的目录。
     - 集成 `window_manager` 与 `tray_manager`，支持关闭窗口时最小化至系统托盘继续后台批量转换，并提供右键托盘菜单（显示主窗口、开始全部转换、退出应用）。
   - **iOS 移动端**：
     - 配置 `Info.plist` 开启 `UIFileSharingEnabled` 与 `LSSupportsOpeningDocumentsInPlace`，声明 `.ncm` UTI 类型（`com.netease.cloudmusic.ncm`），使用户可在系统「文件」App 或微信/网盘中通过「用其他应用打开 / 分享」直接导入 NCM 转换器。
   - **Android 移动端**：
     - 保持 `OpenMultipleDocuments` 与 `OpenDocumentTree` SAF 持久化权限，配合 `DecryptService` 前台服务与通知栏进度更新。

#### 4.4 Interactive Component & State Mapping

| 交互区域 / 组件 | 触发动作 | 状态流转与处理器响应 |
| :--- | :--- | :--- |
| **首页 M3E 概览横幅与操作栏** | 点击「添加 NCM 文件」或拖入文件 | 过滤非 `.ncm` 文件 → 注入 `ConvertViewModel.files` 队列 → 顶部 M3E 状态面板动态更新待处理/已完成/总体积统计。 |
| **M3E 音频转换卡片 (`FileItemView`)** | 点击单曲「转换」或底部「全部转换」 | 状态由 `IDLE` → `PARSING` → `DECRYPTING (0..100%)` → `SEARCHING_LYRIC` → `WRITING_META` → `COMPLETED`；弹簧动画驱动进度条与状态徽章切换。 |
| **设置页 — 系统字体选择器** | 点击「系统字体」卡片 / 切换字体 / 拖动字重滑块 | 异步扫描系统已装字体列表 → 弹出支持搜索与实时字体样式预览的 M3E ModalBottomSheet → 选中后实时更新 `AppPrefs.selectedSystemFont` 与 `fontWeightFlow`，全局 `NcmConverterTheme` 即刻重绘生效。 |
| **设置页 — FlClash 风格分组卡片与色彩引擎** | 切换 Monet 动态取色 / HSL 滑块 / 网格列数 | 实时更新 `AppPrefs` 响应式 `StateFlow` → 动态重算 M3E `surfaceContainer` 层级色值与自适应网格列数。 |
| **跨端架构文档视图 / 导出** | 查看仓库内置 Flutter 移植蓝图与在设置中查阅说明 | 仓库内置完整 `FLUTTER_MIGRATION_BLUEPRINT.md`（含 Flutter `pubspec.yaml`、Isolate 核心代码、三端平台配置与组件映射），设置页同步更新说明。 |
