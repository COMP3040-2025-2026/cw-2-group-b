package com.nottingham.mynottingham.ui.errand

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.databinding.FragmentRestaurantMenuBinding

class RestaurantMenuFragment : Fragment() {

    private var _binding: FragmentRestaurantMenuBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RestaurantMenuViewModel by viewModels()
    private lateinit var adapter: FoodMenuAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRestaurantMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        
        // 顶部返回按钮
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        
        // 底部 View Cart 按钮
        binding.btnViewCart.setOnClickListener { showCartSummary() }
    }

    private fun setupRecyclerView() {
        // 初始化 Adapter, 传入空 map, 之后通过 observer 更新
        adapter = FoodMenuAdapter(
            items = viewModel.menuItems,
            cartQuantities = emptyMap(),
            onAddClick = { viewModel.addItem(it) },
            onPlusClick = { viewModel.increaseItem(it) },
            onMinusClick = { viewModel.decreaseItem(it) }
        )
        
        binding.rvMenuItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@RestaurantMenuFragment.adapter
        }
    }

    private fun setupObservers() {
        // 监听购物车数量变化，刷新列表 UI (显示/隐藏加减号)
        viewModel.cartQuantities.observe(viewLifecycleOwner) { quantities ->
            // 重新创建 Adapter 以更新所有行状态 (或者可以在 Adapter 中写 updateData 方法以提高性能)
            adapter = FoodMenuAdapter(
                items = viewModel.menuItems,
                cartQuantities = quantities,
                onAddClick = { viewModel.addItem(it) },
                onPlusClick = { viewModel.increaseItem(it) },
                onMinusClick = { viewModel.decreaseItem(it) }
            )
            binding.rvMenuItems.adapter = adapter
        }

        // 监听总价和总数，控制底部栏显示
        viewModel.totalCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.layoutCartSummary.visibility = View.VISIBLE
                binding.btnViewCart.text = "View Cart ($count)"
            } else {
                binding.layoutCartSummary.visibility = View.GONE
            }
        }

        viewModel.totalPrice.observe(viewLifecycleOwner) { price ->
            binding.tvTotalPrice.text = String.format("RM %.2f", price)
        }
    }

    private fun showCartSummary() {
        val total = viewModel.totalPrice.value ?: 0.0
        
        AlertDialog.Builder(requireContext())
            .setTitle("Checkout")
            .setMessage("Confirm order for RM ${String.format("%.2f", total)}?\n(Includes Delivery Fee)")
            .setPositiveButton("Place Order") { _, _ ->
                // 实际开发中应替换为真实 User ID
                viewModel.placeOrder("user_123", "Dorm Room 305")
                Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
