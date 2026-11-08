# 外部服务与 API 记录

## 说明

本文件记录 `app/src/main/java/com/godmiracle/coolapk/logic/network/ApiService.kt` 和当前 `NetworkRepo` 的源码契约，便于排查接口漂移。它不是 Coolapk 官方 API 文档，也不是对线上服务稳定性的保证。

- 记录日期：2026-08-13
- Git HEAD 基线：`d41d41d`；本 change 的实现和文档仍以当前工作树为准
- 主调用入口：`logic.repository.NetworkRepo`
- 旧调用入口：`logic.network.Network` + `logic.network.Repository`，当前静态搜索未发现 UI 调用
- 所有接口的真实可用性、权限和返回字段都必须通过真实网络请求或设备验收确认
- 本次文档同步包含源码/测试核对、离线 Gradle 门禁和 OPPO `PGEM10` 真机的 GitHub latest API/Release 浏览器入口验证；下文带日期的其他设备/接口结果仍按各自会话记录区分，不能扩展为完整业务验收。

### 当前匿名只读边界（2026-08-26）

当前源码只保留 API1/API2 的匿名内容读取和本地功能。登录、消息、动态发布、回复编辑、远程点赞/关注、删除、举报、验证码和 OSS 上传已移除；Feed 评论/楼中楼仍可通过只读接口展示。下文历史章节中的 Account、写入接口和登录流程仅用于追溯旧实现，不代表当前源码能力。

## 1. 服务地址

| 服务 | Base URL | Retrofit Qualifier | 主要用途 |
|---|---|---|---|
| API1 | `https://api.coolapk.com/` | `@Api1Service` | 首页、动态详情、搜索、用户和应用等匿名只读接口 |
| API1（不跟随重定向） | `https://api.coolapk.com/` | `@Api1ServiceNoRedirect` | 读取应用下载接口的 `Location` |
| API2 | `https://api2.coolapk.com/` | `@Api2Service` | 动态评论、话题布局、用户资料等兼容接口 |
| GitHub Releases | `https://api.github.com/` | `@GitHubReleaseService` | 当前仓库 latest Release 查询；不携带 Coolapk 凭据 |

图片和网页会额外访问 Coolapk 图片域名、`m.coolapk.com`、网页跳转地址及 OSS 上传返回的 Endpoint/Callback URL。

两组 API Base URL 已集中在 `NetworkEndpoints`，统一以 `/` 结尾；Hilt 网络模块和旧 `ApiServiceCreator` 共用同一组常量。API1/API2 客户端使用最终网络边界；是否能访问实时服务仍需单独通过构建、设备和真实请求验证。

### 请求身份 Host 边界

`NetworkEndpoints.isTrusted` 只接受 HTTPS 且 Host 为 `api.coolapk.com` 或 `api2.coolapk.com`。API1/API2 的 `AddCookiesInterceptor` 仅在该条件满足时添加匿名请求身份 Header 和会话 Cookie。

三个 Coolapk 客户端都在 OkHttp 的最终 network interceptor 中挂载 `NetworkCredentialBoundaryInterceptor`。因此 Retrofit `@Url` 解析出的外部/非 HTTPS URL，以及受信任请求跨 Host 重定向后的新请求，都会移除完整请求身份 Header 集合；请求本身是否继续作为无身份公共请求由网络层决定。`@Api1ServiceNoRedirect` 仍不跟随重定向，下载接口只在 Repository 显式允许时读取 3xx `Location`。GitHub client 使用独立的无凭据边界，不复用上述身份拦截器。

### GitHub Releases 客户端更新 API

客户端更新使用与 Coolapk 完全隔离的 Retrofit/OkHttp client，固定请求：

```text
GET https://api.github.com/repos/godmiracle/Coolapk/releases/latest
```

请求只带 GitHub API 所需的 `Accept: application/vnd.github+json`、`X-GitHub-Api-Version: 2022-11-28` 和固定 User-Agent，不带 Cookie、`X-App-Token`、`X-App-Device` 或其他 Coolapk 请求身份；client 固定使用 HTTP/1.1，并关闭重定向，避免把 API 请求带到未知 Host。

