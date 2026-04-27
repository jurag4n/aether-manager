@file:Suppress("UnstableApiUsage")
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val gitHash: String by lazy {
    try {
        providers.exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
        }.standardOutput.asText.get().trim()
    } catch (_: Exception) { "unknown" }
}

android {
    namespace  = "dev.aether.manager"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.aether.manager"
        minSdk        = 30
        targetSdk     = 36
        versionCode   = 250
        versionName   = "2.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf(
                    "-DANDROID_STL=c++_static",
                    "-DANDROID_ARM_NEON=TRUE"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path    = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        create("release") {
            storeFile     = rootProject.file("aether.jks")
            storePassword = System.getenv("STORE_PASSWORD") ?: ""
            keyAlias      = System.getenv("KEY_ALIAS")      ?: ""
            keyPassword   = System.getenv("KEY_PASSWORD")   ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.map { it as BaseVariantOutputImpl }.forEach { output ->
            output.outputFileName =
                "manager-${variant.versionName}-${variant.versionCode}-$gitHash-${variant.buildType.name}.apk"
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    lint {
        abortOnError       = false
        checkReleaseBuilds = false
        ignoreTestSources  = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
            optIn.add("kotlin.RequiresOptIn")
            freeCompilerArgs.add("-Xsuppress-version-warnings")
        }
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/MANIFEST.MF",
                "**.proto",
                "META-INF/com/**"
            )
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.unity.ads)
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.libsu.io)
    implementation(libs.mmkv)
    implementation(libs.zstd.jni)
    implementation(libs.sqlite.bundled)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.compose.foundation)
    implementation(libs.lottie.compose)
    implementation(libs.timber)
    implementation(libs.androidx.biometric)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content)
    implementation(libs.ktor.serialization)
    implementation(libs.shimmer)
    implementation(libs.compose.charts)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.startup)
    debugImplementation(libs.leakcanary.android)
    debugImplementation(libs.androidx.ui.tooling)
}
