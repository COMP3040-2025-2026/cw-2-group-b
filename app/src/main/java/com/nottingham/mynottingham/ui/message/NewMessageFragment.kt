package com.nottingham.mynottingham.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.databinding.FragmentNewMessageBinding
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * New message screen showing contact suggestions
 */
class NewMessageFragment : Fragment() {

    private var _binding: FragmentNewMessageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewMessageViewModel by viewModels()
    private lateinit var adapter: ContactSuggestionAdapter
    private lateinit var tokenManager: TokenManager

    private var token: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize TokenManager
        tokenManager = TokenManager(requireContext())

        setupToolbar()
        setupSearchView()
        setupAlphabetIndex()
        setupRecyclerView()
        setupObservers()

        // Get token from DataStore and load contacts
        lifecycleScope.launch {
            token = tokenManager.getToken().firstOrNull() ?: ""
            // Remove "Bearer " prefix if present (for backward compatibility)
            token = token.removePrefix("Bearer ").trim()

            if (token.isNotEmpty()) {
                viewModel.loadContactSuggestions(token)
            } else {
                Toast.makeText(context, "No authentication token found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchContacts(newText ?: "")
                return true
            }
        })
    }

    private fun setupAlphabetIndex() {
        binding.alphabetIndex.setOnLetterSelectedListener { letter ->
            val index = viewModel.getIndexForLetter(letter)
            if (index >= 0) {
                binding.recyclerContacts.scrollToPosition(index)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ContactSuggestionAdapter { contact ->
            // Create conversation with selected contact
            lifecycleScope.launch {
                var currentToken = tokenManager.getToken().firstOrNull() ?: ""
                // Remove "Bearer " prefix if present (for backward compatibility)
                currentToken = currentToken.removePrefix("Bearer ").trim()
                if (currentToken.isNotEmpty()) {
                    viewModel.createOneOnOneConversation(currentToken, contact.userId)
                }
            }
        }

        binding.recyclerContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NewMessageFragment.adapter
        }
    }

    private fun setupObservers() {
        // Observe contact suggestions
        viewModel.contactSuggestions.observe(viewLifecycleOwner) { contacts ->
            if (contacts.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.recyclerContacts.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.recyclerContacts.visibility = View.VISIBLE
                adapter.submitList(contacts)
            }
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe conversation created
        viewModel.conversationCreated.observe(viewLifecycleOwner) { conversation ->
            conversation?.let {
                // Navigate to chat detail screen
                val action = NewMessageFragmentDirections.actionNewMessageToChatDetail(
                    conversationId = it.id,
                    participantName = it.participantName,
                    participantId = it.participantId,
                    participantAvatar = it.participantAvatar,
                    isOnline = it.isOnline,
                    isGroup = it.isGroup
                )
                findNavController().navigate(action)
                viewModel.resetConversationCreated()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
