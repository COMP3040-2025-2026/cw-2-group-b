package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentMyTasksBinding

class MyTasksFragment : Fragment() {

    private var _binding: FragmentMyTasksBinding? = null
    private val binding get() = _binding!!
    private var currentTab = Tab.POSTED

    enum class Tab {
        POSTED, ACCEPTED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabButtons()
    }

    private fun setupTabButtons() {
        binding.btnPosted.setOnClickListener {
            selectTab(Tab.POSTED)
        }

        binding.btnAccepted.setOnClickListener {
            selectTab(Tab.ACCEPTED)
        }

        // Initialize with Posted tab selected
        selectTab(Tab.POSTED)
    }

    private fun selectTab(tab: Tab) {
        currentTab = tab

        when (tab) {
            Tab.POSTED -> {
                // Style Posted button as selected
                binding.btnPosted.apply {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
                // Style Accepted button as unselected
                binding.btnAccepted.apply {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
                // TODO: Load posted tasks
            }
            Tab.ACCEPTED -> {
                // Style Accepted button as selected
                binding.btnAccepted.apply {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
                // Style Posted button as unselected
                binding.btnPosted.apply {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                }
                // TODO: Load accepted tasks
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
