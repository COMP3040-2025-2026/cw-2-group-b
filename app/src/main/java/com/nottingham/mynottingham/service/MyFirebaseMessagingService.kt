package com.nottingham.mynottingham.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nottingham.mynottingham.MyNottinghamApplication
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.ui.MainActivity

/**
 * Firebase Cloud Messaging Service
 *
 * Handles incoming FCM messages and displays notifications when the app is in background.
 * Also handles FCM token refresh for push notification registration.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    /**
     * Called when a new FCM token is generated.
     * This happens on first app launch and when the token is refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: $token")
        // TODO: Send token to your server if you need server-side push
        // For now, Firebase handles everything client-side
    }

    /**
     * Called when a message is received from FCM.
     * This is called when the app is in the foreground.
     * For background messages with notification payload, the system handles it automatically.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification - Title: ${notification.title}, Body: ${notification.body}")
            showNotification(
                title = notification.title ?: "My Nottingham",
                body = notification.body ?: "",
                channelId = MyNottinghamApplication.CHANNEL_MESSAGES
            )
        }

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
    }

    /**
     * Handle data messages (custom notifications)
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: "message"
        val title = data["title"] ?: "My Nottingham"
        val body = data["body"] ?: ""

        val channelId = when (type) {
            "message" -> MyNottinghamApplication.CHANNEL_MESSAGES
            "errand" -> MyNottinghamApplication.CHANNEL_ERRANDS
            else -> MyNottinghamApplication.CHANNEL_MESSAGES
        }

        showNotification(title, body, channelId)
    }

    /**
     * Display a notification to the user
     */
    private fun showNotification(title: String, body: String, channelId: String) {
        // Create intent to open the app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show the notification
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)

        Log.d(TAG, "Notification displayed: $title")
    }
}
