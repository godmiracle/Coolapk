## ADDED Requirements

### Requirement: Native build and resource validation covers every new surface

实现 SHALL 对共享 GlassSurface、所有新增/改造 XML、主题 token、drawable 和 Binding 接入执行资源编译、Kotlin 编译、Debug 构建及适用的单元/资源/Lint 检查。验证记录 MUST 区分源码存在、构建通过、模拟器验证和真实设备验证，不得用截图替代编译或行为证据。

#### Scenario: Clean debug validation runs

- **WHEN** implementer 在项目规定的 JDK/SDK 环境执行最小 Debug 构建和相关测试/Lint
- **THEN** 新增资源、ViewBinding/Data Binding、API 31+ 调用和主题引用均通过，失败项明确指向文件和原因

#### Scenario: Existing glass pages regress

- **WHEN** 验证首页、发现页和主导航的既有玻璃 surface
- **THEN** 页面仍能冷启动、切换、搜索、滚动隐藏/恢复，且不出现资源缺失、重复背景或 Material 默认不透明矩形

### Requirement: Device matrix validates platform, theme, font, and orientation behavior

至少 SHALL 在一个 Android 12+ API 31/32 环境和一个当前 Android 真机或等价设备上验证新增 Chrome；并覆盖浅色、深色/动态主题、竖屏、横屏、默认字体和放大字体。验证 MUST 观察 BasePager、用户页、Feed、搜索和消息的冷启动、返回、Tab/菜单/排序/回复/搜索动作及滚动状态。

#### Scenario: API 31/32 receives a supported native result

- **WHEN** 在 API 31 或 32 设备打开每个新增 surface 并滚动其 source
- **THEN** surface 有稳定的模糊或着色回退，前景可读，无 NewApi 崩溃、黑块、递归重绘或触控失效

#### Scenario: Current Android device receives the enhanced result

- **WHEN** 在当前 Android 真机/模拟器上以同一 Debug 包重复上述页面和主题流程
- **THEN** API 33+ 可使用增强效果但不改变布局契约，所有页面动作和截图/UI hierarchy 边界与预期一致

#### Scenario: Enlarged text and landscape preserve layout

- **WHEN** 用户提高字体比例并旋转到横屏，再返回竖屏
- **THEN** Toolbar/Tab/排序栏/FAB 不裁剪、不遮挡系统栏、不产生永久空白；横屏 NavigationRail 和页面内容维持既有结构

### Requirement: Readability, touch, and accessibility are explicitly accepted

验收 SHALL 逐项检查玻璃背景上的标题、Tab、菜单、排序按钮、搜索输入、FAB 图标、空/错误状态和系统返回控件。文字和图标 MUST 在浅色、深色和动态背景下保持清晰；真实控件 MUST 保留 content description、选中态、焦点态和至少 48dp 触控区域。Backdrop 不得被 UI hierarchy 误报为可操作控件。

#### Scenario: Light/dark screenshots remain readable

- **WHEN** 在浅色、深色/动态主题下截取 BasePager、用户、Feed、搜索和消息页面
- **THEN** 标题、Tab、菜单、排序、搜索和 FAB 前景与玻璃背景有明确层次，未出现白字叠白底、黑字叠黑底或高光遮字

#### Scenario: Accessibility hierarchy remains semantic

- **WHEN** 使用 UI hierarchy、TalkBack/辅助功能检查或等价工具查看新增页面
- **THEN** 只暴露真实按钮、输入框、Tab 和列表节点；返回、菜单、排序、回复和搜索动作可以被定位和执行一次

### Requirement: Insets and scroll safety are measured on device

验收 SHALL 在手势导航和三键导航（可用时）下检查状态栏顶部、导航栏底部、FAB/排序浮层 margin、列表末项滚动位置、底栏隐藏/恢复以及页面返回。通过标准是：没有重复 inset、surface 不越过系统安全区、隐藏 Chrome 后内容不被固定截短且末项仍可安全到达。

#### Scenario: Top chrome respects status-bar inset

- **WHEN** 打开用户页、BasePager、Feed、搜索或消息页并观察展开/折叠 Toolbar
- **THEN** Toolbar 和玻璃边缘位于预期状态栏安全区内，折叠/返回不发生跳动或双倍顶部间距

#### Scenario: Bottom actions respect navigation-bar inset

- **WHEN** 滚动 Feed/详情页并显示回复或发布 FAB，或滚动主页面隐藏/恢复底栏
- **THEN** 操作按钮和浮岛与手势/导航栏保持安全距离，隐藏后列表可继续到末项，不留下固定空白层

### Requirement: Scroll performance has a reproducible baseline gate

性能验收 SHALL 在同一设备、同一构建类型、同一内容/滚动路径下分别记录改造前基线和改造后结果，至少三次连续滚动，并保存 `dumpsys gfxinfo <package> framestats` 或等价帧统计。若 janky frame 比例或中位帧耗时相对基线增加超过 10%，或出现持续掉帧、触摸延迟、ANR/崩溃，则该页面 MUST 合并 surface、降低 blur/alpha 或回退为普通表面后重新测量。

#### Scenario: Shared surface passes the frame budget comparison

- **WHEN** 在有长列表的 BasePager、Feed、消息或搜索页面完成三次基线/改造滚动采样
- **THEN** 原始帧统计显示 janky frame 比例和中位帧耗时均未超过基线 10% 回归，且没有 ANR、崩溃或明显触控延迟

#### Scenario: Performance gate fails and scope is reduced

- **WHEN** 某页面因多个独立模糊层使帧统计超过门槛
- **THEN** implementer 优先合并相邻 Chrome、减少 surface 数量、降低效果或关闭该页面 slot，并记录前后数据，不以“全面玻璃化”压过性能风险

### Requirement: Unverified cross-window behavior is reported honestly

BottomSheet、Dialog、Settings Preference 和 WebViewActivity 的跨 Window 玻璃效果 SHALL 明确标记为本 change 未实现/未验证，验收报告 MUST 不把普通表面或静态代码检查描述为跨 Window blur 已完成。

#### Scenario: Out-of-scope surface is exercised

- **WHEN** 验收人员打开 Reply2Reply BottomSheet、Material dialog、设置或 WebView
- **THEN** 记录其仍使用既有主题/普通表面及未验证原因，不要求其伪造 Activity 内 backdrop，也不阻塞本 change 的页面 Chrome 验收
