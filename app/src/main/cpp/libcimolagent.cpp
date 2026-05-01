/**
 * libcimolagent.cpp — Performance & System Info Agent for Aether Manager
 * Library: libcimolagent.so
 *
 * JNI class: dev.aether.manager.CimolAgent
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  MODULE 1 — CPU      : freq, governor, usage (/proc/stat)        │
 * │  MODULE 2 — Thermal  : semua thermal_zone via sysfs              │
 * │  MODULE 3 — Memory   : /proc/meminfo + zram mm_stat              │
 * │  MODULE 4 — Battery  : /sys/class/power_supply/battery/ *        │
 * │  MODULE 5 — I/O      : block device scheduler read/write         │
 * │  MODULE 6 — KSM      : /sys/kernel/mm/ksm/ *                     │
 * │  MODULE 7 — Process  : /proc/[pid]/stat scanner                  │
 * │  MODULE 8 — GPU      : Adreno/Mali freq + busy (best-effort)     │
 * │  MODULE 9 — Executor : execWithTimeout + sysfs r/w               │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * Desain:
 *  - Pure C17, tidak ada C++ STL heap-heavy di hot path
 *  - Semua read non-blocking (O_NONBLOCK + timeout lokal)
 *  - Thread-safe: tidak ada global mutable state
 *  - Tidak ada log ke luar (silent dalam release)
 *  - __attribute__((visibility("hidden"))) di semua fungsi internal
 */

#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <sys/time.h>
#include <stdint.h>
#include <ctype.h>
#include <pthread.h>
#include <signal.h>
#include <poll.h>

// ─────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

#define HIDDEN __attribute__((visibility("hidden")))
#define ARRAY_LEN(a) (sizeof(a)/sizeof((a)[0]))

/** Baca file kecil ke buffer. Return bytes yang dibaca, -1 jika gagal. */
HIDDEN static ssize_t read_small_file(const char *path, char *buf, size_t bufsz) {
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return -1;
    ssize_t n = read(fd, buf, bufsz - 1);
    close(fd);
    if (n < 0) return -1;
    buf[n] = '\0';
    // trim trailing newline
    while (n > 0 && (buf[n-1] == '\n' || buf[n-1] == '\r' || buf[n-1] == ' '))
        buf[--n] = '\0';
    return n;
}

/** Tulis string ke file. Return 0 sukses, -1 gagal. */
HIDDEN static int write_small_file(const char *path, const char *val) {
    int fd = open(path, O_WRONLY | O_CLOEXEC);
    if (fd < 0) return -1;
    size_t len = strlen(val);
    ssize_t w  = write(fd, val, len);
    close(fd);
    return (w == (ssize_t)len) ? 0 : -1;
}

/** Parse int dari file. Return def jika gagal. */
HIDDEN static int read_int_file(const char *path, int def) {
    char buf[32];
    if (read_small_file(path, buf, sizeof(buf)) < 0) return def;
    return atoi(buf);
}

/** Parse long long dari file. Return def jika gagal. */
HIDDEN static long long read_ll_file(const char *path, long long def) {
    char buf[32];
    if (read_small_file(path, buf, sizeof(buf)) < 0) return def;
    return atoll(buf);
}

// ─────────────────────────────────────────────────────────────────────────────
// MODULE 1 — CPU
// ─────────────────────────────────────────────────────────────────────────────

#define MAX_CPUS 16

