package com.nottingham.mynottingham.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
        private const val DATABASE_URL = "https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app"

        /**
         * Save FCM token to Firebase for current user
         * Call this after user login
         */
        fun saveTokenToFirebase(token: String) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val database = FirebaseDatabase.getInstance(DATABASE_URL)
            database.getReference("users").child(userId).child("fcmToken").setValue(token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token saved to Firebase for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save FCM token: ${e.message}")
                }
        }

        /**
         * Remove FCM token from Firebase (call on logout)
         */
        fun removeTokenFromFirebase() {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val database = FirebaseDatabase.getInstance(DATABASE_URL)
            database.getReference("users").child(userId).child("fcmToken").removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token removed for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to remove FCM token: ${e.message}")
                }
        }
    }

    /**
     * Called when a new FCM token is generated.
     * This happens on first app launch and when the token is refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: $token")
        // Save token to Firebase if user is logged in
        saveTokenToFirebase(token)
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
     * Routes to appropriate notification handler based on message type
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: "message"

        when (type) {
            "message" -> {
                // Extract chat-specific fields
                val conversationId = data["conversationId"]
                val senderId = data["senderId"] ?: ""
                val senderName = data["senderName"] ?: data["title"] ?: "New Message"
                val messageContent = data["body"] ?: "You have a new message"

                if (!conversationId.isNullOrEmpty()) {
                    showChatNotification(senderName, messageContent, conversationId, senderId)
                } else {
                    // Fallback to generic notification if conversationId is missing
                    showNotification(senderName, messageContent, MyNottinghamApplication.CHANNEL_MESSAGES)
                }
            }
            "errand" -> {
                val title = data["title"] ?: "Errand Update"
                val body = data["body"] ?: ""
                showNotification(title, body, MyNottinghamApplication.CHANNEL_ERRANDS)
            }
            else -> {
                val title = data["title"] ?: "My Nottingham"
                val body = data["body"] ?: ""
                showNotification(title, body, MyNottinghamApplication.CHANNEL_MESSAGES)
            }
        }
    }

    /**
     * Display WhatsApp-style chat notification
     * Shows sender name as title and message content as body
     * Clicking navigates directly to ChatDetailFragment with proper back stack
     */
    private fun showChatNotification(
        senderName: String,
        messageContent: String,
        conversationId: String,
        senderId: String
    ) {
        // Use NavDeepLinkBuilder for proper back stack handling
        // Back stack: ChatDetailFragment -> MessageFragment -> HomeFragment
        val pendingIntent = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.chatDetailFragment)
            .setArguments(Bundle().apply {
                putString("conversationId", conversationId)
                putString("participantName", senderName)
                putString("participantId", senderId)
                // participantAvatar will use default value from nav_graph
            })
            .createPendingIntent()

        val notification = NotificationCompat.Builder(this, MyNottinghamApplication.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_message)
            .setContentTitle(senderName)
            .setContentText(messageContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        // Use conversationId hashCode as notification ID
        // Same conversation updates existing notification instead of stacking
        notificationManager.notify(conversationId.hashCode(), notification)

        Log.d(TAG, "Chat notification displayed: $senderName - $messageContent")
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
