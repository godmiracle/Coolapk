# stabilize-core-data-network-and-lifecycle Review Report

## 审查元数据

- 角色：独立 reviewer
- 模式：dispatched / report-only
- 流水线：small-feature / verify
- verifyPolicy：standard
- 基线：`HEAD` 与本地 `origin/main` 均为 `d41d41d23f001d85a567a9b75dacbe5949d945ce`；审查对象为该基线加完整工作树
- Git 证据：已检查 `git diff`、`git diff --cached`、`git status --short`；暂存区为空；已逐一读取本 change 范围内的 untracked 实现、测试、fixture、文档和规格文件
- 测试策略：遵照调度约束，本 reviewer **未运行、未生成测试**；测试 gate 由 LEAD 独立执行
- 其他 gate：`git diff --check` 通过；当前无可定位 PR/Greptile 评论；变更不包含需要视觉审查的前端页面

## 结论

**Verdict: FAIL / CHANGES_REQUIRED**

唯一问题共 **9** 个：

| Severity | 数量 |
|---|---:|
| Blocker | 3 |
| Major | 2 |
| Minor | 4 |
| Trivial | 0 |

Scope check：**REQUIREMENTS MISSING**。实现覆盖了五个 capability 的主要代码路径，但 Room 真实升级链、Account 重定向凭据边界，以及多项规格要求的回归证明尚不成立。未发现与本 change 无关的产品代码扩张；工作树中的 `.codex/`、`.rasen/` 等流程资产不计入产品审查范围。

## Standards 轴

### S-01 [Blocker] 真实 HomeMenu v1/v2 升级到 v5 会在 3→4 阶段发生主键冲突

- 位置：`app/src/main/java/com/godmiracle/coolapk/di/DatabaseModule.kt:285-316`；`app/src/androidTest/java/com/godmiracle/coolapk/logic/database/DatabaseMigrationTest.kt:143-159`
- 证据：已发布 v1 初始化逻辑在历史提交 `87798f5` 中依次插入“关注、应用、头条、热榜、话题”，`HomeMenu.id` 为自增主键，因此实际记录占用 id 1–5。当前 1→2 migration 再插入“数码”，得到 id 6；2→3 原样把 `id` 复制为 `position`；3→4 又无条件插入 `position=6` 的“酷图”，触发 `HomeMenu.position` 主键冲突并中止 Room migration。现有 fixture 只写入 id 3、4，1→2 插入落在 id 5，错误地让 position 6 保持空闲，因此无法复现真实安装数据。
- 影响：从真实 v1 或 v2 安装升级时 `home_menu.db` 无法打开，首页菜单及依赖该数据库的启动路径可能失败。这是发布数据升级阻断和核心功能不可用风险。
- 建议修复：在 2→3 中把历史自增 id 映射为确定且无冲突的顺序（例如按 id 排序后生成 0-based dense position），或让后续新增菜单通过查询空闲位置/幂等 upsert 插入；不要重新解释已发布 schema。
- 验收：增加真实五条默认记录 + 1→2 “数码”记录的 v1/v2 fixture，分别执行完整 1→5、2→5 链，断言全部标题、顺序、启用状态和最终 schema identity。
- Fix class：ASK（数据迁移语义需先确认）。

### S-02 [Blocker] Room migration 测试依赖被 Git 忽略的历史 schema，干净检出无法复现

