# TODO 与验证清单

状态约定：只有代码或文档实际修改、验收标准满足并完成必要验证后，才将 `[ ]` 改为 `[x]`。无法在当前环境验证的事项保持未完成，并标注“等待人工验证”。

## 2026-08-13 本次 Code Review 优先问题

- [x] CR-001 修复 Room 迁移丢数据风险
  - `FeedFavoriteDatabase` 升至 v3，覆盖历史 v1/v2 到当前 `FeedEntity` 的数据迁移；`HomeMenu`、`RecentAtUser` 迁移改为复制数据后换表。
  - 已增加历史 schema fixture 和 5 个 Android migration tests；Pixel_10（Android 17）通过。
- [x] CR-002 收紧带凭证请求的 Host 边界
  - 仅向 `api.coolapk.com`、`api2.coolapk.com`、`account.coolapk.com` 的 HTTPS 请求添加/保留敏感 Header；外部 `@Url` 和跨 Host 重定向会清理凭证。
  - `NetworkEndpointsTest` 覆盖全部策略 Header、外部 Host 和非 HTTPS 请求。
- [x] CR-003 隔离接口/设备凭证并阻止备份外泄
  - `xAppToken`、`xAppDevice`、会话身份和设备请求状态迁移到 `credentials.xml`；旧 `settings` 键幂等迁移并清理，普通 UI 偏好保留。
  - `backup_rules.xml` 与 `data_extraction_rules.xml` 均排除 `credentials.xml`；迁移重复执行和规则覆盖测试通过。
- [x] CR-004 移除 WebView 销毁时的进程退出
  - `WebViewActivity.onDestroy()` 只释放 WebView 资源；真实跨 `:webview` 进程启动、返回销毁、再次启动测试通过。
- [x] CR-005 修复 NetworkRepo 的取消与 HTTP 错误语义
  - Retrofit `Call` 使用可取消挂起适配器；非 2xx、必需空 body、传输失败分别返回明确失败，`CancellationException` 继续传播；下载 302 仅作为显式 `Location` 场景保留。
  - 6 个 fake-Call 单元测试覆盖成功、空 body、401/403/500、传输失败、取消和重定向。

本轮未处理手工分页/Paging 3 重构、旧网络封装删除、Release 签名、真实账号和完整业务流程；这些仍按下方待办管理。

## 本次文档整理

- [x] D-001 补全项目上下文、架构、API 和开发文档
  - 优先级：高
  - 涉及文件：`README.md`、`docs/context.md`、`docs/architecture.md`、`docs/api.md`、`docs/development.md`
  - 状态：已完成，已通过文档一致性检查；代码变更后的构建证据已同步
  - 验收标准：文档不再保留项目事实相关的“待填写”；模块、主网络链路、存储、权限、构建和验证边界与源码一致。

- [x] D-002 建立文档化的会话和技术决策记录
  - 优先级：中
  - 涉及文件：`docs/decisions.md`、`docs/changelog.md`、`docs/sessions/2026-08-11.md`
  - 状态：已完成，会话、变更和技术决策已同步记录
  - 验收标准：记录本次用户目标、静态分析范围、未执行的验证和后续行动；不把推测写成已验证事实。

- [x] D-005 更新应用显示名和 README 公开范围
  - 优先级：低
  - 涉及文件：`app/src/main/res/values/strings.xml`、`README.md`、`docs/context.md`、`docs/architecture.md`、`docs/decisions.md`
  - 状态：已完成；应用显示名改为“绿友”，README 已移除内部接口、凭证实现、详细审计证据和历史会话索引
  - 验收标准：用户可见名称统一为“绿友”；包名、工程内部标识和历史兼容边界保持不变；README 保留项目定位、构建和必要许可证/安全声明。

