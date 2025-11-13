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

        // Group items by seller
        val itemsBySeller = CartManager.items.groupBy { it.product.sellerId }

        itemsBySeller.forEach { (sellerId, items) ->
            if (sellerId.isEmpty()) return@forEach

            val sellerOrdersRef = db.child("orders").child(sellerId)

            items.forEach { cartItem ->
                val product = cartItem.product

                // Create a new order entry
                val orderRef = sellerOrdersRef.push()
                val orderData = mapOf(
                    "orderId" to orderRef.key,
                    "buyerId" to buyerId,
                    "productId" to product.id,                // FIXED
                    "productName" to product.name,
                    "quantity" to cartItem.quantity,
                    "totalPrice" to cartItem.quantity * product.price,
                    "status" to "Pending",
                    "timestamp" to System.currentTimeMillis()
                )

                orderRef.setValue(orderData)

                // Update product stock in Firebase
                val productRef = db.child("sellers")
                    .child(sellerId)
                    .child("products")
                    .child(product.id)                         // FIXED

                productRef.child("quantity").get().addOnSuccessListener { snap ->
                    val currentQty = snap.getValue(Int::class.java) ?: product.quantity
                    val newQty = (currentQty - cartItem.quantity).coerceAtLeast(0)

                    productRef.child("quantity").setValue(newQty)
                }
            }
        }

        // Clear cart
        CartManager.clear()
        refreshUI()

        Toast.makeText(requireContext(), "Order placed successfully!", Toast.LENGTH_SHORT).show()
    }
}
