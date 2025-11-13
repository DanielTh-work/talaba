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

        // ✅ Toolbar back button functionality
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
        val adapter = CartAdapter(
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
        binding.recyclerViewCart.adapter = adapter
    }

    private fun refreshUI() {
        // Refresh the RecyclerView adapter with updated cart data
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
            Toast.makeText(requireContext(), "You must be logged in to checkout", Toast.LENGTH_SHORT).show()
            return
        }

        if (CartManager.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Group cart items by seller
        val itemsBySeller = CartManager.items.groupBy { it.product.sellerId ?: "" }

        // For each seller, create orders under orders/{sellerId}/{pushId}
        itemsBySeller.forEach { (sellerId, items) ->
            if (sellerId.isEmpty()) return@forEach

            val ordersRef = db.child("orders").child(sellerId)

            items.forEach { cartItem ->
                val orderRef = ordersRef.push()
                val orderData = mapOf(
                    "orderId" to orderRef.key,
                    "buyerId" to buyerId,
                    "productId" to (cartItem.product.productId ?: ""),
                    "productName" to (cartItem.product.name ?: ""),
                    "quantity" to cartItem.quantity,
                    "totalPrice" to cartItem.quantity * (cartItem.product.price ?: 0.0),
                    "status" to "Pending",
                    "timestamp" to System.currentTimeMillis()
                )
                orderRef.setValue(orderData)

                // ✅ Correct path: sellers/{sellerId}/products/{productId}/quantity
                val prodRef = db.child("sellers")
                    .child(sellerId)
                    .child("products")
                    .child(cartItem.product.productId!!)

                // Read, subtract, and update quantity
                prodRef.child("quantity").get().addOnSuccessListener { snap ->
                    val currentQty = snap.getValue(Int::class.java) ?: cartItem.product.quantity
                    val newQty = (currentQty - cartItem.quantity).coerceAtLeast(0)
                    prodRef.child("quantity").setValue(newQty)
                        .addOnSuccessListener {
                            android.util.Log.d("Checkout", "Updated ${cartItem.product.name}: $currentQty → $newQty")
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("Checkout", "Failed to update quantity: ${e.message}")
                        }
                }.addOnFailureListener { e ->
                    android.util.Log.e("Checkout", "Error reading quantity: ${e.message}")
                }
            }
        }

        // Clear cart and refresh UI
        CartManager.clear()
        refreshUI()
        Toast.makeText(requireContext(), "Order placed successfully!", Toast.LENGTH_SHORT).show()
    }
}
