# 开发、构建与验收指南

## 1. 开发前检查

1. 阅读 `README.md`、`AGENTS.md`、`docs/context.md`、`docs/architecture.md`、`docs/decisions.md` 和 `docs/todo.md`。
2. 确认工作区是否有用户未提交修改；不要用 `git reset --hard` 或大范围格式化清理。
3. 确认 Android Studio/Gradle 工具链和设备状态；CI 基线使用 JDK 17，本地也可以按当前约定使用 Android Studio 自带 JDK 21，但必须记录实际版本。
4. 不要把 `local.properties`、签名文件、Cookie、Token、密码、验证码或 OSS STS 信息加入 Git。

## 2. 环境要求

| 项目 | 要求 |
|---|---|
| JDK | 项目 Java/Kotlin 编译目标为 17；CI 使用 JDK 17，本轮本地验证使用 Android Studio JDK 21 |
| Android SDK | 项目声明 compile/target 34；本机当前 SDK Platform 为 36.1，需以实际构建结果确认兼容性；运行设备最低 Android 12 / API 31 |
| Gradle | 使用仓库 Wrapper 8.14.5；本机另有已校验的 Gradle 8.7 用户缓存 |
| 网络 | 首次构建需要 Google Maven、Maven Central、JitPack、Sonatype |
| ABI | `arm64-v8a`、`armeabi-v7a`、`x86_64` |
| 16 KB | `android-gif-drawable` 使用 1.2.32；AAB 构建应保持 `PAGE_ALIGNMENT_16K`，并在 16 KB 模拟器上做启动回归 |
| 设备 | 建议至少准备一台 API 31+ 真机和一个当前 Android 模拟器 |

当前 `gradlew` 文件为非 executable 权限。可在本地执行一次 `chmod +x gradlew`，或者使用 `bash gradlew`；权限是否提交回仓库需由维护者另行决定。

按本轮本机约定使用 Android Studio 自带 JDK/SDK 时，可先设置：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_SDK_ROOT="/Users/v/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"
```

## 3. 常用命令

```bash
# 查看 Gradle 环境
bash gradlew --version

# 构建 Debug APK
bash gradlew :app:assembleDebug

# 安装 Debug APK
bash gradlew :app:installDebug

# 构建 Release APK
bash gradlew :app:assembleRelease

# 本地 JVM 单元测试
bash gradlew :app:testDebugUnitTest

# 已连接设备/模拟器的 instrumentation 测试
bash gradlew :app:connectedDebugAndroidTest

