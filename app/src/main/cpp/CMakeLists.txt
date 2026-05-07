cmake_minimum_required(VERSION 3.22.1)

project("aether")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_library(aether SHARED
        libaether.cpp
)

add_library(jembut SHARED
        jembut.cpp
)

target_compile_features(aether PRIVATE cxx_std_17)
target_compile_features(jembut PRIVATE cxx_std_17)

target_compile_options(aether PRIVATE
        -fPIC
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        -ffunction-sections
        -fdata-sections
        -fstack-protector-strong
        -D_FORTIFY_SOURCE=2
)

target_compile_options(jembut PRIVATE
        -fPIC
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        -ffunction-sections
        -fdata-sections
        -fstack-protector-strong
        -D_FORTIFY_SOURCE=2
)

target_link_options(aether PRIVATE
        -Wl,--gc-sections
        -Wl,-z,relro
        -Wl,-z,now
        -Wl,-z,noexecstack
)

target_link_options(jembut PRIVATE
        -Wl,--gc-sections
        -Wl,-z,relro
        -Wl,-z,now
        -Wl,-z,noexecstack
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(aether
        ${log-lib}
        ${android-lib}
)

target_link_libraries(jembut
        ${log-lib}
        ${android-lib}
)
