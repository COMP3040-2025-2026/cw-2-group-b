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
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.MenuItem
import com.nottingham.mynottingham.databinding.FragmentRestaurantMenuBinding

class RestaurantMenuFragment : Fragment() {

    private var _binding: FragmentRestaurantMenuBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RestaurantMenuViewModel by viewModels()
    private lateinit var menuAdapter: FoodMenuAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var menuLayoutManager: LinearLayoutManager

    // Flag to prevent scroll listener from triggering during programmatic scroll
    private var isUserScrolling = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRestaurantMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupObservers()
        setupScrollListeners()
        
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnViewCart.setOnClickListener { showCartSummary() }
    }

    private fun setupRecyclerViews() {
        // Category RecyclerView (Left)
        categoryAdapter = CategoryAdapter(listOf<String>()) { position ->
            // When user clicks a category, scroll the menu
            scrollToCategory(position)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
        
        // Menu RecyclerView (Right)
        menuLayoutManager = LinearLayoutManager(context)
        menuAdapter = FoodMenuAdapter(
            items = listOf<Any>(),
            cartQuantities = emptyMap(),
            onAddClick = { viewModel.addItem(it) },
            onPlusClick = { viewModel.increaseItem(it) },
            onMinusClick = { viewModel.decreaseItem(it) }
        )
        binding.rvMenuItems.apply {
            layoutManager = menuLayoutManager
            adapter = menuAdapter
        }
    }

    private fun setupObservers() {
        // Observe categories for the left RecyclerView
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter = CategoryAdapter(categories) { position ->
                scrollToCategory(position)
            }
            binding.rvCategories.adapter = categoryAdapter
        }

        // Observe the combined list for the right RecyclerView
        viewModel.menuListWithHeaders.observe(viewLifecycleOwner) { menuList ->
            menuAdapter.updateItems(menuList)
        }

        // Observe cart quantity changes
        viewModel.cartQuantities.observe(viewLifecycleOwner) { quantities ->
           menuAdapter.updateQuantities(quantities)
        }

        // Observe cart summary
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

    private fun setupScrollListeners() {
        binding.rvMenuItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isUserScrolling) {
                    val firstVisibleItemPosition = menuLayoutManager.findFirstVisibleItemPosition()
                    if (firstVisibleItemPosition != RecyclerView.NO_POSITION) {
                        val item = viewModel.menuListWithHeaders.value?.get(firstVisibleItemPosition)
                        val categoryName = when(item) {
                            is String -> item
                            is MenuItem -> item.category
                            else -> null
                        }
                        
                        if (categoryName != null) {
                            val categoryIndex = viewModel.categories.value?.indexOf(categoryName)
                            if (categoryIndex != null && categoryIndex != -1) {
                                categoryAdapter.setSelectedPosition(categoryIndex)
                                binding.rvCategories.smoothScrollToPosition(categoryIndex)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun scrollToCategory(categoryIndex: Int) {
        val categoryName = viewModel.categories.value?.get(categoryIndex) ?: return
        val menuList = viewModel.menuListWithHeaders.value ?: return

        val positionInMenuList = menuList.indexOf(categoryName)
        
        if (positionInMenuList != -1) {
            isUserScrolling = false // Disable listener to prevent loop
            menuLayoutManager.scrollToPositionWithOffset(positionInMenuList, 0)
            // A small delay to re-enable the listener after programmatic scroll finishes
            binding.rvMenuItems.postDelayed({ isUserScrolling = true }, 100)
            // Also update the selection immediately
            categoryAdapter.setSelectedPosition(categoryIndex)
        }
    }

    private fun showCartSummary() {
        val total = viewModel.totalPrice.value ?: 0.0
        
        AlertDialog.Builder(requireContext())
            .setTitle("Checkout")
            .setMessage("Confirm order for RM ${String.format("%.2f", total)}?\n(Includes Delivery Fee)")
            .setPositiveButton("Place Order") { _, _ ->
                viewModel.placeOrder("user_123", "Dorm Room 305")
                Toast.makeText(requireContext(), "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
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

