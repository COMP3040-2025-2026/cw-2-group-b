package com.nottingham.mynottingham.ui.instatt

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.data.repository.InstattRepository
import kotlinx.coroutines.launch
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

/**
 * InstattViewModel - 共享 ViewModel 用于预加载课程数据
 *
 * 优化策略：
 * - 在进入 INSTATT 模块时就开始加载周课表数据
 * - CalendarFragment 直接使用预加载的数据，无需等待
 */
class InstattViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstattRepository()

    // 周课表数据
    private val _weekCourses = MutableLiveData<List<DayWithCourses>>()
    val weekCourses: LiveData<List<DayWithCourses>> = _weekCourses

    // 加载状态
    private val _isWeekCoursesLoading = MutableLiveData<Boolean>()
    val isWeekCoursesLoading: LiveData<Boolean> = _isWeekCoursesLoading

    // 记录是否已经加载过（避免重复加载）
    private var hasLoadedWeekCourses = false

    /**
     * 预加载周课表数据
     * 在进入 INSTATT 模块时调用
     */
    fun preloadWeekCourses(studentId: String) {
        // 如果已经加载过，不重复加载
        if (hasLoadedWeekCourses && _weekCourses.value?.isNotEmpty() == true) {
            Log.d("InstattViewModel", "📋 Week courses already loaded, skipping")
            return
        }

        viewModelScope.launch {
            try {
                _isWeekCoursesLoading.value = true
                Log.d("InstattViewModel", "📥 Preloading week courses for student: $studentId")

                val weekDates = calculateCurrentWeekDates()
                val tempDaysWithCourses = mutableListOf<DayWithCourses>()

                for ((dayOfWeek, date) in weekDates) {
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

                _weekCourses.value = tempDaysWithCourses
                hasLoadedWeekCourses = true
                Log.d("InstattViewModel", "✅ Week courses preloaded successfully")
            } catch (e: Exception) {
                Log.e("InstattViewModel", "❌ Error preloading week courses", e)
                _weekCourses.value = emptyList()
            } finally {
                _isWeekCoursesLoading.value = false
            }
        }
    }

    /**
     * 强制刷新周课表数据
     */
    fun refreshWeekCourses(studentId: String) {
        hasLoadedWeekCourses = false
        preloadWeekCourses(studentId)
    }

    /**
     * 更新某一天的展开状态
     */
    fun toggleDayExpansion(position: Int) {
        _weekCourses.value?.let { days ->
            val updatedDays = days.toMutableList()
            if (position in updatedDays.indices) {
                updatedDays[position] = updatedDays[position].copy(
                    isExpanded = !updatedDays[position].isExpanded
                )
                _weekCourses.value = updatedDays
            }
        }
    }

    /**
     * 计算当前周每天的日期 (Monday-Sunday)
     */
    private fun calculateCurrentWeekDates(): Map<DayOfWeek, String> {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
        val weekDates = mutableMapOf<DayOfWeek, String>()

        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(JavaDayOfWeek.MONDAY))

        weekDates[DayOfWeek.MONDAY] = monday.format(dateFormatter)
        weekDates[DayOfWeek.TUESDAY] = monday.plusDays(1).format(dateFormatter)
        weekDates[DayOfWeek.WEDNESDAY] = monday.plusDays(2).format(dateFormatter)
        weekDates[DayOfWeek.THURSDAY] = monday.plusDays(3).format(dateFormatter)
        weekDates[DayOfWeek.FRIDAY] = monday.plusDays(4).format(dateFormatter)
        weekDates[DayOfWeek.SATURDAY] = monday.plusDays(5).format(dateFormatter)
        weekDates[DayOfWeek.SUNDAY] = monday.plusDays(6).format(dateFormatter)

        return weekDates
    }
}
