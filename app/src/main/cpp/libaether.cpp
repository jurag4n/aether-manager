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
#include <sys/system_properties.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdint.h>
#include <ctype.h>
#include <dlfcn.h>
#include <pthread.h>
#include <linux/elf.h>

#ifndef DEV_LOG
#  define DEV_LOG 0
#endif

#if DEV_LOG
#  define DEVLOG(fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, "AETHER_SEC", fmt, ##__VA_ARGS__)
#else
#  define DEVLOG(fmt, ...) ((void)0)
#endif

#define XK_PATH   0x71
#define XK_KW     0xA3
#define XK_URL    0x5C
#define XK_MISC   0xC7

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
    for (size_t i=0;i<len;i++){dst[i*2]=H[(src[i]>>4)&0xF];dst[i*2+1]=H[src[i]&0xF];}
    dst[len*2]='\0';
}

__attribute__((visibility("hidden")))
static void xdec(const unsigned char *src, size_t len, uint8_t key, char *dst) {
    for (size_t i=0;i<len;i++) dst[i]=(char)(src[i]^key);
    dst[len]='\0';
}

// ─── OBFUSCATED PATH / KEYWORD CONSTANTS ─────────────────────────────────────

static const uint8_t EP_PROC_STATUS[] = {0x5E,0x01,0x03,0x1E,0x12,0x5E,0x02,0x14,0x1D,0x17,0x5E,0x02,0x05,0x10,0x05,0x04,0x02};
static const uint8_t EP_PROC_MAPS[]   = {0x5E,0x01,0x03,0x1E,0x12,0x5E,0x02,0x14,0x1D,0x17,0x5E,0x1C,0x10,0x01,0x02};
static const uint8_t EP_PROC_FD[]     = {0x5E,0x01,0x03,0x1E,0x12,0x5E,0x02,0x14,0x1D,0x17,0x5E,0x17,0x15};
static const uint8_t EP_DATA_TMP[]    = {0x5E,0x15,0x10,0x05,0x10,0x5E,0x1D,0x1E,0x12,0x10,0x1D,0x5E,0x05,0x1C,0x01};
static const uint8_t EP_LP_SDCARD[]   = {0x5E,0x02,0x15,0x12,0x10,0x03,0x15,0x5E,0x3D,0x04,0x12,0x1A,0x08,0x21,0x10,0x05,0x12,0x19,0x14,0x03};
static const uint8_t EP_DATA_DATA[]   = {0x5E,0x15,0x10,0x05,0x10,0x5E,0x15,0x10,0x05,0x10};
static const uint8_t EP_PROC[]        = {0x5E,0x01,0x03,0x1E,0x12};

