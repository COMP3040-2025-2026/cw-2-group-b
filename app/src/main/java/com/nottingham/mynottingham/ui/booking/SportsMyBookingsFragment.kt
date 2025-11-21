package com.nottingham.mynottingham.ui.booking

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.databinding.FragmentSportsMyBookingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.widget.Toast

class SportsMyBookingsFragment : Fragment(R.layout.fragment_sports_my_bookings) {

    private var _binding: FragmentSportsMyBookingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var rvBookings: RecyclerView
    private lateinit var adapter: SportsBookingAdapter
    
    private val viewModel: BookingViewModel by viewModels()
    
    // Current user ID, should be obtained from Session/Token
    private lateinit var tokenManager: TokenManager
    private var currentUserId: String = "unknown" 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(requireContext())
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                currentUserId = tokenManager.getUserId().first() ?: "unknown"
                // Once userId is available, start observing bookings
                observeBookings()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSportsMyBookingsBinding.bind(view)
        
        rvBookings = binding.rvMyBookings // Use binding to find RecyclerView
        
        setupRecyclerView()
        
        binding.btnBack.setOnClickListener { // Use binding to find back button
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = SportsBookingAdapter(emptyList()) { booking ->
            showCancelConfirmationDialog(booking)
        }
        
        rvBookings.layoutManager = LinearLayoutManager(context)
        rvBookings.adapter = adapter
    }

    private fun observeBookings() {
        // Observe LiveData from ViewModel
        viewModel.getUserBookings(currentUserId).observe(viewLifecycleOwner) { bookings ->
            if (bookings.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                rvBookings.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                rvBookings.visibility = View.VISIBLE
                adapter.updateData(bookings)
            }
        }
    }

    private fun showCancelConfirmationDialog(booking: com.nottingham.mynottingham.data.local.database.entities.BookingEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel your booking for ${booking.facilityName} at ${String.format("%02d:00", booking.timeSlot)} on ${booking.bookingDate}?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.cancelBooking(booking)
                Toast.makeText(requireContext(), "Booking cancelled!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}