package com.nottingham.mynottingham.ui.booking

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.database.entities.BookingEntity
import com.nottingham.mynottingham.databinding.FragmentBookingDetailsBinding
import java.time.LocalDate

class BookingDetailsFragment : Fragment(R.layout.fragment_booking_details) {

    private var binding: FragmentBookingDetailsBinding? = null
    // private val viewModel: BookingViewModel by viewModels()

    private lateinit var dateAdapter: DateAdapter
    private lateinit var timeSlotAdapter: TimeSlotAdapter
    
    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedTimeSlot: Int? = null
    private lateinit var facilityId: String

    // Mock storage for bookings
    private val mockBookingsStore = mutableListOf<BookingEntity>()

    companion object {
        private const val ARG_FACILITY_ID = "facility_id"

        fun newInstance(facilityId: String): BookingDetailsFragment {
            val fragment = BookingDetailsFragment()
            val args = Bundle()
            args.putString(ARG_FACILITY_ID, facilityId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            facilityId = it.getString(ARG_FACILITY_ID) ?: "Unknown Facility"
        }
        // Initialize with some default mock booking if needed, e.g., Alice's 10:00 booking
        if (mockBookingsStore.isEmpty()) { // Only add once
            mockBookingsStore.add(
                BookingEntity(
                    bookingId = 1, // Unique ID for mock
                    facilityId = facilityId,
                    facilityName = facilityId,
                    bookingDate = LocalDate.now().toString(),
                    timeSlot = 10,
                    userId = "u1",
                    userName = "Alice",
                    status = "confirmed"
                )
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentBookingDetailsBinding.bind(view)

        setupToolbar() // Setup toolbar back button and title
        setupDateRecyclerView()
        setupTimeRecyclerView()
        setupButtons()
        
        loadBookingsForDate(selectedDate)
    }

    private fun setupToolbar() {
        binding?.toolbar?.title = facilityId // Set toolbar title to facilityId
        binding?.toolbar?.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupDateRecyclerView() {
        val dateList = (0..6).map { LocalDate.now().plusDays(it.toLong()) }

        dateAdapter = DateAdapter(dateList) { date ->
            selectedDate = date
            loadBookingsForDate(date)
        }
        
        binding?.rvDates?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }
    }

    private fun setupTimeRecyclerView() {
        timeSlotAdapter = TimeSlotAdapter { slot ->
            selectedTimeSlot = slot
            binding?.btnConfirmBooking?.isEnabled = slot != null
        }

        binding?.rvTimeSlots?.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = timeSlotAdapter
        }
    }

    private fun loadBookingsForDate(date: LocalDate) {
        val dateString = date.toString()
        
        // Filter mock bookings for the selected date and facility
        val bookingsForDate = mockBookingsStore.filter { 
            it.bookingDate == dateString && it.facilityId == facilityId 
        }

        timeSlotAdapter.updateBookings(bookingsForDate)
    }

    private fun setupButtons() {
        binding?.btnConfirmBooking?.setOnClickListener {
            if (selectedTimeSlot != null) {
                val newBooking = BookingEntity(
                    bookingId = (mockBookingsStore.maxOfOrNull { it.bookingId } ?: 0) + 1, // Generate new unique ID
                    facilityId = facilityId,
                    facilityName = facilityId,
                    bookingDate = selectedDate.toString(),
                    timeSlot = selectedTimeSlot!!,
                    userId = "currentUser",
                    userName = "Me",
                    status = "confirmed"
                )
                
                // Add new booking to mock storage
                mockBookingsStore.add(newBooking)
                
                Toast.makeText(context, "Booked for ${selectedDate} at ${selectedTimeSlot}:00 for ${facilityId}", Toast.LENGTH_SHORT).show()
                
                loadBookingsForDate(selectedDate) // Refresh list to show new booking
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}