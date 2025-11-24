package com.nottingham.mynottingham.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

/**
 * A Fragment responsible for handling user authentication.
 * It provides a user interface for entering credentials and manages the login process
 * by communicating with a [LoginViewModel].
 */
class LoginFragment : Fragment() {

    // Nullable backing field for the view binding.
    private var _binding: FragmentLoginBinding? = null
    // Non-null accessor for the binding, valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Delegates the ViewModel creation to the framework, scoped to this fragment.
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using view binding.
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the user interface elements and observers after the view has been created.
        setupUI()
        observeViewModel()
    }

    /**
     * Configures the UI listeners and initial state.
     */
    private fun setupUI() {
        // Add text changed listeners to clear errors and hide hints as the user types.
        binding.etUsername.addTextChangedListener { text ->
            binding.tilUsername.error = null // Clear previous username errors.
            binding.tvError.visibility = View.GONE // Hide general error message.
            binding.tilUsername.isHintEnabled = text.isNullOrEmpty()
        }

        binding.etPassword.addTextChangedListener { text ->
            binding.tilPassword.error = null // Clear previous password errors.
            binding.tvError.visibility = View.GONE // Hide general error message.
            binding.tilPassword.isHintEnabled = text.isNullOrEmpty()
        }

        // Set a click listener for the login button.
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Proceed with login only if input is valid.
            if (validateInput(username, password)) {
                viewModel.login(username, password)
            }
        }
    }

    /**
     * Performs basic client-side validation on user input.
     * @param username The username entered by the user.
     * @param password The password entered by the user.
     * @return `true` if input is valid, `false` otherwise.
     */
    private fun validateInput(username: String, password: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    /**
     * Subscribes to LiveData/Flows from the ViewModel to update the UI in response to state changes.
     */
    private fun observeViewModel() {
        // Observe the loading state to show/hide the progress bar and disable UI elements.
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnLogin.isEnabled = !isLoading
                binding.etUsername.isEnabled = !isLoading
                binding.etPassword.isEnabled = !isLoading
            }
        }

        // Observe the error state to display error messages to the user.
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    binding.tvError.text = it
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }

        // Observe the login success event to navigate to the home screen.
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginSuccess.collect { success ->
                if (success) {
                    // On successful login, navigate to the home fragment.
                    findNavController().navigate(R.id.action_login_to_home)
                }
            }
        }
    }

    /**
     * Called when the fragment's view is being destroyed.
     * Cleans up the binding to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Release the binding reference.
    }
}