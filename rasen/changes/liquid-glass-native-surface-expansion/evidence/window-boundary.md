# Window and ordinary-surface boundary audit

Date: 2026-08-26
Evidence level: static source, XML, manifest, and theme inspection only

## Conclusion

No incorrect GlassSurface source or cross-Window backdrop reuse was found in the
Settings, WebView, Reply2Reply BottomSheet, or related Material dialogs. No
application code was changed for task 6.3. These surfaces remain ordinary or
use their existing Window-local Material theme by design.

## Audited boundaries

| Area | Concrete files and owner | Current source / Window boundary | Decision and reason |
| --- | --- | --- | --- |
| Settings shell and Preference rows | `app/src/main/res/layout/fragment_settings.xml`; `SettingsActivity.kt`; `SettingsFragment.kt`; `SettingsPreferenceFragment.kt`; `app/src/main/res/xml/settings.xml` | No GlassSurface source. `SettingsActivity` owns the Activity Window; `settingsContainer` hosts a nested `PreferenceFragmentCompat` and its RecyclerView. | Keep the Toolbar and all Preference rows ordinary. Rows are standard `Preference`, `MaterialSwitchPreference`, and `SimpleMenuPreference` entries styled through `PreferenceThemeOverlay.Rikka.Material3.Mod`; no row declares a glass layout or backdrop. The nested Preference lifecycle is not promoted to a shared backdrop slot in this limited rollout. |
| Settings dialogs | `SettingsFragment.AboutDialog`; dialogs created by `SettingsPreferenceFragment` | Each `DialogFragment` or `MaterialAlertDialogBuilder` creates a Dialog Window and uses only its Window-local content view. | Out of scope. No dialog reads an Activity view, calls `setBackdropSource`, or receives a GlassSurface style. |
| WebView | `app/src/main/res/layout/activity_web_view.xml`; `WebViewActivity.kt`; `app/src/main/AndroidManifest.xml` | No GlassSurface source. `WebViewActivity` owns a separate Activity Window and runs in the declared `:webview` process; its WebView is created dynamically and added to that Activity root. | Keep the Toolbar, progress indicator, and WebView ordinary. The XML has no stable WebView source during inflation, failure handling can leave the dynamic WebView unavailable, and no backdrop may be borrowed from the launching Activity Window. WebView download/error dialogs remain their own Material dialog Windows. |
| Reply2Reply BottomSheet | `app/src/main/res/layout/dialog_reply_to_reply_bottom_sheet.xml`; `Reply2ReplyBottomSheetDialog.kt` | No GlassSurface source. `BottomSheetDialogFragment` creates a `BottomSheetDialog` with its own Dialog Window; the layout contains the indicator, controller, divider, RecyclerView, footer/items, and error text only. | Out of scope. Sampling a Feed/Activity view from this separate Window would violate the sibling-source contract. The RecyclerView rows, pagination footer, loading indicator, and error state remain ordinary. |

## Theme audit

- `Theme.C001apk` selects `PreferenceThemeOverlay.Rikka.Material3.Mod` and
  `CustomBottomSheetDialog.Md3`; neither automatically installs a GlassSurface.
- `PreferenceThemeOverlay.Rikka.Material3.Mod` maps Preference categories and
  row widgets to existing Rikka/AndroidX Preference styles. It does not map any
  row to `LiquidGlassFrameLayout` or a glass backdrop source.
- `CustomBottomSheetDialog.Md3` selects `CustomBottomSheet.Md3`, whose only
  customization is the existing shape appearance overlay. It does not reference
  a GlassSurface token or an Activity backdrop.
- `MaterialAlertDialog` styles in `themes.xml` customize shape only. The
  standalone `Widget.Coolapk.GlassSurface*` styles are not applied globally to
  Activity, Preference, BottomSheet, or Dialog themes.
- `Theme.MaterialTransparent` and `AppThemeTranslucent` are separate Window
  themes and do not establish a cross-Window GlassSurface source for the audited
  pages.

## Negative source evidence

A scoped search across the three layouts and their owning classes found no
`LiquidGlassFrameLayout`, `glassBackdropSource`, `setBackdropSource`, or
`refreshBackdrop` usage. The only backdrop-capable styles found in the audited
theme files are standalone GlassSurface definitions; none is assigned to the
audited Window or Preference styles.

## Verification boundary

This audit does not claim device rendering, light/dark/dynamic-theme appearance,
TalkBack behavior, system-inset behavior, or performance acceptance. Those
remain part of the later device and validation tasks.
