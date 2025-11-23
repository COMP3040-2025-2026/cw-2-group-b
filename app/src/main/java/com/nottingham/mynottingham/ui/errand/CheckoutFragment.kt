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

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RestaurantMenuViewModel by activityViewModels()

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
        setupClickListeners()
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

            val selectedPaymentId = binding.rgPaymentMethod.checkedRadioButtonId
            val paymentMethod = view?.findViewById<RadioButton>(selectedPaymentId)?.text.toString()

            // Call the updated placeOrder method
            viewModel.placeOrder("user_123", address, phone, paymentMethod)

            Toast.makeText(requireContext(), "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
            
            // Clear the back stack up to the main errand/restaurant list screen
            // This will pop CheckoutFragment, CartFragment, and RestaurantMenuFragment
            parentFragmentManager.popBackStack("restaurant_list", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

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
