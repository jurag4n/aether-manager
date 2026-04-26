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

# ── Kotlin intrinsics (DISABLED — VerifyError risk on Kotlin 2.x + R8) ─────
# -assumenosideeffects class kotlin.jvm.internal.Intrinsics { ... }  <- JANGAN aktifkan
# -assumenosideeffects class java.util.Objects { ... }               <- JANGAN aktifkan

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

# ═══════════════════════════════════════════════════════════════════════════════
# FIX RUNTIME CRASHES
# ═══════════════════════════════════════════════════════════════════════════════

# ── DISABLE: Kotlin intrinsics removal (penyebab VerifyError di Kotlin 2.x) ──
# Rule -assumenosideeffects pada Intrinsics tidak aman di R8 fullMode + Kotlin 2.x
# Baris lama dihapus dari sini — jangan tambahkan kembali.

# ── Fix VerifyError: Compose + R8 fullMode ────────────────────────────────────
# R8 agresif meng-inline lambda Compose dan merusak method signature saat runtime.
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Fix VerifyError: Kotlin lambda / SAM conversion ──────────────────────────
-keep class kotlin.jvm.functions.** { *; }
-keep class kotlin.jvm.internal.Lambda { *; }
-keep class * extends kotlin.jvm.internal.Lambda { *; }

# ── Fix "No package ID 6a" — resource compose-charts ter-strip ───────────────
-keep class io.github.ehsannarmani.** { *; }
-dontwarn io.github.ehsannarmani.**

# ── Fix shimmer resource strip ────────────────────────────────────────────────
-keep class com.facebook.shimmer.** { *; }
