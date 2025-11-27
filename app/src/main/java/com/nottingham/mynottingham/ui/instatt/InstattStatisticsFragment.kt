package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.databinding.FragmentInstattStatisticsBinding

class InstattStatisticsFragment : Fragment() {

    private var _binding: FragmentInstattStatisticsBinding? = null
    private val binding get() = _binding!!

    // 使用父 Fragment 的共享 ViewModel
    private val viewModel: InstattViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstattStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observePreloadedData()
    }

    /**
     * 观察预加载的数据
     * 数据已在 InstattFragment 进入时预加载
     */
    private fun observePreloadedData() {
        // 观察所有唯一课程（用于统计）
        viewModel.allCourses.observe(viewLifecycleOwner) { courses ->
            displayStatistics(courses ?: emptyList())
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 可以在这里显示加载指示器
            // binding.progressBar?.isVisible = isLoading
        }
    }

    private fun displayStatistics(courses: List<Course>) {
        if (courses.isEmpty()) {
            binding.rvStatistics.isVisible = false
            binding.layoutEmpty.isVisible = true
        } else {
            binding.rvStatistics.isVisible = true
            binding.layoutEmpty.isVisible = false

            // Use the existing CourseAttendanceAdapter which shows attendance progress
            val adapter = CourseAttendanceAdapter(courses)
            binding.rvStatistics.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
