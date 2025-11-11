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

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(ErrandHomeFragment())
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
