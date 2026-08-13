## Context

项目当前使用多个独立 Room 数据库、Hilt 提供的 Retrofit/OkHttp 客户端和 XML WebView 页面。静态审查发现：部分迁移删除旧表但不复制数据；带 `X-App-Token` 的客户端同时接受任意 `@Url`；WebView Activity 在销毁时结束整个进程；接口/设备状态和普通偏好混存在同一个 SharedPreferences；`NetworkRepo` 自己包装 Retrofit Call，但没有把协程取消和 HTTP 状态完整传递给调用方。

本变更面向当前 Android 客户端，不改变后端接口，不改变 Token 用于接口访问的业务前提，也不实现登录流程或分页架构重构。

## Goals / Non-Goals

**Goals:**

- 让已存在的 Room 数据库可以安全升级并保留用户数据。
- 确保应用访问凭证只出现在受信任的 Coolapk HTTPS 请求中。
- 让 WebView 遵守正常 Activity 生命周期。
- 让接口凭证和设备身份状态不随系统备份跨设备恢复。
- 让网络取消、非成功 HTTP 响应和底层失败具有稳定、可测试的语义。
- 为每项行为补充自动化回归证据。

**Non-Goals:**

- 不实现、修复或重构账号登录状态机。
- 不移除 `X-App-Token`，不改变服务端鉴权协议。
- 不把手工分页状态迁移到 Paging 3，也不在本 change 中重写列表更新逻辑。
- 不删除旧网络封装；只保证当前 `NetworkRepo` 主链路的行为。

## Decisions

### 1. Room 迁移以导出 schema 和真实旧表为准

每条迁移都采用“创建新表、复制兼容字段、删除旧表、重命名”的顺序，并用 `MigrationTestHelper` 验证升级后的 schema 和数据。历史提交显示旧版 `FeedFavorite` v1 只有 `feedId` 和自增 `id`，而历史 version 2 还存在完整的 `FeedFavorite` 表；当前 version 2 则是 `FeedEntity`。因此收藏库升级到 version 3：v1 直接迁移到当前表并对历史不存在的用户/内容字段使用空字符串默认值，旧 v2 统一转换为当前 `FeedEntity`，已是 `FeedEntity` 的 v2 保持数据不动。

选择显式迁移而不是 destructive migration，是因为历史记录、收藏、首页菜单和最近用户都是用户可见数据；不修改已发布的 version 2 schema，避免让已有 Room identity hash 再次漂移。

### 2. 在 OkHttp 拦截器边界做 host allowlist

从最终解析后的 `Request.url` 判断 scheme 和 host。只有 `https` 且 host 属于 `api.coolapk.com`、`api2.coolapk.com` 或 `account.coolapk.com` 时，才添加 `X-App-Token`、Cookie 和应用身份 Header；其他 URL 不携带凭证。直接 `@Url` 请求和跨 host 重定向都要有测试。

选择拦截器边界而不是逐个修改所有 `@Url` 调用，是因为它能覆盖当前 API1/API2 客户端共用的请求路径，并形成单一安全网。

### 3. WebView 只负责释放资源，不负责进程控制

保留现有 WebView 清理逻辑，移除 `exitProcess(0)`。Activity 的返回、重建和销毁由 Android 生命周期管理；本 change 不顺手调整 WebView 页面功能或登录行为。

### 4. 将接口/设备状态从普通偏好中隔离

把 `xAppToken`、`xAppDevice`、`customToken` 以及可能存在的账号 Token/身份字段迁移到专用 credentials SharedPreferences，并在备份规则中排除该文件；普通主题、字体和 UI 偏好仍可按原策略保留。首次运行时兼容读取旧 `settings` 中的字段并完成一次迁移，迁移后清理旧敏感键。

选择分离存储而不是排除整个 `settings.xml`，以避免为了保护接口状态而丢失普通用户偏好。

### 5. 保持现有 Repository API，修复 Call 适配语义

优先在 `NetworkRepo` 的 Call adapter 中使用可取消的挂起桥接：协程取消时调用底层 `Call.cancel()`；`await` 和 `response` 对非 2xx 响应统一失败；`CancellationException` 不转换成普通 `Result.failure`。这样可以降低对已有 ViewModel 的接口影响，分页状态另行处理。

## Risks / Trade-offs

- [历史 schema 缺失] → 从导出 schema、Git 历史或旧版 APK 恢复 fixture；没有 fixture 时暂停迁移实现，不猜字段。
- [第三方或服务端 URL 被当前业务依赖] → 先允许无凭证访问并记录命中 host，再决定是否完全拒绝；所有受保护接口必须使用 allowlist host。
- [专用 credentials 存储迁移中断] → 迁移采用幂等复制，成功写入新文件并校验后再删除旧键；补充重复执行测试。
- [HTTP 错误语义变化] → 统一在 Repository 层转换，并保留响应码/脱敏错误信息；补充 2xx、4xx、5xx、空 body 测试。
- [instrumentation 测试环境不稳定] → 生命周期测试只验证 Activity/进程不被主动终止，网络和数据库核心行为优先使用 JVM/Room fixture 测试。

## Migration Plan

1. 先加入迁移、拦截器和 NetworkRepo 的回归测试，保留失败证据。
2. 实现 Room schema/data migration，并在所有数据库升级路径通过后再合入。
3. 实现 Token host allowlist、credentials 偏好迁移和备份规则。
4. 移除 WebView 进程退出并运行生命周期测试。
5. 运行 unit test、lint、instrumentation test；若旧数据库 fixture 或真实接口行为不确定，暂停发布而不使用 destructive fallback。

回滚时应回滚整个应用版本，不应把数据库版本号降级；已执行的新 schema 需要通过后续前向迁移处理。

## Open Questions

- 当前已从 Git 历史恢复目标数据库的旧实体定义；仍需在 fixture 测试中确认不同历史版本的 room identity hash 不影响升级路径。
- `PrefManager.token` 在当前非登录使用场景下是否仍可能含有账号 Cookie Token？若是，必须与 `xAppToken` 同等排除备份。
- 是否存在服务端返回的非 Coolapk URL 必须由 API 客户端直接访问？若存在，该请求必须改走无凭证公共客户端。
