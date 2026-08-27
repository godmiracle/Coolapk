## Why

当前设置同时存在“检查应用更新”和“检查本客户端更新”。前者原本用于枚举设备上的第三方应用并批量检查更新，但当前应用列表入口已经移除，相关设置和代码成为不可达的遗留链路；后者用于检查本项目 GitHub Release。

需要补充一个匿名、只读的本客户端更新流程：从本项目 GitHub Releases API 获取最新稳定版，展示版本与更新说明，然后把固定 Release 页面交给系统浏览器，由用户自行下载和安装。应用不负责 APK 下载、缓存或安装，也不能把 Coolapk 请求凭据带到 GitHub。流程不能恢复登录、发帖、回复或其他远程写入能力。

## What Changes

- 新增独立的 GitHub Releases API 客户端，请求 `godmiracle/Coolapk` 的 latest Release；使用不带 Coolapk 凭据的独立 HTTPS Retrofit/OkHttp 配置，并在客户端固定使用 HTTP/1.1 提高部分网络环境下的兼容性。
- 在设置页保留“检查本客户端更新”入口，展示当前版本、最新稳定版、发布日期/更新说明和检查失败原因；删除无实际入口的“检查应用更新”设置和批量第三方应用更新链路。
- 客户端 GitHub Release 检查严格由用户点击“检查更新”触发；进入更新页不自动发起网络请求。
- 提供固定的 GitHub Release 网页入口：尚未检查、检测成功或检测失败时都允许用户交给系统浏览器下载，适配国内网络访问 GitHub API 不稳定的场景。
- 只接受非草稿、非预发布且 semver 合法的 Release；应用不读取或传播 API 返回的资产下载 URL。
- 删除本客户端更新专用的 APK 流式下载、私有缓存、SHA-256 下载校验、FileProvider 更新路径、未知来源授权引导和系统安装器 Intent。
- 为 Debug 构建增加独立的 `.debug` applicationId 后缀；Debug 与正式 Release 使用不同包名/签名并存安装，正式包升级继续遵守同包名、同签名和更高 `versionCode`。
- 增加版本解析、Release 元数据校验、固定浏览器 URL、网络边界和更新 UI 的测试，并记录真机验证中 Debug 包不能覆盖正式签名包的边界。

## Capabilities

### New Capabilities

- `github-release-update`: GitHub latest Release 查询、稳定版本比较和固定 Release 浏览器跳转。

### Modified Capabilities

- 移除无当前路由的 Coolapk 第三方应用批量更新能力；应用详情页的单个 APK 下载能力保持不变。

## Impact

- 影响 `app` 模块的 Hilt 网络注入、GitHub DTO/Repository、版本校验工具、设置页 XML/ViewBinding Activity、Debug applicationId 配置和更新相关清理。
- 移除客户端更新不需要的 `REQUEST_INSTALL_PACKAGES`、更新缓存路径和安装器代码；既有图片分享 FileProvider 继续保留。
- 不影响匿名 Coolapk API 的请求身份、Room schema、现有登录/消息/发帖/回复已移除的产品边界、LiquidGlass 页面 Chrome 或应用详情页的单个 APK 下载能力；仅删除无入口的批量第三方应用更新列表。
