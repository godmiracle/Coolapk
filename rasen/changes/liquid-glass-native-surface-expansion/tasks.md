## 1. Baseline and surface contract

- [x] 1.1 Freeze the current glass baseline in `app/src/main/java/com/godmiracle/coolapk/view/LiquidGlassFrameLayout.kt`, `app/src/main/java/com/godmiracle/coolapk/ui/main/MainActivity.kt`, `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/layout/fragment_home.xml`, and `app/src/main/res/layout/fragment_discover.xml`; record the existing source View, surface count, blur/alpha behavior, scroll refresh, and inset boundaries in change evidence without changing app behavior.
- [x] 1.2 Define the shared `GlassSurface` contract and page slot map in the implementation notes/evidence: source must be a content sibling, foreground controls stay crisp, every surface has a fallback, and ordinary RecyclerView items/comments/avatars/covers/loading/error/Preference rows are explicitly excluded.

## 2. Shared native GlassSurface implementation

- [x] 2.1 Extend `app/src/main/java/com/godmiracle/coolapk/view/LiquidGlassFrameLayout.kt` as the shared native GlassSurface implementation; support themed surface color/alpha, blur radius, corner radius, border/highlight, source refresh, configuration refresh, no-source fallback, and attach/detach listener cleanup without introducing Compose or a second blur container.
- [x] 2.2 Add the shared styleable/theme/resource tokens in `app/src/main/res/values/attrs.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/values/themes_custom.xml`, `app/src/main/res/values/colors.xml`, `app/src/main/res/values/dimens.xml`, and only the required `app/src/main/res/drawable/` resources; remove page-local duplicate glass constants and ensure Material child backgrounds cannot add a second opaque surface.
- [ ] 2.3 Add focused platform tests/resources under `app/src/androidTest/java/com/godmiracle/coolapk/view/` and the corresponding test resources for XML inflation, source-not-ready fallback, scroll/page refresh, detach cleanup, light/dark theme resolution, API 31/32 behavior, and foreground accessibility semantics; keep the test independent of real account/API data.

## 3. BasePagerFragment shared page shell

- [x] 3.1 Update `app/src/main/res/layout/base_tablayout_viewpager.xml` with explicit optional GlassSurface slots for the pinned Toolbar, Tab/`extraBar`, and applicable FAB while preserving the existing `CoordinatorLayout`/`AppBarLayout`/`CollapsingToolbarLayout`/`ViewPager2` hierarchy, collapse flags, IDs used by ViewBinding, and scrolling behavior.
  - 实现说明：Toolbar 与 Tab/extraBar 使用共享玻璃层；FAB 保持原生 Material 控件并明确 opt-out，以保留 CoordinatorLayout、HideBottomViewOnScrollBehavior、inset margin、Tooltip 和 48dp 触摸边界。
- [x] 3.2 Update `app/src/main/java/com/godmiracle/coolapk/ui/base/BasePagerFragment.kt` to bind the slots to the correct content sibling, refresh on ViewPager2/page and scroll changes, keep `TabLayoutMediator` and `extraBar` visibility intact, and apply system bottom inset only to the existing FAB/container margin; the FAB click, hide/show behavior, tooltip, and 48dp target must remain unchanged.
  - 实现说明：共享页壳绑定 pager/content sibling，并在页面、滚动、AppBar 和 attach 状态变化时刷新；既有 TabLayoutMediator、extraBar、FAB 行为和 inset 归属保持不变。
- [x] 3.3 Enable or explicitly opt out the shared slots in `app/src/main/java/com/godmiracle/coolapk/ui/topic/TopicFragment.kt`, `app/src/main/java/com/godmiracle/coolapk/ui/app/AppFragment.kt`, `app/src/main/java/com/godmiracle/coolapk/ui/coolpic/CoolPicFragment.kt`, `app/src/main/java/com/godmiracle/coolapk/ui/carousel/CarouselPagerFragment.kt`, and `app/src/main/java/com/godmiracle/coolapk/ui/follow/FollowPagerFragment.kt`; verify existing detail headers, tabs, menus, paging, back navigation, and any special header measurement remain functional.
  - 实现说明：上述派生页复用 BasePagerFragment 的共享 slot，不重复包裹；发布 FAB 和写入动作不属于当前匿名只读产品范围，特殊 FAB 保持显式 opt-out。部分派生页的完整业务动作仍属于后续全量人工验收。

