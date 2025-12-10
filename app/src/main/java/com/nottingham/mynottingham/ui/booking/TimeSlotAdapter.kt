package com.nottingham.mynottingham.ui.booking

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.Booking

class TimeSlotAdapter(
    private val onSlotSelected: (Int?) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeViewHolder>() {

    // Define time slots from 9:00 to 21:00 (integer representation)
    private val allSlots = (9..21).toList()
    private var bookedSlots: List<Booking> = emptyList()
    private var selectedSlot: Int? = null

    fun updateBookings(bookings: List<Booking>) {
        this.bookedSlots = bookings
        this.selectedSlot = null // Reset selection when date changes
        onSlotSelected(null)
        notifyDataSetChanged()
    }

    inner class TimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tv_time_slot)

        fun bind(hour: Int) {
            tvTime.text = String.format("%02d:00", hour)

            // Check if this time slot is in bookedSlots
            val bookingInfo = bookedSlots.find { it.timeSlot == hour }
            val isBooked = bookingInfo != null

            if (isBooked) {
                // State: Booked -> grey, not clickable
                tvTime.setBackgroundResource(R.drawable.bg_time_slot_booked) // Use drawable
                tvTime.setTextColor(Color.GRAY)
                tvTime.isEnabled = false
                tvTime.text = "${String.format("%02d:00", hour)}\n(${bookingInfo?.userName})" // Show booker's name
                tvTime.textSize = 12f
            } else if (selectedSlot == hour) {
                // State: Selected -> purple
                tvTime.setBackgroundResource(R.drawable.bg_time_slot_selected) // Use drawable
                tvTime.setTextColor(Color.WHITE)
                tvTime.isEnabled = true
            } else {
                // State: Available -> default/white with border
                tvTime.setBackgroundResource(R.drawable.bg_time_slot_available) // Use drawable
                tvTime.setTextColor(Color.BLACK)
                tvTime.isEnabled = true
            }

            itemView.setOnClickListener {
                if (!isBooked) {
                    selectedSlot = hour
                    onSlotSelected(hour)
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot_grid, parent, false)
        return TimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.bind(allSlots[position])
    }

    override fun getItemCount() = allSlots.size
}
