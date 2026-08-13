## Why

当前核心数据、网络和页面生命周期路径存在会导致用户数据丢失、凭证越界发送、应用进程异常退出以及错误响应被误判为成功的问题。项目已经完成静态审查和基础构建验证，现在优先修复这些发布风险，并补上可回归的测试边界。

本变更不实现或重构登录流程；`X-App-Token` 仍然是接口访问所需的请求凭证。分页状态大规模重构另行处理。

## What Changes

- 修复 FeedFavorite、HomeMenu、RecentAtUser 的 Room 迁移，保证升级过程中保留已有数据。
- 移除 WebView Activity 销毁时的进程退出行为，保持正常 Activity 生命周期。
- 限制携带 `X-App-Token` 的请求只能访问受信任的 Coolapk HTTPS host。
- 明确 SharedPreferences 中接口 Token、设备标识的备份策略，避免跨设备恢复敏感状态。
- 改进 `NetworkRepo` 的协程取消、HTTP 状态码和异常传播语义。
- 为上述路径增加 migration、拦截器、网络封装和生命周期回归测试。

## Capabilities

### New Capabilities

- `database-migration-safety`: Room 数据库升级必须校验 schema 并保留已有用户数据。
- `api-request-boundary`: 带应用访问凭证的请求只能发往受信任的 HTTPS API host。
- `webview-lifecycle-safety`: WebView 页面销毁不得主动终止整个应用进程。
- `credential-backup-policy`: 接口凭证和设备身份数据必须遵循明确的备份/恢复策略。
- `network-error-cancellation`: 网络层必须正确处理协程取消、非成功 HTTP 响应和底层请求失败。

### Modified Capabilities

## Impact

- 影响 `app/src/main/java/com/godmiracle/coolapk/di/DatabaseModule.kt`、Room entity/schema 和数据库测试。
- 影响 `AddCookiesInterceptor`、`NetworkModule`、`ApiService`、`NetworkRepo` 及相关单元测试。
- 影响 `WebViewActivity`、Android 备份规则和应用配置。
- 不新增后端接口，不改变 Token 的业务用途，不处理登录状态机和分页架构。
