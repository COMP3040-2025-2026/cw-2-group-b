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
            
            tvStatus.text = booking.status.uppercase()

            try {
                val bookingDate = LocalDate.parse(booking.bookingDate)
                val bookingTime = LocalTime.of(booking.timeSlot, 0)
                val bookingDateTime = LocalDateTime.of(bookingDate, bookingTime)
                
                // [关键修复] 统一使用马来西亚时区，防止时间判断出错
                val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
                val now = LocalDateTime.now(zoneId)
                
                // 定义关键时间点
                // 1. 取消截止时间：开始前1小时
                val cancelDeadline = bookingDateTime.minusHours(1)
                // 2. 预定结束时间：开始后1小时（假设每场1小时）
                val endTime = bookingDateTime.plusHours(1)

                if (now.isAfter(endTime)) {
                    // --- 情况A: 预定已结束 (过期) ---
                    // 按钮变为 "Delete"，允许点击删除记录
                    btnCancel.isEnabled = true
                    btnCancel.text = "Delete"
                    btnCancel.setTextColor(Color.RED) // 或者你可以设为黑色/灰色，视设计而定
                    btnCancel.setOnClickListener {
                        onCancelClick(booking)
                    }
                } else if (now.isAfter(cancelDeadline)) {
                    // --- 情况B: 临近开始 或 正在进行中 (不可取消) ---
                    // 按钮变灰，禁用，显示提示文字 (如 Locked 或 Started)
                    btnCancel.isEnabled = false
                    btnCancel.text = "Locked" // 根据你的描述，这里不再显示 Non-cancellable，改为 Locked 或其他状态
                    btnCancel.setTextColor(Color.GRAY)
                } else {
                    // --- 情况C: 还在可取消范围内 ---
                    btnCancel.isEnabled = true
                    btnCancel.text = "Cancel"
                    btnCancel.setTextColor(Color.RED)
                    btnCancel.setOnClickListener {
                        onCancelClick(booking)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                btnCancel.isEnabled = true
            }
        }
    }
}