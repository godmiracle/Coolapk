## Why

当前应用已经在首页、发现页和主导航使用 `LiquidGlassFrameLayout`，但详情页、用户页、动态详情页以及部分独立页面的 Toolbar、Tab 和浮动操作仍各自使用普通 Material 表面，玻璃视觉和 inset 处理缺少共享契约。现在项目的 Android 12+ 原生模糊基线已经确定，适合在不迁移 Compose、不改动业务数据链路的前提下，把玻璃效果扩展到低频页面 Chrome，并用真实的性能、可读性和系统 inset 验收约束范围。

## What Changes

- 将现有 `LiquidGlassFrameLayout` 规范并增强为可复用的 XML/ViewBinding `GlassSurface` 原生表面能力，统一形状、透明度、边缘高光、主题适配、背景采样刷新和系统 inset 约定。
- 通过 `BasePagerFragment` 共享壳扩展详情类页面的 Toolbar、Tab、可选 `extraBar` 和适合的 FAB；保持现有 Fragment、ViewPager2、ViewModel、分页和导航行为。
- 将用户主页的折叠头部/Toolbar Chrome 纳入共享玻璃表面；Feed 详情只保留 Toolbar 玻璃，排序栏使用普通不透明背景，不能采样回复列表。
- 适度改造有明确滚动内容背景的搜索页顶部表面；消息、登录、发布和回复编辑不属于当前匿名只读产品范围。对设置 Preference、WebView、BottomSheet 和 Dialog 只保留审查/验证边界，不直接套用 Activity 内背景采样。
- 增加 XML 资源、主题 token、布局/绑定接入和必要的静态/资源/单元验证；补充 Android 12+ 深浅色、字体可读性、触控/无障碍、横竖屏与系统导航/状态栏 inset 验收。
- 增加滚动场景的性能观测和退出条件，玻璃只作用于页面 Chrome/浮层，不将普通 RecyclerView 动态卡片、评论行、头像、封面、加载错误状态或设置行全部玻璃化。
- 不引入 Compose、AndroidLiquidGlass 依赖或新的网络/数据存储路径；不改变匿名 API、Room schema 和只读业务交互协议。

## Capabilities

### New Capabilities

- `glass-surface`: 共享原生玻璃表面的视觉、采样、主题、降级、inset、触控和无障碍契约。
- `glass-page-chrome`: BasePagerFragment、用户页、Feed 详情及有限其他页面 Chrome 的玻璃化范围与行为保持要求。
- `glass-surface-validation`: 性能、滚动重绘、深浅色/字体可读性、触控语义、系统 inset 和设备验收边界。

### Modified Capabilities

- 无。仓库当前没有可复用的 `rasen/specs/` capability spec；本 change 只新增上述契约。

## Impact

- 影响 `app` 模块的原生 Kotlin View、Fragment、Activity、XML layout、主题/颜色/尺寸/drawable 资源，以及与这些表面对应的单元、资源和设备验收代码。
- 主要实现区域为 `view/LiquidGlassFrameLayout.kt`（及共享 `GlassSurface` 资源/实现）、`ui/base/BasePagerFragment.kt`、`base_tablayout_viewpager.xml`、用户页、Feed 详情和搜索页的 Chrome。
- 不影响匿名 Retrofit/OkHttp、ViewModel 数据状态、RecyclerView item 数据模型、仍在使用的 Room 数据库、Manifest 权限或只读服务端 API；Android 最低版本保持 API 31。