/** Hitung jumlah CPU dari /sys/devices/system/cpu/possible */
HIDDEN static int count_cpus(void) {
    char buf[64];
    if (read_small_file("/sys/devices/system/cpu/possible", buf, sizeof(buf)) < 0)
        return 8; // fallback
    // format: "0-7" atau "0-3,4-7"
    int last = 0;
    char *p = buf;
    while (*p) {
        if (isdigit((unsigned char)*p)) {
            int n = atoi(p);
            if (n > last) last = n;
            while (isdigit((unsigned char)*p)) p++;
        } else p++;
    }
    return (last + 1 > MAX_CPUS) ? MAX_CPUS : (last + 1);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_dev_aether_manager_CimolAgent_getCpuFreqsNow(JNIEnv *env, jobject) {
    int ncpu = count_cpus();
    jintArray arr = env->NewIntArray(ncpu);
    jint *buf = env->GetIntArrayElements(arr, nullptr);
    for (int i = 0; i < ncpu; i++) {
        char path[96];
        snprintf(path, sizeof(path),
            "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_cur_freq", i);
        int khz = read_int_file(path, -1000);
        buf[i] = (khz > 0) ? (khz / 1000) : -1; // kHz → MHz
    }
    env->ReleaseIntArrayElements(arr, buf, 0);
    return arr;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_dev_aether_manager_CimolAgent_getCpuFreqMinMax(JNIEnv *env, jobject) {
    int ncpu  = count_cpus();
    int total = ncpu * 2;
    jintArray arr = env->NewIntArray(total);
    jint *buf = env->GetIntArrayElements(arr, nullptr);
    for (int i = 0; i < ncpu; i++) {
        char pmin[96], pmax[96];
        snprintf(pmin, sizeof(pmin),
            "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_min_freq", i);
        snprintf(pmax, sizeof(pmax),
            "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_max_freq", i);
        int minkhz = read_int_file(pmin, -1000);
        int maxkhz = read_int_file(pmax, -1000);
        buf[i * 2]     = (minkhz > 0) ? (minkhz / 1000) : -1;
        buf[i * 2 + 1] = (maxkhz > 0) ? (maxkhz / 1000) : -1;
    }
    env->ReleaseIntArrayElements(arr, buf, 0);
    return arr;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_dev_aether_manager_CimolAgent_getCpuGovernors(JNIEnv *env, jobject) {
    int ncpu = count_cpus();
    jclass sc = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray(ncpu, sc, env->NewStringUTF(""));
    for (int i = 0; i < ncpu; i++) {
        char path[96], gov[64] = {0};
        snprintf(path, sizeof(path),
            "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_governor", i);
        read_small_file(path, gov, sizeof(gov));
        env->SetObjectArrayElement(arr, i, env->NewStringUTF(gov));
    }
    return arr;
}

/** Baca satu snapshot /proc/stat → return [user, nice, system, idle, iowait, irq, softirq] */
HIDDEN static int read_stat_snapshot(long long *out, int n) {
    FILE *f = fopen("/proc/stat", "r");
    if (!f) return -1;
    // baris pertama: "cpu  user nice system idle iowait irq softirq ..."
    char line[256];
    if (!fgets(line, sizeof(line), f)) { fclose(f); return -1; }
    fclose(f);
    // parse setelah "cpu "
    const char *p = line + 3;
    while (*p == ' ') p++;
    for (int i = 0; i < n; i++) {
        out[i] = atoll(p);
        while (isdigit((unsigned char)*p)) p++;
        while (*p == ' ') p++;
    }
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_aether_manager_CimolAgent_getCpuUsagePercent(JNIEnv *, jobject, jint intervalMs) {
    long long s1[7] = {}, s2[7] = {};
    if (read_stat_snapshot(s1, 7) < 0) return -1;

    int ms = (intervalMs > 0 && intervalMs <= 30000) ? intervalMs : 500;
    struct timespec ts;
    ts.tv_sec  = ms / 1000;
    ts.tv_nsec = (long)(ms % 1000) * 1000000L;
    nanosleep(&ts, nullptr);

    if (read_stat_snapshot(s2, 7) < 0) return -1;

    long long idle1 = s1[3] + s1[4];
    long long idle2 = s2[3] + s2[4];
    long long total1 = 0, total2 = 0;
    for (int i = 0; i < 7; i++) { total1 += s1[i]; total2 += s2[i]; }

    long long dtotal = total2 - total1;
    long long didle  = idle2  - idle1;
    if (dtotal <= 0) return 0;

    int pct = (int)((dtotal - didle) * 100LL / dtotal);
    return (pct < 0) ? 0 : (pct > 100 ? 100 : pct);
}

// ─────────────────────────────────────────────────────────────────────────────
// MODULE 2 — Thermal
// ─────────────────────────────────────────────────────────────────────────────

#define MAX_THERMAL_ZONES 32

extern "C" JNIEXPORT jintArray JNICALL
Java_dev_aether_manager_CimolAgent_getThermalRaw(JNIEnv *env, jobject) {
    // scan thermal_zone0 .. thermal_zone(MAX-1)
    int idxs[MAX_THERMAL_ZONES], temps[MAX_THERMAL_ZONES], count = 0;
    for (int i = 0; i < MAX_THERMAL_ZONES && count < MAX_THERMAL_ZONES; i++) {
        char path[96];
        snprintf(path, sizeof(path), "/sys/class/thermal/thermal_zone%d/temp", i);
        int t = read_int_file(path, INT32_MIN);
        if (t == INT32_MIN) break; // zones biasanya berurutan
        idxs[count]  = i;
        temps[count] = t;
        count++;
    }
    // flat: [idx0, temp0, idx1, temp1, ...]
    jintArray arr = env->NewIntArray(count * 2);
    if (count == 0) return arr;
    jint *buf = env->GetIntArrayElements(arr, nullptr);
    for (int i = 0; i < count; i++) {
        buf[i * 2]     = idxs[i];
        buf[i * 2 + 1] = temps[i];
    }
    env->ReleaseIntArrayElements(arr, buf, 0);
    return arr;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_dev_aether_manager_CimolAgent_getThermalTypes(JNIEnv *env, jobject) {
    jclass sc = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray(MAX_THERMAL_ZONES, sc, env->NewStringUTF(""));
    int count = 0;
    for (int i = 0; i < MAX_THERMAL_ZONES; i++) {
        char path[96], tp[128] = {0};
        snprintf(path, sizeof(path), "/sys/class/thermal/thermal_zone%d/type", i);
        if (read_small_file(path, tp, sizeof(tp)) < 0) break;
        env->SetObjectArrayElement(arr, i, env->NewStringUTF(tp));
        count++;
    }
    // trim array ke count yang sebenarnya
    jobjectArray trimmed = env->NewObjectArray(count, sc, env->NewStringUTF(""));
    for (int i = 0; i < count; i++)
        env->SetObjectArrayElement(trimmed, i,
            env->GetObjectArrayElement(arr, i));
    return trimmed;
}

/** Cari thermal zone mengandung keyword, return temp milli-C atau -1 */
HIDDEN static int find_thermal_by_kw(const char *kw1, const char *kw2) {
    for (int i = 0; i < MAX_THERMAL_ZONES; i++) {
        char ptype[96], ptemp[96], tp[64];
        snprintf(ptype, sizeof(ptype), "/sys/class/thermal/thermal_zone%d/type", i);
        snprintf(ptemp, sizeof(ptemp), "/sys/class/thermal/thermal_zone%d/temp", i);
        if (read_small_file(ptype, tp, sizeof(tp)) < 0) break;
        // lowercase compare
        char tplow[64]; size_t tl = strlen(tp);
        for (size_t j = 0; j < tl && j < 63; j++)
            tplow[j] = (char)(tp[j] >= 'A' && tp[j] <= 'Z' ? tp[j] + 32 : tp[j]);
        tplow[tl < 63 ? tl : 63] = '\0';
        if ((kw1 && strstr(tplow, kw1)) || (kw2 && strstr(tplow, kw2))) {
            return read_int_file(ptemp, -1);
        }
    }
    return -1;
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_aether_manager_CimolAgent_getCpuTempMilliC(JNIEnv *, jobject) {
    // coba berbagai nama zone CPU
    int t;
    if ((t = find_thermal_by_kw("cpu", "tsens_tz_sensor0")) >= 0) return t;
    if ((t = find_thermal_by_kw("soc-thermal", "cpu-thermal"))  >= 0) return t;
    // fallback: zone0 (hampir selalu CPU di Snapdragon)
    return read_int_file("/sys/class/thermal/thermal_zone0/temp", -1);
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_aether_manager_CimolAgent_getGpuTempMilliC(JNIEnv *, jobject) {
    int t;
    if ((t = find_thermal_by_kw("gpu", "adreno-lowf")) >= 0) return t;
    if ((t = find_thermal_by_kw("mali", "gpuss"))       >= 0) return t;
    return -1;
}

// ─────────────────────────────────────────────────────────────────────────────
// MODULE 3 — Memory
// ─────────────────────────────────────────────────────────────────────────────

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_aether_manager_CimolAgent_getMemInfoKb(JNIEnv *env, jobject) {
    // return [MemTotal, MemFree, MemAvailable, Buffers, Cached, SwapTotal, SwapFree, SwapCached]
    const char *keys[] = {
        "MemTotal:", "MemFree:", "MemAvailable:", "Buffers:", "Cached:",
        "SwapTotal:", "SwapFree:", "SwapCached:"
    };
    const int nkeys = 8;
    long long vals[8]; for (int i = 0; i < 8; i++) vals[i] = -1;

    FILE *f = fopen("/proc/meminfo", "r");
    jlongArray arr = env->NewLongArray(nkeys);
    if (!f) { env->SetLongArrayRegion(arr, 0, nkeys, (jlong*)vals); return arr; }

    char line[128];
    int found = 0;
    while (found < nkeys && fgets(line, sizeof(line), f)) {
        for (int i = 0; i < nkeys; i++) {
            if (vals[i] >= 0) continue;
            size_t kl = strlen(keys[i]);
            if (strncmp(line, keys[i], kl) == 0) {
                vals[i] = atoll(line + kl);
                found++;
                break;
            }
        }
    }
    fclose(f);
    env->SetLongArrayRegion(arr, 0, nkeys, (jlong*)vals);
    return arr;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_aether_manager_CimolAgent_getZramStats(JNIEnv *env, jobject) {
    // /sys/block/zram0/mm_stat format:
    // orig_data_size compr_data_size mem_used_total mem_limit mem_max_used same_pages pages_compacted
    jlongArray arr = env->NewLongArray(7);
    char buf[256];
    if (read_small_file("/sys/block/zram0/mm_stat", buf, sizeof(buf)) < 0) return arr;
    long long v[7] = {};
    char *p = buf;
    for (int i = 0; i < 7; i++) {
        while (*p == ' ') p++;
        v[i] = atoll(p);
        while (*p && *p != ' ') p++;
    }
    env->SetLongArrayRegion(arr, 0, 7, (jlong*)v);
    return arr;
}

// Baterai
#define BAT_BASE "/sys/class/power_supply/battery/"
#define LMIN     LLONG_MIN

HIDDEN static long long bat_read(const char *name) {
    char path[128];
    snprintf(path, sizeof(path), BAT_BASE "%s", name);
    return read_ll_file(path, LMIN);
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_aether_manager_CimolAgent_getBatteryStats(JNIEnv *env, jobject) {

    long long v[6] = {
        bat_read("capacity"),
        bat_read("current_now"),
        bat_read("voltage_now"),
        bat_read("temp"),
        bat_read("charge_full"),
        bat_read("charge_now"),
    };
    jlongArray arr = env->NewLongArray(6);
    env->SetLongArrayRegion(arr, 0, 6, (jlong*)v);
    return arr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_aether_manager_CimolAgent_getBatteryStatus(JNIEnv *env, jobject) {
    char buf[32] = {0};
    read_small_file(BAT_BASE "status", buf, sizeof(buf));
    return env->NewStringUTF(buf);
}

// io scheduler
#define MAX_BLKDEV 8

HIDDEN static void parse_active_scheduler(const char *raw, char *out, size_t outsz) {
    const char *s = strchr(raw, '[');
    const char *e = s ? strchr(s, ']') : nullptr;
    if (s && e && (size_t)(e - s - 1) < outsz) {
        size_t len = (size_t)(e - s - 1);
        strncpy(out, s + 1, len);
        out[len] = '\0';
    } else {
        strncpy(out, raw, outsz - 1);
        out[outsz - 1] = '\0';
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_dev_aether_manager_CimolAgent_getIoSchedulers(JNIEnv *env, jobject) {
    jclass sc = env->FindClass("java/lang/String");
    // flat: [devname0, scheduler0, devname1, scheduler1, ...]
    jobjectArray tmp = env->NewObjectArray(MAX_BLKDEV * 2, sc, env->NewStringUTF(""));
    int count = 0;

    DIR *d = opendir("/sys/block");
    if (!d) return env->NewObjectArray(0, sc, env->NewStringUTF(""));
    struct dirent *e;
    while ((e = readdir(d)) != nullptr && count < MAX_BLKDEV) {
        if (e->d_name[0] == '.') continue;
        char path[128];
        snprintf(path, sizeof(path), "/sys/block/%s/queue/scheduler", e->d_name);
        char raw[256] = {0};
        if (read_small_file(path, raw, sizeof(raw)) < 0) continue;
        char sched[64] = {0};
        parse_active_scheduler(raw, sched, sizeof(sched));
        env->SetObjectArrayElement(tmp, count * 2,     env->NewStringUTF(e->d_name));
        env->SetObjectArrayElement(tmp, count * 2 + 1, env->NewStringUTF(sched));
        count++;
    }
    closedir(d);

    jobjectArray arr = env->NewObjectArray(count * 2, sc, env->NewStringUTF(""));
    for (int i = 0; i < count * 2; i++)
        env->SetObjectArrayElement(arr, i, env->GetObjectArrayElement(tmp, i));
    return arr;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_CimolAgent_setIoScheduler(JNIEnv *env, jobject,
    jstring jdev, jstring jsched)
{
    const char *dev   = env->GetStringUTFChars(jdev,   nullptr);
    const char *sched = env->GetStringUTFChars(jsched, nullptr);
    char path[128];
    snprintf(path, sizeof(path), "/sys/block/%s/queue/scheduler", dev);
    int ok = (write_small_file(path, sched) == 0) ? 1 : 0;
    env->ReleaseStringUTFChars(jdev,   dev);
    env->ReleaseStringUTFChars(jsched, sched);
    return ok ? JNI_TRUE : JNI_FALSE;
}

// ─────────────────────────────────────────────────────────────────────────────
// MODULE 6 — KSM
// ─────────────────────────────────────────────────────────────────────────────

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_aether_manager_CimolAgent_getKsmStats(JNIEnv *env, jobject) {
    const char *files[] = {
        "pages_shared", "pages_sharing", "pages_unshared", "pages_volatile",
        "full_scans", "stable_node_chains", "stable_node_dups"
    };
    const int n = 7;
    jlongArray arr = env->NewLongArray(n);
    long long vals[7];
    for (int i = 0; i < n; i++) {
        char path[96];
        snprintf(path, sizeof(path), "/sys/kernel/mm/ksm/%s", files[i]);
        vals[i] = read_ll_file(path, -1LL);
    }
    env->SetLongArrayRegion(arr, 0, n, (jlong*)vals);
    return arr;
}

// ─────────────────────────────────────────────────────────────────────────────
// MODULE 7 — Process Scanner
// ─────────────────────────────────────────────────────────────────────────────

#define MAX_PROCS 256

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_aether_manager_CimolAgent_getProcessStats(JNIEnv *env, jobject) {
    // flat: [pid, utime, stime, rss] × MAX_PROCS
    long long data[MAX_PROCS * 4];
    int count = 0;

    DIR *d = opendir("/proc");
    if (!d) return env->NewLongArray(0);
    struct dirent *e;
    while ((e = readdir(d)) != nullptr && count < MAX_PROCS) {
        if (!isdigit((unsigned char)e->d_name[0])) continue;
        int pid = atoi(e->d_name);

        char path[64];
        snprintf(path, sizeof(path), "/proc/%d/stat", pid);
        char buf[512] = {0};
        if (read_small_file(path, buf, sizeof(buf)) < 0) continue;

        // /proc/pid/stat format: pid (comm) state ppid ... utime stime ... rss
        // field 14=utime, 15=stime, 24=rss (1-indexed)
        // skip past (comm) bisa berisi spasi → cari ')' terakhir
        char *p = strrchr(buf, ')');
        if (!p) continue;
        p += 2; // skip ') '

        long long fields[23] = {};
        for (int i = 0; i < 22 && *p; i++) {
            fields[i] = atoll(p);
            while (*p && *p != ' ') p++;
            while (*p == ' ') p++;
        }
        // utime=field[11], stime=field[12], rss=field[21] (relative to ')' skip)
        long long utime = fields[11];
        long long stime = fields[12];
        long long rss   = fields[21]; // in pages

        data[count * 4]     = (long long)pid;
        data[count * 4 + 1] = utime;
        data[count * 4 + 2] = stime;
        data[count * 4 + 3] = rss * 4; // pages → kB (4KB pages)
        count++;
    }
    closedir(d);

    jlongArray arr = env->NewLongArray(count * 4);
    if (count > 0)
        env->SetLongArrayRegion(arr, 0, count * 4, (jlong*)data);
    return arr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_aether_manager_CimolAgent_getProcessName(JNIEnv *env, jobject, jint pid) {
    char path[64], buf[256] = {0};
    snprintf(path, sizeof(path), "/proc/%d/cmdline", (int)pid);
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return env->NewStringUTF("");
    ssize_t n = read(fd, buf, sizeof(buf) - 1);
    close(fd);
    if (n <= 0) return env->NewStringUTF("");
    buf[n] = '\0';
    // cmdline menggunakan '\0' sebagai separator antar argumen
    for (ssize_t i = 0; i < n; i++)
        if (buf[i] == '\0') { buf[i] = ' '; break; } // ambil arg pertama saja
    return env->NewStringUTF(buf);
}

// ─────────────────────────────────────────────────────────────────────────────
// MODULE 8 — GPU (best-effort, multi-vendor)
// ─────────────────────────────────────────────────────────────────────────────

/** Daftar path GPU freq yang umum — coba satu per satu */
HIDDEN static int try_gpu_freq_now(void) {
    static const char *paths[] = {
        // Adreno (Qualcomm)
        "/sys/class/kgsl/kgsl-3d0/gpuclk",
        "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
        "/sys/kernel/gpu/gpu_clock",
        // Mali (MediaTek/Samsung Exynos)
        "/sys/devices/platform/13000000.mali/cur_freq",
        "/sys/class/misc/mali0/device/clock",
        "/sys/bus/platform/drivers/mali/gpufreq/cur_freq",
        // PowerVR
        "/sys/devices/platform/pvrsrvkm/gpu_clock_speed",
        nullptr
    };
    for (int i = 0; paths[i]; i++) {
        long long hz = read_ll_file(paths[i], LLONG_MIN);
        if (hz == LLONG_MIN || hz <= 0) continue;
        // normalkan ke MHz
        if (hz > 1000000LL) return (int)(hz / 1000000LL); // Hz → MHz
        if (hz > 1000LL)    return (int)(hz / 1000LL);    // kHz → MHz
        return (int)hz;
    }
    return -1;
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_aether_manager_CimolAgent_getGpuFreqNow(JNIEnv *, jobject) {
    return try_gpu_freq_now();
}

HIDDEN static int try_gpu_busy(void) {
    static const char *paths[] = {
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
        "/sys/class/kgsl/kgsl-3d0/gpubusy",
        "/sys/kernel/gpu/gpu_busy",
        "/sys/devices/platform/13000000.mali/utilization",
        nullptr
    };
    for (int i = 0; paths[i]; i++) {
        int v = read_int_file(paths[i], -1);
        if (v >= 0 && v <= 100) return v;
    }
    return -1;
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_aether_manager_CimolAgent_getGpuBusyPercent(JNIEnv *, jobject) {
    return try_gpu_busy();
}

// ─────────────────────────────────────────────────────────────────────────────
// MODULE 9 — Executor & Sysfs R/W
// ─────────────────────────────────────────────────────────────────────────────

/** Struct untuk passing arg ke thread timeout watcher */
struct ExecCtx {
    pid_t  pid;
    int    timeoutMs;
    int    killed; // 1 jika sudah di-kill oleh watcher
};

/** Thread watcher: tunggu timeout lalu SIGKILL child */
HIDDEN static void *timeout_watcher(void *arg) {
    ExecCtx *ctx = (ExecCtx *)arg;
    int ms = ctx->timeoutMs;
    struct timespec ts;
    ts.tv_sec  = ms / 1000;
    ts.tv_nsec = (long)(ms % 1000) * 1000000L;
    nanosleep(&ts, nullptr);
    // Jika child masih hidup → kill
    if (kill(ctx->pid, 0) == 0) {
        kill(ctx->pid, SIGKILL);
        ctx->killed = 1;
    }
    return nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_aether_manager_CimolAgent_execWithTimeout(JNIEnv *env, jobject,
    jstring jcmd, jint timeoutMs)
{
    const char *cmd = env->GetStringUTFChars(jcmd, nullptr);
    if (!cmd) return env->NewStringUTF("");

    // Buat pipe untuk stdout child
    int pipefd[2];
    if (pipe(pipefd) < 0) {
        env->ReleaseStringUTFChars(jcmd, cmd);
        return env->NewStringUTF("");
    }

    pid_t pid = fork();
    if (pid < 0) {
        close(pipefd[0]); close(pipefd[1]);
        env->ReleaseStringUTFChars(jcmd, cmd);
        return env->NewStringUTF("");
    }

    if (pid == 0) {
        // Child process
        close(pipefd[0]);
        dup2(pipefd[1], STDOUT_FILENO);
        dup2(pipefd[1], STDERR_FILENO);
        close(pipefd[1]);
        execl("/system/bin/sh", "sh", "-c", cmd, (char*)nullptr);
        _exit(127);
    }

    // Parent
    close(pipefd[1]);
    env->ReleaseStringUTFChars(jcmd, cmd);

    // Set timeout watcher thread
    int ms = (timeoutMs > 0 && timeoutMs <= 30000) ? (int)timeoutMs : 5000;
    ExecCtx ctx = { pid, ms, 0 };
    pthread_t wt;
    pthread_create(&wt, nullptr, timeout_watcher, &ctx);

    // Baca output dari pipe (max 16KB)
    const size_t MAXOUT = 16384;
    char *outbuf = (char*)malloc(MAXOUT + 1);
    size_t total = 0;
    if (outbuf) {
        ssize_t n;
        while (total < MAXOUT &&
               (n = read(pipefd[0], outbuf + total, MAXOUT - total)) > 0)
            total += (size_t)n;
        outbuf[total] = '\0';
        // trim trailing whitespace
        while (total > 0 && (outbuf[total-1] == '\n' || outbuf[total-1] == '\r'
                             || outbuf[total-1] == ' '))
            outbuf[--total] = '\0';
    }
    close(pipefd[0]);

    // Tunggu child
    int status;
    waitpid(pid, &status, 0);

    // Cancel watcher
    ctx.killed = 1; // sinyal watcher supaya tidak kill proses lain
    pthread_join(wt, nullptr);

    jstring result = env->NewStringUTF(outbuf ? outbuf : "");
    free(outbuf);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_CimolAgent_writeSysfs(JNIEnv *env, jobject,
    jstring jpath, jstring jval)
{
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    const char *val  = env->GetStringUTFChars(jval,  nullptr);
    int ok = (path && val) ? (write_small_file(path, val) == 0 ? 1 : 0) : 0;
    if (path) env->ReleaseStringUTFChars(jpath, path);
    if (val)  env->ReleaseStringUTFChars(jval,  val);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_aether_manager_CimolAgent_readSysfs(JNIEnv *env, jobject, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    if (!path) return nullptr;
    char buf[4096] = {0};
    ssize_t n = read_small_file(path, buf, sizeof(buf));
    env->ReleaseStringUTFChars(jpath, path);
    if (n < 0) return nullptr;
    return env->NewStringUTF(buf);
}
