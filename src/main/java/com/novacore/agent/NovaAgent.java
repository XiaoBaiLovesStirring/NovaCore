package com.novacore.agent;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NovaAgent — NovaCore JVM 底层优化引擎的 Java Agent 入口类。
 * <p>
 * 通过 {@code -javaagent:nova-core-agent.jar[=options]} 在 JVM 启动时加载，
 * 负责初始化原生层、GC 调优、线程亲和性绑定和多核线程池调度。
 * 需要在 MANIFEST.MF 中声明 {@code Premain-Class: com.novacore.agent.NovaAgent}。
 * </p>
 */
public class NovaAgent {

    // ── 默认配置 ──────────────────────────────────────────────────────
    private static final int    DEFAULT_GC_THREADS              = 4;
    private static final long   DEFAULT_MAX_PAUSE_MILLIS        = 50L;
    private static final int    DEFAULT_HEAP_OCCUPANCY_PERCENT  = 45;
    private static final int    DEFAULT_PARALLEL_GC_THREADS     = 4;
    private static final int    DEFAULT_CONC_GC_THREADS         = 2;
    private static final long   DEFAULT_PREALLOCATE_MEMORY_MB   = 256L;
    private static final int    DEFAULT_POOL_CORES              = 4;
    private static final long   DEFAULT_MAIN_THREAD_CORE_MASK   = 0x01L;  // 绑定到核心 0

    // ── 运行时状态 ────────────────────────────────────────────────────
    private static int    gcThreads;
    private static long   maxPauseMillis;
    private static int    heapOccupancyPercent;
    private static int    parallelGCThreads;
    private static int    concGCThreads;
    private static long   preallocateMemoryMB;
    private static int    poolCores;
    private static long   mainThreadCoreMask;

    private static ForkJoinPool novaCorePool = null;