- [x] UI-001 将竖屏主导航改为 Apple 风格悬浮底栏
  - 优先级：中
  - 涉及文件：`app/src/main/res/layout/activity_main.xml`、`app/src/main/res/layout/fragment_home.xml`、`app/src/main/java/com/godmiracle/coolapk/ui/main/MainActivity.kt`、`app/src/main/java/com/godmiracle/coolapk/ui/home/HomeFragment.kt`、`app/src/main/java/com/godmiracle/coolapk/view/LiquidGlassFrameLayout.kt`
  - 状态：已完成液态玻璃代码落地、Android 17 模拟器验收和 Android 16 真机验收；竖屏为三项玻璃底栏 + 右下角独立搜索圆钮，首页顶部无搜索、仅保留 Tab/菜单玻璃区；滚动中隐藏、停止后展示、深色模式和搜索点击均已验证。应用最低基线已提升到 Android 12 / API 31，不再维护 Android 7–11。
  - 证据：`LiquidGlassFrameLayout` 直接使用 `RenderEffect` 对指定背景 View 做模糊并保留前景控件清晰，不再包含旧系统半透明回退分支；应用层同步移除了 API 24–30 的状态栏、下载、版本号、Tooltip、图库保存和旧文件复制分支；竖屏通过 `nav_menu_portrait.xml` 移除底栏搜索菜单项，顶部布局不包含搜索控件，搜索由独立 `SearchActivity` 点击入口承载；横屏继续使用原 `NavigationRailView` 和四项菜单。Debug APK 构建成功并安装到 OPPO `PGEM10` 真机（Android 16 / API 36、arm64-v8a），`MainActivity` 冷启动状态为 `COLD/ok`；真机浅色/深色截图通过；首页真实动态长滑时底栏和搜索按钮移出屏幕，停止后恢复到 `[84,2112][1032,2304]`；点击搜索后当前 Activity 为 `SearchActivity`；最近 300 行真机日志未发现 `FATAL EXCEPTION` 或 `AndroidRuntime`。`:app:testDebugUnitTest` 共 22 个用例全部通过，`:app:lintDebug` 成功且未发现 `NewApi`/`UnsupportedApiCall`；本次清理后 APK 的 `minSdkVersion=31`，液态玻璃调用无需旧系统 API 守卫。
  - 验收标准：竖屏真机/模拟器确认浮岛不覆盖系统手势区，底栏隐藏后内容不被永久截短且末项仍可安全滚动到位，点击和入口行为与改动前一致；深色模式和主题色下保持可读性。

- [x] UI-002 实现本地话题/数码关注聚合
  - 优先级：中
  - 涉及文件：`logic/model/LocalFollow.kt`、`logic/dao/LocalFollowDao.kt`、`logic/database/LocalFollowDatabase.kt`、`logic/repository/LocalFollowRepo.kt`、`ui/follow/LocalFollowFragment.kt`、话题/数码详情关注逻辑
  - 状态：已完成 Debug 构建、单元测试和 Android 16 模拟器流程验收
  - 证据：关注页无本地记录时显示空状态；话题 Android 和数码“小米17 Pro Max”详情页点击“关注”后写入 `local_follow.db`，同时保存详情 `logo` 头像；应用重启后关注页同时显示“数码”和“话题”记录；删除按钮可移除记录。
  - 验收标准：关注页不请求原消息聚合内容；话题/数码关注统一进入本地列表；取消关注后从列表消失；本地数据不依赖登录状态。

- [x] UI-003 更新设置关于页维护者与 Fork 信息
  - 优先级：低
  - 涉及文件：`ui/others/AboutActivity.kt`、`ui/settings/SettingsFragment.kt`、`res/values/strings.xml`
  - 状态：已完成 Debug 构建、单元测试和 Android 16 模拟器 UI 验收
  - 证据：完整关于页和工具栏简版弹窗均显示 `godmiracle`、当前仓库、`HDYOU/c001apk` Fork/参考来源及原项目贡献者；反馈入口已切换到当前仓库 Issues。
  - 验收标准：当前维护者信息清晰可见；上游/Fork 归属不丢失；旧项目占位文案和反馈地址不再作为当前仓库身份显示。

