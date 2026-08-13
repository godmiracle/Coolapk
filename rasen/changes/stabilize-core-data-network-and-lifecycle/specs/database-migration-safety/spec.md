## ADDED Requirements

### Requirement: Room migrations preserve existing user data

每条从已发布版本到当前版本的 Room migration MUST 在改变表结构时复制所有兼容的已有行，不得无条件使用 destructive migration 或在复制前删除旧表；已经发布的 schema version 不得被重新解释。

#### Scenario: Feed favorites migrate from the legacy v1 schema

- **WHEN** 一个包含旧版收藏记录的 `feed_favorite.db` 从 v1 升级到当前版本
- **THEN** 升级后的 `FeedEntity` 表存在，收藏记录数量、`fid` 和 `id` 与旧记录一致，旧版不存在的用户/内容字段使用稳定的空字符串默认值

#### Scenario: Feed favorites migrate from the historical v2 table

- **WHEN** 一个 version 2 的数据库仍使用 `FeedFavorite` 表并包含完整收藏字段
- **THEN** 升级后的 `FeedEntity` 表保留所有兼容字段和记录，不再依赖 `FeedFavorite` 表名

#### Scenario: Feed favorites migrate from the current v2 table

- **WHEN** 一个 version 2 的数据库已经使用当前 `FeedEntity` 表
- **THEN** 升级到当前版本时数据保持不变且 schema identity 校验通过

#### Scenario: Home menu migration preserves custom settings

- **WHEN** 一个包含用户启用/禁用状态和顺序的旧版 `home_menu.db` 执行 2→3 migration
- **THEN** 原有菜单记录和启用状态保留，新字段获得确定且不冲突的值

#### Scenario: Recent mention migration preserves unique users

- **WHEN** 一个包含最近 @ 用户的旧版 `recent_at_user.db` 执行 1→2 migration
- **THEN** 每个可兼容用户仍存在，重复用户名按新表唯一约束幂等处理

### Requirement: Room migration tests validate the final schema

每条已注册 migration MUST 有 fixture 测试验证升级后的 Room identity hash、表名、列名、主键/索引和关键数据。

#### Scenario: Migration test runs without destructive fallback

- **WHEN** migration test 使用旧版 fixture 打开数据库并执行升级
- **THEN** 测试通过且未配置 destructive migration 作为缺省兜底
