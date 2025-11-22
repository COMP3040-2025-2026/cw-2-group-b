package com.nottingham.mynottingham.data.model

/**
 * Restaurant data model
 */
data class Restaurant(
    val id: String,
    val name: String,
    val cuisine: String,
    val rating: Double,
    val monthlySales: String,
    val openingHours: String,
    val imageUrl: String? = null,
    val menuCategories: List<MenuCategory> = emptyList()
)

/**
 * Menu category (e.g., Noodles, Rice Dishes)
 */
data class MenuCategory(
    val id: String,
    val name: String,
    val icon: String,
    val items: List<MenuItem> = emptyList()
)

/**
 * Shopping cart
 */
data class ShoppingCart(
    val restaurantId: String,
    val items: MutableList<CartItem> = mutableListOf()
) {
    val subtotal: Double
        get() = items.sumOf { it.totalPrice }

    val deliveryFee: Double = 2.00

    val total: Double
        get() = subtotal + deliveryFee

    val itemCount: Int
        get() = items.sumOf { it.quantity }
}

/**
 * Delivery address
 */
data class DeliveryAddress(
    val id: String,
    val name: String,
    val addressLine: String,
    val contact: String,
    val isDefault: Boolean = false
)

/**
 * Payment method
 */
enum class PaymentMethod(val displayName: String, val description: String) {
    CREDIT_CARD("Credit/Debit Card", "Visa, Mastercard, etc."),
    E_WALLET("E-Wallet", "Touch 'n Go, GrabPay"),
    CASH_ON_DELIVERY("Cash on Delivery", "Pay when you receive")
}

/**
 * Order
 */
data class FoodOrder(
    val id: String,
    val restaurantId: String,
    val restaurantName: String,
    val items: List<CartItem>,
    val deliveryAddress: DeliveryAddress,
    val paymentMethod: PaymentMethod,
    val subtotal: Double,
    val deliveryFee: Double,
    val total: Double,
    val status: OrderStatus,
    val createdAt: Long
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
