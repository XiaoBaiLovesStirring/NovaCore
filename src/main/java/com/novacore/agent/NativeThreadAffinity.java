package com.novacore.agent;

/**
 * NativeThreadAffinity — JNI 声明类，桥接 Java 与原生 C/C++ 层。
 * <p>
 * 提供以下原生能力：
 * <ul>
 *   <li>线程 CPU 核心亲和性绑定与查询</li>
 *   <li>线程优先级设置</li>
 *   <li>获取系统可用 CPU 核心数</li>
 *   <li>GC 参数原生配置</li>
 *   <li>堆外内存预分配</li>
 * </ul>
 * </p>
 *
 * <p>
 * 对应的 C/C++ 实现需编译为原生共享库 ({@code libnovacore_native.so} /
 * {@code novacore_native.dll} / {@code libnovacore_native.dylib})，
 * 并放置在 JAR 的 {@code native/} 资源目录下或 {@code java.library.path} 中。
 * </p>
 *
 * <h3>JNI 函数命名约定</h3>
 * <p>
 * 对应的 C 函数名格式为:
 * {@code Java_com_novacore_agent_NativeThreadAffinity_<方法名>}。
 * 例如：{@code Java_com_novacore_agent_NativeThreadAffinity_setThreadAffinity}
 * </p>
 *
 * <h3>线程 ID 说明</h3>
 * <p>
 * 传入的 {@code threadId} 参数是 Java 层的 {@link Thread#getId()} 返回值。
 * 在 Linux 上，JNI 实现需要通过 {@code /proc/self/task/<tid>/comm} 或
 * {@code pthread_t} 与内核 TID 做映射。在 Windows 上，需使用
 * {@code OpenThread} + {@code SetThreadAffinityMask}。
 * </p>
 */
public class NativeThreadAffinity {

    // ── 静态初始化 ────────────────────────────────────────────────────

    static {
        // 注意：NativeLoader.loadNativeLibrary() 已在 NovaAgent.premain() 中调用，
        // 这里再次调用是安全的（NativeLoader 内部是幂等的）。
        // 如果 NativeThreadAffinity 被独立使用（不通过 NovaAgent），
        // 则自动触发加载。
        try {
            NativeLoader.loadNativeLibrary();
        } catch (Throwable t) {
            System.err.println("[NativeThreadAffinity] 自动加载原生库失败: " + t.getMessage());
            // 不抛出异常，允许纯 Java 模式降级运行
        }
    }

    // ── 线程亲和性 ────────────────────────────────────────────────────

    /**
     * 设置指定线程的 CPU 核心亲和性。
     * <p>
     * 在 Linux 上通过 {@code sched_setaffinity} 实现，
     * 在 Windows 上通过 {@code SetThreadAffinityMask} 实现。
     * </p>
     *
     * @param threadId Java 线程 ID（通过 {@link Thread#getId()} 获取）
     * @param coreMask CPU 核心位掩码。bit 0 对应核心 0，bit 1 对应核心 1，以此类推。
     *                 例如: {@code 0x01} 表示仅核心 0，{@code 0x03} 表示核心 0 和 1
     * @return true 表示设置成功，false 表示失败
     */
    public static native boolean setThreadAffinity(long threadId, long coreMask);

    /**
     * 查询指定线程当前的 CPU 核心亲和性掩码。
     *
     * @param threadId Java 线程 ID
     * @return 当前 CPU 核心亲和性位掩码，失败时返回 0
     */
    public static native long getThreadAffinity(long threadId);

    // ── 系统信息 ──────────────────────────────────────────────────────

    /**
     * 获取系统可用的 CPU 核心数（逻辑核心）。
     * <p>
     * 在 Linux 上通过 {@code sysconf(_SC_NPROCESSORS_ONLN)} 获取，
     * 在 Windows 上通过 {@code GetSystemInfo} 获取。
     * </p>
     *
     * @return 可用逻辑核心数，失败时返回 -1
     */
    public static native int getAvailableCores();

    // ── 线程优先级 ────────────────────────────────────────────────────

    /**
     * 设置指定线程的调度优先级。
     * <p>
     * 在 Linux 上，如果 JVM 拥有 {@code CAP_SYS_NICE} 权限，
     * 将尝试设置为 {@code SCHED_FIFO} 实时调度策略并设置优先级。
     * 否则使用 {@code setpriority(PRIO_PROCESS)} 调整 nice 值。
     * 在 Windows 上，使用 {@code SetThreadPriority}。
     * </p>
     *
     * @param threadId Java 线程 ID
     * @param priority 优先级值。Java 层取值范围为 {@link Thread#MIN_PRIORITY} (1)
     *                 到 {@link Thread#MAX_PRIORITY} (10)，
     *                 原生实现负责将其映射为操作系统特定的优先级值
     * @return true 表示设置成功，false 表示失败
     */
    public static native boolean setThreadPriority(long threadId, int priority);

    // ── GC 配置 ───────────────────────────────────────────────────────

    /**
     * 通过原生层配置 GC 参数。
     * <p>
     * 此方法尝试在原生层设置 JVM 内部 GC 参数，作为运行时
     * 无法通过 {@code System.setProperty()} 修改的 {@code -XX} 参数的补充。
     * 如果 JVM 实现不支持或原生层不可用，调用方应降级处理。
     * </p>
     *
     * @param gcThreads          GC 线程数
     * @param maxPauseNanos      最大暂停时间目标（纳秒）
     * @param heapOccupancyPercent 堆占用百分比阈值（触发并发标记周期）
     */
    public static native void configureGC(int gcThreads, long maxPauseNanos, int heapOccupancyPercent);

    // ── 内存预分配 ────────────────────────────────────────────────────

    /**
     * 在原生层预分配堆外内存。
     * <p>
     * 通过 {@code mmap} (Linux) 或 {@code VirtualAlloc} (Windows) 预分配
     * 指定大小的内存区域并锁定（{@code mlock} / {@code VirtualLock}），
     * 防止被交换到磁盘，减少运行时延迟抖动。
     * </p>
     *
     * @param sizeBytes 要预分配的字节数
     */
    public static native void preallocateMemory(long sizeBytes);

    // ── 私有构造函数，防止实例化 ──────────────────────────────────────

    private NativeThreadAffinity() {
        throw new UnsupportedOperationException("NativeThreadAffinity 是工具类，不可实例化");
    }
}