# 可用时执行静态检查
bash gradlew :app:lintDebug
```

如果 `./gradlew` 没有执行权限，可把命令开头替换为 `bash gradlew`。如果 Wrapper 尚未下载，先确认网络和 Gradle 缓存；不要把下载的 Gradle 分发包提交进仓库。

手动获取 Gradle 分发包时，先校验 SHA-256，再放入 Gradle Wrapper 用户缓存；不要修改 Wrapper 版本，也不要把 ZIP 复制到仓库。当前 8.7 分发包的校验值为：

```text
544c35d6bd849ae8a5ed0bcea39ba677dc40f49df7d1835561582da2009b961d
```

本轮已验证 `/Users/v/Downloads/gradle-8.7-bin.zip` 与该值一致并放入用户缓存；当前项目的 `gradle-wrapper.properties` 由维护者设置为 Gradle 8.14.5，Wrapper 已实际识别该版本。Android Studio 项目应保持使用 `gradle-wrapper.properties`，不需要把 ZIP 解压到 Android Studio 安装目录。

## 4. Release 签名和版本

`app/build.gradle.kts` 读取未跟踪的 `local.properties`：

```properties
KEYSTORE_PATH=/absolute/path/to/release.jks
KEYSTORE_PASSWORD=本地密钥库密码
KEY_ALIAS=本地别名
KEY_PASSWORD=本地别名密码
```

- 四个值都存在时创建 `release` signing config。
- Debug 始终使用 Debug 签名；Release 缺少 keystore 文件或任一签名字段时，`verifyReleaseSigning` 会阻止 `assembleRelease`、`bundleRelease` 和 Release 打包任务。
- Release 开启 R8/ProGuard 和资源压缩。
- `versionCode` 来自 Git 提交总数，`versionName` 来自当前短 hash。
- Release APK 改名为 `c001apk_<short-hash>(<commit-count>).apk`。

发布前必须确认：签名不是 Debug、mapping 已归档、R8 后页面可启动、真实 API 请求正常、隐私/许可证/权限说明已更新。

## 5. 配置和运行

项目没有 `.env` 或服务端配置文件。当前运行参数主要来自：

- `Constants.kt` 的默认应用/接口版本和请求标识。
- `PrefManager` 的 `settings.xml`（普通 UI/API 版本设置）和 `credentials.xml`（登录、Token、设备身份及请求状态）。
- 设置页面的主题、字体、URL 打开方式，以及受控的设备/请求参数；首次访问时旧 `settings.xml` 敏感键会幂等迁移到 `credentials.xml`。
- `local.properties` 的 Release 签名配置。

第一次运行时，主界面会请求应用信息并尝试检查登录状态。外部 API 不可达时，页面可能出现加载失败、空列表或登录失效；这不应直接判断为编译问题。

## 6. 网络日志和明文流量边界

- `NetworkLogging` 默认将 OkHttp 日志级别设为 `NONE`，Release 永远不会启用 BODY。
- 个人 Debug 调试可使用 `-PenableHttpBodyLogging=true` 显式开启 BODY；Cookie、Authorization、设备/应用 Token、密码、验证码和 STS 等字段仍会输出为 `[REDACTED]`。
- Manifest 已关闭 `usesCleartextTraffic`；应用控制的 HTTP 初始链接、跳转、下载和外部打开路径会先升级为 HTTPS。
- API1、API2、Account 以及 API1 no-redirect 客户端均在最终网络发送处执行 HTTPS + trusted-host 检查；只有 `api.coolapk.com`、`api2.coolapk.com`、`account.coolapk.com` 保留 Token、Cookie、设备和应用身份 Header。外部 `@Url`、非 HTTPS 请求和跨 Host 重定向会清理凭证。
- Account 的 `LoginCookiesInterceptor` 在不可信 URL 上先返回清理后的请求，不消费 `CookieUtil` 登录步骤 flag；登录状态机仍要求串行使用。
- `credentials.xml` 同时被 `backup_rules.xml`、`data_extraction_rules.xml` 排除；这只是隔离和备份策略，当前文件仍未加密。
- `QUERY_ALL_PACKAGES` 仅因应用列表和更新检查实际需要而保留；`REQUEST_INSTALL_PACKAGES` 已移除。

## 7. 业务人工验收矩阵

以下是提交代码或更新 API 适配后建议的最小验收集。每项需要记录构建 hash、设备、账号状态和结果。

| 场景 | 验收内容 | 证据 |
|---|---|---|
| 启动 | 冷启动、热启动、异常页、主题初始化 | Logcat + 截图 |
| 主导航 | 首页/关注/我的切换、底栏搜索动作、返回键 | 操作录屏或截图 |
| 首页 | Tab 初始化、Tab 自定义、关注/头条/热榜/话题/数码/酷图分页 | 页面状态 + 请求结果 |
| 动态 | 详情、图片、富文本、评论分页、楼中楼、投票/问答 | 真实内容截图 |
| 社交操作 | 点赞、关注、拉黑、删除、收藏、举报入口、分享/复制 | 服务端返回 + UI 状态 |
| 发布 | 文本动态、回复、@用户/@话题、表情、验证码 | 发布后的真实内容 |
| 图片 | 九宫格、全屏缩放、保存、分享、复制、OSS 上传 | 文件/分享结果 |
| 搜索 | 动态/应用/数码/用户/话题、排序、历史 | 结果和边界页 |
| 用户/应用 | 用户主页、应用详情、下载链接、安装/更新入口 | 页面 + 下载行为 |
| 本地关注 | 空状态、话题/数码详情关注、合并列表、取消关注和重启后保留 | 截图 + 本地数据库/UI 状态 |
| 我的/设置 | 本地收藏、浏览历史、设置二级入口及设置项持久化 | 页面截图 + 设置结果 |
| 深链 | `coolmarket://`、HTTP/HTTPS `/feed`、`/apk`、`/u`、`/t` 等 | `adb` 启动结果 |
| WebView | 登录态 Cookie、返回、intent Scheme、下载、外部浏览器 | 页面/下载文件 |
| 异常 | 断网、401/403、空响应体、过期 Cookie、重复点击、旋转 | 错误提示和无崩溃证据 |

