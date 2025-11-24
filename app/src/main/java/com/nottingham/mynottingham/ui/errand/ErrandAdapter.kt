package com.nottingham.mynottingham.ui.errand

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.databinding.ItemErrandTaskBinding

data class ErrandTask(
    val taskId: String,
    val title: String,
    val description: String,
    val price: String,
    val location: String,
    val requesterId: String,
    val requesterName: String,
    val requesterAvatar: String,
    val deadline: String?,
    val timestamp: Long
)

class ErrandAdapter(
    private var tasks: MutableList<ErrandTask>,
    private val onItemClicked: (ErrandTask) -> Unit
) : RecyclerView.Adapter<ErrandAdapter.ErrandViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErrandViewHolder {
        val binding = ItemErrandTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ErrandViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ErrandViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
        holder.itemView.setOnClickListener {
            onItemClicked(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<ErrandTask>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    class ErrandViewHolder(private val binding: ItemErrandTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: ErrandTask) {
            binding.tvTitle.text = task.title
            binding.tvDescription.text = task.description
            binding.tvReward.text = "RM${task.price}"
            binding.tvLocation.text = task.location
            binding.tvRequester.text = task.requesterName
        }
    }
}
