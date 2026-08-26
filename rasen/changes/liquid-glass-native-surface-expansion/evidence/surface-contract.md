# GlassSurface contract and page slot map

This change uses `LiquidGlassFrameLayout` as the single native implementation
of the semantic `GlassSurface` contract. No Compose, AndroidLiquidGlass, or
second blur container is permitted.

## Contract

1. A surface source is an independent content sibling (RecyclerView,
   ViewPager2, or the page's content container). It must not be the surface,
   the surface's ancestor, or a View that redraws the surface.
2. The source is sampled into an internal non-accessible backdrop. Toolbar,
   TabLayout, menu, input, sort button, and FAB children remain crisp and
   interactive.
3. Every surface has a theme-resolved color/alpha fallback while the source is
   missing, not laid out, detached, or otherwise unavailable. This fallback is
   not reported as a business loading/error state.
4. Blur, shape, border, highlight, and refresh behavior come from shared
   resources and the shared View implementation. Page code only binds a source
   and selects an explicit slot policy.
5. The surface is inset-neutral. Root/AppBar/FAB/container code remains the
   single owner for system-bar margins and padding.
6. Attach/detach, configuration changes, source changes, and ViewPager2 page
   changes must release stale listeners and refresh the current surface without
   retaining a destroyed Fragment view.

## Slot map

| Page | Glass slots | Backdrop source | Explicit exclusions |
| --- | --- | --- | --- |
| BasePager shell | pinned Toolbar; Tab/`extraBar` chrome; optional FAB | page `view_pager`/content sibling | special header may opt out; pager item content remains ordinary |
| User | collapsed Toolbar/action chrome only | `fragmentContainer` content sibling | profile header, avatar, stats, user list |
| Feed | Toolbar only; sticky sort remains ordinary opaque chrome | Toolbar: `contentLayout`; sort: no backdrop source |正文, sort-bar backdrop sampling, reply rows, images, avatars, loading/error/reply editor |
| Search | search Toolbar only | `recyclerView` history/results sibling | EditText remains ordinary foreground; history/result rows |
| Message | removed from anonymous read-only scope | none | login/message UI and account-only rows |

Settings Preference rows, WebViewActivity, Reply2Reply BottomSheet, and Dialog
remain ordinary or existing themed surfaces. They are cross-Window or
Preference-specific boundaries and are not claimed as supported by static
Activity code.
