package com.nottingham.mynottingham.ui.errand

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseErrandRepository
import com.nottingham.mynottingham.databinding.FragmentTaskDetailsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Task Detail Fragment
 * ✅ Migrated to Firebase - no longer uses backend API
 */
class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding get() = _binding!!

    private val repository = FirebaseErrandRepository()

    // 标记：任务是否被我接了
    private var isAcceptedByMe = false
    private var currentTaskId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 获取基础参数
        currentTaskId = arguments?.getString("taskId") ?: ""
        val title = arguments?.getString("title")
        val description = arguments?.getString("description")
        val price = arguments?.getString("price")
        val location = arguments?.getString("location")
        val requesterName = arguments?.getString("requesterName")
        val requesterId = arguments?.getString("requesterId")
        val requesterAvatar = arguments?.getString("requesterAvatar") ?: "default"
        val timeLimit = arguments?.getString("timeLimit") ?: "No Deadline"
        val timestamp = arguments?.getLong("timestamp") ?: 0

        // 绑定 UI
        binding.tvTaskTitle.text = title
        binding.tvTaskDescription.text = description
        binding.tvTaskPrice.text = "RM $price"
        binding.tvTaskLocation.text = location
        binding.tvRequesterName.text = requesterName
        binding.ivRequesterAvatar.setImageResource(com.nottingham.mynottingham.util.AvatarUtils.getDrawableId(requesterAvatar))

        binding.tvTaskDeadline.text = "Deadline: $timeLimit"
        binding.tvTaskDeadline.visibility = if (timeLimit == "No Deadline") View.GONE else View.VISIBLE

        val currentTime = System.currentTimeMillis()
        val diffMillis = currentTime - timestamp
        val minutesAgo = diffMillis / (1000 * 60)
        binding.tvTaskPosted.text = when {
            minutesAgo < 1L -> "Posted just now"
            minutesAgo < 60L -> "Posted $minutesAgo mins ago"
            minutesAgo < 1440L -> "Posted ${minutesAgo / 60} hours ago"
            else -> "Posted ${minutesAgo / 1440} days ago"
        }
        binding.tvTaskPosted.visibility = View.VISIBLE

        // 启动逻辑
        val tokenManager = TokenManager(requireContext())
        lifecycleScope.launch {
            val currentUserId = tokenManager.getUserId().first()
            val currentUserName = tokenManager.getFullName().first() ?: "User"

            if (currentUserId.isNullOrEmpty()) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                    binding.btnAcceptTask.isEnabled = false
                }
                return@launch
            }

            // 立即检查任务最新状态
            checkTaskStatus(currentTaskId, currentUserId)

            // 判断身份（发布者 vs 接单者）
            if (_binding == null) return@launch

            if (currentUserId == requesterId) {
                // 发布者视图
                binding.btnAcceptTask.visibility = View.GONE
                binding.layoutOwnerActions.visibility = View.VISIBLE

                // 发布者点击 Delete 是永久删除
                binding.btnDelete.setOnClickListener { performDelete(currentTaskId) }
                binding.btnComplete.setOnClickListener { markAsComplete(currentTaskId) }
                binding.btnEdit.setOnClickListener {
                    Toast.makeText(requireContext(), "Edit feature coming soon", Toast.LENGTH_SHORT).show()
                }
            } else {
                // 接单者视图
                binding.btnAcceptTask.visibility = View.VISIBLE
                binding.layoutOwnerActions.visibility = View.GONE

                binding.btnAcceptTask.setOnClickListener {
                    if (isAcceptedByMe) {
                        // 已接单 -> 点击执行"放弃任务" (Drop)
                        performDrop(currentTaskId)
                    } else {
                        // 未接单 -> 点击执行"接受"
                        acceptErrand(currentTaskId, currentUserId, currentUserName)
                    }
                }
            }
        }
    }

    // 从 Firebase 检查任务状态
    private suspend fun checkTaskStatus(taskId: String, currentUserId: String) {
        try {
            val result = repository.getErrandById(taskId)
            result.onSuccess { task ->
                if (task == null || _binding == null) return@onSuccess

                val providerId = task["providerId"] as? String
                val status = task["status"] as? String ?: ""

                if (status == "IN_PROGRESS" && providerId == currentUserId) {
                    // 是我接的任务
                    isAcceptedByMe = true
                    updateButtonToDeleteState()
                } else if (status != "PENDING" && providerId != currentUserId) {
                    // 任务被别人接了
                    if (_binding != null) {
                        binding.btnAcceptTask.isEnabled = false
                        binding.btnAcceptTask.text = "Task Taken"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TaskDetail", "Failed to check status", e)
        }
    }

    private fun acceptErrand(taskId: String, userId: String, userName: String) {
        if (_binding == null) return
        binding.btnAcceptTask.isEnabled = false

        lifecycleScope.launch {
            val result = repository.acceptErrand(taskId, userId, userName)

            if (_binding == null) return@launch

            result.onSuccess {
                Toast.makeText(requireContext(), "Task Accepted!", Toast.LENGTH_SHORT).show()
                isAcceptedByMe = true
                updateButtonToDeleteState()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            binding.btnAcceptTask.isEnabled = true
        }
    }

    private fun updateButtonToDeleteState() {
        if (_binding == null) return
        binding.btnAcceptTask.text = "Drop Task"
        binding.btnAcceptTask.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.error)
        )
        binding.btnAcceptTask.requestLayout()
    }

    // [Owner] 物理删除
    private fun performDelete(taskId: String) {
        lifecycleScope.launch {
            val result = repository.deleteErrand(taskId)

            if (_binding == null) return@launch

            result.onSuccess {
                Toast.makeText(requireContext(), "Task Deleted Permanently", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Delete Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // [Runner] 放弃任务（逻辑删除，回收到池子）
    private fun performDrop(taskId: String) {
        if (_binding == null) return
        binding.btnAcceptTask.isEnabled = false

        lifecycleScope.launch {
            val result = repository.dropErrand(taskId)

            if (_binding == null) return@launch

            result.onSuccess {
                Toast.makeText(requireContext(), "Task Dropped", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }.onFailure { e ->
                Toast.makeText(requireContext(), "Drop Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            binding.btnAcceptTask.isEnabled = true
        }
    }

    private fun markAsComplete(taskId: String) {
        lifecycleScope.launch {
            val result = repository.completeErrand(taskId)

            if (_binding == null) return@launch

            result.onSuccess {
                Toast.makeText(requireContext(), "Completed!", Toast.LENGTH_SHORT).show()
                binding.tvTaskPosted.text = "Status: COMPLETED"
                binding.layoutOwnerActions.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
