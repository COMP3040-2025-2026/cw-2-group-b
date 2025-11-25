package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.databinding.FragmentAllTasksBinding
import androidx.fragment.app.activityViewModels

class AllTasksFragment : Fragment() {

    private var _binding: FragmentAllTasksBinding? = null
    private val binding get() = _binding!!
    private val errandViewModel: ErrandViewModel by activityViewModels {
        ErrandViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
    }


    override fun onResume() {
        super.onResume()
        errandViewModel.loadTasks()
    }

    private fun setupRecyclerView() {
        val adapter = ErrandAdapter(mutableListOf()) { task ->
            val bundle = Bundle().apply {
                putString("taskId", task.taskId)
                putString("title", task.title)
                putString("description", task.description)
                putString("price", task.price)
                putString("location", task.location)
                putString("requesterId", task.requesterId)
                putString("requesterName", task.requesterName)
                putString("requesterAvatar", task.requesterAvatar)
                putString("timeLimit", task.deadline) // Use "timeLimit" as the key
                putLong("timestamp", task.timestamp)
                putString("taskType", task.taskType)
            }
            val taskDetailFragment = TaskDetailFragment().apply {
                arguments = bundle
            }
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    com.nottingham.mynottingham.R.anim.slide_in_right,
                    com.nottingham.mynottingham.R.anim.slide_out_left,
                    com.nottingham.mynottingham.R.anim.slide_in_left,
                    com.nottingham.mynottingham.R.anim.slide_out_right
                )
                .replace(com.nottingham.mynottingham.R.id.errand_fragment_container, taskDetailFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerViewTasks.adapter = adapter

        errandViewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            android.util.Log.d("AllTasksFragment", "Received ${tasks.size} tasks from ViewModel")
            (binding.recyclerViewTasks.adapter as ErrandAdapter).updateTasks(tasks)
        }
    }


    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
