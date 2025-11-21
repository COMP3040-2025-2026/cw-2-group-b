package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.databinding.FragmentTaskDetailsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding get() = _binding!!

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
        
        val timeText = when {
            minutesAgo < 1 -> "Posted just now"
            minutesAgo < 60 -> "Posted $minutesAgo mins ago"
            minutesAgo < 1440 -> "Posted ${minutesAgo / 60} hours ago"
            else -> "Posted ${minutesAgo / 1440} days ago"
        }
        
        binding.tvTaskPosted.text = timeText
        binding.tvTaskPosted.visibility = View.VISIBLE

        val tokenManager = TokenManager(requireContext())
        lifecycleScope.launch {
            val currentUserId = tokenManager.getUserId().first()
            if (currentUserId == requesterId) {
                binding.btnAcceptTask.visibility = View.GONE
            } else {
                binding.btnAcceptTask.visibility = View.VISIBLE
            }
        }

        binding.btnAcceptTask.setOnClickListener {
            Toast.makeText(requireContext(), "Task Accepted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days days ago"
            hours > 0 -> "$hours hours ago"
            minutes > 0 -> "$minutes mins ago"
            else -> "Just now"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
