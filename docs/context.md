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
2. 学习 Retrofit/OkHttp 请求、登录 Cookie、设备参数和分页接口适配。
3. 学习 Room 本地历史/收藏/设置数据管理。
4. 学习富文本、表情、图片九宫格、全屏预览和 OSS 图片上传。

项目不是 Coolapk 官方应用，没有独立服务端，也没有稳定的后端契约。接口兼容性依赖外部服务，源码中出现的“已实现”只能说明客户端存在相应代码路径。

## 当前目标

当前最重要的目标不是扩展功能，而是先建立可维护、可验证的工程基线：

- 让新贡献者能在与项目兼容的 Android Studio/Gradle 环境中完成构建；CI 基线为 JDK 17，本轮本地按要求使用 Android Studio JDK 21 和现有 SDK。
- 识别当前主链路和历史遗留链路，避免修错代码入口。
- 对登录、Token、WebView、图片上传、应用下载等高风险路径建立人工验收清单。
- 逐步补充业务测试和 CI 验证，降低外部 API 变化带来的回归风险。

## 当前状态

### 已确认

- 主应用模块、三个本地图片相关 library module 和 Gradle 多模块配置存在。
- 主导航页面为首页、关注、我的，右侧搜索项启动独立搜索页；关注页是本地话题/数码关注聚合，“我的”包含本地收藏、浏览历史和设置入口，首页 Tab 可通过 Room 配置。
- 当前 ViewModel 普遍注入 `logic.repository.NetworkRepo`，网络主链路使用 Hilt 提供的 API1、API2 和 Account Retrofit 服务；`NetworkRepo` 通过可取消的 Call adapter 处理响应、错误和协程取消。
- 本地数据包括话题/数码关注、浏览历史、动态收藏、首页菜单、搜索历史、最近 @用户、最近表情、用户黑名单和话题黑名单。
- `DatabaseModule` 对 `FeedFavorite`、`HomeMenu`、`RecentAtUser`、`LocalFollow` 和四类 `StringEntity` 数据库保留兼容数据的前向迁移；`DatabaseMigrationTest` 使用 `app/src/androidTest/assets/room-migration-fixtures/` 中的自包含历史 fixture 验证关键行和 Room identity。
- Manifest 包含主入口、Coolapk/自定义 Scheme 深链、WebView 独立进程、FileProvider、网络状态和已安装应用查询权限；未发现应用内安装调用，`REQUEST_INSTALL_PACKAGES` 已移除。
- API1、API2 和 Account 客户端均在最终网络发送边界检查 HTTPS 与三个受信任 Host；不可信 `@Url`/重定向会清理凭证，Account 的不可信请求不会消费登录步骤 flag。`WebViewActivity` 销毁时只释放资源，不主动结束进程。
- GitHub Actions 在 `main` Push/手动触发时构建 Release/Debug 并上传 Actions 制品；推送 `v*` Tag 时额外创建 GitHub Release，上传签名 APK 与 `SHA256SUMS`。CI 仍没有运行业务测试、lint 或真实设备验收，首次 Tag 发布运行待远端验证。

### 源码覆盖但尚未证明

- Coolapk 线上 API 是否仍接受当前路径、Header、版本号、Token 和分页参数。
- 密码登录、验证码、动态发布、图片 OSS 上传、应用下载和 APK 安装是否能在真实账号/设备上完整走通。
- 所有页面在 Android 12 至当前系统、横屏、深色模式和低网速下的稳定性。
- Release R8/资源压缩后反射、Data Binding、图片库和 WebView 是否仍无问题。

### 已知维护风险

- `logic/network/Network.kt` 与 `logic/network/Repository.kt` 保留了一套非 Hilt 的旧网络封装；当前 ViewModel 调用的是 `logic/repository/NetworkRepo.kt`，两套代码存在重复漂移风险。
- `ApiServiceCreator.kt` 只被旧 `Network.kt` 使用，API URL、OkHttp Client 和 Retrofit 配置在两处重复维护。
- API Base URL 已由 `NetworkEndpoints` 集中维护并统一以 `/` 结尾；API1、API2 和 Account 的敏感请求由 HTTPS + trusted-host 边界统一保护。历史会话记录包含构建、单元测试和首页/动态详情启动证据，但本次文档同步未重跑这些验证。
- Debug 网络 BODY 日志默认关闭；显式开启时由 `NetworkLogging` 脱敏，请求本身仍会携带 Cookie/Token/设备参数。
- 登录信息、接口 Token、设备身份和请求参数已从普通 UI 设置隔离到 `credentials.xml`；`backup_rules.xml` 与 `data_extraction_rules.xml` 排除该文件。当前仍是普通（未加密）SharedPreferences，隔离和备份排除不等于加密存储。
- `usesCleartextTraffic` 已关闭；`QUERY_ALL_PACKAGES` 因应用列表/更新检查仍保留，发布或分发前需要继续评估其隐私披露和渠道要求。
- 自定义 Token 设置项在 `PrefManager`/设置界面中存在；当前仅在开关开启且值非空时覆盖自动生成 Token，真实自定义 Token 联调仍待人工验证。
- 自动化测试已包含 Room 迁移、请求 Host 边界、凭证迁移/备份、网络取消/HTTP 错误和 WebView 生命周期回归；它们仍不能替代真实账号、真实设备、实时 API 或完整业务验收。

