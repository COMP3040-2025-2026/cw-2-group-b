package com.nottingham.mynottingham.ui.errand

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
    private var requesterName: String = ""
    private var requesterAvatar: String = "default"
    private var providerId: String? = null
    private var providerName: String? = null
    private var providerAvatar: String? = null

    // Additional task details for editing
    private var taskTitle: String = ""
    private var taskDescription: String = ""
    private var taskOrderAmount: String? = null  // Food/item cost (for FOOD_DELIVERY)
    private var taskReward: String = ""          // Delivery fee / reward
    private var taskLocation: String = ""
    private var taskDeadline: String = ""
    private var taskType: String = ""

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
        taskOrderAmount = arguments?.getString("orderAmount")
        taskReward = arguments?.getString("reward") ?: ""
        taskLocation = arguments?.getString("location") ?: ""
        requesterName = arguments?.getString("requesterName") ?: ""
        requesterId = arguments?.getString("requesterId") ?: ""
        requesterAvatar = arguments?.getString("requesterAvatar") ?: "default"
        providerId = arguments?.getString("providerId")
        providerName = arguments?.getString("providerName")
        providerAvatar = arguments?.getString("providerAvatar")
        currentStatus = arguments?.getString("status") ?: "PENDING"
        taskDeadline = arguments?.getString("timeLimit") ?: "No Deadline"
        taskType = arguments?.getString("taskType") ?: ""
        val timestamp = arguments?.getLong("timestamp") ?: 0

        // Bind UI
        binding.tvTaskTitle.text = taskTitle
        binding.tvTaskDescription.text = taskDescription

        // Display price based on task type
        if (taskType.uppercase() == "FOOD_DELIVERY" && !taskOrderAmount.isNullOrEmpty()) {
            // Food delivery: show both order amount and delivery fee with different colors
            val orderLine = "Order: RM $taskOrderAmount"
            val feeLine = "Fee: RM $taskReward"
            val fullText = "$orderLine\n$feeLine"

            val spannable = SpannableString(fullText)
            // Order line in green
            val greenColor = Color.parseColor("#4CAF50")
            spannable.setSpan(
                ForegroundColorSpan(greenColor),
                0,
                orderLine.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // Fee line in red
            val redColor = ContextCompat.getColor(requireContext(), R.color.error)
            spannable.setSpan(
                ForegroundColorSpan(redColor),
                orderLine.length + 1,  // +1 for newline
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvTaskPrice.text = spannable
        } else {
            // Other task types: just show reward
            binding.tvTaskPrice.text = "RM $taskReward"
        }

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

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from EditTaskFragment
        if (currentTaskId.isNotEmpty() && currentUserId.isNotEmpty()) {
            lifecycleScope.launch {
                refreshTaskStatus()
            }
        }
    }

    private suspend fun refreshTaskStatus() {
        try {
            val result = repository.getErrandById(currentTaskId)
            result.onSuccess { task ->
                if (task == null || _binding == null) return@onSuccess

                // Refresh all task data from Firebase
                taskTitle = task["title"] as? String ?: taskTitle
                taskDescription = task["description"] as? String ?: taskDescription
                taskOrderAmount = (task["orderAmount"] as? Number)?.let { String.format("%.2f", it.toDouble()) }
                taskReward = (task["reward"] as? Number)?.let { String.format("%.2f", it.toDouble()) } ?: taskReward
                taskLocation = task["location"] as? String
                    ?: task["deliveryLocation"] as? String
                    ?: taskLocation
                taskDeadline = task["timeLimit"] as? String ?: taskDeadline
                taskType = task["type"] as? String ?: taskType

                providerId = task["providerId"] as? String
                providerName = task["providerName"] as? String
                providerAvatar = task["providerAvatar"] as? String
                currentStatus = task["status"] as? String ?: "PENDING"

                // Update UI with refreshed data
                updateUI()
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

    /**
     * Update UI with current task data
     */
    private fun updateUI() {
        if (_binding == null) return

        binding.tvTaskTitle.text = taskTitle
        binding.tvTaskDescription.text = taskDescription

        // Display price based on task type
        if (taskType.uppercase() == "FOOD_DELIVERY" && !taskOrderAmount.isNullOrEmpty()) {
            val orderLine = "Order: RM $taskOrderAmount"
            val feeLine = "Fee: RM $taskReward"
            val fullText = "$orderLine\n$feeLine"

            val spannable = SpannableString(fullText)
            val greenColor = Color.parseColor("#4CAF50")
            spannable.setSpan(
                ForegroundColorSpan(greenColor),
                0,
                orderLine.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            val redColor = ContextCompat.getColor(requireContext(), R.color.error)
            spannable.setSpan(
                ForegroundColorSpan(redColor),
                orderLine.length + 1,
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvTaskPrice.text = spannable
        } else {
            binding.tvTaskPrice.text = "RM $taskReward"
        }

        binding.tvTaskLocation.text = taskLocation
        binding.tvTaskDeadline.text = "Deadline: $taskDeadline"
        binding.tvTaskDeadline.visibility = if (taskDeadline == "No Deadline") View.GONE else View.VISIBLE
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
                // Show provider actions: Chat + Drop Task + Start Delivering
                binding.layoutProviderActions.visibility = View.VISIBLE
                binding.btnChatCustomer.setOnClickListener { chatWithCustomer() }
                binding.btnProviderAction.text = "Start Delivering"
                binding.btnProviderAction.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.errand_delivering)
                )
                binding.btnProviderAction.setOnClickListener { startDelivering() }
                binding.btnDropTask.setOnClickListener { confirmDrop() }
            }
            "DELIVERING" -> {
                // Show provider actions: Chat + Drop Task + Mark Complete
                binding.layoutProviderActions.visibility = View.VISIBLE
                binding.btnChatCustomer.setOnClickListener { chatWithCustomer() }
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
        binding.btnProviderAction.isEnabled = false

        lifecycleScope.launch {
            val result = repository.completeErrand(currentTaskId)

            if (_binding == null) return@launch

            result.onSuccess {
                Toast.makeText(requireContext(), "Task completed!", Toast.LENGTH_SHORT).show()
                // Navigate back after completing
                parentFragmentManager.popBackStack()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                if (_binding != null) {
                    binding.btnProviderAction.isEnabled = true
                }
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
                // Only pass the other participant's ID (not current user)
                val result = messageRepository.createConversation(
                    participantIds = listOf(riderId),
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
                        // Save task details so we can restore when coming back
                        saveTaskDetailsForRestore()

                        val bundle = bundleOf(
                            "conversationId" to conversationId,
                            "participantId" to riderId,
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
     * Open chat with the customer (task requester)
     * Used by riders/providers to contact the customer
     */
    private fun chatWithCustomer() {
        if (requesterId.isEmpty()) {
            Toast.makeText(requireContext(), "Customer information not available", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Get current user (rider) info
                val currentUserName = tokenManager.getFullName().first() ?: "Rider"

                // Create or get existing conversation
                val result = messageRepository.createConversation(
                    participantIds = listOf(requesterId),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    isGroup = false
                )

                if (_binding == null) return@launch

                result.onSuccess { conversationId ->
                    // Update conversation participant info for current user (rider)
                    messageRepository.updateConversationParticipantInfo(
                        userId = currentUserId,
                        conversationId = conversationId,
                        participantName = requesterName.ifEmpty { "Customer" },
                        participantAvatar = requesterAvatar
                    )

                    // Update conversation participant info for customer
                    val myName = tokenManager.getFullName().first() ?: "Rider"
                    val myAvatar = tokenManager.getAvatar().first()
                    messageRepository.updateConversationParticipantInfo(
                        userId = requesterId,
                        conversationId = conversationId,
                        participantName = myName,
                        participantAvatar = myAvatar
                    )

                    // Also update participantId for both users
                    messageRepository.updateConversationParticipantId(
                        userId = currentUserId,
                        conversationId = conversationId,
                        participantId = requesterId
                    )
                    messageRepository.updateConversationParticipantId(
                        userId = requesterId,
                        conversationId = conversationId,
                        participantId = currentUserId
                    )

                    // Navigate to ChatDetailFragment
                    try {
                        // Save task details so we can restore when coming back
                        saveTaskDetailsForRestore()

                        val bundle = bundleOf(
                            "conversationId" to conversationId,
                            "participantId" to requesterId,
                            "participantName" to requesterName.ifEmpty { "Customer" },
                            "participantAvatar" to requesterAvatar,
                            "isOnline" to false
                        )

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
     * Save task details so ErrandFragment can restore TaskDetailFragment when coming back from chat
     */
    private fun saveTaskDetailsForRestore() {
        val bundle = Bundle().apply {
            putString("taskId", currentTaskId)
            putString("title", taskTitle)
            putString("description", taskDescription)
            putString("orderAmount", taskOrderAmount)
            putString("reward", taskReward)
            putString("location", taskLocation)
            putString("requesterName", requesterName)
            putString("requesterId", requesterId)
            putString("requesterAvatar", requesterAvatar)
            putString("providerId", providerId)
            putString("providerName", providerName)
            putString("providerAvatar", providerAvatar)
            putString("status", currentStatus)
            putString("timeLimit", taskDeadline)
            putString("taskType", taskType)
        }
        ErrandFragment.savePendingTaskDetails(bundle)
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
                putString("reward", taskReward)
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
