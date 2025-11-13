package com.example.talabat.buyer

object CartManager {

    private val _items = mutableListOf<CartItem>()
    val items: List<CartItem> get() = _items

    fun clear() {
        _items.clear()
    }

    fun addItem(product: Product, qty: Int) {
        val existing = _items.find { it.product.id == product.id }
        if (existing != null) {
            existing.quantity = qty
        } else {
            _items.add(CartItem(product, qty))
        }
    }

    fun updateQuantity(product: Product, qty: Int) {
        val existing = _items.find { it.product.id == product.id }
        if (existing != null) {
            existing.quantity = qty
        }
    }

    fun removeItem(product: Product) {
        _items.removeAll { it.product.id == product.id }
    }

    fun increase(item: CartItem) {
        val p = item.product
        if (item.quantity < p.quantity) {
            item.quantity++
        }
    }

    fun decrease(item: CartItem) {
        if (item.quantity > 1) item.quantity--
        else _items.remove(item)
    }

    fun remove(item: CartItem) {
        _items.remove(item)
    }

    fun totalPrice(): Double =
        _items.sumOf { it.quantity * (it.product.price ?: 0.0) }

    fun isEmpty(): Boolean = _items.isEmpty()
}
