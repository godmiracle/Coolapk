## ADDED Requirements

### Requirement: API and device credentials are isolated from ordinary preferences

接口 Token、设备身份、设备级应用 Token 和自定义 Token MUST 存储在独立的 credentials preference 文件中，不得继续作为普通 UI 设置写入 `settings.xml`。

#### Scenario: Existing installation migrates sensitive keys

- **WHEN** 已有安装首次运行包含 credentials preference 迁移的版本
- **THEN** 旧 `settings` 中的敏感值被幂等复制到 credentials 文件，复制校验成功后旧敏感键被清理

#### Scenario: Repeated migration is safe

- **WHEN** credentials 迁移逻辑执行多次或中途重试
- **THEN** Token 和设备身份不会被覆盖为空，也不会产生重复状态

### Requirement: Credentials are excluded from system backup

credentials preference 文件 MUST 从云备份和设备迁移备份中排除；普通主题、字体等非敏感设置仍可按既有策略处理。

#### Scenario: Backup rules are inspected

- **WHEN** 构建产物读取 Android 备份规则
- **THEN** credentials 文件在 cloud backup 和 device transfer 两类规则中均被排除

#### Scenario: Restored installation regenerates or requests access state

- **WHEN** 应用从另一台设备恢复数据
- **THEN** 不会自动恢复旧设备的接口凭证或设备身份，应用可以重新生成或要求用户重新提供接口访问配置
