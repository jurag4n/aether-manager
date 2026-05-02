cmake_minimum_required(VERSION 3.22.1)

project(aether LANGUAGES CXX)

set(COMMON_COMPILE_OPTIONS
    -O3
    -flto=full
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
    $<$<NOT:$<CONFIG:Release>>:-DDEV_LOG=0>
)

set(COMMON_LINK_OPTIONS
    -flto=full
    -Wl,--gc-sections
    -Wl,--strip-all
    -Wl,--strip-debug
    -Wl,--exclude-libs,ALL
    -Wl,-z,noexecstack
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,notext
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
