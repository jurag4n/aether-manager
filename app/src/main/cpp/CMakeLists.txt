cmake_minimum_required(VERSION 3.22.1)

project(aether LANGUAGES CXX)

# ── Toolchain flags bersama (apply ke kedua library) ─────────────────────────

set(COMMON_COMPILE_OPTIONS
    # Optimasi
    -O3
    -flto=full

    # Hardening stack
    -fstack-protector-strong
    -fstack-clash-protection

    # Symbol hiding — semua internal invisible dari luar .so
    -fvisibility=hidden
    -fvisibility-inlines-hidden

    # Dead code elimination
    -ffunction-sections
    -fdata-sections

    # Strip debug / unwind info
    -fno-exceptions
    -fno-rtti
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables

    # Fortify
    -D_FORTIFY_SOURCE=2
    -DANDROID

    # PIC wajib untuk shared lib
    -fPIC

    # Anti-analysis: tambah control flow integrity
    -fsanitize=cfi-icall
    -flto   # CFI butuh LTO

    # String literal tidak di-merge (lebih sulit di-pattern-scan)
    -fno-merge-constants

    # Jangan emit frame pointer — susah di-trace stack
    -fomit-frame-pointer

    # Jangan sisakan info alignment pad yang bisa jadi signature
    -falign-functions=1
    -falign-loops=1
    -falign-jumps=1

    # Jangan expose build path di binary
    -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.
    -fmacro-prefix-map=${CMAKE_SOURCE_DIR}=.

    # Build defines
    $<$<CONFIG:Release>:-DNDEBUG>
    $<$<NOT:$<CONFIG:Release>>:-DDEV_LOG=1>
)

set(COMMON_LINK_OPTIONS
    -flto=full

    # Dead code strip
    -Wl,--gc-sections

    # Strip semua symbol table — tidak ada nama fungsi tersisa di binary
    -Wl,--strip-all
    -Wl,--strip-debug

    # Jangan expose symbol dari dependency lib ke consumer
    -Wl,--exclude-libs,ALL

    # Hardening memory
    -Wl,-z,noexecstack
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,notext

    # Tidak ada undefined symbol — crash di link time, bukan runtime
    -Wl,--no-undefined

    # Hash style GNU — lebih efisien dan sedikit info di section header
    -Wl,--hash-style=gnu

    # Buang section yang tidak diperlukan runtime
    -Wl,--discard-all

    # CFI linker support
    -fsanitize=cfi-icall
)

# =============================================================================
# libaether.so — Security Guard
# =============================================================================

add_library(aether SHARED libaether.cpp)

target_compile_features(aether PRIVATE cxx_std_17)

set_target_properties(aether PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE TRUE
    # Hapus SONAME dari .so — susah di-identify oleh tools
    NO_SONAME TRUE
)

target_compile_options(aether PRIVATE ${COMMON_COMPILE_OPTIONS})
target_link_options(aether PRIVATE ${COMMON_LINK_OPTIONS})

target_link_libraries(aether PRIVATE
    android
    log
)

# =============================================================================
# libcimolagent.so — System Info Agent
# =============================================================================

add_library(cimolagent SHARED libcimolagent.cpp)

target_compile_features(cimolagent PRIVATE cxx_std_17)

set_target_properties(cimolagent PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE TRUE
    NO_SONAME TRUE
)

# cimolagent tidak butuh CFI (tidak ada indirect call security-critical)
# tapi tetap pakai semua hardening lain
set(CIMOLAGENT_COMPILE_OPTIONS ${COMMON_COMPILE_OPTIONS})
list(REMOVE_ITEM CIMOLAGENT_COMPILE_OPTIONS
    -fsanitize=cfi-icall
    -flto
)
list(APPEND CIMOLAGENT_COMPILE_OPTIONS -flto=full)

set(CIMOLAGENT_LINK_OPTIONS ${COMMON_LINK_OPTIONS})
list(REMOVE_ITEM CIMOLAGENT_LINK_OPTIONS -fsanitize=cfi-icall)

target_compile_options(cimolagent PRIVATE ${CIMOLAGENT_COMPILE_OPTIONS})
target_link_options(cimolagent PRIVATE ${CIMOLAGENT_LINK_OPTIONS})

target_link_libraries(cimolagent PRIVATE
    android
    log
)
