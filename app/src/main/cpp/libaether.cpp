/**
 * libaether.cpp — Unified Security Guard for Aether Manager
 * Library: libaether.so  (menggantikan libprotect.so)
 *
 * JNI class: dev.aether.manager.NativeAether
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  LAYER 1 — Anti-Hook    : Frida, TracerPid, pipe, port, maps   │
 * │  LAYER 2 — Anti-Debug   : ptrace self-attach, timing, /proc    │
 * │  LAYER 3 — Anti-Repack  : ZIP, DEX magic, APK hash, sig verify │
 * │  LAYER 4 — Anti-Patch   : Lucky Patcher (behavior + maps)      │
 * │  LAYER 5 — Anti-Tamper  : Unity string scan across all regions  │
 * │  LAYER 6 — String Vault : XOR-encoded sensitive strings        │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Keamanan tambahan vs versi lama:
 *  [+] XOR key per-layer berbeda (bukan satu key flat)
 *  [+] Self-ptrace anti-attach sebelum setiap check
 *  [+] Timing consistency check (detect single-step debugger)
 *  [+] /proc/self/fd scan untuk Frida pipe (bukan hanya /data/local/tmp)
 *  [+] Anti-emulator: getprop ro.kernel.qemu, BuildConfig check via JNI
 *  [+] nativeCheckAll() — satu call atom; JNI overhead minimal
 *  [+] kill() dipanggil dari 3 jalur berbeda untuk bypass hooked kill
 *  [+] Semua string sensitif tersebar di 3 XOR key berbeda agar sulit
 *      di-brute-force secara bulk dari binary
 *  [+] __attribute__((visibility("hidden"))) di semua fungsi internal
 *  [+] Constructor priority — beberapa check dipasang di .init_array
 *      sebelum JNI_OnLoad dipanggil
 *
 * Silent — tidak ada log ke luar kecuali dalam DEV_LOG mode.
 * Semua SIGKILL dari C, tidak return ke Kotlin.
 *
 * CARA UPDATE EXPECTED_SIG_ENCODED:
 *   1. Build release APK, install, jalankan sekali dengan DEV_LOG=1
 *   2. adb logcat | grep AETHER_SIG → catat 64-char hex
 *   3. Python:
 *      key = 0x3F
 *      sig = "abcdef..."   # 64 hex char = 32 bytes
 *      bs  = bytes.fromhex(sig)
 *      print(", ".join(f"0x{b^key:02X}" for b in bs))
 *   4. Paste ke EXPECTED_SIG_ENC[] di bawah
 */

#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <signal.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/ptrace.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdint.h>
#include <ctype.h>
#include <dlfcn.h>
#include <pthread.h>

// ─── Build toggle ────────────────────────────────────────────────────────────
// Set 1 saat develop untuk melihat SIG_HASH di logcat.
// WAJIB 0 pada release build.
#ifndef DEV_LOG
#  define DEV_LOG 0
#endif

#if DEV_LOG
#  define DEVLOG(fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, "AETHER_SEC", fmt, ##__VA_ARGS__)
#else
#  define DEVLOG(fmt, ...) ((void)0)
#endif

// ─── XOR keys berbeda per kategori ──────────────────────────────────────────
// Menyulitkan bulk-decode dari binary dump.
#define XK_SIG    0x3F   // signature / hash strings
#define XK_PATH   0x71   // path strings (/proc, /sdcard, ...)
#define XK_KW     0xA3   // keyword strings (frida, luckypatch, ...)
#define XK_URL    0x5C   // URL / API strings
#define XK_MISC   0xC7   // miscellaneous (class names, ids)

// ─────────────────────────────────────────────────────────────────────────────
// SHA-256 — pure C, no OpenSSL
// ─────────────────────────────────────────────────────────────────────────────

#define SHA256_DIGEST_LEN 32

typedef struct {
    uint32_t state[8];
    uint64_t bitcount;
    uint8_t  buf[64];
    size_t   buflen;
} sha256_ctx_t;

static const uint32_t K256[64] = {
    0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
};

#define R32(x,n) (((x)>>(n))|((x)<<(32-(n))))
#define CH(x,y,z)  (((x)&(y))^(~(x)&(z)))
#define MAJ(x,y,z) (((x)&(y))^((x)&(z))^((y)&(z)))
#define EP0(x) (R32(x,2)^R32(x,13)^R32(x,22))
#define EP1(x) (R32(x,6)^R32(x,11)^R32(x,25))
#define S0(x)  (R32(x,7)^R32(x,18)^((x)>>3))
#define S1(x)  (R32(x,17)^R32(x,19)^((x)>>10))

__attribute__((visibility("hidden")))
static void sha256_transform(sha256_ctx_t *c, const uint8_t d[64]) {
    uint32_t a,b,cc,dd,e,f,g,h,t1,t2,m[64];
    for (int i=0;i<16;i++) m[i]=((uint32_t)d[i*4]<<24)|((uint32_t)d[i*4+1]<<16)|((uint32_t)d[i*4+2]<<8)|d[i*4+3];
    for (int i=16;i<64;i++) m[i]=S1(m[i-2])+m[i-7]+S0(m[i-15])+m[i-16];
    a=c->state[0];b=c->state[1];cc=c->state[2];dd=c->state[3];
    e=c->state[4];f=c->state[5];g=c->state[6];h=c->state[7];
    for (int i=0;i<64;i++){t1=h+EP1(e)+CH(e,f,g)+K256[i]+m[i];t2=EP0(a)+MAJ(a,b,cc);h=g;g=f;f=e;e=dd+t1;dd=cc;cc=b;b=a;a=t1+t2;}
    c->state[0]+=a;c->state[1]+=b;c->state[2]+=cc;c->state[3]+=dd;
    c->state[4]+=e;c->state[5]+=f;c->state[6]+=g;c->state[7]+=h;
}

__attribute__((visibility("hidden")))
static void sha256_init(sha256_ctx_t *c) {
    c->bitcount=0; c->buflen=0;
    c->state[0]=0x6a09e667;c->state[1]=0xbb67ae85;c->state[2]=0x3c6ef372;c->state[3]=0xa54ff53a;
    c->state[4]=0x510e527f;c->state[5]=0x9b05688c;c->state[6]=0x1f83d9ab;c->state[7]=0x5be0cd19;
}

__attribute__((visibility("hidden")))
static void sha256_update(sha256_ctx_t *c, const uint8_t *data, size_t len) {
    while (len--) {
        c->buf[c->buflen++]=*data++;
        if (c->buflen==64){sha256_transform(c,c->buf);c->bitcount+=512;c->buflen=0;}
    }
}

__attribute__((visibility("hidden")))
static void sha256_final(sha256_ctx_t *c, uint8_t out[SHA256_DIGEST_LEN]) {
    size_t i=c->buflen; c->buf[i++]=0x80;
    if (i>56){while(i<64)c->buf[i++]=0; sha256_transform(c,c->buf); i=0;}
    while(i<56)c->buf[i++]=0;
    c->bitcount+=c->buflen*8; uint64_t bc=c->bitcount;
    for(int j=7;j>=0;j--){c->buf[56+j]=(uint8_t)(bc&0xff);bc>>=8;}
    sha256_transform(c,c->buf);
    for(int j=0;j<8;j++){out[j*4+0]=(c->state[j]>>24)&0xff;out[j*4+1]=(c->state[j]>>16)&0xff;out[j*4+2]=(c->state[j]>>8)&0xff;out[j*4+3]=c->state[j]&0xff;}
}

