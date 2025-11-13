package com.nottingham.mynottingham.ui.instatt

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.SignInStatus
import com.nottingham.mynottingham.data.model.TodayClassStatus
import com.nottingham.mynottingham.databinding.ItemTodayClassBinding

class TodayClassAdapter(
    private val courses: List<Course>,
    private val onSignInClick: ((Course) -> Unit)? = null
) : RecyclerView.Adapter<TodayClassAdapter.TodayClassViewHolder>() {

    inner class TodayClassViewHolder(private val binding: ItemTodayClassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            // Course name
            binding.tvCourseName.text = course.courseName

            // Course code and semester
            binding.tvCourseCode.text = "${course.courseCode} ${course.semester}"

            // Time - extract just the time without AM/PM for cleaner display
            val startTime = course.startTime?.let { extractTime(it) } ?: "00:00"
            val endTime = course.endTime?.let { extractTime(it) } ?: "00:00"
            binding.tvStartTime.text = startTime
            binding.tvEndTime.text = endTime

            // Location
            binding.tvLocation.text = course.location ?: "TBA"

            // Course type
            binding.tvCourseType.text = course.courseType.displayName

            // Status indicator (vertical line on left of course name)
            // Attendance indicator with custom icons based on sign-in status

            // Determine what icon to show based on attendance status first, then sign-in status
            // Priority: If student has been marked (manually or via sign-in), always show that status
            when {
                // Priority 1: Student has been marked as attended (either manually or via sign-in)
                course.hasStudentSigned -> {
                    binding.viewStatusLine.setBackgroundColor(Color.parseColor("#4CAF50"))
                    binding.ivAttendanceIcon.isVisible = true
                    binding.ivAttendanceIcon.setImageResource(R.drawable.ic_attendance_check)
                    binding.ivAttendanceIcon.isClickable = false
                }
                // Priority 2: Session is unlocked and available for sign-in
                course.signInStatus == SignInStatus.UNLOCKED -> {
                    binding.viewStatusLine.setBackgroundColor(Color.parseColor("#2196F3"))
                    binding.ivAttendanceIcon.isVisible = true
                    binding.ivAttendanceIcon.setImageResource(R.drawable.ic_sign_pencil)
                    binding.ivAttendanceIcon.isClickable = true
                    binding.ivAttendanceIcon.setOnClickListener {
                        onSignInClick?.invoke(course)
                    }
                }
                // Priority 3: Session is closed and student didn't attend
                course.signInStatus == SignInStatus.CLOSED -> {
                    binding.viewStatusLine.setBackgroundColor(Color.parseColor("#F44336"))
                    binding.ivAttendanceIcon.isVisible = true
                    binding.ivAttendanceIcon.setImageResource(R.drawable.ic_attendance_cross)
                    binding.ivAttendanceIcon.isClickable = false
                }
                // Priority 4: Session is locked (default state)
                else -> {
                    binding.viewStatusLine.setBackgroundColor(Color.parseColor("#9E9E9E"))
                    binding.ivAttendanceIcon.isVisible = true
                    binding.ivAttendanceIcon.setImageResource(R.drawable.ic_sign_locked)
                    binding.ivAttendanceIcon.isClickable = false
                }
            }
        }

        private fun extractTime(timeString: String): String {
            // Convert "09:00 AM" to "09:00"
            return timeString.split(" ")[0]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodayClassViewHolder {
        val binding = ItemTodayClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TodayClassViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodayClassViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size
}
