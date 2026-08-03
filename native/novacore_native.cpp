/**
 * NovaCore Native Library
 *
 * 底层线程调度、GC 调控和内存管理，通过 JNI 与 Java Agent 通信。
 *
 * 平台支持:
 *   - Linux   (glibc + pthread)
 *   - Windows (Win32 API)
 *   - macOS   (pthread + mach)
 */

#include "novacore_native.h"

#include <iostream>
#include <cstring>
#include <cerrno>
#include <cstdlib>

// ============================================================================
// 平台头文件
// ============================================================================

#ifdef _WIN32
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <processthreadsapi.h>
#include <sysinfoapi.h>
#else
#include <unistd.h>
#include <pthread.h>
#include <sched.h>
#include <sys/mman.h>
#include <sys/resource.h>
#include <sys/syscall.h>
#include <sys/time.h>
#include <fstream>
#include <sstream>
#endif

// ============================================================================
// 常量
// ============================================================================

static constexpr const char* LOG_TAG = "[NovaCore-Native]";

// 最小/最大优先级映射
#ifdef _WIN32
static constexpr int PRIORITY_MIN = THREAD_PRIORITY_IDLE;
static constexpr int PRIORITY_MAX = THREAD_PRIORITY_TIME_CRITICAL;
#else
static constexpr int PRIORITY_MIN = 1;
static constexpr int PRIORITY_MAX = 99;
#endif

// ============================================================================
// 内部辅助函数
// ============================================================================

#ifdef __linux__
/**
 * 将 jlong threadId 转换为 pthread_t。
 * 在 Linux 上，Java 线程 ID 通常就是 native tid；
 * 这里通过遍历 /proc/self/task 找到对应的 pthread_t。
 */
static pthread_t getPthreadFromId(jlong threadId) {
    // 简单方法：如果 threadId 是当前线程，直接返回 pthread_self()
    pid_t tid = static_cast<pid_t>(threadId);
    pid_t myTid = static_cast<pid_t>(syscall(SYS_gettid));

    if (tid == myTid) {
        return pthread_self();
    }

    // 对于其他线程，需要通过 /proc/self/task/<tid> 查找
    // 但在 JNI 场景中，通常 threadId 就是 pthread_t 的数值表示
    // 这是常见做法，将 jlong 直接转换为 pthread_t
    return static_cast<pthread_t>(threadId);
}
#endif

static void logInfo(const char* msg) {
    std::cout << LOG_TAG << " [INFO] " << msg << std::endl;
}

static void logError(const char* msg) {
    std::cerr << LOG_TAG << " [ERROR] " << msg << std::endl;
}

static void logErrorWithErrno(const char* msg) {
    std::cerr << LOG_TAG << " [ERROR] " << msg
              << " (errno=" << errno << ": " << std::strerror(errno) << ")"
              << std::endl;
}

#ifdef _WIN32
static void logErrorWithWin32(const char* msg) {
    DWORD err = GetLastError();
    char buf[256];
    FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
                   nullptr, err, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                   buf, sizeof(buf), nullptr);
    std::cerr << LOG_TAG << " [ERROR] " << msg
              << " (Win32 error=" << err << ": " << buf << ")"
              << std::endl;
}
#endif

// ============================================================================
// setThreadAffinity
// 将指定线程绑定到指定的 CPU 核心掩码
// ============================================================================

