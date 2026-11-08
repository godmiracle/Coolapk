# 项目上下文

## 文档基线

- 项目：`绿友`（技术项目标识仍保留 `c001apk`）
- 分析日期：2026-08-13
- 当前分支：`main`
- 当前 Git HEAD：`d41d41d23f001d85a567a9b75dacbe5949d945ce`（短哈希 `d41d41d`）；S-09 change 的实现、测试和 fixture 仍以当前工作树为准。
- 源码提交时间：2026-08-12 17:11:51 +08:00
- 工作区状态：当前包含该 change 的未提交代码、测试、fixture、文档和 Rasen 流程文件；本轮 docs fixer 只修改本文件及另外三份指定文档，不将工作区视为干净基线。

## 项目定位

这是一个个人学习型 Android 客户端，使用包名 `com.godmiracle.coolapk`、应用名 `绿友`。技术项目标识和部分历史资源仍保留 `c001apk`，以避免不必要的包名、数据和兼容性迁移。它复现 Coolapk 客户端的部分内容浏览和社区交互流程，重点用于：

1. 学习 Android XML UI、Fragment、ViewModel、RecyclerView 和 Data Binding。
2. 学习 Retrofit/OkHttp 请求、设备参数和分页接口适配。
3. 学习 Room 本地历史/收藏/设置数据管理。
4. 学习富文本、表情、图片九宫格和全屏预览。

项目不是 Coolapk 官方应用，没有独立服务端，也没有稳定的后端契约。接口兼容性依赖外部服务，源码中出现的“已实现”只能说明客户端存在相应代码路径。

## 当前目标

当前最重要的目标不是扩展功能，而是先建立可维护、可验证的工程基线：

- 让新贡献者能在与项目兼容的 Android Studio/Gradle 环境中完成构建；CI 基线为 JDK 17，本轮本地按要求使用 Android Studio JDK 21 和现有 SDK。
- 识别当前主链路和历史遗留链路，避免修错代码入口。
- 对设备/API 身份、WebView、应用下载等仍存在的高风险路径建立人工验收清单；已移除的登录和图片上传不再安排验收。
- 逐步补充业务测试和 CI 验证，降低外部 API 变化带来的回归风险。

## 当前状态

### 已确认

- 主应用模块、三个本地图片相关 library module 和 Gradle 多模块配置存在。
- 主导航页面为首页、关注、我的，右侧搜索项启动独立搜索页；关注页是本地话题/数码关注聚合，“我的”包含本地收藏、浏览历史和设置入口，首页 Tab 可通过 Room 配置。
- 当前 ViewModel 普遍注入 `logic.repository.NetworkRepo`，网络主链路使用 Hilt 提供的 API1、API2 Retrofit 服务；`NetworkRepo` 通过可取消的 Call adapter 处理响应、错误和协程取消。
- 当前产品为匿名只读浏览加本地功能：本地数据包括话题/数码关注、浏览历史、动态收藏、首页菜单、搜索历史、用户黑名单和话题黑名单；最近 @用户和最近表情的编辑器存储已移除。
- `DatabaseModule` 只对仍在使用的 `FeedFavorite`、`HomeMenu`、`LocalFollow` 和 `StringEntity` 数据库保留兼容迁移；账号、消息、发帖和回复相关迁移/测试不属于现行范围。
- Manifest 包含主入口、Coolapk/自定义 Scheme 深链、WebView 独立进程、图片分享用 FileProvider 和网络状态权限；旧的第三方应用批量更新链路已移除，客户端更新不声明 `REQUEST_INSTALL_PACKAGES`，只通过系统浏览器打开 Release 页面。
- API1、API2 客户端在最终网络发送边界检查 HTTPS 与受信任 Host；不可信 `@Url`/重定向会清理请求身份。API 请求身份采用参考 `XiaoMengXinX/c001apk_next` 的匿名 iOS UUID、设备档案、Header 和 Token；本轮源码已切换，真实接口仍待验收。当前不提供 Account 客户端、登录状态同步或消息请求；`WebViewActivity` 销毁时只释放资源，不主动结束进程。
- GitHub Actions 在 `main` Push/手动触发时构建 Release/Debug 并上传 Actions 制品；推送 `v*` Tag 时将 Tag 作为 APK `versionName`，额外创建同名 GitHub Release，上传签名 APK 与 `SHA256SUMS`。`v1.1.0` Release 已验证，资产为 `c001apk_v1.1.0.463.apk` 和 `SHA256SUMS`；客户端更新流程读取同一仓库的 latest Release，并提供固定 Release 网页入口作为浏览器备用路径。CI 仍没有运行业务测试、lint 或真实设备更新验收。

