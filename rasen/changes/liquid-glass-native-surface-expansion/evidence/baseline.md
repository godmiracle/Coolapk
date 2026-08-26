# Existing glass baseline

Date: 2026-08-26
Commit inspected: `def065da777ebc2ca51e2be8dfca2bed8d9cd102`

This is a static baseline captured before application-code changes. It records the
current XML/ViewBinding surface behavior; it is not a device or performance
acceptance result.

## Existing surface inventory

| Page | Surface count | Current source View | Current configuration |
| --- | ---: | --- | --- |
| `MainActivity` portrait | 2 | `binding.viewPager` for both `bottomNavSurface` and `searchActionSurface` | Bottom navigation: blur 18dp, alpha 0.28, radius 30dp. Search action: blur 20dp, alpha 0.32, radius 28dp. |
| `HomeFragment` | 1 | `binding.viewPager` | blur 14dp, alpha 0.24, radius 26dp |
| `DiscoverFragment` | 1 | `binding.viewPager` | blur 14dp, alpha 0.24, radius 26dp |

The foreground children remain ordinary Material/XML controls. The existing
surface itself adds an internal non-accessible backdrop child at index 0 and
draws a border plus a second highlight stroke in `dispatchDraw`.

## Source, refresh, and fallback behavior

- `LiquidGlassFrameLayout.setBackdropSource(source)` stores one source View,
  registers a source `ViewTreeObserver.OnScrollChangedListener`, assigns the
  source to the internal backdrop View, and calls `refreshBackdrop()`.
- Scroll refresh currently invalidates the backdrop and posts an animation
  invalidation. `MainActivity` additionally calls `refreshBackdrop()` after its
  `ViewPager2` page selection changes. Home and Discover do not add a page
  callback of their own.
- The backdrop copies the source only when the source has a positive width and
  height. Otherwise it paints its surface color rectangle. The current public
  API does not expose XML attributes, theme tokens, configuration refresh, or a
  source-not-ready state beyond that color rectangle.
- `onDetachedFromWindow()` removes the source scroll listener when the source
  observer is alive. There is no explicit attach-time rebind or configuration
  callback.

## Inset and scroll boundaries

- `activity_main.xml` overlays `ViewPager2` with `bottomNavContainer`. The
  navigation-bar/system-bars bottom inset is converted to the container bottom
  margin in `MainActivity.fixNavigationContainerInsets`; `LiquidGlassFrameLayout`
  itself does not consume insets.
- `fragment_home.xml` and `fragment_discover.xml` use the existing app-bar top
  inset attributes and `ViewPager2` app-bar scrolling behavior. Their top glass
  margins are a fixed 8dp visual token and are independent of system insets.
- Existing lists continue to own their bottom safe-area handling. Main
  navigation chrome hides while content is dragging/settling and returns when
  the source list is idle; the glass surfaces move with that existing chrome.

No application behavior was changed to capture this baseline.
