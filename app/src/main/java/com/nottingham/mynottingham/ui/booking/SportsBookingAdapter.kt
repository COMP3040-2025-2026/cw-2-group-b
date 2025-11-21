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
import java.time.format.DateTimeFormatter

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
            
            // 格式化时间显示: 10 -> "10:00"
            val timeString = String.format("%02d:00", booking.timeSlot)
            tvDateTime.text = "${booking.bookingDate} at $timeString"
            
            tvStatus.text = booking.status.uppercase()

            // --- 核心逻辑：检查是否可以取消 ---
            try {
                val bookingDate = LocalDate.parse(booking.bookingDate) // 假设格式 yyyy-MM-dd
                val bookingTime = LocalTime.of(booking.timeSlot, 0)    // 10:00
                val bookingDateTime = LocalDateTime.of(bookingDate, bookingTime)
                
                val now = LocalDateTime.now()
                
                // 截止时间 = 预定开始时间 - 1小时
                val cancelDeadline = bookingDateTime.minusHours(1)

                // 如果当前时间已经过了截止时间 (即在预定前1小时内，或已经开始)
                if (now.isAfter(cancelDeadline)) {
                    btnCancel.isEnabled = false
                    btnCancel.text = "Non-cancellable"
                    btnCancel.setTextColor(Color.GRAY)
                } else {
                    btnCancel.isEnabled = true
                    btnCancel.text = "Cancel"
                    btnCancel.setTextColor(Color.RED)
                    btnCancel.setOnClickListener {
                        onCancelClick(booking)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 如果解析日期出错，默认允许或禁止，这里保守处理允许
                btnCancel.isEnabled = true
            }
        }
    }
}