### 源码覆盖但尚未证明

- Coolapk 线上 API 是否仍接受当前路径、Header、版本号、Token 和分页参数。
- Coolapk 应用下载是否能在真实设备上完整走通；GitHub latest API 检查和 Release 浏览器跳转已在本轮 OPPO `PGEM10` 验证。账号登录、验证码、动态发布、回复和图片 OSS 上传已明确移出产品范围。
- 所有页面在 Android 12 至当前系统、横屏、深色模式和低网速下的稳定性。
- Release R8/资源压缩后反射、Data Binding、图片库和 WebView 是否仍无问题。

### 已知维护风险

- `logic/network/Network.kt` 与 `logic/network/Repository.kt` 保留了一套非 Hilt 的旧网络封装；当前 ViewModel 调用的是 `logic/repository/NetworkRepo.kt`，两套代码存在重复漂移风险。
- `ApiServiceCreator.kt` 只被旧 `Network.kt` 使用，API URL、OkHttp Client 和 Retrofit 配置在两处重复维护。
- API Base URL 已由 `NetworkEndpoints` 集中维护并统一以 `/` 结尾；API1、API2 的设备/API 身份请求由 HTTPS + trusted-host 边界统一保护。历史会话记录包含构建、单元测试和首页/动态详情启动证据；本轮新增的 GitHub 更新验证单独记录在 2026-08-27 会话和 Rasen evidence 中。
- Debug 网络 BODY 日志默认关闭；显式开启时由 `NetworkLogging` 脱敏，请求本身仍会携带设备/API 参数。
- 设备/API 身份已从普通 UI 设置隔离到 `credentials.xml`；`backup_rules.xml` 与 `data_extraction_rules.xml` 排除该文件。当前仍是普通（未加密）SharedPreferences，隔离和备份排除不等于加密存储。
- `usesCleartextTraffic` 已关闭；旧的应用列表/第三方应用更新检查已移除，因此当前不再声明 `QUERY_ALL_PACKAGES`。客户端更新不声明 `REQUEST_INSTALL_PACKAGES`，只通过固定 Release 页面交给系统浏览器。
- 自定义 Token 设置项在 `PrefManager`/设置界面中存在；当前仅在开关开启且值非空时覆盖自动生成 Token，真实自定义 Token 联调仍待人工验证。
- 自动化测试已包含 Room 迁移、请求 Host 边界、凭证迁移/备份、网络取消/HTTP 错误和 WebView 生命周期回归；它们仍不能替代真实账号、真实设备、实时 API 或完整业务验收。

## 范围与非目标

### 范围

- Android 手机客户端。
- Coolapk 内容浏览和不依赖账号的本地功能。
- 本地历史、收藏、黑名单、首页布局和设置。
- 图片展示、保存/分享/复制；回复仅保留服务端返回的只读展示。

### 非目标

- 不实现 Coolapk 服务端。
- 不承诺对外部 API 的永久兼容。
- 不在没有用户确认和真实验收的情况下宣称“可发布”或“全功能可用”。
- 登录入口已从当前产品移除；客户端更新已接通 GitHub latest Release 查询和固定 Release 浏览器入口，且只有用户点击按钮时才请求，应用不负责 APK 下载或安装。旧的第三方应用批量更新链路已移除，应用详情单个 APK 下载仍保留。Debug 与 Release 使用不同 applicationId；正式包覆盖安装仍要求同包名、同签名和更高 `versionCode`。自定义 Token 仅视为已接通请求 Header 的调试选项，不代表已有可用账号 Token。

## 重要约束

| 约束 | 现状 |
|---|---|
| 平台 | Android，`minSdk 31`（Android 12+），`target/compileSdk 34` |
| 构建 | Kotlin 1.9.24、Java 17、AGP 8.5.1、Gradle Wrapper 8.14.5 |
| ABI | `arm64-v8a`、`armeabi-v7a`、`x86_64`；未启用 `x86`/`armeabi` |
| UI | 继续沿用 XML + ViewBinding/Data Binding；没有证据表明要迁移 Compose |
| 服务端 | 依赖 `api.coolapk.com`、`api2.coolapk.com` 和相关图片地址；客户端更新只读 `api.github.com`/`github.com` 的固定仓库 Release，网络受限时交给系统浏览器，不再接入 Account/OSS 写入服务 |
| 账号 | 当前无登录能力；Cookie、Token、密码、验证码和上传 STS 凭据不得进入仓库或共享日志 |
| 发布 | 本地从未提交的 `local.properties` 读取 Release 密钥；CI 从 Secrets 写入临时 `local.properties`；缺少完整配置时 `verifyReleaseSigning` 阻止 Release 打包；`v*` Tag 作为 APK `versionName` |
| 网络 | Manifest 关闭明文流量；应用控制的 HTTP 初始链接、跳转、下载和外部打开路径先升级为 HTTPS；带凭证客户端只保留三个 Coolapk HTTPS Host |
| 16 KB 页面大小 | Debug APK 的 native 库已通过 16 KB 对齐校验；Debug AAB 已声明 `PAGE_ALIGNMENT_16K`，16 KB 模拟器冷启动和 GIF 原生加载回归通过 |
| 法律/许可 | 根目录包含 AGPL-3.0 文本；第三方代码、图片、字体和上游库需要单独核对许可证 |