## 8. 自动化测试现状

当前源码中的测试清单（按 `@Test` 方法静态计数，不代表本轮已经执行）包括：

- JVM 共 22 个：`ExampleUnitTest.kt`（1）、`NetworkEndpointsTest.kt`（3）、`NetworkLoggingTest.kt`（2）、`NetworkRequestBoundaryTest.kt`（4）、`NetworkCallAdapterTest.kt`（10）、`TokenDeviceUtilsTest.kt`（2）。其中网络边界测试覆盖受信任/外部/非 HTTPS/跨 Host 重定向，Call adapter 测试覆盖非 2xx、空 body、transport failure、取消和 `NetworkRepo` Flow 取消语义。
- instrumentation 共 21 个：`ExampleInstrumentedTest.kt`（1）、`GifNativeCompatibilityTest.kt`（1）、`DatabaseMigrationTest.kt`（11）、`CredentialPreferencesMigrationTest.kt`（6）、`WebViewActivityLifecycleTest.kt`（2）。其中 Room 测试使用 `app/src/androidTest/assets/room-migration-fixtures/` 自包含 fixture，凭证测试覆盖完整键集/重试/两类备份规则，WebView 测试覆盖返回销毁和配置重建。

本次 docs fixer 未运行 `:app:testDebugUnitTest`、`lintDebug`、`compileDebugAndroidTestKotlin` 或 `:app:connectedDebugAndroidTest`，也未进行设备/实时 API 验收，因此不能写当前工作树的“通过”结论。`docs/sessions/2026-08-13.md` 中的 14 个 JVM、10 个 instrumentation 用例是更早的执行记录，不能替代当前源码 22/21 的验证结果。无论自动化测试是否通过，它们仍不能证明真实 API、完整登录、分页、深链或完整图片业务功能。新增业务逻辑时，优先为以下边界增加测试：

1. `NetWorkUtil.openLink` 的 URL 到 Activity/参数映射。
2. `TokenDeviceUtils` 的输入输出稳定性和设备参数变化。
3. Room migration 和历史/收藏/黑名单 CRUD。
4. ViewModel 对空响应、业务错误、分页结束和重复刷新的处理。
5. 富文本 URL、表情和图片列表解析。

## 9. CI 现状

`.github/workflows/ci.yml`：

1. 只在 `main` 分支相关 Push 或手动触发时运行。
2. 使用 JDK 17、Gradle Wrapper validation 和 Gradle 缓存。
3. 如果主分支配置了 `SIGN_KEYSTORE_BASE64` 等 Secrets，会写入临时 `local.properties`。
4. 构建并上传 Release APK、Release mapping、Debug APK。
5. 没有运行 unit test、instrumentation test、lint 或真实设备验收。
6. 普通 Markdown/文本变更会被 `paths-ignore` 忽略，但 workflow 文件本身保留触发影响。

## 10. 常见问题

### Gradle 报 Java Runtime 不存在

确认实际使用的 JDK 可用：本轮本地命令应指向 Android Studio JDK 21，CI 仍使用 JDK 17；`java -version` 和 `javac -version` 都必须成功。只找到 `/usr/bin/java` 不代表当前系统已安装可用 JDK。