- 位置：`app/build.gradle.kts:116-119`；`app/.gitignore:4`；`app/src/androidTest/java/com/godmiracle/coolapk/logic/database/DatabaseMigrationTest.kt:75-136`
- 证据：`androidTest` 把 `app/schemas` 加入 assets，但 `/schemas` 被 `.gitignore` 排除，且 `git ls-files app/schemas` 为空。本地忽略目录恰好含 `FeedFavoriteDatabase/2.json`，而已纳入本次工作树的 `app/src/androidTest/assets` 只有 FeedFavorite/HomeMenu/RecentAtUser 的 v1 fixture。两个测试在第 77、121 行调用 `createDatabase(..., 2)`；干净检出只能由当前 Room 模型重新生成当前 v3 schema，无法重建历史 v2 JSON。
- 影响：迁移测试在开发机上可能借用残留 schema 通过，但 CI/新 clone 缺少 v2 输入，测试 gate 不可复现并会在创建 v2 数据库前失败；当前“迁移已验证”证据不能随变更交付。
- 建议修复：将所有测试依赖的历史 Room schema 纳入版本控制（解除 `app/schemas` 的忽略并提交必要版本），或把包括 FeedFavorite v2 在内的完整 fixture 放入版本化的 androidTest assets；避免同时依赖版本化与被忽略的两个来源。
- 验收：从干净检出、无本地 `app/schemas` 残留的环境运行 migration suite，确认所有 fixture 均来自 Git 跟踪文件。
- Fix class：AUTO-FIX（fixture/ignore 配置），但本次 reviewer 不修改。

### S-03 [Blocker] Account 客户端未应用最终 host 边界，跨 host 重定向可转发登录 Cookie

- 位置：`app/src/main/java/com/godmiracle/coolapk/di/NetworkModule.kt:132-139`；`app/src/main/java/com/godmiracle/coolapk/util/LoginCookiesInterceptor.kt:15-38,62-109`；`app/src/test/java/com/godmiracle/coolapk/logic/network/NetworkEndpointsTest.kt:19-40`
- 证据：API1/API2 已把 `AddCookiesInterceptor` 改为 network interceptor，但 Account client 仍把 `LoginCookiesInterceptor` 注册为 application interceptor并默认跟随重定向。该 interceptor 会添加 `SESSID` Cookie、User-Agent、`X-Requested-With`、`X-App-Id` 等身份头。项目使用 OkHttp 4.12.0；其 application interceptor 位于 follow-up 循环外，跨 origin follow-up 会继承原请求头，框架只自动移除 `Authorization`，不会移除手工 `Cookie` 或这些 X-* 头。因此 `account.coolapk.com` 返回到非 allowlist HTTPS host 的重定向时，凭据仍可发往新 host。现有测试只调用纯函数 `isTrusted`/`stripCredentials`，没有经过 Account client、真实 interceptor 链或重定向。
- 影响：服务端重定向、错误配置或被劫持的重定向响应可把账号会话与设备/应用身份暴露给 allowlist 外 host，直接违反敏感凭据 host/HTTPS 边界。
- 建议修复：给 Account client 增加共享的最终请求 network sanitizer（在每次网络发送前按 HTTPS + allowlist 移除完整敏感头集合），或安全拒绝跨边界重定向；保留登录 flag 状态机时不要简单把有状态 interceptor 重跑而不处理 follow-up 继承头。
- 验收：用实际 OkHttp interceptor/client 链覆盖受信 host、外部 host、HTTP、可信→不可信重定向，并同时断言 Token、Cookie、X-App-Device、User-Agent 和所有应用身份头；Account 与 API 客户端都必须覆盖。
- Fix class：ASK（安全边界与登录状态机交互）。

### S-04 [Major] WebView 生命周期测试在旧 `exitProcess(0)` 实现下也可能通过

- 位置：`app/src/androidTest/java/com/godmiracle/coolapk/ui/others/WebViewActivityLifecycleTest.kt:15-44`；`app/src/main/AndroidManifest.xml:451-454`
- 证据：测试仅启动 WebViewActivity、按返回键、再启动一次并检查 `dumpsys` 中出现类名。WebViewActivity 运行在独立 `:webview` 进程；旧实现的 `exitProcess(0)` 会杀死该子进程，但 Android 随后可为第二次启动创建新进程，因此这套断言仍会通过。测试没有捕获/比较进程 PID，也没有宿主 Activity 存活断言，更没有证明销毁路径未调用进程退出。
- 影响：当前源码确实移除了 `exitProcess(0)`，但回归测试无法防止该缺陷再次出现，未满足规格中的生命周期验证要求。
- 建议修复：让 instrumentation 在关闭前后验证 `:webview` 进程没有因应用代码立即终止（例如记录 PID/进程生命周期），并显式验证宿主 Activity 与测试进程继续存活；也可引入可观测的进程终止 seam，断言销毁路径从不调用它。
- 验收：先确认测试在恢复旧退出调用时可靠失败，再确认当前实现通过，并覆盖返回与配置重建路径。
- Fix class：AUTO-FIX（测试），但本次 reviewer 不修改。

