# ─────────────────────────────────────────────────────────────────────────────
# Aether Manager — ProGuard / R8 Rules
# ─────────────────────────────────────────────────────────────────────────────

# ── Attributes ───────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Gson ─────────────────────────────────────────────────────────────────────
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ── Parcelable ───────────────────────────────────────────────────────────────
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepnames class * implements android.os.Parcelable

# ── Unity Ads ────────────────────────────────────────────────────────────────
-keep class com.unity3d.**   { *; }
-keep class com.unity3d.ads.** { *; }

# ── Native methods (JNI) — KRITIS untuk libprotect.so ────────────────────────
# Pastikan semua method dengan prefix "native" tidak di-rename/strip
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ── IntegrityGuard — jangan obfuscate nama class & native methods ─────────────
# R8 harus bisa menemukan method yang di-export dari JNI dengan nama persis ini.
-keep class dev.aether.manager.security.IntegrityGuard {
    private native java.lang.String nativeGetApkHash(android.content.Context);
    private native boolean nativeIsHooked();
    private native boolean nativeCheckIntegrity(android.content.Context, java.lang.String);
    private native void nativeKillProcess(java.lang.String);
    private native java.lang.String nativeGetPackageName();
}

# ── Kotlin intrinsics (reduce size) ──────────────────────────────────────────
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    ** requireNonNull(...);
}

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── OkHttp / Retrofit ────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ── Ktor ─────────────────────────────────────────────────────────────────────
-dontwarn io.ktor.**

# ── libsu ────────────────────────────────────────────────────────────────────
-keep class com.topjohnwu.superuser.** { *; }

# ── MMKV ─────────────────────────────────────────────────────────────────────
-keep class com.tencent.mmkv.** { *; }

# ── Lottie ───────────────────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }

# ── Coil ─────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Kotlin serialization ──────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Transient <fields>;
}

# ── Suppress common warnings ──────────────────────────────────────────────────
-dontwarn java.lang.invoke.**
-dontwarn **$$Lambda$*
-dontwarn javax.annotation.**
