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

调试日志必须脱敏；真实账号只用于授权测试；发布前应重新评估 `QUERY_ALL_PACKAGES`、`credentials.xml` 的加密/备份边界和 WebView Cookie。`REQUEST_INSTALL_PACKAGES` 已因未发现应用内安装调用而移除，明文流量开关已关闭。

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

## 2026-08-12 - 统一底栏滚动显示时机

### 决定

底栏不再根据列表的上滑/下滑方向切换显示状态。所有接入主导航的 RecyclerView 在 `SCROLL_STATE_DRAGGING` 或 `SCROLL_STATE_SETTLING` 时隐藏浮岛，在 `SCROLL_STATE_IDLE` 时展示浮岛；返回、切换页面等主动恢复底栏的行为继续保留。

### 原因

方向判断会让一次下滑结束后底栏停留在隐藏状态，必须再次上滑才能恢复，和常见移动端导航栏“滚动中收起、停止后回到可操作状态”的预期不一致。使用 RecyclerView 的状态回调可以覆盖手指拖动和惯性滚动，且不依赖 `dy` 的正负。

### 影响

基础列表在 `BaseViewFragment` 统一转发滚动状态；本地关注、话题、消息和设置等自定义 RecyclerView 复用 `MainActivity.onContentScrollStateChanged`。分页加载仍只在空闲状态执行，内容布局和浮岛层级不变。

### 验证

`:app:assembleDebug`、`:app:testDebugUnitTest` 和 `:app:connectedDebugAndroidTest` 已通过。最新 Debug APK 安装到 Pixel_10 模拟器后，长滑进行中 UI 层级不包含 `bottomNavContainer`；上滑停止、下滑停止后均恢复为 `[95,2172][985,2340]`。

## 2026-08-12 - 移除首页应用 Tab

### 决定

首页仅保留内容浏览相关 Tab：关注、头条、热榜、话题、数码和酷图。移除“应用”Tab，并在读取首页菜单时过滤/删除已有的本地应用菜单记录；应用详情、搜索和更新相关底层代码暂不删除。

### 原因

用户希望主界面保持纯浏览，不在首页顶部暴露应用列表/管理入口。保留底层应用路径可以避免无关的深链和已有浏览能力发生破坏性变化。

## 2026-08-12 - 将“我的”和搜索纳入悬浮底栏

### 决定

将竖屏底栏从“首页 / 消息 / 设置”调整为“首页 / 消息 / 我的 / 搜索”。“我的”复用现有 `SettingsFragment` 和设置内容，只更换导航语义、图标和页面标题；“搜索”不新增 ViewPager 页面，直接启动现有 `SearchActivity`。首页顶部的搜索按钮移除，右侧菜单保留。

### 原因

用户希望底栏与 iOS 酷安的入口结构一致，并把搜索从页面顶部移动到底栏。项目没有独立的个人中心页面，复用现有设置页可以完成导航语义调整而不虚构个人资料、账号或社区功能。

### 验证

最终 Debug APK 在 Android 16 `emulator-5554` 安装并冷启动成功；底栏截图确认四项为“首页 / 消息 / 我的 / 搜索”，顶部首页搜索图标已移除。点击“搜索”后当前 Activity 为 `com.godmiracle.coolapk/.ui.search.SearchActivity`；点击“我的”后页面标题为“我的”且原设置内容可见。`:app:assembleDebug` 和 `:app:testDebugUnitTest` 均通过。

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

## 2026-08-12 - 更新关于页的维护者与 Fork 归属

### 决定

