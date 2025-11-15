package com.nottingham.mynottingham.ui.message

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentMessageBinding
import com.nottingham.mynottingham.util.Constants

/**
 * Message fragment showing conversation list
 */
class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MessageViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter

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

        // Get user credentials from SharedPreferences
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        currentUserId = prefs.getString(Constants.KEY_USER_ID, "") ?: ""
        token = prefs.getString(Constants.KEY_USER_TOKEN, "") ?: ""

        // Set current user ID in ViewModel
        viewModel.setCurrentUserId(currentUserId)

        setupRecyclerView()
        setupSearchView()
        setupFab()
        setupObservers()

        // Sync conversations from API
        viewModel.syncConversations(token)
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
                // Toggle pinned status
                viewModel.togglePinned(token, conversation.id, conversation.isPinned)
                Toast.makeText(
                    context,
                    if (conversation.isPinned) "Unpinned" else "Pinned",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        binding.recyclerConversations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MessageFragment.adapter
        }
    }

    private fun setupSearchView() {
        // Note: You may need to add a SearchView to fragment_message.xml
        // For now, we'll skip this if it doesn't exist in the layout
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
