## Context

应用使用 Kotlin、XML Layout、ViewBinding/Data Binding、Fragment、ViewModel、LiveData/Flow、Hilt、Retrofit 和 OkHttp。当前 `NetworkModule` 的 Coolapk 客户端会注入 `AddCookiesInterceptor`、API 身份和网络日志；这些请求头不应发送给 GitHub。现有 `FileProvider` 只服务图片分享，`Utils.downloadApk` 继续服务应用详情页的单个 APK 下载，不复用为本客户端更新路径。旧的批量第三方应用更新页面没有当前路由，将随本 change 清理。

当前发布流程为 `v*` Tag 创建 GitHub Release，Release APK 文件名形如 `c001apk_v1.1.0.463.apk`。开发 Debug 包的 `versionName` 是 Git 短 hash，正式 Tag 构建的 `versionName` 是 semver Tag；因此更新判断必须同时支持“正式版本比较”和“开发包提示最新稳定版”。

## Goals / Non-Goals

**Goals:**

- 通过独立、无凭据的 GitHub API 客户端查询仓库最新稳定 Release。
- 在 XML/ViewBinding 设置入口中提供检查、版本说明和固定 Release 浏览器跳转。
- 对 Release 稳定状态和版本格式做 fail-closed 校验，不将 API 返回的任意下载地址传播给外部应用。
- 保持错误可解释性，并让浏览器/Android 系统负责用户主动下载后的安装确认。

**Non-Goals:**

- 不引入 Compose、AndroidLiquidGlass、WorkManager、后台自动更新或通知推送。
- 不把 GitHub Token、Coolapk Cookie 或设备请求身份发送到 GitHub。
- 不在应用内下载、缓存或安装本客户端 APK，不申请 `REQUEST_INSTALL_PACKAGES`。
- 不保留无入口的批量第三方应用更新检查、列表和批量下载页面；应用详情的单个 APK 下载不在本次删除范围。
- 不修改登录/消息/发帖/回复范围或 Release CI 签名流程。

## Decisions

### 1. 独立 GitHub Retrofit/OkHttp client

新增 `GitHubReleaseApiService` 和独立的 Hilt Qualifier。client 只设置 GitHub API 所需的 `Accept`、API 版本和固定 User-Agent，关闭重定向，不挂载 `AddCookiesInterceptor`、Coolapk credential boundary 或账号状态。API Base URL 固定为 `https://api.github.com/`，仓库路径固定为 `repos/godmiracle/Coolapk/releases/latest`。

### 2. 只接受稳定 Release 元数据

Release 必须不是 draft/prerelease，Tag 必须解析为 `v?major.minor.patch`。应用不依赖资产列表、不读取 `browser_download_url`，因为下载由固定网页和系统浏览器完成。

### 3. 版本判断不把 Git hash 当作 semver

正式版本使用解析后的 major/minor/patch 比较；正式包同版本视为已是最新。当前版本名无法解析时显示“检测到最新稳定版”，避免把 Git hash 与 Release 版本错误排序。正式包升级由 Android 系统检查包名、签名和 `longVersionCode`；应用不在更新流程中预校验或安装 APK。

### 4. 固定 Release 网页交给系统浏览器

更新页提供“浏览器下载 Release（推荐）”入口，始终指向固定的 `https://github.com/godmiracle/Coolapk/releases/latest`，不把 API 返回的任意 `html_url` 或资产 URL 直接交给外部应用。该入口在检测成功、检测失败和初始状态均可用；应用不保留应用内直连下载路径，用户可使用系统浏览器、代理或其他已配置的网络路径完成下载。

GitHub client 强制 HTTP/1.1 并关闭重定向，避免部分国内网络或中间设备对 HTTP/2 的协商兼容问题和 API Host 漂移；这只改善 API 检查兼容性，不改变固定网页入口。

### 5. Debug 与 Release 分离

Debug 构建使用 `applicationIdSuffix = ".debug"` 和 Debug 签名，形成 `com.godmiracle.coolapk.debug`；正式构建保持 `com.godmiracle.coolapk` 和 Release 签名。两个变体可并存安装。旧版同包名 Debug 包迁移到新配置时需要用户卸载一次；正式包之间的覆盖安装仍必须满足包名相同、签名一致和新 `versionCode` 更高。

### 6. 更新检查全部采用显式触发

客户端更新页只在用户点击“检查更新”时请求 GitHub latest Release。页面打开时不自动请求，用户可以在需要时手动检查或重试；固定 Release 浏览器入口仍可在未检查或检查失败状态使用。

### 7. 移除不可达的批量第三方应用更新

当前 `MainActivity`、`HomeFragment` 和 `DiscoverFragment` 均不再挂载 `AppListFragment`，`HomeViewModel` 还会删除/过滤旧“应用”Tab。因此删除 `isCheckUpdate` 设置、`AppList*`、`AppUpdate*`、批量更新 DTO/API 包装和对应资源不会影响当前可达页面。保留 `ApiService.getAppDownloadLink`、`NetworkRepo.getAppDownloadLink` 和 `Utils.downloadApk`，因为应用详情页仍使用单个下载能力。

## Data Flow

```text
SettingsPreferenceFragment
  -> ClientUpdateActivity (XML/ViewBinding)
  -> ClientUpdateViewModel
  -> GitHubReleaseRepository
  -> GitHubReleaseApiService (no Coolapk credentials)
  -> Release metadata validation + semver comparison
  -> fixed GitHub Release page -> system browser download/install
```

## Error Handling

- HTTP 403/404/5xx 映射为包含状态码的用户可读错误，不显示响应体或请求身份。
- 网络异常显示连接失败；Release 草稿、预发布或版本格式不合法时显示明确原因。
- API 检查失败时保留固定 Release 网页入口；浏览器路径不读取、不传播 API 响应中的任意下载地址。
- 协程取消继续向上抛出；Activity 销毁不持有 View 引用。
- 浏览器没有可用处理器时显示可读错误；下载后的包名、签名、版本和未知来源策略由浏览器/Android 系统处理。

## Testing

- JVM 单元测试覆盖 semver、hash 版本、稳定 Release 过滤和 HTTP GitHub client 的无凭据请求边界。
- Android instrumentation 覆盖现有页面/数据/生命周期回归；不创建更新 APK、不启动更新安装器。
- JVM 测试覆盖 GitHub 元数据、固定入口和网络边界；`ClientUpdateActivity` 的显式点击绑定与更新页初始 Idle 状态通过源码检查和 PGEM10 真机验证，Android instrumentation 继续覆盖现有页面/数据/生命周期回归，并确认旧批量更新入口未被重新挂载。
- 真机验证覆盖设置入口、点击“检查更新”后的 latest API 请求、当前 Debug 检测到 `v1.1.0`、固定 Release 页面跳转和 Debug/Release 包名签名边界。不执行登录、消息、发帖、回复编辑或其他已移出匿名范围的测试。
