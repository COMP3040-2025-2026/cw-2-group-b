package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.CourseType
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.databinding.FragmentInstattStatisticsBinding

class InstattStatisticsFragment : Fragment() {

    private var _binding: FragmentInstattStatisticsBinding? = null
    private val binding get() = _binding!!

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
        loadStatistics()
    }

    private fun loadStatistics() {
        // Get all courses for statistics
        val courses = getAllCourses()

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

    private fun getAllCourses(): List<Course> {
        // Mock data - aggregate all unique courses
        // In real app, this would come from database
        return listOf(
            Course(
                id = "1",
                courseName = "Mobile Device Programming",
                courseCode = "COMP3040",
                semester = "25-26",
                attendedClasses = 15,
                totalClasses = 15,
                dayOfWeek = DayOfWeek.MONDAY,
                courseType = CourseType.LAB
            ),
            Course(
                id = "2",
                courseName = "Professional Ethics in Computing",
                courseCode = "COMP3041",
                semester = "25-26",
                attendedClasses = 7,
                totalClasses = 7,
                dayOfWeek = DayOfWeek.MONDAY,
                courseType = CourseType.LECTURE
            ),
            Course(
                id = "3",
                courseName = "Symbolic Artificial Intelligence",
                courseCode = "COMP3070",
                semester = "25-26",
                attendedClasses = 14,
                totalClasses = 14,
                dayOfWeek = DayOfWeek.TUESDAY,
                courseType = CourseType.COMPUTING
            ),
            Course(
                id = "4",
                courseName = "Autonomous Robotic Systems",
                courseCode = "COMP4082",
                semester = "25-26",
                attendedClasses = 15,
                totalClasses = 15,
                dayOfWeek = DayOfWeek.WEDNESDAY,
                courseType = CourseType.LECTURE
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
