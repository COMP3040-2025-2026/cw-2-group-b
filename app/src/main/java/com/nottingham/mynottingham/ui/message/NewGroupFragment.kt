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
import com.nottingham.mynottingham.databinding.FragmentNewGroupBinding
import com.nottingham.mynottingham.util.Constants

/**
 * Fragment for creating a new group conversation
 */
class NewGroupFragment : Fragment() {

    private var _binding: FragmentNewGroupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewGroupViewModel by viewModels()
    private lateinit var adapter: SelectableContactAdapter

    private var token: String = ""
    private var currentUserId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get credentials from SharedPreferences
        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        token = prefs.getString(Constants.KEY_USER_TOKEN, "") ?: ""
        currentUserId = prefs.getString(Constants.KEY_USER_ID, "") ?: ""

        setupToolbar()
        setupRecyclerView()
        setupGroupNameInput()
        setupCreateButton()
        setupObservers()

        // Load contacts
        viewModel.loadContacts(token)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = SelectableContactAdapter { selectedContacts ->
            // Update selected count
            val count = selectedContacts.size
            binding.textSelectedCount.text = "Select participants ($count selected)"

            // Enable/disable create button based on selection
            updateCreateButtonState()
        }

        binding.recyclerContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NewGroupFragment.adapter
        }
    }

    private fun setupGroupNameInput() {
        binding.editGroupName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCreateButtonState()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCreateButton() {
        binding.buttonCreateGroup.setOnClickListener {
            val groupName = binding.editGroupName.text?.toString()?.trim() ?: ""
            val selectedContacts = adapter.getSelectedContacts()

            viewModel.createGroup(token, groupName, selectedContacts)
        }
    }

    private fun updateCreateButtonState() {
        val hasGroupName = !binding.editGroupName.text.isNullOrBlank()
        val hasSelectedContacts = adapter.getSelectedContacts().size >= 2

        binding.buttonCreateGroup.isEnabled = hasGroupName && hasSelectedContacts
    }

    private fun setupObservers() {
        // Observe contacts
        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            adapter.submitList(contacts)
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonCreateGroup.isEnabled = !isLoading &&
                !binding.editGroupName.text.isNullOrBlank() &&
                adapter.getSelectedContacts().size >= 2
        }

        // Observe group created
        viewModel.groupCreated.observe(viewLifecycleOwner) { conversationId ->
            conversationId?.let {
                Toast.makeText(context, "Group created successfully", Toast.LENGTH_SHORT).show()

                // Navigate to chat detail for the new group
                // First get the group details from the adapter
                val selectedContacts = adapter.getSelectedContacts()
                val groupName = binding.editGroupName.text?.toString()?.trim() ?: "Group"

                // Navigate to chat detail
                val action = NewGroupFragmentDirections.actionNewGroupToChatDetail(
                    conversationId = it,
                    participantName = groupName,
                    participantAvatar = null,
                    isOnline = false
                )
                findNavController().navigate(action)

                viewModel.resetGroupCreated()
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
