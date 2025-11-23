package com.nottingham.mynottingham.ui.errand

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.MenuItem
import com.nottingham.mynottingham.databinding.ItemFoodHeaderBinding
import com.nottingham.mynottingham.databinding.ItemFoodMenuBinding

class FoodMenuAdapter(
    private var items: List<Any>,
    private var cartQuantities: Map<String, Int>,
    private val onItemClick: (MenuItem) -> Unit, // Add this line
    private val onAddClick: (MenuItem) -> Unit,
    private val onMinusClick: (MenuItem) -> Unit,
    private val onPlusClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    inner class HeaderViewHolder(val binding: ItemFoodHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    inner class FoodViewHolder(val binding: ItemFoodMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is String -> TYPE_HEADER
            is MenuItem -> TYPE_ITEM
            else -> throw IllegalArgumentException("Invalid type of data at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemFoodHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            TYPE_ITEM -> {
                val binding = ItemFoodMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                FoodViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val headerText = items[position] as String
                holder.binding.tvHeaderTitle.text = headerText.replaceFirstChar { it.uppercase() }
            }
            is FoodViewHolder -> {
                val item = items[position] as MenuItem
                val qty = cartQuantities[item.id] ?: 0

                with(holder.binding) {
                    tvFoodName.text = item.name
                    tvFoodPrice.text = String.format("RM %.2f", item.price)
                    ivFoodImage.setImageResource(getImageResId(item.name))

                    if (qty > 0) {
                        btnAdd.visibility = View.GONE
                        layoutQtyControl.visibility = View.VISIBLE
                        tvQty.text = qty.toString()
                    } else {
                        btnAdd.visibility = View.VISIBLE
                        layoutQtyControl.visibility = View.GONE
                    }

                    btnAdd.setOnClickListener { onAddClick(item) }
                    btnPlus.setOnClickListener { onPlusClick(item) }
                    btnMinus.setOnClickListener { onMinusClick(item) }
                }

                holder.itemView.setOnClickListener { onItemClick(item) }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateQuantities(newQuantities: Map<String, Int>) {
        cartQuantities = newQuantities
        notifyDataSetChanged()
    }
    
    private fun getImageResId(name: String): Int {
        return when {
            name.contains("Beef") -> com.nottingham.mynottingham.R.drawable.bsn
            name.contains("Fried Noodles") -> com.nottingham.mynottingham.R.drawable.fn
            name.contains("Chicken Fried Rice") -> com.nottingham.mynottingham.R.drawable.fcr
            name.contains("Combo") -> com.nottingham.mynottingham.R.drawable.crs
            name.contains("Bubble") -> com.nottingham.mynottingham.R.drawable.bt
            name.contains("Lemon") -> com.nottingham.mynottingham.R.drawable.ilt
            else -> com.nottingham.mynottingham.R.drawable.ic_placeholder
        }
    }
}