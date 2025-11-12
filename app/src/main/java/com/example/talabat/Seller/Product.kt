package com.example.talabat.seller

import java.io.Serializable

data class Product(
    var id: String = "",
    var name: String = "",
    var price: Double = 0.0,
    var quantity: Int = 0,
    var imageUrl: String = ""
) : Serializable
