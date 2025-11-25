package com.nottingham.mynottingham.ui.errand

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.ItemMyTaskBinding

data class MyTask(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val reward: Double,
    val location: String,
    val requesterId: String,
    val providerId: String?,
    val requesterName: String,
    val providerName: String?,
    val requesterAvatar: String?,
    val providerAvatar: String? = null,
    val deadline: String,
    val createdAt: Long
)

class MyTasksAdapter(
    private val onItemClicked: (MyTask) -> Unit,
    private val onDeleteClicked: ((MyTask) -> Unit)? = null
) : ListAdapter<MyTask, MyTasksAdapter.MyTaskViewHolder>(MyTaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyTaskViewHolder {
        val binding = ItemMyTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyTaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task, onItemClicked, onDeleteClicked)
    }

    class MyTaskViewHolder(private val binding: ItemMyTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: MyTask, onItemClicked: (MyTask) -> Unit, onDeleteClicked: ((MyTask) -> Unit)?) {
            val context = binding.root.context

            binding.tvTitle.text = task.title
            binding.tvReward.text = String.format("RM%.2f", task.reward)
            binding.tvDescription.text = task.description

            // Status badge - display formatted status and set colors
            val (statusText, textColor, bgColor) = when (task.status.uppercase()) {
                "PENDING" -> Triple("Pending", R.color.errand_pending, R.color.errand_pending_bg)
                "ACCEPTED", "IN_PROGRESS" -> Triple("Accepted", R.color.errand_accepted, R.color.errand_accepted_bg)
                "DELIVERING" -> Triple("Delivering", R.color.errand_delivering, R.color.errand_delivering_bg)
                "COMPLETED" -> Triple("Completed", R.color.errand_completed, R.color.errand_completed_bg)
                "CANCELLED" -> Triple("Cancelled", R.color.errand_cancelled, R.color.errand_cancelled_bg)
                else -> Triple(task.status.replace("_", " "), R.color.text_secondary, R.color.surface_darker)
            }

            binding.tvStatus.text = statusText
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, textColor))

            // Update status badge background color
            val bgDrawable = binding.tvStatus.background?.mutate()
            if (bgDrawable is GradientDrawable) {
                bgDrawable.setColor(ContextCompat.getColor(context, bgColor))
            }

            // Provider info - only show if there's actually a provider
            if (!task.providerId.isNullOrEmpty() && !task.providerName.isNullOrEmpty()) {
                binding.tvAdditionalInfo.visibility = View.VISIBLE
                binding.tvAdditionalInfo.text = "Accepted by ${task.providerName}"
            } else {
                binding.tvAdditionalInfo.visibility = View.GONE
            }

            // Action button visibility based on status
            when (task.status.uppercase()) {
                "COMPLETED", "CANCELLED" -> {
                    // For history items, show Delete button if callback is provided
                    if (onDeleteClicked != null) {
                        binding.btnAction.visibility = View.VISIBLE
                        binding.btnAction.text = "Delete"
                        binding.btnAction.setTextColor(ContextCompat.getColor(context, R.color.error))
                        binding.btnAction.setOnClickListener { onDeleteClicked(task) }
                    } else {
                        binding.btnAction.visibility = View.GONE
                    }
                }
                else -> {
                    binding.btnAction.visibility = View.VISIBLE
                    binding.btnAction.text = "View Details"
                    binding.btnAction.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    binding.btnAction.setOnClickListener { onItemClicked(task) }
                }
            }

            // Click listeners - card should navigate to details
            itemView.setOnClickListener { onItemClicked(task) }
        }
    }
}

class MyTaskDiffCallback : DiffUtil.ItemCallback<MyTask>() {
    override fun areItemsTheSame(oldItem: MyTask, newItem: MyTask): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MyTask, newItem: MyTask): Boolean {
        return oldItem == newItem
    }
}
