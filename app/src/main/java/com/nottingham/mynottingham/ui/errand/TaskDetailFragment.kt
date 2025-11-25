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
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.data.remote.dto.UpdateStatusRequest
import com.nottingham.mynottingham.databinding.FragmentTaskDetailsBinding
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding get() = _binding!!

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

        val tokenManager = TokenManager(requireContext())
        lifecycleScope.launch {
            val storedToken = tokenManager.getToken().first()
            val currentUserId = tokenManager.getUserId().first()

            if (storedToken.isNullOrEmpty() || currentUserId.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                binding.btnAcceptTask.isEnabled = false
                return@launch
            }

            val token = "Bearer $storedToken"

            // 检查状态
            checkTaskStatus(token, currentTaskId, currentUserId)

            if (currentUserId == requesterId) {
                // [Owner] 发布者
                showOwnerView()
                binding.btnDelete.setOnClickListener { performDelete(token, currentTaskId) }
                binding.btnEdit.setOnClickListener {
                    Toast.makeText(requireContext(), "Edit coming soon", Toast.LENGTH_SHORT).show()
                }
            } else {
                // [Other] 接单者或其他人
                if (isAcceptedByMe) {
                    // [Runner] 接单者
                    showRunnerView()
                    binding.btnComplete.setOnClickListener { markAsComplete(token, currentTaskId) }
                    binding.btnRunnerDrop.setOnClickListener { performDrop(token, currentTaskId) }
                } else {
                    // [Visitor] 访客 -> 这里点击监听器只在未完成时有效
                    // 如果 checkTaskStatus 发现已完成，会禁用按钮，这里的监听器实际上点不到
                    binding.btnAcceptTask.setOnClickListener { acceptErrand(token, currentTaskId) }
                }
            }
        }
    }

    private fun showOwnerView() {
        binding.btnAcceptTask.visibility = View.GONE
        binding.layoutRunnerActions.visibility = View.GONE
        binding.layoutOwnerActions.visibility = View.VISIBLE
    }

    private fun showRunnerView() {
        binding.btnAcceptTask.visibility = View.GONE
        binding.layoutOwnerActions.visibility = View.GONE
        binding.layoutRunnerActions.visibility = View.VISIBLE
    }

    private fun showAcceptView() {
        binding.layoutOwnerActions.visibility = View.GONE
        binding.layoutRunnerActions.visibility = View.GONE
        binding.btnAcceptTask.visibility = View.VISIBLE
        binding.btnAcceptTask.isEnabled = true
        binding.btnAcceptTask.text = "Accept Task"
        binding.btnAcceptTask.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.primary)
        )
    }

    // [关键修改] 增加了对 COMPLETED 状态的处理
    private fun showCompletedView() {
        binding.layoutOwnerActions.visibility = View.GONE
        binding.layoutRunnerActions.visibility = View.GONE
        binding.btnAcceptTask.visibility = View.VISIBLE
        
        binding.btnAcceptTask.isEnabled = false // 禁用点击
        binding.btnAcceptTask.text = "Task Finished" // 修改文字
        binding.btnAcceptTask.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray) // 变灰
        )
        binding.tvTaskPosted.text = "Status: COMPLETED"
    }

    private suspend fun checkTaskStatus(token: String, taskId: String, currentUserId: String) {
        try {
            val response = RetrofitInstance.apiService.getErrandById(token, taskId)
            if (response.isSuccessful && response.body() != null) {
                val task = response.body()!!
                val providerId = task.providerId
                val status = task.status

                // [Fix] 优先判断是否已完成
                if (status == "COMPLETED") {
                    showCompletedView()
                    return // 直接返回，不再执行后续逻辑
                }

                if (status == "IN_PROGRESS" && providerId == currentUserId) {
                    // 是我接的任务且未完成
                    isAcceptedByMe = true
                    if (task.requesterId != currentUserId) {
                        showRunnerView()
                        binding.btnComplete.setOnClickListener { markAsComplete(token, taskId) }
                        binding.btnRunnerDrop.setOnClickListener { performDrop(token, taskId) }
                    }
                } else if (status != "PENDING" && providerId != currentUserId) {
                    // 被别人接了
                    binding.btnAcceptTask.isEnabled = false
                    binding.btnAcceptTask.text = "Task Taken"
                }
            }
        } catch (e: Exception) {
            Log.e("TaskDetail", "Status check failed", e)
        }
    }

    private fun acceptErrand(token: String, taskId: String) {
        binding.btnAcceptTask.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.apiService.acceptErrand(token, taskId)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Task Accepted!", Toast.LENGTH_SHORT).show()
                    isAcceptedByMe = true
                    showRunnerView()
                    setFragmentResult("taskUpdated", bundleOf("refresh" to true)) // Notify MyTasksFragment
                    binding.btnComplete.setOnClickListener { markAsComplete(token, taskId) }
                    binding.btnRunnerDrop.setOnClickListener { performDrop(token, taskId) }
                } else {
                    if (response.code() == 409) {
                        val tokenManager = TokenManager(requireContext())
                        checkTaskStatus(token, taskId, tokenManager.getUserId().first() ?: "")
                    } else {
                        Toast.makeText(requireContext(), "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnAcceptTask.isEnabled = true
            }
        }
    }

    private fun performDrop(token: String, taskId: String) {
        binding.btnRunnerDrop.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.apiService.dropErrand(token, taskId, emptyMap())
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Task Dropped", Toast.LENGTH_SHORT).show()
                    setFragmentResult("taskUpdated", bundleOf("refresh" to true)) // Notify MyTasksFragment
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Drop Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnRunnerDrop.isEnabled = true
            }
        }
    }

    private fun performDelete(token: String, taskId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.apiService.deleteErrand(token, taskId)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    setFragmentResult("taskUpdated", bundleOf("refresh" to true)) // Notify MyTasksFragment
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Delete Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markAsComplete(token: String, taskId: String) {
        lifecycleScope.launch {
            try {
                val request = UpdateStatusRequest("COMPLETED")
                val response = RetrofitInstance.apiService.updateErrandStatus(token, taskId, request)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Task Completed!", Toast.LENGTH_SHORT).show()
                    setFragmentResult("taskUpdated", bundleOf("refresh" to true)) // Notify MyTasksFragment
                    // [Fix] 成功后立即更新界面为完成状态
                    showCompletedView()
                } else {
                    Toast.makeText(requireContext(), "Failed to complete", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}