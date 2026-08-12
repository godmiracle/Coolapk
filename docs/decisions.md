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

继续使用 Material 1.10.0 的 `BottomNavigationView` 承载现有三页主导航和一个搜索动作，但只把它作为 XML/ViewBinding 的透明内容控件；视觉参数按 Miuix 的 `FloatingNavigationBar` 和 iOS 酷安截图移植：外层使用 `surfaceContainer` 实色背景、36dp 外侧留白、8dp 视觉底部间距、无额外阴影、1dp 白色细边、28dp 图标、12sp 标签和低对比选中胶囊。关闭 Material 默认 active indicator 的绘制，改由自定义 item background 提供选中态。横屏继续使用现有 `NavigationRailView`。

### 原因

用户希望下方按钮接近 Miuix/ iOS 酷安的轻量浮岛，而不是 Material 3 的高对比导航容器。Miuix 当前浮动导航默认使用 `surfaceContainer`、36dp 外侧留白、低阴影和 28dp 图标；项目是 XML/ViewBinding，直接引入 Compose Miuix 会扩大架构范围，因此只移植可复用的视觉 token。iOS 截图有五个入口和中央加号，但当前应用只有首页、消息、我的三个页面入口，因此继续保留现有功能，不凭空增加页面；搜索另作为底栏动作项处理。

### 影响

竖屏底栏不再贴满屏幕底部，而是在左右各 36dp、系统手势区上方 8dp 的位置悬浮；系统栏 inset 只通过外层浮岛容器的动态 bottom margin 处理，透明的 `BottomNavigationView` 不再重复绘制背景或吸收可见 inset。ViewPager2 以 overlay 方式承载内容，不永久预留“底栏高度 + 8dp + 系统栏”空间；列表自身的系统底部 inset 和末尾 footer 负责滚动到末尾时的安全距离。横屏沿用同一导航菜单，现有 ViewPager 切换、消息角标、滚动隐藏/显示和无障碍点击语义保持不变。

### 验证

在 Android Studio JDK 21、SDK Platform 36.1 下，最终资源调整后的 `:app:assembleDebug` 和 `:app:testDebugUnitTest` 均成功；单元测试 6 个，0 failures、0 errors、0 skipped。Android 16 模拟器 `emulator-5554` 安装成功、冷启动 `MainActivity` 成功，最终截图确认实色浮岛、细边、选中胶囊、内容安全区和底部手势区；最终包执行“首页 → 消息 → 我的”切换，底栏搜索成功启动 `SearchActivity`，UI 层级确认四项标题和首页选中态。深色模式、主题色和滚动隐藏/显示动画尚未专项验收。

## 2026-08-12 - 整改浮岛重复背景与底部间距

### 决定

将竖屏底栏拆为“单独浮岛容器 + 透明 `BottomNavigationView`”：浮岛容器只绘制一次背景、圆角和边框，Material 导航控件只负责图标、文字和选中项背景。底部视觉间距由 26dp 调整为 8dp，系统导航栏 inset 仍保留为安全区，不参与额外的装饰留白。横屏使用同名容器承载 `NavigationRailView`，但不套用竖屏浮岛行为。

### 原因

原实现把自定义浮岛背景直接挂在 `BottomNavigationView` 上，同时让 Material 导航控件参与自身的背景/inset 处理，容易产生底栏后方的第二层背景；固定 26dp 再叠加系统 inset 也使浮岛离底部过远。

### 验证

Debug APK 在 Android 16 `emulator-5554` 安装并冷启动成功；截图确认竖屏底栏容器边界为 `[95,2172][985,2340]`，底栏与手势区域的间距已收紧，关注页显示单层浮岛。首页/关注/我的切换、底栏搜索启动 `SearchActivity` 均通过；横竖屏布局均通过构建检查。

## 2026-08-12 - 修复底栏隐藏后的内容截短

### 决定

