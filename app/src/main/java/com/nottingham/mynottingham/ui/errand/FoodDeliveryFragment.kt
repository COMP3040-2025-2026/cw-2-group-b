package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentFoodDeliveryBinding
import com.nottingham.mynottingham.ui.errand.RestaurantMenuFragment

class FoodDeliveryFragment : Fragment() {

    private var _binding: FragmentFoodDeliveryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodDeliveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRestaurantClicks()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            // Navigate back to Campus Errand (ErrandHomeFragment)
            // Since this fragment was added via childFragmentManager with addToBackStack,
            // we need to pop the back stack to return to the previous fragment
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRestaurantClicks() {
        // Chinese Restaurant
        binding.cardRestaurantChinese.setOnClickListener {
            // Navigate to RestaurantMenuFragment
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    com.nottingham.mynottingham.R.anim.slide_in_right,
                    com.nottingham.mynottingham.R.anim.slide_out_left,
                    com.nottingham.mynottingham.R.anim.slide_in_left,
                    com.nottingham.mynottingham.R.anim.slide_out_right
                )
                .replace(com.nottingham.mynottingham.R.id.errand_fragment_container, RestaurantMenuFragment())
                .addToBackStack(null)
                .commit()
        }

        // Western Restaurant
        binding.cardRestaurantWestern.setOnClickListener {
            // TODO: Navigate to RestaurantMenuFragment with restaurant data
        }

        // Cafe
        binding.cardRestaurantCafe.setOnClickListener {
            // TODO: Navigate to RestaurantMenuFragment with restaurant data
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}