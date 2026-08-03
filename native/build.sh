#!/bin/bash
# ============================================================================
# NovaCore Native Library - Linux Build Script
# 编译生成 libnovacore_native.so
# ============================================================================

set -e

# 确定 JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME is not set."
    echo "Please set JAVA_HOME to your JDK installation directory."
    echo "Example: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
    exit 1
fi

JNI_INCLUDE="$JAVA_HOME/include"
JNI_PLATFORM_INCLUDE="$JAVA_HOME/include/linux"

if [ ! -d "$JNI_INCLUDE" ]; then
    echo "ERROR: JNI include directory not found: $JNI_INCLUDE"
    exit 1
fi

if [ ! -d "$JNI_PLATFORM_INCLUDE" ]; then
    echo "ERROR: JNI platform include directory not found: $JNI_PLATFORM_INCLUDE"
    exit 1
fi

echo "============================================"
echo " NovaCore Native Library - Linux Build"
echo "============================================"
echo "JAVA_HOME: $JAVA_HOME"
echo "JNI Include: $JNI_INCLUDE"
echo "JNI Platform Include: $JNI_PLATFORM_INCLUDE"
echo ""

# 编译
g++ -shared -fPIC -O3 -march=native -o libnovacore_native.so novacore_native.cpp \
    -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/linux" \
    -lpthread -lrt

echo ""
echo "============================================"
echo " Build Successful"
echo "============================================"
echo "Output: libnovacore_native.so"
echo ""

# 显示输出文件信息
ls -lh libnovacore_native.so
file libnovacore_native.so