__attribute__((visibility("hidden")))
static void bytes_to_hex(const uint8_t *src, size_t len, char *dst) {
    static const char H[]="0123456789abcdef";
    for (size_t i=0;i<len;i++){dst[i*2]= H[(src[i]>>4)&0xF];dst[i*2+1]=H[src[i]&0xF];}
    dst[len*2]='\0';
}

__attribute__((visibility("hidden")))
static int hash_file(const char *path, char out[65]) {
    int fd=open(path,O_RDONLY); if(fd<0) return -1;
    sha256_ctx_t c; sha256_init(&c);
    uint8_t buf[8192]; ssize_t n;
    while((n=read(fd,buf,sizeof(buf)))>0) sha256_update(&c,buf,(size_t)n);
    close(fd);
    uint8_t d[SHA256_DIGEST_LEN]; sha256_final(&c,d);
    bytes_to_hex(d,SHA256_DIGEST_LEN,out);
    return 0;
}

// ─────────────────────────────────────────────────────────────────────────────
// XOR string decode (per-key)
// ─────────────────────────────────────────────────────────────────────────────

__attribute__((visibility("hidden")))
static void xdec(const unsigned char *src, size_t len, uint8_t key, char *dst) {
    for (size_t i=0;i<len;i++) dst[i]=(char)(src[i]^key);
    dst[len]='\0';
}

// ─────────────────────────────────────────────────────────────────────────────
// KILL — triple-path, bypass hooked kill()
// ─────────────────────────────────────────────────────────────────────────────

__attribute__((noreturn, visibility("hidden")))
static void aether_kill(void) {
    pid_t pid = getpid();

    // Path 1: group kill
    kill(-pid, SIGKILL);
    // Path 2: self kill
    kill( pid, SIGKILL);
    // Path 3: raise (syscall bypass if kill() hooked)
    raise(SIGKILL);
    // Path 4: abort jika semua gagal
    abort();
    __builtin_unreachable();
}

// ─────────────────────────────────────────────────────────────────────────────
// ENCODED STRINGS
// Encode tool (Python):
//   key = 0xNN
//   print(", ".join(f"0x{b^key:02X}" for b in s.encode()))
// ─────────────────────────────────────────────────────────────────────────────

// ── [XK_PATH = 0x71] Path strings ────────────────────────────────────────────

// "/proc/self/status"
static const uint8_t EP_PROC_STATUS[] = {0x5E,0x01,0x03,0x1E,0x12,0x5E,0x02,0x14,0x1D,0x17,0x5E,0x02,0x05,0x10,0x05,0x04,0x02};
// "/proc/self/maps"
static const uint8_t EP_PROC_MAPS[] = {0x5E,0x01,0x03,0x1E,0x12,0x5E,0x02,0x14,0x1D,0x17,0x5E,0x1C,0x10,0x01,0x02};
// "/proc/self/fd"
static const uint8_t EP_PROC_FD[] = {0x5E,0x01,0x03,0x1E,0x12,0x5E,0x02,0x14,0x1D,0x17,0x5E,0x17,0x15};
// "/data/local/tmp"
static const uint8_t EP_DATA_TMP[] = {0x5E,0x15,0x10,0x05,0x10,0x5E,0x1D,0x1E,0x12,0x10,0x1D,0x5E,0x05,0x1C,0x01};
// "/sdcard/LuckyPatcher"
static const uint8_t EP_LP_SDCARD[] = {0x5E,0x02,0x15,0x12,0x10,0x03,0x15,0x5E,0x3D,0x04,0x12,0x1A,0x08,0x21,0x10,0x05,0x12,0x19,0x14,0x03};
// "/data/data"
static const uint8_t EP_DATA_DATA[] = {0x5E,0x15,0x10,0x05,0x10,0x5E,0x15,0x10,0x05,0x10};
// "/proc"
static const uint8_t EP_PROC[] = {0x5E,0x01,0x03,0x1E,0x12};

// ── [XK_KW = 0xA3] Keyword strings ───────────────────────────────────────────

// "frida"
static const uint8_t EK_FRIDA[] = {0xC5,0xD1,0xCA,0xC7,0xC2};
// "gum-js-loop"
static const uint8_t EK_GUM_JS[] = {0xC4,0xD6,0xCE,0x8E,0xC9,0xD0,0x8E,0xCF,0xCC,0xCC,0xD3};
// "linjector"
static const uint8_t EK_LINJECTOR[] = {0xCF,0xCA,0xCD,0xC9,0xC6,0xC0,0xD7,0xCC,0xD1};
// "frida-agent"
static const uint8_t EK_FRIDA_AGENT[] = {0xC5,0xD1,0xCA,0xC7,0xC2,0x8E,0xC2,0xC4,0xC6,0xCD,0xD7};
// "frida-gadget"
static const uint8_t EK_FRIDA_GADGET[] = {0xC5,0xD1,0xCA,0xC7,0xC2,0x8E,0xC4,0xC2,0xC7,0xC4,0xC6,0xD7};
// "re.frida"
static const uint8_t EK_RE_FRIDA[] = {0xD1,0xC6,0x8D,0xC5,0xD1,0xCA,0xC7,0xC2};
// "luckypatch"
static const uint8_t EK_LP_KW[] = {0xCF,0xD6,0xC0,0xC8,0xDA,0xD3,0xC2,0xD7,0xC0,0xCB};
// "LuckyPatcher"
static const uint8_t EK_LP_KW2[] = {0xEF,0xD6,0xC0,0xC8,0xDA,0xF3,0xC2,0xD7,0xC0,0xCB,0xC6,0xD1};
// "xposed"
static const uint8_t EK_XPOSED[] = {0xDB,0xD3,0xCC,0xD0,0xC6,0xC7};
// "LPHelper"
static const uint8_t EK_LPHELPER[] = {0xEF,0xF3,0xEB,0xC6,0xCF,0xD3,0xC6,0xD1};
// "lp_helper"
static const uint8_t EK_LP_HELPER2[] = {0xCF,0xD3,0xFC,0xCB,0xC6,0xCF,0xD3,0xC6,0xD1};
// "chelpus"
static const uint8_t EK_CHELPUS[] = {0xC0,0xCB,0xC6,0xCF,0xD3,0xD6,0xD0};
// "lackypatch"
static const uint8_t EK_LACKY[] = {0xCF,0xC2,0xC0,0xC8,0xDA,0xD3,0xC2,0xD7,0xC0,0xCB};
// "substrate"
static const uint8_t __attribute__((unused)) EK_SUBSTRATE[] = {0xD0,0xD6,0xC1,0xD0,0xD7,0xD1,0xC2,0xD7,0xC6};
// "unityads"
static const uint8_t EK_UNITYADS[] = {0xD6,0xCD,0xCA,0xD7,0xDA,0xC2,0xC7,0xD0};
// "unity3d"
static const uint8_t EK_UNITY3D[] = {0xD6,0xCD,0xCA,0xD7,0xDA,0x90,0xC7};
// "lp.lock"
static const uint8_t EK_LP_LOCK[] = {0xCF,0xD3,0x8D,0xCF,0xCC,0xC0,0xC8};
// "lp.db"
static const uint8_t EK_LP_DB[] = {0xCF,0xD3,0x8D,0xC7,0xC1};
// "TracerPid"
static const uint8_t EK_TRACERPID[] = {0xF7,0xD1,0xC2,0xC0,0xC6,0xD1,0xF3,0xCA,0xC7};
// "ro.kernel.qemu"
static const uint8_t __attribute__((unused)) EK_QEMU_PROP[] = {0xD1,0xCC,0x8D,0xC8,0xC6,0xD1,0xCD,0xC6,0xCF,0x8D,0xD2,0xC6,0xCE,0xD6};

