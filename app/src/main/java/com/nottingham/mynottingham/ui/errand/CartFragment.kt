package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentCartBinding

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels to get the shared ViewModel
    private val viewModel: RestaurantMenuViewModel by activityViewModels()

    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbarCart)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems = emptyList()
        )
        binding.rvCartItems.adapter = cartAdapter
    }

    private fun setupObservers() {
        viewModel.cartItems.observe(viewLifecycleOwner) { items ->
            cartAdapter.updateItems(items)
            if (items.isEmpty()) {
                // If cart is empty, go back to the menu
                parentFragmentManager.popBackStack()
            }
        }

        viewModel.totalPrice.observe(viewLifecycleOwner) { total ->
            if (total > 0) {
                // Assuming a fixed delivery fee of 2.00, as in the ViewModel
                val deliveryFee = 2.00
                val subtotal = total - deliveryFee
                binding.tvSubtotal.text = String.format("RM %.2f", subtotal)
                binding.tvDeliveryFee.text = String.format("RM %.2f", deliveryFee)
                binding.tvTotal.text = String.format("RM %.2f", total)
            } else {
                binding.tvSubtotal.text = "RM 0.00"
                binding.tvDeliveryFee.text = "RM 0.00"
                binding.tvTotal.text = "RM 0.00"
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnPlaceOrder.setOnClickListener {
            viewModel.placeOrder("user_123", "Dorm Room 305")
            Toast.makeText(requireContext(), "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
            // Pop back to the restaurant list or main screen
            parentFragmentManager.popBackStack() // Pops CartFragment
            parentFragmentManager.popBackStack() // Pops RestaurantMenuFragment
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_back_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_back -> {
                parentFragmentManager.popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
