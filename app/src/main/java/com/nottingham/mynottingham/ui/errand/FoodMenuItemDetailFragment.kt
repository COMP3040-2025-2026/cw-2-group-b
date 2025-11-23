package com.nottingham.mynottingham.ui.errand

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nottingham.mynottingham.data.model.MenuItem
import com.nottingham.mynottingham.databinding.FragmentFoodMenuItemDetailBinding

class FoodMenuItemDetailFragment : Fragment() {

    private var _binding: FragmentFoodMenuItemDetailBinding? = null
    private val binding get() = _binding!!



    private var menuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                menuItem = it.getParcelable(ARG_MENU_ITEM, MenuItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                menuItem = it.getParcelable(ARG_MENU_ITEM)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodMenuItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        displayMenuItemDetails()


    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun displayMenuItemDetails() {
        menuItem?.let {
            binding.collapsingToolbar.title = it.name
            binding.tvFoodName.text = it.name
            binding.tvFoodDescription.text = it.description
            binding.tvFoodPrice.text = String.format("RM %.2f", it.price)
            binding.ivFoodImage.setImageResource(getImageResId(it.name))
        }
    }

    private fun getImageResId(name: String): Int {
        return when {
            name.contains("Beef") -> com.nottingham.mynottingham.R.drawable.bsn
            name.contains("Fried Noodles") -> com.nottingham.mynottingham.R.drawable.fn
            name.contains("Chicken Fried Rice") -> com.nottingham.mynottingham.R.drawable.fcr
            name.contains("Combo") -> com.nottingham.mynottingham.R.drawable.crs
            name.contains("Bubble") -> com.nottingham.mynottingham.R.drawable.bt
            name.contains("Lemon") -> com.nottingham.mynottingham.R.drawable.ilt
            else -> com.nottingham.mynottingham.R.drawable.ic_placeholder
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MENU_ITEM = "menu_item"

        @JvmStatic
        fun newInstance(menuItem: MenuItem) =
            FoodMenuItemDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MENU_ITEM, menuItem)
                }
            }
    }
}
