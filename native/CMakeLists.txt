cmake_minimum_required(VERSION 3.10)
project(novacore_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# 输出目录
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)

# ============================================================================
# JNI 依赖
# ============================================================================
find_package(JNI REQUIRED)

# ============================================================================
# 编译目标
# ============================================================================
add_library(novacore_native SHARED novacore_native.cpp)

target_include_directories(novacore_native PRIVATE ${JNI_INCLUDE_DIRS})

target_compile_options(novacore_native PRIVATE -O3 -march=native)

# 平台特定定义
if(WIN32)
    target_compile_definitions(novacore_native PRIVATE _WIN32)
elseif(APPLE)
    target_compile_definitions(novacore_native PRIVATE __APPLE__)
    target_link_libraries(novacore_native PRIVATE pthread)
else()
    target_compile_definitions(novacore_native PRIVATE __linux__)
    target_link_libraries(novacore_native PRIVATE pthread rt)
endif()

# ============================================================================
# 安装规则
# ============================================================================
install(TARGETS novacore_native
    LIBRARY DESTINATION lib
    RUNTIME DESTINATION bin
)

# ============================================================================
# 构建信息输出
# ============================================================================
message(STATUS "============================================")
message(STATUS " NovaCore Native Library - CMake Build")
message(STATUS "============================================")
message(STATUS "C++ Standard:     ${CMAKE_CXX_STANDARD}")
message(STATUS "JNI Include Dirs: ${JNI_INCLUDE_DIRS}")
message(STATUS "JNI Libraries:    ${JNI_LIBRARIES}")
message(STATUS "Platform:         ${CMAKE_SYSTEM_NAME}")
message(STATUS "Build Type:       ${CMAKE_BUILD_TYPE}")
message(STATUS "============================================")