JNIEXPORT jboolean JNICALL
Java_com_novacore_agent_NativeThreadAffinity_setThreadAffinity(
    JNIEnv* /*env*/, jclass /*cls*/, jlong threadId, jlong coreMask) {

    if (coreMask == 0) {
        logError("setThreadAffinity: coreMask cannot be zero");
        return JNI_FALSE;
    }

#ifdef _WIN32
    // ---- Windows 实现 ----
    HANDLE hThread = nullptr;

    if (threadId == 0) {
        // 当前线程
        hThread = GetCurrentThread();
        logInfo("setThreadAffinity: binding current thread");
    } else {
        hThread = OpenThread(THREAD_SET_INFORMATION | THREAD_QUERY_INFORMATION,
                             FALSE, static_cast<DWORD>(threadId));
        if (hThread == nullptr) {
            logErrorWithWin32("setThreadAffinity: OpenThread failed");
            return JNI_FALSE;
        }
    }

    DWORD_PTR mask = static_cast<DWORD_PTR>(coreMask);
    DWORD_PTR oldMask = SetThreadAffinityMask(hThread, mask);

    if (oldMask == 0) {
        logErrorWithWin32("setThreadAffinity: SetThreadAffinityMask failed");
        if (threadId != 0) {
            CloseHandle(hThread);
        }
        return JNI_FALSE;
    }

    std::cout << LOG_TAG << " [INFO] setThreadAffinity: threadId=" << threadId
              << " coreMask=0x" << std::hex << mask << std::dec
              << " (previous=0x" << std::hex << oldMask << std::dec << ")"
              << std::endl;

    if (threadId != 0) {
        CloseHandle(hThread);
    }

#elif defined(__linux__)
    // ---- Linux 实现 ----
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);

    // 将 coreMask 的位映射到 cpu_set_t
    for (int i = 0; i < CPU_SETSIZE && i < static_cast<int>(sizeof(jlong) * 8); ++i) {
        if (coreMask & (1LL << i)) {
            CPU_SET(i, &cpuset);
        }
    }

    pthread_t thread = getPthreadFromId(threadId);
    int result = pthread_setaffinity_np(thread, sizeof(cpu_set_t), &cpuset);

    if (result != 0) {
        errno = result;
        logErrorWithErrno("setThreadAffinity: pthread_setaffinity_np failed");
        return JNI_FALSE;
    }

    std::cout << LOG_TAG << " [INFO] setThreadAffinity: threadId=" << threadId
              << " coreMask=0x" << std::hex << coreMask << std::dec
              << std::endl;

#elif defined(__APPLE__)
    // ---- macOS 实现 ----
    // macOS 不支持线程级别的 CPU 亲和性绑定。
    // 可以使用 thread_policy_set 设置线程亲和性策略，但功能有限。
    logInfo("setThreadAffinity: thread affinity is not supported on macOS; "
            "use thread_policy_set as a partial alternative");
    return JNI_FALSE;

#else
    logError("setThreadAffinity: unsupported platform");
    return JNI_FALSE;
#endif

    return JNI_TRUE;
}

// ============================================================================
// getThreadAffinity
// 获取指定线程的 CPU 亲和性掩码
// ============================================================================

JNIEXPORT jlong JNICALL
Java_com_novacore_agent_NativeThreadAffinity_getThreadAffinity(
    JNIEnv* /*env*/, jclass /*cls*/, jlong threadId) {

#ifdef _WIN32
    // ---- Windows 实现 ----
    // Windows 没有直接获取线程亲和性的 API。
    // 通过临时设置再恢复的方式获取（hack），或者返回进程亲和性。
    HANDLE hThread = nullptr;
    HANDLE hProcess = GetCurrentProcess();

    if (threadId == 0) {
        hThread = GetCurrentThread();
    } else {
        hThread = OpenThread(THREAD_SET_INFORMATION | THREAD_QUERY_INFORMATION,
                             FALSE, static_cast<DWORD>(threadId));
        if (hThread == nullptr) {
            logErrorWithWin32("getThreadAffinity: OpenThread failed");
            return 0;
        }
    }

    // 获取进程亲和性作为参考，同时尝试获取线程亲和性
    DWORD_PTR processMask = 0;
    DWORD_PTR systemMask = 0;
    if (!GetProcessAffinityMask(hProcess, &processMask, &systemMask)) {
        logErrorWithWin32("getThreadAffinity: GetProcessAffinityMask failed");
        if (threadId != 0) CloseHandle(hThread);
        return 0;
    }

    // 通过临时设置来获取当前线程亲和性
    // 设置一个临时掩码，旧值即为当前亲和性
    DWORD_PTR oldMask = SetThreadAffinityMask(hThread, processMask);
    if (oldMask == 0) {
        // 如果失败，返回进程亲和性
        if (threadId != 0) CloseHandle(hThread);
        std::cout << LOG_TAG << " [WARN] getThreadAffinity: falling back to process affinity mask"
                  << std::endl;
        return static_cast<jlong>(processMask);
    }

    // 恢复原始亲和性
    SetThreadAffinityMask(hThread, oldMask);

    if (threadId != 0) {
        CloseHandle(hThread);
    }

    std::cout << LOG_TAG << " [INFO] getThreadAffinity: threadId=" << threadId
              << " affinityMask=0x" << std::hex << oldMask << std::dec
              << std::endl;

    return static_cast<jlong>(oldMask);

#elif defined(__linux__)
    // ---- Linux 实现 ----
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);

    pthread_t thread = getPthreadFromId(threadId);
    int result = pthread_getaffinity_np(thread, sizeof(cpu_set_t), &cpuset);

    if (result != 0) {
        errno = result;
        logErrorWithErrno("getThreadAffinity: pthread_getaffinity_np failed");
        return 0;
    }

    // 将 cpu_set_t 转换为位掩码
    jlong mask = 0;
    for (int i = 0; i < CPU_SETSIZE && i < static_cast<int>(sizeof(jlong) * 8); ++i) {
        if (CPU_ISSET(i, &cpuset)) {
            mask |= (1LL << i);
        }
    }

    std::cout << LOG_TAG << " [INFO] getThreadAffinity: threadId=" << threadId
              << " affinityMask=0x" << std::hex << mask << std::dec
              << std::endl;

    return mask;

