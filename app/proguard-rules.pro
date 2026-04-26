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

# ── Native methods (JNI) — KRITIS untuk libaether.so & libcimolagent.so ──────
# Pastikan semua method dengan prefix "native" tidak di-rename/strip
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ── NativeAether — JNI bridge ke libaether.so ─────────────────────────────────
# R8 harus bisa menemukan semua external fun dengan nama JNI yang persis.
-keep class dev.aether.manager.NativeAether {
    public static *** tryLoad();
    public static native ***;
    native <methods>;
}

# ── CimolAgent — JNI bridge ke libcimolagent.so ───────────────────────────────
-keep class dev.aether.manager.CimolAgent {
    public static *** tryLoad();
    public static *** isAvailable();
    native <methods>;
}

# ── BroadcastReceiver & Service — harus tetap ada di release build ────────────
# BootReceiver ada di dalam AetherService.kt; R8 bisa hapus jika tidak di-keep.
-keep class dev.aether.manager.service.BootReceiver { *; }
-keep class dev.aether.manager.service.AetherService { *; }

# ── Application class ─────────────────────────────────────────────────────────
-keep class dev.aether.manager.AetherApplication { *; }

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
