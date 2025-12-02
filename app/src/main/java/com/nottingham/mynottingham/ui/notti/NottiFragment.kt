package com.nottingham.mynottingham.ui.notti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentNottiBinding

/**
 * NottiFragment - Notti AI Assistant UI
 *
 * Provides AI conversation functionality using Firebase AI Logic (Gemini)
 */
class NottiFragment : Fragment() {

    private var _binding: FragmentNottiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NottiViewModel by viewModels()
    private lateinit var chatAdapter: NottiMessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNottiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupInputArea()
        setupQuickActions()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = NottiMessageAdapter(
            onBookNowClick = {
                findNavController().navigate(R.id.action_notti_to_booking)
            }
        )
        binding.recyclerMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupInputArea() {
        // Send button click
        binding.fabSend.setOnClickListener {
            sendMessage()
        }

        // Keyboard send
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun setupQuickActions() {
        binding.chipShuttle.setOnClickListener {
            viewModel.handleQuickAction(NottiViewModel.QuickAction.SHUTTLE)
        }

        binding.chipBooking.setOnClickListener {
            viewModel.handleQuickAction(NottiViewModel.QuickAction.BOOKING)
        }

        binding.chipHelp.setOnClickListener {
            viewModel.handleQuickAction(NottiViewModel.QuickAction.HELP)
        }
    }

    private fun observeViewModel() {
        // Observe message list
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages) {
                // Scroll to latest message
                if (messages.isNotEmpty()) {
                    binding.recyclerMessages.smoothScrollToPosition(messages.size - 1)
                }
            }

            // Show/hide empty state
            binding.layoutEmpty.isVisible = messages.size <= 1
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.fabSend.isEnabled = !isLoading
        }
    }

    private fun sendMessage() {
        val message = binding.etMessage.text?.toString()?.trim()
        if (!message.isNullOrBlank()) {
            viewModel.sendMessage(message)
            binding.etMessage.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