static const uint8_t EK_FRIDA[]          = {0xC5,0xD1,0xCA,0xC7,0xC2};
static const uint8_t EK_GUM_JS[]         = {0xC4,0xD6,0xCE,0x8E,0xC9,0xD0,0x8E,0xCF,0xCC,0xCC,0xD3};
static const uint8_t EK_LINJECTOR[]      = {0xCF,0xCA,0xCD,0xC9,0xC6,0xC0,0xD7,0xCC,0xD1};
static const uint8_t EK_FRIDA_AGENT[]    = {0xC5,0xD1,0xCA,0xC7,0xC2,0x8E,0xC2,0xC4,0xC6,0xCD,0xD7};
static const uint8_t EK_FRIDA_GADGET[]   = {0xC5,0xD1,0xCA,0xC7,0xC2,0x8E,0xC4,0xC2,0xC7,0xC4,0xC6,0xD7};
static const uint8_t EK_RE_FRIDA[]       = {0xD1,0xC6,0x8D,0xC5,0xD1,0xCA,0xC7,0xC2};
static const uint8_t EK_LP_KW[]          = {0xCF,0xD6,0xC0,0xC8,0xDA,0xD3,0xC2,0xD7,0xC0,0xCB};
static const uint8_t EK_LP_KW2[]         = {0xEF,0xD6,0xC0,0xC8,0xDA,0xF3,0xC2,0xD7,0xC0,0xCB,0xC6,0xD1};
static const uint8_t EK_XPOSED[]         = {0xDB,0xD3,0xCC,0xD0,0xC6,0xC7};
static const uint8_t EK_LPHELPER[]       = {0xEF,0xF3,0xEB,0xC6,0xCF,0xD3,0xC6,0xD1};
static const uint8_t EK_LP_HELPER2[]     = {0xCF,0xD3,0xFC,0xCB,0xC6,0xCF,0xD3,0xC6,0xD1};
static const uint8_t EK_CHELPUS[]        = {0xC0,0xCB,0xC6,0xCF,0xD3,0xD6,0xD0};
static const uint8_t EK_LACKY[]          = {0xCF,0xC2,0xC0,0xC8,0xDA,0xD3,0xC2,0xD7,0xC0,0xCB};
static const uint8_t EK_UNITYADS[]       = {0xD6,0xCD,0xCA,0xD7,0xDA,0xC2,0xC7,0xD0};
static const uint8_t EK_UNITY3D[]        = {0xD6,0xCD,0xCA,0xD7,0xDA,0x90,0xC7};
static const uint8_t EK_LP_LOCK[]        = {0xCF,0xD3,0x8D,0xCF,0xCC,0xC0,0xC8};
static const uint8_t EK_LP_DB[]          = {0xCF,0xD3,0x8D,0xC7,0xC1};
static const uint8_t EK_TRACERPID[]      = {0xF7,0xD1,0xC2,0xC0,0xC6,0xD1,0xF3,0xCA,0xC7};
static const uint8_t EK_LP_LITEPATCHER[] = {0xCC,0xD1,0xC4,0x8D,0xCF,0xCA,0xD7,0xC6,0xD3,0xC2,0xD7,0xC0,0xCB,0xC6,0xD1,0xD0,0x8D,0xCF,0xD3};
static const uint8_t EK_LP_PKG2[]        = {0xCF,0xD6,0xC0,0xC8,0xDA,0x8D,0xD3,0xC2,0xD7,0xC0,0xCB,0xC6,0xD1};
static const uint8_t EK_LP_PKG3[]        = {0xD3,0xC2,0xD7,0xC0,0xCB,0x8D,0xC2,0xD3,0xD3,0xCF,0xCA,0xC0,0xC2,0xD7,0xCA,0xCC,0xCD};

// ─── OBFUSCATED URL CONSTANTS (XK_URL = 0x5C) ────────────────────────────────
// https://aether-app-weld.vercel.app/api
static const uint8_t EU_VERCEL_API[]  = {
    0x34,0x28,0x28,0x2C,0x2F,0x66,0x73,0x73,0x3D,0x39,0x28,0x34,0x39,0x2E,0x71,
    0x3D,0x2C,0x2C,0x71,0x2B,0x39,0x30,0x38,0x72,0x2A,0x39,0x2E,0x3F,0x39,0x30,
    0x72,0x3D,0x2C,0x2C,0x73,0x3D,0x2C,0x35
};
// https://github.com/aetherdev01/aether-manager
static const uint8_t EU_GITHUB_REPO[] = {
    0x34,0x28,0x28,0x2C,0x2F,0x66,0x73,0x73,0x3B,0x35,0x28,0x34,0x29,0x3E,0x72,
    0x3F,0x33,0x31,0x73,0x3D,0x39,0x28,0x34,0x39,0x2E,0x38,0x39,0x2A,0x6C,0x6D,
    0x73,0x3D,0x39,0x28,0x34,0x39,0x2E,0x71,0x31,0x3D,0x32,0x3D,0x3B,0x39,0x2E
};
// https://t.me/AetherDev22
static const uint8_t EU_TELEGRAM[]    = {
    0x34,0x28,0x28,0x2C,0x2F,0x66,0x73,0x73,0x28,0x72,0x31,0x39,0x73,0x1D,0x39,
    0x28,0x34,0x39,0x2E,0x18,0x39,0x2A,0x6E,0x6E
};
// Original GitHub API URL (existing)
static const uint8_t EU_GITHUB_API[]  = {
    0x34,0x28,0x28,0x2C,0x2F,0x66,0x73,0x73,0x3D,0x35,0x28,0x34,0x29,0x3E,0x72,
    0x3F,0x33,0x31,0x73,0x3D,0x39,0x28,0x34,0x39,0x2E,0x38,0x39,0x2A,0x6C,0x6D,
    0x73,0x3D,0x39,0x28,0x34,0x39,0x2E,0x71,0x31,0x3D,0x32,0x3D,0x3B,0x39,0x2E,
    0x73,0x39,0x39,0x2C,0x73,0x39,0x39,0x2E,0x39,0x38,0x39,0x3D,0x73,0x2E,0x39,
    0x30,0x39,0x3D,0x2F,0x39,0x2F,0x73,0x30,0x3D,0x28,0x39,0x2F,0x28
};