## 范围与非目标

### 范围

- Android 手机客户端。
- Coolapk 内容浏览、账号状态同步和社区交互。
- 本地历史、收藏、黑名单、首页布局和设置。
- 图片展示、保存/分享/复制和回复图片上传。

### 非目标

- 不实现 Coolapk 服务端。
- 不承诺对外部 API 的永久兼容。
- 不在没有用户确认和真实验收的情况下宣称“可发布”或“全功能可用”。
- 不将源码中预留但未接通的短信登录或更新检测路径当成完成能力；自定义 Token 仅视为已接通请求 Header 的调试选项，不代表已有可用 Token。

## 重要约束

| 约束 | 现状 |
|---|---|
| 平台 | Android，`minSdk 31`（Android 12+），`target/compileSdk 34` |
| 构建 | Kotlin 1.9.24、Java 17、AGP 8.5.1、Gradle Wrapper 8.14.5 |
| ABI | `arm64-v8a`、`armeabi-v7a`、`x86_64`；未启用 `x86`/`armeabi` |
| UI | 继续沿用 XML + ViewBinding/Data Binding；没有证据表明要迁移 Compose |
| 服务端 | 依赖 `api.coolapk.com`、`api2.coolapk.com`、`account.coolapk.com` 和相关图片/OSS 地址 |
| 账号 | 只允许使用个人账号；Cookie、Token、密码、验证码和上传 STS 凭据不得进入仓库或共享日志 |
| 发布 | 本地从未提交的 `local.properties` 读取 Release 密钥；CI 从 Secrets 写入临时 `local.properties`；缺少完整配置时 `verifyReleaseSigning` 阻止 Release 打包 |
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
| `app/src/main/java/com/godmiracle/coolapk/logic/repository/NetworkCallAdapter.kt` | Retrofit Call 的取消、HTTP、空 body 和异常适配 |
| `app/src/main/java/com/godmiracle/coolapk/logic/dao/`、`logic/database/` | Room DAO、数据库和迁移 |
| `app/src/main/java/com/godmiracle/coolapk/util/PrefManager.kt` | 普通 UI 设置与 `credentials.xml` 中的登录/设备请求状态 |
| `app/src/main/java/com/godmiracle/coolapk/util/CredentialPreferencesMigration.kt` | 旧 `settings.xml` 敏感键到凭证文件的幂等迁移 |
| `app/src/main/java/com/godmiracle/coolapk/util/NetWorkUtil.kt` | 外部链接标准化和内部页面路由 |
| `app/src/main/java/com/godmiracle/coolapk/ui/main/MainActivity.kt` | 首页/关注/我的主导航和搜索动作 |
| `app/src/main/java/com/godmiracle/coolapk/ui/feed/` | 动态详情、评论、投票、回复和图片发布 |
| `.github/workflows/ci.yml` | GitHub Actions 构建与制品上传 |
| `gradle/libs.versions.toml` | 依赖版本目录 |

## 运行环境记录

本次分析环境：macOS、工作目录 `/Users/v/ABP/Coolapk`、日期 2026-08-13。此前已使用 Android Studio JDK 21.0.10、SDK Platform 36.1 和 Gradle Wrapper 8.14.5 构建本地签名 Release，完成 APK 签名/16 KB 对齐检查，并安装到 OPPO `PGEM10`（Android 16 / API 36、arm64-v8a）完成 `MainActivity` 冷启动验证。本次 workflow 修改只做静态检查，尚未推送 `v*` Tag 或运行远端 Actions；真实账号、实时 API 长期稳定性和完整业务验收仍未完成。

后续每次真实验收应记录：Android 版本、设备型号、ABI、网络类型、是否登录、应用构建哈希、服务端响应状态和截图/日志位置。
