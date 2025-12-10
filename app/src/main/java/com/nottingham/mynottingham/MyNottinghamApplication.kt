package com.nottingham.mynottingham

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.nottingham.mynottingham.data.repository.FirebaseErrandRepository
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

    companion object {
        // Notification Channel IDs
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_ERRANDS = "errands"
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

        // Create notification channels for Android 8.0+
        createNotificationChannels()

        // Trigger the cleanup of old errand history (7 days)
        cleanupOldErrands()
    }

    /**
     * Creates notification channels for Android 8.0 (API 26) and above.
     * Channels are required for showing notifications on these versions.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Messages channel - for chat messages
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages"
                enableVibration(true)
                enableLights(true)
            }

            // Errands channel - for order status updates
            val errandsChannel = NotificationChannel(
                CHANNEL_ERRANDS,
                "Errand Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for errand order updates"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, errandsChannel)
            )

            android.util.Log.d("MyNottinghamApp", "Notification channels created")
        }
    }

    /**
     * Initiates a background task to delete old errand history (completed/cancelled tasks older than 7 days).
     * This helps keep the Firebase database clean and saves storage.
     */
    private fun cleanupOldErrands() {
        applicationScope.launch {
            try {
                val repository = FirebaseErrandRepository()
                val result = repository.cleanupOldErrands(daysOld = 7)

                result.onSuccess { deletedCount ->
                    android.util.Log.d("MyNottinghamApp", "Successfully cleaned up $deletedCount old errands")
                }
            } catch (e: Exception) {
                android.util.Log.e("MyNottinghamApp", "Failed to cleanup old errands", e)
            }
        }
    }
}