### S-05 [Major] “每条已注册 migration” 的最终 schema 证明未实现

- 位置：`app/src/main/java/com/godmiracle/coolapk/di/DatabaseModule.kt:65-72,83-90,101-108,119-126,171-177,354-359`；`app/src/androidTest/java/com/godmiracle/coolapk/logic/database/DatabaseMigrationTest.kt:52-188`
- 证据：规格要求每条已注册 migration 都有 fixture 验证最终 identity、表、列、主键/索引和关键数据。当前 suite 只有 FeedFavorite、HomeMenu、RecentAtUser 五个测试；四个复用 `StringEntityDatabase_MIGRATION_1_2` 的数据库及 `LocalFollowDatabase_MIGRATION_1_2` 没有 fixture。尤其 StringEntity 1→2 把 `data` 改为主键但直接 `INSERT ... SELECT`，旧 schema 允许重复 data 时可能产生唯一约束失败，当前没有覆盖该历史数据形态。
- 影响：change 对 Room 全量 migration 安全性的声明超出实际证据；未覆盖路径仍可能在用户升级时失败，且无法确认最终 schema identity。
- 建议修复：为每个注册数据库准备已发布 schema fixture和代表性数据；StringEntity fixture至少包含重复/顺序边界并明确确定性去重策略，LocalFollow 断言新列默认值和已有行保留。
- 验收：migration suite 枚举并覆盖所有 `addMigrations` 注册路径，`runMigrationsAndValidate(..., true, ...)` 后核验关键数据及 schema。
- Fix class：ASK（重复数据保留策略需确认）。

### S-06 [Minor] 备份规则测试无法证明 cloud backup 与 device transfer 均排除凭据

- 位置：`app/src/androidTest/java/com/godmiracle/coolapk/util/CredentialPreferencesMigrationTest.kt:65-88`；`app/src/main/res/xml/data_extraction_rules.xml:6-13`
- 证据：同一个 helper 只扫描是否存在任意 `<exclude domain="sharedpref" path="credentials.xml">`。对 `data_extraction_rules.xml`，即使删除 cloud-backup 或 device-transfer 其中一项，测试仍会通过。当前 XML 实现同时包含两项，但测试没有验证父节点语义，也没有断言普通 `settings.xml` 未被排除。
- 影响：未来规则回归可能让凭据进入云恢复或设备迁移而不触发测试。
- 建议修复：解析父节点并分别断言 `cloud-backup`、`device-transfer`，另行断言 legacy full-backup；同时确认普通设置仍遵循既有备份策略。
- Fix class：AUTO-FIX（测试），但本次 reviewer 不修改。

### S-07 [Minor] 凭据迁移测试未覆盖完整键集合与失败重试边界

- 位置：`app/src/main/java/com/godmiracle/coolapk/util/CredentialPreferencesMigration.kt:9-68`；`app/src/androidTest/java/com/godmiracle/coolapk/util/CredentialPreferencesMigrationTest.kt:38-63`
- 证据：实现迁移 19 个键，测试只布置 `isLogin`、`token`、`xAppToken`、`xAppDevice`、`customToken`，且未断言 `isLogin` 的目标值/旧值清理；账号摘要、完整设备身份、User-Agent、SZLMID 等没有覆盖。测试也没有模拟目标 commit、旧值清理 commit 或 marker commit 失败后的重试。
- 影响：键清单遗漏、类型变化或两阶段 commit 回归可能使敏感值残留在可备份的 `settings.xml`，而现有测试仍通过。
- 建议修复：参数化覆盖全部 credential key，并用可控 SharedPreferences fake 分别模拟三个 commit 失败点，验证重试不清空目标值且最终清理 legacy 键。
- Fix class：AUTO-FIX（测试），但本次 reviewer 不修改。

