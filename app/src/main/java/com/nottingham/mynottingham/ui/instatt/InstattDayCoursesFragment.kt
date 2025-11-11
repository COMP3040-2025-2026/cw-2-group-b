package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.databinding.FragmentInstattDayCoursesBinding

class InstattDayCoursesFragment : Fragment() {

    private var _binding: FragmentInstattDayCoursesBinding? = null
    private val binding get() = _binding!!
    private lateinit var dayOfWeek: DayOfWeek

    companion object {
        private const val ARG_DAY_OF_WEEK = "day_of_week"

        fun newInstance(dayOfWeek: DayOfWeek): InstattDayCoursesFragment {
            val fragment = InstattDayCoursesFragment()
            val args = Bundle()
            args.putString(ARG_DAY_OF_WEEK, dayOfWeek.name)
            fragment.arguments = args
            return fragment
        }

        fun newInstanceToday(): InstattDayCoursesFragment {
            val today = getCurrentDayOfWeek()
            return newInstance(today)
        }

        private fun getCurrentDayOfWeek(): DayOfWeek {
            val calendar = java.util.Calendar.getInstance()
            return when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.MONDAY -> DayOfWeek.MONDAY
                java.util.Calendar.TUESDAY -> DayOfWeek.TUESDAY
                java.util.Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
                java.util.Calendar.THURSDAY -> DayOfWeek.THURSDAY
                java.util.Calendar.FRIDAY -> DayOfWeek.FRIDAY
                java.util.Calendar.SATURDAY -> DayOfWeek.SATURDAY
                java.util.Calendar.SUNDAY -> DayOfWeek.SUNDAY
                else -> DayOfWeek.MONDAY
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val dayName = it.getString(ARG_DAY_OF_WEEK) ?: DayOfWeek.MONDAY.name
            dayOfWeek = DayOfWeek.valueOf(dayName)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstattDayCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCourses()
    }

    private fun loadCourses() {
        // Mock data for demonstration
        val courses = getMockCourses(dayOfWeek)

        if (courses.isEmpty()) {
            binding.rvCourses.isVisible = false
            binding.layoutEmpty.isVisible = true
        } else {
            binding.rvCourses.isVisible = true
            binding.layoutEmpty.isVisible = false

            val adapter = CourseAttendanceAdapter(courses)
            binding.rvCourses.adapter = adapter
        }
    }

    private fun getMockCourses(day: DayOfWeek): List<Course> {
        // Mock data - in real app, this would come from database or API
        return when (day) {
            DayOfWeek.MONDAY -> listOf(
                Course(
                    id = "1",
                    courseName = "Mobile Device Programming",
                    courseCode = "COMP3040",
                    semester = "25-26",
                    attendedClasses = 6,
                    totalClasses = 5,
                    dayOfWeek = day,
                    startTime = "09:00 AM",
                    endTime = "11:00 AM",
                    location = "Lab 2A"
                ),
                Course(
                    id = "2",
                    courseName = "Professional Ethics in Computing",
                    courseCode = "COMP3041",
                    semester = "25-26",
                    attendedClasses = 3,
                    totalClasses = 3,
                    dayOfWeek = day,
                    startTime = "02:00 PM",
                    endTime = "04:00 PM",
                    location = "LT3"
                )
            )
            DayOfWeek.TUESDAY -> listOf(
                Course(
                    id = "3",
                    courseName = "Symbolic Artificial Intelligence",
                    courseCode = "COMP3070",
                    semester = "25-26",
                    attendedClasses = 5,
                    totalClasses = 5,
                    dayOfWeek = day,
                    startTime = "10:00 AM",
                    endTime = "12:00 PM",
                    location = "LT1"
                )
            )
            DayOfWeek.WEDNESDAY -> listOf(
                Course(
                    id = "1",
                    courseName = "Mobile Device Programming",
                    courseCode = "COMP3040",
                    semester = "25-26",
                    attendedClasses = 6,
                    totalClasses = 5,
                    dayOfWeek = day,
                    startTime = "09:00 AM",
                    endTime = "11:00 AM",
                    location = "Lab 2A"
                ),
                Course(
                    id = "4",
                    courseName = "Autonomous Robotic Systems",
                    courseCode = "COMP4082",
                    semester = "25-26",
                    attendedClasses = 5,
                    totalClasses = 5,
                    dayOfWeek = day,
                    startTime = "03:00 PM",
                    endTime = "05:00 PM",
                    location = "Lab 3B"
                )
            )
            DayOfWeek.THURSDAY -> listOf(
                Course(
                    id = "2",
                    courseName = "Professional Ethics in Computing",
                    courseCode = "COMP3041",
                    semester = "25-26",
                    attendedClasses = 3,
                    totalClasses = 3,
                    dayOfWeek = day,
                    startTime = "11:00 AM",
                    endTime = "01:00 PM",
                    location = "LT3"
                )
            )
            DayOfWeek.FRIDAY -> listOf(
                Course(
                    id = "3",
                    courseName = "Symbolic Artificial Intelligence",
                    courseCode = "COMP3070",
                    semester = "25-26",
                    attendedClasses = 5,
                    totalClasses = 5,
                    dayOfWeek = day,
                    startTime = "02:00 PM",
                    endTime = "04:00 PM",
                    location = "LT1"
                ),
                Course(
                    id = "4",
                    courseName = "Autonomous Robotic Systems",
                    courseCode = "COMP4082",
                    semester = "25-26",
                    attendedClasses = 5,
                    totalClasses = 5,
                    dayOfWeek = day,
                    startTime = "04:00 PM",
                    endTime = "06:00 PM",
                    location = "Lab 3B"
                )
            )
            else -> emptyList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
