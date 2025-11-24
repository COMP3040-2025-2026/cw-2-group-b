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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

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
        // Start loading bookings from Firebase
        viewModel.getUserBookings(currentUserId)

        // Observe LiveData from ViewModel
        viewModel.userBookings.observe(viewLifecycleOwner) { bookings ->
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
        // 重新计算一下时间，以决定弹窗显示 "Cancel" 还是 "Delete"
        val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
        val now = LocalDateTime.now(zoneId)
        
        val bookingDate = LocalDate.parse(booking.bookingDate)
        val bookingTime = LocalTime.of(booking.timeSlot, 0)
        val bookingDateTime = LocalDateTime.of(bookingDate, bookingTime)
        val endTime = bookingDateTime.plusHours(1)

        // 判断是否是“过期删除”操作
        val isPast = now.isAfter(endTime)

        // 根据状态设置弹窗文案
        val title = if (isPast) "Delete History" else "Cancel Booking"
        val message = if (isPast) 
            "Do you want to delete this booking record?" 
        else 
            "Are you sure you want to cancel your booking for ${booking.facilityName}?"

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                // 无论是取消还是删除，实际上都是从数据库移除该条目，所以调用同一个 ViewModel 方法即可
                viewModel.cancelBooking(booking)
                
                val toastMsg = if (isPast) "Record deleted" else "Booking cancelled"
                Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAndCancelBooking(booking: com.nottingham.mynottingham.data.local.database.entities.BookingEntity) {
        try {
            val bookingDate = LocalDate.parse(booking.bookingDate)
            val bookingDateTime = bookingDate.atTime(booking.timeSlot, 0)
            val zoneId = ZoneId.of("Asia/Kuala_Lumpur")
            val currentDateTime = LocalDateTime.now(zoneId)
            val cutoffTime = bookingDateTime.minusHours(1)

            if (currentDateTime.isAfter(cutoffTime)) {
                Toast.makeText(requireContext(), "Cannot cancel within 1 hour of booking.", Toast.LENGTH_SHORT).show()
                return
            }

            viewModel.cancelBooking(booking)
            Toast.makeText(requireContext(), "Booking cancelled successfully.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error checking booking time.", Toast.LENGTH_SHORT).show()
        }
    }
}