Repository 只校验 Release 不是 draft/prerelease 且 Tag 可解析为 `v?major.minor.patch`，然后按 semver 与当前版本比较。应用不读取或下载 API 返回的 APK URL、大小或 digest，不创建更新缓存，也不声明 `REQUEST_INSTALL_PACKAGES`。检测到更新后，页面只把固定的 `https://github.com/godmiracle/Coolapk/releases/latest` 交给系统浏览器；浏览器负责网页访问、APK 下载和后续系统安装确认。应用不把 API 返回的任意 `html_url` 或资产 URL 直接传播给外部应用。

更新页只在用户点击“检查更新”时调用 latest API；页面打开、重建或从浏览器返回不会自动发起请求。旧的“检查应用更新”批量第三方应用更新链路、设备应用枚举和 `/v6/apk/checkUpdate` 请求已移除，但应用详情的 `getAppDownloadLink` 单个下载路径保留。

GitHub API 的非 2xx、解析失败或 Release 元数据不符合约束时显示可读错误，并保留固定 Release 网页入口。实时 API 可受网络、GitHub 限流或国内网络策略影响，构建和本地 MockWebServer 测试不能替代设备验收。

## 1.1 社区 API 参考（非官方）

本项目补充参考 [Coolapk-API-Collect](https://github.com/Coolapk-UWP/Coolapk-API-Collect) 及其[在线文档](https://coolapk-uwp.github.io/Coolapk-API-Collect/#/)。本次重点核对：[HTTP 首部](https://raw.githubusercontent.com/Coolapk-UWP/Coolapk-API-Collect/main/Coolapk-API/%E6%8E%88%E6%9D%83/HTTP%20%E9%A6%96%E9%83%A8.md)、[主页内容 V8](https://raw.githubusercontent.com/Coolapk-UWP/Coolapk-API-Collect/main/Coolapk-API/%E4%B8%BB%E9%A1%B5/%E4%B8%BB%E9%A1%B5%E5%86%85%E5%AE%B9%20V8.md) 和[动态详情](https://raw.githubusercontent.com/Coolapk-UWP/Coolapk-API-Collect/main/Coolapk-API/%E5%8A%A8%E6%80%81/%E5%8A%A8%E6%80%81%E8%AF%A6%E6%83%85.md)。该资料由社区维护，仓库声明采用 CC-BY-4.0 且仅用于学习和测试；本节只保留与当前浏览链路有关的摘要，不复制完整文档，也不把它视为 Coolapk 官方 API 契约。

本次核对日期：2026-08-12。上游 `main` 树快照：`ab1a5f0d29d28114b425a61998f0ff52e21c59ca`。上游内容可能继续变化，实际请求仍需设备验证。

| 社区资料中的接口 | 认证/必要参数 | 对当前项目的意义 |
|---|---|---|
| `https://api.coolapk.com/v6/main/indexV8` | `X-App-Token`；`page` 必填；`firstItem`、`lastItem` 可选 | 社区资料将首页 V8 归到 API1；项目源码已路由到 API1，2026-08-12 的历史记录曾在模拟器收到 200 |
| `https://api.coolapk.com/v6/page` | `X-App-Token`；`url`、`page` 等分页参数 | 可作为首页其他 Tab 的候选接口；不能直接替换现有 Tab 参数 |
| `https://api.coolapk.com/v6/feed/detail` | `X-App-Token`；`id` 必填 | 社区资料将动态详情归到 API1；项目源码已路由到 API1，2026-08-12 的历史记录曾在模拟器收到 200 |

### 请求身份对照

- 社区资料列出的 `X-Requested-With`、`X-App-Token`、`X-App-Id`、版本号和 API 版本等字段仍可用于理解服务端要求，但当前客户端身份已按 `XiaoMengXinX/c001apk_next` 的 iOS 实现切换；下节记录当前源码实际发送的字段。
- 旧 Android V2 Token 和 `customToken/xAppToken` 覆盖行为只作为历史实现记录；当前 API 请求不再调用 `TokenDeviceUtils` 生成 Android Token。
- 社区资料把登录 Cookie 中的 `uid`、`username`、`token` 标为必要、把 `SESSID` 标为非必要。该表不能单独证明匿名浏览必须登录，是否需要登录态必须用同一接口做匿名/登录对照；调试时不得记录真实值。

### 历史设备对照记录（非本次验证）

在 2026-08-12 的仓库会话记录中，`emulator-5554` 的 Android 16 模拟器采用 API1 Host、整数秒 Token，并保持默认 `13.4.1/2312121` 请求身份后：应用详情、首页 V8、动态详情和动态评论收到 `200 OK`，UI 层级确认首页动态内容和 `FeedActivity` 正文可见。此前的 `403` 由两个变量共同放大：首页/详情错误地走 API2，以及启动时把接口返回的线上应用版本写入第三方客户端的请求 Header。该段只保留为历史证据，不能当作当前实时 API 保证。

需要区分两类验证：源码可以证明当前请求身份格式和 Host 边界规则；2026-08 的历史设备记录验证的是旧 Android 身份，不能作为当前 iOS 身份的成功证据。当前 iOS 身份尚未在本轮通过构建、真实设备或实时接口复验。Cookie、Token、设备码仍只在脱敏日志中记录，不能把日志分享给他人。线上边缘策略可能继续变化。

### 同类实现对照

参考 [HDYOU/c001apk](https://github.com/HDYOU/c001apk)（本次只读对照的快照为 `797656c4590b40e990ec8810442e1995e98d0da8`，2025-12-20）：该 fork 已将启动时自动更新远端应用版本的代码注释掉，支持“固定请求身份”的方向；但其快照仍使用 Float 时间戳 Token，并将首页/动态详情通过 API2 调用。因此本项目只吸收“不要自动改写版本 Header”的证据，不直接复制其旧 Token 和路由。

## 2. 请求身份

### API1/API2

`AddCookiesInterceptor` 只对 `api.coolapk.com` 和 `api2.coolapk.com` 的 HTTPS 请求应用 iOS 客户端身份。实现参考 [XiaoMengXinX/c001apk_next](https://github.com/XiaoMengXinX/c001apk_next)，对照快照为 `d0c5843c8e366f92fe48ee5c2de8f0863c2fb052`（2026-11-09）。当前发送：

- `Accept: */*`、`Accept-Language`
- iPhone `User-Agent`（`iCoolMarket/16.6.1-2610151`）
- `X-Requested-With: XMLHttpRequest`
- `X-Sdk-Int`（模拟 iOS 版本）、`X-Sdk-Locale: zh-HK`
- `X-App-Id: com.coolapk.app`、`X-App-Version: 16.6.1`、`X-App-Code: 2610151`、`X-Api-Version: 16`
- `X-App-Device`、`X-App-Token`

`IosDeviceUtils` 为每个安装实例生成并持久化稳定 UUID 和一组 iPhone 型号档案，存于 `credentials.xml`；设置页可编辑档案或重新随机生成档案，重新生成不会改变 UUID。`X-App-Device` 按上游格式拼接设备字段后进行标准 Base64 编码、反转并去除开头的填充符。`X-App-Token` 按当前 Unix 秒、UUID 和 Bundle ID 通过上游的 MD5/Base64 公式生成，每秒变化。请求仅保留匿名 `CookieUtil.SESSID`，不发送账号 Cookie、CDID 或登录状态。

请求不再附加 Android 身份的 `X-Dark-Mode`、`X-App-Channel`、`X-App-Mode`、`X-App-Supported`；无请求体时移除 `Content-Type`，有请求体时由请求本身设置。Header 只会在两个 Coolapk HTTPS API Host 保留，最终 Host 变化后由 network boundary 清理。

Coolapk 图片请求和以 Coolapk 域名开头的 WebView 页面会使用同一 iOS User-Agent；非 Coolapk 图片和以第三方域名开头的 WebView 页面保留原有 User-Agent 行为。

### 敏感信息

Cookie、Token、设备 ID、设备档案、STS 密钥和 BODY 日志不能提交到仓库、Issue、截图或聊天记录。当前 iOS 设备 UUID/档案和 WebView 使用的 `SZLMID` 存在 `credentials.xml`；普通 UI 偏好仍在 `settings.xml`；两套 Android 备份规则均排除 `credentials.xml`。网络 BODY 日志默认关闭；只有显式 Debug 开关开启时才输出，并由 `NetworkLogging` 脱敏。

### NetworkRepo 的错误与取消语义

`NetworkRepo` 使用 `suspendCancellableCoroutine` 适配 Retrofit `Call`：协程取消会调用底层 `Call.cancel()`，`CancellationException` 继续向上抛出，`Flow` 不发出普通失败结果。非 2xx 响应统一成为带状态码的 `HttpException`；需要实体的 2xx 空 body 成为 `EmptyResponseBodyException`；其他 transport failure 作为保留原始原因的 `Result.failure` 发出。只有 `getAppDownloadLink` 在 no-redirect 客户端中显式允许 3xx 并读取 `Location`。

## 3. API1 接口

### 内容、搜索、用户和应用

| Retrofit 方法 | HTTP | 路径/形式 | 作用 |
|---|---|---|---|
| `getHomeFeed` | GET | `/v6/main/indexV8` | 首页 feed；支持首次启动、安装时间和分页边界 |
| `getFeedContent` | GET | `/v6/feed/detail` | 动态详情 |
| `getSearch` | GET | `/v6/search` | 动态、应用、用户、话题等搜索；带类型、排序和分页参数 |
| `getReply2Reply` | GET | `/v6/feed/replyList?...` | 评论的楼中楼 |
| `getUserSpace` | GET | `/v6/user/space` | 用户空间摘要 |
| `getUserFeed` | GET | `/v6/user/feedList?...` | 用户动态列表 |
| `getAppInfo` | GET | `/v6/apk/detail` | 应用详情 |
| `getAppDownloadLink` | POST | `/v6/apk/download?extra=` | 获取下载重定向地址；使用不跟随重定向客户端 |
| `getProductLayout` | GET | `/v6/product/detail` | 数码产品详情布局 |
| `getFollowList` | GET | `@Url` | 用户/关注/粉丝等列表，调用者传入完整相对或绝对 URL |
| `getDataList` | GET | `/v6/page/dataList` | 通用页面数据列表 |
| `getDyhDetail` | GET | `/v6/dyhArticle/list` | 订阅号/关系内容列表 |
| `getProductList` | GET | `/v6/product/categoryList` | 产品分类或列表 |
| `getCollectionList` | GET | `@Url` | 收藏集合列表 |
| `getSearchTag` | GET | `/v6/feed/searchTag` | 发布/搜索时的标签建议 |
| `loadShareUrl` | GET | `/v6/feed/loadShareUrl` | 解析分享链接 |

### 社交读写

| Retrofit 方法 | HTTP | 路径/形式 | 作用 |
|---|---|---|---|
| `postLikeFeed` | POST | `@Url` | 动态点赞/取消点赞 |
| `postLikeReply` | POST | `@Url` | 评论点赞/取消点赞 |
| `postReply` | POST form | `v6/feed/reply` | 发布回复 |
| `postFollowUnFollow` | POST | `@Url` | 关注/取消关注用户 |
| `postCreateFeed` | POST form | `/v6/feed/createFeed` | 发布动态 |
| `postRequestValidate` | POST form | `/v6/account/requestValidate` | 发布/回复图形验证码校验 |
| `getVoteComment` | GET | `/v6/vote/commentList` | 投票选项评论 |
| `getAnswerList` | GET | `/v6/question/answerList` | 问答答案列表 |
| `postDelete` | POST | `@Url` | 删除动态、评论或通知 |
| `postFollow` | POST form | `/v6/product/changeFollowStatus` | 关注/取消关注产品 |
| `getFollow` | GET | `@Url` | 获取产品/话题关注状态 |
| `postOSSUploadPrepare` | POST form | `/v6/upload/ossUploadPrepare` | 获取图片 OSS 上传临时信息 |

### 消息和登录态检查

| Retrofit 方法 | HTTP | 路径/形式 | 作用 |
|---|---|---|---|
| `checkLoginInfo` | GET | `/v6/account/checkLoginInfo` | 检查登录态、用户摘要和通知数量 |
| `getMessage` | GET | `@Url` | 消息分类列表 |
| `checkCount` | GET | `/v6/notification/checkCount` | 刷新通知角标 |

## 4. API2 接口

| Retrofit 方法 | HTTP | 路径 | 作用 |
|---|---|---|---|
| `getFeedContentReply` | GET | `/v6/feed/replyList` | 动态评论列表，带讨论模式和过滤参数 |
| `getTopicLayout` | GET | `/v6/topic/newTagDetail` | 话题布局 |
| `getProfile` | GET | `/v6/user/profile` | 登录后或用户详情的资料补充 |

## 5. Account 接口

| Retrofit 方法 | HTTP | 路径 | 作用 |
|---|---|---|---|
| `preGetLoginParam` | GET | `/auth/login/` | 预取登录页、会话 Cookie 和 request hash |
| `getLoginParam` | GET | `/auth/loginByCoolApk` | 获取密码登录参数/页面信息 |
| `tryLogin` | POST form | `/auth/loginByCoolApk` | 提交账号、密码、验证码和 request hash |
| `getCaptcha` | GET | `@Url` | 获取登录图形验证码 |
| `getValidateCaptcha` | GET | `@Url` | 获取发布/回复图形验证码 |
| `getSmsLoginParam` | GET | `/auth/login?type=mobile` | 获取短信登录页面参数；当前 UI 接通状态待确认 |
| `getSmsToken` | POST form | `/auth/login?type=mobile` | 获取短信验证码；当前 UI 接通状态待确认 |

## 6. 关键业务流程

### 6.1 应用启动

1. `MainActivity` 首次创建时调用 `getAppInfo("com.coolapk.market")`。
2. `IosDeviceUtils` 加载或创建持久化的匿名 iOS UUID/设备档案；`AddCookiesInterceptor` 按每次请求时间生成 iOS 请求身份，不把远端应用详情版本写回 API 请求 Header。
3. 随后调用 `checkLoginInfo`，同步登录状态、用户摘要、会话 Cookie 和通知角标。
4. 已登录用户在 `MainActivity.onResume` 中按五分钟间隔调用 `checkCount`。

### 6.2 密码登录

1. `LoginActivity` 设置 `isPreGetLoginParam`，调用 `preGetLoginParam`。
2. 从 HTML 的 `<Body data-request-hash>` 读取 request hash，从 `Set-Cookie` 保存 `SESSID`。
3. 调用 `getLoginParam` 更新页面参数和 Cookie。
4. 需要时调用 `getCaptcha`，把图片解码为 Bitmap。
5. 提交 `tryLogin`；成功后从响应 Cookie 解析 UID、用户名和 Token，通过 `PrefManager` 写入 `credentials.xml` 对应的 SharedPreferences。
6. 再调用 `getProfile` 写入头像、等级和经验，重建主界面。

### 6.3 动态/回复发布与图片

1. 页面准备动态或回复表单，必要时调用 `postRequestValidate`。
2. 图片先调用 `postOSSUploadPrepare`，获得临时 STS 凭据、Bucket、Endpoint、文件名和回调信息。
3. `ossUpload` 使用阿里云 OSS SDK 直接上传到临时地址，成功后再提交动态/回复内容。
4. 如果服务端返回 `err_request_captcha` 或图形验证码错误，页面重新获取验证码。

### 6.4 下载链接

1. `getAppDownloadLink` 使用 `@Api1ServiceNoRedirect`。
2. `NetworkRepo` 从 HTTP `Location` Header 取出真实下载 URL，而不是取响应体。
3. 后续页面可能交给系统浏览器、DownloadManager 或安装流程；需要设备验证未知来源、文件名和权限行为。

## 7. 兼容性排查顺序

遇到“列表空白”“登录失败”“点赞失败”或“图片上传失败”时，按以下顺序记录：

1. 当前构建的 Git hash、Android 版本、网络类型和是否已登录。
2. 请求最终 URL、HTTP 状态码、服务端 message 和是否发生重定向；日志必须脱敏。
3. API1/API2/Account 是否选对，是否误用了旧 `Network` 路径。
4. 设置页中的 iOS 设备档案、匿名 `CookieUtil.SESSID` 是否符合当前接口要求；不得在诊断记录中保留完整 Token、UUID 或 Cookie。
5. 分页边界 `lastItem`、动态 ID、用户 ID、话题 ID 是否来自当前页面，而不是旧缓存。
6. 只有完成真实接口请求和页面操作后，才能把问题归因于 UI 或服务端。
