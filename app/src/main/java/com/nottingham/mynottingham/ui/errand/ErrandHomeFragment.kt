package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nottingham.mynottingham.databinding.FragmentErrandHomeBinding

import androidx.fragment.app.activityViewModels

class ErrandHomeFragment : Fragment() {

    private var _binding: FragmentErrandHomeBinding? = null
    private val binding get() = _binding!!
    private val errandViewModel: ErrandViewModel by activityViewModels {
        ErrandViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErrandHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // Reload tasks when fragment becomes visible
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
        binding.recyclerTasks.adapter = adapter
        
        errandViewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            android.util.Log.d("ErrandHomeFragment", "Received ${tasks.size} tasks from ViewModel")
            // Only show first 20 tasks as preview (Home page should be lightweight)
            val previewTasks = tasks.take(100)
            android.util.Log.d("ErrandHomeFragment", "Showing ${previewTasks.size} preview tasks")
            (binding.recyclerTasks.adapter as ErrandAdapter).updateTasks(previewTasks)
        }
    }

    private fun setupClickListeners() {
        // Back button - navigate back to Home using NavController
        binding.btnBack.setOnClickListener {
            // Check if parent ErrandFragment has any child fragments in back stack
            val parentFragment = parentFragment
            if (parentFragment is ErrandFragment) {
                val childBackStackCount = parentFragment.childFragmentManager.backStackEntryCount
                if (childBackStackCount > 0) {
                    // Pop child fragment stack
                    parentFragment.childFragmentManager.popBackStack()
                } else {
                    // No child fragments, navigate back using main NavController
                    findNavController().navigateUp()
                }
            } else {
                // Fallback: use main NavController
                findNavController().navigateUp()
            }
        }

        // See All button
        binding.tvSeeAll.setOnClickListener {
            // Navigate to parent fragment's container to show AllTasksFragment
            val parentFragment = parentFragment
            if (parentFragment is ErrandFragment) {
                parentFragment.childFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        com.nottingham.mynottingham.R.anim.slide_in_right,
                        com.nottingham.mynottingham.R.anim.slide_out_left,
                        com.nottingham.mynottingham.R.anim.slide_in_left,
                        com.nottingham.mynottingham.R.anim.slide_out_right
                    )
                    .replace(com.nottingham.mynottingham.R.id.errand_fragment_container, AllTasksFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        // Category click listeners
        binding.categoryShopping.setOnClickListener {
            MapsToPostTask("Shopping")
        }

        binding.categoryPickup.setOnClickListener {
            MapsToPostTask("Pickup")
        }

        binding.categoryFood.setOnClickListener {
            // Navigate to Food Delivery screen
            val parentFragment = parentFragment
            if (parentFragment is ErrandFragment) {
                parentFragment.childFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        com.nottingham.mynottingham.R.anim.slide_in_right,
                        com.nottingham.mynottingham.R.anim.slide_out_left,
                        com.nottingham.mynottingham.R.anim.slide_in_left,
                        com.nottingham.mynottingham.R.anim.slide_out_right
                    )
                    .replace(com.nottingham.mynottingham.R.id.errand_fragment_container, FoodDeliveryFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.categoryOthers.setOnClickListener {
            MapsToPostTask("Others")
        }
    }

    private fun MapsToPostTask(category: String) {
        val parentFragment = parentFragment
        if (parentFragment is ErrandFragment) {
            val postTaskFragment = PostTaskFragment().apply {
                arguments = Bundle().apply {
                    putString("task_category", category)
                }
            }

            parentFragment.childFragmentManager.beginTransaction()
                .setCustomAnimations(
                    com.nottingham.mynottingham.R.anim.slide_in_right,
                    com.nottingham.mynottingham.R.anim.slide_out_left,
                    com.nottingham.mynottingham.R.anim.slide_in_left,
                    com.nottingham.mynottingham.R.anim.slide_out_right
                )
                .replace(com.nottingham.mynottingham.R.id.errand_fragment_container, postTaskFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
