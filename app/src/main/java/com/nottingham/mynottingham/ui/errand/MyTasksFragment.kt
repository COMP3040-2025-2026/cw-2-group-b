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
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.databinding.FragmentMyTasksBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyTasksFragment : Fragment() {

    private var _binding: FragmentMyTasksBinding? = null
    private val binding get() = _binding!!
    private var isPostedTabSelected = true
    private lateinit var tokenManager: TokenManager
    private lateinit var myTasksAdapter: MyTasksAdapter

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

                // 2. Fetch Data
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.apiService.getAvailableErrands()
                }

                if (response.isSuccessful && response.body() != null) {
                    val allTasks = response.body()!!

                    // Map ErrandResponse to MyTask
                    val allMyTasks = allTasks.map { errand ->
                        MyTask(
                            id = errand.id,
                            title = errand.title,
                            description = errand.description,
                            status = errand.status,
                            reward = errand.fee,
                            requesterId = errand.requesterId,
                            providerId = errand.providerId,
                            requesterName = errand.requesterName,
                            providerName = errand.providerName,
                            requesterAvatar = "", // Not available in ErrandResponse
                            deadline = "", // Not available in ErrandResponse
                            createdAt = errand.createdAt
                        )
                    }
                    
                    // 3. DEBUG: Log the first task to see what the data looks like
                    if (allTasks.isNotEmpty()) {
                        val firstTask = allTasks[0]
                        val reqId = firstTask.requesterId
                        val reqName = firstTask.requesterName
                        val provId = firstTask.providerId
                        val provName = firstTask.providerName
                        Log.d("MyTasksFragment", "First Task Debug: MyID=$myIdStr vs TaskReqID=$reqId($reqName) vs TaskProvID=$provId($provName)")
                    }


                    // 4. Filter Logic
                    val filteredTasks = if (isPostedTabSelected) {
                        allMyTasks.filter { task ->
                            val rId = task.requesterId
                            val rName = task.requesterName
                            // Match ID OR Name
                            rId == myIdStr || rName.equals(myName, ignoreCase = true)
                        }
                    } else {
                        allMyTasks.filter { task ->
                            val pId = task.providerId
                            val pName = task.providerName
                            // Match Provider ID
                            pId == myIdStr || (pName != null && pName.equals(myName, ignoreCase = true))
                        }
                    }

                    // 5. Update UI
                    if (filteredTasks.isEmpty()) {
                        binding.recyclerMyTasks.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                        // Show WHY it is empty
                        val emptyMsg = if (isPostedTabSelected) {
                            "No tasks posted yet.\n(My ID: $myIdStr, Tasks Fetched: ${allTasks.size})"
                        } else {
                            "No tasks accepted yet.\n(My ID: $myIdStr, Tasks Fetched: ${allTasks.size})"
                        }
                        binding.tvEmptyState.text = emptyMsg

                    } else {
                        binding.recyclerMyTasks.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                        myTasksAdapter.submitList(filteredTasks)
                    }
                } else {
                    Toast.makeText(context, "Failed to load tasks: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // DO NOTHING - The user likely navigated away, don't show toast
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
