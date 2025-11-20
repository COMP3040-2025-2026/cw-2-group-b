package com.nottingham.mynottingham.ui.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.databinding.ItemChatMessageReceivedBinding
import com.nottingham.mynottingham.databinding.ItemChatMessageSentBinding
import com.nottingham.mynottingham.util.AvatarUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for chat messages
 * Supports both sent and received message layouts
 */
class ChatMessageAdapter(
    private val currentUserId: String
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemChatMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemChatMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    // ViewHolder for sent messages
    inner class SentMessageViewHolder(
        private val binding: ItemChatMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                tvMessage.text = message.message
                tvTimestamp.text = formatTimestamp(message.timestamp)
                ivAvatar.setImageResource(AvatarUtils.getDrawableId(message.senderAvatar))
            }
        }
    }

    // ViewHolder for received messages
    inner class ReceivedMessageViewHolder(
        private val binding: ItemChatMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                tvMessage.text = message.message
                tvTimestamp.text = formatTimestamp(message.timestamp)
                ivAvatar.setImageResource(AvatarUtils.getDrawableId(message.senderAvatar))
                // Optionally show sender name for group chats
                // textSenderName.text = message.senderName
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
    }
}
