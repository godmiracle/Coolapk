# c001apk

`c001apk` 是一个基于 Kotlin/Android 的 Coolapk 学习型第三方客户端。项目通过 Coolapk 的公开页面/API 形态复现首页、动态、评论、用户、应用、搜索、关注和设置等主要使用路径，定位是个人学习、实验和测试，不是 Coolapk 官方客户端，也不提供独立后端。

项目中的 `Token/LoginUtil` 实现参考了 [CoolbbsYou](https://github.com/WaitFme/CoolbbsYou)，相关上游代码和许可证应在分发前单独核对。

当前仓库由 `godmiracle` 维护，源码仓库为 [godmiracle/Coolapk](https://github.com/godmiracle/Coolapk)。应用“关于”页同时保留 [HDYOU/c001apk](https://github.com/HDYOU/c001apk) 的 Fork/参考来源和原项目贡献者信息。

当前 Android `applicationId` 与 `namespace` 均为 `com.godmiracle.coolapk`，应用显示名称仍为 `c001apk`。

> 文档基线：根据当前工作区源码整理于 2026-08-12。源码 `HEAD` 为 `becc810`（2024-05-08），外部接口和服务端行为可能已经变化；2026-08-12 已在 Android 16 模拟器完成 API1 首页、动态详情和评论的最小浏览验证，完整业务验收尚未完成。

## 项目声明

1. 本项目仅用于个人学习、测试和 Android 客户端研究，严禁滥用。
2. 使用本项目造成的账号、数据、网络请求或其他后果由使用者自行承担。
3. 项目会访问 Coolapk 相关服务，并复用登录 Cookie、账号 Token、设备参数和 User-Agent；请只使用自己的账号，调试日志不得外传。
4. 第三方代码和资源应同时遵守其原始许可证。项目根目录的 [`LICENSE.md`](LICENSE.md) 为 AGPL-3.0 文本，具体代码归属以文件内声明和来源说明为准。

## 当前能力

以下内容由当前源码中的 Activity、Fragment、ViewModel、Repository、资源和 Manifest 交叉确认；“源码覆盖”不等于当前服务端仍可用或已通过实机验收。

### 首页与内容浏览

- 主导航包含首页、关注、我的三项页面，并在右侧提供搜索动作；竖屏底栏采用 Miuix 风格实色悬浮浮岛，滚动时隐藏、停止滚动后展示，关注页展示本地保存的话题/数码关注，“我的”提供本地收藏、浏览历史和设置入口，首页支持自定义 Tab。
- 首页 Tab：关注、头条、热榜、话题、数码、酷图；首页不再提供应用列表入口，保持纯内容浏览。
- 支持动态列表、动态详情、评论列表、楼中楼、投票、问答、文章/图文和酷图等内容形态。
- 支持下拉刷新、分页加载、空数据、加载失败和“没有更多”等列表状态。
- 支持 HTML/富文本链接、话题/用户/动态/应用跳转及 Coolapk 表情渲染。

### 社区交互

- 动态和评论：点赞、回复、删除、举报、拉黑、收藏、分享、复制文本/链接。
- 用户、话题、数码和应用：查看详情、关注/取消关注、进入相关内容列表。
- 发布文本动态和回复；回复流程包含表情选择、@用户/@话题、验证码处理和图片上传准备。
- 图片支持九宫格展示、全屏预览、保存、分享和复制链接。

### 搜索、关注和本地能力

- 搜索动态、应用、数码、用户、话题，并支持动态类型、排序和搜索历史。
- 关注页默认为空；在话题或数码详情页点击关注后，以本地记录形式合并展示头像和标题，并支持取消本地关注。
- 本地保存话题/数码关注、浏览历史、动态收藏、首页 Tab、搜索历史、最近 @用户、最近表情和用户/话题黑名单。
- 设置支持主题/深色模式、主题色、字体大小、图片质量、外部浏览器、缓存清理、黑名单、设备参数和应用更新检查。

### 账号与应用

- 支持账号密码登录和图形验证码；短信登录相关 API/菜单入口仍需单独核验，不能按完整功能宣传。
- 登录后同步用户信息和服务端状态；主导航不再显示原消息页角标。
- 支持应用详情、已安装应用识别、应用列表、更新检查、下载链接和外部打开/手动安装流程。
- 支持 `coolmarket://`、Coolapk HTTP/HTTPS 链接和部分网页链接的内部路由；不能识别的链接会提示并复制。

## 技术栈

| 类别 | 当前实现 |
|---|---|
| 语言与构建 | Kotlin 1.9.24、Java 17、Gradle Wrapper 8.14.5、Android Gradle Plugin 8.5.1 |
| Android | `compileSdk/targetSdk 34`，`minSdk 24` |
| UI | XML Layout、ViewBinding、Data Binding、Fragment、ViewPager2、RecyclerView、Material/Rikka Material；当前没有 Compose |
| 状态与注入 | Android ViewModel、LiveData、Flow、协程、Hilt 2.51.1 |
| 网络 | Retrofit 2.11、OkHttp 4.12、Gson；API1（首页/详情）、API2（评论等）和账号服务三组 Retrofit 客户端 |
| 本地数据 | Room 2.6.1、多个独立数据库、`SharedPreferences` |
| 图片 | Glide 4.16、`android-gif-drawable` 1.2.32（经 Sketch 适配层使用）、项目内 `mojito` 图片预览及两个 loader 模块 |
| 其他 | Jsoup、阿里云 OSS Android SDK、App Center Analytics/Crashes、LeakCanary（Debug） |

## 仓库结构

```text
.
├── app/                      # 主 Android 应用
│   └── src/main/java/com/godmiracle/coolapk/
│       ├── ui/               # Activity、Fragment、ViewModel、Adapter
│       ├── logic/            # model、network、repository、dao、database
│       ├── adapter/          # Data Binding Adapter 和列表适配器
│       ├── util/             # 登录、Token、图片、链接、缓存等横切工具
│       └── view/             # 自定义 View、文本/图片/滚动组件
├── mojito/                   # 本地图片全屏预览库
├── GlideImageLoader/         # Mojito 的 Glide/OkHttp loader
├── SketchImageViewLoader/    # Mojito 的 Sketch loader
├── docs/                     # 项目上下文、架构、API、开发和会话文档
├── .github/workflows/ci.yml  # GitHub Actions 构建流程
├── gradle/                   # Version Catalog 和 Gradle Wrapper
└── src/、scripts/            # 当前仅保留说明文档，不是主源码目录
```

当前主应用约有 255 个 Kotlin/Java 源文件、418 个 Android 资源文件；本地单元测试当前执行 14 个用例（含 Room/网络凭证边界、网络取消与 HTTP 错误、Token 设备参数和日志脱敏测试），instrumentation 已包含 10 个迁移、备份、WebView 和原生兼容性回归，但尚未形成完整业务回归套件。

## 构建与运行

### 环境要求

- Android Studio 或等效 Android SDK 环境。
- Gradle 通过仓库内 Wrapper 运行；项目 Java/Kotlin 编译目标为 17，CI 使用 JDK 17。本轮本地命令行验证按用户要求使用 Android Studio 自带 JDK 21。
- 项目声明 `compileSdk/targetSdk 34`；本机当前检测到 Android SDK Platform 36.1，Debug 构建已在该 SDK 上验证通过，Release 和真实设备兼容性仍需单独验收。
- 可用的 Android 设备/模拟器。
- 首次构建需要访问 Google Maven、Maven Central、JitPack 和 Sonatype 公共仓库。

### Debug 构建

当前仓库中的 `gradlew` 没有 executable bit，首次使用可以执行：

```bash
chmod +x gradlew
./gradlew :app:assembleDebug
```

也可以在文件权限暂时不调整时使用：

```bash
bash gradlew :app:assembleDebug
```

安装到已连接设备：

```bash
./gradlew :app:installDebug
```

### Release 构建与签名

`app/build.gradle.kts` 会读取未提交的 `local.properties`：

```properties
KEYSTORE_PATH=/absolute/path/to/release.jks
KEYSTORE_PASSWORD=请填入本地值
KEY_ALIAS=请填入本地值
KEY_PASSWORD=请填入本地值
```

配置完整密钥时 Release 使用该签名；缺少 keystore 或签名字段时，`verifyReleaseSigning` 会阻止 Release 打包。Release 开启了资源压缩和 R8/ProGuard，输出名形如 `c001apk_<git-short-hash>(<commit-count>).apk`。

```bash
./gradlew :app:assembleRelease
```

版本号来源于 Git：`versionCode = git rev-list HEAD --count`，`versionName = git rev-parse --short HEAD`；离线、浅克隆或没有 Git 历史时可能影响构建。

## 外部服务、权限与安全边界

应用会访问：

- `https://api.coolapk.com`：首页 V8、动态详情及主 API。
- `https://api2.coolapk.com`：动态评论、话题布局、用户资料等兼容接口。
- `https://account.coolapk.com`：登录、验证码和账号页面。
- Coolapk 图片、移动网页和 OSS 上传回调相关地址。

Manifest 声明网络、网络状态、Wi-Fi 状态和查询已安装应用权限；未发现应用内安装调用，因此已移除 `REQUEST_INSTALL_PACKAGES`，并关闭 `android:usesCleartextTraffic`。WebView 运行在 `:webview` 独立进程，内部会写入 Coolapk Cookie；API Token、会话身份和设备请求状态保存在独立的 `credentials.xml`，该文件不参与云备份或设备迁移，普通主题/字体等偏好仍保留。API1/API2 客户端只有在三个 Coolapk HTTPS Host 上携带凭证，外部 `@Url` 和跨 Host 重定向会清理敏感 Header。网络 BODY 日志默认关闭，只有 Debug 显式开关开启时才输出，且敏感字段脱敏。以上行为均应视为敏感边界，不能把调试包、日志或本地配置当作安全发布产物。

## 验证状态

当前项目 Wrapper 使用维护者主动设置的 Gradle 8.14.5；在 Android Studio JDK 21.0.10、SDK Platform 36.1 下，`:app:testDebugUnitTest`（14 tests）、`:app:lintDebug`、`:app:compileDebugAndroidTestKotlin` 和 `:app:connectedDebugAndroidTest`（Pixel_10，10 tests）均成功。Room 历史迁移、凭证迁移/备份规则、WebView 跨进程重开、网络取消/HTTP 错误和原有 GIF native 回归均有证据。APK 通过 `zipalign -P 16` 校验，AAB 配置为 `PAGE_ALIGNMENT_16K`；Release 签名、真实设备、账号和完整人工业务路径仍未验收。完整验收步骤见 [`docs/development.md`](docs/development.md)，待确认项见 [`docs/todo.md`](docs/todo.md)。

## 文档索引

- [`docs/context.md`](docs/context.md)：项目背景、范围、状态、约束和环境记录。
- [`docs/architecture.md`](docs/architecture.md)：模块边界、调用链、数据流、存储和风险。
- [`docs/api.md`](docs/api.md)：源码中的外部服务、请求分组和登录/发布流程。
- [`docs/development.md`](docs/development.md)：开发、构建、签名、测试和人工验收。
- [`docs/decisions.md`](docs/decisions.md)：技术决策和已确认的维护约束。
- [`docs/todo.md`](docs/todo.md)：按优先级管理的未完成事项和验收标准。
- [`docs/sessions/2026-08-11.md`](docs/sessions/2026-08-11.md)：本次文档整理记录。
- [`docs/sessions/2026-08-12.md`](docs/sessions/2026-08-12.md)：社区 API 对照和模拟器浏览验证记录。
- [`docs/sessions/2026-08-13.md`](docs/sessions/2026-08-13.md)：Code Review 五项优先问题的实施和验证记录。
