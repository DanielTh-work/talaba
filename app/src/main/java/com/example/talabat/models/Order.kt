package com.example.talabat.models

data class Order(
    val orderId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val totalPrice: Double = 0.0,

    var status: String = "waiting",
    var deliveryOption: String = "",
    var deliveryAddress: String = "",
    var deliveryExactLocation: String = "",

    var deliveryPrice: Double = 0.0,
    var deliveryVolunteerId: String? = null,
    val rewardPoints: Int = 10,

)

