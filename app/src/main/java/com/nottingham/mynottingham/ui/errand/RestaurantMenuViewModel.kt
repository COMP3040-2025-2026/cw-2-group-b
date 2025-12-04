package com.nottingham.mynottingham.ui.errand

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.model.CartItem
import com.nottingham.mynottingham.data.model.MenuItem
import com.nottingham.mynottingham.data.repository.FirebaseErrandRepository
import kotlinx.coroutines.launch

import com.nottingham.mynottingham.R

class RestaurantMenuViewModel(application: Application) : AndroidViewModel(application) {

    private val errandRepository = FirebaseErrandRepository()

    // 6 fixed menu items
    val menuItems = listOf(
        MenuItem("1", "noodles", "Beef Noodle Soup", "Traditional beef noodle with rich broth", 12.50, R.drawable.bsn),
        MenuItem("2", "noodles", "Fried Noodles", "Stir-fried noodles with vegetables", 10.00, R.drawable.fn),
        MenuItem("3", "rice", "Chicken Fried Rice", "Fragrant fried rice with chicken", 11.00, R.drawable.fcr),
        MenuItem("4", "rice", "Combo Rice Set", "Rice with 2 meat and 2 veg dishes", 15.00, R.drawable.crs),
        MenuItem("5", "drinks", "Bubble Tea", "Sweet milk tea with pearls", 6.00, R.drawable.bt),
        MenuItem("6", "drinks", "Iced Lemon Tea", "Refreshing lemon tea", 4.50, R.drawable.ilt),
        MenuItem("7", "drinks", "Milk", "Fresh milk", 3.00, R.drawable.nn),
        MenuItem("8", "drinks", "Soy Milk", "Fresh soy milk", 3.00, R.drawable.dj),
        MenuItem("9", "rice", "Mapo Tofu Rice", "Spicy tofu with rice", 13.00, R.drawable.mpdf),
        MenuItem("10", "rice", "Egg Fried Rice", "Classic egg fried rice", 8.00, R.drawable.dcf),
        MenuItem("11", "noodles", "Sour and Spicy Noodles", "Sour and spicy noodles", 12.00, R.drawable.snf),
        MenuItem("12", "noodles", "Fried Sauce Noodles", "Noodles with fried sauce", 11.00, R.drawable.zjm),
        MenuItem("13", "noodles", "Chongqing Noodles", "Spicy Chongqing noodles", 12.00, R.drawable.cq)
    )

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    private val _menuListWithHeaders = MutableLiveData<List<Any>>()
    val menuListWithHeaders: LiveData<List<Any>> = _menuListWithHeaders
    
    init {
        val categoryList = menuItems.map { it.category }.distinct()
        _categories.value = categoryList
        
        val combinedList = mutableListOf<Any>()
        categoryList.forEach { category ->
            combinedList.add(category) // Add header
            combinedList.addAll(menuItems.filter { it.category == category }) // Add items
        }
        _menuListWithHeaders.value = combinedList
    }

    // Cart state: Map<ItemId, Count> for quick UI response
    private val _cartQuantities = MutableLiveData<Map<String, Int>>(emptyMap())
    val cartQuantities: LiveData<Map<String, Int>> = _cartQuantities

    // Cart details (for calculating total price and placing order)
    private val _cartItems = MutableLiveData<List<CartItem>>(emptyList())
    val cartItems: LiveData<List<CartItem>> = _cartItems

    // Price details
    private val _subtotal = MutableLiveData(0.0)
    val subtotal: LiveData<Double> = _subtotal

    private val _deliveryFee = MutableLiveData(0.0)
    val deliveryFee: LiveData<Double> = _deliveryFee

    private val _totalPrice = MutableLiveData(0.0)
    val totalPrice: LiveData<Double> = _totalPrice

    private val _totalCount = MutableLiveData(0)
    val totalCount: LiveData<Int> = _totalCount