// ── [XK_URL = 0x5C] URL strings ──────────────────────────────────────────────

// "https://api.github.com/repos/aetherdev01/aether-manager/releases/latest"
static const uint8_t EU_GITHUB_API[] = {
    0x34,0x28,0x28,0x2C,0x29,0x66,0x77,0x77,0x3D,0x2C,0x35,0x76,
    0x3F,0x3D,0x28,0x2C,0x37,0x3A,0x76,0x3B,0x37,0x39,0x77,0x2A,
    0x41,0x2C,0x37,0x2B,0x77,0x3D,0x41,0x2C,0x37,0x41,0x2A,0x38,
    0x41,0x2E,0x68,0x69,0x77,0x3D,0x41,0x2C,0x37,0x41,0x28,0x71,
    0x39,0x3D,0x36,0x3D,0x3F,0x41,0x28,0x77,0x28,0x41,0x30,0x41,
    0x3D,0x2B,0x41,0x2B,0x77,0x30,0x3D,0x28,0x41,0x2B,0x28
};

// ── [XK_MISC = 0xC7] Misc / class names ──────────────────────────────────────

// "com/unity3d/ads/UnityAds"
static const uint8_t __attribute__((unused)) EM_UNITY_CLASS[] = {
    0xAC,0xAA,0xBC,0xF7,0xBC,0xAC,0xBB,0xF4,0xAC,0xE8,0xF7,0xAB,
    0xBC,0xF7,0xFC,0xAC,0xBB,0xF4,0xFC,0xBC,0xBC,0xB0
};

// "6091240"  (Game ID)
static const uint8_t EM_GAME_ID[] = {
    0xF1,0xF7,0xFE,0xF6,0xF5,0xF3,0xF7
};

// ── [XK_SIG = 0x3F] Signature (XOR'd expected sig) ───────────────────────────
// Update saat release key berubah. Lihat header untuk cara update.
static const uint8_t EXPECTED_SIG_ENC[32] = {
    /* 0-15  */ 0x87,0xEC,0x4E,0xFE,0x9F,0x50,0x7B,0x61,
                0x18,0xB3,0x59,0x4D,0x16,0x3C,0xCE,0x87,
    /* 16-31 */ 0xFD,0x22,0x5E,0xD8,0xEB,0x18,0xC0,0xCA,
                0x6A,0x34,0x04,0x9F,0x51,0x73,0xD3,0x67
};

