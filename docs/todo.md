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
  - 状态：原 API 31 实现已完成 Android 16 真机验收；2026-11-09 后续方案改为四项 KernelSU 玻璃底栏与独立搜索，滚动时常驻。当前实现和后续验收以 UI-GLASS-002 为准；横屏仍使用 NavigationRailView。
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

- [x] UI-007 按推荐结构重做话题和数码详情页
  - 优先级：中
  - 涉及文件：`ui/topic/TopicFragment.kt`、`ui/topic/TopicViewModel.kt`、`ui/topic/TopicContentViewModel.kt`、`ui/topic/TopicContentFragment.kt`、`res/layout/base_tablayout_viewpager.xml`、主题头部/排序布局和 URL 单元测试
  - 状态：已完成；按真实接口保留“最近回复/最新发布/热门动态”排序 Tab，兼容的“讨论”Tab 仍可使用内嵌排序栏
  - 证据：主题头部已展示真实 logo、标题、简介和可用统计；关注按钮在设备 `a60fe293` 上点击后即时变为“已关注”；真实“数码日常/充电头兴趣小组”详情的“最近回复 → 最新发布”切换成功刷新列表；`:app:testDebugUnitTest`、`:app:assembleDebug`、`:app:lintDebug` 均成功。用户确认当前接口排序结构保持不变。
  - 验收标准：头部关注无需打开菜单；讨论排序不使用产品专用硬编码路由；切换排序清空旧列表并重置分页；现有 Tab、动态、黑名单、发布入口和其他详情页菜单保持可用；真实 API 不支持的排序参数不得伪造为已验证。

- [x] UI-008 修复数码讨论“最新/热度”排序参数失效
  - 优先级：高
  - 涉及文件：`ui/topic/TopicContentViewModel.kt`、`ui/topic/TopicSortUrlTest.kt`、`docs/decisions.md`、`docs/sessions/2026-08-24.md`
  - 状态：已完成；兼容服务端返回的 percent-encoded 嵌套 `url`，排序参数现在写入内层 URL
  - 证据：修复前回归用例证明 `hot` 会把参数追加到外层；修复后最新/热度 URL 单测、`:app:testDebugUnitTest`、`:app:assembleDebug`、`:app:lintDebug` 均通过。`Pixel_10` 模拟器上同一商品详情的默认、最新、热度请求均返回 `200 OK`，内层 `listType` 分别为 `lastupdate_desc`、`dateline_desc`、`rank_score`。
  - 验收标准：默认排序保持原始内层参数；最新和热度替换内层 `listType`，不再生成外层重复参数；原始未编码 URL 的兼容逻辑和分页刷新行为保持不变。

## 高优先级：发布和安全边界

- [ ] R-001 建立真实设备和真实接口只读验收基线
  - 优先级：高
  - 可信度：已确认当前未完成
  - 涉及文件：`docs/development.md`、登录/网络/动态/图片相关模块
  - 状态：等待人工验证
  - 验收标准：在 Android 13+ 真机和 Android 16 设备各完成启动、首页、动态、评论只读展示、搜索、深链和 WebView 最小路径，并记录构建 hash、响应状态和截图；不包含已移除的登录、发布、回复编辑和消息路径。

- [ ] R-002 评估 Debug BODY 网络日志和凭据暴露
  - 优先级：高
  - 可信度：已确认
  - 涉及文件：`app/src/main/java/com/godmiracle/coolapk/di/NetworkModule.kt`、`logic/network/ApiServiceCreator.kt`、`util/AddCookiesInterceptor.kt`
  - 状态：已修改；Debug 构建和本地单元测试已通过，Release/设备验收待完成
  - 证据：新增统一 `NetworkLogging`；默认日志级别为 `NONE`，仅 `-PenableHttpBodyLogging=true` 且为 Debug 时启用 BODY；Cookie、Token、密码、验证码和 STS 等值经过 `[REDACTED]` 处理。
  - 验收标准：默认日志不输出 Cookie/Token/密码/验证码/STS 信息；必要调试日志有明确开关和脱敏测试；Release 保持无 BODY 日志。