### S-08 [Minor] 网络错误/取消测试未覆盖规格列出的 404 与 Repository 取消传播

- 位置：`app/src/test/java/com/godmiracle/coolapk/logic/repository/NetworkCallAdapterTest.kt:57-117`；`app/src/main/java/com/godmiracle/coolapk/logic/repository/NetworkCallAdapter.kt:43-50`；`app/src/main/java/com/godmiracle/coolapk/logic/repository/NetworkRepo.kt:260-270`
- 证据：HTTP 测试只枚举 401、403、500，遗漏规格明确列出的 404。取消测试只证明 coroutine cancellation 调用 `Call.cancel()`；fake 在取消后不触发 callback，因此没有覆盖 Retrofit 常见的 `onFailure` + `call.isCanceled` 分支。也没有通过 `NetworkRepo.fire`/Flow 收集证明 `CancellationException` 被重新抛出且不会发出 `Result.failure`。
- 影响：核心实现静态上符合目标，但最关键的取消竞态和 Repository 语义缺少回归证明。
- 建议修复：加入 404；让 fake 在 cancel 后回调 onFailure；经可测试的 Repository/Flow seam 断言取消终止收集且零 emission，网络 IOException 则成为原始原因的 failure。
- Fix class：AUTO-FIX（测试），但本次 reviewer 不修改。

### S-09 [Minor] 架构/API/开发文档仍描述旧存储、旧网络错误模型与旧测试基线

- 位置：`docs/architecture.md:112,129-130,142-149,215,221,226`；`docs/context.md:56-59,112`；`docs/development.md:91,125-141`；`docs/api.md:58-76`
- 证据：文档仍称 NetworkRepo 使用 `suspendCoroutine`、HTTP 非 2xx 未统一；仍称凭据都在普通 `settings` SharedPreferences；仍记录只有 6 个 JVM/2 个 instrumentation 用例且 Room/WebView 不可覆盖。`docs/api.md` 未记录最终 host/HTTPS allowlist、跨 host 重定向策略和新的 HTTP/取消语义。项目维护规则明确要求修改请求 Header 或数据库 migration 时同步更新 `docs/api.md`、architecture、todo。
- 影响：后续维护者会依据错误的安全边界、存储位置和错误模型继续开发或审计，降低本 change 的可维护性与可验证性。
- 建议修复：在代码和测试问题解决后同步刷新上述文档，明确证据层级；不要把 LEAD 尚未运行的 test gate 写成 reviewer 已验证。
- Fix class：AUTO-FIX（文档），但本次 reviewer 不修改。

## Spec 轴

以下为独立的规格符合性判断；问题编号交叉引用 Standards 轴，不重复计入 9 个唯一 finding。

| Capability | 结果 | 规格证据 | Finding |
|---|---|---|---|
| database-migration-safety | **FAIL** | 真实 HomeMenu 链会冲突；历史 schema fixture 不可从干净检出获得；未覆盖每条已注册 migration | S-01, S-02, S-05 |
| api-request-boundary | **FAIL** | API network interceptor 方向正确，但 Account application interceptor 的跨 host follow-up 仍携带凭据；四类实际 interceptor 测试缺失 | S-03 |
| credential-backup-policy | **PARTIAL** | 独立 prefs、迁移顺序及两套 XML 的实现静态上合理，但测试未证明完整键集、失败重试及 cloud/device 两类排除 | S-06, S-07 |
| webview-lifecycle-safety | **PARTIAL** | 产品代码已移除 `exitProcess(0)` 并保留资源清理；instrumentation 不能证明退出路径未被调用 | S-04 |
| network-error-cancellation | **PARTIAL** | `suspendCancellableCoroutine`、HTTP error、empty body 和 `CancellationException` 处理静态上合理；404、取消 callback 与 Flow 传播未覆盖 | S-08 |