- [x] UI-004 首页顶部新增“快讯”Tab
  - 优先级：中
  - 涉及文件：`ui/home/HomeViewModel.kt`、`ui/home/HomeFragment.kt`、`ui/homefeed/HomeFeedFragment.kt`、`ui/homefeed/HomeFeedViewModel.kt`、`logic/repository/HomeMenuRepo.kt`、`ui/others/CopyActivity.kt`
  - 状态：已完成 Debug 构建、JVM 单元测试、Lint 和已连接 Android 设备首页验收
  - 证据：默认顺序为“关注、头条、热榜、快讯、话题、数码”；存量 `home_menu.db` 在“话题”前增量插入“快讯”，保留用户原有顺序和启用状态；快讯复用 `/v6/page/dataList`，路由为 `/page?url=V11_HOME_TAB_NEWS`。`a60fe293` 设备 UI 层级确认顺序和选中态，点击后列表实际显示“荣耀Magic9”“酷安手机资讯”等内容；`:app:testDebugUnitTest` 22 个用例全部通过，`:app:assembleDebug` 和 `:app:lintDebug` 成功。
  - 验收标准：新安装和已有安装均出现快讯；点击后进入数据列表；菜单编辑和原有 Tab 不受影响。

- [x] UI-005 底栏新增“发现”并迁移酷图
  - 优先级：中
  - 涉及文件：`ui/main/MainActivity.kt`、`ui/discover/DiscoverFragment.kt`、`res/layout/fragment_discover.xml`、`res/menu/nav_menu_portrait.xml`、`res/menu/nav_menu.xml`、`ui/home/HomeViewModel.kt`、`ui/home/HomeFragment.kt`、`ui/homefeed/HomeFeedFragment.kt`、`ui/homefeed/HomeFeedViewModel.kt`
  - 状态：已完成 Debug 构建、JVM 单元测试、Lint 和已连接 Android 设备验收；后续顺序调整已同步
  - 证据：底栏为“首页、关注、发现、我的”；发现顶部仅有“生活、酷图”。生活使用 `/page?url=V15_ZHUANTI_SHENGHUO`，酷图使用 `/page?url=V11_FIND_COOLPIC`；首页打开时默认选中“头条”。设备 `a60fe293` 上两个发现页面均加载真实列表，首页顶部不再显示酷图，已有 `home_menu.db` 中的酷图也会在启动时移除；`:app:testDebugUnitTest` 22 个用例通过，`:app:assembleDebug` 和 `:app:lintDebug` 成功。
  - 验收标准：底栏可进入发现；生活和酷图均可切换并加载；首页和首页编辑菜单不再出现酷图；关注、我的和搜索入口保持可用。

- [x] UI-006 按 BiliPai 源码补齐底栏交互动效与内容密度
  - 优先级：中
  - 涉及文件：`app/src/main/java/com/godmiracle/coolapk/view/AnimatedBottomNavigationView.kt`、`app/src/main/java/com/godmiracle/coolapk/ui/main/MainActivity.kt`、`app/src/main/res/layout/activity_main.xml`、`app/src/main/res/values/themes.xml`
  - 状态：已完成源码对照、Debug 构建、JVM 单元测试、Lint 和 Android 设备验收
  - 证据：对照 BiliPai 的 `FloatingBottomBar.kt`、`DampedDragAnimation.kt` 和底栏 motion spec，将独立选中胶囊、连续槽位移动、落位压缩/回弹，以及底栏显隐的透明度/缩放动画移植到现有 XML/ViewBinding 架构；移除 Material 自动注入的矩形背景，并按 BiliPai 的垂直 `Column` 将 24dp 图标、11sp 文字和约 1dp 间距整体居中；保留 Material 菜单点击、选中态和无障碍语义，横屏仍使用 `NavigationRailView`。设备 `a60fe293` 已安装并实际切换首页/关注，UI dump 确认图标与文字仅相隔 3px；最终 `:app:assembleDebug`、`:app:testDebugUnitTest` 和 `:app:lintDebug` 全部成功。
  - 验收标准：点击底栏项时选中胶囊连续移动并带落位回弹；图标与文字紧凑居中且无内部白色矩形；列表滚动中底栏退出，停止滚动后从底部带透明度/缩放恢复；四个入口和独立搜索动作保持可用；不改变横屏导航结构。

