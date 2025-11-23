package com.nottingham.mynottingham.ui.errand

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.database.AppDatabase
import com.nottingham.mynottingham.data.local.database.entities.ErrandEntity
import com.nottingham.mynottingham.data.model.CartItem
import com.nottingham.mynottingham.data.model.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class RestaurantMenuViewModel(application: Application) : AndroidViewModel(application) {

    private val errandDao = AppDatabase.getInstance(application).errandDao()

    // 6个固定菜品数据
    val menuItems = listOf(
        MenuItem("1", "noodles", "Beef Noodle Soup", "Traditional beef noodle with rich broth", 12.50),
        MenuItem("2", "noodles", "Fried Noodles", "Stir-fried noodles with vegetables", 10.00),
        MenuItem("3", "rice", "Chicken Fried Rice", "Fragrant fried rice with chicken", 11.00),
        MenuItem("4", "rice", "Combo Rice Set", "Rice with 2 meat and 2 veg dishes", 15.00),
        MenuItem("5", "drinks", "Bubble Tea", "Sweet milk tea with pearls", 6.00),
        MenuItem("6", "drinks", "Iced Lemon Tea", "Refreshing lemon tea", 4.50)
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

    // 购物车状态: Map<ItemId, Count>，用于 UI 快速响应
    private val _cartQuantities = MutableLiveData<Map<String, Int>>(emptyMap())
    val cartQuantities: LiveData<Map<String, Int>> = _cartQuantities

    // 购物车详细信息 (用于计算总价和下单)
    private val _cartItems = MutableLiveData<List<CartItem>>(emptyList())
    val cartItems: LiveData<List<CartItem>> = _cartItems
    
    // 总价和总数
    private val _totalPrice = MutableLiveData(0.0)
    val totalPrice: LiveData<Double> = _totalPrice
    
    private val _totalCount = MutableLiveData(0)
    val totalCount: LiveData<Int> = _totalCount

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
        var total = 0.0
        var count = 0
        val itemsList = mutableListOf<CartItem>()

        qtyMap.forEach { (id, qty) ->
            val menuItem = menuItems.find { it.id == id }
            if (menuItem != null) {
                total += menuItem.price * qty
                count += qty
                itemsList.add(CartItem(menuItem, qty))
            }
        }
        
        // 加上 RM 2.00 运费 (如果有商品)
        if (count > 0) total += 2.00

        _totalPrice.value = total
        _totalCount.value = count
        _cartItems.value = itemsList
    }

    fun placeOrder(userId: String, address: String) {
        val items = _cartItems.value ?: return
        if (items.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val sb = StringBuilder()
            sb.append("Order from Chinese Restaurant:\n")
            items.forEach { 
                sb.append("- ${it.menuItem.name} x${it.quantity}\n") 
            }
            sb.append("\nDelivery Fee: RM 2.00")

            val errand = ErrandEntity(
                id = UUID.randomUUID().toString(),
                requesterId = userId,
                title = "Food Delivery: Chinese Restaurant",
                description = sb.toString(),
                type = "Food Delivery",
                priority = "Standard",
                pickupLocation = "Golden Dragon Restaurant",
                deliveryLocation = address,
                fee = _totalPrice.value ?: 0.0,
                status = "pending",
                imageUrl = "android.resource://com.nottingham.mynottingham/drawable/food_beef_noodle", // 示例图片
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            errandDao.insertErrand(errand)
        }
    }
}
