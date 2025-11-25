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
        private const val MAX_MESSAGES = 100 // 限制存储的最大消息数
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * 保存聊天记录
     * @param userId 用户ID
     * @param messages 消息列表
     */
    fun saveChatHistory(userId: String, messages: List<NottiMessage>) {
        if (userId.isBlank()) return

        // 只保存最近的消息，避免存储过多数据
        val messagesToSave = messages.takeLast(MAX_MESSAGES)

        // 过滤掉 loading 状态的消息
        val filteredMessages = messagesToSave.filter { !it.isLoading }

        val json = gson.toJson(filteredMessages)
        prefs.edit().putString("$KEY_PREFIX$userId", json).apply()
    }

    /**
     * 加载聊天记录
     * @param userId 用户ID
     * @return 消息列表，如果没有记录则返回空列表
     */
    fun loadChatHistory(userId: String): List<NottiMessage> {
        if (userId.isBlank()) return emptyList()

        val json = prefs.getString("$KEY_PREFIX$userId", null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<NottiMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            // 解析失败时返回空列表
            emptyList()
        }
    }

    /**
     * 清除指定用户的聊天记录
     * @param userId 用户ID
     */
    fun clearChatHistory(userId: String) {
        if (userId.isBlank()) return
        prefs.edit().remove("$KEY_PREFIX$userId").apply()
    }

    /**
     * 清除所有用户的聊天记录
     */
    fun clearAllHistory() {
        prefs.edit().clear().apply()
    }
}
