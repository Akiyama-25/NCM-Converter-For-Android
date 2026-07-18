---
name: i18n-bugs
description: 多语言切换功能两个 bug 已修复（2026-07-18）
metadata:
  type: project
---

## 多语言切换功能 Bug（2026-07-17 发现，2026-07-18 修复）

### Bug 1：设备语言为英语时界面显示中文，语言选项消失 ✅ 已修复

**根因**：`MainActivity.onCreate()` 中当 `savedLang == "system"` 时完全跳过 `setApplicationLocales()` 调用，导致 AppCompat 内部持久化的旧 locale（如 zh-SC）未被清除。

**修复**：始终调用 `setApplicationLocales()`，"system" 时传入空列表以清除 AppCompat 覆盖。

### Bug 2：设备语言为日语时 APP 启动崩溃 ✅ 已修复

**根因**：`android:localeConfig` 声明了非标准 BCP 47 标签（zh-SC/zh-TC），在 Android 13+ per-app language 系统中与 AppCompat 产生冲突。

**修复**：
- 移除 `android:localeConfig` 属性（APP 已有内置语言选择器）
- `setApplicationLocales()` 调用包裹 try-catch 防护

### 资源目录结构（当前状态）

```
values/          → English（默认/回退）
values-zh-rSC/   → 简体中文
values-zh-rTC/   → 繁体中文
```

### 修复方向

1. **语言重置逻辑**：对 "system" 应使用 `LocaleListCompat.getEmptyLocaleList()` 并确保 AppCompat 持久化被清除，或改用 `AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())` 配合 `recreate()`
2. **崩溃防护**：在 `AppCompatDelegate.setApplicationLocales()` 调用处添加 try-catch，或在 Application 类中统一处理 locale 初始化
3. **测试覆盖**：需要在英语、日语、中文等不同系统语言下测试语言切换和回退行为

**Why:** 用户需要在 APP 内手动切换语言，且未支持语言应回退到英语
**How to apply:** 修复时参考此文件中的分析，优先解决崩溃问题，再修复回退逻辑
