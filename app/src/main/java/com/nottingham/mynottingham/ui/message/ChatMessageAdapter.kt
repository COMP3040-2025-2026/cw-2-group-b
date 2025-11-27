package com.nottingham.mynottingham.ui.message

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.ui.common.ImageViewerDialog
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

        // Reuse SimpleDateFormat instance to avoid creating new objects on every bind
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
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
                tvTimestamp.text = formatTimestamp(message.timestamp)
                ivAvatar.setImageResource(AvatarUtils.getDrawableId(message.senderAvatar))

                // Handle image messages
                if (message.messageType == "IMAGE" && !message.imageUrl.isNullOrEmpty()) {
                    Log.d("ChatMessageAdapter", "Loading sent image: ${message.imageUrl}")
                    ivMessageImage.visibility = View.VISIBLE
                    tvMessage.visibility = View.GONE
                    Glide.with(itemView.context)
                        .load(message.imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(ivMessageImage)

                    // Click to view full image
                    ivMessageImage.setOnClickListener {
                        ImageViewerDialog(itemView.context, message.imageUrl).show()
                    }
                } else {
                    ivMessageImage.visibility = View.GONE
                    ivMessageImage.setOnClickListener(null)
                    tvMessage.visibility = View.VISIBLE
                    tvMessage.text = if (message.message.isNotBlank()) message.message.trim() else "[Image]"
                }
            }
        }
    }

    // ViewHolder for received messages
    inner class ReceivedMessageViewHolder(
        private val binding: ItemChatMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                tvTimestamp.text = formatTimestamp(message.timestamp)
                ivAvatar.setImageResource(AvatarUtils.getDrawableId(message.senderAvatar))

                // Handle image messages
                if (message.messageType == "IMAGE" && !message.imageUrl.isNullOrEmpty()) {
                    Log.d("ChatMessageAdapter", "Loading received image: ${message.imageUrl}")
                    ivMessageImage.visibility = View.VISIBLE
                    tvMessage.visibility = View.GONE
                    Glide.with(itemView.context)
                        .load(message.imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(ivMessageImage)

                    // Click to view full image
                    ivMessageImage.setOnClickListener {
                        ImageViewerDialog(itemView.context, message.imageUrl).show()
                    }
                } else {
                    ivMessageImage.visibility = View.GONE
                    ivMessageImage.setOnClickListener(null)
                    tvMessage.visibility = View.VISIBLE
                    tvMessage.text = if (message.message.isNotBlank()) message.message.trim() else "?"
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return timeFormatter.format(Date(timestamp))
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
