package com.nottingham.mynottingham.ui.booking

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.database.entities.BookingEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId // [新增] 导入 ZoneId

class SportsBookingAdapter(
    private var bookings: List<BookingEntity>,
    private val onCancelClick: (BookingEntity) -> Unit
) : RecyclerView.Adapter<SportsBookingAdapter.BookingViewHolder>() {

    fun updateData(newBookings: List<BookingEntity>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sports_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount() = bookings.size

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFacilityName: TextView = itemView.findViewById(R.id.tv_facility_name)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_date_time)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val btnCancel: Button = itemView.findViewById(R.id.btn_cancel)

        fun bind(booking: BookingEntity) {
            tvFacilityName.text = booking.facilityName

            val timeString = String.format("%02d:00", booking.timeSlot)
            tvDateTime.text = "${booking.bookingDate} at $timeString"

            // 如果已取消，显示 CANCELLED 状态和 Delete 按钮
            if (booking.status.uppercase() == "CANCELLED") {
                tvStatus.text = "CANCELLED"
                tvStatus.setTextColor(Color.RED)
                btnCancel.visibility = View.VISIBLE
                btnCancel.isEnabled = true
                btnCancel.text = "Delete"
                btnCancel.setTextColor(Color.GRAY)
                btnCancel.setOnClickListener {
                    onCancelClick(booking)
                }
                return
            }

            try {
                val bookingDate = LocalDate.parse(booking.bookingDate)
                val bookingTime = LocalTime.of(booking.timeSlot, 0)
                val bookingDateTime = LocalDateTime.of(bookingDate, bookingTime)

                val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
                val now = LocalDateTime.now(zoneId)

                // 预定结束时间：开始后1小时
                val endTime = bookingDateTime.plusHours(1)

                when {
                    now.isAfter(endTime) -> {
                        // 预定已结束 - 显示 Finished，按钮变为 Delete
                        tvStatus.text = "FINISHED"
                        tvStatus.setTextColor(Color.parseColor("#4CAF50")) // 绿色
                        btnCancel.visibility = View.VISIBLE
                        btnCancel.isEnabled = true
                        btnCancel.text = "Delete"
                        btnCancel.setTextColor(Color.GRAY)
                        btnCancel.setOnClickListener {
                            onCancelClick(booking)
                        }
                    }
                    now.isAfter(bookingDateTime) -> {
                        // 正在进行中 - 显示 In Progress，隐藏按钮
                        tvStatus.text = "IN PROGRESS"
                        tvStatus.setTextColor(Color.parseColor("#FF9800")) // 橙色
                        btnCancel.visibility = View.GONE
                    }
                    else -> {
                        // 未开始 - 显示 Confirmed，按钮为 Cancel
                        tvStatus.text = "CONFIRMED"
                        tvStatus.setTextColor(Color.parseColor("#4CAF50")) // 绿色
                        btnCancel.visibility = View.VISIBLE
                        btnCancel.isEnabled = true
                        btnCancel.text = "Cancel"
                        btnCancel.setTextColor(Color.RED)
                        btnCancel.setOnClickListener {
                            onCancelClick(booking)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tvStatus.text = "CONFIRMED"
                btnCancel.visibility = View.VISIBLE
            }
        }
    }
}