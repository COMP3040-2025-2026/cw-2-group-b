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
    private var hasChildFragment = false

    enum class ErrandTab {
        HOME, TASKS, MY_TASKS
    }

    companion object {
        private const val KEY_HAS_CHILD_FRAGMENT = "has_child_fragment"

        // Static storage for task details when navigating to chat
        // This survives fragment recreation by Navigation Component
        private var pendingTaskDetails: Bundle? = null

        fun savePendingTaskDetails(bundle: Bundle) {
            pendingTaskDetails = bundle
        }

        fun consumePendingTaskDetails(): Bundle? {
            val details = pendingTaskDetails
            pendingTaskDetails = null
            return details
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErrandBinding.inflate(inflater, container, false)
        // Restore state
        savedInstanceState?.let {
            hasChildFragment = it.getBoolean(KEY_HAS_CHILD_FRAGMENT, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackStackListener()
        setupBottomNavigation()

        // Ensure pending fragment transactions are executed
        childFragmentManager.executePendingTransactions()

        // Check if we need to restore TaskDetailFragment after coming back from chat
        val pendingTask = consumePendingTaskDetails()
        if (pendingTask != null) {
            android.util.Log.d("ErrandFragment", "Restoring TaskDetailFragment from pending details")
            // First ensure we have the base fragment
            if (childFragmentManager.findFragmentById(R.id.errand_fragment_container) == null) {
                childFragmentManager.beginTransaction()
                    .replace(R.id.errand_fragment_container, ErrandHomeFragment())
                    .commitNow()
            }
            // Then show TaskDetailFragment
            val taskDetailFragment = TaskDetailFragment().apply {
                arguments = pendingTask
            }
            childFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.errand_fragment_container, taskDetailFragment)
                .addToBackStack(null)
                .commit()
            return
        }

        // Check if there's already a fragment in the container
        val existingFragment = childFragmentManager.findFragmentById(R.id.errand_fragment_container)

        android.util.Log.d("ErrandFragment", "onViewCreated: existingFragment=${existingFragment?.javaClass?.simpleName}, backStackCount=${childFragmentManager.backStackEntryCount}")

        // Only load home fragment if no existing fragment in container
        if (existingFragment == null) {
            loadFragment(ErrandHomeFragment())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save whether we have child fragments in back stack (indicating we're in a detail view)
        outState.putBoolean(KEY_HAS_CHILD_FRAGMENT, childFragmentManager.backStackEntryCount > 0)
    }

    private fun setupBackStackListener() {
        childFragmentManager.addOnBackStackChangedListener {
            val shouldHideBottomNav = childFragmentManager.backStackEntryCount > 0

            // Update hasChildFragment flag
            hasChildFragment = shouldHideBottomNav

            // 1. Control navigation bar visibility
            binding.errandBottomNav.visibility = if (shouldHideBottomNav) View.GONE else View.VISIBLE

            // 2. Dynamically adjust container bottom margin
            // If hiding navigation bar, set bottom margin to 0; otherwise set to 72dp (consistent with xml height)
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
        // [Fix] Clear back stack when switching tabs to prevent logic issues from switching tabs directly from detail page
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