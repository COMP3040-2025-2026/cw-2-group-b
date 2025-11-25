package com.nottingham.mynottingham.ui.notti

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.NottiCardItem
import com.nottingham.mynottingham.data.model.NottiMessage
import com.nottingham.mynottingham.data.model.NottiMessageType
import com.nottingham.mynottingham.databinding.ItemChatMessageAiBinding
import com.nottingham.mynottingham.databinding.ItemChatMessageUserBinding
import com.nottingham.mynottingham.databinding.ItemNottiBookingCardBinding
import com.nottingham.mynottingham.databinding.ItemNottiShuttleCardBinding

/**
 * Adapter for displaying chat messages in Notti AI Assistant
 */
class NottiMessageAdapter(
    private val onBookNowClick: (() -> Unit)? = null
) : ListAdapter<NottiMessage, RecyclerView.ViewHolder>(NottiMessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_AI = 1
        private const val VIEW_TYPE_SHUTTLE_CARD = 2
        private const val VIEW_TYPE_BOOKING_CARD = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.isFromUser -> VIEW_TYPE_USER
            message.messageType == NottiMessageType.SHUTTLE_CARD -> VIEW_TYPE_SHUTTLE_CARD
            message.messageType == NottiMessageType.BOOKING_CARD -> VIEW_TYPE_BOOKING_CARD
            else -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val binding = ItemChatMessageUserBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                UserMessageViewHolder(binding)
            }
            VIEW_TYPE_SHUTTLE_CARD -> {
                val binding = ItemNottiShuttleCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ShuttleCardViewHolder(binding)
            }
            VIEW_TYPE_BOOKING_CARD -> {
                val binding = ItemNottiBookingCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                BookingCardViewHolder(binding)
            }
            else -> {
                val binding = ItemChatMessageAiBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                AiMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AiMessageViewHolder -> holder.bind(message)
            is ShuttleCardViewHolder -> holder.bind(message)
            is BookingCardViewHolder -> holder.bind(message)
        }
    }

    inner class UserMessageViewHolder(
        private val binding: ItemChatMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: NottiMessage) {
            binding.tvMessage.text = message.content
        }
    }

    inner class AiMessageViewHolder(
        private val binding: ItemChatMessageAiBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: NottiMessage) {
            binding.tvMessage.text = message.content
            binding.progressLoading.isVisible = message.isLoading

            // 如果正在加载，显示 "Thinking..."
            if (message.isLoading) {
                binding.tvMessage.text = "Thinking..."
            }

            // 错误状态显示红色
            if (message.isError) {
                binding.tvMessage.setTextColor(
                    binding.root.context.getColor(android.R.color.holo_red_dark)
                )
            } else {
                binding.tvMessage.setTextColor(
                    binding.root.context.getColor(R.color.text_primary)
                )
            }
        }
    }

    inner class ShuttleCardViewHolder(
        private val binding: ItemNottiShuttleCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: NottiMessage) {
            val cardData = message.cardData ?: return

            binding.tvTitle.text = cardData.title
            binding.tvSubtitle.text = cardData.subtitle

            // 设置路线列表
            binding.rvRoutes.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvRoutes.adapter = RouteItemAdapter(cardData.items)
        }
    }

    inner class BookingCardViewHolder(
        private val binding: ItemNottiBookingCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: NottiMessage) {
            val cardData = message.cardData ?: return

            binding.tvTitle.text = cardData.title
            binding.tvSubtitle.text = cardData.subtitle

            // 显示预订状态
            if (cardData.items.isEmpty()) {
                binding.tvStatus.text = "✅ All time slots are available!"
                binding.tvStatus.setBackgroundColor(binding.root.context.getColor(R.color.green_light))
                binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.green_dark))
                binding.rvBookedSlots.visibility = View.GONE
            } else {
                binding.tvStatus.text = "⚠️ Some slots are booked:"
                binding.tvStatus.setBackgroundColor(binding.root.context.getColor(R.color.orange_light))
                binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.orange_dark))
                binding.rvBookedSlots.visibility = View.VISIBLE
                binding.rvBookedSlots.layoutManager = LinearLayoutManager(binding.root.context)
                binding.rvBookedSlots.adapter = BookedSlotAdapter(cardData.items)
            }

            binding.btnBookNow.setOnClickListener {
                onBookNowClick?.invoke()
            }
        }
    }

    // 路线项目适配器
    class RouteItemAdapter(
        private val items: List<NottiCardItem>
    ) : RecyclerView.Adapter<RouteItemAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvBadge: android.widget.TextView = view.findViewById(R.id.tv_route_badge)
            val tvName: android.widget.TextView = view.findViewById(R.id.tv_route_name)
            val tvTimes: android.widget.TextView = view.findViewById(R.id.tv_times)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notti_route, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvBadge.text = item.icon ?: item.label.take(2)
            holder.tvName.text = item.label
            holder.tvTimes.text = item.value

            // 根据路线设置颜色
            val color = getRouteColor(item.icon ?: "")
            holder.tvBadge.setBackgroundColor(color)
        }

        override fun getItemCount() = items.size

        private fun getRouteColor(routeId: String): Int {
            return when (routeId) {
                "A" -> 0xFF9C27B0.toInt()   // Purple
                "B" -> 0xFF2196F3.toInt()   // Blue
                "C1" -> 0xFF4CAF50.toInt()  // Green
                "C2" -> 0xFF8BC34A.toInt()  // Light Green
                "D" -> 0xFFFF9800.toInt()   // Orange
                "E1" -> 0xFF795548.toInt()  // Brown
                "E2" -> 0xFF607D8B.toInt()  // Blue Grey
                "G" -> 0xFFE91E63.toInt()   // Pink
                else -> 0xFF9E9E9E.toInt()  // Grey
            }
        }
    }

    // 已预订时段适配器
    class BookedSlotAdapter(
        private val items: List<NottiCardItem>
    ) : RecyclerView.Adapter<BookedSlotAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvSlot: android.widget.TextView = view as android.widget.TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val textView = android.widget.TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 8, 32, 8)
                textSize = 12f
                setTextColor(context.getColor(R.color.text_secondary))
            }
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvSlot.text = "❌ ${item.label}: ${item.value}"
        }

        override fun getItemCount() = items.size
    }

    class NottiMessageDiffCallback : DiffUtil.ItemCallback<NottiMessage>() {
        override fun areItemsTheSame(oldItem: NottiMessage, newItem: NottiMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NottiMessage, newItem: NottiMessage): Boolean {
            return oldItem == newItem
        }
    }
}
