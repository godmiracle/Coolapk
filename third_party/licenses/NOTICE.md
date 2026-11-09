# 第三方底栏代码与许可证

本项目的 `app/src/main/java/com/godmiracle/coolapk/view/ksu/` 使用并适配了以下上游代码：

- [KernelSU](https://github.com/tiann/KernelSU/tree/c680b548f3fe5c19f51e90ce97d6f0ef89594421/manager/app/src/main/java/me/weishu/kernelsu/ui/component)：`FloatingBottomBar`、`DampedDragAnimation`、`InteractiveHighlight` 和手势检查器，基于提交 `c680b548f3fe5c19f51e90ce97d6f0ef89594421`，上游项目采用 GPL-3.0；许可证全文见 [KernelSU-GPL-3.0.txt](KernelSU-GPL-3.0.txt)。
- KernelSU 玻璃效果辅助文件声明改编自 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 和 [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) 示例，采用 Apache-2.0；保留原文件中的来源声明。
- `InverseLayerScope.kt` 来自 [Miuix 0.9.4](https://github.com/compose-miuix-ui/miuix/tree/v0.9.4/miuix-blur)，保留原始版权和 SPDX 声明，采用 Apache-2.0；许可证全文见 [Apache-2.0.txt](Apache-2.0.txt)。

Miuix Blur 与 Miuix UI 作为 Apache-2.0 依赖引入。项目本身的许可证见根目录 [LICENSE.md](../../LICENSE.md)。
