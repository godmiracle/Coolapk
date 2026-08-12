# 技术决策与维护记录

本文档记录当前代码已经体现的架构选择、文档化后的维护约束和本次分析形成的判断。除非明确写出“本次变更”，其余条目是对现状的整理，不代表本次修改了业务代码。

## 2026-08-11 - 建立项目文档基线

### 决定

将 `README.md`、`docs/context.md`、`docs/architecture.md`、`docs/api.md`、`docs/development.md`、`docs/decisions.md`、`docs/todo.md` 和会话记录作为项目长期上下文的最小集合。

### 原因

原有文档大部分仍是模板占位内容，无法说明真实 Android 模块、API 主链路、Room 数据、发布签名和验证边界。当前仓库依赖外部 API，代码量较大，必须先把事实和未验证项分开记录。

### 影响

后续代码变更需要同步受影响的 API、架构、待办和会话文档；“源码存在”不能直接替代“构建/设备/线上验收通过”。

### 验证

本次已完成源码静态核对；初始环境没有可用 Java Runtime，后续改用 Android Studio 自带 JDK 21 和现有 Android SDK。用户提供的 Gradle 8.7 ZIP 已完成 SHA-256 校验并放入用户缓存，当前 Wrapper 按维护者设置使用 Gradle 8.14.5。旧 `me.panpf:sketch-gif:2.7.1` 坐标无法解析，已切换坐标并完成 Debug APK、签名和 4 个本地单元测试验证；Release 和设备验收仍未完成。

## 2026-08-11 - 保持 XML + ViewBinding/Data Binding 作为当前 UI 基线

### 决定

继续沿用现有 XML Layout、ViewBinding、Data Binding、Fragment、RecyclerView、ViewPager2 和 ViewModel，不在文档补全任务中引入 Compose 或重构 UI 层。

### 原因

当前 `app` 已有大量布局、BindingAdapter、自定义 View 和页面适配器；迁移 UI 会扩大任务范围并掩盖外部 API 与测试问题。

### 影响

新页面应优先复用 `BaseActivity`、`BaseFragment`、`BaseViewModel`、现有 Adapter 和加载状态模型。若未来迁移 Compose，应单独提出架构变更并补充性能/可访问性验收。

## 2026-08-11 - 以 Hilt NetworkRepo 作为当前网络主链路

### 决定

新增或修改业务接口时，默认沿用 `NetworkRepo`、`NetworkModule` 和带 Qualifier 的 Retrofit 服务，不再新增手工创建 Retrofit 的路径。

### 原因

静态搜索确认现有 UI ViewModel 普遍注入 `NetworkRepo`；`NetworkModule` 已统一 API1、API1 no redirect、API2 和 Account 的客户端及拦截器。

### 影响

`ApiService.kt`、`NetworkRepo.kt`、请求拦截器和调用方需要一起审查。`ApiServiceCreator`/`Network`/旧 `Repository` 保留为待清理遗留代码，清理前必须确认没有外部模块或隐藏入口依赖。

## 2026-08-11 - 按用途拆分 Room 数据库

### 决定

继续使用多个独立 Room 数据库和 Qualifier，分别存储历史、收藏、首页菜单、搜索、最近用户、最近表情和黑名单。

### 原因

现有 `DatabaseModule` 已按数据域拆分 Database/DAO/Repository，能避免不同用途的字符串实体互相污染，并支持独立迁移。

### 影响

修改 Entity 必须增加数据库版本和 Migration；不能用清库替代正常迁移。导出/清理功能需要明确区分 Room 数据和图片/HTTP 缓存。

## 2026-08-11 - 外链统一经 NetWorkUtil 路由

### 决定

Coolapk URL、`coolmarket://` 和网页链接先经过 `NetWorkUtil.openLink`，再路由到 Feed、App、User、Topic、Carousel、WebView 或外部浏览器。

### 原因

Manifest 的 `AppLinkActivity` 只负责接收深链，业务页面中也存在大量文本链接；集中标准化可以减少每个页面重复解析 URL。

### 影响

任何新增链接形态必须补充 `NetWorkUtil` 映射、Manifest 匹配和真实设备深链测试。未知 Scheme 默认复制链接，不应静默丢弃。

## 2026-08-11 - Git 派生版本与本地签名配置

### 决定

保留当前以 Git 提交数生成 `versionCode`、以短 hash 生成 `versionName` 的策略；签名密钥只从未跟踪的 `local.properties` 或 CI Secret 注入。

### 原因

该策略避免手动维护版本号和把密钥提交到仓库，并且 CI 已实现相同的 Release/Debug 构建逻辑。

### 影响