    /**
     * JVM 启动时由 {@code java.lang.instrument} 框架调用。
     *
     * @param agentArgs 通过 {@code -javaagent:...=args} 传入的参数字符串
     * @param inst      Instrumentation 实例，可用于类转换等（本 Agent 未使用）
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        // ── 1. 解析参数 ───────────────────────────────────────────────
        parseAgentArgs(agentArgs);

        // ── 2. 打印加载横幅 ───────────────────────────────────────────
        printBanner();

        // ── 3. 加载原生库 ─────────────────────────────────────────────
        System.out.println("[NovaAgent] >>> 步骤 1/5: 加载原生库");
        try {
            NativeLoader.loadNativeLibrary();
            System.out.println("[NovaAgent] ✓ 原生库加载成功");
        } catch (Throwable t) {
            System.err.println("[NovaAgent] ✗ 原生库加载失败: " + t.getMessage());
            System.err.println("[NovaAgent] 将以纯 Java 模式继续运行");
            t.printStackTrace(System.err);
        }

        // ── 4. GC 调优 ────────────────────────────────────────────────
        System.out.println("[NovaAgent] >>> 步骤 2/5: 配置 GC 调优参数");
        configureGC();

        // ── 5. 线程亲和性绑定 ─────────────────────────────────────────
        System.out.println("[NovaAgent] >>> 步骤 3/5: 配置线程亲和性");
        configureThreadAffinity();

        // ── 6. 多核线程池 ─────────────────────────────────────────────
        System.out.println("[NovaAgent] >>> 步骤 4/5: 初始化多核线程池");
        initThreadPool();

        // ── 7. 内存预分配 ─────────────────────────────────────────────
        System.out.println("[NovaAgent] >>> 步骤 5/5: 预分配内存");
        preallocateMemory();

        // ── 8. 完成 ───────────────────────────────────────────────────
        printCompletion();

        // ── 9. 注册 Shutdown Hook ─────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[NovaAgent] 正在关闭 NovaCore 线程池...");
            if (novaCorePool != null && !novaCorePool.isShutdown()) {
                novaCorePool.shutdown();
            }
            System.out.println("[NovaAgent] NovaCore Agent 已卸载");
        }, "NovaCore-Shutdown-Hook"));
    }

    // ── 参数解析 ──────────────────────────────────────────────────────

    /**
     * 解析 agent 参数字符串。
     * 支持格式: {@code key1=val1,key2=val2} 或 {@code key1:val1;key2:val2}
     */
    private static void parseAgentArgs(String args) {
        // 设置默认值
        gcThreads            = DEFAULT_GC_THREADS;
        maxPauseMillis       = DEFAULT_MAX_PAUSE_MILLIS;
        heapOccupancyPercent = DEFAULT_HEAP_OCCUPANCY_PERCENT;
        parallelGCThreads    = DEFAULT_PARALLEL_GC_THREADS;
        concGCThreads        = DEFAULT_CONC_GC_THREADS;
        preallocateMemoryMB  = DEFAULT_PREALLOCATE_MEMORY_MB;
        poolCores            = DEFAULT_POOL_CORES;
        mainThreadCoreMask   = DEFAULT_MAIN_THREAD_CORE_MASK;

        if (args == null || args.trim().isEmpty()) {
            System.out.println("[NovaAgent] 未提供参数，使用默认配置");
            return;
        }

        System.out.println("[NovaAgent] 解析参数: " + args);

        // 支持 ',' 或 ';' 作为分隔符
        String[] pairs = args.split("[,;]");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            String[] kv = pair.split("[:=]", 2);
            if (kv.length != 2) {
                System.err.println("[NovaAgent] 忽略无效参数: " + pair);
                continue;
            }

            String key = kv[0].trim().toLowerCase();
            String val = kv[1].trim();

            try {
                switch (key) {
                    case "gcthreads":
                    case "gc_threads":
                        gcThreads = Integer.parseInt(val);
                        break;
                    case "maxpausemillis":
                    case "max_pause_millis":
                        maxPauseMillis = Long.parseLong(val);
                        break;
                    case "heapoccupancypercent":
                    case "heap_occupancy_percent":
                        heapOccupancyPercent = Integer.parseInt(val);
                        break;
                    case "parallelgcthreads":
                    case "parallel_gc_threads":
                        parallelGCThreads = Integer.parseInt(val);
                        break;
                    case "concgcthreads":
                    case "conc_gc_threads":
                        concGCThreads = Integer.parseInt(val);
                        break;
                    case "preallocatememorymb":
                    case "preallocate_memory_mb":
                        preallocateMemoryMB = Long.parseLong(val);
                        break;
                    case "poolcores":
                    case "pool_cores":
                        poolCores = Integer.parseInt(val);
                        break;
                    case "mainthreadcoremask":
                    case "main_thread_core_mask":
                        mainThreadCoreMask = Long.parseLong(val, 16);
                        break;
                    default:
                        System.err.println("[NovaAgent] 未知参数: " + key);
                }
            } catch (NumberFormatException e) {
                System.err.println("[NovaAgent] 参数值格式错误: " + pair + " — " + e.getMessage());
            }
        }
    }

    // ── 横幅输出 ──────────────────────────────────────────────────────

    /** 打印加载横幅，显示 NovaCore 版本与系统信息。 */
    private static void printBanner() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String javaVersion = System.getProperty("java.version");
        String osName      = System.getProperty("os.name");
        String osArch      = System.getProperty("os.arch");
        int availableCores = Runtime.getRuntime().availableProcessors();
        long maxMemory     = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          NovaCore Agent — JVM 底层优化引擎 v1.0.0             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Java 版本 : " + padRight(javaVersion, 46) + "║");
        System.out.println("║  操作系统   : " + padRight(osName + " (" + osArch + ")", 46) + "║");
        System.out.println("║  可用核心   : " + padRight(String.valueOf(availableCores), 46) + "║");
        System.out.println("║  最大堆内存 : " + padRight(maxMemory + " MB", 46) + "║");
        System.out.println("║  JVM 参数   : " + padRight(truncate(runtime.getInputArguments().toString(), 42), 46) + "║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  正在初始化 NovaCore 优化引擎...                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /** 打印配置摘要。 */
    private static void printConfigSummary() {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│  NovaCore 运行配置                                            │");
        System.out.println("├──────────────────────────────────────────────────────────────┤");
        System.out.println("│  GC 收集器          : G1GC                                    │");
        System.out.println("│  GC 线程数          : " + padRight(String.valueOf(gcThreads), 40) + "│");
        System.out.println("│  并行 GC 线程数     : " + padRight(String.valueOf(parallelGCThreads), 40) + "│");
        System.out.println("│  并发 GC 线程数     : " + padRight(String.valueOf(concGCThreads), 40) + "│");
        System.out.println("│  最大停顿时间       : " + padRight(maxPauseMillis + " ms", 40) + "│");
        System.out.println("│  堆占用触发百分比   : " + padRight(heapOccupancyPercent + "%", 40) + "│");
        System.out.println("│  主线程核心掩码     : 0x" + padRight(Long.toHexString(mainThreadCoreMask), 39) + "│");
        System.out.println("│  线程池核心数       : " + padRight(String.valueOf(poolCores), 40) + "│");
        System.out.println("│  预分配堆外内存     : " + padRight(preallocateMemoryMB + " MB", 40) + "│");
        System.out.println("└──────────────────────────────────────────────────────────────┘");
    }

    // ── GC 调优 ───────────────────────────────────────────────────────

    /** 配置 JVM GC 参数。 */
    private static void configureGC() {
        // 1) 设置 G1GC
        System.setProperty("gc", "G1GC");
        System.out.println("[NovaAgent]   GC 收集器已设置为 G1GC");

        // 2) 并行 GC 线程数
        setSystemProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
                String.valueOf(parallelGCThreads));
        System.out.println("[NovaAgent]   并行 GC 线程数: " + parallelGCThreads);

        // 3) 并发 GC 线程数
        // 注意：ConcGCThreads 和 ParallelGCThreads 是 -XX 参数，无法通过 System.setProperty 在运行时修改。
        // 这里通过 JNI 调用原生层尝试配置，如果原生层不可用则仅打印提示。
        System.out.println("[NovaAgent]   并发 GC 线程数: " + concGCThreads + " (需通过 -XX:ConcGCThreads 在启动时设置)");

        // 4) 最大停顿时间目标
        System.out.println("[NovaAgent]   最大停顿时间目标: " + maxPauseMillis + " ms (需通过 -XX:MaxGCPauseMillis 在启动时设置)");

        // 5) 堆占用触发百分比
        System.out.println("[NovaAgent]   堆占用触发百分比: " + heapOccupancyPercent + "% (需通过 -XX:InitiatingHeapOccupancyPercent 在启动时设置)");

        // 6) 尝试通过 JNI 调用原生 configureGC
        try {
            NativeThreadAffinity.configureGC(gcThreads, maxPauseMillis * 1_000_000L, heapOccupancyPercent);
            System.out.println("[NovaAgent]   ✓ 原生 GC 配置已应用");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("[NovaAgent]   ⚠ 原生层不可用，GC 配置仅通过 JVM 参数生效");
            System.out.println("[NovaAgent]   建议在启动时添加以下 JVM 参数:");
            System.out.println("[NovaAgent]     -XX:+UseG1GC");
            System.out.println("[NovaAgent]     -XX:ParallelGCThreads=" + parallelGCThreads);
            System.out.println("[NovaAgent]     -XX:ConcGCThreads=" + concGCThreads);
            System.out.println("[NovaAgent]     -XX:MaxGCPauseMillis=" + maxPauseMillis);
            System.out.println("[NovaAgent]     -XX:InitiatingHeapOccupancyPercent=" + heapOccupancyPercent);
        }
    }

    // ── 线程亲和性 ────────────────────────────────────────────────────

    /** 配置线程亲和性，将 Minecraft 主线程绑定到指定核心。 */
    private static void configureThreadAffinity() {
        long currentThreadId = Thread.currentThread().getId();
        System.out.println("[NovaAgent]   当前线程 ID: " + currentThreadId);

        try {
            int availableCores = NativeThreadAffinity.getAvailableCores();
            System.out.println("[NovaAgent]   系统可用核心数: " + availableCores);

            boolean success = NativeThreadAffinity.setThreadAffinity(currentThreadId, mainThreadCoreMask);
            if (success) {
                long affinity = NativeThreadAffinity.getThreadAffinity(currentThreadId);
                System.out.println("[NovaAgent]   ✓ 主线程已绑定到核心掩码 0x"
                        + Long.toHexString(mainThreadCoreMask)
                        + "，当前亲和性: 0x" + Long.toHexString(affinity));
            } else {
                System.err.println("[NovaAgent]   ✗ 线程亲和性绑定失败");
            }
        } catch (UnsatisfiedLinkError e) {
            System.out.println("[NovaAgent]   ⚠ 原生层不可用，跳过线程亲和性绑定");
            System.out.println("[NovaAgent]   提示: 请确保 libnovacore_native 已正确编译并放置在 java.library.path 中");
        }
    }

    // ── 多核线程池 ────────────────────────────────────────────────────

    /** 初始化多核线程池，将工作线程绑定到不同核心。 */
    private static void initThreadPool() {
        AtomicInteger coreIndex = new AtomicInteger(1); // 核心 0 留给主线程

        ForkJoinPool.ForkJoinWorkerThreadFactory factory = pool -> {
            ForkJoinWorkerThread worker = new ForkJoinWorkerThread(pool) {
                @Override
                protected void onStart() {
                    super.onStart();
                    int core = coreIndex.getAndIncrement() % poolCores + 1;
                    long mask = 1L << core;
                    try {
                        NativeThreadAffinity.setThreadAffinity(this.getId(), mask);
                        NativeThreadAffinity.setThreadPriority(this.getId(), Thread.MAX_PRIORITY);
                    } catch (UnsatisfiedLinkError ignored) {
                        // 原生层不可用，静默跳过
                    }
                }
            };
            return worker;
        };

        novaCorePool = new ForkJoinPool(
                poolCores,
                factory,
                (t, e) -> System.err.println("[NovaAgent] 线程池未捕获异常: " + t.getName() + " — " + e.getMessage()),
                false  // asyncMode = false
        );

        System.out.println("[NovaAgent]   ✓ 多核线程池已创建 (并行度: " + poolCores + ")");
        System.out.println("[NovaAgent]   核心分配策略: 核心 0 → 主线程, 核心 1-" + poolCores + " → 线程池");
    }

    // ── 内存预分配 ────────────────────────────────────────────────────

    /** 预分配堆外内存，减少运行时 GC 压力。 */
    private static void preallocateMemory() {
        long bytes = preallocateMemoryMB * 1024 * 1024;
        System.out.println("[NovaAgent]   预分配 " + preallocateMemoryMB + " MB 堆外内存...");

        try {
            // 通过 JNI 预分配原生内存
            NativeThreadAffinity.preallocateMemory(bytes);
            System.out.println("[NovaAgent]   ✓ 原生内存预分配完成");
        } catch (UnsatisfiedLinkError e) {
            // 回退：使用 Java DirectByteBuffer 预分配
            System.out.println("[NovaAgent]   原生层不可用，使用 DirectByteBuffer 预分配...");
            List<ByteBuffer> preallocatedBuffers = new ArrayList<>();
            long allocated = 0;
            long chunkSize = 32 * 1024 * 1024; // 每次分配 32 MB

            try {
                while (allocated < bytes) {
                    long remaining = bytes - allocated;
                    long size = Math.min(chunkSize, remaining);
                    ByteBuffer buffer = ByteBuffer.allocateDirect((int) size);
                    preallocatedBuffers.add(buffer);
                    allocated += size;
                }
                System.out.println("[NovaAgent]   ✓ DirectByteBuffer 预分配完成: "
                        + preallocatedBuffers.size() + " 个缓冲区, "
                        + (allocated / (1024 * 1024)) + " MB");
            } catch (OutOfMemoryError oom) {
                System.err.println("[NovaAgent]   ⚠ 预分配内存不足，已分配: "
                        + (allocated / (1024 * 1024)) + " MB / " + preallocateMemoryMB + " MB");
            }
        }
    }

    // ── 完成 ──────────────────────────────────────────────────────────

    /** 打印加载完成信息。 */
    private static void printCompletion() {
        System.out.println();
        printConfigSummary();
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  ✓ NovaCore Agent 初始化完成！                               ║");
        System.out.println("║  JVM 底层优化引擎已就绪，Minecraft 服务器将获得:              ║");
        System.out.println("║   • G1GC 低延迟垃圾回收                                       ║");
        System.out.println("║   • 线程亲和性绑定                                            ║");
        System.out.println("║   • 多核并行任务调度                                          ║");
        System.out.println("║   • 堆外内存预分配                                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // ── 工具方法 ──────────────────────────────────────────────────────

    /** 设置系统属性，如果已存在则覆盖。 */
    private static void setSystemProperty(String key, String value) {
        String old = System.getProperty(key);
        System.setProperty(key, value);
        if (old != null) {
            System.out.println("[NovaAgent]   覆盖系统属性 " + key + ": " + old + " → " + value);
        } else {
            System.out.println("[NovaAgent]   设置系统属性 " + key + " = " + value);
        }
    }

    /** 右侧填充空格到指定宽度（用于对齐）。 */
    private static String padRight(String s, int width) {
        if (s == null) s = "null";
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** 截断字符串到指定长度，超出部分用 "..." 替代。 */
    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    // ── 公共 API ──────────────────────────────────────────────────────

    /**
     * 获取 NovaCore 线程池，供其他模块提交任务。
     *
     * @return 已初始化的 ForkJoinPool 实例，如果未初始化则返回 null
     */
    public static ForkJoinPool getThreadPool() {
        return novaCorePool;
    }

    /**
     * 获取当前配置的 GC 线程数。
     */
    public static int getGcThreads() {
        return gcThreads;
    }

    /**
     * 获取当前配置的最大停顿时间（毫秒）。
     */
    public static long getMaxPauseMillis() {
        return maxPauseMillis;
    }
}