- [ ] R-003 评估普通 SharedPreferences 保存设备/API 身份参数
  - 优先级：高
  - 可信度：已确认
  - 涉及文件：`app/src/main/java/com/godmiracle/coolapk/util/PrefManager.kt`
  - 状态：已完成凭证与普通设置隔离、旧键迁移和备份排除；当前仍使用普通 `SharedPreferences`，加密存储不在本次 change 范围内
  - 证据：`credentials.xml` 保存 API/会话/设备请求状态，首次访问时从旧 `settings` 幂等迁移并清理敏感键；云备份和设备迁移均排除该文件，主题/字体等普通设置仍保留。
  - 验收标准：明确个人学习包与发布包的安全边界；如继续保存设备/API 身份，至少完成威胁评估和清除应用数据验证；如改用加密存储，完成旧值迁移和异常回退测试。

- [ ] R-004 评估 Manifest 广泛权限和明文流量开关
  - 优先级：高
  - 可信度：已确认
  - 涉及文件：`app/src/main/AndroidManifest.xml`
  - 状态：已修改，Debug Manifest 合并已通过；客户端更新不声明 `REQUEST_INSTALL_PACKAGES`，旧的第三方应用批量更新链路移除后当前也不再声明 `QUERY_ALL_PACKAGES`，仍需发布渠道复核权限披露
  - 证据：应用详情单个 APK 下载继续使用 Coolapk 下载链接；客户端更新只通过固定 Release 网页交给系统浏览器；已关闭 `usesCleartextTraffic`，应用控制的 HTTP 链接在 WebView/外部打开前升级为 HTTPS。
  - 验收标准：逐项确认权限必要性、系统版本行为、分发审核影响和隐私披露；不需要的权限移除；明文流量有明确白名单或关闭策略。

- [x] R-005 确认 Release 缺少密钥时的签名策略
  - 优先级：高
  - 可信度：已确认
  - 涉及文件：`app/build.gradle.kts`、`.github/workflows/ci.yml`
  - 状态：已完成；Tag Action、Release APK、版本号、签名证书和 `SHA256SUMS` 均已核验
  - 证据：远端运行 `31777925879` 在 Job 初始化阶段报告 `actions/cache@v4.0.2` 已停用；workflow 已升级到 `actions/cache@v6.1.0`，Tag 构建传入 `-PversionName=$GITHUB_REF_NAME`；`v1.1.0` Action `32952077269` 的 Release 构建、APK 上传和 `Create GitHub Release` 均成功，资产为 `c001apk_v1.1.0.463.apk` 与 `SHA256SUMS`，下载后校验通过。
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

- [x] R-015 增加 GitHub Releases 客户端更新检测与 Release 浏览器入口
  - 优先级：高
  - 可信度：已确认；源码、离线门禁、OPPO `PGEM10` 真机证据、独立 review、Tag Action 和正式 Release 均已核验
  - 涉及文件：`logic/network/GitHubReleaseApiService.kt`、`logic/repository/GitHubReleaseRepository.kt`、`logic/update/`、`ui/clientupdate/`、`app/build.gradle.kts`、`AndroidManifest.xml`、`res/xml/file_provider_paths.xml`
  - 状态：已完成源码清理、资源编译、JVM 单测、AndroidTest APK、lint、OPPO `PGEM10` 当前 Debug 包安装、更新页初始手动触发状态核对、latest API 检查、Firefox Release 页面跳转、connected instrumentation、独立 review、commit、main/Tag 推送和正式 Release 发布；客户端更新严格手动触发，旧的第三方应用批量更新链路已移除
  - 证据：`rasen/changes/github-release-self-update/evidence/verification-report.md`、`review-cycle-report.md`、`ship-log.md`、`docs/sessions/2026-08-27.md`；Release `v1.1.1` 的 APK、版本元数据和 `SHA256SUMS` 已核验。旧版设备证据中的 `versionCode=465` 对 `versionCode=463` 降级拒绝只作为 Android 行为基线，不作为当前应用内下载功能证据。
  - 验收标准：设置入口可打开；固定 GitHub latest Release 请求不携带 Coolapk 凭据；检测 `v1.1.0`；检测成功/失败/未检查时都可打开固定 Release 浏览器页面；应用内不再出现下载/安装动作；Debug 使用 `com.godmiracle.coolapk.debug` 并可与正式包并存；正式包升级仍遵守同包名、同签名和更高 `versionCode`。

