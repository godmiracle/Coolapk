## ADDED Requirements

### Requirement: BasePagerFragment provides optional shared glass chrome slots

`BasePagerFragment` 及其 `base_tablayout_viewpager.xml` 共享壳 SHALL 提供可复用、可退出的 GlassSurface slot，用于折叠 Toolbar、TabLayout、可选 `extraBar` 和适用的 FAB。接入 MUST 保留现有 `CoordinatorLayout`、`AppBarLayout`、`CollapsingToolbarLayout`、collapse flags、TabLayoutMediator、ViewPager2 分页、滚动行为和动态 FAB 的业务接口；相邻的 Toolbar/Tab/extraBar SHOULD 合并为尽可能少的独立 surface。

#### Scenario: A standard BasePager page opens

- **WHEN** Topic、App、CoolPic、Carousel 或 Follow 等继承 `BasePagerFragment` 的页面创建 view
- **THEN** 共享壳按统一 token 显示适用的 Toolbar/Tab/extraBar glass chrome，内容页仍正常加载、分页、切换 Tab 和返回

#### Scenario: A derived page opts out of an unsuitable slot

- **WHEN** 派生页面的特殊头部、内容采样或布局测量不适合某个 glass slot
- **THEN** 页面可以通过明确的共享开关/配置保留普通表面，不复制私有玻璃实现，也不改变该页面的业务内容

#### Scenario: Optional action slots remain explicit

- **WHEN** BasePager 页面存在需要保留的操作浮层并滚动内容
- **THEN** 该操作控件可以明确退出 GlassSurface；本次匿名只读范围不保留发布 FAB，玻璃表面不覆盖列表末项

### Requirement: User page glassifies only profile Chrome

用户主页 SHALL 将 `base_user_page.xml`/`UserPagerFragment` 的折叠 Toolbar、返回/菜单动作及其稳定 Chrome 纳入共享 GlassSurface；用户资料头部、头像、统计内容和用户动态列表 MUST 保持普通内容渲染，不得把个人资料或 RecyclerView item 整体玻璃化。匿名只读范围保留搜索、分享、复制和本地拉黑，移除远程关注、举报和登录入口。

#### Scenario: User profile collapses while scrolling

- **WHEN** 用户从展开的资料头部向上滚动到折叠 Toolbar
- **THEN** Toolbar glass surface 与内容背景对齐并保持标题/菜单可读，折叠高度、滚动位置和用户资料内容不发生跳变

#### Scenario: User menu actions remain accessible

- **WHEN** 用户打开用户页菜单并触发允许的只读或本地动作
- **THEN** 搜索、分享、复制、本地拉黑和返回动作保持可用，surface 不吞掉菜单点击

### Requirement: Feed detail glassifies navigation only

Feed 详情 SHALL 只将 `fragment_feed.xml`/`FeedFragment` 的顶部 Toolbar 纳入共享 GlassSurface。排序栏必须使用普通不透明背景，不得以 `RecyclerView`（包括回复内容）作为 backdrop source；Feed 正文、评论/楼中楼行、图片、头像、加载/错误状态和回复编辑内容 MUST 不变为玻璃 item。排序切换、列表刷新、分页和滚动行为 MUST 保持只读浏览语义；本次范围不提供发布或回复入口。

#### Scenario: Feed toolbar samples a moving feed

- **WHEN** 用户打开 Feed 详情并滚动正文/评论列表
- **THEN** Toolbar glass surface 随内容刷新但标题、用户信息、菜单和返回动作保持清晰，RecyclerView item 没有新增 blur layer

#### Scenario: Sticky sort chrome remains readable

- **WHEN** Feed 列表达到现有 `StickyItemDecorator` 的排序栏显示条件
- **THEN** 排序栏以普通不透明表面显示并保留“默认/最新/热门/楼主”按钮的选中和刷新行为；它不采样或模糊下方回复，未达到条件时不占用内容高度

#### Scenario: Reply content stays crisp

- **WHEN** 用户在 Feed 详情滚动正文或回复列表
- **THEN** 回复行和楼中楼文字、图片、头像使用普通清晰渲染，不出现 GlassSurface backdrop、RenderEffect 模糊或回复编辑入口

### Requirement: Other page Chrome expansion remains limited and content-backed

首轮其他页面 Chrome SHALL 只覆盖搜索页的顶部表面：`fragment_search.xml`/`SearchFragment` 的搜索 Toolbar。消息/登录页面不属于当前匿名只读产品范围；历史、设置 Preference、WebView、BottomSheet 和 Dialog 不得因为“全面覆盖”而被自动玻璃化，若 source 不安全则保留普通表面并记录原因。

#### Scenario: Search page keeps its input and history behavior

- **WHEN** 用户打开 SearchActivity、输入/清除关键词、执行搜索或点击历史项
- **THEN** 搜索 Toolbar 的编辑框、清除/搜索按钮、返回和结果路由保持原行为，glass surface 不覆盖 IME 焦点或历史列表触控

#### Scenario: Removed account surfaces are not reachable

- **WHEN** 用户在匿名只读应用中浏览主导航和内容详情
- **THEN** 不出现登录、消息、发布、回复或远程社交操作入口，也不保留对应不可达编译路径

#### Scenario: Cross-window surfaces remain explicitly out of scope

- **WHEN** 用户打开 Reply2Reply BottomSheet、Material dialog、Settings Preference 或 WebViewActivity
- **THEN** 本 change 不从 Activity 内 View 直接采样这些 Window 的背景；页面继续使用已有普通/专用主题，并将是否需要跨 Window 材质留给后续专项设计

### Requirement: List content and existing navigation structure are preserved

本 change MUST 只改变页面 Chrome 的表面呈现，不得把普通 RecyclerView card、评论行、头像、封面、空/错误/加载状态或 Preference 行包进 GlassSurface。横屏主导航继续使用现有 NavigationRailView；ViewModel、Adapter、NetworkRepo、Room、深链和 API 不得因玻璃接入改变。

#### Scenario: Long list remains ordinary content

- **WHEN** 用户在首页、Feed、用户、消息或搜索结果中持续滚动长列表
- **THEN** 列表 item 的绑定、复用、分页和点击行为保持原有路径，玻璃层数量只来自可见 Chrome/浮层而不是每个 item

#### Scenario: Existing navigation and data paths are exercised

- **WHEN** 用户在主导航、BasePager Tab、用户菜单、Feed 排序、搜索和本地功能之间操作
- **THEN** 只读 Fragment/Activity 导航、网络请求、列表状态和本地数据行为保持一致，变更不引入新的服务端写入或持久化契约
