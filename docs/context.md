# 项目上下文

## 文档基线

- 项目：`c001apk`
- 分析日期：2026-08-11
- 当前分支：`main`
- 当前源码基线：`becc810a12c509b4244b0be47bc9457bea0da088`（短哈希 `becc810`）
- 源码提交时间：2024-05-08 11:55:34 +08:00
- 工作区状态：源码基线未见已跟踪修改；`AGENTS.md`、`docs/`、`rasen/`、`scripts/`、`src/` 和 `README_副本.md` 当前为未跟踪内容，应在提交前由维护者决定是否纳入版本库。

## 项目定位

这是一个个人学习型 Android 客户端，使用包名 `com.godmiracle.coolapk`、应用名 `c001apk`。它复现 Coolapk 客户端的部分内容浏览和社区交互流程，重点用于：

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
- 当前 ViewModel 普遍注入 `logic.repository.NetworkRepo`，网络主链路使用 Hilt 提供的三组 Retrofit 服务。
- 本地数据包括话题/数码关注、浏览历史、动态收藏、首页菜单、搜索历史、最近 @用户、最近表情、用户黑名单和话题黑名单。
- Manifest 包含主入口、Coolapk/自定义 Scheme 深链、WebView 独立进程、FileProvider、网络状态和已安装应用查询权限；未发现应用内安装调用，`REQUEST_INSTALL_PACKAGES` 已移除。
- GitHub Actions 当前只构建 Release 和 Debug APK，并上传 APK/Mapping，没有运行业务测试或 lint。

### 源码覆盖但尚未证明

- Coolapk 线上 API 是否仍接受当前路径、Header、版本号、Token 和分页参数。
- 密码登录、验证码、动态发布、图片 OSS 上传、应用下载和 APK 安装是否能在真实账号/设备上完整走通。
- 所有页面在 Android 7 至当前系统、横屏、深色模式和低网速下的稳定性。
- Release R8/资源压缩后反射、Data Binding、图片库和 WebView 是否仍无问题。

### 已知维护风险

- `logic/network/Network.kt` 与 `logic/network/Repository.kt` 保留了一套非 Hilt 的旧网络封装；当前 ViewModel 调用的是 `logic/repository/NetworkRepo.kt`，两套代码存在重复漂移风险。
- `ApiServiceCreator.kt` 只被旧 `Network.kt` 使用，API URL、OkHttp Client 和 Retrofit 配置在两处重复维护。
- API Base URL 已由 `NetworkEndpoints` 集中维护并统一以 `/` 结尾；构建、单元测试、首页和动态详情 Activity 启动验证已完成。
- Debug 网络 BODY 日志默认关闭；显式开启时由 `NetworkLogging` 脱敏，请求本身仍会携带 Cookie/Token/设备参数。
- 登录信息和设备/请求参数由普通 `SharedPreferences` 保存，未见加密存储。
- `usesCleartextTraffic` 已关闭；`QUERY_ALL_PACKAGES` 因应用列表/更新检查仍保留，发布或分发前需要继续评估其隐私披露和渠道要求。
- 自定义 Token 设置项在 `PrefManager`/设置界面中存在；当前仅在开关开启且值非空时覆盖自动生成 Token，真实自定义 Token 联调仍待人工验证。
- 自动化测试仍是模板示例，不能证明业务功能。

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
| 平台 | Android，`minSdk 24`，`target/compileSdk 34` |
| 构建 | Kotlin 1.9.24、Java 17、AGP 8.5.1、Gradle Wrapper 8.14.5 |
| ABI | `arm64-v8a`、`armeabi-v7a`、`x86_64`；未启用 `x86`/`armeabi` |
| UI | 继续沿用 XML + ViewBinding/Data Binding；没有证据表明要迁移 Compose |
| 服务端 | 依赖 `api.coolapk.com`、`api2.coolapk.com`、`account.coolapk.com` 和相关图片/OSS 地址 |
| 账号 | 只允许使用个人账号；Cookie、Token、密码、验证码和上传 STS 凭据不得进入仓库或共享日志 |
| 发布 | Release 密钥从未提交的 `local.properties` 读取；缺省回退 Debug 签名 |
| 网络 | Manifest 明确允许明文流量，具体实际请求仍由各 URL 和 WebView 行为决定 |
| 16 KB 页面大小 | Debug APK 的 native 库已通过 16 KB 对齐校验；Debug AAB 已声明 `PAGE_ALIGNMENT_16K`，16 KB 模拟器冷启动和 GIF 原生加载回归通过 |
| 法律/许可 | 根目录包含 AGPL-3.0 文本；第三方代码、图片、字体和上游库需要单独核对许可证 |

## 关键文件

| 路径 | 用途 |
|---|---|
| `app/build.gradle.kts` | 应用 SDK、签名、版本、ABI、依赖和构建类型 |
| `app/src/main/AndroidManifest.xml` | 权限、入口 Activity、深链、WebView 进程和 FileProvider |
| `app/src/main/java/com/godmiracle/coolapk/MyApplication.kt` | Hilt 应用入口、主题初始化、Mojito 初始化、未捕获异常处理 |
| `app/src/main/java/com/godmiracle/coolapk/di/NetworkModule.kt` | Hilt Retrofit/OkHttp 服务和请求拦截器 |
| `app/src/main/java/com/godmiracle/coolapk/logic/network/ApiService.kt` | Retrofit 请求定义 |
| `app/src/main/java/com/godmiracle/coolapk/logic/repository/NetworkRepo.kt` | 当前 ViewModel 使用的网络 Repository |
| `app/src/main/java/com/godmiracle/coolapk/logic/dao/`、`logic/database/` | Room DAO、数据库和迁移 |
| `app/src/main/java/com/godmiracle/coolapk/util/PrefManager.kt` | 设置、登录摘要、设备参数和请求版本信息 |
| `app/src/main/java/com/godmiracle/coolapk/util/NetWorkUtil.kt` | 外部链接标准化和内部页面路由 |
| `app/src/main/java/com/godmiracle/coolapk/ui/main/MainActivity.kt` | 首页/关注/我的主导航和搜索动作 |
| `app/src/main/java/com/godmiracle/coolapk/ui/feed/` | 动态详情、评论、投票、回复和图片发布 |
| `.github/workflows/ci.yml` | GitHub Actions 构建与制品上传 |
| `gradle/libs.versions.toml` | 依赖版本目录 |

## 运行环境记录

本次分析环境：macOS、工作目录 `/Users/v/ABP/Coolapk`、日期 2026-08-12。已确认 Android Studio JDK 21.0.10、SDK Platform 36.1 和 Wrapper Gradle 8.14.5。为修复 Android 16 16 KB 模拟器报告的 GIF native RELRO 未对齐，`SketchImageViewLoader` 已使用 `pl.droidsonroids.gif:android-gif-drawable:1.2.32`，AGP 已升至 8.5.1；旧 GIF 依赖提供的 Sketch 私有 bitmap hook 不再使用。Debug APK/AAB 均成功生成，APK `zipalign -P 16` 校验通过，AAB 配置为 `PAGE_ALIGNMENT_16K`；本地 Debug 单元测试 6 个用例和设备 instrumentation tests 2 个用例全部通过。`emulator-5554`（`PAGE_SIZE=16384`）已完成 APK/Bundle 安装、冷启动和 GIF 原生加载验证，未出现 16 KB 兼容性对话框；尚未完成 Release 签名、真实设备和完整业务验收。

后续每次真实验收应记录：Android 版本、设备型号、ABI、网络类型、是否登录、应用构建哈希、服务端响应状态和截图/日志位置。
