# Add project specific ProGuard rules here.

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
# FIX VerifyError — larang R8 merge class (register overflow)
# AGP 8.13.2 + Kotlin 2.3.x: class merging pada Compose composable
# menghasilkan <clinit> >77 argument registers → crash di runtime
# ════════════════════════════════════════════════════════════════
-optimizations !class/merging/*

# ── JNI bridges ──────────────────────────────────────────────────
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
