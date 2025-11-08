package com.nottingham.mynottingham.ui.shuttle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.DayType
import com.nottingham.mynottingham.data.model.RouteSchedule
import com.nottingham.mynottingham.data.model.ShuttleRoute
import com.nottingham.mynottingham.databinding.ItemShuttleRouteBinding

/**
 * Adapter for displaying shuttle routes in RecyclerView
 */
class ShuttleRouteAdapter(
    private val dayType: DayType,
    private val onRouteClick: (ShuttleRoute) -> Unit = {}
) : ListAdapter<ShuttleRoute, ShuttleRouteAdapter.RouteViewHolder>(RouteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemShuttleRouteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position), dayType)
    }

    inner class RouteViewHolder(
        private val binding: ItemShuttleRouteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(route: ShuttleRoute, dayType: DayType) {
            binding.apply {
                // Set route badge
                tvRouteBadge.text = route.routeId

                // Set route badge background based on route ID
                val badgeDrawable = when (route.routeId) {
                    "A" -> R.drawable.bg_route_badge_a
                    "B" -> R.drawable.bg_route_badge_b
                    "C1", "C2" -> R.drawable.bg_route_badge_c
                    "D" -> R.drawable.bg_route_badge_d
                    "E1", "E2" -> R.drawable.bg_route_badge_e
                    "G" -> R.drawable.bg_route_badge_g
                    else -> R.drawable.bg_route_badge_gradient
                }
                tvRouteBadge.setBackgroundResource(badgeDrawable)

                // Set route name and description
                tvRouteName.text = route.routeName
                tvRouteDescription.text = route.description

                // Get schedule for the day type
                val schedule = when (dayType) {
                    DayType.WEEKDAY -> route.weekdaySchedule
                    DayType.FRIDAY -> route.fridaySchedule
                    DayType.WEEKEND -> route.weekendSchedule
                }

                if (schedule != null) {
                    // Show schedule
                    layoutSchedule.visibility = View.VISIBLE
                    tvNoService.visibility = View.GONE

                    // Departure times
                    if (schedule.departureFromCampus.isNotEmpty()) {
                        cardDeparture.visibility = View.VISIBLE
                        tvDepartureTimes.text = schedule.departureFromCampus.joinToString(", ")
                    } else {
                        cardDeparture.visibility = View.GONE
                    }

                    // Return times
                    if (schedule.returnToCampus.isNotEmpty()) {
                        cardReturn.visibility = View.VISIBLE
                        tvReturnTimes.text = schedule.returnToCampus.joinToString(", ")
                    } else {
                        cardReturn.visibility = View.GONE
                    }

                    // Special note
                    if (!route.specialNote.isNullOrEmpty()) {
                        cardSpecialNote.visibility = View.VISIBLE
                        tvSpecialNote.text = route.specialNote
                    } else {
                        cardSpecialNote.visibility = View.GONE
                    }
                } else {
                    // No service on this day
                    layoutSchedule.visibility = View.VISIBLE
                    cardDeparture.visibility = View.GONE
                    cardReturn.visibility = View.GONE
                    cardSpecialNote.visibility = View.GONE
                    tvNoService.visibility = View.VISIBLE
                }

                // Set click listener
                root.setOnClickListener {
                    onRouteClick(route)
                }
            }
        }
    }

    class RouteDiffCallback : DiffUtil.ItemCallback<ShuttleRoute>() {
        override fun areItemsTheSame(oldItem: ShuttleRoute, newItem: ShuttleRoute): Boolean {
            return oldItem.routeId == newItem.routeId
        }

        override fun areContentsTheSame(oldItem: ShuttleRoute, newItem: ShuttleRoute): Boolean {
            return oldItem == newItem
        }
    }
}
