package com.nottingham.mynottingham.ui.instatt

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.data.repository.InstattRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
 * - 在进入 INSTATT 模块时并行加载所有数据
 * - HOME、CALENDAR、STATISTICS 都直接使用预加载的数据，无需等待
 */
class InstattViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstattRepository()

    // 今日课程数据（HOME tab）
    private val _todayCourses = MutableLiveData<List<Course>>()
    val todayCourses: LiveData<List<Course>> = _todayCourses

    // 周课表数据（CALENDAR tab）
    private val _weekCourses = MutableLiveData<List<DayWithCourses>>()
    val weekCourses: LiveData<List<DayWithCourses>> = _weekCourses

    // 统计数据 - 所有唯一课程（STATISTICS tab）
    private val _allCourses = MutableLiveData<List<Course>>()
    val allCourses: LiveData<List<Course>> = _allCourses

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 兼容旧代码
    private val _isWeekCoursesLoading = MutableLiveData<Boolean>()
    val isWeekCoursesLoading: LiveData<Boolean> = _isWeekCoursesLoading

    // 记录是否已经加载过（避免重复加载）
    private var hasLoadedAllData = false
    private var hasLoadedWeekCourses = false

    // 当前日期信息
    private var currentDate: String = ""
    private var currentDayOfWeek: DayOfWeek = DayOfWeek.MONDAY

    /**
     * 预加载所有数据（推荐使用）
     * 并行加载今日课程、周课表、统计数据
     */
    fun preloadAllData(studentId: String) {
        // 如果已经加载过，不重复加载
        if (hasLoadedAllData && _todayCourses.value?.isNotEmpty() == true) {
            Log.d("InstattViewModel", "📋 All data already loaded, skipping")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isWeekCoursesLoading.value = true
                Log.d("InstattViewModel", "📥 Preloading all INSTATT data for student: $studentId")

                // 计算当前日期信息
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
                val today = LocalDate.now()
                currentDate = today.format(dateFormatter)
                currentDayOfWeek = getDayOfWeek(today)

                val weekDates = calculateCurrentWeekDates()
                val allCoursesMap = mutableMapOf<String, Course>()
                val tempDaysWithCourses = mutableListOf<DayWithCourses>()
                var todayCoursesResult: List<Course> = emptyList()

                // 并行加载每天的课程
                val deferredResults = weekDates.map { (dayOfWeek, date) ->
                    async {
                        val result = repository.getStudentCourses(studentId, date)
                        Triple(dayOfWeek, date, result.getOrNull() ?: emptyList())
                    }
                }

                // 等待所有请求完成
                val results = deferredResults.awaitAll()

                // 处理结果
                for ((dayOfWeek, date, allDayCourses) in results) {
                    val courses = allDayCourses.filter { it.dayOfWeek == dayOfWeek }

                    // 收集今日课程
                    if (dayOfWeek == currentDayOfWeek) {
                        todayCoursesResult = courses
                    }

                    // 收集所有唯一课程（用于统计）
                    courses.forEach { course ->
                        allCoursesMap[course.courseCode] = course
                    }

                    tempDaysWithCourses.add(
                        DayWithCourses(
                            day = dayOfWeek,
                            date = date,
                            courses = courses,
                            isExpanded = false
                        )
                    )
                }

                // 更新所有数据
                _todayCourses.value = todayCoursesResult
                _weekCourses.value = tempDaysWithCourses
                _allCourses.value = allCoursesMap.values.toList()

                hasLoadedAllData = true
                hasLoadedWeekCourses = true

                Log.d("InstattViewModel", "✅ All data preloaded: today=${todayCoursesResult.size}, week=${tempDaysWithCourses.size} days, unique=${allCoursesMap.size} courses")
            } catch (e: Exception) {
                Log.e("InstattViewModel", "❌ Error preloading data", e)
                _todayCourses.value = emptyList()
                _weekCourses.value = emptyList()
                _allCourses.value = emptyList()
            } finally {
                _isLoading.value = false
                _isWeekCoursesLoading.value = false
            }
        }
    }

    /**
     * 获取当前星期几
     */
    private fun getDayOfWeek(date: LocalDate): DayOfWeek {
        return when (date.dayOfWeek) {
            JavaDayOfWeek.MONDAY -> DayOfWeek.MONDAY
            JavaDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            JavaDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            JavaDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            JavaDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            JavaDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            JavaDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
    }

    /**
     * 预加载周课表数据（兼容旧代码）
     * 在进入 INSTATT 模块时调用
     */
    fun preloadWeekCourses(studentId: String) {
        // 直接调用 preloadAllData，统一处理
        preloadAllData(studentId)
    }

    /**
     * 强制刷新所有数据
     */
    fun refreshAllData(studentId: String) {
        hasLoadedAllData = false
        hasLoadedWeekCourses = false
        preloadAllData(studentId)
    }

    /**
     * 强制刷新周课表数据（兼容旧代码）
     */
    fun refreshWeekCourses(studentId: String) {
        refreshAllData(studentId)
    }

    /**
     * 获取当前日期
     */
    fun getCurrentDate(): String = currentDate

    /**
     * 获取当前星期
     */
    fun getCurrentDayOfWeek(): DayOfWeek = currentDayOfWeek

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
