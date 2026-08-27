## Requirements

应用 SHALL 提供独立的“检查本客户端更新”入口，并通过固定仓库 `godmiracle/Coolapk` 的 GitHub Releases API 查询最新 Release；该请求 MUST 使用独立的无 Coolapk 凭据 HTTPS client，不得复用会注入 Cookie、设备 Token 或 Coolapk API 身份的 client。

#### Scenario: 查询最新 Release

- **WHEN** 用户打开本客户端更新页并点击检查
- **THEN** 应用请求 `https://api.github.com/repos/godmiracle/Coolapk/releases/latest`，展示当前版本和返回的 Release 版本/更新说明
- **AND** 不触发设备上第三方应用的批量更新检查

#### Scenario: GitHub 请求失败

- **WHEN** API 返回非 2xx、网络不可用或响应无法解析
- **THEN** 页面展示不含凭据和完整响应体的可读失败原因
- **AND** 不展示应用内下载或安装动作
- **AND** 页面仍提供打开固定 Release 网页的入口，交由系统浏览器处理网络受限场景

应用 SHALL 在更新页提供固定的 Release 网页入口，目标必须为 `https://github.com/godmiracle/Coolapk/releases/latest`。该入口 MUST 在检查成功、检查失败和尚未检查时可用；应用不得把 API 返回的任意 `html_url` 或资产 URL 直接交给外部应用。应用不负责 APK 下载、缓存或安装。

#### Scenario: 浏览器打开 Release 页面

- **WHEN** 用户点击“浏览器下载 Release（推荐）”
- **THEN** 应用使用 `ACTION_VIEW` 打开固定的 `https://github.com/godmiracle/Coolapk/releases/latest`
- **AND** 不携带 Coolapk Cookie、Token 或 `X-App-*` 请求身份
- **AND** 系统没有可用浏览器时页面显示可读错误

Release MUST 只接受非 draft、非 prerelease、可解析为 `v?major.minor.patch` 的稳定版本；应用 SHALL 按 semver 比较正式当前版本和 Release 版本。无法将当前 Git hash 解析为 semver 时，应用 MUST 将结果标识为“最新稳定版可用/需更新评估”，不得把 hash 按数字或字典序与 Release 比较。

#### Scenario: 正式版本已是最新

- **WHEN** 当前正式 `versionName` 与 latest Release 的 major/minor/patch 相同
- **THEN** 页面显示已是最新
- **AND** 页面不显示应用内下载按钮

#### Scenario: Debug 或 hash 版本检测到稳定版

- **WHEN** 当前 `versionName` 不是 semver 且 latest Release 是有效稳定版本
- **THEN** 页面显示检测到最新稳定版及其版本信息
- **AND** 用户可继续查看更新说明并打开固定 Release 网页

应用 SHALL 不包含本客户端更新专用的 APK 下载、缓存、FileProvider 传递、未知来源授权引导或系统安装器调用。

应用 SHALL 只在用户点击“检查更新”时请求 GitHub latest Release；打开本客户端更新页、页面重建或从浏览器返回时不得自动发起新的检查请求。用户可以再次点击按钮手动重试。

#### Scenario: 更新页打开不自动检查

- **WHEN** 用户从设置进入本客户端更新页
- **THEN** 页面展示当前版本和“检查更新”按钮
- **AND** 在用户点击按钮前不请求 GitHub API

#### Scenario: Debug 与 Release 包边界

- **WHEN** 用户在同一设备上安装 Debug 和正式 Release
- **THEN** Debug applicationId 为 `com.godmiracle.coolapk.debug`，正式 applicationId 为 `com.godmiracle.coolapk`
- **AND** 两者可以并存，Debug 不作为正式包的覆盖升级来源
- **AND** 正式包之间只有在包名相同、签名一致且新 `versionCode` 更高时才允许覆盖安装

应用 SHALL 移除无当前路由的“检查应用更新”批量链路，包括设置开关、设备应用枚举和批量 `/v6/apk/checkUpdate` 请求、批量更新列表及其入口；应用详情页现有的单个 APK 下载能力不属于本删除范围。

#### Scenario: 不再检查第三方应用更新

- **WHEN** 用户浏览当前主应用的首页、发现、关注或我的页面
- **THEN** 不枚举设备上的全部已安装应用以发起批量更新检查
- **AND** 设置页不显示“检查应用更新”开关
- **AND** 应用详情页仍可保留单个应用的详情和下载路径

设置页新增入口 MUST 使用 XML/ViewBinding Activity，保留系统返回、放大字体和深浅色主题可读性；该功能不得引入 Compose 或改变匿名只读产品范围。

#### Scenario: 连接真机验证

- **WHEN** 在 OPPO `PGEM10` Android API 36 上运行当前 Debug 包
- **THEN** 应用能打开更新页、请求 latest Release、识别 `v1.1.0` 并完成检测路径
- **AND** 更新页能把固定 Release 页面交给系统浏览器
- **AND** 验证报告明确记录 Debug 与正式 Release 的包名/签名边界，不能将系统拒绝替换描述为应用功能成功