### Release 可以构建但不能发布

检查 `local.properties` 是否提供完整的 Release 密钥配置；缺少配置时 `verifyReleaseSigning` 应使 Release 任务失败。成功生成 APK 后仍需用 `apksigner verify --print-certs` 检查签名。

### 登录失败或接口返回空

先确认服务端是否仍接受当前 API 路径、版本 Header、设备码和 Cookie，再检查 `api.coolapk.com`、`api2.coolapk.com` 与 `account.coolapk.com` 是否选对，以及最终请求是否仍为 HTTPS trusted host。`NetworkRepo` 会把非 2xx 和必需空 body 作为失败，取消会继续传播；不要先修改 UI 绕过服务端错误。

### 应用启动时 Retrofit 初始化失败

优先检查 `NetworkEndpoints.kt` 中的 API1、API2、Account Base URL 是否以 `/` 结尾，再执行 `:app:testDebugUnitTest` 和依赖网络注入的 Activity 启动验证。

### Android 16 提示应用不兼容 16 KB

先读取对话框列出的具体 `.so` 路径；不要仅通过关闭提示或添加兼容性开关掩盖问题。检查 APK 中 native 库的 ELF LOAD/RELRO 对齐和 `zipalign -c -P 16 -v 4`，再检查 AAB 的 `bundletool dump config` 是否为 `PAGE_ALIGNMENT_16K`。当前 GIF native 依赖固定为 `pl.droidsonroids.gif:android-gif-drawable:1.2.32`，不要恢复旧的 `io.github.panpf.sketch:sketch-gif:2.7.1`。

### 首次构建下载依赖过慢或 JAR 不完整

Gradle 分发包已安装并不代表 Android Gradle Plugin、Kotlin 插件和业务依赖已缓存。若详细日志显示 Maven 下载长时间无进度，或临时 JAR 只有部分大小，先停止构建并检查网络/代理，再重新执行 `bash gradlew :app:assembleDebug --info --stacktrace --console=plain`。`--offline` 只能使用完整缓存；缺少完整插件依赖时应报告构建阻塞，不应把截断文件当作可用缓存。

本轮曾遇到 `room-compiler-2.6.1.jar` 和 `sqlite-jdbc-3.41.2.2.jar` 下载被远端提前断开；在网络恢复后补齐完整缓存并使用 `--max-workers=1` 重试，最终 Debug 构建通过。GIF 依赖应使用 `pl.droidsonroids.gif:android-gif-drawable:1.2.32`，不要恢复旧的 `io.github.panpf.sketch:sketch-gif:2.7.1` 或更早的 `me.panpf:sketch-gif` 坐标。

### 下载链接为空

`getAppDownloadLink` 依赖不跟随重定向的响应 `Location` Header；请记录状态码和 Header（脱敏），不要只看响应体。

### Room 升级崩溃

同步检查数据库版本、Entity、Migration 和 `app/src/androidTest/assets/room-migration-fixtures/` 中的历史 fixture。当前迁移测试使用 `MigrationTestHelper.runMigrationsAndValidate`，覆盖 FeedFavorite、HomeMenu、RecentAtUser、LocalFollow 和 StringEntity 数据保留；不要通过清空数据库掩盖迁移问题，除非这是明确的用户数据迁移策略。

## 11. 提交前检查

- [ ] 只修改了任务范围内的文件。
- [ ] `git diff --check` 通过。
- [ ] 相关单元测试/静态检查已运行并记录结果。
- [ ] 需要设备的功能已完成真实设备验收，或明确标记“等待人工验证”。
- [ ] 没有敏感信息、调试 Token、签名配置或大范围日志进入 diff。
- [ ] 影响行为的技术选择已同步到 `docs/decisions.md`。
- [ ] `docs/todo.md` 只在验收标准确实满足时勾选。
