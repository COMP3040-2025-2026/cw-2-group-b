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
            // Show options: New Message or New Group
            val options = arrayOf("New Message", "New Group")
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Create")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> findNavController().navigate(R.id.action_message_to_new_message)
                        1 -> findNavController().navigate(R.id.action_message_to_new_group)
                    }
                }
                .show()
        }
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter(
            onConversationClick = { conversation ->
                // Navigate to chat detail screen using Safe Args
                val action = MessageFragmentDirections.actionMessageToChatDetail(
                    conversationId = conversation.id,
                    participantName = conversation.participantName,
                    participantAvatar = conversation.participantAvatar,
                    isOnline = conversation.isOnline
                )
                findNavController().navigate(action)
            },
            onConversationLongClick = { conversation ->
                // Show action bottom sheet
                showConversationActions(conversation)
            },
            onPinClick = { conversation ->
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
            },
            onDeleteClick = { conversation ->
                lifecycleScope.launch {
                    val currentToken = tokenManager.getToken().firstOrNull()?.removePrefix("Bearer ")?.trim() ?: ""
                    if (currentToken.isNotEmpty()) {
                        viewModel.deleteConversation(currentToken, conversation.id)
                        Toast.makeText(context, "Conversation deleted", Toast.LENGTH_SHORT).show()
                    }
                }
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
            private val swipeThreshold = 160 * resources.displayMetrics.density // 160dp in pixels

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used - we handle swipe in onChildDraw
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val adapterViewHolder = viewHolder as ConversationAdapter.ConversationViewHolder
                    val foreground = adapterViewHolder.foreground

                    // Limit swipe distance to show action buttons (max 160dp)
                    val clampedDx = Math.max(-swipeThreshold, dX)

                    // Translate the foreground view
                    foreground.translationX = clampedDx
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                val adapterViewHolder = viewHolder as ConversationAdapter.ConversationViewHolder
                val foreground = adapterViewHolder.foreground

                // Snap to open or closed position
                if (Math.abs(foreground.translationX) > swipeThreshold / 2) {
                    // Snap to open (show buttons)
                    foreground.animate()
                        .translationX(-swipeThreshold)
                        .setDuration(200)
                        .start()
                } else {
                    // Snap to closed
                    foreground.animate()
                        .translationX(0f)
                        .setDuration(200)
                        .start()
                }
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.5f // Allow full swipe when past 50%
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerConversations)
    }

    private fun setupSearchView() {
        // Setup search view in toolbar
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchConversations(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.searchConversations(it) }
                return true
            }
        })

        // Add search menu item to toolbar
        binding.toolbar.inflateMenu(R.menu.menu_message)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    // Toggle search view visibility
                    if (binding.searchView.visibility == View.VISIBLE) {
                        binding.searchView.visibility = View.GONE
                        binding.searchView.setQuery("", false)
                        binding.searchView.clearFocus()
                        binding.toolbar.title = "Messages"
                    } else {
                        binding.searchView.visibility = View.VISIBLE
                        binding.searchView.isIconified = false
                        binding.searchView.requestFocus()
                        binding.toolbar.title = ""
                    }
                    true
                }
                else -> false
            }
        }
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
                val currentToken = tokenManager.getToken().firstOrNull()?.removePrefix("Bearer ")?.trim() ?: ""
                if (currentToken.isNotEmpty()) {
                    viewModel.deleteConversation(currentToken, conversation.id)
                    Toast.makeText(context, "Conversation deleted", Toast.LENGTH_SHORT).show()
                }
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

    override fun onResume() {
        super.onResume()
        // Refresh conversations when returning from chat detail
        if (token.isNotEmpty()) {
            viewModel.syncConversations(token)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
