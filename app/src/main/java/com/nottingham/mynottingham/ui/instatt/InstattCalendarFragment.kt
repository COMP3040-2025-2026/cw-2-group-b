package com.nottingham.mynottingham.ui.instatt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.data.repository.InstattRepository
import com.nottingham.mynottingham.databinding.FragmentInstattCalendarBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

class InstattCalendarFragment : Fragment() {

    private var _binding: FragmentInstattCalendarBinding? = null
    private val binding get() = _binding!!

    private val repository = InstattRepository()
    private lateinit var tokenManager: TokenManager
    // 🔴 修复：将 studentId 从 Long 改为 String，以支持 Firebase UID
    private var studentId: String = ""

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

        // Initialize TokenManager and retrieve actual user ID
        tokenManager = TokenManager(requireContext())
        lifecycleScope.launch {
            // 🔴 修复：直接获取 String 类型的 Firebase UID，不要转换为 Long
            studentId = tokenManager.getUserId().first() ?: ""

            // 🔴 修复：检查是否为空字符串
            if (studentId.isEmpty()) {
                Toast.makeText(
                    context,
                    "User not logged in",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            setupDaysList()
        }
    }

    private fun setupDaysList() {
        // Initialize adapter with empty data
        adapter = DayAdapter(daysWithCourses) { position ->
            toggleDayExpansion(position)
        }
        binding.rvDays.adapter = adapter

        // Load courses for all days of the week
        loadWeekCourses()
    }

    private fun toggleDayExpansion(position: Int) {
        // Toggle expansion state
        daysWithCourses[position].isExpanded = !daysWithCourses[position].isExpanded

        // Notify adapter to update the view
        adapter.notifyItemChanged(position)
    }

    private fun loadWeekCourses() {
        lifecycleScope.launch {
            // Calculate dates for each day of current week
            val weekDates = calculateCurrentWeekDates()

            // Create DayWithCourses for each day
            val tempDaysWithCourses = mutableListOf<DayWithCourses>()

            for ((dayOfWeek, date) in weekDates) {
                // Fetch courses for this specific date
                val result = repository.getStudentCourses(studentId, date)

                val courses = result.getOrNull()?.filter { it.dayOfWeek == dayOfWeek } ?: emptyList()

                tempDaysWithCourses.add(
                    DayWithCourses(
                        day = dayOfWeek,
                        date = date,
                        courses = courses,
                        isExpanded = false
                    )
                )
            }

            // Update adapter data
            daysWithCourses.clear()
            daysWithCourses.addAll(tempDaysWithCourses)
            adapter.notifyDataSetChanged()
        }
    }

    /**
     * Calculate actual dates for each day of the current week (Monday-Sunday)
     * @return Map of DayOfWeek to date string (yyyy-MM-dd)
     */
    private fun calculateCurrentWeekDates(): Map<DayOfWeek, String> {
        val dateFormatter = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        } else {
            null
        }

        val weekDates = mutableMapOf<DayOfWeek, String>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Use Java 8 Time API for API 26+
            val today = LocalDate.now()

            // Get Monday of current week
            val monday = today.with(TemporalAdjusters.previousOrSame(JavaDayOfWeek.MONDAY))

            // Calculate dates for all 7 days
            weekDates[DayOfWeek.MONDAY] = monday.format(dateFormatter!!)
            weekDates[DayOfWeek.TUESDAY] = monday.plusDays(1).format(dateFormatter)
            weekDates[DayOfWeek.WEDNESDAY] = monday.plusDays(2).format(dateFormatter)
            weekDates[DayOfWeek.THURSDAY] = monday.plusDays(3).format(dateFormatter)
            weekDates[DayOfWeek.FRIDAY] = monday.plusDays(4).format(dateFormatter)
            weekDates[DayOfWeek.SATURDAY] = monday.plusDays(5).format(dateFormatter)
            weekDates[DayOfWeek.SUNDAY] = monday.plusDays(6).format(dateFormatter)
        } else {
            // Fallback for older API levels
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Set to Monday of current week
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

            weekDates[DayOfWeek.MONDAY] = sdf.format(calendar.time)

            calendar.add(Calendar.DAY_OF_WEEK, 1)
            weekDates[DayOfWeek.TUESDAY] = sdf.format(calendar.time)

            calendar.add(Calendar.DAY_OF_WEEK, 1)
            weekDates[DayOfWeek.WEDNESDAY] = sdf.format(calendar.time)

            calendar.add(Calendar.DAY_OF_WEEK, 1)
            weekDates[DayOfWeek.THURSDAY] = sdf.format(calendar.time)

            calendar.add(Calendar.DAY_OF_WEEK, 1)
            weekDates[DayOfWeek.FRIDAY] = sdf.format(calendar.time)

            calendar.add(Calendar.DAY_OF_WEEK, 1)
            weekDates[DayOfWeek.SATURDAY] = sdf.format(calendar.time)

            calendar.add(Calendar.DAY_OF_WEEK, 1)
            weekDates[DayOfWeek.SUNDAY] = sdf.format(calendar.time)
        }

        return weekDates
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
