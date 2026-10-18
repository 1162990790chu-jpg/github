# 加速苹果 / Speed Apple

给 Minecraft Java 版 **26.2 + Fabric Loader 0.19.3** 制作的私人试玩模组。

- 新物品：加速苹果，使用原版苹果外观。
- 吃完后获得 **速度Ⅰ，持续 200 游戏刻（正常游戏速度下为 10 秒）**。
- 恢复 4 点饥饿值；饥饿值满时也可以吃。
- 苹果 × 1 + 糖 × 1，无序合成加速苹果 × 1。
- 在创造模式的食物栏中也能找到。

## 下载

- [下载试玩包](加速苹果-26.2-Fabric试玩包.zip?raw=true)：包含模组、Fabric API 和安装说明。
- [仅下载模组 JAR](dist/speedapple-1.0.0+mc26.2.jar?raw=true)：已经安装匹配版本 Fabric API 时使用。

模组已通过 Gradle 编译检查，尚未在游戏内进行食用测试。

## 在 PCL2 中游玩

1. 选择你的 **26.2 / Fabric 0.19.3** 游戏实例。
2. 打开该实例实际使用的游戏文件夹，将本项目 `dist` 中的模组 `.jar` 放入 `mods`。
3. 同时需要与 Minecraft **26.2** 匹配的 **Fabric API**。如果已经安装对应版本，就保留现有文件，不要重复放入多个版本。
4. 启动游戏，用新存档先试玩。苹果与糖放进合成栏即可制作。
5. 开启作弊的测试存档还可以输入：`/give @s speedapple:speed_apple 16`。

## 在 MCreator 中继续修改

先下载或克隆整个仓库，保留 `speed-apple` 文件夹中的全部项目文件。安装 MCreator 2026.2 和下方的 Fabric 26.2 社区生成器后，在 MCreator 中打开 `speedapple.mcreator`。

Windows 下，如果 MCreator 安装在默认的用户程序目录，也可以双击 `打开项目.cmd`。

项目包含三个可编辑元素：

| 元素 | 用途 |
| --- | --- |
| `SpeedApple` | 苹果的名称、食物属性、提示和食用触发器 |
| `SpeedAppleEaten` | 食用后添加速度效果的可视化流程 |
| `SpeedAppleRecipe` | 苹果加糖的无序合成配方 |

在 `SpeedAppleEaten` 中，持续时间 **200** 的单位是游戏刻；效果等级 **0** 对应速度Ⅰ。

修改后使用 MCreator 的导出功能重新生成模组。游戏版本、加载器和生成器要保持一致。

## 开发环境

- MCreator：2026.2.33518，使用官方发行包和自带的 Java 25。
- [Fabric 26.2 社区生成器](https://github.com/CamCam5701/Fabric-26.2-Generator)，使用 `2026.2` 分支的生成器。
- Fabric Loader：0.19.3。
- Fabric API：0.155.2+26.2。

项目通过 MCreator 自己的数据模型和生成器创建，物品、流程和配方没有锁定为手写代码。
`tools/CreateSpeedApple.java` 是首次创建工作区的辅助源码，日常修改不需要运行它。

首次初始化工作区需要下载 Minecraft 和 Fabric 开发依赖。网络受限时，请按自己的网络环境配置 Gradle 代理。

试玩包中的 Fabric API 来自 [Fabric 官方 Maven 仓库](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.155.2+26.2/)，由 [FabricMC](https://github.com/FabricMC/fabric) 项目提供；其许可证包含在原始 JAR 中。
