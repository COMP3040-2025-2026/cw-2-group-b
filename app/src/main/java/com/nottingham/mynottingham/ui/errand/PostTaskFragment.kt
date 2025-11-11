package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentPostTaskBinding

class PostTaskFragment : Fragment() {

    private var _binding: FragmentPostTaskBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupDropdowns()
        setupPostButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupDropdowns() {
        // Task Type dropdown
        val taskTypes = arrayOf("Shopping", "Pickup", "Food Delivery", "Others")
        val taskTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, taskTypes)
        binding.dropdownTaskType.setAdapter(taskTypeAdapter)

        // Deadline dropdown
        val deadlines = arrayOf(
            "ASAP (within 30 mins)",
            "Within 1 hour",
            "Within 2 hours",
            "Within 3 hours",
            "Today",
            "Tomorrow"
        )
        val deadlineAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, deadlines)
        binding.dropdownDeadline.setAdapter(deadlineAdapter)
    }

    private fun setupPostButton() {
        binding.btnPostTask.setOnClickListener {
            // TODO: Validate inputs and create task
            val title = binding.etTaskTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val location = binding.etLocation.text.toString()
            val reward = binding.etReward.text.toString()

            if (title.isNotEmpty() && description.isNotEmpty() && location.isNotEmpty() && reward.isNotEmpty()) {
                // TODO: Create task and navigate back
                requireActivity().onBackPressed()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
