cmake_minimum_required(VERSION 3.22.1)

project(aether LANGUAGES CXX)

# ─────────────────────────────────────────────────────────────────────────────
# libaether.so — Security guard (anti-hook, anti-debug, anti-repack)
# ─────────────────────────────────────────────────────────────────────────────

add_library(aether SHARED libaether.cpp)

target_compile_features(aether PRIVATE cxx_std_17)

set_target_properties(aether PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE TRUE
)

target_compile_options(aether PRIVATE
    -Oz
    -flto=thin
    -fstack-protector-strong
    -fPIC
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -D_FORTIFY_SOURCE=2
    -Wall
    -Wextra
    -Wno-unused-parameter
    -DANDROID
    $<$<CONFIG:Release>:-DNDEBUG>
    $<$<NOT:$<CONFIG:Release>>:-DDEV_LOG=1>
)

target_link_options(aether PRIVATE
    -flto=thin
    -Wl,--gc-sections
    -Wl,-z,noexecstack
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,--exclude-libs,ALL
    -Wl,--strip-all
)

target_link_libraries(aether
    android
    log
)

# ─────────────────────────────────────────────────────────────────────────────
# libcimolagent.so — Performance & system info agent (CPU/thermal/battery/IO)
# ─────────────────────────────────────────────────────────────────────────────

add_library(cimolagent SHARED libcimolagent.cpp)

target_compile_features(cimolagent PRIVATE cxx_std_17)

set_target_properties(cimolagent PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE TRUE
)

target_compile_options(cimolagent PRIVATE
    -Oz
    -flto=thin
    -fstack-protector-strong
    -fPIC
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -D_FORTIFY_SOURCE=2
    -Wall
    -Wextra
    -Wno-unused-parameter
    -DANDROID
    $<$<CONFIG:Release>:-DNDEBUG>
)

target_link_options(cimolagent PRIVATE
    -flto=thin
    -Wl,--gc-sections
    -Wl,-z,noexecstack
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,--exclude-libs,ALL
    -Wl,--strip-all
)

target_link_libraries(cimolagent
    android
    log
)