# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

##---------------Begin: proguard configuration for Gson  ----------
-keepattributes Signature
-keepattributes *Annotation*
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
##---------------End: proguard configuration for Gson  ----------

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepnames class * implements android.os.Parcelable

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ════════════════════════════════════════════════════════════════
# FIX VerifyError — R8 fullMode=false + rules ini wajib semua
# Root cause: R8 AGP 8.7.x merge/inline Compose lambdas → bytecode
# invalid (>77 argument registers) → ditolak Android Runtime verifier
# ════════════════════════════════════════════════════════════════

# Disable optimization & preverification sepenuhnya
-dontoptimize
-dontpreverify

# ── Compose: jangan disentuh R8 sama sekali ──────────────────────
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.runtime.internal.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── Kotlin lambda/function classes — R8 inlining = register overflow
-keep class kotlin.jvm.functions.** { *; }
-keep class kotlin.coroutines.** { *; }
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keepclassmembers class * implements kotlin.jvm.functions.Function* { *; }

# ── Kotlin companion objects — jangan di-merge
-keepclassmembers class * {
    public static ** Companion;
}

# ── JNI bridges (CRITICAL — app crash tanpa ini) ─────────────────
-keep class dev.aether.manager.NativeAether {
    public static *** tryLoad();
    native <methods>;
}
-keep class dev.aether.manager.CimolAgent {
    public static *** tryLoad();
    public static *** isAvailable();
    native <methods>;
}
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# ── App core ─────────────────────────────────────────────────────
-keep class dev.aether.manager.AetherApplication { *; }
-keep class dev.aether.manager.service.AetherService { *; }
-keep class dev.aether.manager.service.BootReceiver { *; }

# ── Room ─────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Kotlin serialization ─────────────────────────────────────────
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ── Libraries ────────────────────────────────────────────────────
-keep class com.topjohnwu.superuser.** { *; }
-keep class com.tencent.mmkv.** { *; }
-keep class com.unity3d.** { *; }
-keep class com.airbnb.lottie.** { *; }
-keep class io.github.ehsannarmani.** { *; }
-keep class com.facebook.shimmer.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn io.ktor.**
-dontwarn coil.**
