package com.nottingham.mynottingham

import android.app.Application
import com.nottingham.mynottingham.data.local.database.AppDatabase

/**
 * Application class for My Nottingham
 * Initializes app-wide components
 */
class MyNottinghamApplication : Application() {

    // Database instance
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize third-party libraries here if needed
        // Example: Timber for logging, Crashlytics, etc.
    }
}