- [ ] R-016 验收 iOS 请求身份下的评论接口
  - 优先级：高
  - 可信度：实现依据上游 `c001apk_next` 快照 `d0c5843c8e366f92fe48ee5c2de8f0863c2fb052`；当前实时行为未验证
  - 涉及文件：`util/IosHeaders.kt`、`util/IosDeviceUtils.kt`、`util/AddCookiesInterceptor.kt`、评论列表与楼中楼只读链路
  - 状态：用户于 2026-11-09 手工确认评论区正常；分页、楼中楼和请求身份细项尚无单独验收记录
  - 证据：用户反馈 v1.1.2 手工测试评论区正常；这不覆盖帖子图片全屏预览问题。
  - 验收标准：在 Android 设备上确认评论列表和楼中楼请求携带 iOS Header 并成功返回、分页正常显示；确认非可信 Host 不带设备身份；不得记录或分享完整 Token、UUID 或 Cookie。

- [ ] R-017 验收帖子图片全屏预览
  - 优先级：高
  - 可信度：加载失败后全屏层拦截触摸的根因及双指缩放修复已在 OPPO PMX110 / Android 17 真机验证；其余交互仍待验收
  - 涉及文件：`mojito/src/main/java/net/mikaelzero/mojito/ui/ImageMojitoFragment.kt`、`mojito/src/main/java/net/mikaelzero/mojito/MojitoView.java`、`SketchImageViewLoader/src/main/java/net/mikaelzero/mojito/view/sketch/SketchContentLoaderImpl.kt`
  - 状态：失败时禁用全屏加载层点击的修复已构建并在真机验证；双击、单指拖动关闭、横向切换和点击退出待验收
  - 证据：此前的 `MojitoView` 多指转发调整不能单独解决问题。真机日志确认 `loadImageFail()` 后全屏 `loadingLayout` 的点击监听关闭了预览，导致图片缩放器收不到事件；禁用失败层点击后，双指放大缩放值由约 `2.39` 增至 `3.24`，反向捏合降至约 `2.75`。
  - 验收标准：确认双指捏合能放大和缩小、双击缩放正常；单指拖动关闭、横向切换及预览图片点击退出行为正常。

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

- [ ] R-009 评估旧 Android Token 配置的清理策略
  - 优先级：中
  - 可信度：旧设置键仍保留，但已不参与当前 API 请求
  - 涉及文件：`util/PrefManager.kt`、`util/AddCookiesInterceptor.kt`、`ui/settings/SettingsPreferenceFragment.kt`、`ui/settings/params/ParamsPreferenceFragment.kt`
  - 状态：API 请求已切换为 iOS 派生 Token；旧 Android Token 生成器、存储键和测试暂留，是否删除待单独评估
  - 证据：当前 `AddCookiesInterceptor` 不再读取 `PrefManager.customToken/xAppToken` 或调用 `TokenDeviceUtils` 生成 API Token；设置参数页显示 iOS 派生 Token。
  - 验收标准：确认是否清理历史凭证键及旧 Android Token 代码/测试；若清理，确认升级后不影响匿名 iOS UUID 与 WebView `SZLMID`。

