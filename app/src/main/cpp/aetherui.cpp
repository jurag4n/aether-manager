#include <jni.h>
#include <android/log.h>
#include <sys/resource.h>
#include <unistd.h>
#include <time.h>
#include <atomic>

#define LOG_TAG "AetherUiBoost"
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static std::atomic<bool> g_ready{false};

static long now_nanos() {
    timespec ts{};
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return static_cast<long>(ts.tv_sec) * 1000000000L + ts.tv_nsec;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_util_UiNativeBoost_nativeWarmUp(JNIEnv*, jobject) {
    // Lightweight native warm-up: touches linker/JNI/clock/sysconf early so the
    // first Compose frames avoid doing this work during UI interaction.
    (void) now_nanos();
    (void) sysconf(_SC_NPROCESSORS_CONF);
    (void) getpriority(PRIO_PROCESS, 0);
    g_ready.store(true, std::memory_order_release);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_aether_manager_util_UiNativeBoost_nativeCpuCount(JNIEnv*, jobject) {
    long n = sysconf(_SC_NPROCESSORS_CONF);
    if (n < 1) n = 1;
    if (n > 256) n = 256;
    return static_cast<jint>(n);
}

extern "C" JNIEXPORT jlong JNICALL
Java_dev_aether_manager_util_UiNativeBoost_nativeFrameClockNanos(JNIEnv*, jobject) {
    return static_cast<jlong>(now_nanos());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_util_UiNativeBoost_nativeIsReady(JNIEnv*, jobject) {
    return g_ready.load(std::memory_order_acquire) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) {
    // Keep this tiny: no thread creation, no blocking I/O, no root calls.
    g_ready.store(false, std::memory_order_release);
    return JNI_VERSION_1_6;
}
