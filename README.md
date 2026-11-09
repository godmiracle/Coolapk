# 绿友

绿友是一款基于 Kotlin/Android 的第三方社区客户端，面向内容浏览、数码信息和本地管理。项目基于 Coolapk 相关页面/API 形态进行学习性实现，当前用于个人学习、实验和测试，不是 Coolapk 官方客户端，也不提供独立后端。

项目接口和服务端行为依赖外部服务，可能随时变化；下方能力表示当前源码覆盖范围，不等同于线上服务长期稳定或完整业务验收。

## 项目声明

- 本项目仅用于个人学习、测试和 Android 客户端研究，请勿滥用。
- 使用时不要公开 Cookie、Token、设备参数或调试日志；当前应用不提供账号登录。
- 第三方代码和资源应遵守各自许可证；项目根目录的 [`LICENSE.md`](LICENSE.md) 和源码中的来源声明均需一并阅读。
- 当前仓库由 [godmiracle](https://github.com/godmiracle) 维护，源码仓库为 [godmiracle/Coolapk](https://github.com/godmiracle/Coolapk)。

## 主要能力

- 首页、关注、发现、我的和搜索等主要浏览入口。
- 动态列表与详情、评论和楼中楼、投票、问答、文章、图文及酷图内容。
- 用户、话题、数码和应用详情，以及本地关注、分享、深链和富文本/表情展示。
- 本地收藏、浏览历史、关注记录、首页 Tab、搜索历史和黑名单等能力。
- 主题、深色模式、字体、图片质量和缓存等常用设置。
- 在设置中检查当前仓库的 GitHub Releases；检测到新版本后由用户通过系统浏览器打开固定 Release 页面，自行下载并安装。

当前产品范围为匿名只读浏览及本地功能；账号登录、消息、动态发布、回复、远程点赞/关注、图片上传等写入或账号能力已移除。旧的“检查应用更新”批量第三方应用更新链路也已移除，应用详情页仍保留单个应用的下载路径。客户端更新仅在用户主动触发时访问固定的 `godmiracle/Coolapk` GitHub Releases，不携带 Coolapk 凭据；应用不在内部下载或安装 APK，只把固定 Release 页面交给系统浏览器。

## 技术栈

- Kotlin、Android XML Layout、ViewBinding/Data Binding、Fragment、RecyclerView 和 ViewPager2
- 主导航使用 Jetpack Compose 与 Miuix Blur 的液态玻璃组件
- ViewModel、LiveData/Flow、协程和 Hilt
- Retrofit、OkHttp、Gson
- Room、SharedPreferences
- Glide、Sketch、Mojito 及本地图片加载适配模块

## 仓库结构

```text
.
├── app/                  # 主 Android 应用
├── mojito/               # 图片全屏预览库
├── GlideImageLoader/     # Mojito 的 Glide/OkHttp 适配
├── SketchImageViewLoader/ # Mojito 的 Sketch 适配
├── docs/                 # 项目背景、架构、接口和开发文档
└── gradle/               # Gradle Wrapper 和版本目录
```

主源码位于 `app/src/main`。

## 构建与运行

环境要求：Android SDK Platform 37.0、Android Gradle Plugin 9.1.1、Gradle Wrapper 9.3.1，以及 Java 17+；应用最低支持 Android 13（API 33），targetSdk 为 36。首次构建需要访问公共 Maven 仓库。

```bash
# 构建 Debug APK
bash gradlew :app:assembleDebug

# 安装到已连接设备
bash gradlew :app:installDebug

# 执行单元测试和 Lint
bash gradlew :app:testDebugUnitTest :app:lintDebug
```

Release 构建需要本地签名配置，详细步骤见 [`docs/development.md`](docs/development.md)。

## GitHub Actions 发布

推送形如 `v1.0.0` 的 Tag 后，GitHub Actions 会使用仓库 Secrets 构建签名版 Release，并自动创建同名 GitHub Release，APK 内部 `versionName` 也会固定为该 Tag（例如 `v1.0.0`），同时上传 APK 和 `SHA256SUMS`。需要在仓库的 `Settings > Secrets and variables > Actions` 中配置：

- `SIGN_KEYSTORE_BASE64`：Release keystore 的 Base64 内容
- `KEYSTORE_PASSWORD`：keystore 密码
- `KEY_ALIAS`：签名别名
- `KEY_PASSWORD`：签名别名密码

```bash
git tag v1.0.0
git push origin v1.0.0
```

不要把 keystore、`local.properties` 或上述密码提交到仓库。首次发布前应在 Actions 页面确认构建成功，并在 Release 页面核对 APK 签名和 `SHA256SUMS`。

## 文档

- [`docs/context.md`](docs/context.md)：项目背景、范围、状态和约束
- [`docs/architecture.md`](docs/architecture.md)：模块边界、调用链和数据流
- [`docs/api.md`](docs/api.md)：外部服务和请求流程
- [`docs/development.md`](docs/development.md)：构建、签名、测试和验收
- [`docs/decisions.md`](docs/decisions.md)：技术决策和维护约束
- [`docs/todo.md`](docs/todo.md)：待办事项和验收标准
- [`docs/changelog.md`](docs/changelog.md)：可追踪变更记录

## 来源与许可证

- `TokenDeviceUtils` 部分实现参考 [CoolbbsYou](https://github.com/WaitFme/CoolbbsYou)。
- 主导航基于 [KernelSU](https://github.com/tiann/KernelSU) 底栏源码和 Miuix Blur；上游版本、来源与许可证见 [`third_party/licenses/NOTICE.md`](third_party/licenses/NOTICE.md)。
- 项目保留 [HDYOU/c001apk](https://github.com/HDYOU/c001apk) 的 Fork/参考来源及原项目贡献者信息，详见应用内“关于”。
- 本项目及其依赖、第三方代码和资源以各自许可证为准。
