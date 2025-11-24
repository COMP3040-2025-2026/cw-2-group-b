package com.nottingham.mynottingham

import android.app.Application
import com.nottingham.mynottingham.data.local.database.AppDatabase
import com.nottingham.mynottingham.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The main Application class for the MyNottingham app.
 * This class is instantiated when the application process is created and is used for
 * initializing app-wide components and maintaining global state.
 */
class MyNottinghamApplication : Application() {

    /**
     * Provides a lazily-initialized singleton instance of the application's database.
     * The database is only created when it's first accessed.
     */
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    /**
     * An application-level CoroutineScope for running background tasks.
     * This scope uses a SupervisorJob, so if one child coroutine fails, it won't cancel the others.
     * Tasks are run on the default dispatcher, which is optimized for CPU-intensive work.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Called when the application is starting, before any other components have been created.
     * Use this method for global initialization.
     */
    override fun onCreate() {
        super.onCreate()
        // You can initialize third-party libraries here if needed.
        // For example: Timber for logging, Firebase Crashlytics, etc.

        // Trigger the cleanup of old messages from the local database.
        cleanupOldMessages()
    }

    /**
     * Initiates a background task to delete messages older than a defined retention period (e.g., 7 days).
     * This helps manage storage and keeps the local database clean.
     * The operation runs in a background coroutine to avoid blocking the main thread.
     */
    private fun cleanupOldMessages() {
        applicationScope.launch {
            try {
                // Create a repository instance to handle data operations.
                val repository = MessageRepository(this@MyNottinghamApplication)
                val result = repository.cleanupOldMessages()

                // Handle the result of the cleanup operation.
                result.onSuccess { deletedCount ->
                    // Optionally, log the number of deleted messages for debugging.
                    android.util.Log.d("MyNottinghamApp", "Successfully cleaned up $deletedCount old messages")
                }
            } catch (e: Exception) {
                // The cleanup task is not critical, so we catch exceptions to prevent crashes.
                // Log the error for debugging purposes.
                android.util.Log.e("MyNottinghamApp", "Failed to cleanup old messages", e)
            }
        }
    }
}
