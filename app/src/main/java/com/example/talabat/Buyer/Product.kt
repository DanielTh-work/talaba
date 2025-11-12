package com.example.talabat.buyer

data class Product(
    var productId: String? = null,
    val name: String? = null,
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val quantity: Int = 0,
    var sellerId: String? = null
)