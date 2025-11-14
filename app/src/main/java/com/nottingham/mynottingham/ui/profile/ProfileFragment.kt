package com.nottingham.mynottingham.ui.profile

import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat

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
        setupSwitchColors()   // ⭐ 新增：在这里调用颜色设置
    }

    private fun setupUI() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            tokenManager.getFullName().collect { fullName ->
                fullName?.let {
                    binding.tvName.text = it
                    binding.tvAvatar.text = it.take(2).uppercase()
                }
            }
        }

        lifecycleScope.launch {
            tokenManager.getStudentId().collect { studentId ->
                studentId?.let {
                    binding.tvStudentId.text = it
                }
            }
        }
    }

    // ⭐⭐⭐ Switch 滑块颜色设置
    private fun setupSwitchColors() {

        val green = ContextCompat.getColor(requireContext(), R.color.primary)      // thumb checked
        val greenLight = ContextCompat.getColor(requireContext(), R.color.primary_light) // track checked

        val gray = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)  // thumb unchecked
        val grayLight = ContextCompat.getColor(requireContext(), R.color.divider)         // track unchecked

        val thumbStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(green, gray)
        )

        val trackStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(greenLight, grayLight)
        )

        // 设置两个 Switch 的颜色
        binding.switchErrand.apply {
            thumbTintList = thumbStateList
            trackTintList = trackStateList
        }

        binding.switchDelivery.apply {
            thumbTintList = thumbStateList
            trackTintList = trackStateList
        }
    }

    private fun showLogoutConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            tokenManager.clearToken()
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
