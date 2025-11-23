package com.nottingham.mynottingham.ui.errand

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.activityViewModels
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.MenuItem as DataMenuItem
import com.nottingham.mynottingham.databinding.FragmentRestaurantMenuBinding

class RestaurantMenuFragment : Fragment() {

    private var _binding: FragmentRestaurantMenuBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RestaurantMenuViewModel by activityViewModels()
    private lateinit var menuAdapter: FoodMenuAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var menuLayoutManager: LinearLayoutManager

    // Flag to prevent scroll listener from triggering during programmatic scroll
    private var isUserScrolling = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRestaurantMenuBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true) // Enable options menu
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)

        setupRecyclerViews()
        setupObservers()
        setupScrollListeners()

        // [修复] 使用 parentFragmentManager 处理手动事务的返回
        // The user wants a back button in the top right, which will be handled by the menu item.
        // The existing navigation icon is usually on the left.
        // If the user wants to keep the left navigation button, this line can remain.
        // For now, I will comment it out as the request is specifically for a right-side button via menu.
        // binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnViewCart.setOnClickListener { showCartSummary() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_back_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_back -> {
                parentFragmentManager.popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
            onItemClick = { dataMenuItem ->
                val fragment = FoodMenuItemDetailFragment.newInstance(dataMenuItem)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.errand_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
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
                            is DataMenuItem -> item.category
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
        parentFragmentManager.beginTransaction()
            .replace(R.id.errand_fragment_container, CartFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}