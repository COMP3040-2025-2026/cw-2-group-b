package com.nottingham.mynottingham.ui.errand

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.nottingham.mynottingham.databinding.ItemMyTaskBinding

data class MyTask(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val reward: Double,
    val requesterId: String,
    val providerId: String?,
    val requesterName: String,
    val providerName: String?,
    val requesterAvatar: String?,
    val location: String,
    val deadline: String,
    val createdAt: Long
)

class MyTasksAdapter(
    private val onItemClicked: (MyTask) -> Unit
) : ListAdapter<MyTask, MyTasksAdapter.MyTaskViewHolder>(MyTaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyTaskViewHolder {
        val binding = ItemMyTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyTaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
        holder.itemView.setOnClickListener {
            onItemClicked(task)
        }
    }

    class MyTaskViewHolder(private val binding: ItemMyTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: MyTask) {
            binding.tvTitle.text = task.title
            binding.tvStatus.text = task.status.replace("_", " ")
            binding.tvReward.text = String.format("RM%.2f", task.reward)
            binding.tvDescription.text = task.description
            binding.tvLocation.text = task.location
            binding.tvDeadline.text = task.deadline

            // Update provider info
            if (!task.providerName.isNullOrEmpty()) {
                binding.tvAdditionalInfo.text = "Accepted by ${task.providerName}"
                binding.tvAdditionalInfo.visibility = android.view.View.VISIBLE
            } else {
                binding.tvAdditionalInfo.visibility = android.view.View.GONE
            }

            // Update status badge color and text color
            val context = binding.root.context
            val statusColorResId = when (task.status) {
                "PENDING" -> com.nottingham.mynottingham.R.color.errand_pending
                "IN_PROGRESS" -> com.nottingham.mynottingham.R.color.errand_accepted
                "COMPLETED" -> com.nottingham.mynottingham.R.color.errand_completed
                else -> android.R.color.darker_gray
            }
            binding.tvStatus.backgroundTintList =
                androidx.core.content.ContextCompat.getColorStateList(context, statusColorResId)

            val textColorResId = when (task.status) {
                "PENDING" -> com.nottingham.mynottingham.R.color.black // Darker text for lighter background
                "IN_PROGRESS", "COMPLETED" -> com.nottingham.mynottingham.R.color.white // White text for darker backgrounds
                else -> com.nottingham.mynottingham.R.color.white
            }
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, textColorResId))
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