## 覆盖图

```text
CODE PATH COVERAGE

Room
  Feed v1 -> v3              [test code exists; clean fixture chain blocked by S-02]
  Feed v2 legacy/current ->3 [test code exists; v2 schema not versioned]
  Home v1/v2 -> v5           [GAP: representative data triggers S-01]
  RecentAtUser v1 -> v2      [covered path, pending LEAD test gate]
  StringEntity/LocalFollow   [GAP: no fixture tests]

Credential request boundary
  Pure host/strip helpers    [covered]
  API actual interceptor     [GAP: trusted/external/HTTP/redirect chain]
  Account redirect follow-up [FAIL: credentials can cross host]

Credential persistence
  Basic copy/cleanup/repeat  [partial]
  All keys/commit failures   [GAP]
  Cloud + device exclusions  [implementation present; assertion partial]

WebView
  Resource cleanup           [implementation present]
  No process exit regression [GAP: old implementation can pass test]

Network
  success/empty/401/403/500/transport/cancel/302 [test code exists]
  404/onFailure-after-cancel/Flow cancellation   [GAP]

USER FLOW COVERAGE
  Existing install database upgrade       [FAIL/GAP]
  Trusted request redirected externally   [FAIL/GAP]
  Restore/transfer without credentials     [PARTIAL]
  Close/reopen WebView without process kill[PARTIAL]
  ViewModel destroyed during HTTP request  [PARTIAL]
```

## 验证边界

- 本报告是源码、Git 历史、工作树、fixture 可交付性及依赖行为的静态审查结论。
- 未运行任何 Gradle、JVM、instrumentation 或设备测试；本文不把现有 session 文档中的历史执行记录当作本 reviewer 的 test gate。
- 修复 Blocker 后应由 LEAD 从干净检出执行测试 gate；Room 与 WebView 还需要设备/instrumentation 证据，网络边界需要真实 interceptor/redirect 级测试。

REVIEW:v1
SPEC:v1

## Round 1 Re-review

### 审查元数据

- 角色：`mandatory non-author re-reviewer`，rasen-auto review-loop 第 1 轮回归审查；本 leaf 为 dispatched/report-only。
- 变更：`stabilize-core-data-network-and-lifecycle`。
- 工作树：`main`，HEAD `d41d41d23f001d85a567a9b75dacbe5949d945ce`。按要求未因分支名停止，审查了当前工作树相对 HEAD 的完整 tracked diff，以及本 change 范围内的 untracked 源码、测试、fixture 和文档。
- 审查动作：只读检查源代码、OkHttp/Room wiring、测试实现、fixture、spec 和文档；没有修改代码，没有生成或运行测试，没有创建 subagent，没有提交，也没有修改 `.rasen` 状态文件。本次唯一写入是向本文件追加本节。
- LEAD 已独立完成、此 reviewer 未重跑的 gate：`:app:testDebugUnitTest` 22/22 passed；`:app:lintDebug` passed；`:app:compileDebugAndroidTestKotlin` passed；Pixel_10 AVD Android 17 上 `:app:connectedDebugAndroidTest` 21/21 passed。

### S-01..S-09 状态