移除 `MainActivity` 对 `ViewPager2` 的永久底部 padding。浮岛仍由独立 `bottomNavContainer` 负责绘制和滚动隐藏/显示；主页面内容不再因为底栏当前是否可见而被固定截短，系统手势区继续由浮岛容器的 bottom margin 和列表自身 inset 处理。

### 原因

修复前 `ViewPager2` 的内部内容列表固定结束在约 `2172px`，即使底栏已经被 `HideBottomViewOnScrollBehavior` 隐藏，屏幕底部仍保留约 `252px` 空区，表现为内容被下方空白层遮挡。该空区来自主容器 padding，不是第二个浮岛背景。

### 验证

- 修复前上滑隐藏底栏的截图出现内容结束后到手势区之间的整块空白。
- 修复后 `ViewPager2` 和内部 RecyclerView 均延伸至屏幕高度 `2424px`；上滑隐藏底栏后内容可继续显示到系统手势区上方，空白层消失。
- 下滑恢复底栏后，浮岛仍显示在 `[95,2172][985,2340]`，与系统手势区保持安全距离。
- Android Studio JDK 21 下 `:app:assembleDebug`、`:app:testDebugUnitTest` 通过；Debug APK 已安装到 Android 16 `emulator-5554` 并完成冷启动、隐藏/恢复回归。

## 2026-08-12 - 将“我的”和搜索纳入悬浮底栏

### 决定

将竖屏底栏从“首页 / 消息 / 设置”调整为“首页 / 消息 / 我的 / 搜索”。“我的”复用现有 `SettingsFragment` 和设置内容，只更换导航语义、图标和页面标题；“搜索”不新增 ViewPager 页面，直接启动现有 `SearchActivity`。首页顶部的搜索按钮移除，右侧菜单保留。

### 原因

用户希望底栏与 iOS 酷安的入口结构一致，并把搜索从页面顶部移动到底栏。项目没有独立的个人中心页面，复用现有设置页可以完成导航语义调整而不虚构个人资料、账号或社区功能。

### 验证

最终 Debug APK 在 Android 16 `emulator-5554` 安装并冷启动成功；底栏截图确认四项为“首页 / 消息 / 我的 / 搜索”，顶部首页搜索图标已移除。点击“搜索”后当前 Activity 为 `com.example.c001apk/.ui.search.SearchActivity`；点击“我的”后页面标题为“我的”且原设置内容可见。`:app:assembleDebug` 和 `:app:testDebugUnitTest` 均通过。

## 2026-08-12 - 将关注改为本地话题/数码聚合

### 决定

主导航不再承载原消息聚合页，改为“首页 / 关注 / 我的 / 搜索”。关注页只读取独立的 `local_follow.db`，以一张合并列表展示用户在话题或数码详情页主动关注的条目；没有本地记录时显示空状态，不请求原消息页数据，也不把服务端的完整关注列表直接当成本地列表。

话题和数码详情页的“关注”动作先写入或删除本地记录，再在登录状态下尽力同步服务端；网络失败不阻断本地关注。记录同时保存详情返回的 `logo` 作为头像，未登录时只维护本地状态，避免本地关注功能被账号状态阻断。记录使用 `(type, targetId)` 作为复合主键，列表按最近操作时间倒序；旧记录没有头像时继续使用类型图标兜底，并在再次打开详情后补齐头像。

“我的”页面只保留本地收藏、浏览历史和设置三个入口；原有设置项通过独立 `SettingsActivity` 作为二级页面承载，避免设置选项与本地内容混在同一层级。

### 验证

在 Android Studio JDK 21、SDK Platform 36.1 下，`:app:assembleDebug` 和 `:app:testDebugUnitTest` 成功，6 个 JVM 测试全部通过。Android 16 `emulator-5554` 已验证：关注页初始空状态、话题 Android 关注、数码“小米17 Pro Max”关注、应用重启后两类记录合并展示；详情页取消本地关注入口和“我的 → 设置”入口已接入。网络详情页是否返回服务端关注成功由外部 API 决定，但本地数据库和列表链路已在模拟器中实测。

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
