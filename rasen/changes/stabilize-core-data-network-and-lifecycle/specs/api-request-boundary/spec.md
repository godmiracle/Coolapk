## ADDED Requirements

### Requirement: Application credentials are limited to trusted API hosts

网络层 MUST 仅在 HTTPS 且目标 host 为 `api.coolapk.com`、`api2.coolapk.com` 或 `account.coolapk.com` 时添加 `X-App-Token`、Cookie 和应用身份 Header。

#### Scenario: Trusted API request carries the app token

- **WHEN** API1 或 API2 客户端请求受信任的 Coolapk HTTPS host
- **THEN** 请求携带接口访问所需的 `X-App-Token` 和既有应用 Header

#### Scenario: External absolute URL does not receive credentials

- **WHEN** Retrofit `@Url` 解析出非 allowlist host 或非 HTTPS URL
- **THEN** 请求不携带 `X-App-Token`、Cookie 或设备身份凭证

#### Scenario: Redirect crosses the trust boundary

- **WHEN** 受信任请求重定向到非 allowlist host
- **THEN** 后续请求不向新 host 转发应用凭证，或请求被安全策略拒绝

### Requirement: Host boundary behavior is regression tested

请求拦截器 MUST 有测试覆盖 allowlist host、外部 host、非 HTTPS 和跨 host 重定向。

#### Scenario: Interceptor tests assert the complete header set

- **WHEN** 测试构造上述四类请求
- **THEN** 测试同时检查 Token、Cookie、X-App-Device 和相关应用 Header，而不是只检查 URL
