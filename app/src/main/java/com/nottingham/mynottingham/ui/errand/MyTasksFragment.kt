package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.data.repository.FirebaseErrandRepository
import com.nottingham.mynottingham.databinding.FragmentMyTasksBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * My Tasks Fragment
 * ✅ Migrated to Firebase - no longer uses backend API
 */
class MyTasksFragment : Fragment() {

    private var _binding: FragmentMyTasksBinding? = null
    private val binding get() = _binding!!
    private var isPostedTabSelected = true
    private lateinit var tokenManager: TokenManager
    private lateinit var myTasksAdapter: MyTasksAdapter

    private val repository = FirebaseErrandRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTasksBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 1. Force Layout Manager (Critical if missing in XML)
        myTasksAdapter = MyTasksAdapter { task ->
            val bundle = Bundle().apply {
                putString("taskId", task.id)
                putString("title", task.title)
                putString("description", task.description)
                putString("price", task.reward.toString())
                putString("location", "") // MyTask does not have location, pass empty string
                putString("requesterId", task.requesterId)
                putString("requesterName", task.requesterName)
                putString("providerName", task.providerName)
                putString("requesterAvatar", task.requesterAvatar)
                putString("timeLimit", task.deadline) // Use "timeLimit" as the key
                putLong("timestamp", task.createdAt)
            }
            val taskDetailFragment = TaskDetailFragment().apply {
                arguments = bundle
            }
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.errand_fragment_container, taskDetailFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerMyTasks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = myTasksAdapter
        }

        // 2. Setup Tabs
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        loadMyTasks()
    }

    private fun setupTabs() {
        binding.btnPosted.setOnClickListener {
            isPostedTabSelected = true
            updateTabStyles()
            loadMyTasks()
        }

        binding.btnAccepted.setOnClickListener {
            isPostedTabSelected = false
            updateTabStyles()
            loadMyTasks()
        }
        updateTabStyles() // Initial style
    }

    private fun updateTabStyles() {
        if (isPostedTabSelected) {
            binding.btnPosted.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.btnPosted.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnAccepted.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            binding.btnAccepted.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        } else {
            binding.btnAccepted.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.btnAccepted.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnPosted.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            binding.btnPosted.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    private fun loadMyTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Get My IDs
                val myIdStr = tokenManager.getUserId().first() ?: ""
                val myName = tokenManager.getUsername().first() ?: ""

                if (myIdStr.isEmpty()) {
                    if (_binding != null) {
                        binding.recyclerMyTasks.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyState.text = "Please login first"
                    }
                    return@launch
                }

                // 2. Collect from Firebase Flow based on tab
                val flow = if (isPostedTabSelected) {
                    repository.getUserRequestedErrands(myIdStr)
                } else {
                    repository.getUserProvidedErrands(myIdStr)
                }

                flow.collect { errands ->
                    if (_binding == null) return@collect

                    // Map Firebase data to MyTask
                    val myTasks = errands.map { errand ->
                        MyTask(
                            id = errand["id"] as? String ?: "",
                            title = errand["title"] as? String ?: "",
                            description = errand["description"] as? String ?: "",
                            status = errand["status"] as? String ?: "PENDING",
                            reward = (errand["reward"] as? Number)?.toDouble() ?: 0.0,
                            requesterId = errand["requesterId"] as? String ?: "",
                            providerId = errand["providerId"] as? String,
                            requesterName = errand["requesterName"] as? String ?: "Unknown",
                            providerName = errand["providerName"] as? String,
                            requesterAvatar = errand["requesterAvatar"] as? String ?: "",
                            deadline = errand["deadline"] as? String ?: "",
                            createdAt = (errand["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    }

                    // 3. DEBUG log
                    Log.d("MyTasksFragment", "Loaded ${myTasks.size} tasks for tab: ${if (isPostedTabSelected) "Posted" else "Accepted"}")

                    // 4. Update UI
                    if (myTasks.isEmpty()) {
                        binding.recyclerMyTasks.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyState.text = if (isPostedTabSelected) {
                            "No tasks posted yet"
                        } else {
                            "No tasks accepted yet"
                        }
                    } else {
                        binding.recyclerMyTasks.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                        myTasksAdapter.submitList(myTasks)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // DO NOTHING - The user likely navigated away, don't show toast
            } catch (e: Exception) {
                e.printStackTrace()
                if (_binding != null) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
