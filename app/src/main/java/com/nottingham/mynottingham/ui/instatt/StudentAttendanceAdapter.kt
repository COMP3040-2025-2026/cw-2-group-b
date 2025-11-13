package com.nottingham.mynottingham.ui.instatt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.AttendanceStatus
import com.nottingham.mynottingham.data.model.StudentAttendance
import com.nottingham.mynottingham.databinding.ItemStudentAttendanceBinding
import java.text.SimpleDateFormat
import java.util.*

class StudentAttendanceAdapter(
    private val onStatusChanged: (StudentAttendance, AttendanceStatus) -> Unit
) : ListAdapter<StudentAttendance, StudentAttendanceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentAttendanceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onStatusChanged)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemStudentAttendanceBinding,
        private val onStatusChanged: (StudentAttendance, AttendanceStatus) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(student: StudentAttendance) {
            binding.apply {
                tvStudentName.text = student.studentName
                tvMatricNumber.text = student.matricNumber
                tvEmail.text = student.email

                // Display check-in time if available
                if (student.checkInTime != null) {
                    try {
                        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val displayFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val date = isoFormat.parse(student.checkInTime)
                        date?.let {
                            tvCheckInTime.text = "Signed in at ${displayFormat.format(it)}"
                            tvCheckInTime.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        tvCheckInTime.visibility = View.GONE
                    }
                } else {
                    tvCheckInTime.visibility = View.GONE
                }

                // Set status icon and label
                val status = student.attendanceStatus
                when (status) {
                    AttendanceStatus.PRESENT -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                        ivStatusIcon.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.success)
                        )
                        tvStatusLabel.text = "PRESENT"
                        tvStatusLabel.setTextColor(
                            ContextCompat.getColor(root.context, R.color.success)
                        )
                    }
                    AttendanceStatus.ABSENT -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_cancel)
                        ivStatusIcon.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.error)
                        )
                        tvStatusLabel.text = "ABSENT"
                        tvStatusLabel.setTextColor(
                            ContextCompat.getColor(root.context, R.color.error)
                        )
                    }
                    AttendanceStatus.LATE -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_access_time)
                        ivStatusIcon.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.warning)
                        )
                        tvStatusLabel.text = "LATE"
                        tvStatusLabel.setTextColor(
                            ContextCompat.getColor(root.context, R.color.warning)
                        )
                    }
                    AttendanceStatus.EXCUSED -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_info)
                        ivStatusIcon.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.info)
                        )
                        tvStatusLabel.text = "EXCUSED"
                        tvStatusLabel.setTextColor(
                            ContextCompat.getColor(root.context, R.color.info)
                        )
                    }
                    null -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_help_outline)
                        ivStatusIcon.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.text_hint)
                        )
                        tvStatusLabel.text = "NOT MARKED"
                        tvStatusLabel.setTextColor(
                            ContextCompat.getColor(root.context, R.color.text_hint)
                        )
                    }
                }

                // Status menu button
                btnStatusMenu.setOnClickListener {
                    showStatusMenu(student)
                }
            }
        }

        private fun showStatusMenu(student: StudentAttendance) {
            val popup = PopupMenu(binding.root.context, binding.btnStatusMenu)
            popup.menuInflater.inflate(R.menu.menu_attendance_status, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                val newStatus = when (item.itemId) {
                    R.id.action_mark_present -> AttendanceStatus.PRESENT
                    R.id.action_mark_absent -> AttendanceStatus.ABSENT
                    R.id.action_mark_late -> AttendanceStatus.LATE
                    R.id.action_mark_excused -> AttendanceStatus.EXCUSED
                    else -> return@setOnMenuItemClickListener false
                }
                onStatusChanged(student, newStatus)
                true
            }

            popup.show()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StudentAttendance>() {
        override fun areItemsTheSame(oldItem: StudentAttendance, newItem: StudentAttendance): Boolean {
            return oldItem.studentId == newItem.studentId
        }

        override fun areContentsTheSame(oldItem: StudentAttendance, newItem: StudentAttendance): Boolean {
            return oldItem == newItem
        }
    }
}
