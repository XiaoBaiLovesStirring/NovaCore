@echo off
REM ============================================================================
REM NovaCore Native Library - Windows Build Script
REM 编译生成 novacore_native.dll
REM ============================================================================

setlocal enabledelayedexpansion

REM 检查 JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set.
    echo Please set JAVA_HOME to your JDK installation directory.
    echo Example: set JAVA_HOME=C:\Program Files\Java\jdk-17
    exit /b 1
)

set JNI_INCLUDE=%JAVA_HOME%\include
set JNI_PLATFORM_INCLUDE=%JAVA_HOME%\include\win32

if not exist "%JNI_INCLUDE%" (
    echo ERROR: JNI include directory not found: %JNI_INCLUDE%
    exit /b 1
)

if not exist "%JNI_PLATFORM_INCLUDE%" (
    echo ERROR: JNI platform include directory not found: %JNI_PLATFORM_INCLUDE%
    exit /b 1
)

echo ============================================
echo  NovaCore Native Library - Windows Build
echo ============================================
echo JAVA_HOME: %JAVA_HOME%
echo JNI Include: %JNI_INCLUDE%
echo JNI Platform Include: %JNI_PLATFORM_INCLUDE%
echo.

REM 编译
g++ -shared -O3 -march=native -o novacore_native.dll novacore_native.cpp ^
    -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" ^
    -static -lpthread

if %ERRORLEVEL% neq 0 (
    echo.
    echo ============================================
    echo  Build Failed
    echo ============================================
    exit /b 1
)

echo.
echo ============================================
echo  Build Successful
echo ============================================
echo Output: novacore_native.dll
echo.

REM 显示输出文件信息
dir novacore_native.dll

endlocal