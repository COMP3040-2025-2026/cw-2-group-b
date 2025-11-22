package com.nottingham.mynottingham.ui.shuttle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.model.DayType
import com.nottingham.mynottingham.databinding.FragmentShuttleBinding

/**
 * Shuttle Bus Fragment - Display shuttle routes and schedules
 */
class ShuttleFragment : Fragment() {

    private var _binding: FragmentShuttleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShuttleViewModel by viewModels()
    private lateinit var adapter: ShuttleRouteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShuttleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Setup RecyclerView adapter; adapter internally manages expand/collapse
        adapter = ShuttleRouteAdapter(DayType.WEEKDAY)
        binding.recyclerRoutes.adapter = adapter

        // Setup chip selection listeners
        binding.chipGroupDayType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val selectedChip = group.findViewById<Chip>(checkedIds.first())
            val dayType = when (selectedChip.id) {
                R.id.chip_weekday -> DayType.WEEKDAY
                R.id.chip_friday -> DayType.FRIDAY
                R.id.chip_weekend -> DayType.WEEKEND
                else -> DayType.WEEKDAY
            }

            viewModel.setDayType(dayType)
        }
    }

    private fun observeViewModel() {
        // Observe routes
        viewModel.routes.observe(viewLifecycleOwner) { routes ->
            updateRoutesList(routes)
        }

        // Observe selected day type
        viewModel.selectedDayType.observe(viewLifecycleOwner) { dayType ->
            updateAdapter(dayType)
            viewModel.routes.value?.let { routes ->
                updateRoutesList(routes)
            }
        }
    }

    private fun updateAdapter(dayType: DayType) {
        adapter = ShuttleRouteAdapter(dayType)
        binding.recyclerRoutes.adapter = adapter

        // Resubmit current routes (filtered)
        viewModel.routes.value?.let { routes ->
            updateRoutesList(routes)
        }
    }

    private fun updateRoutesList(routes: List<com.nottingham.mynottingham.data.model.ShuttleRoute>) {
        val dayType = viewModel.selectedDayType.value ?: DayType.WEEKDAY

        // Filter routes that have schedules for the selected day type
        val filteredRoutes = routes.filter { route ->
            when (dayType) {
                DayType.WEEKDAY -> route.weekdaySchedule != null
                DayType.FRIDAY -> route.fridaySchedule != null
                DayType.WEEKEND -> route.weekendSchedule != null
            }
        }

        if (filteredRoutes.isEmpty()) {
            // Show empty state
            binding.recyclerRoutes.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            adapter.submitList(emptyList())
        } else {
            // Show routes
            binding.recyclerRoutes.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            adapter.submitList(filteredRoutes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
