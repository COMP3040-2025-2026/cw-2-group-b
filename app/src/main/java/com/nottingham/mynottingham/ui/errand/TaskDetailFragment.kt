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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding get() = _binding!!

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
            // [Fix] 获取真正的 JWT Token
            val storedToken = tokenManager.getToken().first()
            val currentUserId = tokenManager.getUserId().first()

            if (storedToken.isNullOrEmpty() || currentUserId.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                binding.btnAcceptTask.isEnabled = false
                return@launch
            }

            val token = "Bearer $storedToken"

            // 立即检查任务最新状态
            checkTaskStatus(token, currentTaskId, currentUserId)

            // 判断身份（发布者 vs 接单者）
            if (currentUserId == requesterId) {
                // 发布者视图
                binding.btnAcceptTask.visibility = View.GONE
                binding.layoutOwnerActions.visibility = View.VISIBLE

                // 发布者点击 Delete 是永久删除
                binding.btnDelete.setOnClickListener { performDelete(token, currentTaskId) }
                binding.btnComplete.setOnClickListener { markAsComplete(token, currentTaskId) }
                binding.btnEdit.setOnClickListener {
                    Toast.makeText(requireContext(), "Edit feature coming soon", Toast.LENGTH_SHORT).show()
                }
            } else {
                // 接单者视图
                binding.btnAcceptTask.visibility = View.VISIBLE
                binding.layoutOwnerActions.visibility = View.GONE

                binding.btnAcceptTask.setOnClickListener {
                    if (isAcceptedByMe) {
                        // 已接单 -> 点击执行“放弃任务” (Drop)
                        performDrop(token, currentTaskId)
                    } else {
                        // 未接单 -> 点击执行“接受”
                        acceptErrand(token, currentTaskId)
                    }
                }
            }
        }
    }

    // 从服务器检查任务状态
    private suspend fun checkTaskStatus(token: String, taskId: String, currentUserId: String) {
        try {
            val response = RetrofitInstance.apiService.getErrandById(token, taskId)
            if (response.isSuccessful && response.body() != null) {
                val task = response.body()!!

                val providerId = task.providerId
                val status = task.status

                if (status == "IN_PROGRESS" && providerId == currentUserId) {
                    // 是我接的任务
                    isAcceptedByMe = true
                    updateButtonToDeleteState()
                } else if (status != "PENDING" && providerId != currentUserId) {
                    // 任务被别人接了
                    binding.btnAcceptTask.isEnabled = false
                    binding.btnAcceptTask.text = "Task Taken"
                }
            }
        } catch (e: Exception) {
            Log.e("TaskDetail", "Failed to check status", e)
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
                    updateButtonToDeleteState()
                } else {
                    val errorBody = response.errorBody()?.string()
                    // 409 说明已被占用，再查一次状态
                    if (response.code() == 409) {
                        val tokenManager = TokenManager(requireContext())
                        checkTaskStatus(token, taskId, tokenManager.getUserId().first() ?: "")
                    } else {
                        Toast.makeText(requireContext(), "Failed: ${response.code()} $errorBody", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnAcceptTask.isEnabled = true
            }
        }
    }

    private fun updateButtonToDeleteState() {
        binding.btnAcceptTask.text = "Delete Task" // 实际上是 Cancel/Drop
        binding.btnAcceptTask.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.error)
        )
        binding.btnAcceptTask.requestLayout()
    }

    // [Owner] 物理删除
    private fun performDelete(token: String, taskId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.apiService.deleteErrand(token, taskId)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Task Deleted Permanently", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Delete Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // [Runner] 放弃任务（逻辑删除，回收到池子）
    private fun performDrop(token: String, taskId: String) {
        binding.btnAcceptTask.isEnabled = false
        lifecycleScope.launch {
            try {
                // 调用 drop 接口，传入空 map
                val response = RetrofitInstance.apiService.dropErrand(token, taskId, emptyMap())
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Task Dropped/Canceled", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack() // 返回上一页
                } else {
                    Toast.makeText(requireContext(), "Drop Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnAcceptTask.isEnabled = true
            }
        }
    }

    private fun markAsComplete(token: String, taskId: String) {
        lifecycleScope.launch {
            try {
                val request = UpdateStatusRequest("COMPLETED")
                val response = RetrofitInstance.apiService.updateErrandStatus(token, taskId, request)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Completed!", Toast.LENGTH_SHORT).show()
                    binding.tvTaskPosted.text = "Status: COMPLETED"
                    binding.layoutOwnerActions.visibility = View.GONE
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}