| Finding | Round 1 re-review 状态 | 精确证据与结论 |
|---|---|---|
| S-01 | CLOSED | `app/src/androidTest/java/com/godmiracle/coolapk/logic/database/DatabaseMigrationTest.kt:148-189,347-358,360-366,505-513` 在 v1 和 v2 fixture 上都写入真实默认五项（关注、应用、头条、热榜、话题），v2 还覆盖迁移新增的数码，并断言热榜状态、最终行集合和 identity hash。`app/src/main/java/com/godmiracle/coolapk/di/DatabaseModule.kt:291-338` 的 2→3 dense position、3→4 酷图插入和 4→5 主键换表不会再与真实默认数据发生 position 冲突。原 Blocker 已关闭。 |
| S-02 | CLOSED | `DatabaseMigrationTest.kt:331-345` 的 `MigrationTestHelper` 固定读取 `room-migration-fixtures`，不解析被 `/schemas` 忽略规则覆盖的 KSP 输出；当前工作树包含所有版本化 JSON fixture，且各测试用 `runMigrationsAndValidate(..., true, ...)`。Room fixture 覆盖路径见 `app/src/androidTest/assets/room-migration-fixtures/` 下的 FeedFavorite v1/v2/v3、legacy FeedFavorite v2/v3、HomeMenu v1/v2/v5、RecentAtUser v1/v2、LocalFollow v1/v2 和四组 StringEntity v1/v2。原 Blocker 已关闭。 |
| S-03 | CLOSED | `app/src/main/java/com/godmiracle/coolapk/logic/network/NetworkEndpoints.kt:17-19,22-99` 对 HTTPS、三个 trusted host 和完整 Cookie/User-Agent/X-App-*/X-Sdk-*/Sec-* 等身份头做最终 sanitize；`app/src/main/java/com/godmiracle/coolapk/di/NetworkModule.kt:112-166` 将 API、API2、Account 的 boundary 放在实际 OkHttp network chain 最后，并保留 redirect/no-redirect 语义。`app/src/test/java/com/godmiracle/coolapk/logic/network/NetworkRequestBoundaryTest.kt:52-100,102-125,128-218` 同时核对生产 wiring、可信/外部/HTTP/跨 host redirect、Account flag 和实际链路；`:209-213` 用逻辑 `Host`/`:authority` 断言，明确没有把 MockWebServer 的 localhost 当作逻辑 host。原 Blocker 已关闭。 |
| S-04 | CLOSED | `app/src/main/java/com/godmiracle/coolapk/ui/others/WebViewActivity.kt:321-348` 的 `onDestroy()` 只做 WebView 资源释放，没有 `exitProcess`。`app/src/androidTest/java/com/godmiracle/coolapk/ui/others/WebViewActivityLifecycleTest.kt:27-58` 要求返回销毁后 child PID 存活且重开复用同一 PID；`:60-99` 要求配置重建后 PID 不变并确认宿主进程存活，因此旧的 `exitProcess(0)` 实现会在 PID 观察断言处失败。原 Major 已关闭。 |
| S-05 | CLOSED | 所有已注册迁移均有版本化 wiring：`DatabaseModule.kt:65-126,153-214` 覆盖四类 StringEntity、FeedFavorite 1→3/2→3、LocalFollow 1→2、HomeMenu 1→2/2→3/3→4/4→5、RecentAtUser 1→2；测试覆盖 Feed `DatabaseMigrationTest.kt:40-146`、Home `:148-189`、Recent/Local/StringEntity `:192-329`，每条路径均断言最终 identity 和关键数据。StringEntity duplicate fixture 在 `:300-329` 明确以 `MAX(id)` 保留最新 duplicate，迁移 SQL 位于 `DatabaseModule.kt:366-380`；RecentAtUser 则以 `ORDER BY id` + `INSERT OR IGNORE` 保留最早记录。原 Major 已关闭。 |
| S-06 | CLOSED | `app/src/main/res/xml/backup_rules.xml:8-10` 只在 full-backup 排除 `credentials.xml`；`data_extraction_rules.xml:6-12` 分别覆盖 cloud-backup 和 device-transfer。`app/src/androidTest/java/com/godmiracle/coolapk/util/CredentialPreferencesMigrationTest.kt:100-134` 对三种语义逐项断言，并确认普通 `settings.xml` 不被排除。原 Minor 已关闭。 |
| S-07 | CLOSED | `app/src/main/java/com/godmiracle/coolapk/util/CredentialPreferencesMigration.kt:9-29` 明确列出 19 个 credential keys，`:45-79` 采用 destination copy、legacy cleanup、marker 三阶段 commit 且失败不标记完成。`app/src/androidTest/java/com/godmiracle/coolapk/util/CredentialPreferencesMigrationTest.kt:37-48,50-98,136-165` 覆盖完整键集、普通设置保留、重复执行和第 1/2/3 个 commit 失败后的安全重试。原 Minor 已关闭。 |
| S-08 | CLOSED | `app/src/main/java/com/godmiracle/coolapk/logic/repository/NetworkCallAdapter.kt:16-58` 覆盖非 2xx（含 404）、必需空 body、transport failure、协程取消后 `Call.cancel()`、cancel 后 callback 和 `CancellationException`；`NetworkCallAdapterTest.kt:66-175` 逐项断言，并覆盖 NetworkRepo Flow 取消后零 emission 与 transport cause identity。`NetworkRepo.kt:260-287` 只在直接 cause 为同一异常类型且 wrapper stack 含 `_COROUTINE.` 时还原原始 transport instance；该条件对应 kotlinx.coroutines 的恢复形态，不遍历任意 cause chain，也没有发现过度拟合或误还原路径。原 Minor 已关闭。 |
| S-09 | CLOSED | `docs/context.md:38-61,117`、`docs/architecture.md:208,220-227`、`docs/development.md:99-105,130-135`、`docs/api.md:7-12,25-31,51-55,65-87` 均与当前 NetworkRepo/Room/credentials/WebView 实现和证据边界一致；明确区分源码/静态覆盖、LEAD gate、历史设备记录，以及尚未复验的实时 API、真实设备、真实账号和 Release 结论，没有把未执行的实时或设备验证写成当前 gate。原 Minor 已关闭。 |

