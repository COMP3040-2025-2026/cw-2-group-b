package com.nottingham.mynottingham.data.model

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int
) {
    val totalPrice: Double
        get() = menuItem.price * quantity
}