## 高优先级：发布和安全边界

- [ ] R-001 建立真实设备和真实接口验收基线
  - 优先级：高
  - 可信度：已确认当前未完成
  - 涉及文件：`docs/development.md`、登录/网络/动态/图片相关模块
  - 状态：等待人工验证
  - 验收标准：在 Android 12+ 真机和当前 Android 设备各完成启动、登录、首页、动态、评论、发布、图片、搜索、消息、深链和 WebView 最小路径，并记录构建 hash、响应状态和截图。

- [ ] R-002 评估 Debug BODY 网络日志和凭据暴露
  - 优先级：高
  - 可信度：已确认
  - 涉及文件：`app/src/main/java/com/godmiracle/coolapk/di/NetworkModule.kt`、`logic/network/ApiServiceCreator.kt`、`util/AddCookiesInterceptor.kt`
  - 状态：已修改；Debug 构建和本地单元测试已通过，Release/设备验收待完成
  - 证据：新增统一 `NetworkLogging`；默认日志级别为 `NONE`，仅 `-PenableHttpBodyLogging=true` 且为 Debug 时启用 BODY；Cookie、Token、密码、验证码和 STS 等值经过 `[REDACTED]` 处理。
  - 验收标准：默认日志不输出 Cookie/Token/密码/验证码/STS 信息；必要调试日志有明确开关和脱敏测试；Release 保持无 BODY 日志。

- [ ] R-003 评估普通 SharedPreferences 保存登录 Token 和设备参数
  - 优先级：高
  - 可信度：已确认
  - 涉及文件：`app/src/main/java/com/godmiracle/coolapk/util/PrefManager.kt`
  - 状态：已完成凭证与普通设置隔离、旧键迁移和备份排除；当前仍使用普通 `SharedPreferences`，加密存储不在本次 change 范围内
  - 证据：`credentials.xml` 保存 API/会话/设备请求状态，首次访问时从旧 `settings` 幂等迁移并清理敏感键；云备份和设备迁移均排除该文件，主题/字体等普通设置仍保留。
  - 验收标准：明确个人学习包与发布包的安全边界；如继续保存，至少完成威胁评估和清除/退出登录验证；如改用加密存储，完成旧值迁移和异常回退测试。

- [ ] R-004 评估 Manifest 广泛权限和明文流量开关
  - 优先级：高
  - 可信度：已确认
  - 涉及文件：`app/src/main/AndroidManifest.xml`
  - 状态：已修改，Debug Manifest 合并已通过；等待设备 WebView/深链和分发审核验证
  - 证据：`QUERY_ALL_PACKAGES` 因应用列表和更新检查仍保留；未发现应用内安装调用，已移除 `REQUEST_INSTALL_PACKAGES`；已关闭 `usesCleartextTraffic`，应用控制的 HTTP 链接在 WebView/外部打开前升级为 HTTPS。
  - 验收标准：逐项确认权限必要性、系统版本行为、分发审核影响和隐私披露；不需要的权限移除；明文流量有明确白名单或关闭策略。

- [ ] R-005 确认 Release 缺少密钥时的签名策略
  - 优先级：高
  - 可信度：已确认
  - 涉及文件：`app/build.gradle.kts`、`.github/workflows/ci.yml`
  - 状态：已定位首次远端失败原因并修复 cache 版本；Tag 版本号已改为写入 APK 的 `versionName`，等待提交后重新运行 `v*` Tag
  - 证据：远端运行 `31777925879` 在 Job 初始化阶段报告 `actions/cache@v4.0.2` 已停用；workflow 已升级到 `actions/cache@v6.1.0`，Tag 构建传入 `-PversionName=$GITHUB_REF_NAME`，仍沿用 `verifyReleaseSigning`、APK 上传和 `SHA256SUMS`。
  - 验收标准：首次 Tag Actions 成功；Release APK 通过 `apksigner verify --print-certs`；`aapt2 dump badging` 的 `versionName` 与 Tag 完全一致；发布页同时包含 APK 和 `SHA256SUMS`。