### 仍开放 findings

无开放的 Blocker、Major、Minor 或 Trivial finding。S-01 至 S-09 均已关闭。

### Accepted-known（不阻塞本轮）

- `DOC-01 [Minor][accepted-known][AUTO-FIX]`：README 和 TODO 中仍有本 change 前的当前计数/基线描述。`README.md:11` 仍写 HEAD `becc810`，`:86` 仍写 JVM 14 / instrumentation 10，`:151` 仍把 14/10 写成当前通过；`docs/todo.md:7-20` 仍将迁移和 fake-Call 覆盖概括为旧的 5/6 测试。当前较新的边界记录已在 `docs/context.md:117`、`docs/development.md:130-135`、`docs/api.md:7-12` 明确为静态计数或 LEAD/历史边界，因此该漂移不改变实现、spec 或本轮 gate，也不升级为 Blocker/Major。后续可在文档整理时同步 README/TODO 的当前基线；本 reviewer 不在允许范围外修改它们。

### Standards / Spec 双轴结论

- **Standards verdict: PASS**。实现路径的 Room 数据保全、最终网络凭证边界、凭证备份隔离、WebView 宿主存活和网络错误/取消可追踪性均有对应代码与测试证据；没有新出现的 Blocker 或 Major。`DOC-01` 仅为 accepted-known Minor。
- **Spec verdict: PASS**。`proposal.md`、`design.md`、`tasks.md` 及五份 delta spec（database migration safety、API request boundary、credential backup policy、WebView lifecycle safety、network error/cancellation）与当前实现和证据闭合；未发现范围外实现改变行为的 scope creep。实时 API、真实账号、Release 和完整业务人工验收仍按 spec/documentation 明确留在验证边界之外。
- **Verdict: CLEAN**

### 验证边界

- 本 reviewer 没有运行或生成任何测试；上面的四项 Gradle/device 结果是 LEAD 独立提供并明确不重跑的 gate，不能表述为本 reviewer 执行结果。
- 本次检查以当前完整工作树为对象，包含本 change 范围内的 untracked fixture、测试和源码；因此交付时这些 untracked 文件必须随实现一起保留，否则 S-02/S-05 等结论不适用于缺失 fixture 的交付物。
- 仍未由本轮证明真实线上 API/登录、真实业务流、Release 签名产物或长期设备稳定性；文档对此保持明确标注。

REVIEW:v1
SPEC:v1
