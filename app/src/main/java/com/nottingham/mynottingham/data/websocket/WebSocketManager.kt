package com.nottingham.mynottingham.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * WebSocket manager for real-time messaging
 */
class WebSocketManager(private val userId: String) {

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    // SharedFlow for broadcasting WebSocket messages
    private val _messageFlow = MutableSharedFlow<WebSocketMessage>(replay = 0)
    val messageFlow: SharedFlow<WebSocketMessage> = _messageFlow

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS) // Keep-alive
        .build()

    /**
     * Connect to WebSocket
     */
    fun connect() {
        if (webSocket != null && webSocket?.isConnected() == true) {
            Log.d(TAG, "WebSocket already connected")
            return
        }

        val wsUrl = "${Constants.WS_BASE_URL}?userId=$userId"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                try {
                    val message = gson.fromJson(text, WebSocketMessage::class.java)
                    // Emit to flow
                    CoroutineScope(Dispatchers.IO).launch {
                        _messageFlow.emit(message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                // Attempt to reconnect after delay
                CoroutineScope(Dispatchers.IO).launch {
                    delay(5000) // Wait 5 seconds
                    connect() // Reconnect
                }
            }
        })
    }

    /**
     * Disconnect WebSocket
     */
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        Log.d(TAG, "WebSocket disconnected")
    }

    /**
     * Join a conversation to receive real-time updates
     */
    fun joinConversation(conversationId: String) {
        val message = WebSocketMessage(
            type = "JOIN_CONVERSATION",
            message = "Joining conversation",
            data = mapOf("conversationId" to conversationId)
        )
        sendMessage(message)
    }

    /**
     * Leave a conversation
     */
    fun leaveConversation(conversationId: String) {
        val message = WebSocketMessage(
            type = "LEAVE_CONVERSATION",
            message = "Leaving conversation",
            data = mapOf("conversationId" to conversationId)
        )
        sendMessage(message)
    }

    /**
     * Send typing indicator
     */
    fun sendTyping(conversationId: String, senderId: String, senderName: String) {
        val message = WebSocketMessage(
            type = "TYPING",
            message = "User is typing",
            data = mapOf(
                "conversationId" to conversationId,
                "senderId" to senderId,
                "senderName" to senderName
            )
        )
        sendMessage(message)
    }

    /**
     * Send stop typing indicator
     */
    fun sendStopTyping(conversationId: String, senderId: String) {
        val message = WebSocketMessage(
            type = "STOP_TYPING",
            message = "User stopped typing",
            data = mapOf(
                "conversationId" to conversationId,
                "senderId" to senderId
            )
        )
        sendMessage(message)
    }

    /**
     * Send message read notification
     */
    fun sendMessageRead(conversationId: String, userId: String) {
        val message = WebSocketMessage(
            type = "MESSAGE_READ",
            message = "Message read",
            data = mapOf(
                "conversationId" to conversationId,
                "userId" to userId
            )
        )
        sendMessage(message)
    }

    /**
     * Send WebSocket message
     */
    private fun sendMessage(message: WebSocketMessage) {
        val json = gson.toJson(message)
        val sent = webSocket?.send(json) ?: false
        if (!sent) {
            Log.e(TAG, "Failed to send WebSocket message")
        }
    }

    /**
     * Check if WebSocket is connected
     */
    private fun WebSocket.isConnected(): Boolean {
        return try {
            // Try to send empty string to check connection
            this.send("")
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "WebSocketManager"

        // Singleton instance
        private var instance: WebSocketManager? = null

        fun getInstance(userId: String): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager(userId).also { instance = it }
            }
        }

        fun destroyInstance() {
            instance?.disconnect()
            instance = null
        }
    }
}

/**
 * WebSocket message data class
 */
data class WebSocketMessage(
    val type: String,
    val message: String,
    val data: Map<String, Any>?
)
