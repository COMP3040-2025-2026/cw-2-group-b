package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.databinding.FragmentErrandHomeBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.fragment.app.activityViewModels

class ErrandHomeFragment : Fragment() {

    private var _binding: FragmentErrandHomeBinding? = null
    private val binding get() = _binding!!
    private val errandViewModel: ErrandViewModel by activityViewModels {
        ErrandViewModelFactory(requireActivity().application)
    }
    private lateinit var tokenManager: TokenManager
    private var isDeliveryMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentErrandHomeBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDeliveryModeUI()
        setupClickListeners()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // Reload delivery mode and tasks when fragment becomes visible
        setupDeliveryModeUI()
        errandViewModel.loadTasks()
    }

    /**
     * Setup UI based on delivery mode
     * - Rider mode: Show balance card, "Available Tasks" title
     * - Normal user: Hide balance card, "My Recent Orders" title
     */
    private fun setupDeliveryModeUI() {
        viewLifecycleOwner.lifecycleScope.launch {
            isDeliveryMode = tokenManager.getDeliveryMode().first()

            if (isDeliveryMode) {
                // Rider mode
                binding.cardBalance.visibility = View.VISIBLE
                binding.tvAvailableTasks.text = "Available Tasks"
                binding.tvSubtitle.text = "Accept tasks, earn rewards"
            } else {
                // Normal user mode
                binding.cardBalance.visibility = View.GONE
                binding.tvAvailableTasks.text = "My Recent Orders"
                binding.tvSubtitle.text = "Post tasks, get things done"
            }
        }
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
            // Only show first 4 tasks as preview on Home page
            val previewTasks = tasks.take(4)
            android.util.Log.d("ErrandHomeFragment", "Showing ${previewTasks.size} preview tasks (max 4)")
            (binding.recyclerTasks.adapter as ErrandAdapter).updateTasks(previewTasks)
        }
    }

    private fun setupClickListeners() {
        // Withdraw button - clear balance (for riders only)
        binding.btnWithdraw.setOnClickListener {
            showWithdrawConfirmDialog()
        }

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

    /**
     * Show withdraw confirmation dialog
     * Withdraws all balance to user's third-party payment app
     */
    private fun showWithdrawConfirmDialog() {
        val currentBalance = binding.tvBalanceAmount.text.toString()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Withdraw Balance")
            .setMessage("Withdraw $currentBalance to your payment app?\n\nThis will clear your balance.")
            .setPositiveButton("Withdraw") { _, _ ->
                performWithdraw()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Perform withdraw - clear balance
     * In production, this would integrate with payment gateway
     */
    private fun performWithdraw() {
        // TODO: In production, integrate with payment gateway API
        // For now, just clear the balance display
        binding.tvBalanceAmount.text = "RM 0.00"
        Toast.makeText(context, "Withdrawal successful! Balance transferred to your payment app.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