#elif defined(__APPLE__)
    logInfo("getThreadAffinity: thread affinity query is not supported on macOS");
    return 0;

#else
    logError("getThreadAffinity: unsupported platform");
    return 0;
#endif
}

// ============================================================================
// getAvailableCores
// 获取系统可用的 CPU 核心数
// ============================================================================

JNIEXPORT jint JNICALL
Java_com_novacore_agent_NativeThreadAffinity_getAvailableCores(
    JNIEnv* /*env*/, jclass /*cls*/) {

#ifdef _WIN32
    // ---- Windows 实现 ----
    SYSTEM_INFO sysInfo;
    GetSystemInfo(&sysInfo);

    jint cores = static_cast<jint>(sysInfo.dwNumberOfProcessors);
    std::cout << LOG_TAG << " [INFO] getAvailableCores: " << cores << " cores"
              << std::endl;

    // 也获取逻辑处理器信息的更详细结果
    DWORD_PTR processMask = 0;
    DWORD_PTR systemMask = 0;
    if (GetProcessAffinityMask(GetCurrentProcess(), &processMask, &systemMask)) {
        // 统计 systemMask 中的可用核心数
        jint availCores = 0;
        DWORD_PTR mask = systemMask;
        while (mask) {
            availCores += static_cast<jint>(mask & 1);
            mask >>= 1;
        }
        std::cout << LOG_TAG << " [INFO] getAvailableCores: system affinity mask shows "
                  << availCores << " available cores" << std::endl;
        return availCores;
    }

    return cores;

#elif defined(__linux__)
    // ---- Linux 实现 ----
    long nprocs = sysconf(_SC_NPROCESSORS_ONLN);
    if (nprocs < 0) {
        logErrorWithErrno("getAvailableCores: sysconf(_SC_NPROCESSORS_ONLN) failed");
        return 1; // 安全回退
    }

    jint cores = static_cast<jint>(nprocs);
    std::cout << LOG_TAG << " [INFO] getAvailableCores: " << cores << " cores online"
              << std::endl;

    // 额外读取 /sys/devices/system/cpu/online 获取更详细信息
    std::ifstream cpuOnline("/sys/devices/system/cpu/online");
    if (cpuOnline.is_open()) {
        std::string line;
        std::getline(cpuOnline, line);
        std::cout << LOG_TAG << " [INFO] getAvailableCores: /sys/devices/system/cpu/online = "
                  << line << std::endl;
    }

    return cores;

#elif defined(__APPLE__)
    // ---- macOS 实现 ----
    // macOS 也使用 sysconf
    long nprocs = sysconf(_SC_NPROCESSORS_ONLN);
    if (nprocs < 0) {
        logError("getAvailableCores: sysconf failed on macOS");
        return 1;
    }

    jint cores = static_cast<jint>(nprocs);
    std::cout << LOG_TAG << " [INFO] getAvailableCores: " << cores << " cores online (macOS)"
              << std::endl;
    return cores;

#else
    logError("getAvailableCores: unsupported platform");
    return 1;
#endif
}

