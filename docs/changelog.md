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
- 将竖屏 `BottomNavigationView` 按 Miuix 和 iOS 酷安参考改为实色浮岛底栏，使用 `surfaceContainer`、36dp 外侧留白、8dp 视觉底部间距、1dp 细边、低对比选中胶囊、28dp 图标、12sp 标签和内容安全区；背景由独立容器单层绘制，保留横屏 `NavigationRailView`。
- 将底栏入口调整为“首页 / 消息 / 我的 / 搜索”：原设置入口改名并复用原设置页，顶部首页搜索按钮移除，底栏搜索复用现有 `SearchActivity`。
- 将主导航调整为“首页 / 关注 / 我的 / 搜索”：移除原消息页在主导航中的内容入口；关注页改为本地话题/数码合并列表，详情页关注动作写入独立 Room 数据库；我的页面保留本地收藏、浏览历史并新增设置二级入口。
- 将 `SketchImageViewLoader` 的 GIF native 依赖切换为 `pl.droidsonroids.gif:android-gif-drawable:1.2.32`，并将 Android Gradle Plugin 升级到 8.5.1，以支持 16 KB 页面大小打包。
- 更新设置关于页的维护者、当前仓库和 Fork/参考来源信息；反馈入口统一指向 `godmiracle/Coolapk`。
- 将 Android `applicationId`、`namespace`、源码/测试包路径和 ProGuard 规则统一迁移到 `com.godmiracle.coolapk`；应用显示名称保持为 `c001apk`。

### Fixed

- 修正文档中“存在 `src/` 即为主源码目录”的误导；主源码实际位于 `app/src/main`。

### Verification

- 已完成静态源码、Gradle、Manifest、测试入口和 CI 配置核对。
- 已检测到 Android Studio JDK 21 和现有 SDK；用户提供的 Gradle 8.7 ZIP 已完成 SHA-256 校验并安装到用户缓存，维护者随后将项目 Wrapper 设置为 Gradle 8.14.5。
- 将 `me.panpf:sketch-gif:2.7.1` 切换为 `io.github.panpf.sketch:sketch-gif:2.7.1`；为适配新 AAR 移除旧 `BuildConfig.VERSION_NAME/VERSION_CODE` 日志引用。
- 在 Android Studio JDK 21.0.10、SDK Platform 36.1 下 Debug APK 构建成功，APK v2 签名核验通过；Debug 单元测试执行 4 个用例且全部通过。Release 签名和真实设备验收仍待完成。
- 本次悬浮底栏修改在 Android Studio JDK 21、SDK Platform 36.1 下完成最终 Debug 构建和 4 个 Debug 单元测试；Android 16 模拟器已完成最终包安装、冷启动、Miuix/iOS 参考图视觉复核和“消息 → 设置 → 首页”切换闭环验收，深色模式、主题色和滚动动画专项仍待完成。
- 底栏搜索迁移后的最终 Debug 包已在 Android 16 模拟器完成安装、冷启动；确认“首页 / 消息 / 我的 / 搜索”渲染，底栏搜索启动 `SearchActivity`，“我的”打开原设置内容并显示新标题。`git diff --check` 通过。
- 本地关注版本在 Android 16 模拟器完成空状态、话题关注、数码关注、应用重启后持久化和合并展示验证；`local_follow.db` 中两类记录均可在关注页展示，取消按钮可删除记录。
- 本地关注记录补充保存话题/数码详情的 `logo` 头像；旧数据库通过 Room 迁移保留，旧记录无头像时使用类型图标兜底。
- 整改底栏重复背景与底部距离：新增透明导航内容容器，浮岛背景只绘制一次；视觉底距从 26dp 收紧到 8dp，系统 inset 仍用于安全避让。
- 修复底栏隐藏后 `ViewPager2` 仍永久保留“底栏高度 + 系统 inset”造成的内容截短；移除主容器底部 padding，浮岛隐藏时内容可继续延伸至手势区上方。
- 统一底栏滚动状态：拖动或惯性滚动时隐藏，RecyclerView 进入空闲状态后展示，不再按上滑/下滑方向决定显示状态。
- 移除首页顶部“应用”Tab；默认菜单、已有本地菜单记录和 Tab 编辑器均不再展示应用列表入口，保留应用详情/更新底层代码供深链或其他浏览路径使用。
- 关于页不再显示旧项目占位文案；完整页与工具栏弹窗均保留上游作者、Fork/参考来源和当前维护者信息。
- Android 16 16 KB 模拟器曾复现 `libpl_droidsonroids_gif.so` 的 `RELRO segment not aligned`；修复后 Debug APK 通过 `zipalign -P 16`，AAB `bundletool dump config` 为 `PAGE_ALIGNMENT_16K`，APK/Bundle 生成的 APK 冷启动均无 `PageSizeMismatchDialog`，并通过 2 个 instrumentation tests（含 GIF native 加载回归）。
- 无 Release keystore 时 `:app:assembleRelease` 已按预期由 `:app:verifyReleaseSigning` 阻止，未把 Debug 签名产物误当作 Release。
- 包名迁移后清理增量产物重新构建成功，6 个 JVM 单元测试通过；新 APK 安装为 `com.godmiracle.coolapk` 并完成 Android 16 模拟器冷启动和主导航 UI 核对。
