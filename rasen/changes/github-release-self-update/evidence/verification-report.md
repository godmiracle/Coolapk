# Verification Report: github-release-self-update

## Summary

| Dimension | Status |
|---|---|
| Completeness | 17/18 change tasks complete; only independent review and delivery task 5.4 remains open |
| Correctness | Offline build/test/lint gates and connected device verification passed |
| Coherence | Continues to use XML/ViewBinding、Hilt、Retrofit and the anonymous read-only boundary |

## Verification evidence

- Current-source offline gate passed: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" bash gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --offline --no-daemon --max-workers=1`，`BUILD SUCCESSFUL`，229 actionable tasks。
- JVM unit tests: 33 test cases, 0 failures, 0 errors, 0 skipped。
- Current-source connected instrumentation: `:app:connectedDebugAndroidTest` on OPPO `PGEM10`，14/14 passed。
- Debug APK metadata: `applicationId=com.godmiracle.coolapk.debug`、`versionCode=465`、`versionName=fd943fc`；合并 Manifest 未声明 `REQUEST_INSTALL_PACKAGES`。
- Package boundary: `com.godmiracle.coolapk`（旧包）和 `com.godmiracle.coolapk.debug`（新 Debug 包）同时存在于设备，证明包名分离后可并存。
- Settings/update page: 通过真实设置入口确认设置页只保留“检查本客户端更新”；进入更新页首先显示“点击检查 GitHub 最新稳定版”，没有在页面打开时请求 GitHub，也没有旧“检查应用更新”开关。
- Update API/browser path: latest API 识别 `c001apk v1.1.0`、固定 Release 页面由 Firefox 打开等路径已在本 change 的既有真机验证中确认；当前实现仍只有检查按钮和“浏览器下载 Release（推荐）”，没有应用内下载/安装动作。
- Browser route: 点击固定 `https://github.com/godmiracle/Coolapk/releases/latest` 后 Firefox 进入前台并打开 `releases/tag/v1.1.0`。
- Batch-update cleanup: 当前 `app/src` 静态扫描没有 `getAppsUpdate`、`isCheckUpdate`、`QUERY_ALL_PACKAGES`、AppList/AppUpdate/UpdateList 代码或资源；应用详情仍保留 `getAppDownloadLink` 和 `Utils.downloadApk`。
- `git diff --check` passed。

## 安装覆盖边界

Android 正式包覆盖安装必须同时满足：`applicationId` 相同、签名证书一致、目标包 `versionCode` 更高。当前 Debug 使用 `com.godmiracle.coolapk.debug` 和 Debug 签名，正式包保持 `com.godmiracle.coolapk` 和 Release 签名；两者不互相覆盖。旧版使用正式包名的 Debug 安装若仍保留，需要用户手动卸载一次完成迁移。

此前 `versionCode=465` 的旧 Debug 包尝试安装已发布 Release `versionCode=463` 时被系统拒绝，属于正确的降级保护，不应通过放宽校验或强制覆盖绕过。

## 当前范围与排除项

- 本轮明确移除客户端更新专用的 APK 下载、缓存、SHA-256 下载校验、FileProvider 更新路径、未知来源引导和 Package Installer Intent；浏览器下载后的安装由浏览器和 Android 系统负责。
- 本轮同时移除无当前路由的第三方应用批量更新检查、应用枚举、批量 API、设置开关和列表资源；不影响应用详情的单个 APK 下载。
- 未执行登录、消息、发帖、回复编辑、远程社交写入、字体放大、三键导航或 TalkBack 测试；这些不属于本轮范围，且账号/写入功能已移出当前产品边界。
- 未把浏览器内下载/安装声明为应用内自动化验收；本轮验证的是应用跳转到 Release 页面。

## Remaining

- Rasen task 5.4：重新执行独立 review-cycle，并在用户明确要求后提交/推送；本轮没有执行 commit 或 push。

VERIFY VERDICT: PASS for implementation and device verification; DELIVERY PENDING
SPEC:v1
