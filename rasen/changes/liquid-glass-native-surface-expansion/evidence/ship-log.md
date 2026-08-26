# Ship Log: liquid-glass-native-surface-expansion

**Date:** 2026-08-26 17:33 +0800
**Mode:** push
**Branch:** `main`
**Implementation commit:** `e9c1e01f0f5622370e63df6ed6c388c43c9fb5b6`
**Follow-up commit:** `6f1c4148097f649bb8fe2c564d7fc71d41382ba3`
**Final tree:** `602366c0581d6faa82aae3187afe8e0682da47dc`
**Status:** Pushed

## Pre-Flight Results

- The implementation commit and the Release tag were pushed after the user explicitly requested delivery.
- Rasen implementation tasks: 15/23 complete. The remaining items are the full platform/API matrix, Feed instrumentation, inset/accessibility/performance acceptance, and the final acceptance checkbox; they remain unclaimed.
- No formal verification-report artifact was present. Scoped build, lint, JVM test, focused instrumentation, device cold-start, and remote Release evidence were available and recorded below.

## Test Gate

- Local gate passed with Android Studio JDK 21, offline dependencies, and no build cache: `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest` (26 tests), and `:app:lintDebug`.
- Focused connected gate passed on OPPO `PGEM10` / API 36 (`a60fe293`): `LiquidGlassFrameLayoutInstrumentedTest`, 1/1, 0 failures.
- After the instrumentation runner cleanup, the current Debug APK was reinstalled and `MainActivity` cold launch returned `Status: ok`, `LaunchState: COLD`.
- Full Feed UI matrix, API 31/32, enlarged-font, three-button, TalkBack, and three-run same-device performance gates were not claimed; the user waived those manual cases for this delivery.

## Deployment

- `main` implementation push: [e9c1e01](https://github.com/godmiracle/Coolapk/commit/e9c1e01f0f5622370e63df6ed6c388c43c9fb5b6)
- `v1.1.0` Tag points to the implementation commit and its Release Action [32952077269](https://github.com/godmiracle/Coolapk/actions/runs/32952077269) completed successfully.
- Release: [c001apk v1.1.0](https://github.com/godmiracle/Coolapk/releases/tag/v1.1.0)
- APK: [c001apk_v1.1.0.463.apk](https://github.com/godmiracle/Coolapk/releases/download/v1.1.0/c001apk_v1.1.0.463.apk)
- Checksum file: [SHA256SUMS](https://github.com/godmiracle/Coolapk/releases/download/v1.1.0/SHA256SUMS)
- APK SHA-256: `9b7af318a0f6ecb3a18206271eeb877d5d25d02481988e4946f16e30c7d1dfb6`; downloaded `sha256sum -c SHA256SUMS` passed.
- `aapt2` reported `versionCode=463`, `versionName=v1.1.0`; `apksigner` verified the v3 signature.
- CI filename/checksum follow-up [6f1c414](https://github.com/godmiracle/Coolapk/commit/6f1c4148097f649bb8fe2c564d7fc71d41382ba3) was pushed to `main`; its Action [32953039982](https://github.com/godmiracle/Coolapk/actions/runs/32953039982) also completed successfully. The branch run correctly skipped Release creation because it was not a Tag.
