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
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.data.remote.dto.UserDto
import com.nottingham.mynottingham.data.remote.dto.UserUpdateRequest
import com.nottingham.mynottingham.databinding.FragmentProfileBinding
import com.nottingham.mynottingham.util.AvatarUtils // 导入刚才创建的工具类
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
        setupSwitchColors()   // ⭐ 新增：在这里调用颜色设置
    }

    private fun setupUI() {
        // 点击头像弹出选择框
        binding.ivProfileAvatar.setOnClickListener {
            showAvatarSelectionDialog()
        }
        // 点击注销等其他逻辑保持不变...
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun loadUserInfo() {
        // 1. 监听本地存储的头像变化，自动更新 UI
        viewLifecycleOwner.lifecycleScope.launch {
            tokenManager.getAvatar().collect { avatarKey ->
                // 使用工具类把字符串 "tx1" 变成资源 ID
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

    // 显示底部弹窗
    private fun showAvatarSelectionDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_avatar_selection, null)
        dialog.setContentView(view)

        val gridLayout = view.findViewById<GridLayout>(R.id.grid_avatars)
        val btnCancel = view.findViewById<View>(R.id.btn_cancel_avatar)

        // 动态把 tx1-tx13 添加到网格里
        AvatarUtils.AVATAR_KEYS.forEach { avatarKey ->
            val imageView = ImageView(context).apply {
                // 设置图片
                setImageResource(AvatarUtils.getDrawableId(avatarKey))
                
                // 设置布局参数 (宽高、边距)
                val size = 150 // 像素大小
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(20, 20, 20, 20)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                background = ContextCompat.getDrawable(context, R.drawable.bg_avatar) // 可选：给个背景
                
                // 点击图片触发更新
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

    // 核心逻辑：更新头像到服务器
    private fun updateAvatar(avatarKey: String) {
        // 1. 乐观更新：先显示给用户看，不用等网络
        binding.ivProfileAvatar.setImageResource(AvatarUtils.getDrawableId(avatarKey))

        lifecycleScope.launch {
            try {
                val token = "Bearer " + tokenManager.getToken().first()
                val userIdString = tokenManager.getUserId().first()

                if (userIdString == null) return@launch
                val userId = userIdString.toLong()

                // Retrieve all necessary user data from TokenManager
                val username = tokenManager.getUsername().first() ?: ""
                val email = tokenManager.getEmail().first() ?: ""
                val fullName = tokenManager.getFullName().first() ?: ""
                val userType = tokenManager.getUserType().first() ?: ""
                val phone = tokenManager.getPhone().first() // Assuming getPhone exists in TokenManager

                // Construct the update request object
                val request = UserUpdateRequest(
                    username = username,
                    email = email,
                    fullName = fullName,
                    role = userType, // Map userType to role
                    status = "ACTIVE", // Assume user is active
                    avatarUrl = avatarKey,
                    phone = phone
                )

                // 3. 发送请求
                val response = RetrofitInstance.apiService.updateUser(token, userId, request)

                if (response.isSuccessful) {
                    // 4. 成功后保存到本地 TokenManager
                    tokenManager.saveAvatar(avatarKey)
                    Toast.makeText(context, "Avatar updated!", Toast.LENGTH_SHORT).show()
                } else {
                    // 失败回滚
                    Toast.makeText(context, "Update failed: ${response.message()}", Toast.LENGTH_SHORT).show()
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
