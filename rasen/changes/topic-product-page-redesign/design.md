## Context

`TopicActivity` 同时承载话题和数码详情。`TopicFragment` 目前复用 `BasePagerFragment` 的 Toolbar、TabLayout 和 ViewPager2，主题信息只显示在 Toolbar 标题/副标题中，关注动作位于 `topic_product_menu.xml` 菜单，讨论排序也位于菜单子项。

现有 `BasePagerFragment` 的 `CollapsingToolbarLayout` 含有一个供用户页、应用页替换的 `view` 占位；`TopicFragment` 可以复用这一占位注入自己的头部，不改变其他页面。`HomeFeedResponse.Data` 已包含 `logo`、`intro`、`follow_num`、`hot_num`、讨论数和 `userAction.follow` 等可选字段。`TopicViewModel` 已通过 `LocalFollowRepo` 实现本地优先关注。

## Goals / Non-Goals

**Goals:**

- 在话题/数码详情页首屏展示主题头像、名称、简介和可用统计信息。
- 把关注变成头部直接操作，立即反馈本地关注状态，并保留现有登录后服务端尽力同步。
- 在“讨论”Tab 下方提供“默认 / 最新 / 热度”单选切换。
- 切换排序时复用当前讨论 Tab 的原始 URL，只修改嵌套请求中的 `listType`，并重置现有分页状态。
- 保留动态列表、分页、黑名单过滤、Tab 切换和发布 FAB。

**Non-Goals:**

- 不新增服务端接口、不新增本地数据库表、不引入第三方依赖。
- 不硬编码截图中的规则卡片、关注者头像条或具体统计文案。
- 不在本次变更中给话题/数码目录列表增加行内快捷关注。
- 不改变应用页、用户页及其他 `BasePagerFragment` 子页面的菜单行为。

## Decisions

### 1. 复用共享页面骨架，注入主题专属头部

在 `base_tablayout_viewpager.xml` 增加默认隐藏的 AppBar 扩展容器，用于放置排序控件；保留现有 `view` 占位。`TopicFragment` 使用 `ReplaceViewHelper` 将 `base_view_topic.xml` 替换到 `view` 中。这样用户页和应用页仍可继续替换同一占位，主题头部的布局和状态只属于话题页。

备选方案是复制一套 Topic 专属 Pager 基类，放弃该方案，因为会重复 Toolbar、TabLayout、ViewPager2、滚动和 FAB 行为。

### 2. 关注状态以本地数据库为首要反馈来源

头部按钮直接调用 `TopicViewModel.toggleFollow()`。ViewModel 继续先写入/删除 `local_follow.db`，再根据登录状态尝试同步服务端；`followState` 驱动按钮更新。菜单中的关注项在 TopicFragment 中隐藏，但资源保留给 AppFragment 使用。

备选方案是等待服务端响应后再更新按钮，放弃该方案，因为当前项目已经明确本地关注不应被网络失败阻断。

### 3. 排序基于当前 Tab URL，不重建产品专用路由

`TopicContentViewModel` 保存创建时的原始 URL。默认排序使用原始 URL；最新和热度只在嵌套 URL 中添加或替换 `listType=dateline_desc`、`listType=rank_score`。切换排序重置 `page`、`lastItem`、`isEnd`、列表和加载状态，然后沿用 `BaseViewFragment` 的刷新/分页流程。

备选方案是继续在 Fragment 中拼接 `/product/feedList`，放弃该方案，因为它会把话题 Tab 误路由到产品接口。

### 4. 使用现有 MaterialButtonToggleGroup 风格

排序控件使用 XML 中已有的 `MaterialButtonToggleGroup` 和 `materialButtonOutlinedStyle`，设为单选且必须有选中项。默认选中“默认”，只在当前 Tab 标题为“讨论”时显示。

## Risks / Trade-offs

- [话题接口不接受 `listType`] → 保持请求 URL 与当前 Tab 一致；加入 URL 构造单元测试，并在真实设备/接口验证中记录话题排序是否可用。
- [头部字段缺失或为空] → 对每个统计字段做可选展示；缺失时隐藏对应文本，不显示 `null` 或伪造数据。
- [头部增加 AppBar 高度] → 头部内容放在现有 CollapsingToolbarLayout 中，并让内容列表继续使用 `appbar_scrolling_view_behavior`。
- [快速连续点击排序] → ToggleGroup 只处理选中事件，ViewModel 每次切换都会取消/覆盖当前分页状态；请求取消和错误展示继续由现有 NetworkRepo/BaseViewModel 处理。

## Migration Plan

1. 添加主题头部、排序控件和共享 AppBar 扩展容器。
2. 更新 TopicViewModel/TopicFragment/TopicContentViewModel 的状态和交互。
3. 增加 URL 映射和关注状态回归测试，执行 JVM 测试、Debug 构建和 lint。
4. 若真实接口验证发现话题排序不支持，只保留服务端可用选项并记录为后续待办；不回退一套错误的产品路由。

## Open Questions

- 当前真实话题“讨论”Tab 返回的 URL 是否接受 `listType=rank_score` 和 `listType=dateline_desc`，需要设备/实时接口验证。
