package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nottingham.mynottingham.databinding.FragmentInstattCalendarBinding

class InstattCalendarFragment : Fragment() {

    private var _binding: FragmentInstattCalendarBinding? = null
    private val binding get() = _binding!!

    // Use parent Fragment's shared ViewModel
    private val viewModel: InstattViewModel by viewModels({ requireParentFragment() })

    private lateinit var adapter: DayAdapter
    private val daysWithCourses = mutableListOf<DayWithCourses>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstattCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDaysList()
        observeData()
    }

    private fun setupDaysList() {
        // Initialize adapter with empty data
        adapter = DayAdapter(daysWithCourses) { position ->
            // Update local data state first
            if (position in daysWithCourses.indices) {
                daysWithCourses[position].isExpanded = !daysWithCourses[position].isExpanded
                // Only notify single item change to trigger animation
                adapter.notifyItemChanged(position)
            }
        }
        binding.rvDays.adapter = adapter
    }

    private fun observeData() {
        // Observe preloaded weekly schedule data
        viewModel.weekCourses.observe(viewLifecycleOwner) { courses ->
            if (courses.isNotEmpty() && daysWithCourses.isEmpty()) {
                // Only update all data on first load
                daysWithCourses.clear()
                daysWithCourses.addAll(courses)
                adapter.notifyDataSetChanged()
            }
        }

        // Observe loading state (optional: show loading indicator)
        viewModel.isWeekCoursesLoading.observe(viewLifecycleOwner) { isLoading ->
            // If layout has ProgressBar, can control visibility here
            // binding.progressBar?.isVisible = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