    private val _selectedDeliveryTime = MutableLiveData<String>()
    val selectedDeliveryTime: LiveData<String> = _selectedDeliveryTime

    private val _selectedDeliveryExtraFee = MutableLiveData(0.0)
    val selectedDeliveryExtraFee: LiveData<Double> = _selectedDeliveryExtraFee

    fun setDeliveryOption(deliveryTime: String, extraFee: Double) {
        _selectedDeliveryTime.value = deliveryTime
        _selectedDeliveryExtraFee.value = extraFee
        calculateTotals(_cartQuantities.value ?: emptyMap()) // Recalculate totals with new delivery extra fee
    }

    fun addItem(item: MenuItem) {
        updateCart(item, 1)
    }

    fun increaseItem(item: MenuItem) {
        updateCart(item, 1)
    }

    fun decreaseItem(item: MenuItem) {
        updateCart(item, -1)
    }

    private fun updateCart(item: MenuItem, change: Int) {
        val currentQtyMap = _cartQuantities.value?.toMutableMap() ?: mutableMapOf()
        val currentCount = currentQtyMap[item.id] ?: 0
        val newCount = currentCount + change

        if (newCount <= 0) {
            currentQtyMap.remove(item.id)
        } else {
            currentQtyMap[item.id] = newCount
        }

        _cartQuantities.value = currentQtyMap
        calculateTotals(currentQtyMap)
    }

    private fun calculateTotals(qtyMap: Map<String, Int>) {
        var subtotalValue = 0.0
        var count = 0
        var baseDeliveryFee = 0.0 // Initialize fee accumulator
        val itemsList = mutableListOf<CartItem>()

        qtyMap.forEach { (id, qty) ->
            val menuItem = menuItems.find { it.id == id }
            if (menuItem != null) {
                subtotalValue += menuItem.price * qty
                count += qty
                itemsList.add(CartItem(menuItem, qty))

                // Delivery fee logic based on category
                when (menuItem.category) {
                    "noodles", "rice" -> baseDeliveryFee += 2.0 * qty
                    "drinks" -> baseDeliveryFee += 1.0 * qty
                }
            }
        }

        // Add selected delivery option's extra fee
        val totalDeliveryFee = baseDeliveryFee + (_selectedDeliveryExtraFee.value ?: 0.0)

        _subtotal.value = subtotalValue
        _deliveryFee.value = totalDeliveryFee
        _totalPrice.value = subtotalValue + totalDeliveryFee
        _totalCount.value = count
        _cartItems.value = itemsList
    }

    fun placeOrder(
        userId: String,
        userName: String,
        userAvatar: String?,
        address: String,
        contact: String,
        paymentMethod: String,
        deliveryTime: String
    ) {
        val items = _cartItems.value ?: return
        if (items.isEmpty()) return

        viewModelScope.launch {
            // Description contains order items and contact information
            val sb = StringBuilder()
            items.forEach {
                sb.append("${it.menuItem.name} x${it.quantity}\n")
            }
            sb.append("\nPhone: $contact")

            val errandData = mapOf(
                "requesterId" to userId,
                "requesterName" to userName,
                "requesterAvatar" to (userAvatar ?: "default"),
                "title" to "Buy food from Chinese Restaurant",
                "description" to sb.toString().trim(),
                "type" to "FOOD_DELIVERY",
                "location" to address,
                "orderAmount" to (_subtotal.value ?: 0.0),  // Food cost for rider to purchase
                "reward" to (_deliveryFee.value ?: 0.0),    // Delivery fee for rider
                "timeLimit" to deliveryTime
            )

            errandRepository.createErrand(errandData)

            // Clear cart after successful order
            _cartQuantities.postValue(emptyMap())
            _cartItems.postValue(emptyList())
            _subtotal.postValue(0.0)
            _deliveryFee.postValue(0.0)
            _totalPrice.postValue(0.0)
            _totalCount.postValue(0)
        }
    }
}
