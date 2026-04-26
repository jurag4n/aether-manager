# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
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

# DISABLED: causes VerifyError with Kotlin 2.x + R8
#-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
#	public static void check*(...);
#	public static void throw*(...);
#}

# DISABLED: causes VerifyError with Kotlin 2.x + R8
#-assumenosideeffects class java.util.Objects{
#    ** requireNonNull(...);
#}

#-keep class com.frb.engine.Starter {
#    public static void main(java.lang.String[]);
#}

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# ════════════════════════════════════════════════════════════════
# REQUIRED RULES — Aether Manager
# ════════════════════════════════════════════════════════════════

# ── Fix VerifyError: Compose + R8 optimization conflict ──────────
# R8 merges/inlines Compose lambdas and breaks method signatures at runtime.
-dontoptimize
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── JNI bridges (CRITICAL — app will crash without these) ────────
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
