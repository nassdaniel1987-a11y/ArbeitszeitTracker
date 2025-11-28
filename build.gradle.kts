// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.7.3" apply false  // Updated: Android 15 Support
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false  // Gradle 9 kompatibel
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false  // Compose Compiler Plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20" apply false  // Kotlinx Serialization für Type-safe Navigation
    id("com.google.devtools.ksp") version "2.2.20-2.0.4" apply false  // KSP für Kotlin 2.2.20
    id("com.google.dagger.hilt.android") version "2.57.1" apply false  // Kotlin 2.2.20 + KSP2 kompatibel
    id("com.google.gms.google-services") version "4.4.2" apply false  // Firebase Google Services
}