static const uint8_t EM_GAME_ID[] = {0xF1,0xF7,0xFE,0xF6,0xF5,0xF3,0xF7};

// ─── LAYER 1 — ANTI-HOOK (Frida detection) ───────────────────────────────────

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

// ─── LAYER 2 — ANTI-DEBUG ────────────────────────────────────────────────────

__attribute__((visibility("hidden")))
static int l2_timing_check(void) {
    struct timeval t0, t1;
    gettimeofday(&t0, NULL);
    volatile int x = 0;
    for (int i=0;i<1000;i++) x+=i;
    (void)x;
    gettimeofday(&t1, NULL);
    long us = (t1.tv_sec - t0.tv_sec) * 1000000L + (t1.tv_usec - t0.tv_usec);
    return (us > 500000) ? 1 : 0;
}

__attribute__((visibility("hidden")))
static int l2_self_ptrace(void) {
    errno = 0;
    if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1) {
        return (errno == EPERM) ? 1 : 0;
    }
    ptrace(PTRACE_DETACH, 0, NULL, NULL);
    return 0;
}

__attribute__((visibility("hidden")))
static int layer2_anti_debug(void) {
    // l2_self_ptrace() intentionally removed:
    // PTRACE_TRACEME returns EPERM on Android 10+ due to SELinux/seccomp policy,
    // even without any debugger — causes false-positive kill on all modern rooted
    // devices. Debugger is already covered by l1_tracer_pid() in Layer 1
    // (reads TracerPid from /proc/self/status which is reliable).
    if (l2_timing_check()) return 1;
    return 0;
}

// ─── LAYER 3 — ZIP/DEX INTEGRITY ─────────────────────────────────────────────

static const uint8_t ZIP_LFH[4]  = {0x50,0x4B,0x03,0x04};
static const uint8_t ZIP_EOCD[4] = {0x50,0x4B,0x05,0x06};
static const uint8_t ZIP_CD[4]   = {0x50,0x4B,0x01,0x02};

__attribute__((visibility("hidden")))
static void get_source_dir(JNIEnv *env, jobject ctx, char *out, size_t outlen);

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
    const char *VERS[]={"035","036","037","038","039","040","041","042","043","044",NULL};
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

// ─── LAYER 4 — ANTI-PATCH (LuckyPatcher, etc.) ───────────────────────────────

