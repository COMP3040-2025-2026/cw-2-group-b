package com.nottingham.mynottingham.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
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
        viewModel.welcomeMessage.observe(viewLifecycleOwner) { message ->
            binding.tvWelcome.text = message
        }

        viewModel.facultyYearMessage.observe(viewLifecycleOwner) { message ->
            binding.tvFacultyYear.text = message
            binding.tvFacultyYear.visibility = if (message.isBlank()) View.GONE else View.VISIBLE
        }

        viewModel.isTeacher.observe(viewLifecycleOwner) { isTeacher ->
            if (isTeacher) {
                binding.tvFacultyYear.visibility = View.GONE
            } else {
                if (binding.tvFacultyYear.text.isNotBlank()) {
                    binding.tvFacultyYear.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
