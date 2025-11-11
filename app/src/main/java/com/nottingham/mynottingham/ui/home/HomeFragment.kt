package com.nottingham.mynottingham.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentHomeBinding
import com.nottingham.mynottingham.util.showToast

/**
 * Home Fragment - Main landing page showing all campus services
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
            // Setup card click listeners for navigation with smooth animations
            cardInstatt.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_instatt)
            }

            cardShuttle.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_shuttle)
            }

            cardErrand.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_errand)
            }

            cardNotti.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_notti)
            }

            cardSports.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_sports)
            }
        }
    }

    private fun observeViewModel() {
        // Observe ViewModel LiveData here
        viewModel.welcomeMessage.observe(viewLifecycleOwner) { message ->
            binding.tvWelcome.text = message
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
