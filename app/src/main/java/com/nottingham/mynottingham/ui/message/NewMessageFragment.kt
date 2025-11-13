package com.nottingham.mynottingham.ui.message

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.databinding.FragmentNewMessageBinding
import com.nottingham.mynottingham.util.Constants

/**
 * New message screen showing contact suggestions
 */
class NewMessageFragment : Fragment() {

    private var _binding: FragmentNewMessageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewMessageViewModel by viewModels()
    private lateinit var adapter: ContactSuggestionAdapter

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

        // Get token from SharedPreferences
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        token = prefs.getString(Constants.KEY_USER_TOKEN, "") ?: ""

        setupToolbar()
        setupRecyclerView()
        setupObservers()

        // Load contacts
        viewModel.loadContactSuggestions(token)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = ContactSuggestionAdapter { contact ->
            // Create conversation with selected contact
            viewModel.createOneOnOneConversation(token, contact.userId)
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
                // TODO: Navigate to chat detail screen when Safe Args is available
                Toast.makeText(context, "Conversation created with ${it.participantName}", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
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
