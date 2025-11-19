package com.example.talabat.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.talabat.databinding.FragmentCartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.talabat.models.Order

class CartFragment : Fragment() {

    private lateinit var binding: FragmentCartBinding
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)

        setupRecycler()
        refreshUI()

        // Back button
        binding.cartToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnClearCart.setOnClickListener {
            CartManager.clear()
            refreshUI()
        }

        binding.btnCheckout.setOnClickListener { placeOrder() }

        return binding.root
    }

    private fun setupRecycler() {
        binding.recyclerViewCart.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCart.adapter = CartAdapter(
            CartManager.items.toMutableList(),
            onIncrease = {
                CartManager.increase(it); refreshUI()
            },
            onDecrease = {
                CartManager.decrease(it); refreshUI()
            },
            onRemove = {
                CartManager.remove(it); refreshUI()
            }
        )
    }

    private fun refreshUI() {
        binding.recyclerViewCart.adapter = CartAdapter(
            CartManager.items.toMutableList(),
            onIncrease = { CartManager.increase(it); refreshUI() },
            onDecrease = { CartManager.decrease(it); refreshUI() },
            onRemove = { CartManager.remove(it); refreshUI() }
        )

        binding.tvTotal.text = "Total: ${CartManager.totalPrice()}"
        binding.btnCheckout.isEnabled = !CartManager.isEmpty()
    }

    private fun placeOrder() {
        val buyerId = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (CartManager.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Group products by seller → each seller gets ONE order
        val itemsBySeller = CartManager.items.groupBy { it.product.sellerId }

        val ordersRef = db.child("orders")

        itemsBySeller.forEach { (sellerId, cartItems) ->

            val orderId = ordersRef.push().key!!
            val itemsList = mutableListOf<Map<String, Any>>()
            var totalPrice = 0.0

            cartItems.forEach { item ->
                val map = mapOf(
                    "productId" to item.product.id,
                    "productName" to item.product.name,
                    "quantity" to item.quantity,
                    "price" to item.product.price
                )
                itemsList.add(map)

                totalPrice += item.product.price * item.quantity
            }

            val order = Order(
                orderId = orderId,
                buyerId = buyerId,
                sellerId = sellerId,
                items = itemsList,
                totalPrice = totalPrice,
                status = "waiting",                    // IMPORTANT !!
                deliveryOption = "pickup",             // temporary until we add UI
                deliveryAddress = "",
                deliveryPrice = 0.0
            )

            ordersRef.child(orderId).setValue(order)

            // Reduce product quantities
            cartItems.forEach { cartItem ->
                val productRef = db.child("sellers")
                    .child(sellerId)
                    .child("products")
                    .child(cartItem.product.id)

                productRef.child("quantity").get().addOnSuccessListener { snap ->
                    val currentQty = snap.getValue(Int::class.java) ?: 0
                    val newQty = (currentQty - cartItem.quantity).coerceAtLeast(0)
                    productRef.child("quantity").setValue(newQty)
                }
            }
        }

        CartManager.clear()
        refreshUI()

        Toast.makeText(requireContext(), "Order placed!", Toast.LENGTH_SHORT).show()
    }

}
