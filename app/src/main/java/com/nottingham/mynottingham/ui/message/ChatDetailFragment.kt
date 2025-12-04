package com.nottingham.mynottingham.ui.message

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.Contact
import com.nottingham.mynottingham.data.model.GroupMember
import com.nottingham.mynottingham.data.model.GroupRole
import com.nottingham.mynottingham.databinding.BottomSheetAddMemberBinding
import com.nottingham.mynottingham.databinding.BottomSheetGroupInfoBinding
import com.nottingham.mynottingham.databinding.BottomSheetUserProfileBinding
import com.nottingham.mynottingham.databinding.FragmentChatDetailBinding
import com.nottingham.mynottingham.util.AvatarUtils
import com.nottingham.mynottingham.util.Constants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Chat detail screen showing conversation messages
 */
class ChatDetailFragment : Fragment() {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var adapter: ChatMessageAdapter
    private lateinit var tokenManager: TokenManager

    // Arguments - Using Safe Args
    private val args: ChatDetailFragmentArgs by navArgs()
    private var currentUserId: String = ""
    private var token: String = ""
    private var isTyping = false

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            viewModel.sendImageMessage(imageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize TokenManager
        tokenManager = TokenManager(requireContext())

        // Get arguments from Safe Args
        val conversationId = args.conversationId
        val participantName = args.participantName
        val participantId = args.participantId
        val participantAvatar = args.participantAvatar
        val isOnline = args.isOnline
        val isGroup = args.isGroup

        setupToolbar(participantName, participantAvatar, isOnline, isGroup)
        setupInputField()
        setupObservers(participantName, isGroup)

        // Observe participant's real-time presence status (only for 1:1 chats)
        if (!isGroup && participantId.isNotEmpty()) {
            viewModel.observeParticipantPresence(participantId)
        }

        // Get user credentials from DataStore and initialize chat
        lifecycleScope.launch {
            currentUserId = tokenManager.getUserId().firstOrNull() ?: ""
            token = tokenManager.getToken().firstOrNull()?.removePrefix("Bearer ")?.trim() ?: ""

            // Setup RecyclerView AFTER currentUserId is loaded
            setupRecyclerView()

            // Initialize chat if conversationId is available
            if (conversationId.isNotEmpty()) {
                // Use fullName for display (e.g., "Bob Chen"), not username (e.g., "student2")
                val currentUserName = tokenManager.getFullName().firstOrNull() ?: ""
                val currentUserAvatar = tokenManager.getAvatar().firstOrNull()
                viewModel.initializeChat(conversationId, currentUserId, currentUserName, currentUserAvatar)
                viewModel.markAsRead()

                // For group chats, observe membership status to detect removal
                if (isGroup) {
                    viewModel.observeGroupMembership()
                }
            }
        }
    }

    private fun setupToolbar(participantName: String, participantAvatar: String?, isOnline: Boolean, isGroup: Boolean) {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Set participant info
        binding.textName.text = participantName
        binding.textStatus.text = if (isGroup) {
            "Group Chat"
        } else if (isOnline) {
            "Active now"
        } else {
            "Offline"
        }

        // Set avatar
        if (isGroup) {
            binding.imageAvatar.setImageResource(R.drawable.ic_group)
        } else {
            binding.imageAvatar.setImageResource(AvatarUtils.getDrawableId(participantAvatar))
        }

        // More button click
        binding.buttonMore.setOnClickListener {
            if (isGroup) {
                showGroupInfoBottomSheet()
            } else {
                showUserProfileBottomSheet(args.participantId)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatMessageAdapter(currentUserId)

        binding.recyclerMessages.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true // Start from bottom
                reverseLayout = false
            }
            adapter = this@ChatDetailFragment.adapter
        }
    }

