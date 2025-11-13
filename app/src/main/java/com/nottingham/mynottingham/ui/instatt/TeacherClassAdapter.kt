package com.nottingham.mynottingham.ui.instatt

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.SignInStatus
import com.nottingham.mynottingham.databinding.ItemTeacherClassBinding

class TeacherClassAdapter(
    private val courses: List<Course>,
    private val onToggleSignIn: (Course) -> Unit,
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<TeacherClassAdapter.TeacherClassViewHolder>() {

    inner class TeacherClassViewHolder(private val binding: ItemTeacherClassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            // Course name
            binding.tvCourseName.text = course.courseName

            // Course code and semester
            binding.tvCourseCode.text = "${course.courseCode} ${course.semester}"

            // Time
            val startTime = course.startTime?.split(" ")?.get(0) ?: "00:00"
            val endTime = course.endTime?.split(" ")?.get(0) ?: "00:00"
            binding.tvStartTime.text = startTime
            binding.tvEndTime.text = endTime

            // Location
            binding.tvLocation.text = course.location ?: "TBA"

            // Course type
            binding.tvCourseType.text = course.courseType.displayName

            // Status line color based on sign-in status
            when (course.signInStatus) {
                SignInStatus.LOCKED -> {
                    binding.viewStatusLine.setBackgroundColor(Color.parseColor("#9E9E9E"))
                    binding.btnToggleSignin.text = "Unlock"
                    binding.btnToggleSignin.isEnabled = true
                }
                SignInStatus.UNLOCKED -> {
                    binding.viewStatusLine.setBackgroundColor(Color.parseColor("#4CAF50"))
                    binding.btnToggleSignin.text = "Lock"
                    binding.btnToggleSignin.isEnabled = true
                }
                SignInStatus.CLOSED -> {
                    binding.viewStatusLine.setBackgroundColor(Color.parseColor("#F44336"))
                    binding.btnToggleSignin.text = "Closed"
                    binding.btnToggleSignin.isEnabled = false
                }
            }

            // Toggle button click
            binding.btnToggleSignin.setOnClickListener {
                onToggleSignIn(course)
            }

            // Card click to open course management dialog
            binding.root.setOnClickListener {
                onCourseClick(course)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherClassViewHolder {
        val binding = ItemTeacherClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TeacherClassViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeacherClassViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size
}
