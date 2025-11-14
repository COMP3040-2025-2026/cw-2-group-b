package com.nottingham.mynottingham.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadUserInfo()
    }

    private fun setupUI() {
        // Logout button click listener
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun loadUserInfo() {
        // Load user info from TokenManager and display
        lifecycleScope.launch {
            // Load and display full name
            tokenManager.getFullName().collect { fullName ->
                fullName?.let {
                    binding.tvName.text = it
                    // Set avatar to first 2 letters of full name
                    binding.tvAvatar.text = it.take(2).uppercase()
                }
            }
        }

        lifecycleScope.launch {
            // Load and display student ID
            tokenManager.getStudentId().collect { studentId ->
                studentId?.let {
                    binding.tvStudentId.text = it
                }
            }
        }
    }

    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            // Clear all saved data
            tokenManager.clearToken()

            // Navigate to login screen
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
