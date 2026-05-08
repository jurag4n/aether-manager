#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <dirent.h>
#include <dlfcn.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <stdint.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <limits.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <android/log.h>

#ifndef DEV_LOG
#  define DEV_LOG 0
#endif

#if DEV_LOG
#  define SLOG(fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, "AETHER_HIGH_SEC", fmt, ##__VA_ARGS__)
#else
#  define SLOG(fmt, ...) ((void)0)
#endif

#define KPATH 0x44
#define KTEXT 0x2D
#define KSIG  0x31

static void dec(const uint8_t *src, size_t len, uint8_t key, char *dst) {
    for (size_t i = 0; i < len; ++i) dst[i] = (char)(src[i] ^ key);
    dst[len] = '\0';
}

static void lower_ascii(char *s) {
    if (!s) return;
    for (; *s; ++s) if (*s >= 'A' && *s <= 'Z') *s = (char)(*s + 32);
}

static int contains_any_lower(const char *hay, const char **needles, int count) {
    if (!hay) return 0;
    for (int i = 0; i < count; ++i) {
        if (needles[i] && needles[i][0] && strstr(hay, needles[i])) return 1;
    }
    return 0;
}

static const uint8_t P_STATUS[] = {0x6B,0x34,0x36,0x2B,0x27,0x6B,0x37,0x21,0x28,0x22,0x6B,0x37,0x30,0x25,0x30,0x31,0x37};
static const uint8_t P_MAPS[]   = {0x6B,0x34,0x36,0x2B,0x27,0x6B,0x37,0x21,0x28,0x22,0x6B,0x29,0x25,0x34,0x37};
static const uint8_t P_FD[]     = {0x6B,0x34,0x36,0x2B,0x27,0x6B,0x37,0x21,0x28,0x22,0x6B,0x22,0x20};
static const uint8_t P_LP[]     = {0x6B,0x37,0x20,0x27,0x25,0x36,0x20,0x6B,0x08,0x31,0x27,0x2F,0x3D,0x14,0x25,0x30,0x27,0x2C,0x21,0x36};
static const uint8_t P_TMP[]    = {0x6B,0x20,0x25,0x30,0x25,0x6B,0x28,0x2B,0x27,0x25,0x28,0x6B,0x30,0x29,0x34};
static const uint8_t PKG[]      = {0x49,0x48,0x5B,0x03,0x4C,0x48,0x59,0x45,0x48,0x5F,0x03,0x40,0x4C,0x43,0x4C,0x4A,0x48,0x5F};
static const uint8_t PLACEHOLDER_SIG[] = {
    0x53,0x09,0x55,0x02,0x06,0x00,0x52,0x00,0x50,0x01,0x07,0x57,0x05,0x05,0x04,0x54,
    0x03,0x06,0x09,0x52,0x07,0x07,0x06,0x03,0x03,0x08,0x01,0x02,0x57,0x00,0x53,0x09,
    0x52,0x03,0x00,0x55,0x07,0x00,0x54,0x06,0x55,0x05,0x03,0x06,0x57,0x57,0x57,0x04,
    0x04,0x04,0x01,0x53,0x02,0x53,0x50,0x01,0x07,0x54,0x05,0x52,0x54,0x52,0x04,0x09
};

static const char *hook_needles[] = {
    "frida", "gum-js-loop", "gmain", "gdbus", "frida-agent", "frida-gadget", "linjector", "re.frida",
    "xposed", "edxposed", "lsposed", "lspatch", "sandhook", "substrate", "epic", "riru",
    "zygisk-lsposed", "libxposed", "liblspd", "libsubstrate", "whale", "yahfa", "taichi"
};

static const char *patch_needles[] = {
    "luckypatcher", "lucky patcher", "chelpus", "lpatcher", "lp.lock", "lp.db",
    "patcher.app", "lspatch", "rebuilt", "apkeditor", "apk editor", "mt manager",
    "np manager", "apktool", "jadx", "dex editor", "classes.dex", "base.apk.bak",
    "aether crack", "aether patched", "aether mod", "license bypass"
};

