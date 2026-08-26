## ADDED Requirements

### Requirement: Shared native GlassSurface rendering contract

应用 SHALL 提供一个统一的 XML/ViewBinding 原生 `GlassSurface` 表面契约，并优先由现有 `LiquidGlassFrameLayout` 演进承载。所有本 change 新增的玻璃 Chrome MUST 复用该契约，不得在页面中复制另一套 `RenderEffect`、圆角、alpha 或边缘高光实现；契约不得引入 Compose、AndroidLiquidGlass 或新的 UI 运行时依赖。

#### Scenario: XML page chrome inflates the shared surface

- **WHEN** 页面 XML 为 Toolbar、Tab、排序栏或适用的浮动操作声明 `GlassSurface`
- **THEN** 该 surface 由共享原生 View/Binding 承载，子控件仍以普通 Material/XML 控件渲染，页面不需要新增 Compose 内容树

#### Scenario: Existing glass entry points remain usable

- **WHEN** 首页、发现页或主导航继续引用现有 `LiquidGlassFrameLayout`
- **THEN** 共享契约的增强不会破坏这些既有 surface 的构建、绑定、点击和背景刷新行为

### Requirement: Backdrop sampling and safe fallback

`GlassSurface` SHALL 从明确指定的内容 sibling/parent View 采样背景并在 surface 内施加平台模糊；背景内容必须位于 surface 后方，surface 的文字、图标、按钮等子控件 MUST 保持清晰。source 尚未布局、暂不可用或效果无法创建时，surface MUST 使用稳定的主题着色/半透明回退，不得崩溃、显示未初始化黑块或遮蔽子控件。Android API 31/32 和 API 33+ 均 MUST 有可接受结果，且实现不得依赖 API 33+ 专属能力才能工作。

#### Scenario: Content scrolls behind a surface

- **WHEN** 用户滚动作为 backdrop source 的 RecyclerView/ViewPager2 内容
- **THEN** surface 显示与内容位置一致的模糊背景，前景标题、Tab、菜单和按钮不被模糊或重复绘制

#### Scenario: Source is unavailable during first layout

- **WHEN** surface 已 inflate 但 source 尚未完成布局、页面正在切换或 source 暂时为空
- **THEN** surface 显示可读的主题着色表面并在 source 可用后刷新，不抛异常、不把空白 source 当成业务错误

#### Scenario: Android 12+ platform boundary

- **WHEN** 同一 surface 在 API 31/32、API 33+ 设备上运行
- **THEN** API 31/32 使用项目支持范围内的原生模糊/着色行为，API 33+ 可以增强但不得成为必要条件，API 24–30 不被重新纳入维护范围

### Requirement: Shared theme, shape, and edge tokens

所有共享 surface SHALL 使用集中定义的主题/资源 token 控制 surface color、透明度、blur radius、corner radius、border 和 highlight。token MUST 适配浅色、深色、动态主题和用户字体比例；页面接入不得硬编码一组只适用于单个截图的颜色或尺寸。边缘高光和边框不得通过额外阴影层制造大面积不透明背景。

#### Scenario: Light and dark theme resolve the same contract

- **WHEN** 用户在浅色、深色或动态取色主题下打开同一页面
- **THEN** surface 的透明度、圆角、边缘层次和前景文字/图标对比度保持在可读范围，且无需为每个页面复制另一套玻璃参数

#### Scenario: Font scale changes surface content

- **WHEN** 用户提高系统字体比例并打开带玻璃 Toolbar/Tab 的页面
- **THEN** surface 能随内容测量，标题、Tab、菜单和按钮不被裁剪，surface 不以固定高度覆盖文本

### Requirement: Lifecycle and refresh safety

`GlassSurface` MUST 在 attach/detach 生命周期内管理 source 的滚动/布局刷新监听；页面销毁后不得继续持有已销毁 View 或触发无效重绘。source MUST NOT 是 surface 自身、包含 surface 的祖先或会再次绘制该 surface 的循环路径。ViewPager2 切页、页面重建和主题配置变化后 MUST 能主动刷新 surface。

#### Scenario: Page view is destroyed and recreated

- **WHEN** Fragment view 销毁后再次创建，或 Activity 因配置变化重建
- **THEN** 旧 surface 移除 source listener，新 surface 绑定当前 content source，且没有重复回调、泄漏或对旧 binding 的访问

#### Scenario: ViewPager2 changes the visible page

- **WHEN** BasePagerFragment 或主页面的 ViewPager2 完成选中页切换
- **THEN** 关联 surface 在下一帧使用当前内容刷新，玻璃区域不保留上一页的错误快照

### Requirement: GlassSurface does not consume system insets

`GlassSurface` SHALL 保持系统 inset 中立：不得自行设置 `fitsSystemWindows`、重复消费 `WindowInsetsCompat` 或向页面内容注入额外底部 padding。状态栏/导航栏 inset 必须由现有页面 root、AppBar 或 FAB/浮岛容器单点处理；surface 的视觉 margin 与系统安全区 MUST 分开计算。

#### Scenario: Portrait gesture navigation

- **WHEN** 竖屏手势导航下打开带顶部或底部玻璃 Chrome 的页面
- **THEN** Chrome 不覆盖状态栏/手势区，列表末项可滚动到安全位置，且隐藏浮层后不会留下固定的底部空白

#### Scenario: Landscape and three-button navigation

- **WHEN** 横屏或三键导航模式下打开同一页面
- **THEN** surface 不重复增加系统栏间距，现有 NavigationRail/页面内容的 inset 和触控区域仍正确

### Requirement: Clear foreground interaction and accessibility

玻璃背景层 MUST 不抢占其子控件的点击、焦点和无障碍语义；可点击 surface 只能用于明确的独立动作并保留 content description。Toolbar、Tab、排序按钮、FAB 和搜索动作 MUST 保持原有 Material 点击、选中、键盘/辅助功能导航和至少 48dp 触控目标，不得通过玻璃包装改变语义。

#### Scenario: User activates a control inside a glass surface

- **WHEN** 用户点击、键盘聚焦或通过辅助功能激活玻璃 Toolbar/Tab/FAB 内的控件
- **THEN** 原有导航、排序、发布、搜索或返回动作发生一次，背景层不拦截事件且不会产生重复点击

#### Scenario: Surface is inspected by accessibility tools

- **WHEN** UI hierarchy 或辅助功能工具读取页面
- **THEN** backdrop 绘制层不作为额外可操作节点暴露，真实控件的文字、角色、content description、选中态和可聚焦边界仍可识别
