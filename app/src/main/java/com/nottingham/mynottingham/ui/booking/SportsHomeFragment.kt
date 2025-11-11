package com.nottingham.mynottingham.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentSportsHomeBinding

class SportsHomeFragment : Fragment() {

    private var _binding: FragmentSportsHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportsHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Facilities card
        binding.cardFacilities.setOnClickListener {
            navigateToFragment(CheckAvailabilityFragment())
        }

        // Equipment Rental card
        binding.cardEquipment.setOnClickListener {
            navigateToFragment(EquipmentRentalFragment())
        }

        // My Bookings card
        binding.cardMyBookings.setOnClickListener {
            navigateToFragment(SportsMyBookingsFragment())
        }

        // Guidelines card
        binding.cardGuidelines.setOnClickListener {
            navigateToFragment(GuidelinesFragment())
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.booking_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
