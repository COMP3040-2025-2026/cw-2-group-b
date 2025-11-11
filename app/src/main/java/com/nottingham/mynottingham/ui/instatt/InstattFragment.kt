package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentInstattBinding

class InstattFragment : Fragment() {

    private var _binding: FragmentInstattBinding? = null
    private val binding get() = _binding!!
    private var currentTab = InstattTab.HOME

    enum class InstattTab {
        HOME, CALENDAR, STATISTICS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstattBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()

        // Load default fragment - show today's courses
        if (savedInstanceState == null) {
            loadFragment(InstattDayCoursesFragment.newInstanceToday())
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Menu button
        binding.btnMenu.setOnClickListener {
            // TODO: Show menu options
        }

        // Bottom navigation
        binding.navHome.setOnClickListener {
            selectTab(InstattTab.HOME)
        }

        binding.navCalendar.setOnClickListener {
            selectTab(InstattTab.CALENDAR)
        }

        binding.navStatistics.setOnClickListener {
            selectTab(InstattTab.STATISTICS)
        }
    }

    private fun selectTab(tab: InstattTab) {
        if (currentTab == tab) return
        currentTab = tab

        // Update indicator - only show under Calendar tab
        binding.indicatorCalendar.isVisible = (tab == InstattTab.CALENDAR)

        // Load fragment
        when (tab) {
            InstattTab.HOME -> {
                // Show today's courses
                loadFragment(InstattDayCoursesFragment.newInstanceToday())
            }
            InstattTab.CALENDAR -> {
                // Show weekly calendar
                loadFragment(InstattCalendarFragment())
            }
            InstattTab.STATISTICS -> {
                // TODO: Create statistics fragment - for now show today's courses
                loadFragment(InstattDayCoursesFragment.newInstanceToday())
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.instatt_container, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