- [x] R-013 验证 Retrofit Base URL 尾部斜杠
  - 优先级：高
  - 可信度：高概率
  - 涉及文件：`app/src/main/java/com/godmiracle/coolapk/di/NetworkModule.kt`、`logic/network/ApiServiceCreator.kt`
  - 状态：已完成；Base URL 尾部斜杠、网络注入、首页和动态详情运行时链路均已验证
  - 证据：API1、API2、Account 的 Base URL 已集中到 `NetworkEndpoints`，统一以 `/` 结尾；新增 Retrofit Base URL 构造测试，并同步覆盖 Hilt 和旧网络封装两套配置。当前 Android Studio JDK 21/SDK Platform 36.1 下 `:app:testDebugUnitTest` 共 6 个用例通过，`:app:assembleDebug` 通过；`emulator-5554` 上应用详情、首页 V8、动态详情和动态评论均收到 `200 OK`，首页真实动态卡片和 `FeedActivity` 正文均已显示。
  - 验收标准：在可用 JDK 环境执行最小构建并启动依赖网络注入的 Activity；若失败，统一修复两套配置并补测试/构建证据；若未失败，记录 Retrofit 版本行为和原因。

- [x] R-014 修复 Android 16 16 KB 页面大小兼容性
  - 优先级：高
  - 可信度：已确认
  - 涉及文件：`gradle/libs.versions.toml`、`app/build.gradle.kts`、`SketchImageViewLoader/build.gradle.kts`、`SketchImageViewLoader/src/main/java/net/mikaelzero/mojito/view/sketch/core/Sketch.java`、`SketchImageViewLoader/src/main/java/net/mikaelzero/mojito/view/sketch/core/drawable/SketchGifFactory.java`、`SketchImageViewLoader/src/main/java/net/mikaelzero/mojito/view/sketch/core/drawable/SketchGifDrawableImpl.java`、`app/src/androidTest/java/com/example/coolapk/GifNativeCompatibilityTest.kt`
  - 状态：已完成；APK、AAB、16 KB 模拟器冷启动和 GIF native 回归均通过
  - 证据：旧 `libpl_droidsonroids_gif.so` 在 16 KB 模拟器触发 `RELRO segment not aligned`；依赖已切换到 `pl.droidsonroids.gif:android-gif-drawable:1.2.32`，AGP 升至 8.5.1。Debug APK 的 native 库通过 `zipalign -P 16` 和 ELF 对齐检查，AAB `bundletool dump config` 为 `PAGE_ALIGNMENT_16K`；`emulator-5554` 的 `PAGE_SIZE=16384`，APK 与 Bundle 生成的 APK 冷启动均无 `PageSizeMismatchDialog`，包状态为 `pageSizeCompat=0`；2 个 instrumentation tests 全部通过。
  - 验收标准：所有打包 native 库的 LOAD/RELRO 满足 16 KB 对齐；AAB 请求 16 KB 页面大小；16 KB 环境启动不再弹兼容性提示；GIF native drawable 能实际创建并释放。

## 中优先级：架构和测试

- [ ] R-006 合并或移除旧网络封装
  - 优先级：中
  - 可信度：高概率
  - 涉及文件：`logic/network/ApiServiceCreator.kt`、`logic/network/Network.kt`、`logic/network/Repository.kt`、`logic/repository/NetworkRepo.kt`
  - 状态：待确认外部调用者后处理
  - 证据：当前 UI ViewModel 引用 `NetworkRepo`，静态搜索未发现 UI 调用旧 `Repository`。
  - 验收标准：确认无外部模块/脚本依赖；统一 API URL、拦截器和错误处理；删除重复实现后构建、测试和关键接口回归通过。

- [ ] R-007 将业务测试加入 CI
  - 优先级：中
  - 可信度：已确认当前未覆盖
  - 涉及文件：`.github/workflows/ci.yml`、`app/src/test`、`app/src/androidTest`
  - 状态：待处理
  - 验收标准：至少执行 unit test、instrumentation test 和 lint；失败时阻止合并；测试不依赖真实账号或未脱敏网络日志。