## 关键文件

| 路径 | 用途 |
|---|---|
| `app/build.gradle.kts` | 应用 SDK、签名、版本、ABI、依赖和构建类型 |
| `app/src/main/AndroidManifest.xml` | 权限、入口 Activity、深链、WebView 进程和 FileProvider |
| `app/src/main/java/com/godmiracle/coolapk/MyApplication.kt` | Hilt 应用入口、主题初始化、Mojito 初始化、未捕获异常处理 |
| `app/src/main/java/com/godmiracle/coolapk/di/NetworkModule.kt` | Hilt Retrofit/OkHttp 服务和请求拦截器 |
| `app/src/main/java/com/godmiracle/coolapk/logic/network/NetworkEndpoints.kt` | API Base URL、trusted-host 判断和凭证清理策略 |
| `app/src/main/java/com/godmiracle/coolapk/logic/network/ApiService.kt` | Retrofit 请求定义 |
| `app/src/main/java/com/godmiracle/coolapk/logic/repository/NetworkRepo.kt` | 当前 ViewModel 使用的网络 Repository |
| `app/src/main/java/com/godmiracle/coolapk/logic/repository/GitHubReleaseRepository.kt` | GitHub latest Release 元数据校验 |
| `app/src/main/java/com/godmiracle/coolapk/ui/clientupdate/` | 客户端更新设置页、状态和固定 Release 浏览器入口 |
| `app/src/main/java/com/godmiracle/coolapk/logic/repository/NetworkCallAdapter.kt` | Retrofit Call 的取消、HTTP、空 body 和异常适配 |
| `app/src/main/java/com/godmiracle/coolapk/logic/dao/`、`logic/database/` | Room DAO、数据库和迁移 |
| `app/src/main/java/com/godmiracle/coolapk/util/PrefManager.kt` | 普通 UI 设置与设备/API 请求身份状态；清理历史账号键 |
| `app/src/main/java/com/godmiracle/coolapk/util/CredentialPreferencesMigration.kt` | 清理历史账号键并保留设备/API 身份的兼容迁移 |
| `app/src/main/java/com/godmiracle/coolapk/util/NetWorkUtil.kt` | 外部链接标准化和内部页面路由 |
| `app/src/main/java/com/godmiracle/coolapk/ui/main/MainActivity.kt` | 首页/关注/我的主导航和搜索动作 |
| `app/src/main/java/com/godmiracle/coolapk/ui/feed/` | 动态详情、评论、投票、问答和只读楼中楼展示 |
| `.github/workflows/ci.yml` | GitHub Actions 构建与制品上传 |
| `gradle/libs.versions.toml` | 依赖版本目录 |

## 运行环境记录

本次分析环境：macOS、工作目录 `/Users/v/ABP/Coolapk`、日期 2026-08-14。此前已使用 Android Studio JDK 21.0.10、SDK Platform 36.1 和 Gradle Wrapper 8.14.5 构建本地签名 Release，完成 APK 签名/16 KB 对齐检查，并安装到 OPPO `PGEM10`（Android 16 / API 36、arm64-v8a）完成 `MainActivity` 冷启动验证。远端 CI 首次运行 `31777925879` 在 Job 初始化阶段因 `actions/cache@v4.0.2` 停用失败；当前远端没有 Tag 或 Release。本次修复还将 `v*` Tag 传入 Gradle `versionName`，待提交后重新运行验证；真实账号、实时 API 长期稳定性和完整业务验收仍未完成。

后续每次真实验收应记录：Android 版本、设备型号、ABI、网络类型、应用构建哈希、服务端响应状态和截图/日志位置。

## 2026-08-26 LiquidGlass 页面 Chrome 扩展记录