__attribute__((visibility("hidden")))
static int l4_lp_filesystem(void) {
    char sdcard[64], tmp[32], dd[32], lock_kw[16], db_kw[8];
    xdec(EP_LP_SDCARD, sizeof(EP_LP_SDCARD), XK_PATH, sdcard);
    xdec(EP_DATA_TMP,  sizeof(EP_DATA_TMP),  XK_PATH, tmp);
    xdec(EP_DATA_DATA, sizeof(EP_DATA_DATA), XK_PATH, dd);
    xdec(EK_LP_LOCK,   sizeof(EK_LP_LOCK),   XK_KW,   lock_kw);
    xdec(EK_LP_DB,     sizeof(EK_LP_DB),     XK_KW,   db_kw);
    struct stat st;
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
    char kws[9][48];
    xdec(EK_LP_KW,         sizeof(EK_LP_KW),         XK_KW,kws[0]);
    xdec(EK_LP_KW2,        sizeof(EK_LP_KW2),         XK_KW,kws[1]);
    xdec(EK_XPOSED,        sizeof(EK_XPOSED),         XK_KW,kws[2]);
    xdec(EK_LPHELPER,      sizeof(EK_LPHELPER),       XK_KW,kws[3]);
    xdec(EK_LP_HELPER2,    sizeof(EK_LP_HELPER2),     XK_KW,kws[4]);
    xdec(EK_CHELPUS,       sizeof(EK_CHELPUS),        XK_KW,kws[5]);
    xdec(EK_LP_LITEPATCHER,sizeof(EK_LP_LITEPATCHER), XK_KW,kws[6]);
    xdec(EK_LP_PKG2,       sizeof(EK_LP_PKG2),        XK_KW,kws[7]);
    xdec(EK_LP_PKG3,       sizeof(EK_LP_PKG3),        XK_KW,kws[8]);
    FILE *f=fopen(maps,"r"); if(!f) return 0;
    char line[1024];
    while(fgets(line,sizeof(line),f)) {
        char low[1024]; size_t ll=strlen(line); if(ll>=sizeof(low)) ll=sizeof(low)-1;
        for(size_t i=0;i<ll;i++) low[i]=(char)(line[i]>='A'&&line[i]<='Z'?line[i]+32:line[i]);
        low[ll]='\0';
        for(int k=0;k<9;k++) {
            if(k==2) continue;
            if(strstr(low,kws[k])){fclose(f);return 1;}
        }
    }
    fclose(f); return 0;
}

__attribute__((visibility("hidden")))
static int l4_lp_process(void) {
    char proc[16]; xdec(EP_PROC,sizeof(EP_PROC),XK_PATH,proc);
    char kws[8][48];
    xdec(EK_LP_KW,         sizeof(EK_LP_KW),         XK_KW,kws[0]);
    xdec(EK_CHELPUS,       sizeof(EK_CHELPUS),       XK_KW,kws[1]);
    xdec(EK_LACKY,         sizeof(EK_LACKY),         XK_KW,kws[2]);
    xdec(EK_LPHELPER,      sizeof(EK_LPHELPER),      XK_KW,kws[3]);
    xdec(EK_LP_HELPER2,    sizeof(EK_LP_HELPER2),    XK_KW,kws[4]);
    xdec(EK_LP_LITEPATCHER,sizeof(EK_LP_LITEPATCHER),XK_KW,kws[5]);
    xdec(EK_LP_PKG2,       sizeof(EK_LP_PKG2),       XK_KW,kws[6]);
    xdec(EK_LP_PKG3,       sizeof(EK_LP_PKG3),       XK_KW,kws[7]);
    DIR *d=opendir(proc); if(!d) return 0;
    struct dirent *e;
    while((e=readdir(d))!=NULL) {
        if(!isdigit((unsigned char)e->d_name[0])) continue;
        char path[64]; snprintf(path,sizeof(path),"%s/%s/cmdline",proc,e->d_name);
        int fd=open(path,O_RDONLY); if(fd<0) continue;
        char cmd[256]={0}; ssize_t n=read(fd,cmd,sizeof(cmd)-1); close(fd);
        if(n<=0) continue;
        for(int i=0;i<n;i++) if(cmd[i]>='A'&&cmd[i]<='Z') cmd[i]+=32;
        for(int k=0;k<8;k++) if(strstr(cmd,kws[k])){closedir(d);return 1;}
    }
    closedir(d); return 0;
}

__attribute__((visibility("hidden")))
static int l4_installer_check(JNIEnv *env, jobject ctx) {
    if(!env||!ctx) return 0;
    char kws[7][48];
    xdec(EK_LP_KW,         sizeof(EK_LP_KW),         XK_KW,kws[0]);
    xdec(EK_CHELPUS,       sizeof(EK_CHELPUS),       XK_KW,kws[1]);
    xdec(EK_LACKY,         sizeof(EK_LACKY),         XK_KW,kws[2]);
    xdec(EK_LPHELPER,      sizeof(EK_LPHELPER),      XK_KW,kws[3]);
    xdec(EK_LP_LITEPATCHER,sizeof(EK_LP_LITEPATCHER),XK_KW,kws[4]);
    xdec(EK_LP_PKG2,       sizeof(EK_LP_PKG2),       XK_KW,kws[5]);
    xdec(EK_LP_PKG3,       sizeof(EK_LP_PKG3),       XK_KW,kws[6]);
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
    for(int k=0;k<7;k++) if(strstr(low,kws[k])){bad=1;break;}
    env->ReleaseStringUTFChars(instJ,inst);
    return bad;
}

