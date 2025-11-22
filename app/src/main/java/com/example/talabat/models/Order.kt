package com.example.talabat.models

data class Order(
    val orderId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val totalPrice: Double = 0.0,
    var status: String = "waiting",          // waiting → preparing → ready → delivered
    var deliveryOption: String = "",         // "pickup" or "delivery"
    var deliveryAddress: String = "",
    var deliveryPrice: Double = 0.0,
    var deliveryVolunteerId: String? = null, // UID of volunteer (same as buyer UID)
    val rewardPoints: Int = 10,              // default points for delivery
    val timestamp: Long = System.currentTimeMillis()
)
