package com.nottingham.mynottingham.ui.message

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.databinding.FragmentChatDetailBinding
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

        setupToolbar(participantName, participantAvatar, isOnline)
        setupInputField()
        setupObservers(participantName)

        // Observe participant's real-time presence status
        if (participantId.isNotEmpty()) {
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
                val currentUserName = tokenManager.getUsername().firstOrNull() ?: ""
                viewModel.initializeChat(conversationId, currentUserId, currentUserName)
                viewModel.markAsRead()
            }
        }
    }

    private fun setupToolbar(participantName: String, participantAvatar: String?, isOnline: Boolean) {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Set participant info
        binding.textName.text = participantName
        binding.textStatus.text = if (isOnline) "Active now" else "Offline"

        // Set avatar initials
        val initials = participantName.split(" ")
            .take(2)
            .map { it.firstOrNull()?.uppercase() ?: "" }
            .joinToString("")
        binding.imageAvatar.setImageDrawable(null) // Clear previous image
        // TODO: Set avatar properly when ImageView is available in layout
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

    private fun setupObservers(participantName: String) {
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
}
