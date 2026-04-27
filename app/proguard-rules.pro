# ============================================================
# AE Manager - proguard-rules.pro
# compileSdk 36 / targetSdk 36 / minSdk 30
# ============================================================

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# KOTLIN
# ============================================================
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class * {
    @kotlin.jvm.JvmStatic <methods>;
    @kotlin.jvm.JvmField <fields>;
}

# Kotlin Serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** *; }
-keep,includedescriptorclasses class dev.aether.manager.**$$serializer { *; }
-keepclassmembers class dev.aether.manager.** {
    *** Companion;
}
-keepclasseswithmembers class dev.aether.manager.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ============================================================
# ANDROIDX / COMPOSE
# ============================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Navigation
-keep class androidx.navigation.** { *; }

# Lifecycle / ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Startup
-keep class * extends androidx.startup.Initializer { *; }

# Splash screen
-keep class androidx.core.splashscreen.** { *; }

# ============================================================
# ROOM
# ============================================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ============================================================
# OKHTTP / RETROFIT
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Gson (retrofit converter)
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ============================================================
# KTOR
# ============================================================
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }

# ============================================================
# LIBSU (root shell)
# ============================================================
-keep class com.topjohnwu.superuser.** { *; }
-dontwarn com.topjohnwu.superuser.**

# ============================================================
# MMKV
# ============================================================
-keep class com.tencent.mmkv.** { *; }
-keepclasseswithmembernames class * { native <methods>; }

# ============================================================
# COIL
# ============================================================
-dontwarn coil.**
-keep class coil.** { *; }

# ============================================================
# LOTTIE
# ============================================================
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ============================================================
# TIMBER
# ============================================================
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.Timber { *; }

# ============================================================
# FIREBASE / GMS ADS
# ============================================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Unity Ads
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.ads.**

# ============================================================
# ZSTD / SQLITE
# ============================================================
-dontwarn com.github.luben.zstd.**
-keep class com.github.luben.zstd.** { *; }
-keep class org.sqlite.** { *; }
-dontwarn org.sqlite.**

# ============================================================
# APP MODEL CLASSES (sesuaikan package)
# ============================================================
-keep class dev.aether.manager.data.model.** { *; }
-keep class dev.aether.manager.data.entity.** { *; }

# ============================================================
# NATIVE (CMake / JNI)
# ============================================================
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class dev.aether.manager.native.** { *; }