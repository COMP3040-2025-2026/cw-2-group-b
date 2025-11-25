package com.nottingham.mynottingham.ui.instatt

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentInstattBinding
import com.nottingham.mynottingham.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InstattFragment : Fragment() {

    private var _binding: FragmentInstattBinding? = null
    private val binding get() = _binding!!
    private var currentTab = InstattTab.HOME
    private lateinit var tokenManager: TokenManager

    // 共享 ViewModel 用于预加载数据
    private val viewModel: InstattViewModel by viewModels()

    enum class InstattTab {
        HOME, CALENDAR, STATISTICS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstattBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check user role and load appropriate interface
        lifecycleScope.launch {
            val userType = tokenManager.getUserType().first()

            when (userType) {
                "TEACHER" -> {
                    // Teacher interface: hide entire app bar and load teacher fragment
                    binding.appBar.isVisible = false

                    // Remove AppBar scrolling behavior to fill entire screen
                    val params = binding.instattContainer.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = null
                    binding.instattContainer.layoutParams = params

                    if (savedInstanceState == null) {
                        loadFragment(TeacherInstattFragment())
                    }
                }
                else -> {
                    // Student interface: show app bar and setup tabs
                    binding.appBar.isVisible = true

                    setupClickListeners()

                    // Initialize indicator position after layout
                    binding.root.post {
                        positionIndicator(currentTab, false)
                    }

                    // 预加载周课表数据（在后台进行）
                    val studentId = tokenManager.getUserId().first() ?: ""
                    if (studentId.isNotEmpty()) {
                        viewModel.preloadWeekCourses(studentId)
                    }

                    // Load default fragment - show today's courses
                    if (savedInstanceState == null) {
                        loadFragment(InstattDayCoursesFragment.newInstanceToday())
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
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

        // Animate indicator to new position
        positionIndicator(tab, true)

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
                // Show attendance statistics
                loadFragment(InstattStatisticsFragment())
            }
        }
    }

    private fun positionIndicator(tab: InstattTab, animate: Boolean) {
        // Get the target tab view
        val targetView = when (tab) {
            InstattTab.HOME -> binding.navHome
            InstattTab.CALENDAR -> binding.navCalendar
            InstattTab.STATISTICS -> binding.navStatistics
        }

        // Calculate the target X position (center of the tab)
        val targetX = targetView.x + (targetView.width / 2f) - (binding.tabIndicator.width / 2f)

        if (animate) {
            // Animate the indicator to the new position
            ObjectAnimator.ofFloat(binding.tabIndicator, "x", binding.tabIndicator.x, targetX).apply {
                duration = 250
                start()
            }
        } else {
            // Set position without animation
            binding.tabIndicator.x = targetX
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