关于页以当前仓库远程地址确定维护者信息：显示 `godmiracle` 和 [godmiracle/Coolapk](https://github.com/godmiracle/Coolapk)。同时保留 [HDYOU/c001apk](https://github.com/HDYOU/c001apk) 的 Fork/参考来源、原项目仓库及原有贡献者信息，并在设置工具栏弹窗和反馈入口中统一使用当前仓库链接。

### 原因

原关于页仍显示旧项目的占位文案、旧 GitHub 仓库和开发者身份，容易把当前维护者与上游贡献混淆。当前仓库远程地址可作为本地可验证的维护者依据；未提供单独的真实姓名或头像，因此使用 GitHub 用户名和应用图标作为保守展示。

### 验证

Debug APK 构建成功，6 个 JVM 单元测试全部通过。Android 16 模拟器 UI 层级已确认完整关于页显示维护者、当前仓库、Fork/参考来源和原项目仓库；设置工具栏简版弹窗也显示相同信息。

## 2026-08-12 - 修复 Android 16 16 KB 页面大小兼容性

### 决定

将 `SketchImageViewLoader` 的 GIF 原生依赖从旧的 `io.github.panpf.sketch:sketch-gif:2.7.1` 切换为 `pl.droidsonroids.gif:android-gif-drawable:1.2.32`，并将 Android Gradle Plugin 升至 8.5.1。`SketchGifFactory` 和 `SketchGifDrawableImpl` 改为使用新库的公开 API，不再依赖旧 AAR 的 `me.panpf.sketch.gif.BuildConfig` 或 `GifDrawable` protected bitmap hook；应用的 instrumentation 测试以 test-only 依赖直接覆盖 GIF native 加载。

### 原因

Android 16 16 KB 模拟器启动旧包时明确显示 `lib/arm64-v8a/libpl_droidsonroids_gif.so : RELRO segment not aligned`，并记录 `PageSizeMismatchDialog`。Android 官方的 [16 KB 页面大小指南](https://developer.android.com/guide/practices/page-sizes)要求 native `.so` 满足对应对齐；上游 [android-gif-drawable 16 KB 支持 PR](https://github.com/koral--/android-gif-drawable/pull/847) 已在后续版本加入相关支持，因此升级到 1.2.32，并让 AGP 8.5.1 生成 16 KB 对齐的 App Bundle 配置。

### 影响

GIF 播放能力保留，但新版本不再暴露旧的 protected bitmap 生命周期 hook，因此移除了 Sketch 针对 GIF 的 `BitmapPool` 复用路径；非 GIF 图片的 bitmap pool 路径不变。这是 GIF 峰值内存/性能需要继续观察的行为变化。没有通过 `pageSizeCompat` 或其他兼容性开关隐藏问题，也没有修改 ABI 范围。

### 验证

在 Android Studio JDK 21.0.10、SDK Platform 36.1 和 `emulator-5554`（`PAGE_SIZE=16384`）上，`:app:assembleDebug`、`:app:testDebugUnitTest`、`:app:bundleDebug` 和 `:app:connectedDebugAndroidTest` 均成功；6 个 JVM 单元测试和 2 个 instrumentation tests 全部通过。APK 通过 `zipalign -P 16` 及 ELF LOAD/RELRO 检查，AAB `bundletool dump config` 为 `PAGE_ALIGNMENT_16K`；直接 APK 和 Bundle 生成的 APK 冷启动均未出现 `PageSizeMismatchDialog`，包状态为 `pageSizeCompat=0`。

## 2026-08-12 - 统一 Android 包名

### 决定

将 Android `applicationId`、`namespace`、源码包声明、测试包声明、ProGuard 规则和相关文档统一改为 `com.godmiracle.coolapk`；应用显示名称 `c001apk` 保持不变。

### 原因

原包名 `com.example.c001apk` 使用了示例域名前缀，不适合作为当前维护者的长期应用标识。新包名与当前仓库维护者 `godmiracle` 一致，且不改变现有功能和界面品牌。

### 影响与边界

包名变更会被 Android 视为新应用：旧安装不能直接覆盖升级，旧包的 SharedPreferences、数据库、登录状态和本地关注不会自动迁移。当前阶段仍是 Debug/个人使用，接受新旧包并存；正式发布前应固定该包名，并重新配置需要绑定包名的第三方服务。

### 验证

清理 Gradle/Hilt 增量产物后，`:app:assembleDebug` 和 `:app:testDebugUnitTest` 成功；新 APK 已安装为 `com.godmiracle.coolapk`，Android 16 模拟器冷启动成功，UI 层级确认首页、关注、我的和搜索底栏仍正常。

## 2026-08-13 - 实施 Code Review 五项高优先级修复

### 决定

- Room 迁移采用创建新表、复制兼容字段、删除旧表、重命名的前向迁移；`FeedFavoriteDatabase` 保留已发布 v2 schema，升至 v3 同时兼容历史 v1 `FeedFavorite`、历史 v2 `FeedFavorite` 和当前 v2 `FeedEntity`。
- API1/API2 的凭证拦截器改为网络拦截器，并按最终 HTTPS Host 做 allowlist；只有三个 Coolapk API Host 允许 Token、Cookie、设备和应用身份 Header，外部 `@Url` 与跨 Host 重定向自动清理。
- API、会话和设备请求状态从普通 `settings.xml` 隔离到 `credentials.xml`，旧键以同步、幂等方式迁移后清理；普通 UI 偏好不随凭证文件排除。两套 Android 备份规则均排除 `credentials.xml`。
- `WebViewActivity` 销毁时只执行资源释放，不再调用 `exitProcess(0)`；独立 `:webview` 进程由 Android 生命周期管理。
- `NetworkRepo` 使用可取消 Retrofit Call 适配器；取消调用底层 `Call.cancel()`，非 2xx 和必需空 body 失败，`CancellationException` 继续向上抛出。下载链接的 3xx 只在 no-redirect 场景显式允许，以保留 `Location` 读取。

### 原因

这五项分别对应数据丢失、凭证外泄、备份恢复泄露、Activity 关闭导致宿主进程退出和页面销毁后网络请求继续运行等高影响问题。修改保持现有 XML/Hilt/Retrofit/Room 架构，不实现登录流程，不移除接口访问所需的 `X-App-Token`，也不扩大到 Paging 3 重构。

### 验证

在 Android Studio JDK 21、Pixel_10（Android 17）上，`:app:testDebugUnitTest` 14 个用例、`:app:lintDebug`、`:app:compileDebugAndroidTestKotlin` 和 `:app:connectedDebugAndroidTest` 10 个用例全部通过。Instrumentation 覆盖 5 个 Room migration tests、2 个凭证迁移/备份规则 tests、1 个跨进程 WebView 生命周期 test，以及既有示例/GIF native tests。未验证 Release 签名、真实设备、真实账号和完整线上业务路径。

## 2026-08-13 - 竖屏主界面改为原生液态玻璃层

### 决定

- 参考 [BiliPai](https://github.com/jay3-yy/BiliPai) 的玻璃材质方向，在现有 XML/ViewBinding 架构内新增 `LiquidGlassFrameLayout`，不引入 Compose 或新的 UI 框架。
- 应用最低基线为 Android 12 / API 31，直接通过 `RenderEffect` 模糊指定的 `ViewPager2` 背景，玻璃容器内部的 Tab、导航文字和图标保持清晰；不再维护 Android 7–11 兼容回退。
- 竖屏底部拆成“三项玻璃导航条 + 右下角独立玻璃搜索圆钮”，顶部搜索入口不再增加；横屏继续使用原有 `NavigationRailView` 和四项菜单，避免改变横屏布局边界。
- 首页顶部只给 Tab 和编辑菜单增加玻璃容器，不新增业务标题或假搜索框。

### 原因

用户明确不需要顶部搜索，并希望沿用已有右下角搜索动作。当前项目以 XML/ViewBinding 为主，液态玻璃效果应集中在一个可复用原生容器中，避免为局部视觉改造迁移整套 Compose 依赖；既然维护范围限定为 Android 12+，无需继续保留旧系统降级实现。

### 影响

主导航的页面切换、搜索 Activity、滚动隐藏/停止后展示、系统 inset 和横屏导航逻辑保持原有边界。玻璃背景需要在内容滚动和 ViewPager 页面切换时刷新；Android 12+ 直接执行背景 View 重绘和模糊，仍需通过真实设备观察帧率和功耗。应用层同时移除了 API 24–30 的状态栏、下载、版本号、Tooltip 和旧图库保存分支。

### 验证

已通过变更布局的 `xmllint` 检查、`aapt2` 资源编译和 `LiquidGlassFrameLayout` 的 Android SDK 独立 Kotlin 编译。初次 Gradle 尝试曾因本地 socket 权限被拦截；随后已使用 Android Studio JDK 21 完成 APK 重建、安装、冷启动和 Android 16 真机视觉回归。

## 2026-08-13 - 液态玻璃界面设备验收

### 验证

- 使用 Android Studio JDK 21、Gradle Wrapper 8.14.5 和项目现有离线依赖缓存执行 `:app:assembleDebug :app:testDebugUnitTest --offline --max-workers=1`，构建成功；6 个测试套件共 22 个用例，0 failures、0 errors、0 skipped。
- 新 APK 已安装到 `emulator-5554`；设备为 Android 17 / API 37.1、16 KB page size。`MainActivity` 冷启动返回 `Status: ok`、`LaunchState: COLD`。
- 浅色截图确认顶部 Tab 玻璃区、三项底栏和右下角搜索圆钮存在；切换深色模式后重启，顶部文字、选中态、边缘高光、导航图标和搜索按钮仍保持可读，随后已恢复浅色模式。
- 在真实动态列表上执行持续长滑时，底栏和搜索按钮移出屏幕；停止滚动约 2 秒后底栏恢复，UI 层级边界为 `[74,2172][1038,2340]`。点击右下角搜索后，当前 Activity 确认为 `com.godmiracle.coolapk/.ui.search.SearchActivity`。

### Android 12+ 平台边界

APK `minSdkVersion=31`，`LiquidGlassFrameLayout` 直接调用 `RenderEffect` 和 `setRenderEffect`，不再包含 Android 7–11 的 API 守卫或半透明回退路径。`:app:lintDebug` 成功，报告中未出现 `NewApi` 或 `UnsupportedApiCall`；Android 16 / API 36 OPPO `PGEM10` 真机已完成安装、冷启动和液态玻璃界面回归。Android 7–11 已明确移出维护范围，不再安排旧系统构建或视觉验收。

## 2026-08-13 - Android 16 真机验收

### 验证

- 真机型号为 OPPO `PGEM10`，Android 16 / API 36，`arm64-v8a`；最新 Debug APK 安装成功。
- `MainActivity` 冷启动返回 `Status: ok`、`LaunchState: COLD`，最终 Activity 仍为 `MainActivity`；最近 300 行真机日志未发现 `FATAL EXCEPTION` 或 `AndroidRuntime`。
- 真机浅色截图确认顶部玻璃 Tab 区、三项玻璃底栏和右下角独立搜索圆钮正常渲染；UI 层级确认 `topGlassSurface`、`bottomNavSurface`、`searchActionSurface` 及首页/关注/我的三个入口均存在。
- 真机深色模式重启后，顶部玻璃区、底栏边缘高光、选中态、文字、图标和搜索圆钮保持可读；随后已恢复浅色模式。
- 点击右下角搜索按钮后，当前 Activity 确认为 `com.godmiracle.coolapk/.ui.search.SearchActivity`。
- 首页真实动态列表持续长滑期间，截图中底栏和搜索按钮均隐藏；停止滚动后恢复，UI 层级边界为 `[84,2112][1032,2304]`。

## 2026-08-13 - 首页顶部新增快讯 Tab

### 决定

“快讯”复用现有 `HomeFeedFragment` 的通用 `/v6/page/dataList` 分页链路，使用首页配置对应的 `V11_HOME_TAB_NEWS` 路由，并放在“热榜”和“话题”之间。新安装通过默认菜单列表获得该 Tab；已有安装在 `HomeViewModel` 启动时按当前菜单排序在“话题”前插入，保留用户已有的排序和启用状态。

### 原因

首页 Tab 本身由 `HomeMenu` 本地数据库驱动，单纯修改默认列表无法覆盖已有用户。运行时增量插入可以避免升级 Room schema，也不会覆盖用户自定义菜单；快讯数据的返回、分页和卡片展示已经由现有 `HomeFeedViewModel` 统一处理。

### 影响与边界

菜单编辑页需要清空并重建本地列表，避免存量菜单增量同步时重复显示。快讯内容仍依赖 Coolapk 线上 `V11_HOME_TAB_NEWS` 数据页；接口异常时沿用现有列表的加载失败/空状态处理。

### 验证

`:app:testDebugUnitTest` 22 个用例全部通过，`:app:assembleDebug` 和 `:app:lintDebug` 成功。Debug APK 安装到设备 `a60fe293` 后，首页 UI 层级确认“关注、头条、热榜、快讯、话题”顺序；点击“快讯”后 Tab 选中且真实列表显示“荣耀Magic9”“酷安手机资讯”等内容。

## 2026-08-13 - 新增发现底栏并迁移酷图

### 决定

主导航新增“发现”页，底栏顺序调整为“首页、关注、发现、我的”。发现页使用独立的顶部 Tab，仅包含“生活”和“酷图”；生活路由为 `V15_ZHUANTI_SHENGHUO`，酷图路由为 `V11_FIND_COOLPIC`，两者复用现有 `HomeFeedFragment` 的分页列表实现。

首页首次进入时默认选中“头条”，通过 `HomeViewModel.DEFAULT_TAB_TITLE` 集中维护默认 Tab 名称；用户后续切换 Tab 不被底栏顺序调整覆盖。

首页不再展示“酷图”。`HomeViewModel` 启动时删除存量 `home_menu.db` 中的“酷图”条目，默认菜单也不再生成该条目；不升级 Room schema，避免把已迁移数据和发现页状态混在首页菜单里。

### 原因

酷图属于发现内容，和首页的关注、头条、热榜、快讯、话题、数码不是同一组导航语义。独立发现页可以保留酷图原有数据接口，同时为生活内容提供明确入口；复用动态列表链路可保持现有加载、分页、错误和滚动行为。

### 验证

`:app:testDebugUnitTest` 22 个用例通过，`:app:assembleDebug` 和 `:app:lintDebug` 成功。设备 `a60fe293` 确认底栏为“首页、关注、发现、我的”、首页默认选中“头条”、发现页两个 Tab；生活页显示“户外兴趣小组”等真实内容，酷图页显示“#风景#”“AppleOS”等真实内容，首页顶部不再包含酷图。

## 2026-08-13 - 应用显示名称改为“绿友”

### 决定

- 将 `app/src/main/res/values/strings.xml` 中的 `app_name` 更新为“绿友”，让桌面图标、设置和关于页等复用该资源的用户可见位置统一使用新名称。
- 保留 `applicationId`、`namespace`、包路径、工程名、主题内部标识、历史资源目录和 Release 产物命名中的 `c001apk`，不做与显示名无关的兼容性迁移。
- README 只保留面向使用者和贡献者的项目定位、能力、构建和必要声明；接口地址、凭证实现、详细测试证据和历史会话索引继续放在 `docs/`。

### 原因

本次需求是调整应用品牌和公开说明，不是迁移 Android 身份或重命名历史代码。只修改显示名可以减少包名、深链、本地数据和已有安装的兼容风险，同时让 README 更适合作为项目入口文档。

## 2026-08-13 - GitHub Tag 自动发布签名 Release

### 决定

- 保留 `main` Push 和手动触发的 Actions 制品流程；新增 `v*` Tag 触发，用于正式发布路径。
- Tag 构建从 `SIGN_KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS` 和 `KEY_PASSWORD` 注入临时 `local.properties`，沿用 `verifyReleaseSigning` 阻止未签名或配置不完整的 Release。
- Tag 构建使用 runner 自带的 `gh release create`，配合 `--verify-tag` 和 `--generate-notes` 创建同名 GitHub Release，并上传 Release APK 与 `SHA256SUMS`。
- Job 声明 `contents: write`，仅由 `v*` Tag 步骤使用 `GITHUB_TOKEN` 创建 Release；不把 keystore、密码或 Token 写入仓库。

### 原因

将日常 CI 的 Actions 制品与对外 Release 分开，推送版本 Tag 即可得到可追溯的发布页，避免再次手动上传 APK 或忘记生成校验文件。`--verify-tag` 也能避免发布命令意外创建错误 Tag。

### 影响与边界

仓库必须先配置四个签名 Secrets；首次推送 `v*` Tag 后仍需检查远端 Actions、Release 资产和 APK 签名。workflow 目前不运行测试、lint 或真实设备验收；本地签名 Release 和 OPPO `PGEM10` 真机冷启动已单独完成，不能替代首次远端 Tag 验证。

### 验证

本次完成 workflow 与文档静态检查，未推送 `v*` Tag、未创建远端 Release。GitHub CLI 的参数和 Actions Token 权限依据官方文档配置，远端运行结果待首次发布后确认。

## 2026-08-14 - 按 BiliPai 源码移植底栏动效

### 决定

- 以 BiliPai 仓库中的 `FloatingBottomBar.kt`、`DampedDragAnimation.kt` 和底栏 motion spec 为行为参考，在 Coolapk 现有 XML/ViewBinding 架构内新增 `AnimatedBottomNavigationView`，不引入 Compose。
- 将底栏拆成 Material 菜单前景和独立绘制的选中胶囊：胶囊在菜单槽位间连续移动，落位时执行横向压缩、纵向补偿和阻尼回弹；底栏显隐在原有 `HideBottomViewOnScrollBehavior` 位移之外补充透明度与从底部的轻微缩放。
- 关闭 Material 自带 active indicator，避免静态指示器和自定义动效叠加；保留 Material 的菜单、选中态、点击和无障碍语义。横屏不使用该自定义 View，继续使用 `NavigationRailView`。

### 原因

用户需要参考上游项目的实际动效代码，而不是只复刻静态设计图。项目当前以 XML/ViewBinding 为主，独立 View 可以复用现有菜单和页面切换逻辑，同时移植 BiliPai 的动画结构与时长参数，避免引入整套 Compose 依赖。

### 验证

- 真机 `a60fe293` 安装最终 Debug APK，切换“首页 → 关注”后确认选中胶囊准确落在星标项；修复了坐标矩形重复偏移导致胶囊落在“发现”的问题。
- 80ms 真机截帧确认页面滑动与底栏选中态同步变化；滚动显隐仍由现有 RecyclerView 状态回调驱动。
- Android Studio JDK 21、离线依赖下 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=1` 成功；Lint 仅保留项目原有告警，未新增自定义底栏告警。

## 2026-08-14 - 对齐 BiliPai 底栏内容密度

### 决定

- 保留 BiliPai 的 64dp shell 和 56dp 选中指示器，但将 XML 底栏内容调整为 24dp 图标、11sp 标签、0dp Material 上下 padding。
- 在 `AnimatedBottomNavigationView` 的布局阶段，把 Material 默认“图标置顶、文字置底”的两个子树重新按 BiliPai 的垂直 `Column` 居中，图标与标签之间保持约 1dp；只移动视觉子树，不改变 item 的触摸和无障碍边界。
- Material `NavigationBarView` 会把透明 XML 背景替换成默认 `MaterialShapeDrawable`，因此显式移除自定义 View 根背景，让外层 `LiquidGlassFrameLayout` 成为唯一浮岛背景，避免内部白色矩形。

### 验证

- 真机 `a60fe293` 的 UI dump 显示底栏图标容器结束于 `y=2221`、标签组起始于 `y=2224`，实际间隔为 3px（约 1dp）；截图确认首页/关注选中胶囊、文字和独立搜索圆钮均正常。
- 收紧后的 `:app:assembleDebug` 成功；最终全量构建、单元测试和 Lint 继续作为本次交付验证项执行。

## 2026-08-14 - 修复 CI cache 失败并对齐 Tag 版本号

### 决定

- 将 workflow 的 `actions/cache` 从已停用的固定版本 `v4.0.2` 升级到 `v6.1.0`，避免在 Job 初始化阶段被 GitHub 自动拦截。
- `app/build.gradle.kts` 支持 `-PversionName` 覆盖默认的 Git 短 hash；`v*` Tag 构建把 `GITHUB_REF_NAME` 原样传入，因此 Tag `v1.0.0` 生成的 APK `versionName` 也是 `v1.0.0`。
- 非 Tag 的本地、`main` Push 和手动构建继续回退到 Git 短 hash，保留现有开发构建命名行为。

### 原因

远端 CI 运行 `31777925879` 在 Job 初始化阶段报告 `actions/cache@v4.0.2` 已停用，Gradle 尚未执行；远端 API 同时确认当前没有 Tag 或 Release。版本号此前只取 Git 短 hash，与 GitHub Release Tag 没有绑定关系。

### 影响与边界

修复提交推送后，需要重新推送一个未占用的 `v*` Tag；如果使用同一个 Tag，必须确认远端没有残留 Tag/Release 或先删除后重建。首次成功运行仍需检查 APK 签名、`versionName`、Release 资产和 `SHA256SUMS`，本次未代替用户执行远端推送。

### 验证

已通过远端 Actions API 和 Check Run annotation 确认失败原因；本地 `:app:assembleRelease -PversionName=v1.0.0 --offline` 构建成功，APK 文件名为 `c001apk_v1.0.0(457).apk`，badging 显示 `versionName='v1.0.0'`，v3 签名和 16 KB 对齐检查通过。GitHub Actions 远端运行仍待修复提交后的新 Tag。

## 2026-08-14 - 按推荐结构重做话题和数码详情页

### 决定

- 复用 `BasePagerFragment` 的 Toolbar、TabLayout、ViewPager2 和 `view` 占位，在折叠 AppBar 中注入主题头部；共享布局只新增默认隐藏的 `extraBar`，不改变用户页和应用页的占位替换行为。
- 关注按钮从 TopicFragment 菜单移动到头部，继续使用 `LocalFollowRepo` 的本地优先状态；点击后先反馈本地状态，再尽力同步服务端。
- 讨论排序使用当前 Tab 的原始 URL，只增删嵌套请求中的 `listType=dateline_desc` 或 `listType=rank_score`，不再拼接产品专用 `/product/feedList` 路由；切换时清空列表并重置分页游标。
- 头部只显示接口实际返回的 logo、简介和统计字段；不硬编码截图中的规则卡片、关注者头像或具体文案。

### 原因与边界

用户需要关注入口首屏可见，并能快速切换内容排序。当前真实接口观察到部分话题直接返回“最近回复 / 最新发布 / 热门动态”三个排序 Tab，而不是独立的“讨论”Tab；因此保留服务端 Tab 的兼容展示，只有存在“讨论”Tab 时才显示额外的“默认 / 最新 / 热度”单选栏，避免重复展示或伪造接口能力。

### 验证

Android Studio JDK 21 下，`:app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=1` 成功；Debug APK 已安装到设备 `a60fe293`。真实详情页确认头部 logo/标题/简介/统计可见，点击“关注”后按钮即时变为“已关注”；切换“最新发布”后首条动态和列表内容刷新；最近 250 行设备日志未发现 `FATAL EXCEPTION` 或 `AndroidRuntime`。尚未在一个明确返回“讨论”Tab 的真实话题上完成额外排序栏及其服务端 `listType` 参数验收；用户确认当前真实接口结构保持不变，因此不再为寻找该样本扩大本次变更范围。

## 2026-08-14 - 接受真实接口的现有排序 Tab 结构

### 决定

话题和数码页以真实接口返回为准：当前页面继续使用“最近回复 / 最新发布 / 热门动态”等服务端排序 Tab，不强行改造成截图中的独立“讨论”Tab。代码保留兼容“讨论”Tab 的默认/最新/热度内嵌排序栏，但不再把它作为当前页面必须出现的 UI。

### 原因

设备实测的真实话题接口没有返回独立“讨论”Tab。继续寻找特定样本会扩大交付范围，且可能制造与服务端数据模型不一致的页面结构；用户已确认保持现状。
