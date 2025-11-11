package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nottingham.mynottingham.databinding.FragmentRestaurantMenuBinding

class RestaurantMenuFragment : Fragment() {

    private var _binding: FragmentRestaurantMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestaurantMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupButtons() {
        // View Cart button
        binding.btnViewCart.setOnClickListener {
            // TODO: Navigate to CartFragment
        }

        // Add to cart buttons for menu items
        // These would typically be handled by a RecyclerView adapter
        // For now, they're just placeholders in the layout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
