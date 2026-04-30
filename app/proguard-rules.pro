-repackageclasses ''
-allowaccessmodification
-overloadaggressively

-optimizationpasses 7
-optimizations !code/simplification/cast,field/*,class/merging/*,code/allocation/variable

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontusemixedcaseclassnames

-keep public class dev.aether.manager.MainActivity
-keep public class dev.aether.manager.SplashActivity
-keep public class dev.aether.manager.SetupActivity
-keep public class dev.aether.manager.AetherApplication

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

-keepclassmembers class dev.aether.manager.CimolAgent$ThermalZone {
    <fields>;
    <init>(...);
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep class kotlin.Metadata { *; }

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
    public static void parameter*(...);
}

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

-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(...);
    void sourceInformationMarkerStart(...);
    void sourceInformationMarkerEnd(...);
    boolean isTraceInProgress();
    void traceEventStart(...);
    void traceEventEnd();
}

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.publicsuffix.PublicSuffixDatabase { *; }
-keepnames class okhttp3.** { *; }

-dontwarn kotlinx.serialization.**

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keep class com.topjohnwu.superuser.** { *; }
-keepclassmembers class com.topjohnwu.superuser.** { *; }

-keep class com.unity3d.ads.** { *; }
-keepclassmembers class com.unity3d.ads.** { *; }

-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.google.ads.** { *; }

-keep class com.tencent.mmkv.** { *; }
-keepclassmembers class com.tencent.mmkv.** { *; }

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.**

-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

-dontwarn coil.**

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

-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn **$$Lambda$*
-dontwarn io.ktor.**

-obfuscationdictionary      proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt
