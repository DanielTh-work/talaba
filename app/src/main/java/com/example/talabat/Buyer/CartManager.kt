package com.example.talabat.buyer

object CartManager {
    private val _items = mutableListOf<CartItem>()
    val items: List<CartItem> get() = _items

    fun clear() { _items.clear() }

    fun addToCart(product: Product) {
        val existing = _items.find { it.product.productId == product.productId }
        if (existing != null) {
            // ensure not exceeding available quantity
            if (product.quantity <= existing.quantity) return
            existing.quantity++
        } else {
            _items.add(CartItem(product, 1))
        }
    }

    fun increase(item: CartItem) {
        val p = item.product
        if (item.quantity < p.quantity) item.quantity++
    }

    fun decrease(item: CartItem) {
        if (item.quantity > 1) item.quantity-- else _items.remove(item)
    }

    fun remove(item: CartItem) { _items.remove(item) }

    fun totalPrice(): Double = _items.sumOf { it.quantity * (it.product.price ?: 0.0) }

    fun isEmpty() = _items.isEmpty()
}
