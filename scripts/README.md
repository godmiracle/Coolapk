# 辅助脚本说明

当前仓库没有项目自定义脚本；构建和测试入口由 Gradle Wrapper 与 GitHub Actions 提供：

- 本地构建：`./gradlew :app:assembleDebug`、`./gradlew :app:assembleRelease`
- 本地测试：`./gradlew :app:testDebugUnitTest`、`./gradlew :app:connectedDebugAndroidTest`
- CI：`.github/workflows/ci.yml`

如果新增脚本，请说明输入、输出、是否联网、是否读取敏感配置以及失败条件，并同步到 `docs/development.md`。不要将签名密码、Cookie、Token 或本地绝对路径写入脚本。
