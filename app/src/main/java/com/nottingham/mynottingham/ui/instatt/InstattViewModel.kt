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
 * InstattViewModel - Shared ViewModel for preloading course data
 *
 * Optimization strategy:
 * - Parallel load all data when entering INSTATT module
 * - HOME, CALENDAR, STATISTICS use preloaded data directly without waiting
 */
class InstattViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstattRepository()

    // Today's course data (HOME tab)
    private val _todayCourses = MutableLiveData<List<Course>>()
    val todayCourses: LiveData<List<Course>> = _todayCourses

    // Weekly course data (CALENDAR tab)
    private val _weekCourses = MutableLiveData<List<DayWithCourses>>()
    val weekCourses: LiveData<List<DayWithCourses>> = _weekCourses

    // Statistics data - all unique courses (STATISTICS tab)
    private val _allCourses = MutableLiveData<List<Course>>()
    val allCourses: LiveData<List<Course>> = _allCourses

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Backward compatibility with old code
    private val _isWeekCoursesLoading = MutableLiveData<Boolean>()
    val isWeekCoursesLoading: LiveData<Boolean> = _isWeekCoursesLoading

    // Flag to track if data has been loaded (prevent duplicate loading)
    private var hasLoadedAllData = false
    private var hasLoadedWeekCourses = false

    // Current date information
    private var currentDate: String = ""
    private var currentDayOfWeek: DayOfWeek = DayOfWeek.MONDAY

    /**
     * Preload all data (recommended)
     * Parallel load today's courses, weekly schedule, and statistics data
     */
    fun preloadAllData(studentId: String) {
        // Skip if already loaded
        if (hasLoadedAllData && _todayCourses.value?.isNotEmpty() == true) {
            Log.d("InstattViewModel", "All data already loaded, skipping")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isWeekCoursesLoading.value = true
                Log.d("InstattViewModel", "Preloading all INSTATT data for student: $studentId")

                // Calculate current date information
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
                val today = LocalDate.now()
                currentDate = today.format(dateFormatter)
                currentDayOfWeek = getDayOfWeek(today)

                val weekDates = calculateCurrentWeekDates()
                val allCoursesMap = mutableMapOf<String, Course>()
                val tempDaysWithCourses = mutableListOf<DayWithCourses>()
                var todayCoursesResult: List<Course> = emptyList()

                // Parallel load courses for each day
                val deferredResults = weekDates.map { (dayOfWeek, date) ->
                    async {
                        val result = repository.getStudentCourses(studentId, date)
                        Triple(dayOfWeek, date, result.getOrNull() ?: emptyList())
                    }
                }

                // Wait for all requests to complete
                val results = deferredResults.awaitAll()

                // Process results
                for ((dayOfWeek, date, allDayCourses) in results) {
                    val courses = allDayCourses.filter { it.dayOfWeek == dayOfWeek }

                    // Collect today's courses
                    if (dayOfWeek == currentDayOfWeek) {
                        todayCoursesResult = courses
                    }

                    // Collect all unique courses (for statistics)
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

                // Update all data
                _todayCourses.value = todayCoursesResult
                _weekCourses.value = tempDaysWithCourses
                _allCourses.value = allCoursesMap.values.toList()

                hasLoadedAllData = true
                hasLoadedWeekCourses = true

                Log.d("InstattViewModel", "All data preloaded: today=${todayCoursesResult.size}, week=${tempDaysWithCourses.size} days, unique=${allCoursesMap.size} courses")
            } catch (e: Exception) {
                Log.e("InstattViewModel", "Error preloading data", e)
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
     * Get current day of week
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
     * Preload weekly course data (backward compatibility)
     * Called when entering INSTATT module
     */
    fun preloadWeekCourses(studentId: String) {
        // Call preloadAllData directly for unified handling
        preloadAllData(studentId)
    }

    /**
     * Force refresh all data
     */
    fun refreshAllData(studentId: String) {
        hasLoadedAllData = false
        hasLoadedWeekCourses = false
        preloadAllData(studentId)
    }

    /**
     * Force refresh weekly course data (backward compatibility)
     */
    fun refreshWeekCourses(studentId: String) {
        refreshAllData(studentId)
    }

    /**
     * Get current date
     */
    fun getCurrentDate(): String = currentDate

    /**
     * Get current day of week
     */
    fun getCurrentDayOfWeek(): DayOfWeek = currentDayOfWeek

    /**
     * Toggle expansion state for a day
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
     * Calculate each day's date for current week (Monday-Sunday)
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
