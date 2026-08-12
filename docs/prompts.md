# AI 协作提示词记录

本文件保存适用于本项目的启动、分析和验证提示词。提示词不能替代源码、构建、测试和真实设备证据。

## 开始任务

```text
请先读取 README.md、AGENTS.md、docs/context.md、docs/architecture.md、docs/decisions.md、docs/todo.md，
再检查 settings.gradle.kts、app/build.gradle.kts、AndroidManifest.xml 和当前任务涉及的源码。
先说明已确认事实、未确认项和计划，不要根据 Coolapk/Android 经验猜测接口或类名。
```

## 网络/API 变更

```text
请先确认当前 UI ViewModel 是否使用 NetworkRepo，API 应该走 API1、API2 还是 Account，
以及是否需要跟随重定向。检查 ApiService、NetworkModule、NetworkRepo、拦截器、调用方和错误状态。
修改后补充 docs/api.md、docs/decisions.md 和 docs/todo.md，并执行最小构建/测试；真实接口未验证时明确标记。
```

## 登录、Token、WebView 或上传

```text
这是敏感链路。请先检查 CookieUtil、PrefManager、拦截器、WebViewActivity、OSSUpload 和 Manifest 权限。
不要输出真实 Cookie、Token、密码、验证码或 STS 凭据；不要把 Debug BODY 日志当作可分享证据。
分别说明源码路径、编译结果、设备结果和真实服务端结果。
```

## 代码 Review

```text
请先阅读 docs/review.md，再按功能正确性、异常/并发、资源生命周期、架构边界、性能、安全隐私、
平台兼容性、测试覆盖和文档同步进行 Review。每个问题必须有唯一编号、优先级、可信度、文件/位置、
证据、影响、建议和验收标准；将可执行项同步到 docs/todo.md，无法验证的项目保持未完成。
```

## 完成任务

```text
完成后请运行 git diff --check，并汇总修改文件、验证命令、通过/失败/未执行原因、风险和后续事项。
如果是独立开发会话，新增 docs/sessions/YYYY-MM-DD.md；只有验收标准和验证都满足，才勾选 docs/todo.md。
```
