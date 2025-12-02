package com.nottingham.mynottingham.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentBookingSuccessBinding

/**
 * Booking Success Fragment
 *
 * Display booking success confirmation page with booking details
 */
class BookingSuccessFragment : Fragment() {

    private var _binding: FragmentBookingSuccessBinding? = null
    private val binding get() = _binding!!

    private var facilityName: String = ""
    private var bookingDate: String = ""
    private var timeSlot: Int = 0

    companion object {
        private const val ARG_FACILITY = "facility"
        private const val ARG_DATE = "date"
        private const val ARG_TIME_SLOT = "time_slot"

        fun newInstance(
            facility: String,
            date: String,
            timeSlot: Int
        ): BookingSuccessFragment {
            return BookingSuccessFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FACILITY, facility)
                    putString(ARG_DATE, date)
                    putInt(ARG_TIME_SLOT, timeSlot)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            facilityName = it.getString(ARG_FACILITY, "")
            bookingDate = it.getString(ARG_DATE, "")
            timeSlot = it.getInt(ARG_TIME_SLOT, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.tvFacility.text = facilityName
        binding.tvDate.text = bookingDate
        binding.tvTime.text = String.format("%02d:00 - %02d:00", timeSlot, timeSlot + 1)
    }

    private fun setupClickListeners() {
        // View My Bookings - navigate to SportsMyBookingsFragment
        binding.btnViewBookings.setOnClickListener {
            parentFragmentManager.popBackStack() // Pop success fragment
            parentFragmentManager.popBackStack() // Pop booking details fragment

            // Navigate to My Bookings
            val myBookingsFragment = SportsMyBookingsFragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.booking_container, myBookingsFragment)
                .addToBackStack(null)
                .commit()
        }

        // Back to Sports Home
        binding.btnBackHome.setOnClickListener {
            // Pop all booking-related fragments back to sports home
            parentFragmentManager.popBackStack() // Pop success fragment
            parentFragmentManager.popBackStack() // Pop booking details fragment
            parentFragmentManager.popBackStack() // Pop check availability fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
