package com.nottingham.mynottingham.ui.forum

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.ItemCategoryTabBinding

class CategoryTabsAdapter(
    private val categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryTabsAdapter.CategoryTabViewHolder>() {

    private var selectedCategory: String = categories.first()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryTabViewHolder {
        val binding = ItemCategoryTabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryTabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryTabViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun setSelectedCategory(category: String) {
        selectedCategory = category
        notifyDataSetChanged()
    }

    inner class CategoryTabViewHolder(
        private val binding: ItemCategoryTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: String) {
            binding.tabText.text = category
            if (category == selectedCategory) {
                binding.tabText.setBackgroundResource(R.drawable.bg_category_tab_active)
                binding.tabText.setTextColor(itemView.context.getColor(R.color.primary))
            } else {
                binding.tabText.setBackgroundResource(R.drawable.bg_category_tab_inactive)
                binding.tabText.setTextColor(itemView.context.getColor(android.R.color.white))
            }
            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }
}
