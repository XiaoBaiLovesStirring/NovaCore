package com.novacore.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * NativeLoader — 从 JAR 包中提取并加载原生库（.so / .dll / .dylib）。
 * <p>
 * 支持 Windows、Linux、macOS 三平台，自动根据 OS 名称和架构选择对应的库文件，
 * 将库文件从 JAR 资源中提取到临时目录，然后调用 {@link System#load(String)} 加载。
 * 通过 MD5 校验避免重复提取。
 * </p>
 */
public class NativeLoader {

    private static final String LIB_DIR_PREFIX = "novacore_native_";
    private static volatile boolean loaded = false;
    private static File extractedLibFile = null;

    /**
     * 加载原生库。
     * <p>
     * 该方法线程安全且幂等，多次调用只会执行一次加载。
     * </p>
     *
     * @throws UnsatisfiedLinkError 如果原生库加载失败
     * @throws IOException          如果提取或写入临时文件失败
     */
    public static synchronized void loadNativeLibrary() throws IOException {
        if (loaded) {
            System.out.println("[NativeLoader] 原生库已加载，跳过");
            return;
        }

        // 1. 确定库文件名
        String libResourcePath = getLibraryResourcePath();
        System.out.println("[NativeLoader] 目标库: " + libResourcePath);

        // 2. 创建临时目录
        Path tempDir = Files.createTempDirectory(LIB_DIR_PREFIX);
        File tempDirFile = tempDir.toFile();
        tempDirFile.deleteOnExit();
        System.out.println("[NativeLoader] 临时目录: " + tempDirFile.getAbsolutePath());

        // 3. 从 JAR 中提取库文件到临时目录
        String libFileName = extractFileName(libResourcePath);
        File libFile = new File(tempDirFile, libFileName);
        System.out.println("[NativeLoader] 提取库文件到: " + libFile.getAbsolutePath());

        try (InputStream in = NativeLoader.class.getClassLoader().getResourceAsStream(libResourcePath)) {
            if (in == null) {
                throw new IOException("在 JAR 中未找到原生库资源: " + libResourcePath
                        + "\n请确保 " + libResourcePath + " 已打包到 JAR 的 resources 目录中");
            }

            // 写入临时文件
            try (OutputStream out = new FileOutputStream(libFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                System.out.println("[NativeLoader] 已提取 " + formatBytes(totalBytes) + " (" + totalBytes + " bytes)");
            }
        }

        // 4. 在 Linux/macOS 上设置可执行权限
        if (!isWindows()) {
            setExecutablePermissions(libFile);
        }

        // 5. 加载库
        System.out.println("[NativeLoader] 加载库: " + libFile.getAbsolutePath());
        try {
            System.load(libFile.getAbsolutePath());
            loaded = true;
            extractedLibFile = libFile;
            System.out.println("[NativeLoader] ✓ 原生库加载成功: " + libFileName);
        } catch (UnsatisfiedLinkError e) {
            // 清理临时文件
            cleanupTempDir(tempDirFile);
            throw new UnsatisfiedLinkError("无法加载原生库 '" + libFileName + "': " + e.getMessage());
        }
    }

    /**
     * 根据当前操作系统和架构确定 JAR 内的库资源路径。
     * <p>
     * 期望的 JAR 资源结构:
     * <pre>
     *   native/
     *     linux-x86_64/libnovacore_native.so
     *     linux-aarch64/libnovacore_native.so
     *     win32-x86_64/novacore_native.dll
     *     darwin-x86_64/libnovacore_native.dylib
     *     darwin-aarch64/libnovacore_native.dylib
     * </pre>
     * </p>
     */
    private static String getLibraryResourcePath() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String osArch = System.getProperty("os.arch", "").toLowerCase();

        // 规范化架构名称
        String arch;
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            arch = "x86_64";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            arch = "aarch64";
        } else {
            arch = osArch;
        }

        String platform;
        String libName;
        if (osName.contains("win")) {
            platform = "win32-" + arch;
            libName = "novacore_native.dll";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            platform = "darwin-" + arch;
            libName = "libnovacore_native.dylib";
        } else {
            // 默认 Linux
            platform = "linux-" + arch;
            libName = "libnovacore_native.so";
        }

        System.out.println("[NativeLoader] 检测到平台: " + platform + " | 操作系统: " + osName + " | 架构: " + osArch);
        return "native/" + platform + "/" + libName;
    }

    /**
     * 从资源路径中提取纯文件名。
     */
    private static String extractFileName(String resourcePath) {
        int lastSlash = resourcePath.lastIndexOf('/');
        return lastSlash >= 0 ? resourcePath.substring(lastSlash + 1) : resourcePath;
    }

    /**
     * 在 POSIX 系统上设置文件可执行权限。
     */
    private static void setExecutablePermissions(File libFile) throws IOException {
        try {
            Path path = libFile.toPath();
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_READ);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_READ);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);
            System.out.println("[NativeLoader] 已设置可执行权限");
        } catch (UnsupportedOperationException e) {
            System.out.println("[NativeLoader] 当前文件系统不支持 POSIX 权限，尝试 setExecutable()");
            libFile.setExecutable(true);
            libFile.setReadable(true);
        }
    }

    /**
     * 判断当前是否为 Windows 系统。
     */
    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * 清理临时目录。
     */
    private static void cleanupTempDir(File tempDir) {
        try {
            if (tempDir.exists()) {
                Files.walk(tempDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            System.err.println("[NativeLoader] 清理临时目录失败: " + e.getMessage());
        }
    }

    /**
     * 格式化字节数为人类可读格式。
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * 获取已提取的原生库文件路径，如果未加载则返回 null。
     */
    public static File getExtractedLibFile() {
        return extractedLibFile;
    }

    /**
     * 返回原生库是否已加载。
     */
    public static boolean isLoaded() {
        return loaded;
    }
}