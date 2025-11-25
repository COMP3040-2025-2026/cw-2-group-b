package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.MenuItem
import com.nottingham.mynottingham.databinding.FragmentFoodDeliveryBinding

class FoodDeliveryFragment : Fragment() {

    private var _binding: FragmentFoodDeliveryBinding? = null
    private val binding get() = _binding!!

    // 获取共享 ViewModel 以访问菜单数据
    private val viewModel: RestaurantMenuViewModel by activityViewModels()
    private lateinit var searchAdapter: FoodSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodDeliveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupSearch()
        setupRestaurantClicks()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupSearch() {
        // 初始化简单的纯文字适配器
        searchAdapter = FoodSearchAdapter { menuItem ->
            navigateToRestaurantWithItem(menuItem)
        }

        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
            // 给下拉列表加一个简单的背景和阴影，让它浮在内容上面
            setBackgroundResource(R.drawable.bg_search_tag) // 需确保有此drawable，或使用白色背景
            elevation = 10f
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                binding.btnClearSearch.isVisible = query.isNotEmpty()

                if (query.isNotEmpty()) {
                    performSearch(query)
                } else {
                    binding.rvSearchResults.isVisible = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            binding.rvSearchResults.isVisible = false
        }
    }

    private fun performSearch(query: String) {
        val allItems = viewModel.menuItems
        // 简单的模糊搜索：匹配菜名
        val filteredList = allItems.filter {
            it.name.contains(query, ignoreCase = true)
        }

        if (filteredList.isNotEmpty()) {
            searchAdapter.submitList(filteredList)
            binding.rvSearchResults.isVisible = true
        } else {
            binding.rvSearchResults.isVisible = false
        }
    }

    private fun navigateToRestaurantWithItem(item: MenuItem) {
        // 跳转并传递 highlight_item_id
        val fragment = RestaurantMenuFragment().apply {
            arguments = Bundle().apply {
                putString("highlight_item_id", item.id)
            }
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
            )
            .replace(R.id.errand_fragment_container, fragment)
            .addToBackStack("restaurant_menu")
            .commit()

        // 重置搜索状态
        binding.etSearch.text.clear()
        binding.rvSearchResults.isVisible = false
    }

    private fun setupRestaurantClicks() {
        binding.cardRestaurantChinese.setOnClickListener {
            viewModel.setRestaurantName("Chinese Restaurant")
            parentFragmentManager.beginTransaction()
                .replace(R.id.errand_fragment_container, RestaurantMenuFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.cardRestaurantWestern.apply {
            alpha = 0.5f
            isClickable = false
        }
        binding.cardRestaurantCafe.apply {
            alpha = 0.5f
            isClickable = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- 简化的 Adapter 内部类 ---
    inner class FoodSearchAdapter(private val onGoClick: (MenuItem) -> Unit) :
        RecyclerView.Adapter<FoodSearchAdapter.SearchViewHolder>() {

        private var items = listOf<MenuItem>()

        fun submitList(newItems: List<MenuItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
            // 使用新建的 item_search_food.xml
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result, parent, false)
            return SearchViewHolder(view)
        }

        override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvName: TextView = view.findViewById(R.id.tv_food_name)
            private val btnGo: TextView = view.findViewById(R.id.btn_go)
            private val root: View = view

            fun bind(item: MenuItem) {
                tvName.text = item.name

                // 点击整个条目或者点击 GO 都可以跳转
                val clickListener = View.OnClickListener { onGoClick(item) }
                btnGo.setOnClickListener(clickListener)
                root.setOnClickListener(clickListener)
            }
        }
    }
}