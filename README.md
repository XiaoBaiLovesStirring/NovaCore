# NovaCore 2.0 — Mixin 性能优化引擎

Minecraft 1.12.2 Forge 极致性能优化模组，基于 **Mixin 字节码注入** + **JVM Agent 底层调控** + **C++ 原生线程调度**。

## 架构

```
NovaCore 2.0
├── Mixin 注入层 (12 个 @Mixin)
│   ├── STANDARD (6): 光照 / 区块 / 内存 / 剔除 / 数学 / OpenGL
│   └── EXTREME (+3): Tick降频 / 粒子限制 / 渲染激进
├── GUI 配置 (IModGuiFactory)
│   └── 游戏内可视化配置 STANDARD/EXTREME/CUSTOM
├── Java Agent (NovaCoreAgent.jar)
│   └── JVM GC 调优 / 线程亲和性 / 多核线程池 / 内存预分配
└── Native 库 (libnovacore_native.so / .dll)
    └── 线程亲和性 / 调度优先级 / GC 调控 / 大页内存
```

## 构建

```bash
# 构建 NovaCore 模组
./gradlew build

# 构建 Java Agent JAR
./gradlew buildAgentJar

# 编译原生库 (需要 g++ 和 JAVA_HOME)
cd native
bash build.sh       # Linux
build.bat           # Windows
```

## 安装

### 1. 安装 NovaCore 模组

将 `build/libs/NovaCore-2.0.0-mixin.jar` 放入 `mods/` 目录。

### 2. 安装 Java Agent (可选，JVM 底层优化)

将 `build/libs/NovaCoreAgent-1.0.0-agent.jar` 放入 Minecraft 根目录。

### 3. 安装原生库 (可选，配合 Agent)

将编译好的 `libnovacore_native.so` (Linux) 或 `novacore_native.dll` (Windows) 放入：
- 与 Agent JAR 同级目录
- 或任意 `java.library.path` 路径

## 启动参数

### 基础启动 (仅模组)

```
--tweakClass org.spongepowered.asm.launch.MixinTweaker
```

### 完整启动 (模组 + Agent + 原生库)

```bash
java \
  -javaagent:NovaCoreAgent-1.0.0-agent.jar=gcThreads=4;maxPauseMillis=50;preallocateMemoryMB=512 \
  -Dfml.coreMods.load=com.novacore.NovaCorePlugin \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=50 \
  -XX:ParallelGCThreads=4 \
  -XX:ConcGCThreads=2 \
  -XX:+UseStringDeduplication \
  -XX:+UseNUMA \
  -XX:+AlwaysPreTouch \
  -jar forge-1.12.2-14.23.5.2864-universal.jar \
  --tweakClass org.spongepowered.asm.launch.MixinTweaker \
  nogui
```

### Agent 参数说明

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `gcThreads` | 4 | GC 并行线程数 |
| `maxPauseMillis` | 50 | 最大 GC 停顿 (ms) |
| `heapOccupancyPercent` | 45 | 堆占用触发 GC 阈值 |
| `parallelGCThreads` | 4 | Parallel GC 线程数 |
| `concGCThreads` | 2 | 并发 GC 线程数 |
| `preallocateMemoryMB` | 512 | 预分配堆外内存 (MB) |
| `poolCores` | 3 | 线程池绑定核心数 |
| `mainThreadCoreMask` | 0x1 | 主线程核心掩码 (十六进制) |

### 游戏内配置

启动后进入 **Mods → NovaCore → Config**，可切换：
- **STANDARD** — 6 模块稳定运行
- **EXTREME** — +3 模块极限性能
- **CUSTOM** — 手动逐项开关

## Manifest 配置

模组 JAR 的 `META-INF/MANIFEST.MF`：

```
Manifest-Version: 1.0
FMLCorePlugin: com.novacore.NovaCorePlugin
FMLCorePluginContainsFMLMod: true
ForceLoadAsMod: true
MixinConfigs: mixins.novacore.json
TweakClass: org.spongepowered.asm.launch.MixinTweaker
TweakOrder: 0
```

## 项目结构

```
src/main/java/com/novacore/
├── NovaCorePlugin.java          # Coremod 入口
├── NovaCoreConfig.java          # 运行时配置
├── NovaCoreContainer.java       # 模组容器
├── asm/                         # 原始 ASM Helper (保留兼容)
│   ├── NovaMathHelper.java
│   ├── NovaLightEngine.java
│   ├── NovaChunkIO.java
│   ├── NovaCullingHelper.java
│   ├── NovaGLHelper.java
│   ├── NovaMemoryHelper.java
│   ├── NovaTickRateHelper.java
│   ├── NovaParticleLimiter.java
│   └── NovaRenderAggression.java
├── mixin/                       # Mixin 注入层 (新)
│   ├── NovaMixinPlugin.java
│   ├── MixinMathHelper.java
│   ├── MixinWorld_Lighting.java
│   ├── MixinAnvilChunkLoader.java
│   ├── MixinPlayerChunkMapEntry.java
│   ├── MixinWorldServer.java
│   ├── MixinFakePlayer.java
│   ├── MixinChunk.java
│   ├── MixinRenderGlobal_EntityCulling.java
│   ├── MixinRenderGlobal_OpenGL.java
│   ├── MixinWorld_TickRate.java
│   ├── MixinEntity_TickRate.java
│   ├── MixinTileEntity_TickRate.java
│   ├── MixinParticleManager.java
│   └── MixinRender_Shadows.java
├── gui/                         # GUI 配置界面
│   ├── NovaCoreGuiFactory.java
│   └── NovaCoreGuiConfig.java
└── agent/                       # Java Agent
    ├── NovaAgent.java
    ├── NativeLoader.java
    └── NativeThreadAffinity.java

native/                          # C++ 原生库
├── novacore_native.h
├── novacore_native.cpp
├── build.sh
├── build.bat
└── CMakeLists.txt
```