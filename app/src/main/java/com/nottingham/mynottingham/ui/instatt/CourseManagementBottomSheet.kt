package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.AttendanceStatus
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.SignInStatus
import com.nottingham.mynottingham.data.model.StudentAttendance
import com.nottingham.mynottingham.data.repository.InstattRepository
import com.nottingham.mynottingham.databinding.DialogCourseManagementBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class CourseManagementBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogCourseManagementBinding? = null
    private val binding get() = _binding!!

    private val repository = InstattRepository()
    private lateinit var tokenManager: TokenManager
    // 🔴 修复：将 teacherId 从 Long 改为 String，以支持 Firebase UID
    private var teacherId: String = ""

    private lateinit var course: Course
    private lateinit var studentAdapter: StudentAttendanceAdapter

    // 🔴 新增：监听器，用于通知父界面刷新
    var onSessionStatusChanged: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCourseManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get course from arguments
        course = arguments?.getParcelable(ARG_COURSE) as? Course
            ?: run {
                dismiss()
                return
            }

        setupUI()
        setupRecyclerView()

        // Initialize TokenManager and retrieve actual user ID
        tokenManager = TokenManager(requireContext())
        lifecycleScope.launch {
            // 🔴 修复：直接获取 String 类型的 Firebase UID，不要转换为 Long
            teacherId = tokenManager.getUserId().first() ?: ""

            // 🔴 修复：检查是否为空字符串
            if (teacherId.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "User not logged in",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
                return@launch
            }

            loadStudentList()
        }
    }

    private fun setupUI() {
        binding.apply {
            // Display course information
            tvCourseName.text = course.courseName
            tvCourseCode.text = course.courseCode
            tvCourseTime.text = "${course.startTime} - ${course.endTime}"
            tvCourseLocation.text = course.location

            // Update session status indicator
            updateSessionStatusUI()

            // Close button
            btnClose.setOnClickListener {
                dismiss()
            }

            // Unlock button
            btnUnlockSession.setOnClickListener {
                unlockSession()
            }

            // Lock button
            btnLockSession.setOnClickListener {
                lockSession()
            }
        }
    }

    private fun updateSessionStatusUI() {
        binding.apply {
            when (course.signInStatus) {
                SignInStatus.LOCKED -> {
                    tvSessionStatus.text = "Session is LOCKED"
                    tvSessionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.error)
                    )
                    ivSessionStatusIcon.setImageResource(R.drawable.ic_lock)
                    ivSessionStatusIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.error)
                    )
                    btnUnlockSession.isEnabled = true
                    btnLockSession.isEnabled = false
                }
                SignInStatus.UNLOCKED, SignInStatus.SIGNED -> {
                    // SIGNED is student-specific, teacher sees UNLOCKED state
                    tvSessionStatus.text = "Session is UNLOCKED"
                    tvSessionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.success)
                    )
                    ivSessionStatusIcon.setImageResource(R.drawable.ic_lock_open)
                    ivSessionStatusIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.success)
                    )
                    btnUnlockSession.isEnabled = false
                    btnLockSession.isEnabled = true
                }
                SignInStatus.CLOSED -> {
                    tvSessionStatus.text = "Session is CLOSED"
                    tvSessionStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_hint)
                    )
                    ivSessionStatusIcon.setImageResource(R.drawable.ic_lock)
                    ivSessionStatusIcon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.text_hint)
                    )
                    btnUnlockSession.isEnabled = false
                    btnLockSession.isEnabled = false
                }
            }
        }
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAttendanceAdapter { student, newStatus ->
            markAttendance(student, newStatus)
        }

        binding.rvStudentList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = studentAdapter
        }
    }

    private fun loadStudentList() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvStudentList.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate.now().format(dateFormatter)
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        // 使用 Firebase Flow 实时监听学生签到名单
        lifecycleScope.launch {
            repository.getStudentAttendanceList(
                teacherId,
                course.id,  // ✅ 修复：直接使用 String ID
                today
            ).collect { students ->
                // 每当有学生签到，这里会自动收到更新
                binding.progressBar.visibility = View.GONE

                if (students.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvStudentList.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvStudentList.visibility = View.VISIBLE
                    studentAdapter.submitList(students)
                }
            }
        }
    }

    private fun unlockSession() {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate.now().format(dateFormatter)
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        lifecycleScope.launch {
            val result = repository.unlockSession(teacherId, course.id, today)  // ✅ 修复：直接使用 String ID

            result.onSuccess {
                Toast.makeText(
                    requireContext(),
                    "Session unlocked successfully",
                    Toast.LENGTH_SHORT
                ).show()
                course.signInStatus = SignInStatus.UNLOCKED
                updateSessionStatusUI()

                // 🔴 修复：通知父界面刷新课程列表
                onSessionStatusChanged?.invoke()

                // 添加调试日志
                android.util.Log.d(
                    "CourseManagement",
                    "✅ Session ${course.id} unlocked, Firebase updated at sessions/${course.id}_$today"
                )
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Failed to unlock: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()

                android.util.Log.e("CourseManagement", "❌ Failed to unlock: ${error.message}", error)
            }
        }
    }

    private fun lockSession() {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate.now().format(dateFormatter)
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        lifecycleScope.launch {
            val result = repository.lockSession(teacherId, course.id, today)  // ✅ 修复：直接使用 String ID

            result.onSuccess {
                Toast.makeText(
                    requireContext(),
                    "Session locked successfully",
                    Toast.LENGTH_SHORT
                ).show()
                course.signInStatus = SignInStatus.LOCKED
                updateSessionStatusUI()

                // 🔴 修复：通知父界面刷新课程列表
                onSessionStatusChanged?.invoke()

                android.util.Log.d(
                    "CourseManagement",
                    "✅ Session ${course.id} locked, Firebase updated"
                )
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Failed to lock: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()

                android.util.Log.e("CourseManagement", "❌ Failed to lock: ${error.message}", error)
            }
        }
    }

    private fun markAttendance(student: StudentAttendance, status: AttendanceStatus) {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val today = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate.now().format(dateFormatter)
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        lifecycleScope.launch {
            val result = repository.markAttendance(
                teacherId = teacherId,
                studentUid = student.studentId,  // 🔴 修复：使用 studentUid 参数名
                courseScheduleId = course.id,
                date = today,
                status = status.name,
                studentName = student.studentName,
                matricNumber = student.matricNumber,
                email = student.email
            )

            result.onSuccess { isFirstMark ->
                val message = if (isFirstMark) {
                    "Marked ${student.studentName} as ${status.name}\n📊 Session #${course.totalClasses + 1} started"
                } else {
                    "Marked ${student.studentName} as ${status.name}"
                }
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()
                // Firebase 会自动通知所有监听者，无需手动刷新
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Failed to mark attendance: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COURSE = "course"

        fun newInstance(course: Course): CourseManagementBottomSheet {
            return CourseManagementBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_COURSE, course)
                }
            }
        }
    }
}
