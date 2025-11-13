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

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.etUsername.addTextChangedListener { text ->
            binding.tilUsername.error = null
            binding.tvError.visibility = View.GONE

            binding.tilUsername.isHintEnabled = text.isNullOrEmpty()
        }

        binding.etPassword.addTextChangedListener { text ->
            binding.tilPassword.error = null
            binding.tvError.visibility = View.GONE

            binding.tilPassword.isHintEnabled = text.isNullOrEmpty()
        }

        // 登录按钮点击
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(username, password)) {
                viewModel.login(username, password)
            }
        }
    }

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

    private fun observeViewModel() {
        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnLogin.isEnabled = !isLoading
                binding.etUsername.isEnabled = !isLoading
                binding.etPassword.isEnabled = !isLoading
            }
        }

        // Observe error state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    binding.tvError.text = it
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }

        // Observe login success
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginSuccess.collect { success ->
                if (success) {
                    // Navigate to home
                    findNavController().navigate(R.id.action_login_to_home)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}