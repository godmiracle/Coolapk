# Ship Log: github-release-self-update

**Date:** 2026-08-27T11:36:41+08:00
**Mode:** push
**Branch:** main
**Commit:** f25fb28b110e3efe7c0c563c1aaa75fd28aa7e03
**Tree:** e2c72d1bf79a1a16a919a1687a7ebe628c0eb0d2
**Status:** Pushed

## Pre-Flight Results

- Verification: pass；包含离线构建、单元测试、Lint、Instrumentation、Release 构建和 Tier C review evidence。
- Tasks: 18/18 complete。

## Test Gate

- Required scope: `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`、`:app:connectedDebugAndroidTest`、`:app:assembleRelease`。
- Rationale: 变更同时影响 GitHub 网络边界、XML/ViewBinding 更新页、Manifest/权限、Debug/Release 包标识、旧批量更新清理和 CI 发布资产，因此需要 JVM、Android 资源/编译、Lint、真机和正式包门禁。
- Tests: Debug/Unit/AndroidTest APK/Lint 离线命令 `BUILD SUCCESSFUL`；提交前 OPPO `PGEM10` connected instrumentation 14/14；Release 构建 `BUILD SUCCESSFUL`。提交后 connected 重跑因设备断开返回 `No connected devices!`，不改变提交前同源码的通过证据。
- Tree: e2c72d1bf79a1a16a919a1687a7ebe628c0eb0d2

## Remote Delivery

- `origin/main`：`f25fb28b110e3efe7c0c563c1aaa75fd28aa7e03`。
- `v1.1.1`：Tag 已推送到同一 commit；main Action `33036449231` 和 Tag Action `33036469151` 均成功。
- Release：`https://github.com/godmiracle/Coolapk/releases/tag/v1.1.1`。
- Asset：`c001apk_v1.1.1.466.apk`；正式包 `com.godmiracle.coolapk`，`versionName=v1.1.1`，`versionCode=466`。
- SHA256：`509d267b697419d4881187aa8163d916db3ecc5f0ca283d4890865e77347245a`，与 `SHA256SUMS` 一致；GitHub 下载文件名的括号被规范化为点号，已按实际下载文件直接比对。

## Post-Ship

- 后续按 Rasen 生命周期执行 `rasen-retain github-release-self-update`，然后在确认合并/归档策略后执行 Archive。

REVIEW:v1
SPEC:v1