__attribute__((visibility("hidden")))
static int layer4_anti_patch(JNIEnv *env, jobject ctx) {
    if(l4_lp_filesystem())         return 1;
    if(l4_lp_maps())                return 1;
    if(l4_lp_process())             return 1;
    if(l4_installer_check(env,ctx)) return 1;
    return 0;
}

// ─── LAYER 5 — UNITY INTEGRITY ───────────────────────────────────────────────

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
    if(apk_ok&&!found) return 0;
    return 1;
}

__attribute__((visibility("hidden")))
static int l5_unity_class(JNIEnv *env) {
    if(!env) return 1;
    static const char cls[]="com/unity3d/ads/UnityAds";
    if(env->ExceptionCheck()) env->ExceptionClear();
    jclass c=env->FindClass(cls);
    if(env->ExceptionCheck()) env->ExceptionClear();
    if(!c) {
        char ua[16]; xdec(EK_UNITYADS,sizeof(EK_UNITYADS),XK_KW,ua);
        char maps_path[32]; xdec(EP_PROC_MAPS,sizeof(EP_PROC_MAPS),XK_PATH,maps_path);
        FILE *f=fopen(maps_path,"r"); if(!f) return 1;
        char line[1024]; int unity_in_maps=0;
        while(fgets(line,sizeof(line),f)) {
            for(int i=0;line[i];i++) if(line[i]>='A'&&line[i]<='Z') line[i]+=32;
            if(strstr(line,ua)){unity_in_maps=1;break;}
        }
        fclose(f);
        return unity_in_maps ? 0 : 1;
    }
    env->DeleteLocalRef(c); return 1;
}

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

// ─── LAYER 7 — ANTI-CLONER ───────────────────────────────────────────────────

static const uint8_t EK_PARALLEL[]    = {0xD3,0xC2,0xD1,0xC2,0xCF,0xCF,0xC6,0xCF,0xD0};
static const uint8_t EK_DUALSPACE[]   = {0xC7,0xD6,0xC2,0xCF,0xD0,0xD3,0xC2,0xC0,0xC6};
static const uint8_t EK_CLONEAPP[]    = {0xC0,0xCF,0xCC,0xCD,0xC6,0xC2,0xD3,0xD3};
static const uint8_t EK_TWINMATE[]    = {0xD7,0xD4,0xCA,0xCD,0xCE,0xC2,0xD7,0xC6};
static const uint8_t EK_MULTISPACE[]  = {0xCE,0xD6,0xCF,0xD7,0xCA,0xD0,0xD3,0xC2,0xC0,0xC6};

__attribute__((visibility("hidden")))
static int layer7_anti_cloner(JNIEnv *env, jobject ctx) {
    if(!env||!ctx) return 0;
    jclass cc=env->GetObjectClass(ctx);
    jmethodID gpn=env->GetMethodID(cc,"getPackageName","()Ljava/lang/String;");
    if(!gpn) return 0;
    jstring pkgJ=(jstring)env->CallObjectMethod(ctx,gpn); if(!pkgJ) return 0;
    const char *pkg=env->GetStringUTFChars(pkgJ,nullptr); if(!pkg) return 0;
    char low[256]={0}; size_t pl=strlen(pkg); if(pl>=sizeof(low)) pl=sizeof(low)-1;
    for(size_t i=0;i<pl;i++) low[i]=(char)(pkg[i]>='A'&&pkg[i]<='Z'?pkg[i]+32:pkg[i]);
    env->ReleaseStringUTFChars(pkgJ,pkg);
    char kws[5][32];
    xdec(EK_PARALLEL,   sizeof(EK_PARALLEL),   XK_KW, kws[0]);
    xdec(EK_DUALSPACE,  sizeof(EK_DUALSPACE),  XK_KW, kws[1]);
    xdec(EK_CLONEAPP,   sizeof(EK_CLONEAPP),   XK_KW, kws[2]);
    xdec(EK_TWINMATE,   sizeof(EK_TWINMATE),   XK_KW, kws[3]);
    xdec(EK_MULTISPACE, sizeof(EK_MULTISPACE), XK_KW, kws[4]);
    for(int k=0;k<5;k++) if(strstr(low,kws[k])) return 1;
    return 0;
}

