plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        
        // Backend URL configuration
        buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:3000/\"")
        
        // Certificate pins (empty for debug, required for release)
        buildConfigField("String", "CERT_PIN_1", "\"\"")
        buildConfigField("String", "CERT_PIN_2", "\"\"")
    }

    buildTypes {
        debug {
            // Development backend (emulator)
            buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:3000/\"")
            // No certificate pinning in debug
            buildConfigField("String", "CERT_PIN_1", "\"\"")
            buildConfigField("String", "CERT_PIN_2", "\"\"")
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
            val localProperties = java.util.Properties()
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
            
            // CRITICAL: Fail build if certificate pins not provided
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
            
            buildConfigField("String", "CERT_PIN_1", "\"$certPin1\"")
            buildConfigField("String", "CERT_PIN_2", "\"$certPin2\"")
            
            println("✓ Certificate pins configured for production build")
            
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
    
    // Plaid Link SDK
    implementation("com.plaid.link:sdk-core:4.2.0")
    
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    // Room compiler (kapt)
    // Note: For annotation processing, add to plugins: id("kotlin-kapt")
    // Then uncomment: kapt("androidx.room:room-compiler:2.6.1")
    
    // Security - Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // MFA - Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // MFA - Credential Manager (FIDO2, Passkeys) - Android 14+
    implementation("androidx.credentials:credentials:1.3.0-alpha01")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0-alpha01")
    
    // Pull to Refresh
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    // Compose Testing
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

