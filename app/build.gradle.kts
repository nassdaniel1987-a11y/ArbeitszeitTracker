plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)  // Code Quality
    alias(libs.plugins.baseline.profile)  // Performance
}

// Gemini API Key aus local.properties lesen
val geminiApiKey: String = run {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.readLines()
            .firstOrNull { it.startsWith("GEMINI_API_KEY=") }
            ?.substringAfter("=")
            ?: ""
    } else {
        ""
    }
}

android {
    namespace = "com.arbeitszeit.tracker"
    compileSdk = 35  // Android 15 (Zukunftssicher)

    defaultConfig {
        applicationId = "com.arbeitszeit.tracker"
        minSdk = 26
        targetSdk = 35  // Android 15 Support
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Room Schema Export - Dokumentiert Database-Änderungen automatisch
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        // Gemini API Key in BuildConfig verfügbar machen
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // Code-Optimierung aktivieren
            isShrinkResources = true  // Ungenutzte Ressourcen entfernen
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Baseline Profile aktivieren
            baselineProfile.automaticGenerationDuringBuild = true
        }
        debug {
            // Baseline Profile für Debug-Builds
            baselineProfile.automaticGenerationDuringBuild = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xcontext-receivers",  // Cleaner Code durch Context Receivers
                // Compose Compiler Metrics & Reports (für Performance-Analyse)
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$projectDir/compose-metrics",
                "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$projectDir/compose-reports"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true  // Aktiviere BuildConfig für API Keys
    }

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
            // Google Drive API - INDEX.LIST Konflikt beheben
            excludes += "META-INF/INDEX.LIST"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        // Lint-Bug in Kotlin 2.2.20 + Lifecycle 2.10.0 workaround
        checkReleaseBuilds = false  // Deaktiviere Lint für Release-Builds
        abortOnError = false
    }
}

// Detekt Konfiguration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    baseline = file("$projectDir/config/detekt/baseline.xml")
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose & Material 3 (via BOM & Bundles)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Room Database (via Bundle)
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // ViewModel & Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt Dependency Injection (via Bundle)
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Apache POI (Excel)
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Google Play Services - Geofencing
    implementation(libs.play.services.location)

    // Google Drive API & Credentials (NEU: Credential Manager)
    implementation(libs.bundles.credentials)  // Neue Credential Manager API
    implementation(libs.play.services.auth)  // Noch benötigt für Drive API
    implementation(libs.bundles.google.api) {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // OpenStreetMap (OSMDroid)
    implementation(libs.osmdroid.android)

    // Google Plus Codes (Open Location Code)
    implementation(libs.openlocationcode)

    // Ktor Client für Gemini API
    implementation(libs.bundles.ktor)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Testing - Unit Tests
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)

    // Hilt Testing
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // Baseline Profile - wird automatisch generiert bei Release Builds
}
