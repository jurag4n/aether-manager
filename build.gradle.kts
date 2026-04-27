plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}

subprojects {
    configurations.all {
        resolutionStrategy {
            force("androidx.core:core:1.16.0")
            force("androidx.core:core-ktx:1.16.0")
            force("androidx.activity:activity:1.10.1")
            force("androidx.activity:activity-ktx:1.10.1")
            force("androidx.activity:activity-compose:1.10.1")
            force("androidx.navigation:navigation-compose:2.9.0")
            force("androidx.navigation:navigation-runtime-ktx:2.9.0")
            force("androidx.navigation:navigation-common-ktx:2.9.0")
            force("io.ktor:ktor-client-android:3.1.3")
            force("io.ktor:ktor-client-core:3.1.3")
            // FIX INTI: paksa semua transitive dep pakai stdlib yg sama
            // sehingga compose-charts tidak bisa bawa stdlib 2.3.x
            force("org.jetbrains.kotlin:kotlin-stdlib:2.1.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.21")
        }
    }
}
