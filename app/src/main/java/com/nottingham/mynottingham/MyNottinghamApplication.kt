package com.nottingham.mynottingham

import android.app.Application
import com.nottingham.mynottingham.data.local.database.AppDatabase
import com.nottingham.mynottingham.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for My Nottingham
 * Initializes app-wide components
 */
class MyNottinghamApplication : Application() {

    // Database instance
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    // Application scope for background operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Initialize third-party libraries here if needed
        // Example: Timber for logging, Crashlytics, etc.

        // Clean up old messages (7 days retention policy)
        cleanupOldMessages()
    }

    /**
     * Clean up messages older than 7 days
     * Runs in background on app startup
     */
    private fun cleanupOldMessages() {
        applicationScope.launch {
            try {
                val repository = MessageRepository(this@MyNottinghamApplication)
                val result = repository.cleanupOldMessages()
                result.onSuccess { deletedCount ->
                    // Log deleted message count if needed
                    android.util.Log.d("MyNottinghamApp", "Cleaned up $deletedCount old messages")
                }
            } catch (e: Exception) {
                // Silently fail - not critical for app operation
                android.util.Log.e("MyNottinghamApp", "Failed to cleanup old messages", e)
            }
        }
    }
}