构建必须具备完整 Git 历史或至少可执行 Git 命令；发布前必须显式检查签名，不能把 Debug 签名的成功构建当作发布产物。

## 2026-08-11 - 敏感请求只允许用于个人学习和受控调试

### 决定

不在本次文档任务中扩大账号、Token、设备伪装、WebView Cookie、应用查询或 APK 安装能力；相关路径只记录风险和验收要求。

### 原因

代码会构造 Coolapk 请求 Header、保存登录 Token、写入 WebView Cookie、读取已安装应用并处理下载文件。扩大这些能力会引入安全、隐私、服务条款和分发审核风险。

### 影响

调试日志必须脱敏；真实账号只用于授权测试；发布前应重新评估 `QUERY_ALL_PACKAGES`、普通 SharedPreferences 存 Token 和 WebView Cookie。`REQUEST_INSTALL_PACKAGES` 已因未发现应用内安装调用而移除，明文流量开关已关闭。

## 2026-08-11 - 默认关闭并脱敏网络 BODY 日志

### 决定

统一通过 `NetworkLogging` 创建 OkHttp 日志拦截器。Debug 和 Release 默认均不输出 BODY；只有显式传入 `-PenableHttpBodyLogging=true` 且为 Debug 构建时才启用 BODY。日志输出会脱敏 Cookie、Authorization、设备/应用 Token、密码、验证码、STS 和常见 Token 字段；Release 由 `BuildConfig.DEBUG` 保证不会启用 BODY。

### 原因

`AddCookiesInterceptor` 和登录拦截器会主动写入身份凭据，原有 Debug BODY 日志会把请求头和请求体直接交给 Logcat。保留受控的显式开关可以支持个人调试，同时降低默认泄露风险。

### 影响

需要完整 HTTP 调试时使用受控的 Debug 命令，并检查脱敏结果；不得把启用 BODY 的日志外传。相关逻辑由本地单元测试覆盖；本轮已在 Android Studio JDK 21、SDK Platform 36.1 下执行 4 个 Debug 单元测试并全部通过。Release 和真实设备日志验收仍待完成。

## 2026-08-11 - Manifest 权限和明文流量收紧

### 决定

保留 `QUERY_ALL_PACKAGES`，因为应用列表页面和更新检查实际枚举已安装应用；移除未发现对应应用内安装调用的 `REQUEST_INSTALL_PACKAGES`。将 `usesCleartextTraffic` 设为 `false`，并在应用控制的 WebView 初始链接、HTTP 跳转、下载和外部打开路径中先升级为 HTTPS。

### 原因

当前网络 API、图片和账号服务均使用 HTTPS。已安装应用枚举是现有功能的一部分，而应用自身没有直接调用系统安装包 API；明文 HTTP 不应作为默认网络能力保留。

### 影响

不支持 HTTPS 的外部 HTTP 页面可能无法继续在 WebView 中加载，需要真实设备验证深链、网页跳转和下载行为。`QUERY_ALL_PACKAGES` 仍属于广泛可见性能力，发布分发前必须单独评估隐私披露和渠道要求。

## 2026-08-11 - Release 缺少签名密钥时失败

### 决定

Debug 始终使用 Debug 签名；Release 仅在存在 keystore 配置时设置 Release signing config，所有 Release 打包任务必须先通过 `verifyReleaseSigning`，缺少 keystore 文件或任一签名字段时直接失败。CI 与本地共用同一校验逻辑。

### 原因

Release 构建成功不应被误认为可发布 APK。失败前置到 Release 打包任务可以保留 Debug 开发体验，同时阻止无意发布 Debug 签名产物。

### 影响

执行 `:app:assembleRelease` 前必须在未跟踪的 `local.properties` 或 CI Secret 中提供完整签名配置；Debug 构建不受影响。仍需用真实 keystore 执行 APK 签名检查。

## 2026-08-11 - 集中维护 Retrofit Base URL

### 决定

新增 `NetworkEndpoints` 集中保存 API1、API2 和 Account Base URL，所有地址统一以 `/` 结尾；Hilt `NetworkModule` 和旧 `ApiServiceCreator` 均复用同一组常量。

### 原因

Retrofit Base URL 是两套网络封装的共同前置条件，集中维护可以避免一套配置修复而另一套继续使用裸域名。

### 影响

新增单元测试检查尾部斜杠并实际构造 Retrofit；本轮当前 JDK/SDK 下构建和单元测试已通过，仍需通过 Activity 启动验证网络注入链路。

## 2026-08-11 - 切换 Sketch GIF 的 Maven 坐标并兼容新 BuildConfig

### 决定

