# Device validation

Date: 2026-08-26
Evidence level: API 36 physical-device spot validation, not full acceptance

## Device and build

- Device: OPPO PGEM10
- Serial: a60fe293
- Android: API 36
- ABI: arm64-v8a
- Resolution: 1080×2376
- Navigation: gesture navigation
- Package: com.godmiracle.coolapk
- Version code: 462
- Version name: def065d

## Build and instrumentation

The following completed successfully after the anonymous-scope cleanup with Android Studio JDK 21 and offline dependencies:

- env JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home ./gradlew :app:assembleDebug --offline --no-daemon --max-workers=1 -Dkotlin.incremental=false -Dkotlin.compiler.execution.strategy=in-process --no-build-cache
- env JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home ./gradlew :app:assembleDebugAndroidTest --offline --no-daemon --max-workers=1 -Dkotlin.incremental=false -Dkotlin.compiler.execution.strategy=in-process --no-build-cache
- env JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home ./gradlew :app:testDebugUnitTest --offline --no-daemon --max-workers=1 -Dkotlin.incremental=false -Dkotlin.compiler.execution.strategy=in-process --no-build-cache (26 tests)
- env JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home ./gradlew :app:lintDebug --offline --no-daemon --max-workers=1 -Dkotlin.incremental=false -Dkotlin.compiler.execution.strategy=in-process --no-build-cache
- focused connected test `LiquidGlassFrameLayoutInstrumentedTest`: 1/1 on PGEM10
- git diff --check

After the focused test runner removed the target package, the current
`app-debug.apk` was installed again. `adb shell am start -W
-n com.godmiracle.coolapk/.ui.main.MainActivity` returned `Status: ok`,
`LaunchState: COLD`, and the Activity remained the focused/resumed Activity.

The lint output retained only pre-existing kapt/data binding/Gradle deprecation warnings; no new NewApi or UnsupportedApi warning was introduced.

The 22-test connected Android instrumentation suite is historical pre-cleanup evidence only; it included the now-removed credential migration test and is not counted as current coverage. It is not a replacement for Feed-specific UI coverage. The current run deliberately executed only the GlassSurface test and did not run login, publish, reply-editor, message, or remote-social-write flows.

## Automated Search A/B sample

The uncommitted Glass expansion was compared with a temporary build of the repository HEAD before the expansion. Both builds used the same device and persisted test data. Three isolated long-scroll samples produced:

| Sample | Baseline | Current Glass build |
|---|---:|---:|
| Idle jank | 10/11 (90.91%) | 9/10 (90.00%) |
| Scroll 1 | 32/106 (30.19%) | 27/100 (27.00%) |
| Scroll 2 | 20/94 (21.28%) | 15/90 (16.67%) |
| Scroll 3 | 16/93 (17.20%) | 24/97 (24.74%) |
| Weighted scroll total | 68/293 (23.21%) | 66/287 (23.00%) |

The current build has no consistent jank regression in this small Search-only sample. The baseline and current raw dumps are retained in the local temporary directory; this does not satisfy the full three-page before/after performance gate.

The connected test harness temporarily removed the app package and left user_rotation=1 after completion. The current APK was reinstalled without clearing data, rotation was restored to accelerometer_rotation=1 and user_rotation=0, and MainActivity was cold-launched successfully.

## UI paths checked

| Path | Result |
|---|---|
| MainActivity cold launch | ok; MainActivity remained foreground |
| Main navigation: Home, Follow, Discover, My | ok; bottom glass and independent search glass visible where applicable |
| SearchActivity | ok; glass Toolbar, focused input, IME, history and back |
| FeedActivity | ok; Toolbar and sticky sort visible; selecting 最新 made dateLine checked=true |
| UserActivity | ok; glass Toolbar, back and short scroll |
| Message detail | not claimed; Activity is non-exported and no public main-nav entry was found |

## Theme and rotation

- Dark mode was restored to yes after the check.
- Light mode screenshot: /private/tmp/coolapk-liquid-glass-main-light.png
- Dark mode screenshot: /private/tmp/coolapk-liquid-glass-restored-dark.png
- Landscape screenshot: /private/tmp/coolapk-liquid-glass-landscape.png
- Main screenshot: /private/tmp/coolapk-liquid-glass-main.png
- Search screenshot: /private/tmp/coolapk-liquid-glass-search.png
- Feed screenshot: /private/tmp/coolapk-liquid-glass-feed.png
- User screenshot: /private/tmp/coolapk-liquid-glass-user.png
- Final device state: accelerometer_rotation=1, user_rotation=0, Night mode=yes, MainActivity foreground

## Remaining acceptance boundary

API 31/32, three-button navigation, enlarged font, TalkBack, full page/action matrix, full Feed instrumentation, and three repeated same-device before/after frame-stat runs remain unverified. A long Home swipe once returned to Launcher; a shorter repeat did not reproduce it, so this remains an open device-risk observation.