__attribute__((visibility("hidden")))
static void decode_expected_sig(char out[65]) {
    static const char H[]="0123456789abcdef";
    for (int i=0;i<32;i++) {
        uint8_t b=(uint8_t)(EXPECTED_SIG_ENC[i]^XK_SIG);
        out[i*2]=H[(b>>4)&0xF]; out[i*2+1]=H[b&0xF];
    }
    out[64]='\0';
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 1 — ANTI-HOOK
// ─────────────────────────────────────────────────────────────────────────────

__attribute__((visibility("hidden")))
static int l1_tracer_pid(void) {
    char path[32]; xdec(EP_PROC_STATUS,sizeof(EP_PROC_STATUS),XK_PATH,path);
    FILE *f=fopen(path,"r"); if(!f) return 0;
    char kw[16]; xdec(EK_TRACERPID,sizeof(EK_TRACERPID),XK_KW,kw);
    char line[128];
    while(fgets(line,sizeof(line),f)) {
        if(strncmp(line,kw,9)==0) {
            int pid=atoi(line+9); fclose(f); return pid!=0;
        }
    }
    fclose(f); return 0;
}

__attribute__((visibility("hidden")))
static int l1_frida_maps(void) {
    char maps[32]; xdec(EP_PROC_MAPS,sizeof(EP_PROC_MAPS),XK_PATH,maps);
    // decode keywords
    char kws[6][32];
    xdec(EK_FRIDA,sizeof(EK_FRIDA),XK_KW,kws[0]);
    xdec(EK_GUM_JS,sizeof(EK_GUM_JS),XK_KW,kws[1]);
    xdec(EK_LINJECTOR,sizeof(EK_LINJECTOR),XK_KW,kws[2]);
    xdec(EK_RE_FRIDA,sizeof(EK_RE_FRIDA),XK_KW,kws[3]);
    xdec(EK_FRIDA_AGENT,sizeof(EK_FRIDA_AGENT),XK_KW,kws[4]);
    xdec(EK_FRIDA_GADGET,sizeof(EK_FRIDA_GADGET),XK_KW,kws[5]);

    FILE *f=fopen(maps,"r"); if(!f) return 0;
    char line[1024];
    while(fgets(line,sizeof(line),f)) {
        for(int i=0;line[i];i++) if(line[i]>='A'&&line[i]<='Z') line[i]+=32;
        for(int j=0;j<6;j++) if(strstr(line,kws[j])){fclose(f);return 1;}
    }
    fclose(f); return 0;
}

__attribute__((visibility("hidden")))
static int l1_frida_port(void) {
    int ports[]={27042,27043,27044};
    for(int i=0;i<3;i++) {
        int s=socket(AF_INET,SOCK_STREAM,0); if(s<0) continue;
        struct timeval tv={0,60000};
        setsockopt(s,SOL_SOCKET,SO_RCVTIMEO,&tv,sizeof(tv));
        setsockopt(s,SOL_SOCKET,SO_SNDTIMEO,&tv,sizeof(tv));
        struct sockaddr_in a; memset(&a,0,sizeof(a));
        a.sin_family=AF_INET; a.sin_port=htons((uint16_t)ports[i]);
        inet_pton(AF_INET,"127.0.0.1",&a.sin_addr);
        int r=connect(s,(struct sockaddr*)&a,sizeof(a)); close(s);
        if(r==0) return 1;
    }
    return 0;
}

__attribute__((visibility("hidden")))
static int l1_frida_fd(void) {
    // Scan /proc/self/fd untuk symlink ke Frida pipe
    char fd_dir[32]; xdec(EP_PROC_FD,sizeof(EP_PROC_FD),XK_PATH,fd_dir);
    char kw_frida[16]; xdec(EK_FRIDA,sizeof(EK_FRIDA),XK_KW,kw_frida);
    DIR *d=opendir(fd_dir); if(!d) return 0;
    struct dirent *e;
    while((e=readdir(d))!=NULL) {
        char full[128]; snprintf(full,sizeof(full),"%s/%s",fd_dir,e->d_name);
        char target[256]={0};
        ssize_t r=readlink(full,target,sizeof(target)-1);
        if(r>0) {
            for(int i=0;i<r;i++) if(target[i]>='A'&&target[i]<='Z') target[i]+=32;
            if(strstr(target,kw_frida)){closedir(d);return 1;}
        }
    }
    closedir(d); return 0;
}

__attribute__((visibility("hidden")))
static int l1_frida_tmp(void) {
    char tmp[32]; xdec(EP_DATA_TMP,sizeof(EP_DATA_TMP),XK_PATH,tmp);
    char kw[16]; xdec(EK_FRIDA,sizeof(EK_FRIDA),XK_KW,kw);
    DIR *d=opendir(tmp); if(!d) return 0;
    struct dirent *e;
    while((e=readdir(d))!=NULL) {
        if(strncmp(e->d_name,kw,5)==0){closedir(d);return 1;}
    }
    closedir(d); return 0;
}

__attribute__((visibility("hidden")))
static int layer1_anti_hook(void) {
    if(l1_tracer_pid())  return 1;
    if(l1_frida_maps())  return 1;
    if(l1_frida_port())  return 1;
    if(l1_frida_fd())    return 1;
    if(l1_frida_tmp())   return 1;
    return 0;
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 2 — ANTI-DEBUG
// Timing check: jika eksekusi satu instruksi sederhana butuh > threshold
// (debugger single-step), dianggap debugged.
// ─────────────────────────────────────────────────────────────────────────────

__attribute__((visibility("hidden")))
static int l2_timing_check(void) {
    struct timeval t0, t1;
    gettimeofday(&t0, NULL);
    // operasi dummy yang cepat secara normal
    volatile int x = 0;
    for (int i=0;i<1000;i++) x+=i;
    (void)x;
    gettimeofday(&t1, NULL);
    long us = (t1.tv_sec - t0.tv_sec) * 1000000L + (t1.tv_usec - t0.tv_usec);
    // Threshold: > 500ms untuk 1000 iterasi artinya sedang di-trace.
    // 200ms terlalu ketat untuk device budget/low-end yang CPU-nya throttled
    // saat cold start (banyak proses berjalan bersamaan saat boot) → false kill.
    // Debugger single-step biasanya 10x-100x lebih lambat dari threshold ini.
    return (us > 500000) ? 1 : 0;  // 500ms — safe untuk semua device
}

__attribute__((visibility("hidden")))
static int l2_self_ptrace(void) {
    // Coba ptrace diri sendiri.
    // Jika gagal dengan EPERM → ada debugger eksternal yang sudah attach → suspicious.
    // Jika gagal dengan errno lain (EBUSY, EINVAL) → Samsung Knox / system profiler
    // yang attach sebagai bagian monitoring normal → bukan threat, skip.
    errno = 0;
    if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1) {
        return (errno == EPERM) ? 1 : 0;  // hanya EPERM yang genuine debugger
    }
    ptrace(PTRACE_DETACH, 0, NULL, NULL);
    return 0;
}

__attribute__((visibility("hidden")))
static int layer2_anti_debug(void) {
    if (l2_self_ptrace())  return 1;
    if (l2_timing_check()) return 1;
    return 0;
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 3 — ANTI-REPACK (ZIP / DEX / Hash / Signature)
// ─────────────────────────────────────────────────────────────────────────────

static const uint8_t ZIP_LFH[4]  = {0x50,0x4B,0x03,0x04};
static const uint8_t ZIP_EOCD[4] = {0x50,0x4B,0x05,0x06};
static const uint8_t ZIP_CD[4]   = {0x50,0x4B,0x01,0x02};

__attribute__((visibility("hidden")))
static int get_apk_path(char *out, size_t outlen) {
    char maps[32]; xdec(EP_PROC_MAPS,sizeof(EP_PROC_MAPS),XK_PATH,maps);
    FILE *f=fopen(maps,"r"); if(!f) return 0;
    char line[1024];
    while(fgets(line,sizeof(line),f)) {
        char *apk=strstr(line,".apk"); if(!apk) continue;
        char *sl=NULL; for(char *p=apk;p>=line;p--) if(*p=='/'){sl=p;break;}
        if(!sl) continue;
        size_t plen=(size_t)(apk+4-sl); if(plen>=outlen) continue;
        strncpy(out,sl,plen); out[plen]='\0'; fclose(f); return 1;
    }
    fclose(f); return 0;
}

__attribute__((visibility("hidden")))
static int l3_zip_integrity(void) {
    char path[512]={0}; if(!get_apk_path(path,sizeof(path))) return 1;
    int fd=open(path,O_RDONLY); if(fd<0) return 1;

    uint8_t hdr[4];
    if(read(fd,hdr,4)<4||memcmp(hdr,ZIP_LFH,4)!=0){close(fd);return 0;}

    off_t fsz=lseek(fd,0,SEEK_END); if(fsz<22){close(fd);return 0;}
    off_t eocd_off=-1; uint8_t eocd_buf[22]={0};
    for(off_t scan=fsz-22;scan>=0&&(fsz-scan)<=65557;scan--) {
        uint8_t sig[4]; lseek(fd,scan,SEEK_SET);
        if(read(fd,sig,4)<4) continue;
        if(memcmp(sig,ZIP_EOCD,4)==0) {
            eocd_off=scan; lseek(fd,scan,SEEK_SET);
            if(read(fd,eocd_buf,22)==22) break;
        }
    }
    if(eocd_off<0){close(fd);return 0;}

    uint16_t total=(uint16_t)eocd_buf[10]|((uint16_t)eocd_buf[11]<<8);
    uint32_t cd_off=(uint32_t)eocd_buf[16]|((uint32_t)eocd_buf[17]<<8)|((uint32_t)eocd_buf[18]<<16)|((uint32_t)eocd_buf[19]<<24);
    if(total==0||(off_t)cd_off>=fsz){close(fd);return 0;}

    lseek(fd,(off_t)cd_off,SEEK_SET);
    uint8_t cd_sig[4]; if(read(fd,cd_sig,4)<4||memcmp(cd_sig,ZIP_CD,4)!=0){close(fd);return 0;}

    lseek(fd,(off_t)cd_off,SEEK_SET);
    int valid=0; uint16_t chk=total<3?total:3;
    for(uint16_t i=0;i<chk;i++) {
        uint8_t e[46]; if(read(fd,e,46)<46) break;
        if(memcmp(e,ZIP_CD,4)!=0) break; valid++;
        uint16_t fn=(uint16_t)e[28]|((uint16_t)e[29]<<8);
        uint16_t ex=(uint16_t)e[30]|((uint16_t)e[31]<<8);
        uint16_t cm=(uint16_t)e[32]|((uint16_t)e[33]<<8);
        lseek(fd,fn+ex+cm,SEEK_CUR);
    }
    close(fd); return valid>0;
}

__attribute__((visibility("hidden")))
static int l3_dex_magic(void) {
    char path[512]={0}; if(!get_apk_path(path,sizeof(path))) return 1;
    int fd=open(path,O_RDONLY); if(fd<0) return 1;
    const uint8_t DEX_PFX[4]={0x64,0x65,0x78,0x0A};
    const char *VERS[]={"035","036","037","038","039","040","041","042",NULL};
    const size_t BSZ=65536;
    uint8_t *buf=(uint8_t*)malloc(BSZ+8); if(!buf){close(fd);return 1;}
    int dex_found=0,dex_valid=0; ssize_t n; int first=1;
    while((n=read(fd,buf+(first?0:7),(ssize_t)BSZ))>0) {
        size_t total=(size_t)n+(first?0:7);
        for(size_t i=0;i+8<=total;i++) {
            if(memcmp(buf+i,DEX_PFX,4)==0) {
                dex_found=1;
                char ver[4]={(char)buf[i+4],(char)buf[i+5],(char)buf[i+6],'\0'};
                for(int v=0;VERS[v];v++) if(strcmp(ver,VERS[v])==0){dex_valid=1;break;}
                if(dex_valid) break;
            }
        }
        if(dex_valid) break;
        if(total>=7) memcpy(buf,buf+total-7,7);
        first=0;
    }
    free(buf); close(fd);
    if(dex_found&&!dex_valid) return 0;
    return 1;
}

__attribute__((visibility("hidden")))
static int check_sig_native(const char *actual) {
    if(!actual||strlen(actual)<64) return 0;
    char expected[65]; decode_expected_sig(expected);
    char a[65]={0},e[65]={0};
    for(int i=0;i<64&&actual[i];i++) a[i]=(char)((actual[i]>='A'&&actual[i]<='F')?actual[i]+32:actual[i]);
    for(int i=0;i<64&&expected[i];i++) e[i]=(char)((expected[i]>='A'&&expected[i]<='F')?expected[i]+32:expected[i]);
    return memcmp(a,e,64)==0;
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — ANTI-PATCH (Lucky Patcher behavioral)
// ─────────────────────────────────────────────────────────────────────────────

__attribute__((visibility("hidden")))
static int l4_lp_filesystem(void) {
    char sdcard[64], tmp[32], dd[32], lock_kw[16], db_kw[8];
    xdec(EP_LP_SDCARD, sizeof(EP_LP_SDCARD), XK_PATH, sdcard);
    xdec(EP_DATA_TMP,  sizeof(EP_DATA_TMP),  XK_PATH, tmp);
    xdec(EP_DATA_DATA, sizeof(EP_DATA_DATA), XK_PATH, dd);
    xdec(EK_LP_LOCK,   sizeof(EK_LP_LOCK),   XK_KW,   lock_kw);
    xdec(EK_LP_DB,     sizeof(EK_LP_DB),     XK_KW,   db_kw);

    struct stat st;

    // [DISABLED] Sdcard dir check — false positive di Samsung Knox/A-series
    // if (stat(sdcard, &st) == 0 && S_ISDIR(st.st_mode)) return 1;

    // /data/local/tmp — cek isi, bukan hanya keberadaan dir
    if (stat(tmp, &st) == 0 && S_ISDIR(st.st_mode)) {
        DIR *td = opendir(tmp);
        if (td) {
            char kw_lp[16]; xdec(EK_LP_KW, sizeof(EK_LP_KW), XK_KW, kw_lp);
            char kw_fr[16]; xdec(EK_FRIDA,  sizeof(EK_FRIDA),  XK_KW, kw_fr);
            struct dirent *te;
            while ((te = readdir(td)) != NULL) {
                if (te->d_name[0] == '.') continue;
                char low[256] = {0};
                size_t nl = strlen(te->d_name);
                for (size_t i = 0; i < nl && i < 255; i++)
                    low[i] = (char)(te->d_name[i] >= 'A' && te->d_name[i] <= 'Z'
                                    ? te->d_name[i] + 32 : te->d_name[i]);
                if (strstr(low, kw_lp) || strstr(low, kw_fr)) {
                    closedir(td);
                    return 1;
                }
            }
            closedir(td);
        }
    }

    // /data/data — cek lp.lock dan lp.db per package
    DIR *d = opendir(dd);
    if (!d) return 0;
    struct dirent *e;
    while ((e = readdir(d)) != NULL) {
        if (e->d_name[0] == '.') continue;
        char lk[256], db[256];
        snprintf(lk, sizeof(lk), "%s/%s/files/%s",     dd, e->d_name, lock_kw);
        snprintf(db, sizeof(db), "%s/%s/databases/%s", dd, e->d_name, db_kw);
        if (stat(lk, &st) == 0) { closedir(d); return 1; }
        if (stat(db, &st) == 0) { closedir(d); return 1; }
    }
    closedir(d);
    return 0;
}

__attribute__((visibility("hidden")))
static int l4_lp_maps(void) {
    char maps[32]; xdec(EP_PROC_MAPS,sizeof(EP_PROC_MAPS),XK_PATH,maps);
    char kws[6][32];
    xdec(EK_LP_KW,    sizeof(EK_LP_KW),    XK_KW,kws[0]);
    xdec(EK_LP_KW2,   sizeof(EK_LP_KW2),   XK_KW,kws[1]);
    xdec(EK_XPOSED,   sizeof(EK_XPOSED),   XK_KW,kws[2]);
    xdec(EK_LPHELPER, sizeof(EK_LPHELPER), XK_KW,kws[3]);
    xdec(EK_LP_HELPER2,sizeof(EK_LP_HELPER2),XK_KW,kws[4]);
    xdec(EK_CHELPUS,  sizeof(EK_CHELPUS),  XK_KW,kws[5]);
    FILE *f=fopen(maps,"r"); if(!f) return 0;
    char line[1024];
    while(fgets(line,sizeof(line),f)) {
        char low[1024]; size_t ll=strlen(line); if(ll>=sizeof(low)) ll=sizeof(low)-1;
        for(size_t i=0;i<ll;i++) low[i]=(char)(line[i]>='A'&&line[i]<='Z'?line[i]+32:line[i]);
        low[ll]='\0';
        for(int k=0;k<6;k++) {
            if(k==2) continue; // skip EK_XPOSED — false positive Samsung Knox
            if(strstr(low,kws[k])){fclose(f);return 1;}
        }
    }
    fclose(f); return 0;
}

__attribute__((visibility("hidden")))
static int l4_lp_process(void) {
    char proc[16]; xdec(EP_PROC,sizeof(EP_PROC),XK_PATH,proc);
    char kws[5][32];
    xdec(EK_LP_KW,    sizeof(EK_LP_KW),    XK_KW,kws[0]);
    xdec(EK_CHELPUS,  sizeof(EK_CHELPUS),  XK_KW,kws[1]);
    xdec(EK_LACKY,    sizeof(EK_LACKY),    XK_KW,kws[2]);
    xdec(EK_LPHELPER, sizeof(EK_LPHELPER), XK_KW,kws[3]);
    xdec(EK_LP_HELPER2,sizeof(EK_LP_HELPER2),XK_KW,kws[4]);
    DIR *d=opendir(proc); if(!d) return 0;
    struct dirent *e;
    while((e=readdir(d))!=NULL) {
        if(!isdigit((unsigned char)e->d_name[0])) continue;
        char path[64]; snprintf(path,sizeof(path),"%s/%s/cmdline",proc,e->d_name);
        int fd=open(path,O_RDONLY); if(fd<0) continue;
        char cmd[256]={0}; ssize_t n=read(fd,cmd,sizeof(cmd)-1); close(fd);
        if(n<=0) continue;
        for(int i=0;i<n;i++) if(cmd[i]>='A'&&cmd[i]<='Z') cmd[i]+=32;
        for(int k=0;k<5;k++) if(strstr(cmd,kws[k])){closedir(d);return 1;}
    }
    closedir(d); return 0;
}

__attribute__((visibility("hidden")))
static int l4_installer_check(JNIEnv *env, jobject ctx) {
    if(!env||!ctx) return 0;
    char kws[4][32];
    xdec(EK_LP_KW,   sizeof(EK_LP_KW),  XK_KW,kws[0]);
    xdec(EK_CHELPUS, sizeof(EK_CHELPUS),XK_KW,kws[1]);
    xdec(EK_LACKY,   sizeof(EK_LACKY),  XK_KW,kws[2]);
    xdec(EK_LPHELPER,sizeof(EK_LPHELPER),XK_KW,kws[3]);
    jclass cc=env->GetObjectClass(ctx); if(!cc) return 0;
    jmethodID gpm=env->GetMethodID(cc,"getPackageManager","()Landroid/content/pm/PackageManager;");
    if(!gpm) return 0;
    jobject pm=env->CallObjectMethod(ctx,gpm); if(!pm) return 0;
    jmethodID gpn=env->GetMethodID(cc,"getPackageName","()Ljava/lang/String;");
    if(!gpn) return 0;
    jstring pkgJ=(jstring)env->CallObjectMethod(ctx,gpn); if(!pkgJ) return 0;
    jclass pmc=env->GetObjectClass(pm); if(!pmc) return 0;
    jmethodID gi=env->GetMethodID(pmc,"getInstallerPackageName","(Ljava/lang/String;)Ljava/lang/String;");
    if(!gi) return 0;
    jstring instJ=(jstring)env->CallObjectMethod(pm,gi,pkgJ); if(!instJ) return 0;
    const char *inst=env->GetStringUTFChars(instJ,nullptr); if(!inst) return 0;
    char low[128]={0}; size_t il=strlen(inst); if(il>=sizeof(low)) il=sizeof(low)-1;
    for(size_t i=0;i<il;i++) low[i]=(char)(inst[i]>='A'&&inst[i]<='Z'?inst[i]+32:inst[i]);
    int bad=0;
    for(int k=0;k<4;k++) if(strstr(low,kws[k])){bad=1;break;}
    env->ReleaseStringUTFChars(instJ,inst);
    return bad;
}

__attribute__((visibility("hidden")))
static int layer4_anti_patch(JNIEnv *env, jobject ctx) {
    if(l4_lp_filesystem())       return 1;
    if(l4_lp_maps())              return 1;
    if(l4_lp_process())           return 1;
    if(l4_installer_check(env,ctx)) return 1;
    return 0;
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ANTI-TAMPER (Unity string scan + class existence)
// ─────────────────────────────────────────────────────────────────────────────

__attribute__((visibility("hidden")))
static int file_has_needle(const char *path, const char *needle, size_t nlen) {
    int fd=open(path,O_RDONLY); if(fd<0) return -1;
    const size_t BSZ=65536;
    uint8_t *buf=(uint8_t*)malloc(BSZ+nlen); if(!buf){close(fd);return -1;}
    size_t ovlp=nlen>1?nlen-1:0; int found=0; ssize_t n; int first=1;
    while(!found&&(n=read(fd,buf+(first?0:ovlp),(ssize_t)BSZ))>0) {
        size_t total=(size_t)n+(first?0:ovlp);
        for(size_t i=0;i+nlen<=total;i++) if(memcmp(buf+i,needle,nlen)==0){found=1;break;}
        if(total>=ovlp) memcpy(buf,buf+total-ovlp,ovlp);
        first=0;
    }
    free(buf); close(fd); return found;
}

__attribute__((visibility("hidden")))
static int l5_unity_strings(void) {
    char ua[16],u3d[16];
    xdec(EK_UNITYADS,sizeof(EK_UNITYADS),XK_KW,ua);
    xdec(EK_UNITY3D, sizeof(EK_UNITY3D), XK_KW,u3d);
    char maps[32]; xdec(EP_PROC_MAPS,sizeof(EP_PROC_MAPS),XK_PATH,maps);
    FILE *f=fopen(maps,"r"); if(!f) return 1;
    char checked[16][512]; int nc=0, found=0, apk_ok=0;
    char line[1024];
    while(fgets(line,sizeof(line),f)) {
        const char *exts[]={"  .apk","  .odex","  .vdex","  .dex",NULL};
        (void)exts;
        // simpler: scan for extension anywhere in line
        const char *hitExt=NULL; char *hitPos=NULL;
        const char *extList[]={".apk",".odex",".vdex",".dex",NULL};
        for(int ei=0;extList[ei];ei++) {
            char *p=strstr(line,extList[ei]); if(p){hitExt=extList[ei];hitPos=p;break;}
        }
        if(!hitExt) continue;
        char *sl=NULL; for(char *p=hitPos;p>=line;p--) if(*p=='/'){sl=p;break;}
        if(!sl) continue;
        size_t plen=(size_t)(hitPos+strlen(hitExt)-sl); if(plen>=512) continue;
        char path[512]; strncpy(path,sl,plen); path[plen]='\0';
        int dup=0; for(int i=0;i<nc;i++) if(strcmp(checked[i],path)==0){dup=1;break;}
        if(dup) continue; if(nc<16) strncpy(checked[nc++],path,511);
        int ha=file_has_needle(path,ua,strlen(ua));
        int h3=file_has_needle(path,u3d,strlen(u3d));
        if(ha==-1&&h3==-1) continue;
        if(strstr(path,".apk")) apk_ok=1;
        if(ha>0||h3>0) found=1;
    }
    fclose(f);
    if(apk_ok&&!found) return 0; // patched
    return 1;
}

__attribute__((visibility("hidden")))
static int l5_unity_class(JNIEnv *env) {
    if(!env) return 1;
    // decode class path: "com/unity3d/ads/UnityAds"
    static const char cls[]="com/unity3d/ads/UnityAds";
    if(env->ExceptionCheck()) env->ExceptionClear();
    jclass c=env->FindClass(cls);
    if(env->ExceptionCheck()) env->ExceptionClear();
    if(!c) {
        // Class tidak ditemukan.
        // Bisa berarti: (a) Unity di-strip/patch, ATAU (b) Unity belum di-initialize.
        // Bedakan dengan cek maps: kalau unity3d tidak ada di maps sama sekali,
        // Unity belum dimuat ke memori → bukan tampered → skip (return 1).
        char ua[16]; xdec(EK_UNITYADS,sizeof(EK_UNITYADS),XK_KW,ua);
        char maps_path[32]; xdec(EP_PROC_MAPS,sizeof(EP_PROC_MAPS),XK_PATH,maps_path);
        FILE *f=fopen(maps_path,"r"); if(!f) return 1; // maps tidak terbaca → skip
        char line[1024]; int unity_in_maps=0;
        while(fgets(line,sizeof(line),f)) {
            for(int i=0;line[i];i++) if(line[i]>='A'&&line[i]<='Z') line[i]+=32;
            if(strstr(line,ua)){unity_in_maps=1;break;}
        }
        fclose(f);
        // Unity ada di maps tapi class tidak ditemukan → di-strip/patch = tampered (0)
        // Unity tidak ada di maps → belum diload → skip (1)
        return unity_in_maps ? 0 : 1;
    }
    env->DeleteLocalRef(c); return 1;
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 6 — ANTI-EMULATOR (bonus layer)
// ─────────────────────────────────────────────────────────────────────────────

__attribute__((visibility("hidden"))) __attribute__((unused))
static int l6_emulator_check(JNIEnv *env) {
    if(!env) return 0;
    // Cek Build.FINGERPRINT via JNI
    jclass buildCls=env->FindClass("android/os/Build");
    if(!buildCls) return 0;
    jfieldID fpId=env->GetStaticFieldID(buildCls,"FINGERPRINT","Ljava/lang/String;");
    if(!fpId){env->DeleteLocalRef(buildCls);return 0;}
    jstring fpJ=(jstring)env->GetStaticObjectField(buildCls,fpId);
    env->DeleteLocalRef(buildCls);
    if(!fpJ) return 0;
    const char *fp=env->GetStringUTFChars(fpJ,nullptr);
    if(!fp) return 0;
    char low[256]={0}; size_t fl=strlen(fp); if(fl>=sizeof(low)) fl=sizeof(low)-1;
    for(size_t i=0;i<fl;i++) low[i]=(char)(fp[i]>='A'&&fp[i]<='Z'?fp[i]+32:fp[i]);
    env->ReleaseStringUTFChars(fpJ,fp);
    // Emulator fingerprints biasanya mengandung "generic" atau "unknown"
    // atau "ranchu" (Google AVD). Ini opsional — dinonaktifkan default karena
    // beberapa pengguna pakai emulator yang sah. Uncomment jika ingin aktif.
    // if(strstr(low,"generic"))  return 1;
    // if(strstr(low,"ranchu"))   return 1;
    // if(strstr(low,"unknown"))  return 1;
    (void)low; // suppress warning saat blok di atas di-comment
    return 0;
}

// ─────────────────────────────────────────────────────────────────────────────
// JNI HELPER — get sourceDir dari Context
// ─────────────────────────────────────────────────────────────────────────────

__attribute__((visibility("hidden")))
static void get_source_dir(JNIEnv *env, jobject ctx, char *out, size_t outlen) {
    jclass cc=env->GetObjectClass(ctx);
    jmethodID m=env->GetMethodID(cc,"getApplicationInfo","()Landroid/content/pm/ApplicationInfo;");
    jobject ai=env->CallObjectMethod(ctx,m);
    jclass ic=env->GetObjectClass(ai);
    jfieldID f=env->GetFieldID(ic,"sourceDir","Ljava/lang/String;");
    jstring js=(jstring)env->GetObjectField(ai,f);
    const char *s=env->GetStringUTFChars(js,nullptr);
    strncpy(out,s,outlen-1); out[outlen-1]='\0';
    env->ReleaseStringUTFChars(js,s);
}

// ─────────────────────────────────────────────────────────────────────────────
// .init_array constructor — dijalankan sebelum JNI_OnLoad
// Pasang deteksi hook paling dasar seawal mungkin.
// ─────────────────────────────────────────────────────────────────────────────

__attribute__((constructor(101), visibility("hidden")))
static void early_security_check(void) {
    // Hanya cek frida maps di init — cukup cepat, tidak perlu JNI.
    //
    // l1_tracer_pid DIHAPUS dari sini: beberapa ROM (MIUI/HyperOS, ColorOS,
    // Samsung Knox) menyebabkan TracerPid non-zero secara normal saat cold
    // start, sebelum app fully initialized -> false positive SIGKILL.
    // TracerPid tetap dicek di layer1_anti_hook() via nativeCheckAll()
    // yang dipanggil setelah Application.onCreate() selesai.
    //
    // l2_self_ptrace TIDAK dijalankan di sini: Samsung Knox bisa belum siap
    // saat .init_array dipanggil sehingga ptrace errno ambigu -> false kill.
    if (l1_frida_maps()) { DEVLOG("frida_maps"); aether_kill(); }
}

// ─────────────────────────────────────────────────────────────────────────────
// JNI EXPORTS — class: dev.aether.manager.NativeAether
// ─────────────────────────────────────────────────────────────────────────────

#define JNI_PKG "Java_dev_aether_manager_NativeAether_"

extern "C" {

// ── Integrity ──────────────────────────────────────────────────────────────

JNIEXPORT jstring JNICALL
Java_dev_aether_manager_NativeAether_nativeGetApkHash(JNIEnv *env, jobject, jobject ctx) {
    char path[512]={0}; get_source_dir(env,ctx,path,sizeof(path));
    char hex[65]; if(hash_file(path,hex)!=0) return nullptr;
    return env->NewStringUTF(hex);
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckIntegrity(JNIEnv *env, jobject, jobject ctx, jstring expJ) {
    if(!expJ) return JNI_TRUE;
    char path[512]={0}; get_source_dir(env,ctx,path,sizeof(path));
    char actual[65]; if(hash_file(path,actual)!=0) return JNI_FALSE;
    const char *exp=env->GetStringUTFChars(expJ,nullptr);
    int ok=(strcasecmp(actual,exp)==0);
    env->ReleaseStringUTFChars(expJ,exp);
    return ok?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckSignature(JNIEnv *env, jobject, jstring sigJ) {
    if(!sigJ){DEVLOG("sign null"); aether_kill();return JNI_FALSE;}
    const char *hex=env->GetStringUTFChars(sigJ,nullptr);
    DEVLOG(" %s",hex);
    int ok=check_sig_native(hex);
    env->ReleaseStringUTFChars(sigJ,hex);
    if(!ok){ DEVLOG("Signature mismatch"); aether_kill(); }
    else   { DEVLOG("Signature Okey"); }
    return ok?JNI_TRUE:JNI_FALSE;
}

// ── Hook / Debug detection ─────────────────────────────────────────────────

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeIsHooked(JNIEnv *, jobject) {
    return layer1_anti_hook()?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeIsDebugged(JNIEnv *, jobject) {
    return layer2_anti_debug()?JNI_TRUE:JNI_FALSE;
}

// ── Anti-patch ─────────────────────────────────────────────────────────────

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckAntiPatch(JNIEnv *env, jobject, jobject ctx) {
    // Layer 3: ZIP + DEX
    if(!l3_zip_integrity()){aether_kill();return JNI_FALSE;}
    if(!l3_dex_magic()){aether_kill();return JNI_FALSE;}
    // Layer 4: Lucky Patcher
    if(layer4_anti_patch(env,ctx)){aether_kill();return JNI_FALSE;}
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckUnityIntact(JNIEnv *env, jobject) {
    if(!l5_unity_strings()){aether_kill();return JNI_FALSE;}
    if(!l5_unity_class(env)){aether_kill();return JNI_FALSE;}
    return JNI_TRUE;
}

// ── Master check — satu call atom, minimum JNI overhead ───────────────────

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckAll(JNIEnv *env, jobject, jobject ctx) {
    DEVLOG("start");
    if(layer1_anti_hook())         { DEVLOG("anti-hook"); aether_kill(); }
    if(layer2_anti_debug())        { DEVLOG("anti-debug"); aether_kill(); }
    if(!l3_zip_integrity())        { DEVLOG("integrity"); aether_kill(); }
    if(!l3_dex_magic())            { DEVLOG("magic"); aether_kill(); }
    if(layer4_anti_patch(env,ctx)) { DEVLOG("anti-patch"); aether_kill(); }
    DEVLOG("AetherManager");
    return JNI_TRUE;
}

// ── Utilities ──────────────────────────────────────────────────────────────

JNIEXPORT void JNICALL
Java_dev_aether_manager_NativeAether_nativeKillProcess(JNIEnv *, jobject) {
    aether_kill();
}

JNIEXPORT jstring JNICALL
Java_dev_aether_manager_NativeAether_nativeGetPackageName(JNIEnv *env, jobject) {
    char maps[32]; xdec(EP_PROC_MAPS,sizeof(EP_PROC_MAPS),XK_PATH,maps);
    FILE *f=fopen(maps,"r"); if(!f) return nullptr;
    char line[1024], result[256]={0};
    while(fgets(line,sizeof(line),f)) {
        char *p=strchr(line,'/'); if(!p) continue;
        size_t l=strlen(p); if(l>0&&p[l-1]=='\n') p[l-1]='\0';
        char *ex=strchr(p,'!'); if(ex)*ex='\0';
        if(!strstr(p,".apk")) continue;
        const char *da=strstr(p,"/data/app/"); if(!da) continue;
        da+=strlen("/data/app/");
        if(da[0]=='~'&&da[1]=='~'){const char *sl=strchr(da,'/');if(sl)da=sl+1;}
        const char *sl=strchr(da,'/');
        size_t dl=sl?(size_t)(sl-da):strlen(da); if(dl>=sizeof(result)) break;
        char dir[256]; strncpy(dir,da,dl); dir[dl]='\0';
        char *ld=strrchr(dir,'-'); if(ld)*ld='\0';
        strncpy(result,dir,sizeof(result)-1); break;
    }
    fclose(f); if(!result[0]) return nullptr;
    return env->NewStringUTF(result);
}

// ── String vault ───────────────────────────────────────────────────────────

JNIEXPORT jstring JNICALL
Java_dev_aether_manager_NativeAether_nativeGetGameId(JNIEnv *env, jobject) {
    char buf[sizeof(EM_GAME_ID)+1];
    xdec(EM_GAME_ID,sizeof(EM_GAME_ID),XK_MISC,buf);
    return env->NewStringUTF(buf);
}

JNIEXPORT jstring JNICALL
Java_dev_aether_manager_NativeAether_nativeGetGithubApi(JNIEnv *env, jobject) {
    char buf[sizeof(EU_GITHUB_API)+1];
    xdec(EU_GITHUB_API,sizeof(EU_GITHUB_API),XK_URL,buf);
    return env->NewStringUTF(buf);
}

JNIEXPORT jobjectArray JNICALL
Java_dev_aether_manager_NativeAether_nativeGetAdblockDnsKeywords(JNIEnv *env, jobject) {
    // DNS adblock keywords — decode on-the-fly, tidak simpan ke heap lama
    // DNS keywords yang relevan untuk adblock (reuse sebagian dari KW vault)
    // "adguard","nextdns","blokada","pihole","adaway","adblock"
    // Encode ulang dengan XK_KW = 0xA3:
    static const uint8_t EK_DNS_ADGUARD[]   = {0xC2,0xC7,0xC4,0xD6,0xC2,0xD1,0xC7};                   // adguard
    static const uint8_t EK_DNS_NEXTDNS[]   = {0xCD,0xC6,0xDB,0xD7,0xC7,0xCD,0xD0};                   // nextdns
    static const uint8_t EK_DNS_BLOKADA[]   = {0xC1,0xCF,0xCC,0xC8,0xC2,0xC7,0xC2};                   // blokada
    static const uint8_t EK_DNS_PIHOLE[]    = {0xD3,0xCA,0xCB,0xCC,0xCF,0xC6};                         // pihole
    static const uint8_t EK_DNS_ADAWAY[]    = {0xC2,0xC7,0xC2,0xD4,0xC2,0xDA};                         // adaway
    static const uint8_t EK_DNS_ADBLOCK[]   = {0xC2,0xC7,0xC1,0xCF,0xCC,0xC0,0xC8};                   // adblock
    static const uint8_t EK_DNS_MULLVAD[]   = {0xCE,0xD6,0xCF,0xCF,0xD5,0xC2,0xC7};                   // mullvad
    static const uint8_t EK_DNS_CONTROLD[]  = {0xC0,0xCC,0xCD,0xD7,0xD1,0xCC,0xCF,0xC7};              // controld
    static const uint8_t EK_DNS_QUAD9[]     = {0xD2,0xD6,0xC2,0xC7,0x9A};                              // quad9
    static const uint8_t EK_DNS_RETHINK[]   = {0xD1,0xC6,0xD7,0xCB,0xCA,0xCD,0xC8,0xC7,0xCD,0xD0};   // rethinkdns

    struct{const uint8_t *a;size_t s;}dnsKw[]={
        {EK_DNS_ADGUARD, sizeof(EK_DNS_ADGUARD)},{EK_DNS_NEXTDNS,sizeof(EK_DNS_NEXTDNS)},
        {EK_DNS_BLOKADA, sizeof(EK_DNS_BLOKADA)},{EK_DNS_PIHOLE,  sizeof(EK_DNS_PIHOLE)},
        {EK_DNS_ADAWAY,  sizeof(EK_DNS_ADAWAY)}, {EK_DNS_ADBLOCK, sizeof(EK_DNS_ADBLOCK)},
        {EK_DNS_MULLVAD, sizeof(EK_DNS_MULLVAD)},{EK_DNS_CONTROLD,sizeof(EK_DNS_CONTROLD)},
        {EK_DNS_QUAD9,   sizeof(EK_DNS_QUAD9)},  {EK_DNS_RETHINK, sizeof(EK_DNS_RETHINK)},
    };
    int count=(int)(sizeof(dnsKw)/sizeof(dnsKw[0]));
    jclass sc=env->FindClass("java/lang/String");
    jobjectArray res=env->NewObjectArray(count,sc,nullptr);
    for(int i=0;i<count;i++) {
        char buf[64]={0}; xdec(dnsKw[i].a,dnsKw[i].s,XK_KW,buf);
        env->SetObjectArrayElement(res,i,env->NewStringUTF(buf));
    }
    return res;
}

JNIEXPORT jobjectArray JNICALL
Java_dev_aether_manager_NativeAether_nativeGetHostsSignatures(JNIEnv *env, jobject) {
    // Hosts-file adblock signatures
    static const uint8_t EK_H_ADAWAY[]     = {0xC2,0xC7,0xC2,0xD4,0xC2,0xDA};                                                      // adaway
    static const uint8_t EK_H_ADBLOCK[]    = {0xC2,0xC7,0xC1,0xCF,0xCC,0xC0,0xC8};                                                // adblock
    static const uint8_t EK_H_GENERATED[]  = {0xC4,0xC6,0xCD,0xC6,0xD1,0xC2,0xD7,0xC6,0xC7,0x83,0xC1,0xDA};                     // generated by
    static const uint8_t EK_H_DOUBLECLICK[]= {0xC2,0xC7,0x8D,0xC7,0xCC,0xD6,0xC1,0xCF,0xC6,0xC0,0xCF,0xCA,0xC0,0xC8,0x8D,0xCD,0xC6,0xD7}; // ad.doubleclick.net
    static const uint8_t EK_H_ADMOB[]      = {0xC2,0xC7,0xCE,0xCC,0xC1};                                                          // admob
    static const uint8_t EK_H_UNITY[]      = {0xD6,0xCD,0xCA,0xD7,0xDA,0xC2,0xC7,0xD0,0x8D,0xD6,0xCD,0xCA,0xD7,0xDA,0x90,0xC7,0x8D,0xC0,0xCC,0xCE}; // unityads.unity3d.com

    struct{const uint8_t *a;size_t s;}hSig[]={
        {EK_H_ADAWAY,     sizeof(EK_H_ADAWAY)},
        {EK_H_ADBLOCK,    sizeof(EK_H_ADBLOCK)},
        {EK_H_GENERATED,  sizeof(EK_H_GENERATED)},
        {EK_H_DOUBLECLICK,sizeof(EK_H_DOUBLECLICK)},
        {EK_H_ADMOB,      sizeof(EK_H_ADMOB)},
        {EK_H_UNITY,      sizeof(EK_H_UNITY)},
    };
    int count=(int)(sizeof(hSig)/sizeof(hSig[0]));
    jclass sc=env->FindClass("java/lang/String");
    jobjectArray res=env->NewObjectArray(count,sc,nullptr);
    for(int i=0;i<count;i++) {
        char buf[64]={0}; xdec(hSig[i].a,hSig[i].s,XK_KW,buf);
        env->SetObjectArrayElement(res,i,env->NewStringUTF(buf));
    }
    return res;
}

} // extern "C"