package com.nottingham.mynottingham.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.model.Conversation
import com.nottingham.mynottingham.databinding.BottomSheetConversationActionsBinding
import com.nottingham.mynottingham.databinding.FragmentMessageBinding
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Message fragment showing conversation list
 */
class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter
    private lateinit var tokenManager: TokenManager

    private var currentUserId: String = ""
    private var token: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize TokenManager
        tokenManager = TokenManager(requireContext())

        setupRecyclerView()
        setupSearchView()
        setupFab()
        setupObservers()

        // Get user credentials from DataStore (both userId and token)
        lifecycleScope.launch {
            // Get userId from DataStore
            currentUserId = tokenManager.getUserId().firstOrNull() ?: ""
            viewModel.setCurrentUserId(currentUserId)

            // Get token from DataStore
            token = tokenManager.getToken().firstOrNull() ?: ""
            // Remove "Bearer " prefix if present (for backward compatibility)
            token = token.removePrefix("Bearer ").trim()

            if (token.isNotEmpty()) {
                viewModel.syncConversations(token)
            } else {
                Toast.makeText(context, "No authentication token found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFab() {
        binding.fabNewMessage.setOnClickListener {
            findNavController().navigate(R.id.action_message_to_new_message)
        }
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter(
            onConversationClick = { conversation ->
                // TODO: Navigate to chat detail screen
                // Temporarily disabled until Safe Args generates navigation classes
                Toast.makeText(context, "Chat with ${conversation.participantName}", Toast.LENGTH_SHORT).show()
            },
            onConversationLongClick = { conversation ->
                // Show action bottom sheet
                showConversationActions(conversation)
            }
        )

        binding.recyclerConversations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MessageFragment.adapter
            // Disable item animations to prevent jump effect
            itemAnimator = null
        }

        // Setup swipe gestures
        setupSwipeGestures()
    }

    private fun setupSwipeGestures() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val conversation = adapter.currentList[position]

                // Show action bottom sheet
                showConversationActions(conversation)

                // Reset the swipe
                adapter.notifyItemChanged(position)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerConversations)
    }

    private fun setupSearchView() {
        // Note: You may need to add a SearchView to fragment_message.xml
        // For now, we'll skip this if it doesn't exist in the layout
    }

    private fun showConversationActions(conversation: Conversation) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val binding = BottomSheetConversationActionsBinding.inflate(layoutInflater)
        bottomSheet.setContentView(binding.root)

        // Update pin text and icon based on current status
        if (conversation.isPinned) {
            binding.tvPin.text = "Unpin conversation"
        } else {
            binding.tvPin.text = "Pin conversation"
        }

        // Update mark read text based on unread count
        if (conversation.unreadCount > 0) {
            binding.tvMarkRead.text = "Mark as read"
        } else {
            binding.tvMarkRead.text = "Mark as unread"
        }

        // Pin/Unpin action
        binding.layoutPin.setOnClickListener {
            lifecycleScope.launch {
                val currentToken = tokenManager.getToken().firstOrNull()?.removePrefix("Bearer ")?.trim() ?: ""
                if (currentToken.isNotEmpty()) {
                    viewModel.togglePinned(currentToken, conversation.id, conversation.isPinned)
                    Toast.makeText(
                        context,
                        if (conversation.isPinned) "Unpinned" else "Pinned",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            bottomSheet.dismiss()
        }

        // Mark as read/unread action
        binding.layoutMarkRead.setOnClickListener {
            lifecycleScope.launch {
                val currentToken = tokenManager.getToken().firstOrNull()?.removePrefix("Bearer ")?.trim() ?: ""
                if (currentToken.isNotEmpty()) {
                    if (conversation.unreadCount > 0) {
                        viewModel.markAsRead(currentToken, conversation.id, currentUserId)
                        Toast.makeText(context, "Marked as read", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.markAsUnread(conversation.id)
                        Toast.makeText(context, "Marked as unread", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            bottomSheet.dismiss()
        }

        // Delete action
        binding.layoutDelete.setOnClickListener {
            lifecycleScope.launch {
                viewModel.deleteConversation(conversation.id)
                Toast.makeText(context, "Conversation deleted", Toast.LENGTH_SHORT).show()
            }
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun setupObservers() {
        // Observe conversations
        viewModel.conversations.observe(viewLifecycleOwner) { conversations ->
            if (conversations.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.recyclerConversations.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerConversations.visibility = View.VISIBLE
                adapter.submitList(conversations)
            }
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
