# Implementer 9 handoff

Date: 2026-08-26
Change: `liquid-glass-native-surface-expansion`
Reason: `interrupted-after-feed-layout-edit`

## Actual status

- `fragment_feed.xml` now contains a Toolbar `LiquidGlassFrameLayout` around the existing `toolBar`, and a `stickySortGlassSurface` around the existing `tabLayout` and divider.
- Existing binding IDs and Data Binding expressions remain in the XML hierarchy; Feed正文、回复行、图片、头像和 error/loading includes remain outside the glass surfaces.
- `FeedFragment.kt` has not yet been updated for source refresh, sticky-surface visibility, or the reply FAB behavior/inset boundary.
- The reply FAB remains a direct `CoordinatorLayout` child and is not yet wrapped; its current Material behavior is therefore unchanged.

## Verification boundary

- No Feed build or XML validation was run after this XML edit.
- Device, API 31/32, TalkBack, theme, inset, interaction, and performance verification remain unverified.

## Remaining

- Implement and verify FeedFragment source binding/refresh and sticky-surface visibility.
- Decide and record the reply FAB Material opt-out or safely migrate behavior to a direct glass wrapper.
- Keep tasks 5.1–5.3 unchecked until code and evidence are complete.
