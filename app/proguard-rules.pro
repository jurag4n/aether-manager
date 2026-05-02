# ── Repackage semua ke root, izinkan mixed case ───────────────────────────────
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# ── Optimasi R8 maksimal ──────────────────────────────────────────────────────
-optimizationpasses 7
-optimizations !code/simplification/cast,field/*,class/merging/*,code/allocation/variable

# ── Attributes wajib ─────────────────────────────────────────────────────────
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# =============================================================================
# AETHER MANAGER — CORE (wajib di AndroidManifest, tidak bisa di-rename)
# =============================================================================

-keep public class dev.aether.manager.MainActivity { <init>(); }
-keep public class dev.aether.manager.SplashActivity { <init>(); }
-keep public class dev.aether.manager.SetupActivity { <init>(); }
-keep public class dev.aether.manager.AetherApplication { <init>(); }
-keep public class dev.aether.manager.service.AetherService { <init>(); }
-keep public class dev.aether.manager.service.BootReceiver { <init>(); }

# =============================================================================
# JNI — libaether.so
# =============================================================================

-keep class dev.aether.manager.NativeAether {
    public static boolean tryLoad();
    public static boolean isLoaded();
    public static native boolean nativeCheckSignature(java.lang.String);
    public static native java.lang.String nativeGetApkHash(android.content.Context);
    public static native boolean nativeCheckIntegrity(android.content.Context, java.lang.String);
    public static native boolean nativeIsHooked();
    public static native boolean nativeIsDebugged();
    public static native boolean nativeCheckAntiPatch(android.content.Context);
    public static native boolean nativeCheckUnityIntact();
    public static native boolean nativeCheckAll(android.content.Context);
    public static native void nativeKillProcess();
    public static native java.lang.String nativeGetGameId();
    public static native java.lang.String nativeGetGithubApi();
    public static native java.lang.String[] nativeGetAdblockDnsKeywords();
    public static native java.lang.String[] nativeGetHostsSignatures();
    public static native java.lang.String nativeGetPackageName();
}

# Safety net untuk semua native method
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# =============================================================================
# KOTLIN
# =============================================================================

-keep class kotlin.Metadata { *; }
-keepclassmembernames class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
    public static void parameter*(...);
}

# =============================================================================
# KOTLINX SERIALIZATION
# =============================================================================

-dontnote kotlinx.serialization.AnnotationsKt
-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
    <fields>;
}

# =============================================================================
# JETPACK COMPOSE
# =============================================================================

-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(...);
    void sourceInformationMarkerStart(...);
    void sourceInformationMarkerEnd(...);
    boolean isTraceInProgress();
    void traceEventStart(...);
    void traceEventEnd();
}

# =============================================================================
# OKHTTP — hanya yang wajib, jangan keepnames seluruh namespace
# =============================================================================

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.publicsuffix.PublicSuffixDatabase { *; }
-keepclassmembers class okhttp3.** { <init>(...); }

# =============================================================================
# RETROFIT
# =============================================================================

-dontwarn retrofit2.**
-keepclassmembers,allowobfuscation class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# =============================================================================
# LIBSU
# =============================================================================

-keep class com.topjohnwu.superuser.** { *; }
-keepclassmembers class com.topjohnwu.superuser.** { *; }

# =============================================================================
# ADS
# =============================================================================

-keep class com.unity3d.ads.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.google.ads.** { *; }

# =============================================================================
# MMKV
# =============================================================================

-keep class com.tencent.mmkv.** { *; }
-keepclassmembers class com.tencent.mmkv.** { *; }

# =============================================================================
# ROOM
# =============================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# =============================================================================
# LOTTIE / COIL
# =============================================================================

-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**
-dontwarn coil.**

# =============================================================================
# LOGGING — strip di release
# =============================================================================

-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}
-assumenosideeffects class timber.log.Timber$Tree {
    public *** v(...);
    public *** d(...);
    public *** i(...);
    public *** w(...);
    public *** e(...);
}
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# =============================================================================
# PARCELABLE / SERIALIZABLE / ENUM
# =============================================================================

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public final ** name();
    public final int ordinal();
}

# =============================================================================
# SUPPRESS WARNINGS
# =============================================================================

-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn **$$Lambda$*
-dontwarn io.ktor.**

# =============================================================================
# OBFUSCATION DICTIONARY
# =============================================================================

-obfuscationdictionary      proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt