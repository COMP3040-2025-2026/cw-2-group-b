package com.nottingham.mynottingham.ui.errand

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseErrandRepository
import com.nottingham.mynottingham.data.repository.FirebaseMessageRepository
import com.nottingham.mynottingham.databinding.FragmentTaskDetailsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Task Detail Fragment
 * ✅ Migrated to Firebase - no longer uses backend API
 * ✅ Supports delivery workflow: PENDING → ACCEPTED → DELIVERING → COMPLETED
 */
class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding get() = _binding!!

    private val repository = FirebaseErrandRepository()
    private val messageRepository = FirebaseMessageRepository()
    private lateinit var tokenManager: TokenManager

    private var currentTaskId: String = ""
    private var currentStatus: String = "PENDING"
    private var currentUserId: String = ""
    private var requesterId: String = ""
    private var providerId: String? = null
    private var providerName: String? = null
    private var providerAvatar: String? = null

    // Additional task details for editing
    private var taskTitle: String = ""
    private var taskDescription: String = ""
    private var taskPrice: String = ""
    private var taskLocation: String = ""
    private var taskDeadline: String = ""

    companion object {
        private const val MAX_ACTIVE_ORDERS = 3
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Get parameters from arguments
        currentTaskId = arguments?.getString("taskId") ?: ""
        taskTitle = arguments?.getString("title") ?: ""
        taskDescription = arguments?.getString("description") ?: ""
        taskPrice = arguments?.getString("price") ?: ""
        taskLocation = arguments?.getString("location") ?: ""
        val requesterName = arguments?.getString("requesterName")
        requesterId = arguments?.getString("requesterId") ?: ""
        val requesterAvatar = arguments?.getString("requesterAvatar") ?: "default"
        providerId = arguments?.getString("providerId")
        providerName = arguments?.getString("providerName")
        providerAvatar = arguments?.getString("providerAvatar")
        currentStatus = arguments?.getString("status") ?: "PENDING"
        taskDeadline = arguments?.getString("timeLimit") ?: "No Deadline"
        val timestamp = arguments?.getLong("timestamp") ?: 0

        // Bind UI
        binding.tvTaskTitle.text = taskTitle
        binding.tvTaskDescription.text = taskDescription
        binding.tvTaskPrice.text = "RM $taskPrice"
        binding.tvTaskLocation.text = taskLocation
        binding.tvRequesterName.text = requesterName
        binding.ivRequesterAvatar.setImageResource(
            com.nottingham.mynottingham.util.AvatarUtils.getDrawableId(requesterAvatar)
        )

        binding.tvTaskDeadline.text = "Deadline: $taskDeadline"
        binding.tvTaskDeadline.visibility = if (taskDeadline == "No Deadline") View.GONE else View.VISIBLE

        // Calculate time ago
        val currentTime = System.currentTimeMillis()
        val diffMillis = currentTime - timestamp
        val minutesAgo = diffMillis / (1000 * 60)
        binding.tvTaskPosted.text = when {
            minutesAgo < 1L -> "Posted just now"
            minutesAgo < 60L -> "Posted $minutesAgo mins ago"
            minutesAgo < 1440L -> "Posted ${minutesAgo / 60} hours ago"
            else -> "Posted ${minutesAgo / 1440} days ago"
        }
        binding.tvTaskPosted.visibility = View.VISIBLE

        // Setup based on user role
        lifecycleScope.launch {
            currentUserId = tokenManager.getUserId().first() ?: ""

            if (currentUserId.isEmpty()) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                    binding.btnAcceptTask.isEnabled = false
                }
                return@launch
            }

            // Refresh status from Firebase
            refreshTaskStatus()
        }
    }

    private suspend fun refreshTaskStatus() {
        try {
            val result = repository.getErrandById(currentTaskId)
            result.onSuccess { task ->
                if (task == null || _binding == null) return@onSuccess

                providerId = task["providerId"] as? String
                providerName = task["providerName"] as? String
                providerAvatar = task["providerAvatar"] as? String
                currentStatus = task["status"] as? String ?: "PENDING"

                setupUIBasedOnRole()
            }.onFailure {
                // Use the status from arguments if Firebase fails
                setupUIBasedOnRole()
            }
        } catch (e: Exception) {
            Log.e("TaskDetail", "Failed to refresh status", e)
            setupUIBasedOnRole()
        }
    }

    private fun setupUIBasedOnRole() {
        if (_binding == null) return

        val isOwner = currentUserId == requesterId
        val isProvider = providerId == currentUserId

        if (isOwner) {
            setupOwnerView()
        } else if (isProvider) {
            setupProviderView()
        } else {
            setupOtherUserView()
        }
    }

    /**
     * Requester (owner) view:
     * - PENDING: Can Edit, Delete
     * - ACCEPTED: Can see status, cannot delete
     * - DELIVERING: Can see status, cannot delete
     * - COMPLETED/CANCELLED: Just show status
     */
    private fun setupOwnerView() {
        if (_binding == null) return

        binding.btnAcceptTask.visibility = View.GONE
        binding.layoutProviderActions.visibility = View.GONE
        binding.layoutOwnerActions.visibility = View.VISIBLE

        when (currentStatus.uppercase()) {
            "PENDING" -> {
                binding.layoutEditDelete.visibility = View.VISIBLE
                binding.btnComplete.visibility = View.GONE
                binding.btnChatRider.visibility = View.GONE

                binding.btnEdit.setOnClickListener { navigateToEditTask() }
                binding.btnDelete.setOnClickListener { confirmDelete() }
            }
            "ACCEPTED" -> {
                binding.layoutEditDelete.visibility = View.GONE
                binding.btnComplete.visibility = View.GONE
                // Show Chat with Rider button
                binding.btnChatRider.visibility = View.VISIBLE
                binding.btnChatRider.setOnClickListener { chatWithRider() }
                showStatusMessage("Waiting for rider to start delivery")
            }
            "DELIVERING" -> {
                binding.layoutEditDelete.visibility = View.GONE
                binding.btnComplete.visibility = View.GONE
                // Show Chat with Rider button
                binding.btnChatRider.visibility = View.VISIBLE
                binding.btnChatRider.setOnClickListener { chatWithRider() }
                showStatusMessage("Rider is delivering your order")
            }
            "COMPLETED" -> {
                binding.layoutOwnerActions.visibility = View.GONE
                showStatusMessage("Task completed")
            }
            "CANCELLED" -> {
                binding.layoutOwnerActions.visibility = View.GONE
                showStatusMessage("Task was cancelled")
            }
            else -> {
                // IN_PROGRESS (legacy status)
                binding.layoutEditDelete.visibility = View.GONE
                binding.btnComplete.visibility = View.GONE
                showStatusMessage("Task in progress")
            }
        }
    }

    /**
     * Provider (rider) view:
     * - ACCEPTED: "Start Delivering" + "Drop Task"
     * - DELIVERING: "Mark Complete" + "Drop Task"
     */
    private fun setupProviderView() {
        if (_binding == null) return

        // Hide other action layouts
        binding.btnAcceptTask.visibility = View.GONE
        binding.layoutOwnerActions.visibility = View.GONE

        when (currentStatus.uppercase()) {
            "ACCEPTED", "IN_PROGRESS" -> {
                // Show provider actions: Drop Task + Start Delivering
                binding.layoutProviderActions.visibility = View.VISIBLE
                binding.btnProviderAction.text = "Start Delivering"
                binding.btnProviderAction.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.errand_delivering)
                )
                binding.btnProviderAction.setOnClickListener { startDelivering() }
                binding.btnDropTask.setOnClickListener { confirmDrop() }
            }
            "DELIVERING" -> {
                // Show provider actions: Drop Task + Mark Complete
                binding.layoutProviderActions.visibility = View.VISIBLE
                binding.btnProviderAction.text = "Mark Complete"
                binding.btnProviderAction.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.errand_completed)
                )
                binding.btnProviderAction.setOnClickListener { completeTask() }
                binding.btnDropTask.setOnClickListener { confirmDrop() }
            }
            "COMPLETED" -> {
                binding.layoutProviderActions.visibility = View.GONE
                showStatusMessage("Task completed")
            }
            else -> {
                binding.layoutProviderActions.visibility = View.GONE
            }
        }
    }

    /**
     * Other user view (not owner, not provider):
     * - PENDING: Can accept (if delivery mode on and < 3 orders)
     * - Otherwise: Show "Task Taken"
     */
    private fun setupOtherUserView() {
        if (_binding == null) return

        binding.layoutOwnerActions.visibility = View.GONE
        binding.layoutProviderActions.visibility = View.GONE

        if (currentStatus.uppercase() == "PENDING") {
            binding.btnAcceptTask.visibility = View.VISIBLE
            binding.btnAcceptTask.text = "Accept Task"
            binding.btnAcceptTask.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
            binding.btnAcceptTask.setOnClickListener { checkAndAcceptTask() }
        } else {
            binding.btnAcceptTask.visibility = View.VISIBLE
            binding.btnAcceptTask.isEnabled = false
            binding.btnAcceptTask.text = "Task Taken"
        }
    }

    private fun showStatusMessage(message: String) {
        if (_binding == null) return
        binding.tvTaskPosted.text = "Status: $message"
    }

    /**
     * Check delivery mode and order limit before accepting
     */
    private fun checkAndAcceptTask() {
        if (_binding == null) return
        binding.btnAcceptTask.isEnabled = false

        lifecycleScope.launch {
            try {
                // Check 1: Is Delivery Mode enabled?
                val deliveryModeEnabled = tokenManager.getDeliveryMode().first()
                if (!deliveryModeEnabled) {
                    if (_binding != null) {
                        Toast.makeText(
                            requireContext(),
                            "Please enable Delivery Mode in Profile settings first",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.btnAcceptTask.isEnabled = true
                    }
                    return@launch
                }

                // Check 2: Order limit
                val activeCountResult = repository.getActiveErrandCount(currentUserId)
                activeCountResult.onSuccess { count ->
                    if (_binding == null) return@onSuccess

                    if (count >= MAX_ACTIVE_ORDERS) {
                        Toast.makeText(
                            requireContext(),
                            "You have reached the maximum of $MAX_ACTIVE_ORDERS active orders",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.btnAcceptTask.isEnabled = true
                        return@onSuccess
                    }

                    // All checks passed, accept the task
                    acceptTask()
                }.onFailure { e ->
                    if (_binding != null) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.btnAcceptTask.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnAcceptTask.isEnabled = true
                }
            }
        }
    }

    private fun acceptTask() {
        lifecycleScope.launch {
            try {
                val userName = tokenManager.getFullName().first() ?: "User"
                val userAvatar = tokenManager.getAvatar().first()

                val result = repository.acceptErrand(currentTaskId, currentUserId, userName, userAvatar)

                if (_binding == null) return@launch

                result.onSuccess {
                    Toast.makeText(requireContext(), "Task Accepted!", Toast.LENGTH_SHORT).show()
                    providerId = currentUserId
                    currentStatus = "ACCEPTED"
                    setupProviderView()
                }.onFailure { e ->
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            if (_binding != null) {
                binding.btnAcceptTask.isEnabled = true
            }
        }
    }

    private fun startDelivering() {
        if (_binding == null) return
        binding.btnAcceptTask.isEnabled = false

        lifecycleScope.launch {
            val result = repository.startDelivering(currentTaskId)

            if (_binding == null) return@launch

            result.onSuccess {
                Toast.makeText(requireContext(), "Delivery started!", Toast.LENGTH_SHORT).show()
                currentStatus = "DELIVERING"
                setupProviderView()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            binding.btnAcceptTask.isEnabled = true
        }
    }

    private fun completeTask() {
        if (_binding == null) return
        binding.btnAcceptTask.isEnabled = false

        lifecycleScope.launch {
            val result = repository.completeErrand(currentTaskId)

            if (_binding == null) return@launch

            result.onSuccess {
                Toast.makeText(requireContext(), "Task completed!", Toast.LENGTH_SHORT).show()
                currentStatus = "COMPLETED"
                binding.btnAcceptTask.visibility = View.GONE
                binding.layoutOwnerActions.visibility = View.GONE
                showStatusMessage("Task completed")
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnAcceptTask.isEnabled = true
            }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to permanently delete this task?")
            .setPositiveButton("Delete") { _, _ -> performDelete() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete() {
        lifecycleScope.launch {
            val result = repository.deleteErrand(currentTaskId)

            if (_binding == null) return@launch

            result.onSuccess {
                Toast.makeText(requireContext(), "Task Deleted", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Delete Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDrop() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Drop Task")
            .setMessage("Are you sure you want to drop this task? It will be available for other riders.")
            .setPositiveButton("Drop") { _, _ -> performDrop() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDrop() {
        if (_binding == null) return
        binding.btnDropTask.isEnabled = false

        lifecycleScope.launch {
            val result = repository.dropErrand(currentTaskId)

            if (_binding == null) return@launch

            result.onSuccess {
                Toast.makeText(requireContext(), "Task Dropped", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Drop Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            if (_binding != null) {
                binding.btnDropTask.isEnabled = true
            }
        }
    }

    /**
     * Open chat with the rider
     */
    private fun chatWithRider() {
        val riderId = providerId
        val riderName = providerName

        if (riderId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Rider information not available", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Get current user info
                val currentUserName = tokenManager.getFullName().first() ?: "User"

                // Create or get existing conversation
                val participantIds = listOf(currentUserId, riderId)
                val result = messageRepository.createConversation(
                    participantIds = participantIds,
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    isGroup = false
                )

                if (_binding == null) return@launch

                result.onSuccess { conversationId ->
                    // Update conversation participant info for current user
                    messageRepository.updateConversationParticipantInfo(
                        userId = currentUserId,
                        conversationId = conversationId,
                        participantName = riderName ?: "Rider",
                        participantAvatar = providerAvatar
                    )

                    // Update conversation participant info for rider
                    val myName = tokenManager.getFullName().first() ?: "User"
                    val myAvatar = tokenManager.getAvatar().first()
                    messageRepository.updateConversationParticipantInfo(
                        userId = riderId,
                        conversationId = conversationId,
                        participantName = myName,
                        participantAvatar = myAvatar
                    )

                    // Also update participantId for both users
                    messageRepository.updateConversationParticipantId(
                        userId = currentUserId,
                        conversationId = conversationId,
                        participantId = riderId
                    )
                    messageRepository.updateConversationParticipantId(
                        userId = riderId,
                        conversationId = conversationId,
                        participantId = currentUserId
                    )

                    // Navigate to ChatDetailFragment using NavController
                    try {
                        val bundle = bundleOf(
                            "conversationId" to conversationId,
                            "participantName" to (riderName ?: "Rider"),
                            "participantAvatar" to providerAvatar,
                            "isOnline" to false
                        )

                        // Find the main NavController from the activity
                        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        navController.navigate(R.id.chatDetailFragment, bundle)
                    } catch (e: Exception) {
                        Log.e("TaskDetail", "Navigation failed", e)
                        Toast.makeText(requireContext(), "Failed to open chat", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { e ->
                    Toast.makeText(requireContext(), "Failed to create conversation: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Navigate to EditTaskFragment to edit the task
     */
    private fun navigateToEditTask() {
        val editFragment = EditTaskFragment().apply {
            arguments = Bundle().apply {
                putString("taskId", currentTaskId)
                putString("title", taskTitle)
                putString("description", taskDescription)
                putString("reward", taskPrice)
                putString("location", taskLocation)
                putString("deadline", taskDeadline)
                putString("status", currentStatus)
                putString("providerId", providerId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.errand_fragment_container, editFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
