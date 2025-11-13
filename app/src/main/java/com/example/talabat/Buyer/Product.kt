package com.example.talabat.buyer

data class Product(
    val id: String = "",          // Firebase productId
    val sellerId: String = "",    // Firebase seller UID
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val imageUrl: String = ""
)
