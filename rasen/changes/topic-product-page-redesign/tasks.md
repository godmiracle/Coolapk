## 1. 页面结构与资源

- [x] 1.1 在共享 `base_tablayout_viewpager.xml` 增加默认隐藏的 AppBar 扩展容器，确保其他 Pager 页面保持原布局。
- [x] 1.2 新增话题/数码主题头部布局，展示图标、标题、简介、可用统计和“关注 / 已关注”按钮。
- [x] 1.3 新增“默认 / 最新 / 热度”单选排序布局，复用现有 MaterialButtonToggleGroup 风格。

## 2. 主题详情交互

- [x] 2.1 将布局响应保存到 `TopicViewModel`，驱动主题头部字段和缺失字段隐藏。
- [x] 2.2 用主题头部按钮调用现有本地优先关注逻辑，并观察 `followState` 更新按钮文本和状态。
- [x] 2.3 在 `TopicFragment` 启用头部和排序控件，按当前 Tab 控制排序栏显示，隐藏 TopicFragment 内原有菜单排序/关注入口但保留 AppFragment 菜单行为。

## 3. 排序与分页

- [x] 3.1 为讨论排序建立默认/最新/热度状态，基于原始 Tab URL 添加或替换嵌套 `listType`，移除产品专用硬编码路由。
- [x] 3.2 切换排序时清空旧列表并重置分页字段，沿用现有刷新、加载、黑名单过滤和分页流程。
- [x] 3.3 为排序 URL 构造和切换重置补充 JVM 单元测试，覆盖默认、`dateline_desc`、`rank_score` 和已有 `listType`。

## 4. 验证与文档

- [x] 4.1 执行 `:app:testDebugUnitTest`、`:app:assembleDebug` 和 `:app:lintDebug`，修复本次变更引入的问题。
- [x] 4.2 在可用设备或 UI 层级证据条件满足时验证头部、关注状态、排序切换、分页重置和其他详情页菜单；无法验证的真实话题排序接口明确记录。
  - 设备已验证头部、直接关注和服务端“最近回复 → 最新发布”切换；最终 UI 层级确认统计完整显示、关注按钮和现有排序 Tab 可用，设备日志无崩溃。
  - 当前真实接口未提供明确“讨论”Tab，因此额外排序栏、对应 `listType` 请求和该分支的分页重置没有真实接口证据，已同步记录到 `docs/todo.md`、`docs/decisions.md` 和会话记录。
- [x] 4.3 按项目规范更新 `docs/todo.md`、必要的 `docs/decisions.md` 和本次开发会话记录。
