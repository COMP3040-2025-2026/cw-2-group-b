package com.nottingham.mynottingham.ui.errand

import android.os.Bundle
import android.view.*
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentCheckoutBinding
import com.nottingham.mynottingham.ui.errand.ErrandViewModel
import com.nottingham.mynottingham.ui.errand.ErrandViewModelFactory
import com.nottingham.mynottingham.ui.errand.ErrandTask
import java.util.UUID

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RestaurantMenuViewModel by activityViewModels()
    private val errandViewModel: ErrandViewModel by activityViewModels {
        ErrandViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDeliveryTimeSelection()
        setupClickListeners()
        setupObservers()
    }

    private fun setupDeliveryTimeSelection() {
        // Set initial delivery option (ASAP is default checked)
        viewModel.setDeliveryOption("ASAP (within 30 mins)", 2.0)

        binding.rgDeliveryTime.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rb_delivery_asap -> {
                    viewModel.setDeliveryOption("ASAP (within 30 mins)", 2.0)
                }
                R.id.rb_delivery_1_hour -> {
                    viewModel.setDeliveryOption("Within 1 hour", 0.0)
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.subtotal.observe(viewLifecycleOwner) { subtotal ->
            binding.tvSubtotal.text = String.format("RM %.2f", subtotal)
        }

        viewModel.deliveryFee.observe(viewLifecycleOwner) { fee ->
            binding.tvDeliveryFee.text = String.format("RM %.2f", fee)
        }

        viewModel.totalPrice.observe(viewLifecycleOwner) { total ->
            binding.tvTotal.text = String.format("RM %.2f", total)
        }
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbarCheckout)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupClickListeners() {
        binding.btnConfirmOrder.setOnClickListener {
            val address = binding.etAddress.text.toString().trim()
            val phone = binding.etPhoneNumber.text.toString().trim()
            
            if (address.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedDeliveryTimeId = binding.rgDeliveryTime.checkedRadioButtonId
            val deliveryTime = view?.findViewById<RadioButton>(selectedDeliveryTimeId)?.text.toString()

            // Construct description
            val items = viewModel.cartItems.value ?: emptyList()
            val description = items.joinToString(separator = "\n") {
                "${it.menuItem.name} x${it.quantity}"
            } + "\n\nPhone: $phone"

            // Create ErrandTask
            val restaurantName = viewModel.restaurantName.value ?: "a restaurant"
            val newTask = ErrandTask(
                taskId = UUID.randomUUID().toString(),
                title = "Buy food from $restaurantName",
                description = description,
                location = address,
                price = (viewModel.deliveryFee.value ?: 0.0).toString(),
                deadline = deliveryTime,
                requesterId = "", // Will be set by the ViewModel
                requesterName = "", // Will be set by the ViewModel
                requesterAvatar = "",
                timestamp = System.currentTimeMillis()
            )

            // Add task via ErrandViewModel
            errandViewModel.addTask(newTask)

            Toast.makeText(requireContext(), "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
            
            // Navigate to OrderSuccessFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.errand_fragment_container, OrderSuccessFragment())
                .commit()
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
