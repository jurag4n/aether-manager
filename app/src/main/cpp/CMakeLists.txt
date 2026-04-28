cmake_minimum_required(VERSION 3.22.1)

project(aether LANGUAGES CXX)

add_library(aether SHARED libaether.cpp)

target_compile_features(aether PRIVATE cxx_std_17)

set_target_properties(aether PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE TRUE
)

target_compile_options(aether PRIVATE
    -O2
    -flto=full
    -fstack-protector-all
    -fPIC
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    -fno-exceptions
    -fno-rtti
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables
    -D_FORTIFY_SOURCE=2
    -DANDROID
    $<$<CONFIG:Release>:-DNDEBUG>
    $<$<NOT:$<CONFIG:Release>>:-DDEV_LOG=1>
)

target_link_options(aether PRIVATE
    -flto=full
    -Wl,--gc-sections
    -Wl,--strip-all
    -Wl,--exclude-libs,ALL
    -Wl,-z,noexecstack
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,nodlopen
    -Wl,--no-undefined
)

target_link_libraries(aether
    android
    log
)

add_library(cimolagent SHARED libcimolagent.cpp)

target_compile_features(cimolagent PRIVATE cxx_std_17)

set_target_properties(cimolagent PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE TRUE
)

target_compile_options(cimolagent PRIVATE
    -O2
    -flto=full
    -fstack-protector-all
    -fPIC
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    -fno-exceptions
    -fno-rtti
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables
    -D_FORTIFY_SOURCE=2
    -DANDROID
    $<$<CONFIG:Release>:-DNDEBUG>
)

target_link_options(cimolagent PRIVATE
    -flto=full
    -Wl,--gc-sections
    -Wl,--strip-all
    -Wl,--exclude-libs,ALL
    -Wl,-z,noexecstack
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,--no-undefined
)

target_link_libraries(cimolagent
    android
    log
)
