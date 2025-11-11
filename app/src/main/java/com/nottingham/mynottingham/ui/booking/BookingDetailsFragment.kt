package com.nottingham.mynottingham.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.databinding.FragmentBookingDetailsBinding

class BookingDetailsFragment : Fragment() {

    private var _binding: FragmentBookingDetailsBinding? = null
    private val binding get() = _binding!!
    private var facilityName: String = ""

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
        arguments?.let {
            facilityName = it.getString(ARG_FACILITY_NAME) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupFacilityInfo()
        setupBookingButton()
    }

    private fun setupToolbar() {
        binding.toolbar.title = facilityName
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupFacilityInfo() {
        binding.tvFacilityName.text = facilityName
        binding.tvLocation.text = "Sports Complex"
        binding.tvDuration.text = "1 hour per slot"
    }

    private fun setupBookingButton() {
        binding.btnConfirmBooking.setOnClickListener {
            // TODO: Implement booking confirmation logic
            Toast.makeText(
                requireContext(),
                "Booking confirmed for $facilityName",
                Toast.LENGTH_SHORT
            ).show()
            requireActivity().onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
