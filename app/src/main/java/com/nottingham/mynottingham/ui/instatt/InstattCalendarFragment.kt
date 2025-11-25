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

    // 使用父 Fragment 的共享 ViewModel
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
            // 先更新本地数据状态
            if (position in daysWithCourses.indices) {
                daysWithCourses[position].isExpanded = !daysWithCourses[position].isExpanded
                // 只通知单个 item 变化，触发动画
                adapter.notifyItemChanged(position)
            }
        }
        binding.rvDays.adapter = adapter
    }

    private fun observeData() {
        // 观察预加载的周课表数据
        viewModel.weekCourses.observe(viewLifecycleOwner) { courses ->
            if (courses.isNotEmpty() && daysWithCourses.isEmpty()) {
                // 只在首次加载时更新全部数据
                daysWithCourses.clear()
                daysWithCourses.addAll(courses)
                adapter.notifyDataSetChanged()
            }
        }

        // 观察加载状态（可选：显示加载指示器）
        viewModel.isWeekCoursesLoading.observe(viewLifecycleOwner) { isLoading ->
            // 如果布局中有 ProgressBar，可以在这里控制显示/隐藏
            // binding.progressBar?.isVisible = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
