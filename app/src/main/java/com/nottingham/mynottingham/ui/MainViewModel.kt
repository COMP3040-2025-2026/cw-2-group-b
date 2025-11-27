package com.nottingham.mynottingham.ui

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.MyNottinghamApplication
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.repository.FirebaseErrandRepository
import com.nottingham.mynottingham.data.repository.FirebaseMessageRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val messageRepository = FirebaseMessageRepository()
    private val errandRepository = FirebaseErrandRepository()

    // Cache to store the last known status of errands to detect changes
    // Key: ErrandID, Value: Status
    private val errandStatusCache = mutableMapOf<String, String>()

    private val _unreadMessageCount = MutableLiveData<Int>()
    val unreadMessageCount: LiveData<Int> = _unreadMessageCount

    fun startListeningToUnreadCount(userId: String) {
        viewModelScope.launch {
            messageRepository.getTotalUnreadCountFlow(userId).collect { count ->
                _unreadMessageCount.postValue(count)
            }
        }

        // Start monitoring errand status changes
        startMonitoringErrands(userId)
    }

    /**
     * Monitor errand status changes for both Requester and Runner roles
     */
    private fun startMonitoringErrands(userId: String) {
        // 1. Monitor tasks requested by me (As Requester)
        // I want to know when someone accepts or delivers my order
        viewModelScope.launch {
            errandRepository.getUserRequestedErrands(userId).collect { errands ->
                processErrandUpdates(errands, isRequester = true)
            }
        }

        // 2. Monitor tasks provided by me (As Runner/Provider)
        // I want to know if the requester cancels the order I am delivering
        viewModelScope.launch {
            errandRepository.getUserProvidedErrands(userId).collect { errands ->
                processErrandUpdates(errands, isRequester = false)
            }
        }
    }

    /**
     * Process updates and trigger notifications if status changes
     */
    private fun processErrandUpdates(errands: List<Map<String, Any>>, isRequester: Boolean) {
        for (errand in errands) {
            val id = errand["id"] as? String ?: continue
            val newStatus = errand["status"] as? String ?: continue
            val title = errand["title"] as? String ?: "Errand"

            // If not in cache, it's initial load or new item. Just cache it without notification.
            if (!errandStatusCache.containsKey(id)) {
                errandStatusCache[id] = newStatus
                continue
            }

            val oldStatus = errandStatusCache[id]

            // If status changed
            if (oldStatus != newStatus) {
                if (isRequester) {
                    // Handle notifications for the person who created the order
                    handleRequesterNotification(title, newStatus)
                } else {
                    // Handle notifications for the runner delivering the order
                    handleRunnerNotification(title, newStatus)
                }
                // Update cache
                errandStatusCache[id] = newStatus
            }
        }
    }

    /**
     * Notifications for Requester (发布者收到的通知)
     */
    private fun handleRequesterNotification(title: String, newStatus: String) {
        val (notificationTitle, notificationBody) = when (newStatus) {
            "ACCEPTED" -> "Order Accepted" to "Your order '$title' has been accepted by a runner."
            "DELIVERING" -> "Delivery Started" to "Your order '$title' is on the way."
            "COMPLETED" -> "Order Completed" to "Your order '$title' has been completed. Please confirm receipt."
            "CANCELLED" -> "Order Cancelled" to "Your order '$title' has been cancelled."
            else -> return // Ignore other status changes
        }

        showLocalNotification(notificationTitle, notificationBody)
    }

    /**
     * Notifications for Runner (送货人收到的通知)
     * Runners generally don't need notifications for actions they perform themselves (like accepting),
     * but they need to know if the Requester cancels the order.
     */
    private fun handleRunnerNotification(title: String, newStatus: String) {
        val (notificationTitle, notificationBody) = when (newStatus) {
            "CANCELLED" -> "Order Cancelled" to "The order '$title' has been cancelled by the requester."
            else -> return // Don't notify for actions the runner performed themselves (Accepted/Delivering/Completed)
        }

        showLocalNotification(notificationTitle, notificationBody)
    }

    /**
     * Show a local notification in the system tray
     */
    private fun showLocalNotification(title: String, body: String) {
        val context = getApplication<Application>()

        // Open MainActivity when tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(), // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MyNottinghamApplication.CHANNEL_ERRANDS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // Expandable text
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use a unique ID based on time to show multiple notifications
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}