// ─── LAYER 8 — ELF SELF-INTEGRITY ────────────────────────────────────────────

__attribute__((visibility("hidden")))
static int layer8_elf_self_integrity(void) {
    Dl_info info; memset(&info,0,sizeof(info));
    if(!dladdr((void*)layer8_elf_self_integrity,&info)) return 0;
    if(!info.dli_fname) return 0;

    int fd=open(info.dli_fname,O_RDONLY); if(fd<0) return 0;
    Elf64_Ehdr ehdr; memset(&ehdr,0,sizeof(ehdr));
    if(read(fd,&ehdr,sizeof(ehdr))<(ssize_t)sizeof(ehdr)){close(fd);return 0;}
    if(memcmp(ehdr.e_ident,ELFMAG,SELFMAG)!=0){close(fd);return 0;}

    off_t shoff=(off_t)ehdr.e_shoff;
    uint16_t shentsize=ehdr.e_shentsize, shnum=ehdr.e_shnum, shstrndx=ehdr.e_shstrndx;
    if(shoff==0||shnum==0||shstrndx>=shnum){close(fd);return 0;}

    Elf64_Shdr strhdr; memset(&strhdr,0,sizeof(strhdr));
    lseek(fd,shoff+(off_t)(shstrndx*shentsize),SEEK_SET);
    if(read(fd,&strhdr,sizeof(strhdr))<(ssize_t)sizeof(strhdr)){close(fd);return 0;}

    size_t strsz=(size_t)strhdr.sh_size;
    if(strsz==0||strsz>1048576){close(fd);return 0;}
    char *strtab=(char*)malloc(strsz); if(!strtab){close(fd);return 0;}
    lseek(fd,(off_t)strhdr.sh_offset,SEEK_SET);
    if(read(fd,strtab,(ssize_t)strsz)<(ssize_t)strsz){free(strtab);close(fd);return 0;}

    int result=1;
    for(uint16_t i=0;i<shnum&&result;i++) {
        Elf64_Shdr shdr; memset(&shdr,0,sizeof(shdr));
        lseek(fd,shoff+(off_t)(i*shentsize),SEEK_SET);
        if(read(fd,&shdr,sizeof(shdr))<(ssize_t)sizeof(shdr)) continue;
        if(shdr.sh_name>=strsz) continue;
        const char *name=strtab+shdr.sh_name;
        if(strcmp(name,".rodata")!=0) continue;
        if(shdr.sh_size==0||shdr.sh_size>4194304) continue;

        uint8_t *disk_data=(uint8_t*)malloc((size_t)shdr.sh_size);
        if(!disk_data) continue;
        lseek(fd,(off_t)shdr.sh_offset,SEEK_SET);
        ssize_t got=read(fd,disk_data,(ssize_t)shdr.sh_size);
        if(got<(ssize_t)shdr.sh_size){free(disk_data);continue;}

        const uint8_t *mem_data=(const uint8_t*)((uintptr_t)info.dli_fbase+shdr.sh_addr);
        sha256_ctx_t cdisk,cmem;
        sha256_init(&cdisk); sha256_update(&cdisk,disk_data,(size_t)shdr.sh_size);
        uint8_t hdisk[SHA256_DIGEST_LEN]; sha256_final(&cdisk,hdisk);
        sha256_init(&cmem); sha256_update(&cmem,mem_data,(size_t)shdr.sh_size);
        uint8_t hmem[SHA256_DIGEST_LEN]; sha256_final(&cmem,hmem);
        if(memcmp(hdisk,hmem,SHA256_DIGEST_LEN)!=0) result=0;
        free(disk_data);
    }
    free(strtab); close(fd);
    return result;
}

