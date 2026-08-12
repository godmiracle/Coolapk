# Changelog

本文件使用简化版 Keep a Changelog，记录项目和文档的可追踪变化。外部 Coolapk API 的线上变化不在本文件中猜测，需以请求证据和对应会话记录为准。

## Unreleased

### Added

- 补全项目定位、功能边界、模块结构、技术栈和安全声明。
- 新增 `docs/api.md`，记录 API1/API2/Account 服务、请求身份和关键业务流程。
- 新增 `docs/development.md`，记录构建、签名、测试、CI 和人工验收步骤。
- 新增 2026-08-11 文档整理会话记录。
- 新增 2026-08-12 主导航悬浮底栏会话记录。

### Changed

- 用当前源码事实替换 `docs/context.md`、`docs/architecture.md`、`docs/decisions.md` 和 `docs/todo.md` 的模板占位内容。
- 明确区分 `NetworkRepo` 当前主链路与旧 `Network/Repository` 遗留链路。
- 把凭据存储、BODY 日志、广泛权限、明文流量、Release 签名和业务测试缺口列为可追踪待办。
- 处理 R-002、R-004、R-005、R-013：网络日志默认关闭并脱敏，移除未使用的安装包权限，关闭明文流量并升级应用控制的 HTTP 链接，Release 缺少签名配置时失败，统一 Retrofit Base URL 尾部斜杠。
- 将竖屏 `BottomNavigationView` 按参考图改为半透明淡紫玻璃风格的悬浮底栏，增加白色描边、大选中胶囊、30dp 图标、14sp 标签和系统栏安全间距，保留横屏 `NavigationRailView`。
- 将 `SketchImageViewLoader` 的 GIF native 依赖切换为 `pl.droidsonroids.gif:android-gif-drawable:1.2.32`，并将 Android Gradle Plugin 升级到 8.5.1，以支持 16 KB 页面大小打包。

### Fixed

- 修正文档中“存在 `src/` 即为主源码目录”的误导；主源码实际位于 `app/src/main`。

### Verification

- 已完成静态源码、Gradle、Manifest、测试入口和 CI 配置核对。
- 已检测到 Android Studio JDK 21 和现有 SDK；用户提供的 Gradle 8.7 ZIP 已完成 SHA-256 校验并安装到用户缓存，维护者随后将项目 Wrapper 设置为 Gradle 8.14.5。
- 将 `me.panpf:sketch-gif:2.7.1` 切换为 `io.github.panpf.sketch:sketch-gif:2.7.1`；为适配新 AAR 移除旧 `BuildConfig.VERSION_NAME/VERSION_CODE` 日志引用。
- 在 Android Studio JDK 21.0.10、SDK Platform 36.1 下 Debug APK 构建成功，APK v2 签名核验通过；Debug 单元测试执行 4 个用例且全部通过。Release 签名和真实设备验收仍待完成。
- 本次悬浮底栏修改在 Android Studio JDK 21、SDK Platform 36.1 下完成 Debug 构建和 4 个 Debug 单元测试；Android 16 模拟器已完成安装、冷启动、参考图视觉复核和三段底栏切换验收，深色模式、主题色和滚动动画专项仍待完成。
- Android 16 16 KB 模拟器曾复现 `libpl_droidsonroids_gif.so` 的 `RELRO segment not aligned`；修复后 Debug APK 通过 `zipalign -P 16`，AAB `bundletool dump config` 为 `PAGE_ALIGNMENT_16K`，APK/Bundle 生成的 APK 冷启动均无 `PageSizeMismatchDialog`，并通过 2 个 instrumentation tests（含 GIF native 加载回归）。
- 无 Release keystore 时 `:app:assembleRelease` 已按预期由 `:app:verifyReleaseSigning` 阻止，未把 Debug 签名产物误当作 Release。
