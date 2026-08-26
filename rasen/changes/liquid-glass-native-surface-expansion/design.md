## Context

本 change 建立在现有 Android 12+ 原生玻璃实现之上。`app` 使用 Kotlin、XML Layout、ViewBinding/Data Binding、Fragment、RecyclerView、ViewPager2 和 Material 组件；没有 Compose 主链路。现有 `LiquidGlassFrameLayout` 通过一个内部 backdrop View 绘制指定背景 View，并使用 `RenderEffect` 模糊，前景文字、图标和按钮保持清晰；首页、发现页和主导航已经使用它。`BasePagerFragment` 则统一提供 `AppBarLayout`、折叠 Toolbar、TabLayout、ViewPager2、可选 `extraBar` 和动态 FAB，是详情类页面的主要扩展点。

本次设计只处理页面 Chrome：用户主页的折叠 Toolbar、Feed 详情页的 Toolbar，以及搜索页的顶部表面。Feed 排序栏必须是普通不透明表面，不能采样回复列表；动态卡片、评论行、头像、封面、加载/错误状态、设置 Preference 行不进入玻璃层；BottomSheet、Dialog 和独立 WebView Window 不直接复用 Activity 内的背景采样。当前产品为匿名只读范围，登录、消息、发布、回复和远程社交写入路径不属于保留行为。

仓库当前没有 `rasen/specs/` capability spec，因此本 change 的三个新 spec 是实现和验收的唯一 planning contract：共享 `GlassSurface`、页面 Chrome 范围、以及验证边界。

## Goals / Non-Goals

**Goals:**

- 在 XML/ViewBinding 路线上形成单一的 `GlassSurface` 表面契约，统一背景采样、模糊、透明度、圆角、边缘高光、主题色、生命周期刷新和降级行为。
- 让 `BasePagerFragment` 的共享壳可以以最少页面特例承载玻璃 Toolbar、Tab/extraBar 和适用的浮动操作表面，同时保留折叠、分页、点击、滚动隐藏和无障碍语义。
- 以相同契约接入用户主页和 Feed 详情的关键 Chrome，并在搜索、消息页有限接入有滚动背景的顶部 Chrome。
- 让系统状态栏/导航栏 inset 只由现有页面外层容器处理；玻璃表面本身不重复消费 inset，也不制造底部永久空白。
- 用同一设备上的基线对比验证滚动重绘成本、深浅色和字体可读性、触控目标、横竖屏布局和 Android 12+ 行为。

**Non-Goals:**

- 不迁移 Compose，不引入 AndroidLiquidGlass、Compose Multiplatform backdrop 或新的 UI 框架/依赖。
- 不把玻璃背景放进 RecyclerView item，不改动列表数据、Adapter、分页、ViewModel、网络 API 或 Room schema。
- 不在第一轮实现跨 Window 的 BottomSheet/Dialog/WebView 模糊，不凭静态代码推断这些效果已经完成。
- 不改变 Android 最低版本、主导航菜单、用户/Feed 业务动作或已有横屏导航结构。

## Decisions

### 1. 以现有 `LiquidGlassFrameLayout` 承载共享 `GlassSurface` 契约

`GlassSurface` 是本 change 的语义和行为名称；实现优先演进现有 `app/src/main/java/com/godmiracle/coolapk/view/LiquidGlassFrameLayout.kt`，不并行创建第二套模糊容器。必要的 XML styleable、主题 token 和资源使用统一命名，现有调用点保持可迁移。表面至少支持：

- 可选的 sibling/parent content backdrop source；source 变化或页面切换时显式刷新。
- 统一的 surface color/alpha、blur radius、corner radius、border/highlight token，并从 Material/主题颜色解析深浅色和动态主题。
- source 暂不可用、View 尚未布局或 RenderEffect 不可用时的稳定半透明/着色表面，不抛异常、不遮蔽子控件。
- attach/detach 时注册和移除滚动监听，禁止把自身或包含自身的祖先作为 source，避免重绘递归。

