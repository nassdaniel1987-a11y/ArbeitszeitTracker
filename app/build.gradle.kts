plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")  // Compose Compiler Plugin für Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.serialization")  // Kotlinx Serialization für Type-safe Navigation
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.arbeitszeit.tracker"
    compileSdk = 35  // Android 15 (Zukunftssicher)

    defaultConfig {
        applicationId = "com.arbeitszeit.tracker"
        minSdk = 26
        targetSdk = 35  // Android 15 Support
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Room Schema Export - Dokumentiert Database-Änderungen automatisch
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // Code-Optimierung aktivieren
            isShrinkResources = true  // Ungenutzte Ressourcen entfernen
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xcontext-receivers"  // Cleaner Code durch Context Receivers
        )
    }
    buildFeatures {
        compose = true
    }
    // composeOptions nicht mehr nötig - Compose Compiler Plugin regelt das automatisch
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/*.kotlin_module"
            // Exclude module-info files that cause jlink issues
            excludes += "module-info.class"
            excludes += "META-INF/versions/*/module-info.class"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose & Material 3
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))  // Neueste BOM mit K2 Compiler Support
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0")  // Adaptive Navigation für Phone/Tablet/Desktop
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")  // Window Size Classes
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")  // Type-safe Navigation Support
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")  // Kotlinx Serialization für Type-safe Routes
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.5")  // Google Fonts Support

    // Room Database - Kotlin 2.2.20 kompatibel
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Hilt Dependency Injection - Kotlin 2.2.20 + KSP2 kompatibel
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-android-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")  // Hilt + Compose Navigation
    implementation("androidx.hilt:hilt-work:1.2.0")  // Hilt + WorkManager
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")


    // Apache POI (Excel)
    implementation("org.apache.poi:poi:5.3.0")  // Neueste stabile Version
    implementation("org.apache.poi:poi-ooxml:5.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")  // Neueste stabile
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Google Play Services - Geofencing
    implementation("com.google.android.gms:play-services-location:21.3.0")  // Neueste

    // Google Drive API für Cloud-Backup (disabled - requires OAuth setup)
    // implementation("com.google.android.gms:play-services-auth:21.2.0")
    // implementation("com.google.api-client:google-api-client-android:2.6.0")
    // implementation("com.google.apis:google-api-services-drive:v3-rev20240914-2.0.0")

    // OpenStreetMap (OSMDroid) - Open Source Maps
    implementation("org.osmdroid:osmdroid-android:6.1.20")  // Neueste

    // Google Plus Codes (Open Location Code)
    implementation("com.google.openlocationcode:openlocationcode:1.0.4")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.11.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing - Unit Tests
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("app.cash.turbine:turbine:1.1.0")  // Flow Testing

    // Hilt Testing
    testImplementation("com.google.dagger:hilt-android-testing:2.57.1")
    kspTest("com.google.dagger:hilt-android-compiler:2.57.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.57.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.57.1")
}
