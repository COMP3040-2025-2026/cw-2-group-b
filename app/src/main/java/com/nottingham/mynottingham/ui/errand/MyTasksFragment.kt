package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseErrandRepository
import com.nottingham.mynottingham.databinding.FragmentMyTasksBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * My Tasks Fragment
 * ✅ Migrated to Firebase - no longer uses backend API
 * ✅ Supports 3 tabs: Posted, Accepted, History
 */
class MyTasksFragment : Fragment() {

    private var _binding: FragmentMyTasksBinding? = null
    private val binding get() = _binding!!

    enum class TabType { POSTED, ACCEPTED, HISTORY }
    private var currentTab = TabType.POSTED

    private lateinit var tokenManager: TokenManager
    private lateinit var myTasksAdapter: MyTasksAdapter

    private val repository = FirebaseErrandRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTasksBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 1. Force Layout Manager (Critical if missing in XML)
        myTasksAdapter = MyTasksAdapter(
            onItemClicked = { task ->
                val bundle = Bundle().apply {
                    putString("taskId", task.id)
                    putString("title", task.title)
                    putString("description", task.description)
                    task.orderAmount?.let { putString("orderAmount", String.format("%.2f", it)) }
                    putString("reward", String.format("%.2f", task.reward))
                    putString("taskType", task.taskType)
                    putString("location", task.location)
                    putString("requesterId", task.requesterId)
                    putString("requesterName", task.requesterName)
                    putString("providerId", task.providerId)
                    putString("providerName", task.providerName)
                    putString("requesterAvatar", task.requesterAvatar)
                    putString("providerAvatar", task.providerAvatar)
                    putString("status", task.status)
                    putString("timeLimit", task.deadline) // Use "timeLimit" as the key
                    putLong("timestamp", task.createdAt)
                }
                val taskDetailFragment = TaskDetailFragment().apply {
                    arguments = bundle
                }
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .replace(R.id.errand_fragment_container, taskDetailFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClicked = { task ->
                confirmDeleteTask(task)
            }
        )
        binding.recyclerMyTasks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = myTasksAdapter
        }

        // 2. Setup Tabs
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        loadMyTasks()
    }

    private fun setupTabs() {
        binding.btnPosted.setOnClickListener {
            if (currentTab != TabType.POSTED) {
                currentTab = TabType.POSTED
                updateTabStyles()
                loadMyTasks()
            }
        }

        binding.btnAccepted.setOnClickListener {
            if (currentTab != TabType.ACCEPTED) {
                currentTab = TabType.ACCEPTED
                updateTabStyles()
                loadMyTasks()
            }
        }

        binding.btnHistory.setOnClickListener {
            if (currentTab != TabType.HISTORY) {
                currentTab = TabType.HISTORY
                updateTabStyles()
                loadMyTasks()
            }
        }

        updateTabStyles() // Initial style
    }

    private fun updateTabStyles() {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val transparentColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        val secondaryTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Reset all buttons to unselected state
        binding.btnPosted.setBackgroundColor(transparentColor)
        binding.btnPosted.setTextColor(secondaryTextColor)
        binding.btnAccepted.setBackgroundColor(transparentColor)
        binding.btnAccepted.setTextColor(secondaryTextColor)
        binding.btnHistory.setBackgroundColor(transparentColor)
        binding.btnHistory.setTextColor(secondaryTextColor)

        // Highlight selected tab
        when (currentTab) {
            TabType.POSTED -> {
                binding.btnPosted.setBackgroundColor(primaryColor)
                binding.btnPosted.setTextColor(whiteColor)
            }
            TabType.ACCEPTED -> {
                binding.btnAccepted.setBackgroundColor(primaryColor)
                binding.btnAccepted.setTextColor(whiteColor)
            }
            TabType.HISTORY -> {
                binding.btnHistory.setBackgroundColor(primaryColor)
                binding.btnHistory.setTextColor(whiteColor)
            }
        }
    }

    private fun loadMyTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Get My IDs
                val myIdStr = tokenManager.getUserId().first() ?: ""

                if (myIdStr.isEmpty()) {
                    if (_binding != null) {
                        binding.recyclerMyTasks.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyState.text = "Please login first"
                    }
                    return@launch
                }

                // 2. Collect from Firebase Flow based on tab
                val flow = when (currentTab) {
                    TabType.POSTED -> repository.getUserRequestedErrands(myIdStr)
                    TabType.ACCEPTED -> repository.getUserProvidedErrands(myIdStr)
                    TabType.HISTORY -> repository.getUserHistoryErrands(myIdStr)
                }

                flow.collect { errands ->
                    if (_binding == null) return@collect

                    // Map Firebase data to MyTask and apply filtering
                    val myTasks = errands.mapNotNull { errand ->
                        val status = errand["status"] as? String ?: "PENDING"

                        // For Posted/Accepted tabs, filter out completed/cancelled
                        if (currentTab != TabType.HISTORY) {
                            if (status == "COMPLETED" || status == "CANCELLED") {
                                return@mapNotNull null
                            }
                        }

                        MyTask(
                            id = errand["id"] as? String ?: "",
                            title = errand["title"] as? String ?: "",
                            description = errand["description"] as? String ?: "",
                            status = status,
                            orderAmount = (errand["orderAmount"] as? Number)?.toDouble(),
                            reward = (errand["reward"] as? Number)?.toDouble() ?: 0.0,
                            taskType = errand["type"] as? String ?: "",
                            location = errand["location"] as? String
                                ?: errand["deliveryLocation"] as? String
                                ?: errand["pickupLocation"] as? String
                                ?: "",
                            requesterId = errand["requesterId"] as? String ?: "",
                            providerId = errand["providerId"] as? String,
                            requesterName = errand["requesterName"] as? String ?: "Unknown",
                            providerName = errand["providerName"] as? String,
                            requesterAvatar = errand["requesterAvatar"] as? String,
                            providerAvatar = errand["providerAvatar"] as? String,
                            deadline = errand["timeLimit"] as? String
                                ?: errand["deadline"] as? String
                                ?: "",
                            createdAt = (errand["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    }

                    // 3. DEBUG log
                    Log.d("MyTasksFragment", "Loaded ${myTasks.size} tasks for tab: $currentTab")

                    // 4. Update UI
                    if (myTasks.isEmpty()) {
                        binding.recyclerMyTasks.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyState.text = when (currentTab) {
                            TabType.POSTED -> "No tasks posted yet"
                            TabType.ACCEPTED -> "No tasks accepted yet"
                            TabType.HISTORY -> "No history yet"
                        }
                    } else {
                        binding.recyclerMyTasks.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                        myTasksAdapter.submitList(myTasks)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // DO NOTHING - The user likely navigated away, don't show toast
            } catch (e: Exception) {
                e.printStackTrace()
                if (_binding != null) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Show confirmation dialog and delete task from history
     */
    private fun confirmDeleteTask(task: MyTask) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete History")
            .setMessage("Are you sure you want to delete \"${task.title}\" from history?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: MyTask) {
        lifecycleScope.launch {
            try {
                val result = repository.deleteErrand(task.id)

                if (_binding == null) return@launch

                result.onSuccess {
                    Toast.makeText(requireContext(), "Deleted from history", Toast.LENGTH_SHORT).show()
                    // The list will auto-update via Firebase Flow
                }.onFailure { e ->
                    Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