将 `SketchImageViewLoader` 的 GIF 依赖从无法解析的 `me.panpf:sketch-gif:2.7.1` 改为 Maven Central 发布的 `io.github.panpf.sketch:sketch-gif:2.7.1`。保留原有 `me.panpf.sketch.gif` Java 包名；同时移除旧代码对该 AAR 已不再提供的 `BuildConfig.VERSION_NAME` 和 `BuildConfig.VERSION_CODE` 的日志引用，仅保留 `BUILD_TYPE`。

### 原因

旧坐标在当前 Google Maven、Maven Central、JitPack、Sonatype 和 Gradle Plugin Portal 配置下均无法解析。新坐标的 AAR 可正常下载，但其 `BuildConfig` 只有 `DEBUG`、`LIBRARY_PACKAGE_NAME` 和 `BUILD_TYPE`，继续引用旧版本字段会在 `SketchImageViewLoader` Java 编译阶段失败。

### 验证

在 Android Studio JDK 21.0.10、SDK Platform 36.1、Gradle Wrapper 8.14.5 下，`:app:assembleDebug` 成功；`app-debug.apk` 通过 Android v2 签名验证；`:app:testDebugUnitTest` 执行 4 个用例且 0 failures、0 errors、0 skipped。

## 2026-08-12 - 将竖屏主导航改为悬浮底栏

### 决定

继续使用 Material 1.10.0 的 `BottomNavigationView` 承载现有三段式主导航，在竖屏布局中按参考图增加 24dp 水平边距、80dp 高度、18dp 基础底部间距、半透明淡紫玻璃背景、2dp 白色描边、阴影和主题色选中态；使用 Material 1.10 支持的 `itemActiveIndicatorStyle` 配置大选中胶囊，图标设为 30dp、标签设为 14sp 并始终展示。横屏继续使用现有 `NavigationRailView`。

### 原因

用户希望下方按钮参考截图呈现 Apple 风格的悬浮底栏。复用现有导航控件可以保留 ViewPager 切换、消息角标、滚动隐藏/显示和无障碍点击语义，同时把视觉变化限制在主布局、主题和资源层。截图包含四个视觉入口，但当前应用只有首页、消息、设置三个真实目的地，因此只复刻视觉结构，不凭空增加“超级用户”或“模块”页面。项目当前锁定 Material 1.10.0，使用该版本已提供的 active indicator 样式属性。

### 影响

竖屏底栏不再贴满屏幕底部，而是在左右各 24dp、基础底部 18dp 的位置悬浮；透明系统导航栏的 inset 通过动态 bottom margin 叠加处理，不再写入底栏内部 padding。列表和发布按钮沿用现有底部空间预留逻辑，横屏布局不受影响。

### 验证

在 Android Studio JDK 21、SDK Platform 36.1 下，`:app:assembleDebug` 和 `:app:testDebugUnitTest` 均成功；单元测试共 4 个，0 failures、0 errors、0 skipped。Android 16 模拟器 `emulator-5554` 安装成功、冷启动 `MainActivity` 成功，最终截图确认玻璃拟态悬浮栏、选中胶囊和底部手势区安全间距，UI 层级确认“消息 → 设置 → 首页”切换成功。深色模式、主题色和滚动隐藏/显示动画尚未专项验收。

## 现状中的待确认项

- 设置页的 `customToken`/`xAppToken` 已接入 `AddCookiesInterceptor`；仍需用人工输入的真实 Token 做设备联调，不能把“Header 已覆盖”当成 Token 本身有效。
- 短信登录 API 是否仍可用、UI 是否应重新接通，或应移除未完成入口。
- 旧网络层是否有仓库外调用者；确认后再统一到 `NetworkRepo` 或删除。
- Release 缺少 keystore 或签名字段时由 `verifyReleaseSigning` 阻止打包；仍需用真实 keystore 验证 APK 签名。
- Coolapk API、Cookie、Header、图片和 OSS 规则是否发生变化；必须以真实请求证据更新，而不是只改文档。

## 2026-08-12 - 将社区 API 整理作为排查参考，不直接覆盖现有路由

### 决定

