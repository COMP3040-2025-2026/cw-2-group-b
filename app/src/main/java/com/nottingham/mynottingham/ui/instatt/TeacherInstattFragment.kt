package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController // Moved this import to the top
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.CourseType
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.data.model.SignInStatus
import com.nottingham.mynottingham.data.repository.InstattRepository
import com.nottingham.mynottingham.databinding.FragmentTeacherInstattBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class TeacherInstattFragment : Fragment() {

    private var _binding: FragmentTeacherInstattBinding? = null
    private val binding get() = _binding!!

    private val courses = mutableListOf<Course>()
    private lateinit var adapter: TeacherClassAdapter

    // Backend integration
    private val repository = InstattRepository()
    private lateinit var tokenManager: TokenManager
    // 🔴 修复：将 teacherId 从 Long 改为 String，以支持 Firebase UID
    private var teacherId: String = ""

    // 移除轮询机制 - 改用 Firebase 实时监听
    // private val handler = Handler(Looper.getMainLooper())
    // private var isPolling = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherInstattBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        // Initialize TokenManager and retrieve actual user ID
        tokenManager = TokenManager(requireContext())
        lifecycleScope.launch {
            // 🔴 修复：直接获取 String 类型的 Firebase UID，不要转换为 Long
            teacherId = tokenManager.getUserId().first() ?: ""

            // 🔴 修复：检查是否为空字符串
            if (teacherId.isEmpty()) {
                Toast.makeText(
                    context,
                    "User not logged in",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            loadTodayCourses()
            // 移除轮询 - Firebase 实时监听会自动更新
            // startPolling()
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() } // Correctly placed
        // Set current date
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Calendar.getInstance().time)

        // Setup adapter
        adapter = TeacherClassAdapter(
            courses = courses,
            onToggleSignIn = { course ->
                toggleSignIn(course)
            },
            onCourseClick = { course ->
                showCourseManagementDialog(course)
            }
        )
        binding.rvClasses.adapter = adapter

        // Tab listeners (for future implementation)
        binding.tvTabToday.setOnClickListener {
            // Already on today
        }
        binding.tvTabWeek.setOnClickListener {
            Toast.makeText(context, "Week view coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTodayCourses() {
        // Get today's date in ISO format
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate.now().format(dateFormatter)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        lifecycleScope.launch {
            val result = repository.getTeacherCourses(teacherId, today)

            result.onSuccess { fetchedCourses ->
                // Update courses list
                courses.clear()
                courses.addAll(fetchedCourses)
                displayCourses()
            }.onFailure { error ->
                // Fallback to mock data if API fails
                Toast.makeText(
                    context,
                    "⚠️ Backend offline - INSTATT features require backend connection",
                    Toast.LENGTH_LONG
                ).show()

                // Log warning for debugging
                android.util.Log.w(
                    "InstattTeacher",
                    "Using mock courses - INSTATT unlock/lock won't work without backend. Error: ${error.message}"
                )

                // Use mock data as fallback (for UI testing only)
                val currentDay = getCurrentDayOfWeek()
                courses.clear()
                courses.addAll(getMockTeacherCourses(currentDay))
                displayCourses()

                // Note: Firebase operations won't work properly with mock course IDs
            }
        }
    }

    private fun displayCourses() {
        if (courses.isEmpty()) {
            binding.rvClasses.isVisible = false
            binding.layoutEmpty.isVisible = true
        } else {
            binding.rvClasses.isVisible = true
            binding.layoutEmpty.isVisible = false
            adapter.notifyDataSetChanged()
        }
    }

    private fun toggleSignIn(course: Course) {
        // Get today's date
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate.now().format(dateFormatter)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        when (course.signInStatus) {
            SignInStatus.LOCKED, SignInStatus.CLOSED -> {
                // Unlock sign-in via Firebase - 实时生效
                // LOCKED 和 CLOSED 状态都允许重新开启签到
                lifecycleScope.launch {
                    val result = repository.unlockSession(teacherId, course.id, today)  // ✅ 直接使用 String ID

                    result.onSuccess {
                        Toast.makeText(
                            context,
                            "Sign-in unlocked for ${course.courseName}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Firebase 会自动通知所有学生端，无需手动刷新
                        // 但为了更新本地 UI，仍然刷新一次
                        loadTodayCourses()
                    }.onFailure { error ->
                        Toast.makeText(
                            context,
                            "Failed to unlock: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            SignInStatus.UNLOCKED -> {
                // Lock sign-in via Firebase - 实时生效
                lifecycleScope.launch {
                    val result = repository.lockSession(teacherId, course.id, today)  // ✅ 直接使用 String ID

                    result.onSuccess {
                        Toast.makeText(
                            context,
                            "Sign-in closed for ${course.courseName}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Firebase 会自动通知所有学生端，无需手动刷新
                        loadTodayCourses()
                    }.onFailure { error ->
                        Toast.makeText(
                            context,
                            "Failed to lock: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // 移除轮询机制 - 已被 Firebase 实时监听取代
    // private fun startPolling() { ... }
    // private fun stopPolling() { ... }

    private fun getCurrentDayOfWeek(): DayOfWeek {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.TUESDAY -> DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> DayOfWeek.THURSDAY
            Calendar.FRIDAY -> DayOfWeek.FRIDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            Calendar.SUNDAY -> DayOfWeek.SUNDAY
            else -> DayOfWeek.MONDAY
        }
    }

    private fun getMockTeacherCourses(day: DayOfWeek): List<Course> {
        // Mock data - in real app, this would come from database or API
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
                    signInStatus = SignInStatus.LOCKED
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
                    signInStatus = SignInStatus.LOCKED
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
                    signInStatus = SignInStatus.LOCKED
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
                    signInStatus = SignInStatus.LOCKED
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
                    signInStatus = SignInStatus.LOCKED
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
                    signInStatus = SignInStatus.LOCKED
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
                    signInStatus = SignInStatus.LOCKED
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
                    signInStatus = SignInStatus.LOCKED
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
                    signInStatus = SignInStatus.LOCKED
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
                    signInStatus = SignInStatus.LOCKED
                )
            )
            else -> emptyList()
        }
    }

    private fun showCourseManagementDialog(course: Course) {
        val bottomSheet = CourseManagementBottomSheet.newInstance(course)
        bottomSheet.show(parentFragmentManager, "CourseManagementBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // stopPolling() - 已移除轮询
        // Firebase Flow 会在 lifecycleScope 结束时自动清理
        _binding = null
    }
}

