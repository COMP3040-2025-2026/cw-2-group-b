package com.nottingham.mynottingham.ui.instatt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.databinding.ItemDayBinding

class DayAdapter(
    private val days: List<DayOfWeek>,
    private val onDayClick: (DayOfWeek) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    inner class DayViewHolder(private val binding: ItemDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(day: DayOfWeek) {
            binding.tvDayName.text = day.displayName

            binding.root.setOnClickListener {
                onDayClick(day)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size
}
