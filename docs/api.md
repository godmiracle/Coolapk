# 外部服务与 API 记录

## 说明

本文件记录 `app/src/main/java/com/example/c001apk/logic/network/ApiService.kt` 和当前 `NetworkRepo` 的源码契约，便于排查接口漂移。它不是 Coolapk 官方 API 文档，也不是对线上服务稳定性的保证。

- 记录日期：2026-08-12
- 源码基线：`becc810`
- 主调用入口：`logic.repository.NetworkRepo`
- 旧调用入口：`logic.network.Network` + `logic.network.Repository`，当前静态搜索未发现 UI 调用
- 所有接口的真实可用性、权限和返回字段都必须通过真实网络请求或设备验收确认

## 1. 服务地址

| 服务 | Base URL | Retrofit Qualifier | 主要用途 |
|---|---|---|---|
| API1 | `https://api.coolapk.com/` | `@Api1Service` | 首页、动态详情、搜索、用户、应用、社交操作、消息、发布等 |
| API1（不跟随重定向） | `https://api.coolapk.com/` | `@Api1ServiceNoRedirect` | 读取应用下载接口的 `Location` |
| API2 | `https://api2.coolapk.com/` | `@Api2Service` | 动态评论、话题布局、用户资料等兼容接口 |
| Account | `https://account.coolapk.com/` | `@AccountService` | 登录页面、Cookie、验证码和短信登录参数 |

图片和网页会额外访问 Coolapk 图片域名、`m.coolapk.com`、网页跳转地址及 OSS 上传返回的 Endpoint/Callback URL。

三组 Base URL 已集中在 `NetworkEndpoints`，统一以 `/` 结尾；Hilt 网络模块和旧 `ApiServiceCreator` 共用同一组常量。仍需通过构建、单元测试和依赖网络注入的 Activity 启动完成运行时验证。

## 1.1 社区 API 参考（非官方）

