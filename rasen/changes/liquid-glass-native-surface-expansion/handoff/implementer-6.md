# Implementer 6 handoff

Date: 2026-08-26
Change: `liquid-glass-native-surface-expansion`
Reason: `user-interrupted-before-second-slice-edit`

## Actual status

- 本次续作已按用户要求立即停止。
- 本次续作没有修改任何应用代码或资源；只新增本 handoff。
- 工作区继续保留上一轮已完成的 `app/src/main/res/layout/base_tablayout_viewpager.xml` 单文件 XML 改动：现有 Toolbar 与 Tab/`extraBar` 已放入 `LiquidGlassFrameLayout`，但动态 FAB slot 尚未接入。
- 本次续作没有修改 `BasePagerFragment.kt` 或任何其他 Kotlin 文件。
- `tasks.md` 未修改，3.1/3.2 未标记完成。

## Inspection completed before interruption

- 已只读检查当前 Git diff、`tasks.md` 的 3.1/3.2、`BasePagerFragment.kt`、目标 XML，以及 BasePager 派生页对 `initFab`、`fabBehavior`、`binding.view`、`extraBar` 的聚焦引用。
- 真实实现中 FAB 仍由 `BasePagerFragment.initFab()` 动态创建并直接加入 `CoordinatorLayout`；`HideBottomViewOnScrollBehavior<FloatingActionButton>`、navigation bar bottom inset、tooltip 和派生页 click listener 均仍在原路径。
- Topic/App 会替换 `binding.view` 对应的 header View；因此后续 Toolbar backdrop source 不能假设被替换后的旧 `binding.view` 仍然 attached，必须使用替换后仍稳定存在的 sibling/container，或提供可验证的显式绑定。

## Verification boundary

- 本次续作没有运行 Gradle、资源编译、Kotlin 编译、测试或 `git diff --check`，因此没有本次续作编译结果。
- 上一轮在仅有目标 XML 改动后曾运行指定的离线 `:app:assembleDebug`，结果为 `BUILD SUCCESSFUL in 16s`；该结果早于本次未落地的 FAB/Kotlin 续作，不能作为 3.1 剩余部分或 3.2 的验证证据。
- 不宣称设备、API 31/32、TalkBack、inset 或性能验证通过。

## Remaining

- 3.1：动态 FAB GlassSurface slot 仍未实现。
- 3.2：尚未在 `BasePagerFragment.kt` 中绑定稳定 content sibling，也未增加 ViewPager2 page/scroll refresh 与生命周期清理。
- 后续实现必须保持现有 IDs、`TabLayoutMediator`、`extraBar` 可见性、FAB click、滚动 hide/show、tooltip、至少 48dp 触控目标和 bottom inset；不得触碰 User/Feed/Search/Message、2.3 或文档。
