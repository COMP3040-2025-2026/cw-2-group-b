package com.nottingham.mynottingham.ui.errand

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.ItemCategoryBinding

class CategoryAdapter(
    private var categories: List<String>,
    private val onCategoryClick: (Int) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.binding.tvCategoryName.text = category.replaceFirstChar { it.uppercase() }
        val context = holder.itemView.context

        if (position == selectedPosition) {
            holder.binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            holder.binding.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        } else {
            holder.binding.root.setBackgroundColor(Color.TRANSPARENT)
            holder.binding.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        holder.itemView.setOnClickListener {
            onCategoryClick(position)
        }
    }

    override fun getItemCount() = categories.size

    fun setSelectedPosition(position: Int) {
        if (selectedPosition == position) return
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }
}