// ─── LAYER 9 — GOT HOOK CHECK ────────────────────────────────────────────────

__attribute__((visibility("hidden")))
static int layer9_got_hook_check(void) {
    void *libc=dlopen("libc.so",RTLD_NOW|RTLD_NOLOAD);
    if(!libc) libc=dlopen("libc.so.6",RTLD_NOW|RTLD_NOLOAD);
    if(!libc) return 0;

    void *sym_fopen = dlsym(libc,"fopen");
    void *sym_read  = dlsym(libc,"read");
    dlclose(libc);

    Dl_info info_fopen, info_read;
    if(sym_fopen && dladdr(sym_fopen,&info_fopen)) {
        if(info_fopen.dli_fname && !strstr(info_fopen.dli_fname,"libc")) return 1;
    }
    if(sym_read && dladdr(sym_read,&info_read)) {
        if(info_read.dli_fname && !strstr(info_read.dli_fname,"libc")) return 1;
    }
    return 0;
}

// ─── EARLY CONSTRUCTOR ───────────────────────────────────────────────────────
// NOTE: Do NOT call _exit() here. This constructor runs before Application.onCreate()
// and before BuildConfig.DEBUG is accessible, so any kill here is unconditional and
// will fire on dev devices with Magisk/KernelSU modules (false positive).
// Actual enforcement is delegated to the JNI layer (nativeIsHooked) which is gated
// by BuildConfig.DEBUG in AetherApplication.
__attribute__((constructor(101), visibility("hidden")))
static void early_security_check(void) {
    DEVLOG("libaether loaded");
}

// ─── JNI EXPORTS ─────────────────────────────────────────────────────────────

extern "C" {

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeIsHooked(JNIEnv *, jobject) {
    return layer1_anti_hook()?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeIsDebugged(JNIEnv *, jobject) {
    return layer2_anti_debug()?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckAntiPatch(JNIEnv *env, jobject, jobject ctx) {
    if(!l3_zip_integrity()) return JNI_FALSE;
    if(!l3_dex_magic())     return JNI_FALSE;
    if(layer4_anti_patch(env,ctx)) return JNI_FALSE;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckUnityIntact(JNIEnv *env, jobject) {
    if(!l5_unity_strings()) return JNI_FALSE;
    if(!l5_unity_class(env)) return JNI_FALSE;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckCloner(JNIEnv *env, jobject, jobject ctx) {
    return layer7_anti_cloner(env,ctx)?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckElfIntegrity(JNIEnv *, jobject) {
    return layer8_elf_self_integrity()?JNI_TRUE:JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckGotHook(JNIEnv *, jobject) {
    return layer9_got_hook_check()?JNI_FALSE:JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_dev_aether_manager_NativeAether_nativeCheckAll(JNIEnv *env, jobject, jobject ctx) {
    DEVLOG("start");
    if(layer1_anti_hook())         { DEVLOG("anti-hook");    return JNI_FALSE; }
    if(layer2_anti_debug())        { DEVLOG("anti-debug");   return JNI_FALSE; }
    if(!l3_zip_integrity())        { DEVLOG("zip");          return JNI_FALSE; }
    if(!l3_dex_magic())            { DEVLOG("dex");          return JNI_FALSE; }
    if(layer4_anti_patch(env,ctx)) { DEVLOG("anti-patch");   return JNI_FALSE; }
    if(!l5_unity_strings())        { DEVLOG("unity-str");    return JNI_FALSE; }
    if(layer7_anti_cloner(env,ctx)){ DEVLOG("cloner");       return JNI_FALSE; }
    if(!layer8_elf_self_integrity()){ DEVLOG("elf");         return JNI_FALSE; }
    if(layer9_got_hook_check())    { DEVLOG("got-hook");     return JNI_FALSE; }
    DEVLOG("AetherManager");
    return JNI_TRUE;
}

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

// ─── URL GETTERS (obfuscated, XOR decode saat runtime) ───────────────────────

JNIEXPORT jstring JNICALL
Java_dev_aether_manager_NativeAether_nativeGetVercelApi(JNIEnv *env, jobject) {
    char buf[sizeof(EU_VERCEL_API)+1];
    xdec(EU_VERCEL_API,sizeof(EU_VERCEL_API),XK_URL,buf);
    return env->NewStringUTF(buf);
}

JNIEXPORT jstring JNICALL
Java_dev_aether_manager_NativeAether_nativeGetGithubRepo(JNIEnv *env, jobject) {
    char buf[sizeof(EU_GITHUB_REPO)+1];
    xdec(EU_GITHUB_REPO,sizeof(EU_GITHUB_REPO),XK_URL,buf);
    return env->NewStringUTF(buf);
}

JNIEXPORT jstring JNICALL
Java_dev_aether_manager_NativeAether_nativeGetTelegram(JNIEnv *env, jobject) {
    char buf[sizeof(EU_TELEGRAM)+1];
    xdec(EU_TELEGRAM,sizeof(EU_TELEGRAM),XK_URL,buf);
    return env->NewStringUTF(buf);
}

JNIEXPORT jobjectArray JNICALL
Java_dev_aether_manager_NativeAether_nativeGetAdblockDnsKeywords(JNIEnv *env, jobject) {
    static const uint8_t EK_DNS_ADGUARD[]  = {0xC2,0xC7,0xC4,0xD6,0xC2,0xD1,0xC7};
    static const uint8_t EK_DNS_NEXTDNS[]  = {0xCD,0xC6,0xDB,0xD7,0xC7,0xCD,0xD0};
    static const uint8_t EK_DNS_BLOKADA[]  = {0xC1,0xCF,0xCC,0xC8,0xC2,0xC7,0xC2};
    static const uint8_t EK_DNS_PIHOLE[]   = {0xD3,0xCA,0xCB,0xCC,0xCF,0xC6};
    static const uint8_t EK_DNS_ADAWAY[]   = {0xC2,0xC7,0xC2,0xD4,0xC2,0xDA};
    static const uint8_t EK_DNS_ADBLOCK[]  = {0xC2,0xC7,0xC1,0xCF,0xCC,0xC0,0xC8};
    static const uint8_t EK_DNS_MULLVAD[]  = {0xCE,0xD6,0xCF,0xCF,0xD5,0xC2,0xC7};
    static const uint8_t EK_DNS_CONTROLD[] = {0xC0,0xCC,0xCD,0xD7,0xD1,0xCC,0xCF,0xC7};
    static const uint8_t EK_DNS_QUAD9[]    = {0xD2,0xD6,0xC2,0xC7,0x9A};
    static const uint8_t EK_DNS_RETHINK[]  = {0xD1,0xC6,0xD7,0xCB,0xCA,0xCD,0xC8,0xC7,0xCD,0xD0};
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
    static const uint8_t EK_H_ADAWAY[]      = {0xC2,0xC7,0xC2,0xD4,0xC2,0xDA};
    static const uint8_t EK_H_ADBLOCK[]     = {0xC2,0xC7,0xC1,0xCF,0xCC,0xC0,0xC8};
    static const uint8_t EK_H_GENERATED[]   = {0xC4,0xC6,0xCD,0xC6,0xD1,0xC2,0xD7,0xC6,0xC7,0x83,0xC1,0xDA};
    static const uint8_t EK_H_DOUBLECLICK[] = {0xC2,0xC7,0x8D,0xC7,0xCC,0xD6,0xC1,0xCF,0xC6,0xC0,0xCF,0xCA,0xC0,0xC8,0x8D,0xCD,0xC6,0xD7};
    static const uint8_t EK_H_ADMOB[]       = {0xC2,0xC7,0xCE,0xCC,0xC1};
    static const uint8_t EK_H_UNITY[]       = {0xD6,0xCD,0xCA,0xD7,0xDA,0xC2,0xC7,0xD0,0x8D,0xD6,0xCD,0xCA,0xD7,0xDA,0x90,0xC7,0x8D,0xC0,0xCC,0xCE};
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

} // extern "C"
