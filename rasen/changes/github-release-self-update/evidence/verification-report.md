# Verification Report: github-release-self-update

## Summary

| Dimension | Status |
|---|---|
| Completeness | 18/18 change tasks complete |
| Correctness | Offline build/test/lint gates and OPPO `PGEM10` device verification passed for the final app source |
| Coherence | Continues to use XML/ViewBinding、Hilt、Retrofit and the anonymous read-only boundary |
| Delivery | `main` and `v1.1.1` Tag pushed; both GitHub Actions runs succeeded; Release APK and checksum verified |

## Verification evidence

- Tested commit：`f25fb28b110e3efe7c0c563c1aaa75fd28aa7e03`；content tree：`e2c72d1bf79a1a16a919a1687a7ebe628c0eb0d2`。
- Current-source offline gate：`env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" bash gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --offline --no-daemon --max-workers=1`，`BUILD SUCCESSFUL`，229 actionable tasks。
- JVM unit tests：33 test cases，0 failures，0 errors，0 skipped。
- 提交前同一最终 app 源码的 connected instrumentation：`:app:connectedDebugAndroidTest`，OPPO `PGEM10`，14/14 passed。提交后的同命令重跑因设备已断开返回 `No connected devices!`，这是环境前置条件失败，不是测试断言失败。
- 本地正式包构建：`env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" bash gradlew :app:assembleRelease -PversionName=v1.1.1 --offline --no-daemon --max-workers=1`，`BUILD SUCCESSFUL`。
- Debug APK metadata：`applicationId=com.godmiracle.coolapk.debug`、`versionCode=465`、`versionName=fd943fc`；合并 Manifest 未声明 `REQUEST_INSTALL_PACKAGES`。
- 包名边界：`com.godmiracle.coolapk`（正式包）和 `com.godmiracle.coolapk.debug`（Debug 包）曾同时存在于设备，证明两者可并存。
- 设置/更新页：真实设置入口只保留“检查本客户端更新”；进入更新页首先显示手动检查状态，没有页面打开即请求，也没有应用内下载/安装按钮。
- Update API/browser path：设备内 latest API 已识别 `c001apk v1.1.0`；固定 Release 页面由 Firefox 打开至 `releases/tag/v1.1.0`。
- Batch-update cleanup：当前 `app/src` 静态扫描没有 `getAppsUpdate`、`isCheckUpdate`、`QUERY_ALL_PACKAGES`、AppList/AppUpdate/UpdateList 代码或资源；应用详情仍保留 `getAppDownloadLink` 和 `Utils.downloadApk`。
- `git diff --check`：passed。

## Release delivery evidence

- main Action：`33036449231`，success；Tag Action：`33036469151`，success。
- Release：`https://github.com/godmiracle/Coolapk/releases/tag/v1.1.1`，非 draft、非 prerelease。
- Release APK：`c001apk_v1.1.1.466.apk`；`aapt2 dump badging` 报告包名 `com.godmiracle.coolapk`、`versionName=v1.1.1`、`versionCode=466`。
- APK SHA256：`509d267b697419d4881187aa8163d916db3ecc5f0ca283d4890865e77347245a`，与 Release `SHA256SUMS` 的摘要一致。GitHub 下载端将括号文件名规范化为点号，因此按实际下载文件名直接比对摘要。
- Release APK 未发现 LeakCanary 条目。

## 安装覆盖边界

Android 正式包覆盖安装必须同时满足：`applicationId` 相同、签名证书一致、目标包 `versionCode` 更高。当前 Debug 使用 `com.godmiracle.coolapk.debug` 和 Debug 签名，正式包保持 `com.godmiracle.coolapk` 和 Release 签名；两者不互相覆盖。旧版使用正式包名的 Debug 安装若仍保留，需要用户手动卸载一次完成迁移。

## 当前范围与排除项

- 本轮明确移除客户端更新专用的 APK 下载、缓存、SHA-256 下载校验、FileProvider 更新路径、未知来源引导和 Package Installer Intent；浏览器下载后的安装由浏览器和 Android 系统负责。
- 本轮同时移除无当前路由的第三方应用批量更新检查、应用枚举、批量 API、设置开关和列表资源；不影响应用详情的单个 APK 下载。
- 未执行登录、消息、发帖、回复编辑、远程社交写入、字体放大、三键导航或 TalkBack 测试；这些不属于本轮范围，且账号/写入功能已移出当前产品边界。
- 未把浏览器内下载/安装声明为应用内自动化验收；本轮验证的是应用跳转到 Release 页面以及正式 Release 资产完整性。

## Remaining

- 本 change 的 implementation、review、delivery 已完成；后续可按 Rasen 生命周期执行 retention/archive。

VERIFY VERDICT: PASS for implementation, device evidence, and remote Release delivery
SPEC:v1
