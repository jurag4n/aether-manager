cmake_minimum_required(VERSION 3.22.1)

project(aether LANGUAGES CXX)

set(COMMON_COMPILE_OPTIONS
    -O3
    -flto=thin
    -fstack-protector-strong
    -fPIC
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    -fno-exceptions
    -fno-rtti
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables
    -fomit-frame-pointer
    -D_FORTIFY_SOURCE=2
    -DANDROID
    -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.
    -fmacro-prefix-map=${CMAKE_SOURCE_DIR}=.
    $<$<CONFIG:Release>:-DNDEBUG>
    $<$<NOT:$<CONFIG:Release>>:-DDEV_LOG=1>
)

set(COMMON_LINK_OPTIONS
    -flto=thin
    -Wl,--gc-sections
    -Wl,--strip-all
    -Wl,--strip-debug
    -Wl,--exclude-libs,ALL
    -Wl,-z,noexecstack
    -Wl,-z,relro
    -Wl,-z,now
    # REMOVED: -Wl,-z,notext  <-- ini yang bikin text relocation lolos tapi justru
    #                              menyebabkan dlopen gagal di Android API 23+
    -Wl,--no-undefined
    -Wl,--hash-style=gnu
    -Wl,--discard-all
)

set(AETHER_COMPILE_OPTIONS
    ${COMMON_COMPILE_OPTIONS}
)

set(AETHER_LINK_OPTIONS
    ${COMMON_LINK_OPTIONS}
)

# ─── libaether.so ────────────────────────────────────────────────────────────
add_library(aether SHARED libaether.cpp)

target_compile_features(aether PRIVATE cxx_std_17)

set_target_properties(aether PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE TRUE
    NO_SONAME TRUE
)

target_compile_options(aether PRIVATE ${AETHER_COMPILE_OPTIONS})
target_link_options(aether PRIVATE ${AETHER_LINK_OPTIONS})

target_link_libraries(aether PRIVATE
    android
    log
)

# ─── libaethersec.so ───────────────────────────────────────────────────────
add_library(aethersec SHARED aethersec.cpp)

target_compile_features(aethersec PRIVATE cxx_std_17)

set_target_properties(aethersec PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE TRUE
    NO_SONAME TRUE
)

target_compile_options(aethersec PRIVATE ${COMMON_COMPILE_OPTIONS})
target_link_options(aethersec PRIVATE ${COMMON_LINK_OPTIONS})

target_link_libraries(aethersec PRIVATE
    android
    log
)


# ─── libaetherui.so ──────────────────────────────────────────────────────────
# Native helper ringan untuk warm-up JNI/linker dan low-level timing agar UI
# Compose lebih stabil saat first launch/first interaction.
add_library(aetherui SHARED aetherui.cpp)

target_compile_features(aetherui PRIVATE cxx_std_17)

set_target_properties(aetherui PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE TRUE
    NO_SONAME TRUE
)

target_compile_options(aetherui PRIVATE
    -O2
    -flto=thin
    -fstack-protector-strong
    -fPIC
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -fno-exceptions
    -fno-rtti
    -D_FORTIFY_SOURCE=2
    -DANDROID
    $<$<CONFIG:Release>:-DNDEBUG>
)

target_link_options(aetherui PRIVATE
    -flto=thin
    -Wl,--gc-sections
    -Wl,--strip-all
    -Wl,--exclude-libs,ALL
    -Wl,-z,noexecstack
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,--no-undefined
)

target_link_libraries(aetherui PRIVATE
    android
    log
)