## 4. User page Chrome

- [x] 4.1 Modify `app/src/main/res/layout/base_user_page.xml` to add the shared surface only around the stable/collapsed Toolbar action Chrome, preserving the profile header replacement slot, `fragmentContainer`, AppBar scrolling flags, current top inset handling, and ordinary avatar/stat/list rendering.
- [x] 4.2 Update `app/src/main/java/com/godmiracle/coolapk/ui/user/UserPagerFragment.kt` (and `UserActivity.kt` only if binding/inset ownership requires it) to attach the user content sibling as backdrop, refresh across collapse/recreation, and verify back, share, block, search and other anonymous read-only menu behavior through UI hierarchy and device interaction.
  - 验证边界：API 36 真机已进入用户页、返回并完成短滚动；远程关注、举报和登录依赖动作已从当前产品范围移除。

## 5. Feed detail Chrome

- [x] 5.1 Modify `app/src/main/res/layout/fragment_feed.xml` to provide a shared surface for the Toolbar while keeping the sticky sort `tabLayout` ordinary and opaque; never wrap Feed正文, reply rows, images, or error/loading includes; preserve existing binding IDs and `StickyItemDecorator` assumptions.
  - 实现说明：仅 Toolbar 使用共享玻璃层；排序栏不绑定 `RecyclerView` backdrop，匿名只读范围移除回复 FAB，正文、回复行、图片和错误/加载布局均未包裹。
- [x] 5.2 Update `app/src/main/java/com/godmiracle/coolapk/ui/feed/FeedFragment.kt` to bind only the Toolbar content sibling, preserve sort visibility/selection, pagination and scroll behavior, and remove the reply write entry point.
  - 验证说明：API 36 真机此前已进入 FeedActivity；本轮确认排序栏无 backdrop source，Debug 构建通过。
- [ ] 5.3 Add or extend focused Feed UI/instrumentation coverage under `app/src/androidTest/java/com/godmiracle/coolapk/ui/feed/` for cold start with fixture/mock data, Toolbar/menu, sticky sort appearance and selection, reply visibility/inset, long-scroll hide/show, and confirmation that Feed/Reply RecyclerView items do not gain GlassSurface nodes or extra opaque layers.

## 6. Limited other page Chrome

- [x] 6.1 Update `app/src/main/res/layout/fragment_search.xml` and `app/src/main/java/com/godmiracle/coolapk/ui/search/SearchFragment.kt` to glassify only the content-backed search Toolbar; preserve EditText/IME focus, clear/search controls, back navigation, history/result handoff, source refresh, and bottom list inset.
  - 验证说明：API 36 真机已从独立搜索入口进入 SearchActivity，输入框、IME、历史项、返回和玻璃 Toolbar 均正常。
- [x] 6.2 Remove the obsolete message/login surface from the anonymous read-only product; do not retain message Toolbar, login/guest state, badges, or account-only resources.
  - 验证边界：消息与登录源码、资源和 Manifest 入口已删除；不运行登录/消息测试。
- [x] 6.3 Audit `app/src/main/res/layout/fragment_settings.xml`, `app/src/main/res/layout/activity_web_view.xml`, `app/src/main/res/layout/dialog_reply_to_reply_bottom_sheet.xml`, related settings/WebView/BottomSheet/Dialog classes, and `app/src/main/res/values/themes.xml`; ensure no Activity-local backdrop is incorrectly reused across Windows, Preference rows remain ordinary, and any intentionally unmodified surface is recorded as out of scope with a reason.
  - 证据：evidence/window-boundary.md；Settings Preference、独立 WebView Window、Reply2Reply BottomSheet/Dialog 均保持 Window-local ordinary surface。

