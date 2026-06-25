#include <jni.h>
#include <string.h>
#include <ctype.h>

static const char EXPECTED_SIG[] = "0000000000000000000000000000000000000000000000000000000000000000";

static int is_configured(const char *value) {
    if (!value) return 0;
    size_t len = strlen(value);
    if (len != 64) return 0;
    int all_zero = 1;
    for (size_t i = 0; i < len; ++i) {
        char c = (char)tolower((unsigned char)value[i]);
        if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return 0;
        if (c != '0') all_zero = 0;
    }
    return !all_zero;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aether_security_AetherSecurityNative_nativeVerifySignature(JNIEnv *env, jobject, jstring shaJ) {
    if (!shaJ || !is_configured(EXPECTED_SIG)) return JNI_FALSE;

    const char *sha = env->GetStringUTFChars(shaJ, nullptr);
    if (!sha) return JNI_FALSE;

    char got[65];
    size_t len = strlen(sha);
    if (len > 64) len = 64;
    for (size_t i = 0; i < len; ++i) got[i] = (char)tolower((unsigned char)sha[i]);
    got[len] = '\0';

    char expected[65];
    for (size_t i = 0; i < 64; ++i) expected[i] = (char)tolower((unsigned char)EXPECTED_SIG[i]);
    expected[64] = '\0';

    int ok = (strlen(got) == 64 && strcmp(got, expected) == 0);
    env->ReleaseStringUTFChars(shaJ, sha);
    return ok ? JNI_TRUE : JNI_FALSE;
}