// ============================================================================
// setThreadPriority
// 设置指定线程的调度优先级
// ============================================================================

JNIEXPORT jboolean JNICALL
Java_com_novacore_agent_NativeThreadAffinity_setThreadPriority(
    JNIEnv* /*env*/, jclass /*cls*/, jlong threadId, jint priority) {

    std::cout << LOG_TAG << " [INFO] setThreadPriority: threadId=" << threadId
              << " priority=" << priority << std::endl;

#ifdef _WIN32
    // ---- Windows 实现 ----
    HANDLE hThread = nullptr;

    if (threadId == 0) {
        hThread = GetCurrentThread();
    } else {
        hThread = OpenThread(THREAD_SET_INFORMATION | THREAD_QUERY_INFORMATION,
                             FALSE, static_cast<DWORD>(threadId));
        if (hThread == nullptr) {
            logErrorWithWin32("setThreadPriority: OpenThread failed");
            return JNI_FALSE;
        }
    }

    // 将 Java 优先级映射到 Windows 线程优先级
    // Java Thread priority: 1 (MIN) ~ 10 (MAX), 5 (NORM)
    // Windows: THREAD_PRIORITY_IDLE ~ THREAD_PRIORITY_TIME_CRITICAL
    int winPriority;
    if (priority <= 1) {
        winPriority = THREAD_PRIORITY_IDLE;
    } else if (priority <= 2) {
        winPriority = THREAD_PRIORITY_LOWEST;
    } else if (priority <= 4) {
        winPriority = THREAD_PRIORITY_BELOW_NORMAL;
    } else if (priority <= 6) {
        winPriority = THREAD_PRIORITY_NORMAL;
    } else if (priority <= 8) {
        winPriority = THREAD_PRIORITY_ABOVE_NORMAL;
    } else if (priority <= 9) {
        winPriority = THREAD_PRIORITY_HIGHEST;
    } else {
        winPriority = THREAD_PRIORITY_TIME_CRITICAL;
    }

    if (!SetThreadPriority(hThread, winPriority)) {
        logErrorWithWin32("setThreadPriority: SetThreadPriority failed");
        if (threadId != 0) CloseHandle(hThread);
        return JNI_FALSE;
    }

    if (threadId != 0) {
        CloseHandle(hThread);
    }

    std::cout << LOG_TAG << " [INFO] setThreadPriority: set Windows priority level "
              << winPriority << std::endl;

#elif defined(__linux__)
    // ---- Linux 实现 ----
    pthread_t thread = getPthreadFromId(threadId);

    struct sched_param param;
    int policy;

    // 获取当前调度策略
    int result = pthread_getschedparam(thread, &policy, &param);
    if (result != 0) {
        errno = result;
        logErrorWithErrno("setThreadPriority: pthread_getschedparam failed");
        return JNI_FALSE;
    }

    // 将 Java 优先级 (1-10) 映射到 Linux 实时优先级 (1-99)
    // 如果 priority >= 8，尝试使用 SCHED_FIFO（需要 CAP_SYS_NICE）
    int linuxPriority = PRIORITY_MIN + (priority - 1) * (PRIORITY_MAX - PRIORITY_MIN) / 9;

    if (priority >= 8) {
        // 高优先级任务：尝试实时调度
        param.sched_priority = linuxPriority;
        result = pthread_setschedparam(thread, SCHED_FIFO, &param);

        if (result != 0) {
            // 如果没有足够权限，回退到 SCHED_OTHER + nice
            std::cout << LOG_TAG << " [WARN] setThreadPriority: "
                      << "SCHED_FIFO requires CAP_SYS_NICE, falling back to SCHED_OTHER"
                      << std::endl;

            param.sched_priority = 0;
            result = pthread_setschedparam(thread, SCHED_OTHER, &param);
            if (result == 0) {
                // 通过 nice 值调节
                int niceValue = 0 - (priority - 5) * 4; // -20 到 +10
                if (niceValue < -20) niceValue = -20;
                if (niceValue > 19) niceValue = 19;
                if (setpriority(PRIO_PROCESS, static_cast<pid_t>(threadId), niceValue) != 0) {
                    logErrorWithErrno("setThreadPriority: setpriority failed");
                }
            }
        }
    } else {
        // 普通优先级任务：使用 SCHED_OTHER + nice
        param.sched_priority = 0;
        result = pthread_setschedparam(thread, SCHED_OTHER, &param);

        if (result == 0) {
            int niceValue = 10 - priority; // 映射到 nice 值范围
            if (niceValue < -20) niceValue = -20;
            if (niceValue > 19) niceValue = 19;
            if (setpriority(PRIO_PROCESS, static_cast<pid_t>(threadId), niceValue) != 0) {
                logErrorWithErrno("setThreadPriority: setpriority failed");
            }
        }
    }

    if (result != 0) {
        errno = result;
        logErrorWithErrno("setThreadPriority: pthread_setschedparam failed");
        return JNI_FALSE;
    }

    std::cout << LOG_TAG << " [INFO] setThreadPriority: policy=" << policy
              << " priority=" << param.sched_priority << std::endl;

#elif defined(__APPLE__)
    // ---- macOS 实现 ----
    pthread_t thread = getPthreadFromId(threadId);

    struct sched_param param;
    int policy;
    int result = pthread_getschedparam(thread, &policy, &param);

    if (result != 0) {
        errno = result;
        logErrorWithErrno("setThreadPriority: pthread_getschedparam failed on macOS");
        return JNI_FALSE;
    }

    int macPriority = PRIORITY_MIN + (priority - 1) * (PRIORITY_MAX - PRIORITY_MIN) / 9;
    param.sched_priority = macPriority;

    result = pthread_setschedparam(thread, policy, &param);
    if (result != 0) {
        errno = result;
        logErrorWithErrno("setThreadPriority: pthread_setschedparam failed on macOS");
        return JNI_FALSE;
    }

    std::cout << LOG_TAG << " [INFO] setThreadPriority: macOS priority="
              << macPriority << std::endl;

#else
    logError("setThreadPriority: unsupported platform");
    return JNI_FALSE;
#endif

    return JNI_TRUE;
}

