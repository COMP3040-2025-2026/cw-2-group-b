package com.nottingham.mynottingham.ui.errand

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.MenuItem
import com.nottingham.mynottingham.databinding.ItemFoodMenuBinding

class FoodMenuAdapter(
    private val items: List<MenuItem>,
    private val cartQuantities: Map<String, Int>, // 传入当前的购物车数量 Map <ItemID, Quantity>
    private val onAddClick: (MenuItem) -> Unit,
    private val onMinusClick: (MenuItem) -> Unit,
    private val onPlusClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<FoodMenuAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(val binding: ItemFoodMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = items[position]
        val qty = cartQuantities[item.id] ?: 0

        with(holder.binding) {
            tvFoodName.text = item.name
            tvFoodDesc.text = item.description
            tvFoodPrice.text = String.format("RM %.2f", item.price)
            
            // 设置图片 (假设您在 data class 中添加了 imageResId)
            // 如果没有, 这里需要根据 item.id 或名称手动设置图片
             ivFoodImage.setImageResource(getImageResId(item.name))

            // 控制按钮状态
            if (qty > 0) {
                btnAdd.visibility = View.GONE
                layoutQtyControl.visibility = View.VISIBLE
                tvQty.text = qty.toString()
            } else {
                btnAdd.visibility = View.VISIBLE
                layoutQtyControl.visibility = View.GONE
            }

            // 点击事件
            btnAdd.setOnClickListener { onAddClick(item) }
            btnPlus.setOnClickListener { onPlusClick(item) }
            btnMinus.setOnClickListener { onMinusClick(item) }
        }
    }

    override fun getItemCount() = items.size
    
    // 简单的图片映射辅助函数
    private fun getImageResId(name: String): Int {
        return when {
            name.contains("Beef") -> com.nottingham.mynottingham.R.drawable.bsn
            name.contains("Fried Noodles") -> com.nottingham.mynottingham.R.drawable.fn
            name.contains("Chicken Fried Rice") -> com.nottingham.mynottingham.R.drawable.fcr
            name.contains("Combo") -> com.nottingham.mynottingham.R.drawable.crs
            name.contains("Bubble") -> com.nottingham.mynottingham.R.drawable.bt
            name.contains("Lemon") -> com.nottingham.mynottingham.R.drawable.ilt
            else -> com.nottingham.mynottingham.R.drawable.ic_placeholder // 默认图片
        }
    }
}