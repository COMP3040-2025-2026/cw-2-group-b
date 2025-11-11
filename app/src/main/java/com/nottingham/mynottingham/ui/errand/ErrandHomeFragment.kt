package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.databinding.FragmentErrandHomeBinding

class ErrandHomeFragment : Fragment() {

    private var _binding: FragmentErrandHomeBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // FAB button to create new task
        binding.fabCreateTask.setOnClickListener {
            // Navigate to parent fragment's container to show PostTaskFragment
            val parentFragment = parentFragment
            if (parentFragment is ErrandFragment) {
                parentFragment.childFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        com.nottingham.mynottingham.R.anim.slide_in_right,
                        com.nottingham.mynottingham.R.anim.slide_out_left,
                        com.nottingham.mynottingham.R.anim.slide_in_left,
                        com.nottingham.mynottingham.R.anim.slide_out_right
                    )
                    .replace(com.nottingham.mynottingham.R.id.errand_fragment_container, PostTaskFragment())
                    .addToBackStack(null)
                    .commit()
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
            // TODO: Filter by Shopping category
        }

        binding.categoryPickup.setOnClickListener {
            // TODO: Filter by Pickup category
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
            // TODO: Filter by Others category
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