本项目补充参考 [Coolapk-API-Collect](https://github.com/Coolapk-UWP/Coolapk-API-Collect) 及其[在线文档](https://coolapk-uwp.github.io/Coolapk-API-Collect/#/)。本次重点核对：[HTTP 首部](https://raw.githubusercontent.com/Coolapk-UWP/Coolapk-API-Collect/main/Coolapk-API/%E6%8E%88%E6%9D%83/HTTP%20%E9%A6%96%E9%83%A8.md)、[主页内容 V8](https://raw.githubusercontent.com/Coolapk-UWP/Coolapk-API-Collect/main/Coolapk-API/%E4%B8%BB%E9%A1%B5/%E4%B8%BB%E9%A1%B5%E5%86%85%E5%AE%B9%20V8.md) 和[动态详情](https://raw.githubusercontent.com/Coolapk-UWP/Coolapk-API-Collect/main/Coolapk-API/%E5%8A%A8%E6%80%81/%E5%8A%A8%E6%80%81%E8%AF%A6%E6%83%85.md)。该资料由社区维护，仓库声明采用 CC-BY-4.0 且仅用于学习和测试；本节只保留与当前浏览链路有关的摘要，不复制完整文档，也不把它视为 Coolapk 官方 API 契约。

本次核对日期：2026-08-12。上游 `main` 树快照：`ab1a5f0d29d28114b425a61998f0ff52e21c59ca`。上游内容可能继续变化，实际请求仍需设备验证。

| 社区资料中的接口 | 认证/必要参数 | 对当前项目的意义 |
|---|---|---|
| `https://api.coolapk.com/v6/main/indexV8` | `X-App-Token`；`page` 必填；`firstItem`、`lastItem` 可选 | 社区资料将首页 V8 归到 API1；当前项目已路由到 API1，并在模拟器收到 200 |
| `https://api.coolapk.com/v6/page` | `X-App-Token`；`url`、`page` 等分页参数 | 可作为首页其他 Tab 的候选接口；不能直接替换现有 Tab 参数 |
| `https://api.coolapk.com/v6/feed/detail` | `X-App-Token`；`id` 必填 | 社区资料将动态详情归到 API1；当前项目已路由到 API1，并在模拟器收到 200 |

### 请求身份对照

- 社区资料将 `X-Requested-With: XMLHttpRequest`、`X-App-Token`、`X-App-Id`、版本号和 API 版本列为 JSON/接口请求的重要请求头；当前 `AddCookiesInterceptor` 已发送这些字段。
- 社区资料的 V2 Token 示例使用整数 Unix 秒（等价于 `ToUnixTimeSeconds()`）。当前 `TokenDeviceUtils` 已改为 `System.currentTimeMillis() / 1000L`；自定义 Token 开关启用且值非空时，`AddCookiesInterceptor` 会使用该值覆盖自动生成值。
- 社区资料把登录 Cookie 中的 `uid`、`username`、`token` 标为必要、把 `SESSID` 标为非必要。该表不能单独证明匿名浏览必须登录，是否需要登录态必须用同一接口做匿名/登录对照；调试时不得记录真实值。

### 本次对照结论

在 `emulator-5554` 的 Android 16 模拟器上，采用 API1 Host、整数秒 Token，并保持默认 `13.4.1/2312121` 请求身份后：应用详情、首页 V8、动态详情和动态评论均收到 `200 OK`；UI 层级确认首页显示真实动态内容，点击后 `FeedActivity` 显示动态正文。此前的 `403` 由两个变量共同放大：首页/详情错误地走 API2，以及启动时把接口返回的线上应用版本写入第三方客户端的请求 Header。当前 `MainViewModel` 保持请求身份稳定，不再自动跟随线上应用版本。

需要区分两类验证：`X-App-Token` 已在请求日志中确认存在，且修正整数秒后首页/详情实测成功；Cookie、Token、设备码仍只在脱敏日志中记录，不能把日志分享给他人。线上边缘策略可能继续变化，因此仍需在真实设备上复验。

### 同类实现对照

参考 [HDYOU/c001apk](https://github.com/HDYOU/c001apk)（本次只读对照的快照为 `797656c4590b40e990ec8810442e1995e98d0da8`，2025-12-20）：该 fork 已将启动时自动更新远端应用版本的代码注释掉，支持“固定请求身份”的方向；但其快照仍使用 Float 时间戳 Token，并将首页/动态详情通过 API2 调用。因此本项目只吸收“不要自动改写版本 Header”的证据，不直接复制其旧 Token 和路由。

## 2. 请求身份

### API1/API2

`AddCookiesInterceptor` 统一加入：

- `User-Agent`
- `X-Requested-With: XMLHttpRequest`
- `X-Sdk-Int`、`X-Sdk-Locale`
- `X-App-Id`、`X-App-Token`、`X-App-Version`、`X-App-Code`
- `X-Api-Version`、`X-App-Device`、`X-Dark-Mode`
- `X-App-Channel`、`X-App-Mode`、`X-App-Supported`
- `Content-Type: application/x-www-form-urlencoded`

登录时 Cookie 由 `uid`、`username`、`token` 组成；未登录时发送 `CookieUtil.SESSID`。`X-App-Token` 由 `TokenDeviceUtils` 基于时间、设备码、MD5、Base64 和 BCrypt 计算。

### Account

`LoginCookiesInterceptor` 依赖 `CookieUtil` 的一次性 flag 来决定当前请求属于：预取登录页、读取登录参数、验证码、密码登录或短信登录。修改登录顺序、并发请求或复用 flag 时必须重新验证。

### 敏感信息

Cookie、Token、账号、密码、验证码、设备码、STS 密钥和 BODY 日志不能提交到仓库、Issue、截图或聊天记录。网络 BODY 日志默认关闭；只有显式 Debug 开关开启时才输出，并由 `NetworkLogging` 脱敏。

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
| `getAppsUpdate` | POST multipart | `/v6/apk/checkUpdate?coolmarket_beta=0` | 批量应用更新检查 |
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
2. `MainViewModel` 保持 `Constants`/`PrefManager` 中的稳定请求身份，不把远端应用详情版本写回 API 请求 Header。
3. 随后调用 `checkLoginInfo`，同步登录状态、用户摘要、会话 Cookie 和通知角标。
4. 已登录用户在 `MainActivity.onResume` 中按五分钟间隔调用 `checkCount`。

### 6.2 密码登录

1. `LoginActivity` 设置 `isPreGetLoginParam`，调用 `preGetLoginParam`。
2. 从 HTML 的 `<Body data-request-hash>` 读取 request hash，从 `Set-Cookie` 保存 `SESSID`。
3. 调用 `getLoginParam` 更新页面参数和 Cookie。
4. 需要时调用 `getCaptcha`，把图片解码为 Bitmap。
5. 提交 `tryLogin`；成功后从响应 Cookie 解析 UID、用户名和 Token，写入 `PrefManager`。
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
4. `PrefManager` 中的版本、设备、User-Agent、`CookieUtil.SESSID` 是否为空或过期。
5. 分页边界 `lastItem`、动态 ID、用户 ID、话题 ID 是否来自当前页面，而不是旧缓存。
6. 只有完成真实接口请求和页面操作后，才能把问题归因于 UI 或服务端。
