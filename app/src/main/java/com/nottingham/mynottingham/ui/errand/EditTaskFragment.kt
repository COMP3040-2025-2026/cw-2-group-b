package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nottingham.mynottingham.data.repository.FirebaseErrandRepository
import com.nottingham.mynottingham.databinding.FragmentEditTaskBinding
import kotlinx.coroutines.launch

/**
 * Edit Task Fragment
 *
 * Allows users to edit their posted tasks.
 * - PENDING status: Direct edit allowed
 * - ACCEPTED/DELIVERING status: Shows info banner, edit request sent to rider (P2)
 */
class EditTaskFragment : Fragment() {

    private var _binding: FragmentEditTaskBinding? = null
    private val binding get() = _binding!!

    private val repository = FirebaseErrandRepository()

    // Task data from arguments
    private var taskId: String = ""
    private var taskStatus: String = "PENDING"
    private var providerId: String? = null

    companion object {
        private val DEADLINES = arrayOf(
            "ASAP (within 30 mins)",
            "Within 1 hour",
            "Within 2 hours",
            "Within 3 hours",
            "Today",
            "Tomorrow"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get task data from arguments
        taskId = arguments?.getString("taskId") ?: ""
        val title = arguments?.getString("title") ?: ""
        val description = arguments?.getString("description") ?: ""
        val reward = arguments?.getString("reward") ?: ""
        val location = arguments?.getString("location") ?: ""
        val deadline = arguments?.getString("deadline") ?: DEADLINES[0]
        taskStatus = arguments?.getString("status") ?: "PENDING"
        providerId = arguments?.getString("providerId")

        Log.d("EditTask", "Loaded task - ID: $taskId, Status: $taskStatus, Reward: $reward")

        setupToolbar()
        setupDropdowns()
        prefillForm(title, description, reward, location, deadline)
        setupInfoBanner()
        setupUpdateButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupDropdowns() {
        val deadlineAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            DEADLINES
        )
        binding.dropdownDeadline.setAdapter(deadlineAdapter)
    }

    private fun prefillForm(
        title: String,
        description: String,
        reward: String,
        location: String,
        deadline: String
    ) {
        binding.etTaskTitle.setText(title)
        binding.etDescription.setText(description)
        binding.etReward.setText(reward)
        binding.etLocation.setText(location)

        // Set deadline dropdown
        val deadlineIndex = DEADLINES.indexOfFirst { it == deadline }
        if (deadlineIndex >= 0) {
            binding.dropdownDeadline.setText(DEADLINES[deadlineIndex], false)
        } else {
            // Try partial match
            val partialMatch = DEADLINES.indexOfFirst { deadline.contains(it) || it.contains(deadline) }
            if (partialMatch >= 0) {
                binding.dropdownDeadline.setText(DEADLINES[partialMatch], false)
            } else {
                binding.dropdownDeadline.setText(DEADLINES[0], false)
            }
        }
    }

    private fun setupInfoBanner() {
        when (taskStatus.uppercase()) {
            "ACCEPTED", "DELIVERING" -> {
                binding.cardInfoBanner.visibility = View.VISIBLE
                binding.tvInfoBanner.text =
                    "This task has been accepted. Your edit request will be sent to the rider for approval."
            }
            else -> {
                binding.cardInfoBanner.visibility = View.GONE
            }
        }
    }

    private fun setupUpdateButton() {
        binding.btnUpdateTask.setOnClickListener {
            if (validateForm()) {
                updateTask()
            }
        }
    }

    private fun validateForm(): Boolean {
        val title = binding.etTaskTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val reward = binding.etReward.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilTaskTitle.error = "Title is required"
            return false
        }
        binding.tilTaskTitle.error = null

        if (description.isEmpty()) {
            binding.tilDescription.error = "Description is required"
            return false
        }
        binding.tilDescription.error = null

        if (location.isEmpty()) {
            binding.tilLocation.error = "Location is required"
            return false
        }
        binding.tilLocation.error = null

        if (reward.isEmpty()) {
            binding.tilReward.error = "Reward is required"
            return false
        }
        binding.tilReward.error = null

        return true
    }

    private fun updateTask() {
        binding.btnUpdateTask.isEnabled = false

        // Verify taskId is valid
        if (taskId.isEmpty()) {
            Log.e("EditTask", "Cannot update: taskId is empty!")
            Toast.makeText(requireContext(), "Error: Task ID not found", Toast.LENGTH_SHORT).show()
            binding.btnUpdateTask.isEnabled = true
            return
        }

        // Clean reward string - remove "RM ", spaces, and any non-numeric characters except decimal point
        val rewardText = binding.etReward.text.toString()
            .replace("RM", "", ignoreCase = true)
            .replace(" ", "")
            .trim()
        val rewardValue = rewardText.toDoubleOrNull() ?: 0.0

        val locationValue = binding.etLocation.text.toString().trim()
        val updates = mapOf<String, Any>(
            "title" to binding.etTaskTitle.text.toString().trim(),
            "description" to binding.etDescription.text.toString().trim(),
            "location" to locationValue,           // Update both for compatibility
            "deliveryLocation" to locationValue,   // Update both for compatibility
            "reward" to rewardValue,
            "timeLimit" to binding.dropdownDeadline.text.toString(),
            "updatedAt" to System.currentTimeMillis()
        )

        Log.d("EditTask", "Updating task: $taskId with status: $taskStatus")
        Log.d("EditTask", "Updates: $updates")

        lifecycleScope.launch {
            when (taskStatus.uppercase()) {
                "PENDING" -> {
                    // Direct update for PENDING tasks
                    Log.d("EditTask", "Executing Firebase update for PENDING task")
                    val result = repository.updateErrand(taskId, updates)

                    if (_binding == null) return@launch

                    result.onSuccess {
                        Log.d("EditTask", "Firebase update successful!")
                        Toast.makeText(requireContext(), "Task updated successfully", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }.onFailure { e ->
                        Log.e("EditTask", "Firebase update failed: ${e.message}", e)
                        Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.btnUpdateTask.isEnabled = true
                    }
                }
                "ACCEPTED", "DELIVERING" -> {
                    // For accepted tasks, send edit request to rider (P2 feature)
                    // For now, just show a message
                    Toast.makeText(
                        requireContext(),
                        "Edit request feature coming soon. Please chat with the rider to request changes.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnUpdateTask.isEnabled = true
                }
                else -> {
                    Toast.makeText(requireContext(), "Cannot edit task in current status", Toast.LENGTH_SHORT).show()
                    binding.btnUpdateTask.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
