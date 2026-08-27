## 1. 规划与网络边界

- [x] 1.1 增加 GitHub Release DTO、API service、独立 Hilt Retrofit/OkHttp client 和固定仓库端点。
- [x] 1.2 增加 semver 解析、latest Release 稳定状态校验和版本比较。
- [x] 1.3 保持现有 Coolapk credential interceptor 不进入 GitHub 请求，并补充网络边界测试。

## 2. 浏览器更新路径与清理

- [x] 2.1 提供固定 Release 网页入口，检测成功、失败和未检查时均可使用。
- [x] 2.2 移除本客户端更新专用的 APK 下载、缓存、SHA-256 下载校验和安装器状态。
- [x] 2.3 移除 `REQUEST_INSTALL_PACKAGES`、更新 FileProvider 路径、未知来源设置引导和系统 Package Installer Intent。
- [x] 2.4 移除无当前路由的第三方应用批量更新检查、列表、入口和对应 API/资源；保留应用详情单个 APK 下载。

## 3. UI 与入口

- [x] 3.1 移除“检查应用更新”开关和 `isCheckUpdate` 配置，保留“检查本客户端更新”入口。
- [x] 3.2 新增 XML/ViewBinding 更新 Activity/ViewModel，展示检查、更新说明和浏览器跳转状态。
- [x] 3.3 适配错误、取消、返回、深浅色和字体缩放状态。
- [x] 3.4 固定 GitHub Release 浏览器入口；API 受限时仍可交给系统浏览器下载。
- [x] 3.5 将客户端 GitHub 检查改为严格手动触发，进入页面和页面重建不自动请求。

## 4. Debug/Release 安装边界

- [x] 4.1 为 Debug 增加 `.debug` applicationId 后缀，保留正式包 applicationId 和 Release 签名配置。
- [x] 4.2 记录正式包覆盖安装要求：同包名、同签名和更高 `versionCode`；Debug 与正式包可并存。

## 5. 验证与交付

- [x] 5.1 补充/调整 JVM/Android instrumentation 测试并执行资源编译、Debug 构建、Unit Test、Instrumentation 和 Lint。
- [x] 5.2 安装到 OPPO `PGEM10`，验证客户端检查只有点击按钮才请求、固定 Release 浏览器入口和 Debug/Release 包边界。
- [x] 5.3 更新项目文档与 `docs/sessions/2026-08-27.md`，保存 Rasen evidence。
- [x] 5.4 完成 review-cycle、提交变更并报告 commit/设备/构建证据。
