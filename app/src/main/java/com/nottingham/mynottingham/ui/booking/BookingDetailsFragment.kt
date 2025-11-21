package com.nottingham.mynottingham.ui.booking

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.databinding.FragmentBookingDetailsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class BookingDetailsFragment : Fragment(R.layout.fragment_booking_details) {

    private var _binding: FragmentBookingDetailsBinding? = null
    private val binding get() = _binding!!
    
    // 获取 ViewModel
    private val viewModel: BookingViewModel by viewModels()

    private lateinit var tokenManager: TokenManager

    private var currentFacilityName: String = ""
    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedTimeSlot: Int? = null
    
    // Dynamically obtained current logged-in user
    private var currentUserId: String = "unknown"
    private var currentUserName: String = "Unknown User"

    companion object {
        private const val ARG_FACILITY_NAME = "facility_name"

        fun newInstance(facilityName: String): BookingDetailsFragment {
            val fragment = BookingDetailsFragment()
            val args = Bundle()
            args.putString(ARG_FACILITY_NAME, facilityName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(requireContext()) // Initialize TokenManager

        arguments?.let {
            currentFacilityName = it.getString(ARG_FACILITY_NAME) ?: ""
        }

        // Collect user ID and name
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                currentUserId = tokenManager.getUserId().first() ?: "unknown"
                currentUserName = tokenManager.getFullName().first() ?: "Unknown User"
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBookingDetailsBinding.bind(view)

        setupUI()
        setupDateList()
        setupTimeGrid()
        observeData()
        
        // Initial load of booking data for today
        viewModel.loadOccupiedSlots(currentFacilityName, selectedDate.toString())
    }

    private fun setupUI() {
        binding.tvFacilityName.text = currentFacilityName
        binding.toolbar.title = currentFacilityName
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.btnConfirmBooking.setOnClickListener {
            if (selectedTimeSlot != null) {
                confirmBooking()
            } else {
                Toast.makeText(context, "Please select a time slot", Toast.LENGTH_SHORT).show()
            }
        }
        // Disable button initially
        binding.btnConfirmBooking.isEnabled = false
    }

    private fun setupDateList() {
        // Generate today and next 6 days, total 7 days
        val dateList = (0..6).map { LocalDate.now().plusDays(it.toLong()) }
        
        val dateAdapter = DateAdapter(dateList) { date ->
            selectedDate = date
            // Reset selected time slot when date changes
            selectedTimeSlot = null
            binding.btnConfirmBooking.isEnabled = false
            
            // Reload occupied slots for the selected date
            viewModel.loadOccupiedSlots(currentFacilityName, date.toString())
        }

        binding.rvDates.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }
    }

    private fun setupTimeGrid() {
        // Initialize time grid Adapter
        val timeAdapter = TimeSlotAdapter { slot ->
            selectedTimeSlot = slot
            // Only enable booking button if a valid time slot is selected
            binding.btnConfirmBooking.isEnabled = (slot != null)
        }

        binding.rvTimeSlots.apply {
            layoutManager = GridLayoutManager(context, 3) // 3 columns
            adapter = timeAdapter
        }
    }

    private fun observeData() {
        // Observe database query results
        viewModel.occupiedSlots.observe(viewLifecycleOwner) { bookings ->
            // Pass the latest booking list to the Adapter to handle display (grey out)
            (binding.rvTimeSlots.adapter as? TimeSlotAdapter)?.updateBookings(bookings)
        }
    }

    private fun confirmBooking() {
        // Call ViewModel to save data to database
        viewModel.saveBooking(
            facilityName = currentFacilityName,
            date = selectedDate.toString(),
            timeSlot = selectedTimeSlot!!,
            userId = currentUserId,
            userName = currentUserName,
            onSuccess = {
                Toast.makeText(context, "Booking Successful!", Toast.LENGTH_SHORT).show()
                // For better UX, reset selection state
                selectedTimeSlot = null
                binding.btnConfirmBooking.isEnabled = false
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
