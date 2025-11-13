package com.nottingham.mynottingham.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.Conversation
import com.nottingham.mynottingham.databinding.ItemConversationBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for conversation list
 */
class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit,
    private val onConversationLongClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder>(ConversationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConversationViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onConversationClick(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onConversationLongClick(getItem(position))
                    true
                } else {
                    false
                }
            }
        }

        fun bind(conversation: Conversation) {
            binding.apply {
                // Set avatar initials
                val initials = conversation.participantName.split(" ")
                    .take(2)
                    .map { it.firstOrNull()?.uppercase() ?: "" }
                    .joinToString("")
                tvAvatar.text = initials

                // Show/hide online indicator
                viewOnline.visibility = if (conversation.isOnline) View.VISIBLE else View.GONE

                // Set name
                tvName.text = conversation.participantName

                // Set last message
                tvLastMessage.text = conversation.lastMessage

                // Set timestamp
                tvTime.text = formatTimestamp(conversation.lastMessageTime)

                // Show/hide unread badge
                if (conversation.unreadCount > 0) {
                    tvUnreadCount.visibility = View.VISIBLE
                    tvUnreadCount.text = if (conversation.unreadCount > 99) {
                        "99+"
                    } else {
                        conversation.unreadCount.toString()
                    }
                } else {
                    tvUnreadCount.visibility = View.GONE
                }

                // Show pin indicator (you may want to add this to the layout)
                // For now, we can use a subtle background or text style change
                root.alpha = if (conversation.isPinned) 1.0f else 0.9f
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "Just now" // Less than 1 minute
                diff < 3600_000 -> "${diff / 60_000}m ago" // Less than 1 hour
                diff < 86400_000 -> { // Less than 24 hours
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
                diff < 172800_000 -> "Yesterday" // Less than 2 days
                diff < 604800_000 -> "${diff / 86400_000} days ago" // Less than 7 days
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}

class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
    override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
        return oldItem == newItem
    }
}
