package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentErrandBinding

class ErrandFragment : Fragment() {

    private var _binding: FragmentErrandBinding? = null
    private val binding get() = _binding!!
    private var currentTab = ErrandTab.HOME

    enum class ErrandTab {
        HOME, TASKS, MY_TASKS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErrandBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavigation()
        setupBackStackListener() // [新增] 设置回退栈监听

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(ErrandHomeFragment())
        }
    }

    private fun setupBackStackListener() {
        childFragmentManager.addOnBackStackChangedListener {
            val shouldHideBottomNav = childFragmentManager.backStackEntryCount > 0

            // 1. 控制导航栏的显示/隐藏
            binding.errandBottomNav.visibility = if (shouldHideBottomNav) View.GONE else View.VISIBLE

            // 2. 动态调整容器的底部边距 (Margin)
            // 如果隐藏导航栏，底部边距设为 0；否则设为 72dp (与 xml 中的高度一致)
            val params = binding.errandFragmentContainer.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams

            val density = resources.displayMetrics.density
            val marginInDp = if (shouldHideBottomNav) 0 else 72
            params.bottomMargin = (marginInDp * density).toInt()

            binding.errandFragmentContainer.layoutParams = params
        }
    }

    private fun setupBottomNavigation() {
        binding.navErrandHome.setOnClickListener {
            selectTab(ErrandTab.HOME)
        }

        binding.navErrandTasks.setOnClickListener {
            selectTab(ErrandTab.TASKS)
        }

        binding.navErrandMyTasks.setOnClickListener {
            selectTab(ErrandTab.MY_TASKS)
        }

        // Initialize with Home tab selected
        selectTab(ErrandTab.HOME)
    }

    private fun selectTab(tab: ErrandTab) {
        if (currentTab == tab) return
        currentTab = tab

        // Reset all tabs to unselected state
        binding.labelHome.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        binding.labelTasks.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        binding.labelMyTasks.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

        // Load the appropriate fragment and update selected tab color
        when (tab) {
            ErrandTab.HOME -> {
                binding.labelHome.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                loadFragment(ErrandHomeFragment())
            }
            ErrandTab.TASKS -> {
                binding.labelTasks.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                loadFragment(AllTasksFragment())
            }
            ErrandTab.MY_TASKS -> {
                binding.labelMyTasks.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                loadFragment(MyTasksFragment())
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        // [修复] 切换 Tab 时清空回退栈，防止从详情页直接切 Tab 导致的逻辑混乱
        if (childFragmentManager.backStackEntryCount > 0) {
            val firstEntry = childFragmentManager.getBackStackEntryAt(0)
            childFragmentManager.popBackStack(firstEntry.id, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        childFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.errand_fragment_container, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}