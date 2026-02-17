import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// Apply certificate pin extraction script
apply(from = "build-logic/get-certificate-pins.gradle.kts")

android {
    namespace = "im.ilyk.ccoptimizer"
    compileSdk = 34

    defaultConfig {
        applicationId = "im.ilyk.ccoptimizer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        // Backend URL configuration (HTTP for testing - avoids SSL issues)
        buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:12080/\"")

        // Certificate pins (empty for debug, required for release)
        buildConfigField("String", "CERT_PIN_1", "\"\"")
        buildConfigField("String", "CERT_PIN_2", "\"\"")

        // Analytics configuration
        buildConfigField("Boolean", "ANALYTICS_ENABLED", "false")
        buildConfigField("String", "ANALYTICS_PROVIDER", "\"none\"")
        buildConfigField("String", "ANALYTICS_KEY", "\"\"")

        // Crash reporting configuration
        buildConfigField("Boolean", "CRASH_REPORTING_ENABLED", "false")
        buildConfigField("String", "CRASH_REPORTING_PROVIDER", "\"none\"")
        buildConfigField("String", "SENTRY_DSN", "\"\"")
        buildConfigField("String", "BUGSNAG_API_KEY", "\"\"")
    }

    buildTypes {
        debug {
            // Development backend (HTTP for testing - no SSL issues)
            buildConfigField("String", "BACKEND_URL", "\"http://192.168.86.78:12080/\"")
            // No certificate pinning in debug
            buildConfigField("String", "CERT_PIN_1", "\"\"")
            buildConfigField("String", "CERT_PIN_2", "\"\"")
            // Disable analytics in debug
            buildConfigField("Boolean", "ANALYTICS_ENABLED", "false")
            buildConfigField("String", "ANALYTICS_PROVIDER", "\"none\"")
            buildConfigField("String", "ANALYTICS_KEY", "\"\"")
            // Disable crash reporting in debug
            buildConfigField("Boolean", "CRASH_REPORTING_ENABLED", "false")
            buildConfigField("String", "CRASH_REPORTING_PROVIDER", "\"none\"")
            buildConfigField("String", "SENTRY_DSN", "\"\"")
            buildConfigField("String", "BUGSNAG_API_KEY", "\"\"")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Production backend URL from environment or gradle.properties
            val backendUrl = findProperty("BACKEND_URL")?.toString()
                ?: System.getenv("BACKEND_URL")
                ?: "https://your-production-backend.com/"
            buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")

            // Certificate pins from local.properties (auto-generated or manual)
            val localProperties = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localProperties.load(localPropsFile.inputStream())
            }

            val certPin1 = localProperties.getProperty("CERT_PIN_1")
                ?: System.getenv("CERT_PIN_1")
                ?: ""
            val certPin2 = localProperties.getProperty("CERT_PIN_2")
                ?: System.getenv("CERT_PIN_2")
                ?: ""

            buildConfigField("String", "CERT_PIN_1", "\"$certPin1\"")
            buildConfigField("String", "CERT_PIN_2", "\"$certPin2\"")

            // Analytics configuration (can be overridden via gradle.properties or env vars)
            val analyticsEnabled = findProperty("ANALYTICS_ENABLED")?.toString()?.toBoolean() ?: false
            val analyticsProvider = findProperty("ANALYTICS_PROVIDER")?.toString() ?: "none"
            val analyticsKey = findProperty("ANALYTICS_KEY")?.toString() ?: ""
            buildConfigField("Boolean", "ANALYTICS_ENABLED", "$analyticsEnabled")
            buildConfigField("String", "ANALYTICS_PROVIDER", "\"$analyticsProvider\"")
            buildConfigField("String", "ANALYTICS_KEY", "\"$analyticsKey\"")

            // Crash reporting configuration (can be overridden via gradle.properties or env vars)
            val crashReportingEnabled = findProperty("CRASH_REPORTING_ENABLED")?.toString()?.toBoolean() ?: false
            val crashReportingProvider = findProperty("CRASH_REPORTING_PROVIDER")?.toString() ?: "none"
            val sentryDsn = findProperty("SENTRY_DSN")?.toString() ?: ""
            val bugsnagApiKey = findProperty("BUGSNAG_API_KEY")?.toString() ?: ""
            buildConfigField("Boolean", "CRASH_REPORTING_ENABLED", "$crashReportingEnabled")
            buildConfigField("String", "CRASH_REPORTING_PROVIDER", "\"$crashReportingProvider\"")
            buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
            buildConfigField("String", "BUGSNAG_API_KEY", "\"$bugsnagApiKey\"")

            // Security: Make debugging harder
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true  // Enable BuildConfig generation
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

// Validate certificate pins before building release
tasks.register("validateCertificatePins") {
    doLast {
        val localProperties = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProperties.load(localPropsFile.inputStream())
        }

        val certPin1 = localProperties.getProperty("CERT_PIN_1")
            ?: System.getenv("CERT_PIN_1")
            ?: ""
        val certPin2 = localProperties.getProperty("CERT_PIN_2")
            ?: System.getenv("CERT_PIN_2")
            ?: ""

        if (certPin1.isEmpty() || certPin2.isEmpty()) {
            throw GradleException(
                """
                ╔═══════════════════════════════════════════════════════════════╗
                ║  PRODUCTION BUILD BLOCKED - CERTIFICATE PINS REQUIRED         ║
                ╠═══════════════════════════════════════════════════════════════╣
                ║                                                               ║
                ║  Certificate pins are REQUIRED for production builds.        ║
                ║  This is a critical security measure.                        ║
                ║                                                               ║
                ║  To get certificate pins, run:                               ║
                ║    ./gradlew getCertificatePins                              ║
                ║                                                               ║
                ║  OR manually set in local.properties:                        ║
                ║    CERT_PIN_1=your_primary_pin                               ║
                ║    CERT_PIN_2=your_backup_pin                                ║
                ║                                                               ║
                ║  OR set environment variables:                               ║
                ║    export CERT_PIN_1=your_primary_pin                        ║
                ║    export CERT_PIN_2=your_backup_pin                         ║
                ║                                                               ║
                ║  To get pins manually from your server:                      ║
                ║    openssl s_client -connect your-domain.com:443 | \\        ║
                ║      openssl x509 -pubkey -noout | \\                        ║
                ║      openssl rsa -pubin -outform der | \\                    ║
                ║      openssl dgst -sha256 -binary | \\                       ║
                ║      openssl enc -base64                                     ║
                ║                                                               ║
                ╚═══════════════════════════════════════════════════════════════╝
                """.trimIndent()
            )
        }
        println("✓ Certificate pins validated for production build")
    }
}

// Make validation run before any release build tasks
tasks.whenTaskAdded {
    if (name.contains("Release") && (name.startsWith("assemble") || name.startsWith("bundle"))) {
        dependsOn("validateCertificatePins")
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Compose Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    
    // Swipe to dismiss for card actions
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2")

    // Coil for image loading in Compose
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Lottie for animations
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // Plaid Link SDK
    implementation("com.plaid.link:sdk-core:4.2.0")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // OkHttp for direct HTTP calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    // Room compiler (KSP)
    ksp("androidx.room:room-compiler:2.6.1")
    
    // SQLCipher for encrypted database
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    
    // Moshi for JSON serialization
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
    
    // Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Security - Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // MFA - Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // MFA - Credential Manager (FIDO2, Passkeys) - Android 14+
    implementation("androidx.credentials:credentials:1.3.0-alpha01")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0-alpha01")

    // Social Authentication - Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Social Authentication - Facebook Login
    implementation("com.facebook.android:facebook-login:16.1.3")

    // Pull to Refresh (deprecated but stable)
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.work:work-testing:2.9.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.48")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("org.mockito:mockito-android:5.7.0")

    // Compose Testing
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