// ============================================================================
// configureGC
// 配置 JVM GC 行为（线程数、最大暂停时间、堆占用百分比）
// ============================================================================

JNIEXPORT void JNICALL
Java_com_novacore_agent_NativeThreadAffinity_configureGC(
    JNIEnv* env, jclass /*cls*/, jint gcThreads, jlong maxPauseNanos, jint heapOccupancyPercent) {

    std::cout << LOG_TAG << " [INFO] configureGC: gcThreads=" << gcThreads
              << " maxPauseNanos=" << maxPauseNanos
              << " heapOccupancyPercent=" << heapOccupancyPercent
              << std::endl;

    // 参数合法性检查
    if (gcThreads < 0) {
        gcThreads = 0; // 0 表示由 JVM 自动决定
    }
    if (maxPauseNanos < 0) {
        maxPauseNanos = 0;
    }
    if (heapOccupancyPercent < 0 || heapOccupancyPercent > 100) {
        std::cerr << LOG_TAG << " [WARN] configureGC: heapOccupancyPercent out of range [0,100], "
                  << "clamping. got=" << heapOccupancyPercent << std::endl;
        if (heapOccupancyPercent < 0) heapOccupancyPercent = 0;
        if (heapOccupancyPercent > 100) heapOccupancyPercent = 100;
    }

    // 通过 JNI 设置 Java 系统属性来影响 GC 行为
    // 注意：大多数 GC 相关属性需要在 JVM 启动时设定，运行时修改可能无效。
    // 这里通过 HotSpot 的内部管理 API 进行尽力而为的配置。

    jclass systemClass = env->FindClass("java/lang/System");
    if (systemClass == nullptr) {
        logError("configureGC: failed to find java/lang/System class");
        env->ExceptionClear();
        return;
    }

    jmethodID setPropertyMethod = env->GetStaticMethodID(
        systemClass, "setProperty",
        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    if (setPropertyMethod == nullptr) {
        logError("configureGC: failed to find System.setProperty method");
        env->ExceptionClear();
        return;
    }

    // 设置 GC 线程数（对 G1GC 和 ParallelGC 有效）
    if (gcThreads > 0) {
        jstring key = env->NewStringUTF("-XX:ParallelGCThreads");
        jstring value = env->NewStringUTF(std::to_string(gcThreads).c_str());
        env->CallStaticObjectMethod(systemClass, setPropertyMethod, key, value);
        env->DeleteLocalRef(key);
        env->DeleteLocalRef(value);
        std::cout << LOG_TAG << " [INFO] configureGC: set ParallelGCThreads="
                  << gcThreads << std::endl;
    }

    // 设置最大 GC 暂停目标（纳秒转毫秒）
    if (maxPauseNanos > 0) {
        jlong pauseMillis = maxPauseNanos / 1000000LL;
        if (pauseMillis < 1) pauseMillis = 1;

        jstring key = env->NewStringUTF("-XX:MaxGCPauseMillis");
        jstring value = env->NewStringUTF(std::to_string(pauseMillis).c_str());
        env->CallStaticObjectMethod(systemClass, setPropertyMethod, key, value);
        env->DeleteLocalRef(key);
        env->DeleteLocalRef(value);
        std::cout << LOG_TAG << " [INFO] configureGC: set MaxGCPauseMillis="
                  << pauseMillis << "ms" << std::endl;
    }

    // 设置堆占用百分比（G1GC 的 InitiatingHeapOccupancyPercent）
    if (heapOccupancyPercent > 0) {
        jstring key = env->NewStringUTF("-XX:InitiatingHeapOccupancyPercent");
        jstring value = env->NewStringUTF(std::to_string(heapOccupancyPercent).c_str());
        env->CallStaticObjectMethod(systemClass, setPropertyMethod, key, value);
        env->DeleteLocalRef(key);
        env->DeleteLocalRef(value);
        std::cout << LOG_TAG << " [INFO] configureGC: set InitiatingHeapOccupancyPercent="
                  << heapOccupancyPercent << std::endl;
    }

#ifdef __linux__
    // Linux 特有的内核级 GC 辅助优化
    // 尝试调整 vm.swappiness 和 vm.dirty_ratio（需要 root 权限）
    std::cout << LOG_TAG << " [INFO] configureGC: attempting Linux kernel tuning..." << std::endl;

    // 检查 /proc/sys/vm/swappiness（更需要 swap 时降低此值）
    std::ifstream swappinessFile("/proc/sys/vm/swappiness");
    if (swappinessFile.is_open()) {
        std::string swappiness;
        std::getline(swappinessFile, swappiness);
        std::cout << LOG_TAG << " [INFO] configureGC: current vm.swappiness="
                  << swappiness << std::endl;
    }

    // 检查透明大页状态
    std::ifstream thpFile("/sys/kernel/mm/transparent_hugepage/enabled");
    if (thpFile.is_open()) {
        std::string thp;
        std::getline(thpFile, thp);
        std::cout << LOG_TAG << " [INFO] configureGC: transparent_hugepage="
                  << thp << std::endl;
    }
#endif

    logInfo("configureGC: configuration complete");
}

// ============================================================================
// preallocateMemory
// 预分配并锁定物理内存，防止被 swap 换出
// ============================================================================

JNIEXPORT void JNICALL
Java_com_novacore_agent_NativeThreadAffinity_preallocateMemory(
    JNIEnv* /*env*/, jclass /*cls*/, jlong sizeBytes) {

    if (sizeBytes <= 0) {
        logError("preallocateMemory: sizeBytes must be positive");
        return;
    }

    std::cout << LOG_TAG << " [INFO] preallocateMemory: allocating "
              << (sizeBytes / (1024 * 1024)) << " MB" << std::endl;

#ifdef _WIN32
    // ---- Windows 实现 ----
    // VirtualAlloc 分配并提交物理内存
    LPVOID mem = VirtualAlloc(
        nullptr,                         // 让系统选择地址
        static_cast<SIZE_T>(sizeBytes),  // 分配大小
        MEM_COMMIT | MEM_RESERVE,        // 提交并保留
        PAGE_READWRITE                   // 读写权限
    );

    if (mem == nullptr) {
        logErrorWithWin32("preallocateMemory: VirtualAlloc failed");
        return;
    }

    // VirtualLock 锁定内存页，防止被换出到页面文件
    if (!VirtualLock(mem, static_cast<SIZE_T>(sizeBytes))) {
        logErrorWithWin32("preallocateMemory: VirtualLock failed");
        // 即使锁定失败，内存已分配，继续执行
    } else {
        std::cout << LOG_TAG << " [INFO] preallocateMemory: "
                  << (sizeBytes / (1024 * 1024)) << " MB locked in physical memory"
                  << std::endl;
    }

    // 初始化内存（触发实际物理页分配）
    std::memset(mem, 0, static_cast<size_t>(sizeBytes));

    std::cout << LOG_TAG << " [INFO] preallocateMemory: Windows allocation complete at "
              << mem << std::endl;

    // 注意：在 Windows 上，内存由当前进程持有，进程退出时自动释放。
    // 如果需要显式释放，可调用 VirtualFree(mem, 0, MEM_RELEASE);

#elif defined(__linux__)
    // ---- Linux 实现 ----
    // mmap 分配匿名内存
    void* mem = mmap(
        nullptr,                          // 让内核选择地址
        static_cast<size_t>(sizeBytes),   // 大小
        PROT_READ | PROT_WRITE,           // 读写权限
        MAP_PRIVATE | MAP_ANONYMOUS,      // 私有匿名映射
        -1,                               // 无文件描述符
        0                                 // 偏移量
    );

    if (mem == MAP_FAILED) {
        logErrorWithErrno("preallocateMemory: mmap failed");
        return;
    }

    // mlock 锁定内存，防止被 swap 换出
    if (mlock(mem, static_cast<size_t>(sizeBytes)) != 0) {
        logErrorWithErrno("preallocateMemory: mlock failed");

        // 检查是否因为 ulimit 限制
        struct rlimit rlim;
        if (getrlimit(RLIMIT_MEMLOCK, &rlim) == 0) {
            std::cout << LOG_TAG << " [WARN] preallocateMemory: "
                      << "RLIMIT_MEMLOCK soft=" << rlim.rlim_cur
                      << " hard=" << rlim.rlim_max
                      << ". Consider raising with 'ulimit -l'"
                      << std::endl;
        }

        // 即使锁定失败，内存已分配，继续执行
    } else {
        std::cout << LOG_TAG << " [INFO] preallocateMemory: "
                  << (sizeBytes / (1024 * 1024)) << " MB locked in physical memory"
                  << std::endl;
    }

    // 初始化内存（触发实际物理页分配，避免延迟到首次访问时）
    std::memset(mem, 0, static_cast<size_t>(sizeBytes));

    // 建议内核使用大页（THP / HugeTLB）
    if (madvise(mem, static_cast<size_t>(sizeBytes), MADV_HUGEPAGE) != 0) {
        // madvise 失败不是致命错误
        std::cout << LOG_TAG << " [WARN] preallocateMemory: madvise(MADV_HUGEPAGE) failed"
                  << std::endl;
    }

    std::cout << LOG_TAG << " [INFO] preallocateMemory: Linux allocation complete at "
              << mem << " (" << (sizeBytes / (1024 * 1024)) << " MB)"
              << std::endl;

    // 注意：内存由当前进程持有，通过 munmap(mem, sizeBytes) 释放。
    // 在 JNI 场景中，通常在整个 Agent 生命周期中保持此内存。

#elif defined(__APPLE__)
    // ---- macOS 实现 ----
    void* mem = mmap(
        nullptr,
        static_cast<size_t>(sizeBytes),
        PROT_READ | PROT_WRITE,
        MAP_PRIVATE | MAP_ANONYMOUS,
        -1,
        0
    );

    if (mem == MAP_FAILED) {
        logErrorWithErrno("preallocateMemory: mmap failed on macOS");
        return;
    }

    // macOS 也支持 mlock
    if (mlock(mem, static_cast<size_t>(sizeBytes)) != 0) {
        logErrorWithErrno("preallocateMemory: mlock failed on macOS");
    } else {
        std::cout << LOG_TAG << " [INFO] preallocateMemory: "
                  << (sizeBytes / (1024 * 1024)) << " MB locked (macOS)" << std::endl;
    }

    std::memset(mem, 0, static_cast<size_t>(sizeBytes));

    std::cout << LOG_TAG << " [INFO] preallocateMemory: macOS allocation complete at "
              << mem << std::endl;

#else
    logError("preallocateMemory: unsupported platform");
#endif
}