## Why

话题和数码详情页当前使用通用的 Toolbar + TabLayout 结构，“关注”和“排序规则”都藏在菜单中，用户需要额外操作才能完成核心动作。需要把详情页改成以主题信息、关注和内容排序为中心的页面，让关注状态首屏可见、讨论排序可直接切换。

## What Changes

- 为话题和数码详情页增加可折叠的主题头部，展示图标、名称、简介和可用统计信息。
- 将“关注 / 已关注”改为主题头部中的直接操作，沿用本地优先、未登录可用的关注逻辑。
- 在“讨论”内容区域直接展示“默认 / 最新 / 热度”排序切换，不再要求打开 Toolbar 菜单。
- 切换排序时重置分页游标和列表，避免不同排序结果混合。
- 保留现有服务端 Tab、动态列表、分页、黑名单过滤和发布入口。
- 不在本次范围内硬编码截图中的规则卡片或关注者头像数据。

## Capabilities

### New Capabilities

- `topic-product-page`: 话题和数码详情页的主题头部、直接关注和讨论排序交互。

### Modified Capabilities

<!-- 当前 rasen/specs/ 没有可复用的既有能力规格。 -->

## Impact

- 影响 `ui/topic` 的 Fragment、ViewModel 和 XML 布局，以及话题/数码详情的排序菜单资源。
- 复用 `LocalFollowRepo`、现有 `TopicContentViewModel` 分页链路和 `HomeFeedResponse.Data` 中已有的 logo、intro、统计字段。
- 需要确认话题讨论 Tab 是否支持与数码相同的服务端排序参数；不能把产品 `/product/feedList` 路由直接用于话题。
- 不新增第三方依赖，不改变本地关注数据库结构。