**替代方案：** 新增 Compose `GlassSurface` 或引入 AndroidLiquidGlass 能提供更多折射效果，但会改变项目 UI 基线、扩大依赖和测试面；只在现有 native View 中做一套新的 `GlassSurface` 类则会与已验证的 `LiquidGlassFrameLayout` 重复实现，故不采用。

### 2. 用资源 token 和 slot wrapper 统一视觉，不在页面中复制数值

圆角、表面 alpha、模糊半径、边缘高光、内外间距和主题色集中到 `values`/`drawable` 资源或共享 style。页面 XML 只声明明确的 surface slot；Kotlin 负责把实际滚动内容 View 绑定为 backdrop source。相邻的 Toolbar/Tab/extraBar 尽可能合并为一个 surface，避免同一屏同时启动过多独立 blur layer；FAB 仅在确实需要独立悬浮层时使用单独 surface。

**替代方案：** 每个页面自行设置半透明背景和 `RenderEffect` 可以减少抽象初期工作，但会继续产生 alpha、圆角、inset 和主题漂移，且无法统一性能退出条件，故不采用。

### 3. `BasePagerFragment` 以可选 Chrome slot 扩展

在 `base_tablayout_viewpager.xml` 保留 `CoordinatorLayout → AppBarLayout → CollapsingToolbarLayout → ViewPager2` 的层级和滚动 flags，只在 Toolbar、Tab/extraBar 和 FAB 的稳定槽位增加共享表面容器。`BasePagerFragment` 在 view 创建完成后绑定 source、刷新时机和可选 slot；派生页面不重写数据/导航链路，必要时通过显式开关退出某个 slot。Toolbar 的 pin、TabLayoutMediator、`extraBar` 可见性、FAB 的 `HideBottomViewOnScrollBehavior` 和 bottom inset 计算都必须继续由原有结构负责。

**替代方案：** 将整个 `AppBarLayout` 或整个页面包进一个大玻璃层会改变折叠测量、放大重绘区域并使内容和 Chrome 混层，故不采用。

### 4. 页面接入按内容背景和风险分层

- `BasePagerFragment` 派生的 Topic/App/CoolPic/Carousel/Follow 等详情壳优先复用共享 slot；拥有特殊头部的页面可关闭不适合的 slot。
- 用户页只玻璃化折叠 Toolbar/动作 Chrome；用户资料头部和头像图片仍是普通内容，不把完整个人资料卡变成玻璃 item。
- Feed 详情只玻璃化 Toolbar；排序栏使用普通不透明背景，不给回复列表施加 backdrop blur，Feed 正文、回复列表和图片仍保持原有布局。
- 搜索页只接入有明确列表背景的顶部 Toolbar；消息页及登录页已从匿名只读范围移除。历史/设置等其他独立页面先经过同一 slot 审查，若没有安全 source 不强行玻璃化。Preference 行、WebView 和跨 Window 浮层默认不接入。

这样可以满足“适度的其他页面 Chrome”，同时把高频列表和不确定的跨 Window 场景留在边界内。

### 5. Inset 由外层布局单点负责

GlassSurface 不设置 `fitsSystemWindows`，也不主动修改 child padding。状态栏顶部 inset 继续由页面的 AppBar/root 自定义 inset 属性消费；底部导航 inset 继续由现有 FAB/浮岛容器的 `WindowInsetsCompat` listener 转换为 margin。新增 surface 的内外间距属于视觉 token，不得把 navigation bar inset 再叠加成装饰空白。横屏沿用既有 NavigationRail/页面 inset 逻辑，不为横屏新增另一套玻璃导航。

### 6. 性能以同设备基线对比作为合入门槛

实现阶段在同一设备、同一构建类型和同一滚动脚本下记录未改造基线与改造结果，使用 `adb shell dumpsys gfxinfo <package> framestats` 或等价的 Android 帧统计记录三次连续滚动。若改造使 janky frame 比例或中位帧耗时相对基线增加超过 10%，或出现明显持续掉帧、触摸延迟、ANR/崩溃，则减少独立 surface、合并相邻 Chrome、降低 blur 或对该页面回退为普通表面；原始数据和决定必须留在 change evidence，不用主观截图替代性能结论。

