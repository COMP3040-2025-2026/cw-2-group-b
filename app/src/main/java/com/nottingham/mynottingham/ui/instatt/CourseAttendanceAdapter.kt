package com.nottingham.mynottingham.ui.instatt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.databinding.ItemCourseAttendanceBinding

class CourseAttendanceAdapter(
    private val courses: List<Course>
) : RecyclerView.Adapter<CourseAttendanceAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(private val binding: ItemCourseAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.tvCourseName.text = course.courseName
            binding.tvCourseCode.text = "${course.courseCode} ${course.semester}"

            // Time
            val timeText = if (course.startTime != null && course.endTime != null) {
                "${course.startTime} - ${course.endTime}"
            } else {
                "Time not set"
            }
            binding.tvTime.text = timeText

            // Location
            binding.tvLocation.text = course.location ?: "Location not set"

            // Calculate attendance percentage
            val percentage = if (course.totalClasses > 0) {
                (course.attendedClasses.toFloat() / course.totalClasses * 100).toInt()
            } else {
                0
            }

            binding.progressAttendance.progress = percentage
            binding.tvAttendanceRatio.text = "${course.attendedClasses}/${course.totalClasses}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseAttendanceBinding.inflate(
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
}
