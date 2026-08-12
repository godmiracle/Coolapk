# `src/` 目录说明

当前项目的生产 Android 源码不在根目录 `src/`，而在：

```text
app/src/main/java/com/example/c001apk/
app/src/main/res/
app/src/test/
app/src/androidTest/
```

根目录 `src/` 目前只是文档占位目录，不应在这里新增业务代码。新增 Android 功能应根据职责放入 `app` 的 `ui`、`logic`、`adapter`、`util` 或 `view` 包，并同步检查对应的资源、Manifest、测试和项目文档。
