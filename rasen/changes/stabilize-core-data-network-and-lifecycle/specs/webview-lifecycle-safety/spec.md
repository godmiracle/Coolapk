## ADDED Requirements

### Requirement: WebView Activity follows normal process lifecycle

销毁 `WebViewActivity` MUST 只释放 WebView 和 Activity 资源，不得主动终止应用进程。

#### Scenario: User leaves a WebView page

- **WHEN** 用户按返回键离开 WebView 页面
- **THEN** WebView 被清理，主页面进程继续运行且可以再次打开页面

#### Scenario: WebView Activity is recreated

- **WHEN** 系统因旋转或配置变化销毁并重建 WebView Activity
- **THEN** 应用进程不被退出，Activity 可以按正常生命周期重新初始化

### Requirement: WebView lifecycle regression is verified

生命周期测试 MUST 验证 WebView Activity 的销毁不会调用进程退出路径。

#### Scenario: Instrumentation test closes the Activity

- **WHEN** instrumentation test 启动并关闭 WebView Activity
- **THEN** 测试进程和宿主 Activity 仍然存活