    private fun setupInputField() {
        // Send button click
        binding.buttonSend.setOnClickListener {
            val message = binding.editMessage.text?.toString()?.trim()
            if (!message.isNullOrEmpty()) {
                viewModel.sendMessage(message)
            }
        }

        // Image attachment button click
        binding.buttonAttachImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Text change listener for typing indicator
        binding.editMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()

                if (hasText && !isTyping) {
                    isTyping = true
                    viewModel.updateTyping(true)
                } else if (!hasText && isTyping) {
                    isTyping = false
                    viewModel.updateTyping(false)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers(participantName: String, isGroup: Boolean) {
        // Observe messages
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            if (messages.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.recyclerMessages.visibility = View.GONE
                binding.textParticipantName.text = participantName
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerMessages.visibility = View.VISIBLE
                adapter.submitList(messages) {
                    // Scroll to bottom after list is updated
                    binding.recyclerMessages.post {
                        binding.recyclerMessages.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe sending message state
        viewModel.sendingMessage.observe(viewLifecycleOwner) { isSending ->
            binding.buttonSend.isEnabled = !isSending
            binding.buttonAttachImage.isEnabled = !isSending
        }

        // Observe image uploading state
        viewModel.uploadingImage.observe(viewLifecycleOwner) { isUploading ->
            binding.buttonAttachImage.alpha = if (isUploading) 0.5f else 1.0f
        }

        // Observe message sent
        viewModel.messageSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                binding.editMessage.text?.clear()
                viewModel.resetMessageSent()
            }
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Only observe typing and presence for 1:1 chats
        if (!isGroup) {
            // Observe typing status - typing takes priority over presence status
            viewModel.typingStatus.observe(viewLifecycleOwner) { typingText ->
                if (typingText != null) {
                    binding.textStatus.text = typingText
                }
                // If no typing, let participantStatus handle it
            }

            // Observe participant's real-time presence status (Telegram-style)
            viewModel.participantStatus.observe(viewLifecycleOwner) { status ->
                // Only update if not showing typing indicator
                if (viewModel.typingStatus.value == null) {
                    binding.textStatus.text = status
                }
            }
        } else {
            // For group chats, observe membership status
            viewModel.membershipStatus.observe(viewLifecycleOwner) { status ->
                if (status == "REMOVED") {
                    // Hide input layout and show removed notice
                    binding.layoutInput.visibility = View.GONE
                    binding.layoutRemovedNotice.visibility = View.VISIBLE
                    // Update toolbar status
                    binding.textStatus.text = "You have been removed"
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop typing indicator when leaving screen
        if (isTyping) {
            viewModel.updateTyping(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ========== Bottom Sheets ==========

    private fun showUserProfileBottomSheet(userId: String) {
        if (userId.isEmpty()) return

        viewModel.loadUserProfile(userId)

        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetUserProfileBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        // Observe user profile
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                sheetBinding.apply {
                    tvName.text = it["name"] as? String ?: "Unknown"
                    tvEmail.text = it["email"] as? String ?: ""

                    val userRole = it["role"] as? String ?: "STUDENT"
                    tvRole.text = if (userRole == "TEACHER") "Teacher" else "Student"

                    // Avatar
                    ivAvatar.setImageResource(AvatarUtils.getDrawableId(it["avatar"] as? String))

                    // Faculty
                    val faculty = it["faculty"] as? String
                    if (!faculty.isNullOrEmpty()) {
                        tvFaculty.text = faculty
                        tvFacultyLabel.text = if (userRole == "TEACHER") "Department" else "Faculty"
                        layoutFaculty.isVisible = true
                    } else {
                        layoutFaculty.isVisible = false
                    }

                    // Program (students only)
                    val program = it["program"] as? String
                    if (!program.isNullOrEmpty() && userRole != "TEACHER") {
                        tvProgram.text = program
                        layoutProgram.isVisible = true
                    } else {
                        layoutProgram.isVisible = false
                    }

                    // Year (students only)
                    val year = it["year"] as? String
                    if (!year.isNullOrEmpty() && userRole != "TEACHER") {
                        tvYear.text = "Year $year"
                        layoutYear.isVisible = true
                        dividerYear.isVisible = true
                    } else {
                        layoutYear.isVisible = false
                        dividerYear.isVisible = false
                    }
                }
            }
        }

        bottomSheet.show()
    }

    private fun showGroupInfoBottomSheet() {
        viewModel.loadGroupInfo()

        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetGroupInfoBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        var memberAdapter: GroupMemberAdapter? = null

        // Observe group info
        viewModel.groupInfo.observe(viewLifecycleOwner) { info ->
            info?.let {
                sheetBinding.tvGroupName.text = it.name
                sheetBinding.tvMemberCount.text = "${it.members.size} members"
            }
        }

        // Observe current user role
        viewModel.currentUserRole.observe(viewLifecycleOwner) { role ->
            val isOwnerOrAdmin = role == GroupRole.OWNER || role == GroupRole.ADMIN
            val isOwner = role == GroupRole.OWNER

            sheetBinding.apply {
                // Show edit button for owner/admin
                btnEditName.isVisible = isOwnerOrAdmin
                btnAddMember.isVisible = isOwnerOrAdmin

                // Show admin actions
                layoutAdminActions.isVisible = true
                btnLeaveGroup.isVisible = !isOwner
                btnDissolveGroup.isVisible = isOwner
            }

            // Update adapter with new role
            memberAdapter?.let { adapter ->
                viewModel.groupMembers.value?.let { members ->
                    adapter.submitList(members.toList())
                }
            }
        }

        // Observe group members
        viewModel.groupMembers.observe(viewLifecycleOwner) { members ->
            val role = viewModel.currentUserRole.value ?: GroupRole.MEMBER

            memberAdapter = GroupMemberAdapter(
                currentUserId = currentUserId,
                currentUserRole = role,
                onMemberClick = { member ->
                    bottomSheet.dismiss()
                    showMemberProfileBottomSheet(member)
                },
                onSetAdmin = { member ->
                    viewModel.setAsAdmin(member.id)
                },
                onRemoveAdmin = { member ->
                    viewModel.removeAdmin(member.id)
                },
                onRemoveMember = { member ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Remove Member")
                        .setMessage("Remove ${member.name} from the group?")
                        .setPositiveButton("Remove") { _, _ ->
                            viewModel.removeGroupMember(member.id)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                },
                onTransferOwnership = { member ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Transfer Ownership")
                        .setMessage("Transfer group ownership to ${member.name}?")
                        .setPositiveButton("Transfer") { _, _ ->
                            viewModel.transferOwnership(member.id)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            )

            sheetBinding.recyclerMembers.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = memberAdapter
            }
            memberAdapter?.submitList(members)
        }

        // Edit group name
        sheetBinding.btnEditName.setOnClickListener {
            val input = EditText(requireContext()).apply {
                setText(viewModel.groupInfo.value?.name ?: "")
                hint = "Group name"
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Edit Group Name")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        viewModel.updateGroupName(newName)
                        sheetBinding.tvGroupName.text = newName
                        binding.textName.text = newName
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Add member button
        sheetBinding.btnAddMember.setOnClickListener {
            bottomSheet.dismiss()
            val existingMemberIds = viewModel.groupMembers.value?.map { it.id } ?: emptyList()
            showAddMemberBottomSheet(existingMemberIds)
        }

        // Leave group
        sheetBinding.btnLeaveGroup.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Leave") { _, _ ->
                    viewModel.leaveGroup()
                    bottomSheet.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Dissolve group
        sheetBinding.btnDissolveGroup.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Dissolve Group")
                .setMessage("Are you sure you want to dissolve this group? This action cannot be undone.")
                .setPositiveButton("Dissolve") { _, _ ->
                    viewModel.dissolveGroup()
                    bottomSheet.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Observe operation success
        viewModel.operationSuccess.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    "LEFT_GROUP", "GROUP_DISSOLVED" -> {
                        bottomSheet.dismiss()
                        findNavController().navigateUp()
                    }
                    else -> {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                }
                viewModel.clearOperationSuccess()
            }
        }

        bottomSheet.show()
    }

    private fun showMemberProfileBottomSheet(member: GroupMember) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetUserProfileBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        sheetBinding.apply {
            tvName.text = member.name
            tvEmail.text = member.email ?: ""
            tvRole.text = if (member.userRole == "TEACHER") "Teacher" else "Student"
            ivAvatar.setImageResource(AvatarUtils.getDrawableId(member.avatar))

            // Faculty
            if (!member.faculty.isNullOrEmpty()) {
                tvFaculty.text = member.faculty
                tvFacultyLabel.text = if (member.userRole == "TEACHER") "Department" else "Faculty"
                layoutFaculty.isVisible = true
            } else {
                layoutFaculty.isVisible = false
            }

            // Program
            if (!member.program.isNullOrEmpty() && member.userRole != "TEACHER") {
                tvProgram.text = member.program
                layoutProgram.isVisible = true
            } else {
                layoutProgram.isVisible = false
            }

            // Year
            if (!member.year.isNullOrEmpty() && member.userRole != "TEACHER") {
                tvYear.text = "Year ${member.year}"
                layoutYear.isVisible = true
                dividerYear.isVisible = true
            } else {
                layoutYear.isVisible = false
                dividerYear.isVisible = false
            }

            // Show message button if not self
            if (member.id != currentUserId) {
                btnMessage.isVisible = true
                btnMessage.setOnClickListener {
                    bottomSheet.dismiss()
                    // TODO: Navigate to 1:1 chat with this member
                    // This would require creating or finding an existing conversation first
                    Toast.makeText(context, "Private chat with ${member.name} coming soon", Toast.LENGTH_SHORT).show()
                }
            }
        }

        bottomSheet.show()
    }

    private fun showAddMemberBottomSheet(existingMemberIds: List<String>) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetAddMemberBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        // Load available contacts
        viewModel.loadAvailableContacts(existingMemberIds)

        // Setup adapter
        val adapter = SelectableContactAdapter { selectedContacts ->
            val count = selectedContacts.size
            sheetBinding.tvSelectedCount.text = "$count selected"
            sheetBinding.btnAddMembers.isEnabled = count > 0
        }

        sheetBinding.recyclerContacts.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        // Show loading
        sheetBinding.progressBar.visibility = View.VISIBLE
        sheetBinding.recyclerContacts.visibility = View.GONE
        sheetBinding.layoutEmpty.visibility = View.GONE

        // Observe available contacts
        viewModel.availableContacts.observe(viewLifecycleOwner) { contacts ->
            sheetBinding.progressBar.visibility = View.GONE

            if (contacts.isEmpty()) {
                sheetBinding.layoutEmpty.visibility = View.VISIBLE
                sheetBinding.recyclerContacts.visibility = View.GONE
            } else {
                sheetBinding.layoutEmpty.visibility = View.GONE
                sheetBinding.recyclerContacts.visibility = View.VISIBLE
                adapter.submitList(contacts)
            }
        }

        // Add members button
        sheetBinding.btnAddMembers.setOnClickListener {
            val selectedIds = adapter.getSelectedContacts().map { it.id }
            if (selectedIds.isNotEmpty()) {
                viewModel.addGroupMembers(selectedIds)
                bottomSheet.dismiss()
            }
        }

        bottomSheet.show()
    }
}