- [ ] R-008 补充 URL 路由、Token、Room migration 和 ViewModel 状态测试
  - 优先级：中
  - 可信度：已确认当前只有模板测试
  - 涉及文件：`util/NetWorkUtil.kt`、`util/TokenDeviceUtils.kt`、`logic/database/`、`ui/**ViewModel.kt`
  - 状态：待处理
  - 验收标准：覆盖正常/空值/非法 URL、分页结束、错误响应、重复刷新、数据库升级和设备参数变化。

- [ ] R-009 确认 `customToken`/`xAppToken` 的预期行为
  - 优先级：中
  - 可信度：已确认存在配置与请求链路不一致
  - 涉及文件：`util/PrefManager.kt`、`util/AddCookiesInterceptor.kt`、`ui/settings/SettingsPreferenceFragment.kt`、`ui/settings/params/ParamsPreferenceFragment.kt`
  - 状态：已实现“启用且非空时覆盖自动生成值”的行为，设置页人工验收和真实自定义 Token 联调待完成
  - 证据：`AddCookiesInterceptor` 已接入 `PrefManager.customToken/xAppToken`；默认关闭或空值时仍使用自动生成 Token；`TokenDeviceUtilsTest` 已覆盖覆盖/回退分支。不得在日志、截图或聊天记录中粘贴真实 Token。
  - 验收标准：明确自定义值是否应覆盖自动生成值；实现或删除设置入口；补充请求 Header 的单元/集成测试和迁移说明。

- [ ] R-010 确认短信登录入口状态
  - 优先级：中
  - 可信度：高概率
  - 涉及文件：`ui/login/LoginActivity.kt`、`ui/login/LoginViewModel.kt`、`logic/network/ApiService.kt`
  - 状态：待人工验证
  - 验收标准：真实设备上确认入口是否展示、参数是否完整、验证码发送和登录是否可用；不可用时隐藏或明确标注未完成。

## 低优先级：维护体验

- [ ] R-011 决定是否提交 `gradlew` executable bit
  - 优先级：低
  - 可信度：已确认
  - 涉及文件：`gradlew`
  - 状态：待维护者决定
  - 证据：当前 Git mode 为 `100644`，CI 通过 `chmod +x gradlew` 后执行。
  - 验收标准：本地和 CI 的启动方式一致；若不提交权限变更，README 明确使用 `bash gradlew`。

- [ ] R-012 增加截图、演示和最小故障排查样例
  - 优先级：低
  - 可信度：已确认当前缺失
  - 涉及文件：`README.md`、`docs/development.md`
  - 状态：待处理
  - 验收标准：不含账号或隐私数据，至少提供启动、首页、动态、设置和错误状态的脱敏截图或录屏索引。

## 已完成事项（待本轮验证后勾选）

- [x] D-003 确认实际技术栈、四个 Gradle 模块、Manifest 入口、主要 UI/网络/存储边界
  - 优先级：中
  - 状态：已完成静态分析和 Debug 构建验证

- [x] D-004 统一 Android 包名
  - 优先级：中
  - 涉及文件：`app/build.gradle.kts`、`app/src/main/java/com/godmiracle/coolapk/`、`app/src/test/java/com/godmiracle/coolapk/`、`app/src/androidTest/java/com/godmiracle/coolapk/`、`app/proguard-rules.pro`
  - 状态：已完成清理构建、单元测试、APK 安装和 Android 16 模拟器冷启动验证
  - 证据：新包名为 `com.godmiracle.coolapk`；源码、测试、Manifest 生成组件和 ProGuard 规则无旧包名残留，历史决策和会话文档保留旧包名作为迁移记录。
  - 边界：旧包与新包被 Android 视为两个应用，本地数据不会自动迁移。

## Review Issues 模板

后续代码 Review 按 `docs/review.md` 记录。每个问题必须有唯一编号、优先级、可信度、涉及文件、证据、影响、建议和验收标准；无法验证的项目不得勾选完成。
