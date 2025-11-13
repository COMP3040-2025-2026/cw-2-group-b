package com.nottingham.mynottingham.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentCheckAvailabilityBinding

class CheckAvailabilityFragment : Fragment() {

    private var _binding: FragmentCheckAvailabilityBinding? = null
    private val binding get() = _binding!!

    private val facilities = listOf(
        "3g Pitch",
        "Badminton Court 1",
        "Badminton Court 2",
        "Field 1",
        "Field 2 (centre)",
        "Outdoor Court 1",
        "Outdoor Court 2",
        "Outdoor Court 3",
        "Outdoor Court 4",
        "Sports Hall Court 1 (min 6 players)",
        "Sports Hall Court 2 (min 6 players)",
        "Squash / Table Tennis Court 1",
        "Squash/ Table Tennis Court 2",
        "Tennis Court 1",
        "Tennis Court 2"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckAvailabilityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupDropdown()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            facilities
        )
        binding.dropdownFacility.setAdapter(adapter)

        binding.dropdownFacility.setOnItemClickListener { _, _, position, _ ->
            val selectedFacility = facilities[position]
            navigateToBookingDetails(selectedFacility)
        }
    }

    private fun navigateToBookingDetails(facilityName: String) {
        val fragment = BookingDetailsFragment.newInstance(facilityName)
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
