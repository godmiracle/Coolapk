# Implementer 8 handoff

Date: 2026-08-26
Change: `liquid-glass-native-surface-expansion`
Reason: `user-interrupted-before-feed-edit`

## Actual status

- 本轮已按用户要求立即停止。
- 本轮没有修改 Feed 代码或资源；`app/src/main/res/layout/fragment_feed.xml` 与 `app/src/main/java/com/godmiracle/coolapk/ui/feed/FeedFragment.kt` 均没有产生本轮 diff。
- 本轮只新增本 handoff，没有运行 Feed 构建、测试、Lint、设备验证或性能验证。
- Rasen checklist 仍为 4/24：只有 `1.1`、`1.2`、`2.1`、`2.2` 已勾选；本轮未修改 `tasks.md`。
- 工作区继承了此前已落地但未在 checklist 勾选的 BasePager 与 User page 实现；不能把这些代码存在描述为对应任务已完成验收。

## Feed inspection completed before interruption

- 已只读检查 `fragment_feed.xml`、`FeedFragment.kt`、`StickyItemDecorator.kt` 与 `LiquidGlassFrameLayout` 的 source/lifecycle API。
- 当前 Feed hierarchy 为 `CoordinatorLayout` 下的 `AppBarLayout/MaterialToolbar`、`contentLayout`、direct reply `FloatingActionButton` 以及 indicator/error includes。
- sticky `tabLayout` 位于 `contentLayout` 内并包含现有 divider；`StickyItemDecorator.SortShowListener` 直接控制 `binding.tabLayout.isVisible`，横屏初始化也会隐藏它。
- reply FAB 当前直接持有 `HideBottomViewOnScrollBehavior<FloatingActionButton>`，navigation bar bottom inset 通过现有 listener 计算后写入 FAB 的 Coordinator margins。
- 中断前只形成了候选方案：Toolbar source 使用稳定 `contentLayout` sibling；sticky source 使用 `recyclerView`；reply 如使用 direct Coordinator glass wrapper，必须把 behavior/gravity/margins/inset 所有权迁移到 wrapper，并同步 wrapper/FAB visibility。该方案尚未写入代码，也未验证，不能视为决定已落地。

## Complete task state

- Checked and recorded complete: `1.1`, `1.2`, `2.1`, `2.2`.
- Remaining in checklist: `2.3`; `3.1`–`3.3`; `4.1`–`4.2`; `5.1`–`5.3`; `6.1`–`6.3`; `7.1`–`7.6`; `8.1`–`8.2`.
- Current bounded slice remains entirely pending: `5.1` and `5.2` have no Feed implementation from this turn; `5.3` was out of this slice and remains pending.

## Required next action

1. Re-open only `fragment_feed.xml` and `FeedFragment.kt` as edit targets, using the already inspected `StickyItemDecorator` and `LiquidGlassFrameLayout` contracts.
2. Add Toolbar and sticky sort surfaces without wrapping Feed正文、reply rows、images、avatars、loading/error includes.
3. Either preserve reply FAB as an explicit Material opt-out with a recorded behavior/inset reason, or place it in a direct Coordinator glass wrapper and move `HideBottomViewOnScrollBehavior`, gravity, margins, visibility, and navigation inset ownership to that wrapper.
4. Synchronize sticky surface visibility with `tabLayout` for decorator callbacks and landscape, bind safe sources, refresh on RecyclerView scroll/attach/recreation, and clean up on `onDestroyView`.
5. Run explicit-JAVA_HOME offline `:app:assembleDebug` and `git diff --check` after implementation.

## Unverified

- Feed resource/Kotlin compilation for tasks `5.1`–`5.2`.
- Device behavior, API 31/32, TalkBack/accessibility hierarchy, light/dark/dynamic theme, portrait/landscape, enlarged font, gesture/three-button inset behavior, reply FAB hide/show and bottom margin, sticky sort interaction, and navigation/menu/reply behavior.
- Same-device frame statistics, jank ratio, median frame time, ANR/crash/touch-latency performance gate.
