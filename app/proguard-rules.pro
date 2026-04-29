# =============================================================================
# Aether Manager — ProGuard / R8 Rules
# Package : dev.aether.manager
# Target  : R8 full mode + proguard-android-optimize.txt
# =============================================================================

# ── Agresif: repackage semua class ke root package tunggal ───────────────────
# Semua package (termasuk lib pihak ketiga) digabung jadi satu flat namespace
# sehingga di Dex explorer tidak ada struktur package yang terbaca
-repackageclasses ''

# Lindungi package utama app dari repackaging — Activities & JNI harus tetap
# di dev.aether.manager agar AndroidManifest dan System.loadLibrary() valid
-keeppackagenames dev.aether.manager
-keeppackagenames dev.aether.manager.**
-allowaccessmodification
-overloadaggressively

# ── Optimasi R8 maksimal ──────────────────────────────────────────────────────
-optimizationpasses 7
-optimizations !code/simplification/cast,field/*,class/merging/*,code/allocation/variable

# ── Attributes wajib ─────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontusemixedcaseclassnames

# =============================================================================
# AETHER MANAGER — CORE CLASSES
# Jangan rename/strip class utama aplikasi
# =============================================================================

# Activities, Application, Service
-keep public class dev.aether.manager.MainActivity
-keep public class dev.aether.manager.SplashActivity
-keep public class dev.aether.manager.SetupActivity
-keep public class dev.aether.manager.AetherApplication

# =============================================================================
# JNI — libaether.so & libcimolagent.so
# Semua external fun harus dipertahankan persis — nama method adalah JNI symbol
# =============================================================================

-keep class dev.aether.manager.NativeAether {
    public static boolean tryLoad();
    public static boolean isLoaded();
    public static native boolean nativeCheckSignature(java.lang.String);
    public static native boolean nativeCheckAntiPatch(android.content.Context);
    public static native boolean nativeCheckUnityIntact();
    public static native boolean nativeCheckAll(android.content.Context);
    public static native void nativeKillProcess();
    public static native java.lang.String nativeGetGameId();
    public static native java.lang.String nativeGetGithubApi();
    public static native java.lang.String[] nativeGetAdblockDnsKeywords();
    public static native java.lang.String[] nativeGetHostsSignatures();
}

-keep class dev.aether.manager.CimolAgent {
    public static boolean tryLoad();
    public static boolean isAvailable();
    public static native int[] getCpuFreqsNow();
    public static native int[] getCpuFreqMinMax();
    public static native java.lang.String[] getCpuGovernors();
    public static native int getCpuUsagePercent(int);
    public static native int[] getThermalRaw();
    public static native java.lang.String[] getThermalTypes();
    public static native int getCpuTempMilliC();
    public static native int getGpuTempMilliC();
    public static native long[] getMemInfoKb();
    public static native long[] getZramStats();
    public static native long[] getBatteryStats();
    public static native java.lang.String getBatteryStatus();
    public static native java.lang.String[] getIoSchedulers();
    public static native boolean setIoScheduler(java.lang.String, java.lang.String);
    public static native long[] getKsmStats();
    public static native long[] getProcessStats();
    public static native java.lang.String getProcessName(int);
    public static native int getGpuFreqNow();
    public static native int getGpuBusyPercent();
    public static native java.lang.String execWithTimeout(java.lang.String, int);
    public static native boolean writeSysfs(java.lang.String, java.lang.String);
    public static native java.lang.String readSysfs(java.lang.String);
}

# Data class inside CimolAgent (dipakai via Kotlin, butuh field intact)
-keepclassmembers class dev.aether.manager.CimolAgent$ThermalZone {
    <fields>;
    <init>(...);
}

# Rule umum untuk semua native method (safety net)
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# static initializer dijaga otomatis karena class NativeAether & CimolAgent
# sudah di-keep penuh via rule explicit di atas.

# =============================================================================
# KOTLIN
# =============================================================================

# Metadata diperlukan untuk reflection dan serialization
-keep class kotlin.Metadata { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Hapus null-check intrinsics untuk APK lebih kecil (aman di release)
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
    public static void parameter*(...);
}

# =============================================================================
# KOTLINX SERIALIZATION
# Dipakai di UpdateChecker untuk parse JSON GitHub API
# =============================================================================

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
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
# R8 sudah handle Compose dengan baik — hanya strip source info debug
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
# OKHTTP — dipakai UpdateChecker & download APK
# =============================================================================

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.publicsuffix.PublicSuffixDatabase { *; }
-keepnames class okhttp3.** { *; }

# =============================================================================
# KOTLINX SERIALIZATION JSON (dipakai langsung, bukan via Gson)
# =============================================================================

-dontwarn kotlinx.serialization.**

# =============================================================================
# RETROFIT — ada di dependencies, keep interface-nya
# =============================================================================

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# =============================================================================
# LIBSU — root shell, KRITIS untuk TweakApplier
# =============================================================================

-keep class com.topjohnwu.superuser.** { *; }
-keepclassmembers class com.topjohnwu.superuser.** { *; }

# =============================================================================
# UNITY ADS — dipakai AdManager & InterstitialAdManager
# =============================================================================

-keep class com.unity3d.ads.** { *; }
-keepclassmembers class com.unity3d.ads.** { *; }

# =============================================================================
# GOOGLE MOBILE ADS (AdMob) — dipakai AdMobInterstitialManager
# =============================================================================

-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.google.ads.** { *; }

# =============================================================================
# MMKV — SharedPreferences pengganti
# =============================================================================

-keep class com.tencent.mmkv.** { *; }
-keepclassmembers class com.tencent.mmkv.** { *; }

# =============================================================================
# ROOM — database (ada di deps, meski belum banyak dipakai)
# =============================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

# =============================================================================
# LOTTIE — animasi
# =============================================================================

-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# =============================================================================
# COIL — image loading
# =============================================================================

-dontwarn coil.**

# =============================================================================
# TIMBER — logging (strip di release)
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

# =============================================================================
# ANDROID LOG — strip di release
# =============================================================================

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# =============================================================================
# PARCELABLE
# =============================================================================

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# =============================================================================
# SERIALIZABLE
# =============================================================================

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# =============================================================================
# ENUM — sering dipakai di kondisional Kotlin
# =============================================================================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public final ** name();
    public final int ordinal();
}

# =============================================================================
# SUPPRESS COMMON WARNINGS — library yang tidak perlu warning
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
