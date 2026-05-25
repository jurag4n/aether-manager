#include <jni.h>
#include <stdint.h>
#include <string.h>
#include <ctype.h>

#define KSIG 0x31

static const uint8_t EXPECTED_SIG[] = {
    0x53,0x09,0x55,0x02,0x06,0x00,0x52,0x00,0x50,0x01,0x07,0x57,0x05,0x05,0x04,0x54,
    0x03,0x06,0x09,0x52,0x07,0x07,0x06,0x03,0x03,0x08,0x01,0x02,0x57,0x00,0x53,0x09,
    0x52,0x03,0x00,0x55,0x07,0x00,0x54,0x06,0x55,0x05,0x03,0x06,0x57,0x57,0x57,0x04,
    0x04,0x04,0x01,0x53,0x02,0x53,0x50,0x01,0x07,0x54,0x05,0x52,0x54,0x52,0x04,0x09
};

static void decode(const uint8_t *src, size_t len, uint8_t key, char *dst) {
    for (size_t i = 0; i < len; ++i) dst[i] = (char)(src[i] ^ key);
    dst[len] = '\0';
}

static void to_lower_ascii(char *s) {
    if (!s) return;
    for (; *s; ++s) *s = (char)tolower((unsigned char)*s);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aether_security_AetherSecurityNative_nativeVerifySignature(JNIEnv *env, jobject, jstring shaJ) {
    if (!shaJ) return JNI_FALSE;

    char expected[96];
    decode(EXPECTED_SIG, sizeof(EXPECTED_SIG), KSIG, expected);
    to_lower_ascii(expected);

    const char *sha = env->GetStringUTFChars(shaJ, nullptr);
    if (!sha) return JNI_FALSE;

    char got[96];
    size_t len = strlen(sha);
    if (len >= sizeof(got)) len = sizeof(got) - 1;
    for (size_t i = 0; i < len; ++i) got[i] = (char)tolower((unsigned char)sha[i]);
    got[len] = '\0';

    int ok = (len == 64 && strlen(expected) == 64 && strcmp(got, expected) == 0);
    env->ReleaseStringUTFChars(shaJ, sha);
    return ok ? JNI_TRUE : JNI_FALSE;
}