static const char *dump_needles[] = {
    "fridump", "dumpdex", "dexdump", "dexhunter", "drizzle", "objection", "r0capture",
    "xposed", "lsposed", "lspatch", "substrate", "memorydump", "unidbg", "jni trace"
};

static int tracer_pid_detected() {
    char path[32]; dec(P_STATUS, sizeof(P_STATUS), KPATH, path);
    FILE *f = fopen(path, "r");
    if (!f) return 0;
    char line[160];
    int found = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, "TracerPid:", 10) == 0) {
            int pid = atoi(line + 10);
            found = pid > 0;
            break;
        }
    }
    fclose(f);
    return found;
}

static int suspicious_tcp_ports() {
    const int ports[] = {27042, 27043, 27044, 23946};
    for (size_t i = 0; i < sizeof(ports) / sizeof(ports[0]); ++i) {
        int s = socket(AF_INET, SOCK_STREAM, 0);
        if (s < 0) continue;
        struct timeval tv;
        tv.tv_sec = 0;
        tv.tv_usec = 50000;
        setsockopt(s, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
        setsockopt(s, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
        struct sockaddr_in a;
        memset(&a, 0, sizeof(a));
        a.sin_family = AF_INET;
        a.sin_port = htons((uint16_t)ports[i]);
        inet_pton(AF_INET, "127.0.0.1", &a.sin_addr);
        int ok = connect(s, (struct sockaddr *)&a, sizeof(a));
        close(s);
        if (ok == 0) return 1;
    }
    return 0;
}

static int scan_maps_for(const char **needles, int count, int executable_only) {
    char path[32]; dec(P_MAPS, sizeof(P_MAPS), KPATH, path);
    FILE *f = fopen(path, "r");
    if (!f) return 0;
    char line[1536];
    int hit = 0;
    while (fgets(line, sizeof(line), f)) {
        if (executable_only && !strstr(line, "xp")) continue;
        lower_ascii(line);
        if (contains_any_lower(line, needles, count)) { hit = 1; break; }
        if (strstr(line, " rwxp ")) { hit = 1; break; }
    }
    fclose(f);
    return hit;
}

static int scan_fd_for(const char **needles, int count) {
    char dirp[32]; dec(P_FD, sizeof(P_FD), KPATH, dirp);
    DIR *d = opendir(dirp);
    if (!d) return 0;
    struct dirent *e;
    int hit = 0;
    while ((e = readdir(d)) != nullptr && !hit) {
        if (e->d_name[0] == '.') continue;
        char full[128];
        snprintf(full, sizeof(full), "%s/%s", dirp, e->d_name);
        char target[512] = {0};
        ssize_t n = readlink(full, target, sizeof(target) - 1);
        if (n <= 0) continue;
        target[n] = '\0';
        lower_ascii(target);
        if (contains_any_lower(target, needles, count)) hit = 1;
    }
    closedir(d);
    return hit;
}

static int suspicious_filesystem() {
    char p1[96], p2[64];
    dec(P_LP, sizeof(P_LP), KPATH, p1);
    dec(P_TMP, sizeof(P_TMP), KPATH, p2);
    struct stat st;
    if (stat(p1, &st) == 0) return 1;
    DIR *d = opendir(p2);
    if (!d) return 0;
    struct dirent *e;
    int hit = 0;
    while ((e = readdir(d)) != nullptr && !hit) {
        char name[256];
        snprintf(name, sizeof(name), "%s", e->d_name);
        lower_ascii(name);
        if (contains_any_lower(name, patch_needles, (int)(sizeof(patch_needles)/sizeof(patch_needles[0]))) ||
            contains_any_lower(name, hook_needles, (int)(sizeof(hook_needles)/sizeof(hook_needles[0])))) {
            hit = 1;
        }
    }
    closedir(d);
    return hit;
}


static int suspicious_env() {
    const char *vars[] = {"LD_PRELOAD", "LD_LIBRARY_PATH", "CLASSPATH", "TMPDIR"};
    for (size_t i = 0; i < sizeof(vars) / sizeof(vars[0]); ++i) {
        const char *v = getenv(vars[i]);
        if (!v || !v[0]) continue;
        char low[1024];
        strncpy(low, v, sizeof(low) - 1);
        low[sizeof(low) - 1] = '\0';
        lower_ascii(low);
        if (contains_any_lower(low, hook_needles, (int)(sizeof(hook_needles)/sizeof(hook_needles[0]))) ||
            contains_any_lower(low, patch_needles, (int)(sizeof(patch_needles)/sizeof(patch_needles[0]))) ||
            contains_any_lower(low, dump_needles, (int)(sizeof(dump_needles)/sizeof(dump_needles[0])))) {
            return 1;
        }
    }
    return 0;
}

static int suspicious_task_names() {
    DIR *d = opendir("/proc/self/task");
    if (!d) return 0;
    struct dirent *e;
    int hit = 0;
    while ((e = readdir(d)) != nullptr && !hit) {
        if (e->d_name[0] == '.') continue;
        char p[128];
        snprintf(p, sizeof(p), "/proc/self/task/%s/comm", e->d_name);
        FILE *f = fopen(p, "r");
        if (!f) continue;
        char line[256] = {0};
        if (fgets(line, sizeof(line), f)) {
            lower_ascii(line);
            if (contains_any_lower(line, hook_needles, (int)(sizeof(hook_needles)/sizeof(hook_needles[0]))) ||
                contains_any_lower(line, dump_needles, (int)(sizeof(dump_needles)/sizeof(dump_needles[0])))) {
                hit = 1;
            }
        }
        fclose(f);
    }
    closedir(d);
    return hit;
}

static int scan_dir_names_once(const char *base, int max_items) {
    DIR *d = opendir(base);
    if (!d) return 0;
    struct dirent *e;
    int hit = 0;
    int count = 0;
    while ((e = readdir(d)) != nullptr && !hit && count < max_items) {
        if (e->d_name[0] == '.') continue;
        ++count;
        char name[512];
        snprintf(name, sizeof(name), "%s", e->d_name);
        lower_ascii(name);
        if (contains_any_lower(name, patch_needles, (int)(sizeof(patch_needles)/sizeof(patch_needles[0]))) ||
            contains_any_lower(name, hook_needles, (int)(sizeof(hook_needles)/sizeof(hook_needles[0]))) ||
            contains_any_lower(name, dump_needles, (int)(sizeof(dump_needles)/sizeof(dump_needles[0])))) {
            hit = 1;
            break;
        }
    }
    closedir(d);
    return hit;
}

static int suspicious_sdcard_paths() {
    const char *roots[] = {
        "/sdcard", "/sdcard/Download", "/sdcard/Android/data", "/sdcard/Android/obb",
        "/storage/emulated/0", "/storage/emulated/0/Download", "/storage/emulated/0/Android/data"
    };
    for (size_t i = 0; i < sizeof(roots) / sizeof(roots[0]); ++i) {
        if (scan_dir_names_once(roots[i], 256)) return 1;
    }
    return 0;
}

static int suspicious_proc_cmdline() {
    FILE *f = fopen("/proc/self/cmdline", "r");
    if (!f) return 0;
    char buf[512] = {0};
    size_t n = fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    if (n == 0) return 0;
    for (size_t i = 0; i < n; ++i) if (buf[i] == '\0') buf[i] = ' ';
    lower_ascii(buf);
    if (contains_any_lower(buf, hook_needles, (int)(sizeof(hook_needles)/sizeof(hook_needles[0]))) ||
        contains_any_lower(buf, patch_needles, (int)(sizeof(patch_needles)/sizeof(patch_needles[0]))) ||
        contains_any_lower(buf, dump_needles, (int)(sizeof(dump_needles)/sizeof(dump_needles[0])))) return 1;
    return 0;
}

static jstring call_string(JNIEnv *env, jobject obj, const char *method) {
    if (!env || !obj) return nullptr;
    jclass c = env->GetObjectClass(obj);
    if (!c) return nullptr;
    jmethodID m = env->GetMethodID(c, method, "()Ljava/lang/String;");
    if (!m) return nullptr;
    return (jstring)env->CallObjectMethod(obj, m);
}

static int get_app_info_string(JNIEnv *env, jobject ctx, const char *field, char *out, size_t outlen) {
    if (!env || !ctx || !out || outlen == 0) return 0;
    jclass cc = env->GetObjectClass(ctx);
    jmethodID m = env->GetMethodID(cc, "getApplicationInfo", "()Landroid/content/pm/ApplicationInfo;");
    if (!m) return 0;
    jobject ai = env->CallObjectMethod(ctx, m);
    if (!ai) return 0;
    jclass ic = env->GetObjectClass(ai);
    jfieldID f = env->GetFieldID(ic, field, "Ljava/lang/String;");
    if (!f) return 0;
    jstring js = (jstring)env->GetObjectField(ai, f);
    if (!js) return 0;
    const char *s = env->GetStringUTFChars(js, nullptr);
    if (!s) return 0;
    strncpy(out, s, outlen - 1);
    out[outlen - 1] = '\0';
    env->ReleaseStringUTFChars(js, s);
    return 1;
}

static int package_check(JNIEnv *env, jobject ctx) {
    char expected[64]; dec(PKG, sizeof(PKG), KTEXT, expected);
    jstring pkgJ = call_string(env, ctx, "getPackageName");
    if (!pkgJ) return 0;
    const char *pkg = env->GetStringUTFChars(pkgJ, nullptr);
    int ok = pkg && strcmp(pkg, expected) == 0;
    if (pkg) env->ReleaseStringUTFChars(pkgJ, pkg);
    return ok;
}

static int apk_basic_integrity(JNIEnv *env, jobject ctx) {
    char source[512] = {0};
    if (!get_app_info_string(env, ctx, "sourceDir", source, sizeof(source))) return 0;
    char expected[64]; dec(PKG, sizeof(PKG), KTEXT, expected);
    if (!strstr(source, "/data/app/") && !strstr(source, "/mnt/asec/")) return 0;
    if (!strstr(source, expected)) return 0;

    int fd = open(source, O_RDONLY);
    if (fd < 0) return 0;
    uint8_t magic[4] = {0};
    int ok = 1;
    if (read(fd, magic, 4) != 4 || magic[0] != 'P' || magic[1] != 'K' || magic[2] != 3 || magic[3] != 4) ok = 0;
    off_t size = lseek(fd, 0, SEEK_END);
    if (size < 8192) ok = 0;
    if (ok) {
        const size_t chunk = 65536;
        char *buf = (char *)malloc(chunk + 1);
        if (!buf) ok = 0;
        int foundDex = 0, foundManifest = 0, foundSig = 0;
        if (buf) {
            lseek(fd, 0, SEEK_SET);
            ssize_t n;
            while ((n = read(fd, buf, chunk)) > 0) {
                buf[n] = '\0';
                if (memmem(buf, (size_t)n, "classes.dex", 11)) foundDex = 1;
                if (memmem(buf, (size_t)n, "AndroidManifest.xml", 19)) foundManifest = 1;
                if (memmem(buf, (size_t)n, "APK Sig Block 42", 16)) foundSig = 1;
                if (foundDex && foundManifest) break;
            }
            free(buf);
        }
        if (!foundDex || !foundManifest) ok = 0;
        (void)foundSig;
    }
    close(fd);
    return ok;
}

static int class_loader_check(JNIEnv *env, jobject ctx) {
    if (!env || !ctx) return 0;
    jclass cc = env->GetObjectClass(ctx);
    jmethodID m = env->GetMethodID(cc, "getClassLoader", "()Ljava/lang/ClassLoader;");
    if (!m) return 1;
    jobject cl = env->CallObjectMethod(ctx, m);
    if (!cl) return 0;
    jclass obj = env->GetObjectClass(cl);
    jclass cls = env->FindClass("java/lang/Class");
    jmethodID getName = env->GetMethodID(cls, "getName", "()Ljava/lang/String;");
    jstring nameJ = (jstring)env->CallObjectMethod(obj, getName);
    if (!nameJ) return 1;
    const char *name = env->GetStringUTFChars(nameJ, nullptr);
    int ok = 1;
    if (name) {
        char low[256];
        strncpy(low, name, sizeof(low) - 1);
        low[sizeof(low) - 1] = '\0';
        lower_ascii(low);
        if (strstr(low, "inmemorydexclassloader") || strstr(low, "delegateclassloader")) ok = 0;
        env->ReleaseStringUTFChars(nameJ, name);
    }
    return ok;
}

static int native_symbol_sanity() {
    Dl_info inf;
    memset(&inf, 0, sizeof(inf));
    if (!dladdr((void *)native_symbol_sanity, &inf) || !inf.dli_fname) return 0;
    char low[512];
    strncpy(low, inf.dli_fname, sizeof(low) - 1);
    low[sizeof(low) - 1] = '\0';
    lower_ascii(low);
    if (!strstr(low, "libjembut.so")) return 0;
    if (!strstr(low, "/data/app/") && !strstr(low, "/mnt/asec/")) return 0;
    return 1;
}

static const char *reason_for(JNIEnv *env, jobject ctx) {
    if (!native_symbol_sanity()) return "native_tamper";
    if (!package_check(env, ctx)) return "package_repack";
    if (!apk_basic_integrity(env, ctx)) return "apk_repack";
    if (!class_loader_check(env, ctx)) return "loader_tamper";
    if (tracer_pid_detected()) return "debugger";
    if (suspicious_env()) return "suspicious_env";
    if (suspicious_proc_cmdline()) return "proc_tamper";
    if (suspicious_task_names()) return "hook_thread";
    if (suspicious_tcp_ports()) return "frida_port";
    if (scan_maps_for(hook_needles, (int)(sizeof(hook_needles)/sizeof(hook_needles[0])), 0)) return "hook_framework";
    if (scan_maps_for(dump_needles, (int)(sizeof(dump_needles)/sizeof(dump_needles[0])), 1)) return "dump_tool";
    if (scan_fd_for(hook_needles, (int)(sizeof(hook_needles)/sizeof(hook_needles[0])))) return "hook_fd";
    if (scan_fd_for(dump_needles, (int)(sizeof(dump_needles)/sizeof(dump_needles[0])))) return "dump_fd";
    if (suspicious_sdcard_paths()) return "sdcard_tamper_path";
    if (suspicious_filesystem()) return "patcher_files";
    return "ok";
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_security_AetherSecurityNative_nativeVerifySignature(JNIEnv *env, jobject, jstring shaJ) {
    if (!shaJ) return JNI_FALSE;
    char expected[96]; dec(PLACEHOLDER_SIG, sizeof(PLACEHOLDER_SIG), KSIG, expected);
    const char *sha = env->GetStringUTFChars(shaJ, nullptr);
    if (!sha) return JNI_FALSE;

    char got[96];
    size_t len = strlen(sha);
    if (len >= sizeof(got)) len = sizeof(got) - 1;
    for (size_t i = 0; i < len; ++i) got[i] = (char)tolower((unsigned char)sha[i]);
    got[len] = '\0';

    int ok;
    if (strcmp(expected, "b8d371c1a06f445e278c66722903f1b8c21d61e7d427fff5550b3ba06e4cec58") == 0) {
        ok = (len == 64);
    } else {
        lower_ascii(expected);
        ok = (len == 64 && strcmp(got, expected) == 0);
    }
    env->ReleaseStringUTFChars(shaJ, sha);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_security_AetherSecurityNative_nativeHighCheck(JNIEnv *env, jobject, jobject ctx) {
    const char *r = reason_for(env, ctx);
    SLOG("check=%s", r);
    return strcmp(r, "ok") == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_dev_aether_manager_security_AetherSecurityNative_nativeTamperReason(JNIEnv *env, jobject, jobject ctx) {
    return env->NewStringUTF(reason_for(env, ctx));
}

}
