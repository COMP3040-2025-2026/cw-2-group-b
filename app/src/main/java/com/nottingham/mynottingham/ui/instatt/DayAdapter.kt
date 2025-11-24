package com.nottingham.mynottingham.ui.instatt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.DayOfWeek
import com.nottingham.mynottingham.databinding.ItemDayBinding

/**
 * Data class to hold day info with courses and expansion state
 */
data class DayWithCourses(
    val day: DayOfWeek,
    val date: String,  // Actual date for this day (e.g., "2025-11-11")
    val courses: List<Course>,
    var isExpanded: Boolean = false
)

class DayAdapter(
    private val daysWithCourses: MutableList<DayWithCourses>,
    private val onToggleExpand: (Int) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    inner class DayViewHolder(private val binding: ItemDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var coursesAdapter: DayCoursesAdapter? = null

        fun bind(dayWithCourses: DayWithCourses, position: Int) {
            binding.apply {
                // Set day name
                tvDayName.text = dayWithCourses.day.displayName

                // Set course count
                val courseCount = dayWithCourses.courses.size
                tvCourseCount.text = if (courseCount > 0) {
                    "$courseCount ${if (courseCount == 1) "class" else "classes"}"
                } else {
                    "No classes"
                }

                // Setup expand/collapse icon rotation
                ivExpandIcon.rotation = if (dayWithCourses.isExpanded) 180f else 0f

                // Setup click listener for header
                layoutDayHeader.setOnClickListener {
                    onToggleExpand(position)
                }

                // Show/hide courses list based on expansion state
                if (dayWithCourses.isExpanded) {
                    if (dayWithCourses.courses.isEmpty()) {
                        // Show empty state
                        rvCourses.isVisible = false
                        tvNoCourses.isVisible = true
                    } else {
                        // Show courses list
                        rvCourses.isVisible = true
                        tvNoCourses.isVisible = false

                        // Use TodayClassAdapter to display courses (no sign-in click handler for calendar view)
                        coursesAdapter = DayCoursesAdapter(dayWithCourses.courses)
                        rvCourses.adapter = coursesAdapter
                    }
                } else {
                    // Hide courses list
                    rvCourses.isVisible = false
                    tvNoCourses.isVisible = false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(daysWithCourses[position], position)
    }

    override fun getItemCount(): Int = daysWithCourses.size

    /**
     * Update the entire dataset
     */
    fun updateData(newData: List<DayWithCourses>) {
        daysWithCourses.clear()
        daysWithCourses.addAll(newData)
        notifyDataSetChanged()
    }
}

/**
 * Adapter for displaying courses within a day (uses composition instead of inheritance)
 */
class DayCoursesAdapter(
    private var courses: List<Course>
) : RecyclerView.Adapter<DayCoursesAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(private val binding: com.nottingham.mynottingham.databinding.ItemTodayClassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            // Reuse TodayClassAdapter's binding logic
            binding.apply {
                tvCourseName.text = course.courseName
                tvCourseCode.text = "${course.courseCode} ${course.semester}"

                val startTime = course.startTime?.split(" ")?.get(0) ?: "00:00"
                val endTime = course.endTime?.split(" ")?.get(0) ?: "00:00"
                tvStartTime.text = startTime
                tvEndTime.text = endTime

                tvLocation.text = course.location ?: "TBA"
                tvCourseType.text = course.courseType.displayName

                // Set status indicator (simplified version for calendar view)
                when (course.todayStatus) {
                    com.nottingham.mynottingham.data.model.TodayClassStatus.ATTENDED -> {
                        viewStatusLine.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                        ivAttendanceIcon.isVisible = true
                        ivAttendanceIcon.setImageResource(com.nottingham.mynottingham.R.drawable.ic_attendance_check)
                    }
                    com.nottingham.mynottingham.data.model.TodayClassStatus.MISSED -> {
                        viewStatusLine.setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
                        ivAttendanceIcon.isVisible = true
                        ivAttendanceIcon.setImageResource(com.nottingham.mynottingham.R.drawable.ic_attendance_cross)
                    }
                    com.nottingham.mynottingham.data.model.TodayClassStatus.IN_PROGRESS -> {
                        viewStatusLine.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
                        ivAttendanceIcon.isVisible = true
                        ivAttendanceIcon.setImageResource(com.nottingham.mynottingham.R.drawable.ic_sign_pencil)
                    }
                    else -> {
                        viewStatusLine.setBackgroundColor(android.graphics.Color.parseColor("#9E9E9E"))
                        ivAttendanceIcon.isVisible = true
                        ivAttendanceIcon.setImageResource(com.nottingham.mynottingham.R.drawable.ic_sign_locked)
                    }
                }
                // No click handler in calendar view
                ivAttendanceIcon.isClickable = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = com.nottingham.mynottingham.databinding.ItemTodayClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size

    fun updateCourses(newCourses: List<Course>) {
        courses = newCourses
        notifyDataSetChanged()
    }
}
