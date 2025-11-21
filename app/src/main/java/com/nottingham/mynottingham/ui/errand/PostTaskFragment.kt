package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.databinding.FragmentPostTaskBinding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.nottingham.mynottingham.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class PostTaskFragment : Fragment() {

    private var _binding: FragmentPostTaskBinding? = null
    private val binding get() = _binding!!
    private val errandViewModel: ErrandViewModel by activityViewModels()

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
        // Retrieve category from arguments and set it
        val category = arguments?.getString("task_category")
        binding.etTaskType.setText(category)
        setupDropdowns()
        setupPostButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupDropdowns() {
        // Deadline dropdown
        val deadlines = arrayOf(
            "ASAP (within 30 mins)",
            "Within 1 hour",
            "Within 2 hours",
            "Within 3 hours",
            "Today",
            "Tomorrow"
        )
        val deadlineAdapter = ArrayAdapter(requireView().context, android.R.layout.simple_spinner_dropdown_item, deadlines)
        binding.dropdownDeadline.setAdapter(deadlineAdapter)
        binding.dropdownDeadline.setText(deadlines[0], false)
    }

    private fun setupPostButton() {
        binding.btnPostTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val location = binding.etLocation.text.toString()
            val reward = binding.etReward.text.toString()
            val taskType = binding.etTaskType.text.toString()

            if (title.isNotEmpty() && description.isNotEmpty() && location.isNotEmpty() && reward.isNotEmpty()) {
                lifecycleScope.launch {
                    val tokenManager = TokenManager(requireContext())
                    val userId = tokenManager.getUserId().first() ?: ""
                    val userName = tokenManager.getFullName().first() ?: "Unknown User"
                    
                    val deadline = binding.dropdownDeadline.text.toString()
                    val newTask = ErrandTask(
                        taskId = UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        price = reward,
                        location = location,
                        requesterId = userId,
                        requesterName = userName,
                        requesterAvatar = "", // Add avatar if available
                        deadline = deadline,
                        timestamp = System.currentTimeMillis()
                    )
                    errandViewModel.addTask(newTask)
                    requireActivity().onBackPressed()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
