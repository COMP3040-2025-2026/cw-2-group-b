plugins {
    // The Android Application plugin, required for building an Android app.
    alias(libs.plugins.android.application)
    // The Kotlin Android plugin, enabling Kotlin support for Android.
    alias(libs.plugins.kotlin.android)
    // Enables the Kotlin automatic Parcelable implementation generator.
    id("kotlin-parcelize")
    // Kotlin Symbol Processing API, used for code generation in libraries like Room and Glide.
    alias(libs.plugins.ksp)
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

    // DATABASE (ROOM) //
    // Room is a persistence library providing an abstraction layer over SQLite.
    implementation(libs.androidx.room.runtime)
    // Kotlin extensions and coroutine support for Room.
    implementation(libs.androidx.room.ktx)
    // Room's annotation processor (code generator).
    ksp(libs.androidx.room.compiler)

    // NETWORKING (RETROFIT & OKHTTP) //
    // A type-safe HTTP client for Android and Java.
    implementation(libs.retrofit)
    // A converter which uses Gson for JSON serialization/deserialization.
    implementation(libs.retrofit.converter.gson)
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
    ksp(libs.glide.compiler)

    // DATA PERSISTENCE (DATASTORE) //
    // A data storage solution that allows you to store key-value pairs or typed objects with protocol buffers.
    implementation(libs.androidx.datastore.preferences)

    // TESTING //
    // Standard unit testing framework.
    testImplementation(libs.junit)
    // AndroidX Test libraries for UI and integration testing.
    androidTestImplementation(libs.androidx.junit)
    // Framework for writing and running UI tests.
    androidTestImplementation(libs.androidx.espresso.core)
    // Provides testing support for the Navigation component.
    androidTestImplementation(libs.androidx.navigation.testing)
}
