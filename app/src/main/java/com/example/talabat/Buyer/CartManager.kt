package com.example.talabat.buyer

object CartManager {

    private val _items = mutableListOf<CartItem>()
    val items: List<CartItem> get() = _items

    var currentSellerId: String? = null    // ⭐ NEW: Track locked seller

    // Clear everything
    fun clear() {
        _items.clear()
        currentSellerId = null            // Reset seller lock
    }

    // Add item (with seller check)
    fun addItem(product: Product, qty: Int): Boolean {
        val sellerId = product.sellerId

        // First item → lock seller
        if (currentSellerId == null) {
            currentSellerId = sellerId
        }

        // Trying to add from another seller?
        if (currentSellerId != sellerId) {
            return false                  // ❌ reject addition
        }

        val existing = _items.find { it.product.id == product.id }
        if (existing != null) {
            existing.quantity = qty
        } else {
            _items.add(CartItem(product, qty))
        }

        return true                       // ✔ added successfully
    }

    fun updateQuantity(product: Product, qty: Int) {
        val existing = _items.find { it.product.id == product.id }
        if (existing != null) {
            existing.quantity = qty
        }
    }

    fun removeItem(product: Product) {
        _items.removeAll { it.product.id == product.id }
        if (_items.isEmpty()) currentSellerId = null   // unlock seller
    }

    fun increase(item: CartItem) {
        val p = item.product
        if (item.quantity < p.quantity) {
            item.quantity++
        }
    }

    fun decrease(item: CartItem) {
        if (item.quantity > 1) item.quantity--
        else {
            _items.remove(item)
            if (_items.isEmpty()) currentSellerId = null
        }
    }

    fun remove(item: CartItem) {
        _items.remove(item)
        if (_items.isEmpty()) currentSellerId = null
    }

    fun totalPrice(): Double =
        _items.sumOf { it.quantity * (it.product.price ?: 0.0) }

    fun isEmpty(): Boolean = _items.isEmpty()
}