将 [Coolapk-API-Collect](https://github.com/Coolapk-UWP/Coolapk-API-Collect) 作为非官方、带 CC-BY-4.0 归属的接口参考来源，先记录其与当前代码的差异，再通过单变量设备实验决定是否调整 Host、Token 或 Cookie。

### 原因

社区资料将首页 V8 和动态详情列为 `api.coolapk.com`，而当前项目的 `NetworkRepo` 将两者都通过 API2 服务调用；资料中的 V2 Token 使用整数 Unix 秒，而当前实现使用 `Float` 时间戳。模拟器实测同时观察到上游 `403 Forbidden`，但尚未用单变量实验证明具体拒绝原因。

### 影响

本次只补充文档和验证结论，不直接切换 API Host 或改变 Token 算法。下一次修复应先保留状态码和脱敏错误体，再按“详情 Host → Token 时间戳 → Cookie/请求头”的顺序逐项验证，避免多个变量同时变化。

## 2026-08-12 - 固定浏览请求身份并按社区资料修正首页/详情路由

### 决定

- `getHomeFeed` 和 `getFeedContent` 通过 API1 客户端访问 `https://api.coolapk.com/`。
- V2 `X-App-Token` 的时间戳使用整数 Unix 秒；已有 `customToken/xAppToken` 设置在启用且非空时覆盖自动生成值，默认仍自动生成。
- 启动时保留本地稳定的 `VERSION_NAME/VERSION_CODE/User-Agent`，不再把 `/v6/apk/detail` 返回的线上应用版本写回第三方客户端的请求身份。

### 原因

社区资料将首页 V8、动态详情和 `X-App-Token` 归到 API1/请求身份链路；A/B 设备实验进一步确认：API1 + 整数秒 Token + 默认 `13.4.1/2312121` 请求身份可以让应用详情、首页、动态详情和评论返回 200，而自动切换到线上 `16.5.1/2607271` 后首页再次返回 403。参考的 [HDYOU/c001apk](https://github.com/HDYOU/c001apk) 最新快照也已把远端版本写回逻辑注释掉，但其 Token 和首页/详情路由仍是旧实现，因此只作为对照证据。

### 验证

在 Android 16 `emulator-5554` 上安装脱敏诊断包后，日志确认 `X-App-Token` 已发送且被脱敏；应用详情、`/v6/main/indexV8`、`/v6/feed/detail?id=73186553` 和对应评论请求均为 `200 OK`，UI 层级确认首页动态正文和 `FeedActivity` 正文可见。正常 Debug 包随后需重新构建，确保 BODY 日志开关恢复为 `false`。

### 未决边界

这只证明当前模拟器和当前时间窗口的匿名浏览链路，不证明 Coolapk API 长期稳定，也不覆盖登录、发布、图片上传、真实设备或 Release 包。不要把任何真实 Cookie、设备码或 Token 写入日志、文档或聊天记录。

## 2026-08-12 - 修复 Android 16 16 KB 页面大小兼容性

### 决定

将 `SketchImageViewLoader` 的 GIF 原生依赖从旧的 `io.github.panpf.sketch:sketch-gif:2.7.1` 切换为 `pl.droidsonroids.gif:android-gif-drawable:1.2.32`，并将 Android Gradle Plugin 升至 8.5.1。`SketchGifFactory` 和 `SketchGifDrawableImpl` 改为使用新库的公开 API，不再依赖旧 AAR 的 `me.panpf.sketch.gif.BuildConfig` 或 `GifDrawable` protected bitmap hook；应用的 instrumentation 测试以 test-only 依赖直接覆盖 GIF native 加载。

### 原因

Android 16 16 KB 模拟器启动旧包时明确显示 `lib/arm64-v8a/libpl_droidsonroids_gif.so : RELRO segment not aligned`，并记录 `PageSizeMismatchDialog`。Android 官方的 [16 KB 页面大小指南](https://developer.android.com/guide/practices/page-sizes)要求 native `.so` 满足对应对齐；上游 [android-gif-drawable 16 KB 支持 PR](https://github.com/koral--/android-gif-drawable/pull/847) 已在后续版本加入相关支持，因此升级到 1.2.32，并让 AGP 8.5.1 生成 16 KB 对齐的 App Bundle 配置。

### 影响

GIF 播放能力保留，但新版本不再暴露旧的 protected bitmap 生命周期 hook，因此移除了 Sketch 针对 GIF 的 `BitmapPool` 复用路径；非 GIF 图片的 bitmap pool 路径不变。这是 GIF 峰值内存/性能需要继续观察的行为变化。没有通过 `pageSizeCompat` 或其他兼容性开关隐藏问题，也没有修改 ABI 范围。

### 验证

在 Android Studio JDK 21.0.10、SDK Platform 36.1 和 `emulator-5554`（`PAGE_SIZE=16384`）上，`:app:assembleDebug`、`:app:testDebugUnitTest`、`:app:bundleDebug` 和 `:app:connectedDebugAndroidTest` 均成功；6 个 JVM 单元测试和 2 个 instrumentation tests 全部通过。APK 通过 `zipalign -P 16` 及 ELF LOAD/RELRO 检查，AAB `bundletool dump config` 为 `PAGE_ALIGNMENT_16K`；直接 APK 和 Bundle 生成的 APK 冷启动均未出现 `PageSizeMismatchDialog`，包状态为 `pageSizeCompat=0`。