**替代方案：** 只做一次冷启动截图或只依赖 Debug 构建成功无法观察滚动重绘和功耗；引入完整 Macrobenchmark 模块会扩大本 change 的工程面，故第一轮采用可重复的设备帧统计，若数据表明需要再单独提出基准测试 change。

## Risks / Trade-offs

- **[背景采样递归或错位]** → 每个 surface 的 source 必须是独立的内容 sibling，不能是自身/祖先；在 source 尚未布局和 ViewPager 切页时主动刷新，并在 attach/detach 清理监听；设备 UI dump/截图确认采样区域与 surface 对齐。
- **[多个 RenderEffect 增加 GPU/CPU 和功耗]** → 相邻 Chrome 合并、限制每屏独立 blur surface 数量、只对低频 Chrome 使用；按同设备帧统计门槛回退页面或降低效果。
- **[Material 默认背景覆盖玻璃层]** → surface 内的 Toolbar/Navigation 子控件保持透明或使用既有透明 item background，保证只有共享 surface 绘制玻璃；不修改控件的点击、选中和无障碍边界。
- **[折叠 AppBar 测量或滚动行为回归]** → 保留现有 AppBar/CollapsingToolbar/ViewPager 层级和 collapse flags，只增加稳定 slot wrapper；对 Topic/App/User/Feed 做冷启动、折叠、Tab 切换和返回验证，问题页面可以通过显式开关退出。
- **[透明背景下文字对比度不足]** → 颜色来自主题 token，前景控件保持清晰；浅色、深色、动态主题和放大字体分别检查标题、Tab、菜单、排序按钮、FAB 图标及错误/空状态，不通过把列表 item 玻璃化解决。
- **[系统 inset 重复消费]** → surface 不消费 inset，沿用页面 root/AppBar/FAB 的既有 listener；检查手势导航和三键导航、竖屏和横屏的 top/bottom 边界，确认列表末尾仍可滚动到安全区域。
- **[BottomSheet/Dialog/WebView 误用 Activity source]** → 第一轮不做跨 Window 采样；这些页面只记录为未决/不在范围，除非后续独立 change 提供专门的 Window 级背景和真机验收方案。

## Migration Plan

1. 先扩展共享 `GlassSurface` 契约、资源 token、生命周期/刷新逻辑和最小单元/资源验证，确保现有首页、发现页和主导航仍能构建。
2. 在 `BasePagerFragment`/`base_tablayout_viewpager.xml` 接入可选 slot，再逐个启用详情类页面；先验证折叠、Tab、extraBar、FAB 和 inset，失败时可按页面关闭 slot。
3. 接入 `base_user_page.xml`/`UserPagerFragment` 与 `fragment_feed.xml`/`FeedFragment` 的指定 Chrome；保持正文/列表 item 不变。
4. 接入搜索和消息顶部 Chrome，完成其他页面审查记录；Settings、WebView、BottomSheet/Dialog 维持普通表面。
5. 在 Android 12+ 模拟器和当前 Android 真机执行构建、Lint/资源检查、深浅色/字体/横竖屏/inset/交互/性能验收。若性能或可读性门槛失败，优先减少表面范围或回退某页面，不扩大 blur。

本 change 无数据库、网络或持久化迁移。回滚只需移除页面 XML slot/启用开关并保留共享容器的普通着色回退；不需要数据回滚或版本迁移。

## Open Questions

- 具体页面在最终实现时应选择 `recyclerView`、`ViewPager2`、`CustomViewFlipper` 或其他内容 sibling 作为 backdrop source；实现者必须以实际 view hierarchy 和设备截图确定，不能猜测。
- `BottomSheet`、`Dialog` 和 `WebViewActivity` 是否需要真正的跨 Window 材质，应作为后续独立设计；本 change 的验收只确认它们没有被错误接入。
- 性能基线的原始帧统计需在实现分支和目标设备上采集；若设备统计本身不稳定，应保留三次原始结果并由 reviewer 决定是否缩小表面范围，不能宣称“性能已验证”而不提供数据。
