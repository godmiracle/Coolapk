# Planning Context

## User intent

用户希望按照已确认的推荐技术路线落地：在当前 XML/ViewBinding Android 项目中，以原生渲染为主引入 LiquidGlass 视觉体系，改造所有适合玻璃化的页面 Chrome 和浮层；不为此迁移 Compose，也不把玻璃效果铺到普通列表内容。

## Current findings

- 项目是传统 Android 多模块应用，主 UI 使用 Fragment、XML、ViewBinding/Data Binding、CoordinatorLayout、AppBarLayout、RecyclerView 和 ViewPager2；当前 Kotlin 1.9.24、AGP 8.5.1、minSdk 31，无 Compose 主链路。
- 项目已有 `LiquidGlassFrameLayout`，在 API 31+ 通过指定背景 View 的重绘和 `RenderEffect` 实现模糊，前景控件保持清晰；已经用于主导航底栏、搜索按钮、首页和发现页顶部玻璃区。
- `BasePagerFragment` 统一承载折叠头部、Toolbar、TabLayout、ViewPager2 和可选 FAB，当前被话题/数码、应用等详情页复用，是扩展玻璃页面 Chrome 的主要杠杆点。
- 用户主页有独立的折叠头部和 Toolbar；Feed 详情页有 Toolbar、悬浮排序栏和回复 FAB；这些是适合玻璃化的浮层/导航表面。
- RecyclerView 动态卡片、评论行、头像/封面、加载/错误状态和设置 Preference 行不作为玻璃化目标，以避免重绘成本、层级混乱和文本对比度风险。
- AndroidLiquidGlass 是 Compose Multiplatform backdrop 库，当前不直接作为应用依赖；仅借鉴 backdrop、边缘高光和 API 33+ 折射思路。XML 原生实现优先，保留 API 31+ 模糊能力，并为 API 33+ 可选效果设计降级。
- 当前玻璃实现的性能风险尚未完整量化，尤其是滚动过程中重绘背景 View 的帧率、CPU/GPU 和功耗，需要在改造中补充验证。

## Planned scope

1. 抽象并增强原生 XML 玻璃容器/样式，使玻璃表面具备统一的形状、透明度、边缘高光、主题色和 inset 处理。
2. 改造 `BasePagerFragment` 共享页面壳，覆盖详情页 Toolbar、Tab/extraBar 和 FAB 的适合部分。
3. 改造用户主页 Toolbar/顶部导航和 Feed 详情页的 Toolbar、排序浮层、回复 FAB。
4. 盘点搜索、历史、消息、设置、WebView 等页面，仅对有内容滚动背景的页面 Chrome 进行适度接入。
5. 对 BottomSheet、回复编辑器和 Dialog 保留专项验证边界，不在第一轮假设它们可以直接复用 Activity 内的背景采样。
6. 增加构建、静态检查、单元/资源验证，并记录滚动性能和深浅色/inset 验收结果。

## Decomposition decision

本轮使用 `small-feature` 单 change，不拆成并行子 change。原因是玻璃渲染容器、主题 token、共享页面壳和性能验证存在直接依赖，拆分会让多个 worker 同时修改同一套 UI 基础设施；先由一个 proposal/design 定义边界，再由 implementer 串行落地，reviewer 独立检查。

## Constraints and decisions

- 不引入 Compose，不改变现有 ViewModel、LiveData/Flow、Fragment 导航和 RecyclerView 数据链路。
- 保持 minSdk 31 的平台边界；API 33+ 效果必须有不影响 API 31/32 的回退。
- 不把玻璃效果放进高频 RecyclerView item；所有新玻璃表面必须保留可读文字、触控目标和无障碍语义。
- 不覆盖用户已有未相关改动；代码变更保持小范围并同步受影响的中文项目文档。
- 任何 BottomSheet/Dialog 跨 Window 的背景效果必须以实际设备验证为准，不能只依据静态代码推断完成。
