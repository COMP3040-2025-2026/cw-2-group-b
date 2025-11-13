package com.nottingham.mynottingham.ui.message

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentChatDetailBinding
import com.nottingham.mynottingham.util.Constants

/**
 * Chat detail screen showing conversation messages
 */
class ChatDetailFragment : Fragment() {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var adapter: ChatMessageAdapter

    // Arguments - TODO: Use Safe Args when available
    // private val args: ChatDetailFragmentArgs by navArgs()
    private var conversationId: String = ""
    private var participantName: String = ""
    private var participantAvatar: String? = null
    private var isOnline: Boolean = false
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

        // Get user credentials from SharedPreferences
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        currentUserId = prefs.getString(Constants.KEY_USER_ID, "") ?: ""
        token = prefs.getString(Constants.KEY_USER_TOKEN, "") ?: ""

        // Get arguments from bundle
        conversationId = arguments?.getString("conversationId") ?: ""
        participantName = arguments?.getString("participantName") ?: ""
        participantAvatar = arguments?.getString("participantAvatar")
        isOnline = arguments?.getBoolean("isOnline", false) ?: false

        setupToolbar()
        setupRecyclerView()
        setupInputField()
        setupObservers()

        // Initialize chat if conversationId is available
        if (conversationId.isNotEmpty()) {
            viewModel.initializeChat(conversationId, currentUserId)
            viewModel.loadMessages(token)
            viewModel.markAsRead(token)
        }
    }

    private fun setupToolbar() {
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
                viewModel.sendMessage(token, message)
            }
        }

        // Text change listener for typing indicator
        binding.editMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()

                if (hasText && !isTyping) {
                    isTyping = true
                    viewModel.updateTyping(token, true)
                } else if (!hasText && isTyping) {
                    isTyping = false
                    viewModel.updateTyping(token, false)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupObservers() {
        // Observe messages
        viewModel.messages.observe(viewLifecycleOwner) { messagesLiveData ->
            messagesLiveData?.observe(viewLifecycleOwner) { messages ->
                if (messages.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.recyclerMessages.visibility = View.GONE
                    binding.textParticipantName.text = participantName
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.recyclerMessages.visibility = View.VISIBLE
                    adapter.submitList(messages.reversed()) // Reverse to show newest at bottom
                    binding.recyclerMessages.scrollToPosition(adapter.itemCount - 1)
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
    }

    override fun onPause() {
        super.onPause()
        // Stop typing indicator when leaving screen
        if (isTyping) {
            viewModel.updateTyping(token, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
