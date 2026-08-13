## ADDED Requirements

### Requirement: Repository network calls are cancellable

`NetworkRepo` 的挂起网络调用 MUST 将协程取消传递给底层 Retrofit `Call.cancel()`，避免页面销毁后仍持续执行请求。

#### Scenario: ViewModel scope is cancelled

- **WHEN** 发起网络请求的 ViewModel 被清理并取消其协程
- **THEN** 底层 Call 被取消，Repository 不再发出成功或普通失败结果

#### Scenario: Call fails before response

- **WHEN** OkHttp/Retrofit 报告网络连接失败
- **THEN** Repository 发出包含原始失败原因的 `Result.failure`，且不会崩溃进程

### Requirement: Non-success HTTP responses are failures

Repository MUST 将非 2xx HTTP 响应转换为失败结果，并保留状态码等非敏感诊断信息；不得仅因错误响应包含 body 就当作成功。

#### Scenario: Server returns an HTTP error

- **WHEN** 服务端返回 401、403、404 或 5xx
- **THEN** 调用方收到失败结果并可根据状态码处理，不会进入成功数据分支

#### Scenario: Successful response has an empty body

- **WHEN** 需要实体的请求返回 2xx 但 body 为空
- **THEN** Repository 返回明确的空响应失败，而不是向下游传递不完整对象

### Requirement: Coroutine cancellation is not swallowed

网络封装 MUST 重新抛出 `CancellationException`，不得把取消转换成普通业务失败并继续更新 UI 状态。

#### Scenario: CancellationException is raised

- **WHEN** 底层请求或协程抛出 `CancellationException`
- **THEN** Flow 终止并继续传播取消信号，调用方不会收到普通 `Result.failure`
