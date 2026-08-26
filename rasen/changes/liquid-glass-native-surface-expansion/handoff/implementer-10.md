# Implementer 10 handoff

Date: 2026-08-26
Change: `liquid-glass-native-surface-expansion`
Reason: `feed-slice-and-anonymous-scope-landed-without-structured-return`

## Actual status

- Feed XML and Kotlin edits are present in the shared workspace.
- `fragment_feed.xml` wraps only the Toolbar in `LiquidGlassFrameLayout`; the sort bar, Feed content, reply rows, images, avatars, and error/loading includes remain ordinary.
- `FeedFragment.kt` binds only Toolbar to `contentLayout`; the sort bar no longer samples `recyclerView`, so reply content cannot be blurred by the sticky chrome. The reply FAB/editor was removed with the anonymous read-only scope.
- Login, message, publish/reply editor, remote like/follow, report/delete, captcha, and OSS upload paths were removed from the current source; read-only replies, sorting, local follow/history/block/favorite behavior remain.
- After clearing stale generated outputs, an explicit-JAVA_HOME offline `:app:assembleDebug` completed successfully with Kotlin process-in-process compilation; `git diff --check` passed.
- After the anonymous cleanup, `assembleDebugAndroidTest`, `testDebugUnitTest` (26 tests), and `lintDebug` completed successfully; the focused `LiquidGlassFrameLayoutInstrumentedTest` passed 1/1 on PGEM10. The current APK was then reinstalled and `MainActivity` cold-launched with `Status: ok`.

## Remaining

- Feed task 5.3 focused instrumentation remains pending.
- The post-cleanup JVM unit test and lint reruns are pending; do not treat historical pre-cleanup connected-test results as current coverage.
- Device, API 31/32, theme, font-scale, TalkBack, inset, interaction, and performance evidence remain unverified.
