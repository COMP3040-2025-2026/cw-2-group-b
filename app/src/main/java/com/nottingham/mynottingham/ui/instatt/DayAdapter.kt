package com.nottingham.mynottingham.ui.instatt

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
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

    // 记录每个位置的上一次展开状态
    private val previousExpandStates = mutableMapOf<Int, Boolean>()

    inner class DayViewHolder(private val binding: ItemDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var coursesAdapter: DayCoursesAdapter? = null
        private var currentAnimator: ValueAnimator? = null

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

                // Setup click listener for header
                layoutDayHeader.setOnClickListener {
                    onToggleExpand(position)
                }

                // 获取需要动画的内容视图
                val contentView = if (dayWithCourses.courses.isEmpty()) tvNoCourses else rvCourses

                // 每次绑定时都更新 adapter（因为 ViewHolder 会被复用）
                if (dayWithCourses.courses.isNotEmpty()) {
                    coursesAdapter = DayCoursesAdapter(dayWithCourses.courses)
                    rvCourses.adapter = coursesAdapter
                }

                // 检查是否需要动画（状态变化时）- 使用 adapter 级别的状态跟踪
                val previousState = previousExpandStates[position]
                val shouldAnimate = previousState != null && previousState != dayWithCourses.isExpanded
                previousExpandStates[position] = dayWithCourses.isExpanded

                if (shouldAnimate) {
                    // 取消之前的动画
                    currentAnimator?.cancel()

                    // 箭头旋转动画
                    val targetRotation = if (dayWithCourses.isExpanded) 180f else 0f
                    ivExpandIcon.animate()
                        .rotation(targetRotation)
                        .setDuration(250)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()

                    // 展开/收起动画
                    if (dayWithCourses.isExpanded) {
                        expandView(contentView)
                    } else {
                        collapseView(contentView)
                    }
                } else {
                    // 初始状态，不需要动画
                    ivExpandIcon.rotation = if (dayWithCourses.isExpanded) 180f else 0f

                    if (dayWithCourses.isExpanded) {
                        if (dayWithCourses.courses.isEmpty()) {
                            rvCourses.isVisible = false
                            tvNoCourses.isVisible = true
                        } else {
                            rvCourses.isVisible = true
                            tvNoCourses.isVisible = false
                        }
                    } else {
                        rvCourses.isVisible = false
                        tvNoCourses.isVisible = false
                    }
                }
            }
        }

        /**
         * 平滑展开视图
         */
        private fun expandView(view: View) {
            view.visibility = View.VISIBLE
            view.alpha = 0f

            // 测量视图高度
            view.measure(
                View.MeasureSpec.makeMeasureSpec(binding.root.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val targetHeight = view.measuredHeight

            // 设置初始高度为 0
            view.layoutParams.height = 0
            view.requestLayout()

            currentAnimator = ValueAnimator.ofInt(0, targetHeight).apply {
                duration = 250
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Int
                    view.layoutParams.height = value
                    view.alpha = animation.animatedFraction
                    view.requestLayout()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                })
                start()
            }
        }

        /**
         * 平滑收起视图
         */
        private fun collapseView(view: View) {
            val initialHeight = view.height

            currentAnimator = ValueAnimator.ofInt(initialHeight, 0).apply {
                duration = 250
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Int
                    view.layoutParams.height = value
                    view.alpha = 1f - animation.animatedFraction
                    view.requestLayout()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        view.visibility = View.GONE
                        view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                })
                start()
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
        previousExpandStates.clear()
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
