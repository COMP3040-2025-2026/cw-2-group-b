package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
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
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.InstattRepository
import com.nottingham.mynottingham.databinding.FragmentInstattDayCoursesBinding
import kotlinx.coroutines.flow.first
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
    private lateinit var tokenManager: TokenManager
    private var studentId: Long = 0L
    private var studentName: String = ""

    // 移除轮询机制 - 改用 Firebase 实时监听
    // private val handler = Handler(Looper.getMainLooper())
    // private var isPolling = false

    // Server time cache - updated when fragment is created
    private var serverDate: String? = null
    private var serverDayOfWeek: DayOfWeek? = null
    private var serverTime: String? = null

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

        // Initialize TokenManager and retrieve actual user ID
        tokenManager = TokenManager(requireContext())
        lifecycleScope.launch {
            studentId = tokenManager.getUserId().first()?.toLongOrNull() ?: 0L
            studentName = tokenManager.getFullName().first() ?: "Student"

            if (studentId == 0L) {
                Toast.makeText(
                    context,
                    "User not logged in",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            // Fetch server time first, then load courses
            fetchServerTime()
        }
    }

    private fun fetchServerTime() {
        lifecycleScope.launch {
            val result = repository.getSystemTime()

            result.onSuccess { systemTime ->
                // Cache server time
                serverDate = systemTime.currentDate
                serverDayOfWeek = systemTime.dayOfWeek
                serverTime = systemTime.currentTime

                // Now load courses with server date
                loadCourses()
                // 移除轮询 - Firebase 实时监听会自动更新
                // startPolling()
            }.onFailure { error ->
                // Fallback to local time if server time fails
                Toast.makeText(
                    context,
                    "Failed to sync with server time: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()

                // Use local time as fallback
                serverDate = null
                serverDayOfWeek = null
                serverTime = null
                loadCourses()
                // startPolling()
            }
        }
    }

    private fun loadCourses() {
        // Use server date if available, otherwise fallback to local date
        val today = serverDate ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
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

                // 为每个课程启动 Firebase 实时监听
                startFirebaseListeners(filteredCourses, today)
            }.onFailure { error ->
                // Fallback to mock data if API fails
                Toast.makeText(
                    context,
                    "⚠️ Backend offline - INSTATT features require backend connection",
                    Toast.LENGTH_LONG
                ).show()

                // Log warning for debugging
                android.util.Log.w(
                    "InstattStudent",
                    "Using mock courses - INSTATT sign-in won't work without backend. Error: ${error.message}"
                )

                // Use mock data as fallback (for UI testing only)
                val mockCourses = getMockCourses(dayOfWeek)
                displayCourses(mockCourses)

                // Note: Firebase listeners won't be started for mock courses
                // since they need real courseScheduleIds from MySQL
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

            // Get current time - use server time if available, otherwise system time
            val currentTime = serverTime ?: getCurrentTime()

            // Use TodayClassAdapter for today's view with sign-in callback
            val adapter = TodayClassAdapter(courses, currentTime) { course ->
                handleSignIn(course)
            }
            binding.rvCourses.adapter = adapter
        }
    }

    /**
     * 实时监听 Firebase 签到状态变化
     * 当教师 unlock session 时，学生端按钮立即变亮
     */
    private fun startFirebaseListeners(courses: List<Course>, date: String) {
        courses.forEach { course ->
            lifecycleScope.launch {
                repository.listenToSessionLockStatus(
                    courseScheduleId = course.id.toLong(),
                    date = date
                ).collect { isLocked ->
                    // 更新课程的签到状态
                    course.signInStatus = if (isLocked) {
                        SignInStatus.LOCKED
                    } else {
                        SignInStatus.UNLOCKED
                    }

                    // 刷新 UI
                    binding.rvCourses.adapter?.notifyDataSetChanged()

                    // 日志输出，便于调试
                    android.util.Log.d(
                        "InstattStudent",
                        "Course ${course.courseCode}: isLocked=$isLocked"
                    )
                }
            }
        }
    }

    private fun handleSignIn(course: Course) {
        // Use server date if available, otherwise fallback to local date
        val today = serverDate ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
            LocalDate.now().format(dateFormatter)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        // 显示 loading 提示
        Toast.makeText(
            context,
            "Signing in...",
            Toast.LENGTH_SHORT
        ).show()

        lifecycleScope.launch {
            // 使用 Firebase 签到 - 毫秒级响应
            val result = repository.signIn(
                studentId = studentId,
                courseScheduleId = course.id.toLong(),
                date = today,
                studentName = studentName,
                matricNumber = null, // 可以从 TokenManager 获取学号
                email = null // 可以从 TokenManager 获取邮箱
            )

            result.onSuccess {
                Toast.makeText(
                    context,
                    "Signed in to ${course.courseName}",
                    Toast.LENGTH_SHORT
                ).show()

                // Firebase 会自动通知教师端，无需手动刷新
                // 但为了更新本地 UI，仍然刷新一次
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

    // 移除轮询机制 - 已被 Firebase 实时监听取代
    // 如果需要实时监听课程签到状态，可以在这里添加 Firebase Flow 监听
    // 例如：监听所有今日课程的 isLocked 状态变化

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
        // stopPolling() - 已移除轮询
        // Firebase Flow 会在 lifecycleScope 结束时自动清理
        _binding = null
    }
}

