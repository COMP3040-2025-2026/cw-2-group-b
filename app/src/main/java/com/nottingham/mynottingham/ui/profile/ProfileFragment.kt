package com.nottingham.mynottingham.ui.profile

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseUserRepository
import com.nottingham.mynottingham.databinding.FragmentProfileBinding
import com.nottingham.mynottingham.util.AvatarUtils // Import the AvatarUtils utility class
import kotlinx.coroutines.flow.first
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
        setupSwitchColors()   // Call color setup here
        setupDeliveryMode()   // Delivery mode switch
    }

    private fun setupUI() {
        // Click avatar to show selection dialog
        binding.ivProfileAvatar.setOnClickListener {
            showAvatarSelectionDialog()
        }
        // Click logout and other logic unchanged...
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun loadUserInfo() {
        // 1. Listen to local avatar changes and auto-update UI
        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getAvatar().collect { avatarKey ->
                // Use utility class to convert string "tx1" to resource ID
                val resId = AvatarUtils.getDrawableId(avatarKey)
                binding.ivProfileAvatar.setImageResource(resId)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getFullName().collect { fullName ->
                fullName?.let {
                    binding.tvName.text = it
                    // The avatar is now an image, so no need to set text here
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getEmail().collect { email ->
                email?.let {
                    binding.tvEmail.text = it
                }
            }
        }

        // Determine user type and load appropriate info
        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getUserType().collect { userType ->
                if (userType == "TEACHER") {
                    loadTeacherInfo()
                } else {
                    loadStudentInfo()
                }
            }
        }
    }

    private fun loadStudentInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getStudentId().collect { studentId ->
                studentId?.let {
                    binding.tvStudentId.text = "Student ID: $it"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getFaculty().collect { faculty ->
                binding.labelFaculty.text = "Faculty"
                binding.valueFaculty.text = faculty ?: "—"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getYearOfStudy().collect { year ->
                binding.labelYear.text = "Year"
                binding.valueYear.text = year?.let { "Year $it" } ?: "—"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getMajor().collect { major ->
                binding.labelProgram.text = "Program"
                binding.valueProgram.text = major ?: "—"
            }
        }
    }

    private fun loadTeacherInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getEmployeeId().collect { employeeId ->
                employeeId?.let {
                    binding.tvStudentId.text = "Employee ID: $it"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getDepartment().collect { department ->
                binding.labelFaculty.text = "Department"
                binding.valueFaculty.text = department ?: "—"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getTitle().collect { title ->
                binding.labelYear.text = "Title"
                binding.valueYear.text = title ?: "—"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getOfficeRoom().collect { officeRoom ->
                binding.labelProgram.text = "Office"
                binding.valueProgram.text = officeRoom ?: "—"
            }
        }
    }

    // Show bottom sheet dialog
    private fun showAvatarSelectionDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_avatar_selection, null)
        dialog.setContentView(view)

        val gridLayout = view.findViewById<GridLayout>(R.id.grid_avatars)
        val btnCancel = view.findViewById<View>(R.id.btn_cancel_avatar)

        // Dynamically add tx1-tx13 to grid
        AvatarUtils.AVATAR_KEYS.forEach { avatarKey ->
            val imageView = ImageView(context).apply {
                // Set image
                setImageResource(AvatarUtils.getDrawableId(avatarKey))

                // Set layout parameters (width, height, margins)
                val size = 150 // pixel size
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(20, 20, 20, 20)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                background = ContextCompat.getDrawable(context, R.drawable.bg_avatar) // Optional: add background

                // Click image to trigger update
                setOnClickListener {
                    updateAvatar(avatarKey)
                    dialog.dismiss()
                }
            }
            gridLayout.addView(imageView)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // Core logic: update avatar to Firebase
    private fun updateAvatar(avatarKey: String) {
        // 1. Optimistic update: show to user immediately, no need to wait for network
        binding.ivProfileAvatar.setImageResource(AvatarUtils.getDrawableId(avatarKey))

        lifecycleScope.launch {
            try {
                val userIdString = tokenManager.getUserId().first()

                if (userIdString == null) return@launch

                // 2. Update avatar using Firebase
                val firebaseUserRepo = FirebaseUserRepository()
                val updates = mapOf("profileImageUrl" to avatarKey)
                val result = firebaseUserRepo.updateUserProfile(userIdString, updates)

                if (result.isSuccess) {
                    // 3. Save to local TokenManager after success
                    tokenManager.saveAvatar(avatarKey)
                    Toast.makeText(context, "Avatar updated!", Toast.LENGTH_SHORT).show()
                } else {
                    // Revert on failure
                    Toast.makeText(context, "Update failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    // Revert the avatar change
                    val oldAvatar = tokenManager.getAvatar().first()
                    binding.ivProfileAvatar.setImageResource(AvatarUtils.getDrawableId(oldAvatar))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                // Revert the avatar change
                val oldAvatar = tokenManager.getAvatar().first()
                binding.ivProfileAvatar.setImageResource(AvatarUtils.getDrawableId(oldAvatar))
            }
        }
    }

    // Switch slider color configuration
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

        // Set Delivery Mode Switch colors
        binding.switchDelivery.apply {
            thumbTintList = thumbStateList
            trackTintList = trackStateList
        }
    }

    private fun setupDeliveryMode() {
        // Load current delivery mode state
        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getDeliveryMode().collect { isEnabled ->
                binding.switchDelivery.isChecked = isEnabled
            }
        }

        // Save delivery mode when switch changes - sync to both local and Firebase
        binding.switchDelivery.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                try {
                    val userId = tokenManager.getUserId().first()
                    if (!userId.isNullOrEmpty()) {
                        // Sync to Firebase
                        val firebaseUserRepo = FirebaseUserRepository()
                        val result = firebaseUserRepo.updateUserProfile(userId, mapOf("deliveryMode" to isChecked))

                        if (result.isSuccess) {
                            // Save locally only after Firebase sync succeeds
                            tokenManager.setDeliveryMode(isChecked)
                        } else {
                            // Revert switch if Firebase sync failed
                            binding.switchDelivery.isChecked = !isChecked
                        }
                    }
                } catch (e: Exception) {
                    binding.switchDelivery.isChecked = !isChecked
                }
            }
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
            // Clear authentication data
            tokenManager.clearToken()

            // Clear all local database data to prevent privacy leaks
            val database = com.nottingham.mynottingham.data.local.database.AppDatabase.getInstance(requireContext())
            database.conversationDao().deleteAllConversations()
            database.conversationParticipantDao().deleteAllParticipants()
            database.messageDao().deleteAllMessages()

            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