本轮在既有 XML/ViewBinding 架构内，将共享原生 LiquidGlassFrameLayout 扩展到 BasePager、User、Feed 和 Search 的页面级操作 Chrome。玻璃层只取同一 Window 内的内容 sibling，前景 Toolbar、Tab、输入框和 Material 控件保持清晰；Feed 排序栏恢复为普通不透明表面，不能采样或模糊回复列表。普通卡片、RecyclerView item、评论/回复、头像、封面、图片、加载/错误、Preference、WebView、BottomSheet 和 Dialog 不作为玻璃对象。当前匿名只读范围不保留 FAB、登录、消息、发布或回复编辑入口。

实现不引入 Compose 或外部 AndroidLiquidGlass 依赖，最低平台继续为 Android 12/API 31，目标 SDK 34。LiquidGlassFrameLayout 直接使用 RenderEffect，具备主题色/透明度、模糊、圆角、边框、高光、source 刷新、fallback 和 attach/detach 清理。

已连接设备为 OPPO PGEM10、Android API 36、arm64-v8a、1080×2376、手势导航。当前 Debug 包使用独立的 `com.godmiracle.coolapk.debug` applicationId；正式包 `com.godmiracle.coolapk` 可与其并存。消息/登录/发布/回复编辑不再作为真机业务流验证对象。

当前仍未完成 API 31/32、三键导航、放大字体、TalkBack、全页面动作矩阵、同设备 before/after 三轮长滚动帧统计和完整 Feed instrumentation。一次较长的首页滑动曾观察到返回 Launcher，随后短距离滑动复测未重现且当前前台保持 MainActivity；该现象保留为设备验收风险，不宣称已完全排除。

### 自动化续测结果

此前 connectedDebugAndroidTest 的历史记录覆盖 GlassSurface fixture、Room migration、16 KB GIF、WebView 生命周期和凭证迁移；本次匿名范围清理后不把旧凭证/登录相关用例计入当前通过结论。Search 的基线与当前 Glass 版本各进行了三次独立长滑，jank 加权合计为 23.21% 与 23.00%，没有形成一致性回归；这仍不能替代所有页面的性能前后对比。

## 2026-08-26 匿名只读范围与 Feed 回复可读性修正

登录已从产品范围移除，连带删除登录、消息、发帖、回复编辑、远程点赞/关注、举报和删除等仅依赖账号的入口与网络写入路径。Feed 详情保留服务端返回的评论/楼中楼只读展示、排序和本地操作；回复行、楼中楼和排序栏不得使用 LiquidGlass backdrop 或 RenderEffect 模糊。详情顶部 Toolbar 仍可使用玻璃层。

## 2026-08-26 GitHub Releases 客户端更新流程

在设置页新增“检查本客户端更新”。流程使用独立的无 Coolapk 凭据 GitHub client，请求固定的 `https://api.github.com/repos/godmiracle/Coolapk/releases/latest`，并固定 HTTP/1.1、关闭重定向；只接受非草稿、非预发布且 semver 合法的 Release。更新页始终提供“浏览器下载 Release（推荐）”，固定指向 `https://github.com/godmiracle/Coolapk/releases/latest`，用于国内网络访问 API 不稳定时交给系统浏览器下载。应用不读取资产下载 URL、不在内部下载 APK、不创建更新缓存、不通过 FileProvider 或 Package Installer 传递更新包。

客户端更新页只在用户点击“检查更新”时请求 latest API；打开页面、页面重建或从浏览器返回不会自动检查。旧的“检查应用更新”批量第三方应用更新链路已移除，但应用详情页的单个 APK 下载仍保留。

Debug 构建使用 `applicationIdSuffix = ".debug"`，包名为 `com.godmiracle.coolapk.debug`，并使用 Debug 签名；正式构建包名为 `com.godmiracle.coolapk`，使用 Release 签名。两者可以并存，Debug 不再尝试覆盖正式包。正式包从 Release 页面升级时必须保持包名和签名一致，且新包 `versionCode` 更高；旧版同包名 Debug 安装若仍存在，需要卸载一次完成迁移。

当前源码、单元测试、AndroidTest APK 和 lint 已通过；2026-08-27 在 OPPO `PGEM10` 上已验证新 Debug APK 安装、旧正式包名并存、latest API 检测、更新页无应用内下载/安装按钮，以及 Release 页面由 Firefox 打开。此前应用内下载的 `c001apk_v1.1.0.463.apk` 试装被系统拒绝，是因为旧 Debug `versionCode=465` 高于 Release `versionCode=463`；该降级保护不应绕过。直接网络探测受到当前环境 GitHub API `403` 限制，不能代替设备验证，但本轮设备请求已成功。
