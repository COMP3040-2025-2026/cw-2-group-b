import java.util.Properties

plugins {
    // The Android Application plugin, required for building an Android app.
    alias(libs.plugins.android.application)
    // The Kotlin Android plugin, enabling Kotlin support for Android.
    alias(libs.plugins.kotlin.android)
    // Enables the Kotlin automatic Parcelable implementation generator.
    id("kotlin-parcelize")
    alias(libs.plugins.navigation.safeargs)
    // Google Services plugin for Firebase
    alias(libs.plugins.google.services)
}

// Load API keys from local.properties
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    // Defines the application's package name for resource access.
    namespace = "com.nottingham.mynottingham"
    // Specifies the API level to compile the app against.
    compileSdk = 34

    defaultConfig {
        // A unique identifier for the app on the device and in the Play Store.
        applicationId = "com.nottingham.mynottingham"
        // The minimum API level required to run the app.
        minSdk = 30
        // The API level the app is designed and tested for.
        targetSdk = 34
        // The version code, an integer that increases with each release.
        versionCode = 1
        // The user-facing version name.
        versionName = "1.0"

        // The test runner for Android instrumented tests.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Claude API Key from local.properties
        buildConfigField("String", "CLAUDE_API_KEY", "\"${localProperties.getProperty("CLAUDE_API_KEY", "")}\"")
    }

    buildTypes {
        // Configuration for the release build.
        release {
            // Disables code shrinking, obfuscation, and optimization for this build.
            isMinifyEnabled = false
            // Specifies the ProGuard rules files for code shrinking.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // Configures Java language compatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Configures Kotlin compiler options.
    kotlinOptions {
        jvmTarget = "17"
    }
    // Enables modern Android development features.
    buildFeatures {
        // Enables View Binding for safer and more concise view access.
        viewBinding = true
        // Enable BuildConfig generation
        buildConfig = true
    }
}

dependencies {
    // CORE ANDROIDX LIBRARIES //
    // Core Kotlin extensions for Android.
    implementation(libs.androidx.core.ktx)
    // Provides backward compatibility for newer Android features.
    implementation(libs.androidx.appcompat)
    // Google's Material Design components for UI.
    implementation(libs.material)
    // Provides the Activity base class and utilities.
    implementation(libs.androidx.activity)
    // A flexible layout manager for creating responsive UIs.
    implementation(libs.androidx.constraintlayout)

    // LIFECYCLE MANAGEMENT //
    // ViewModel and LiveData support with Kotlin extensions.
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    // Provides lifecycle-aware coroutine scopes.
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // NAVIGATION COMPONENT //
    // Provides support for building single-activity apps with navigation.
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // NETWORKING (RETROFIT & OKHTTP) //
    // A type-safe HTTP client for Android and Java.
    implementation(libs.retrofit)
    // A converter which uses Gson for JSON serialization/deserialization.
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    // An OkHttp interceptor which logs HTTP request and response data.
    implementation(libs.okhttp.logging.interceptor)

    // JSON SERIALIZATION //
    // A Java library to convert Java Objects into their JSON representation and vice versa.
    implementation(libs.gson)

    // ASYNCHRONOUS PROGRAMMING (COROUTINES) //
    // Coroutine support for the Android UI thread.
    implementation(libs.kotlinx.coroutines.android)
    // Core library for Kotlin coroutines.
    implementation(libs.kotlinx.coroutines.core)

    // IMAGE LOADING (GLIDE) //
    // A fast and efficient image loading library for Android.
    implementation(libs.glide)
    // Glide's annotation processor for performance optimization.
    annotationProcessor(libs.glide.compiler)
    // CircleImageView for circular image views.
    implementation(libs.circleimageview)

    // DATA PERSISTENCE (DATASTORE) //
    // A data storage solution that allows you to store key-value pairs or typed objects with protocol buffers.
    implementation(libs.androidx.datastore.preferences)

    // FIREBASE //
    // Firebase BOM - manages all Firebase library versions
    implementation(platform(libs.firebase.bom))
    // Firebase Authentication for user identity management
    implementation(libs.firebase.auth)
    // Firebase Realtime Database with Kotlin extensions
    implementation(libs.firebase.database)
    // Firebase Analytics (optional but recommended)
    implementation(libs.firebase.analytics)
    // Firebase Cloud Messaging for push notifications
    implementation(libs.firebase.messaging)
    // Note: Notti AI uses Claude API via OkHttp (already included)

    // TESTING //
    // Standard unit testing framework.
    testImplementation(libs.junit)
    // AndroidX Architecture Components testing for InstantTaskExecutorRule.
    testImplementation(libs.androidx.arch.core.testing)
    // Kotlin Coroutines testing utilities.
    testImplementation(libs.kotlinx.coroutines.test)
    // AndroidX Test libraries for UI and integration testing.
    androidTestImplementation(libs.androidx.junit)
    // Framework for writing and running UI tests.
    androidTestImplementation(libs.androidx.espresso.core)
    // Provides testing support for the Navigation component.
    androidTestImplementation(libs.androidx.navigation.testing)
    // AndroidX Test Core and Rules for UI testing.
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    // Fragment testing utilities.
    debugImplementation(libs.androidx.fragment.testing)
}