- [x] R-010 移除已废弃短信登录入口
  - 优先级：中
  - 可信度：高概率
  - 涉及文件：`ui/login/LoginActivity.kt`、`ui/login/LoginViewModel.kt`、`logic/network/ApiService.kt`
  - 状态：已完成；登录 Activity、ViewModel、资源、网络接口和 Manifest 入口已删除，不安排登录测试
  - 验收标准：当前源码、资源、Manifest 和网络边界不再保留短信登录入口或登录状态机。

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

## 当前进行中：UI-GLASS-EXPANSION

- [ ] 将 LiquidGlass 原生层扩展到适合的页面级操作 Chrome
  - 优先级：中
  - 涉及文件：LiquidGlassFrameLayout、BasePager、User、Feed、Search 及相关 XML/主题资源
  - 状态：匿名只读范围清理和 Feed 回复可读性修正已落地；最低版本后来提升到 API 33，完整验收仍待 API 33、三键导航、放大字体、TalkBack、全页面动作和性能前后对比
 - 证据：rasen/changes/liquid-glass-native-surface-expansion/evidence/device-validation.md、window-boundary.md；设备截图和 UI hierarchy 位于本次本机临时验证目录
  - 自动化补充：匿名清理后 `assembleDebug`、`assembleDebugAndroidTest`、`testDebugUnitTest`（26 个）和 `lintDebug` 通过，GlassSurface 定向真机 instrumentation 1/1 通过；历史 connected 22/22 结果不计入本轮清理后的覆盖。Search 基线与当前 Glass 版本三次独立长滑的加权 jank 为 23.21% 与 23.00%，未形成一致性回归，但全页面性能前后对比仍待完成
  - 边界：不包裹普通列表项、评论/回复、Feed 排序栏、头像、图片、加载/错误、Preference、WebView、BottomSheet/Dialog；Feed 回复编辑、登录、消息和远程写入入口已移除
  - 备注：一次长距离首页滑动曾观察到 Launcher，短距离复测未重现，需在完整滚动性能验收中继续关注

- [ ] UI-GLASS-002 将主导航玻璃升级为 KSU/Miuix 风格折射并迁移到 Android 16 工具链
  - 优先级：中
  - 涉及文件：`view/ksu/`、`MainActivity`、主布局、Android/Kotlin 构建配置、底栏真机测试
  - 状态：直接适配 KernelSU 上游底栏及 Miuix Blur 0.9.4；用户选择滚动时始终显示。独立测试包在 OPPO PMX110 / API 37 上通过底栏手势及实际页面 inset 共 2 项测试，完成滚动常驻和静止重绘检查；原生 Compose 背景对照与按压截图已记录。一次首页长滑为 372 帧，系统新口径卡顿 2 帧、legacy 口径 251 帧，尚无可比基线。Actions Release 包已验签并覆盖安装真机，正式包前台交互、API 33 / Android 16、三键导航、浅色与滚动性能对比仍待完成，详见 2026-11-10 会话。
  - 验收标准：API 33+ 检查浅色/深色、选中项移动、内容滚动和页面切换；Android 16 检查系统导航手势区/三键导航、前景图标文字清晰度及玻璃区域折射连续性；对比旧版本检查长列表滚动帧率
  - 边界：Android 16 大屏下方向/可调整性策略、阿拉伯及南亚文字高度、定时任务行为属于全应用 targetSdk 迁移续验

- [x] ANON-001 清理匿名只读范围内的账号、消息和编辑器残留
  - 优先级：高
  - 状态：已完成代码删除、资源清理、网络测试收口、Debug/AndroidTest 构建、26 个 JVM 单测、lint 和定向 GlassSurface 真机测试；未运行登录/发帖/回复编辑/消息测试
  - 验收标准：主源码、资源、Manifest、测试和依赖无登录/发帖/回复编辑/消息不可达残留；保留服务端评论只读展示、本地功能和 `EmojiUtils` 富文本渲染。
