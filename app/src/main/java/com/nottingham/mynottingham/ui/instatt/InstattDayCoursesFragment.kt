package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.CourseType
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.data.model.SignInStatus
import com.nottingham.mynottingham.data.model.TodayClassStatus
import com.nottingham.mynottingham.data.repository.InstattRepository
import com.nottingham.mynottingham.databinding.FragmentInstattDayCoursesBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class InstattDayCoursesFragment : Fragment() {

    private var _binding: FragmentInstattDayCoursesBinding? = null
    private val binding get() = _binding!!
    private lateinit var dayOfWeek: DayOfWeek

    private val repository = InstattRepository()
    private val studentId: Long = 1L // TODO: Get from login session
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false

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
        startPolling()
    }

    private fun loadCourses() {
        // Get today's date in ISO format
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate.now().format(dateFormatter)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        lifecycleScope.launch {
            val result = repository.getStudentCourses(studentId, today)

            result.onSuccess { courses ->
                // Filter courses by current day of week
                val filteredCourses = courses.filter { it.dayOfWeek == dayOfWeek }

                displayCourses(filteredCourses)
            }.onFailure { error ->
                // Fallback to mock data if API fails
                Toast.makeText(
                    context,
                    "Using offline data (Backend not connected)",
                    Toast.LENGTH_SHORT
                ).show()

                // Use mock data as fallback
                val mockCourses = getMockCourses(dayOfWeek)
                displayCourses(mockCourses)
            }
        }
    }

    private fun displayCourses(courses: List<Course>) {
        if (courses.isEmpty()) {
            binding.rvCourses.isVisible = false
            binding.layoutEmpty.isVisible = true
        } else {
            binding.rvCourses.isVisible = true
            binding.layoutEmpty.isVisible = false

            // Use TodayClassAdapter for today's view with sign-in callback
            val adapter = TodayClassAdapter(courses) { course ->
                handleSignIn(course)
            }
            binding.rvCourses.adapter = adapter
        }
    }

    private fun handleSignIn(course: Course) {
        // Get today's date
        val dateFormatter = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        } else null

        val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && dateFormatter != null) {
            LocalDate.now().format(dateFormatter)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        lifecycleScope.launch {
            val result = repository.signIn(studentId, course.id.toLong(), today)

            result.onSuccess {
                Toast.makeText(
                    context,
                    "Signed in to ${course.courseName}",
                    Toast.LENGTH_SHORT
                ).show()

                // Reload to update UI
                loadCourses()
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    "Sign-in failed: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startPolling() {
        if (isPolling) return
        isPolling = true

        val pollingRunnable = object : Runnable {
            override fun run() {
                if (isPolling && _binding != null) {
                    loadCourses()
                    handler.postDelayed(this, 3_000) // Poll every 3 seconds for real-time updates
                }
            }
        }

        handler.postDelayed(pollingRunnable, 3_000)
    }

    private fun stopPolling() {
        isPolling = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun getMockCourses(day: DayOfWeek): List<Course> {
        // Mock data - in real app, this would come from database or API
        val currentTime = getCurrentTime()

        return when (day) {
            DayOfWeek.MONDAY -> listOf(
                Course(
                    id = "0",
                    courseName = "Data Structures",
                    courseCode = "COMP2001",
                    semester = "25-26",
                    attendedClasses = 14,
                    totalClasses = 15,
                    dayOfWeek = day,
                    startTime = "08:00",
                    endTime = "09:00",
                    location = "LT1",
                    courseType = CourseType.LECTURE,
                    todayStatus = TodayClassStatus.MISSED // Force MISSED for testing
                ),
                Course(
                    id = "1",
                    courseName = "Mobile Device Programming",
                    courseCode = "COMP3040",
                    semester = "25-26",
                    attendedClasses = 15,
                    totalClasses = 15,
                    dayOfWeek = day,
                    startTime = "09:00",
                    endTime = "10:00",
                    location = "Lab 2A",
                    courseType = CourseType.LAB,
                    todayStatus = TodayClassStatus.ATTENDED // Force ATTENDED for testing
                ),
                Course(
                    id = "2",
                    courseName = "Professional Ethics in Computing",
                    courseCode = "COMP3041",
                    semester = "25-26",
                    attendedClasses = 7,
                    totalClasses = 7,
                    dayOfWeek = day,
                    startTime = "14:00",
                    endTime = "16:00",
                    location = "LT3",
                    courseType = CourseType.LECTURE,
                    todayStatus = determineStatus("14:00", "16:00", currentTime, false)
                )
            )
            DayOfWeek.TUESDAY -> listOf(
                Course(
                    id = "3",
                    courseName = "Symbolic Artificial Intelligence",
                    courseCode = "COMP3070",
                    semester = "25-26",
                    attendedClasses = 14,
                    totalClasses = 14,
                    dayOfWeek = day,
                    startTime = "09:00",
                    endTime = "11:00",
                    location = "BB80",
                    courseType = CourseType.COMPUTING,
                    todayStatus = determineStatus("09:00", "11:00", currentTime, true)
                ),
                Course(
                    id = "4",
                    courseName = "Autonomous Robotic Systems",
                    courseCode = "COMP4082",
                    semester = "25-26",
                    attendedClasses = 15,
                    totalClasses = 15,
                    dayOfWeek = day,
                    startTime = "14:00",
                    endTime = "16:00",
                    location = "F1A24",
                    courseType = CourseType.LECTURE,
                    todayStatus = determineStatus("14:00", "16:00", currentTime, true) // ARS: Attended
                )
            )
            DayOfWeek.WEDNESDAY -> listOf(
                Course(
                    id = "1",
                    courseName = "Mobile Device Programming",
                    courseCode = "COMP3040",
                    semester = "25-26",
                    attendedClasses = 15,
                    totalClasses = 15,
                    dayOfWeek = day,
                    startTime = "09:00",
                    endTime = "11:00",
                    location = "Lab 2A",
                    courseType = CourseType.LAB,
                    signInStatus = SignInStatus.UNLOCKED,  // Available for sign-in
                    hasStudentSigned = false  // Not signed yet - will show pencil
                ),
                Course(
                    id = "4",
                    courseName = "Autonomous Robotic Systems",
                    courseCode = "COMP4082",
                    semester = "25-26",
                    attendedClasses = 15,
                    totalClasses = 15,
                    dayOfWeek = day,
                    startTime = "15:00",
                    endTime = "17:00",
                    location = "Lab 3B",
                    courseType = CourseType.LAB,
                    signInStatus = SignInStatus.LOCKED  // Not yet available - will show lock
                )
            )
            DayOfWeek.THURSDAY -> listOf(
                Course(
                    id = "2",
                    courseName = "Professional Ethics in Computing",
                    courseCode = "COMP3041",
                    semester = "25-26",
                    attendedClasses = 7,
                    totalClasses = 7,
                    dayOfWeek = day,
                    startTime = "11:00",
                    endTime = "13:00",
                    location = "LT3",
                    courseType = CourseType.TUTORIAL,
                    todayStatus = determineStatus("11:00", "13:00", currentTime, true)
                )
            )
            DayOfWeek.FRIDAY -> listOf(
                Course(
                    id = "3",
                    courseName = "Symbolic Artificial Intelligence",
                    courseCode = "COMP3070",
                    semester = "25-26",
                    attendedClasses = 14,
                    totalClasses = 14,
                    dayOfWeek = day,
                    startTime = "14:00",
                    endTime = "16:00",
                    location = "LT1",
                    courseType = CourseType.LECTURE,
                    todayStatus = determineStatus("14:00", "16:00", currentTime, true)
                ),
                Course(
                    id = "4",
                    courseName = "Autonomous Robotic Systems",
                    courseCode = "COMP4082",
                    semester = "25-26",
                    attendedClasses = 15,
                    totalClasses = 15,
                    dayOfWeek = day,
                    startTime = "16:00",
                    endTime = "18:00",
                    location = "Lab 3B",
                    courseType = CourseType.LAB,
                    todayStatus = determineStatus("16:00", "18:00", currentTime, true) // ARS: Attended
                )
            )
            else -> emptyList()
        }
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(calendar.time)
    }

    private fun determineStatus(
        startTime: String,
        endTime: String,
        currentTime: String,
        attended: Boolean
    ): TodayClassStatus {
        // Convert times to comparable integers (e.g., "09:00" -> 900)
        val start = startTime.replace(":", "").toInt()
        val end = endTime.replace(":", "").toInt()
        val current = currentTime.replace(":", "").toInt()

        return when {
            current < start -> TodayClassStatus.UPCOMING
            current in start..end -> TodayClassStatus.IN_PROGRESS
            current > end && attended -> TodayClassStatus.ATTENDED
            current > end && !attended -> TodayClassStatus.MISSED
            else -> TodayClassStatus.UPCOMING
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPolling()
        _binding = null
    }
}
