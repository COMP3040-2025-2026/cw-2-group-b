package com.nottingham.mynottingham.ui.booking

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nottingham.mynottingham.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class DateAdapter(
    private val dates: List<LocalDate>,
    private val onDateSelected: (LocalDate) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private var selectedPosition = 0 // Default to today selected

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.card_date)
        val tvDayName: TextView = itemView.findViewById(R.id.tv_day_name)
        val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)

        fun bind(date: LocalDate, position: Int) {
            tvDayName.text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            tvDayNumber.text = date.dayOfMonth.toString()

            if (selectedPosition == position) {
                card.setCardBackgroundColor(Color.parseColor("#6200EE")) // Selected color (purple)
                tvDayName.setTextColor(Color.WHITE)
                tvDayNumber.setTextColor(Color.WHITE)
            } else {
                card.setCardBackgroundColor(Color.WHITE)
                tvDayName.setTextColor(Color.BLACK)
                tvDayNumber.setTextColor(Color.BLACK)
            }

            itemView.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                onDateSelected(date)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_card, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(dates[position], position)
    }

    override fun getItemCount() = dates.size
}