## 7. Build, device, readability, inset, and performance verification

- [x] 7.1 Run resource/XML validation, `:app:assembleDebug`, focused unit/instrumentation tests, and `:app:lintDebug` for the final implementation; resolve new resource, ViewBinding/Data Binding, API, accessibility, and opaque-background warnings without changing unrelated modules.
 - 结果：当前匿名只读清理单元已完成资源处理、`assembleDebug`、`assembleDebugAndroidTest`、`testDebugUnitTest`（26 个）和 `lintDebug`；LiquidGlass fixture 定向真机 instrumentation 1/1 通过。仅保留既有 kapt/data binding/Gradle deprecation 类告警。
  - 补充：此前 connectedDebugAndroidTest 在 PGEM10 上 22/22 通过；它覆盖清理前范围，不能替代本轮匿名只读后的静态验证和 7.5 的全页面三轮 before/after 门槛。
- [ ] 7.2 On an API 31/32 environment and the current Android device or equivalent, cold-launch and exercise BasePager, User, Feed, Search, and Message in light, dark/dynamic theme, portrait, landscape, default font, and enlarged font; capture build identity, device/API, screenshots/UI hierarchy, and action results for back, tabs, menus, sort, reply, search, and scroll.
- [ ] 7.3 Validate insets on gesture navigation and available three-button navigation: inspect status-bar top edge, navigation-bar/FAB bottom margin, sort-bar placement, list end reachability, main floating navigation hide/show, rotation, and return; fail the task if any surface double-consumes insets, covers system bars, clips the last item, or leaves permanent bottom blank space.
- [ ] 7.4 Perform the readability/accessibility pass on every changed surface: verify title/Tab/menu/sort/search/FAB/error/empty foreground contrast in light/dark/dynamic themes, at least 48dp touch targets, content descriptions, selected/focused states, and UI hierarchy semantics; record unverified TalkBack/device cases rather than marking them passed.
- [ ] 7.5 Capture same-device before/after frame statistics for three repeated long-scroll runs on BasePager, Feed, Message, and Search using `adb shell dumpsys gfxinfo <package> framestats` or an equivalent reproducible method; pass only when median frame time and janky-frame ratio do not regress by more than 10% and there is no ANR, crash, or obvious touch latency.
- [ ] 7.6 If 7.5 fails, reduce independent surfaces or merge adjacent Toolbar/Tab/extraBar layers, lower blur/alpha, or disable the affected page slot in the scoped files; rerun 7.1–7.5 and preserve the raw before/after evidence and final trade-off decision.

## 8. Documentation and Rasen handoff

- [ ] 8.1 After implementation and acceptance only, update the affected Chinese project documentation (`docs/context.md`, `docs/architecture.md`, `docs/decisions.md`, `docs/todo.md`, and `docs/sessions/YYYY-MM-DD.md`) with the final native XML boundary, excluded surfaces, evidence level, performance/inset results, and any unresolved BottomSheet/Dialog/WebView limitation; do not mark unverified behavior complete.

## 当前执行状态（2026-08-26）

- 原生 XML GlassSurface 扩展、资源、页面绑定和最小独立 fixture 已落地。
- API 36 OPPO PGEM10 真机已验证主导航、搜索、发现、关注、我的、Feed、用户页，以及浅色/深色/横屏切换后的可用性；设备已恢复为竖屏、自动旋转开启、深色模式开启。
- 仍未完成 API 31/32、三键导航、放大字体、TalkBack、全页面完整动作矩阵、同设备 before/after 三轮帧统计和完整 Feed instrumentation，因此 2.3、5.3、7.2-7.6、8.1-8.2 保持未勾选。
- Rasen 当前停留在 apply；本轮不提交、不推送、不归档，待上述验收门槛或用户明确接受剩余边界后再进入 review/ship。
