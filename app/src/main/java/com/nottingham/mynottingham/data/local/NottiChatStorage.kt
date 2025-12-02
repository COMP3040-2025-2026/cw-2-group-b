package com.nottingham.mynottingham.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nottingham.mynottingham.data.model.NottiMessage

/**
 * Local storage for Notti AI chat history
 * Stores chat history per user account
 */
class NottiChatStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "notti_chat_prefs"
        private const val KEY_PREFIX = "chat_history_"
        private const val MAX_MESSAGES = 100 // Limit maximum stored messages
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Save chat history
     * @param userId User ID
     * @param messages Message list
     */
    fun saveChatHistory(userId: String, messages: List<NottiMessage>) {
        if (userId.isBlank()) return

        // Only save recent messages to avoid storing too much data
        val messagesToSave = messages.takeLast(MAX_MESSAGES)

        // Filter out loading status messages
        val filteredMessages = messagesToSave.filter { !it.isLoading }

        val json = gson.toJson(filteredMessages)
        prefs.edit().putString("$KEY_PREFIX$userId", json).apply()
    }

    /**
     * Load chat history
     * @param userId User ID
     * @return Message list, returns empty list if no records
     */
    fun loadChatHistory(userId: String): List<NottiMessage> {
        if (userId.isBlank()) return emptyList()

        val json = prefs.getString("$KEY_PREFIX$userId", null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<NottiMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            // Return empty list if parsing fails
            emptyList()
        }
    }

    /**
     * Clear chat history for specific user
     * @param userId User ID
     */
    fun clearChatHistory(userId: String) {
        if (userId.isBlank()) return
        prefs.edit().remove("$KEY_PREFIX$userId").apply()
    }

    /**
     * Clear chat history for all users
     */
    fun clearAllHistory() {
        prefs.edit().clear().apply()
    }
}
