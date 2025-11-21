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
import com.example.talabat.R

class CartFragment : Fragment() {

    private lateinit var binding: FragmentCartBinding
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    // Delivery data (updated from DeliveryOptionsFragment)
    private var selectedDeliveryOption = "pickup"
    private var selectedDeliveryAddress = ""
    private var selectedDeliveryPrice = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)

        setupRecycler()
        refreshUI()

        // 🔥 Listen for delivery option result from DeliveryOptionsFragment
        parentFragmentManager.setFragmentResultListener("deliveryData", viewLifecycleOwner) { _, result ->

            selectedDeliveryOption = result.getString("deliveryOption") ?: "pickup"
            selectedDeliveryAddress = result.getString("deliveryAddress") ?: ""
            selectedDeliveryPrice = result.getDouble("deliveryPrice")

            // After selecting delivery option → create the order
            placeOrderWithDelivery()
        }

        // Back button
        binding.cartToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnClearCart.setOnClickListener {
            CartManager.clear()
            refreshUI()
        }

        // 🔥 Checkout → open DeliveryOptionsFragment
        binding.btnCheckout.setOnClickListener {
            val fragment = DeliveryOptionsFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, fragment)
                .addToBackStack(null)
                .commit()
        }

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

    // 🔥 NEW Updated Place Order with Delivery Logic
    private fun placeOrderWithDelivery() {

        val buyerId = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (CartManager.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val itemsBySeller = CartManager.items.groupBy { it.product.sellerId }
        val ordersRef = db.child("orders")

        itemsBySeller.forEach { (sellerId, cartItems) ->

            val orderId = ordersRef.push().key!!      // ⭐ important – we pass this to tracking
            val itemsList = mutableListOf<Map<String, Any>>()
            var totalPrice = 0.0

            cartItems.forEach { item ->
                itemsList.add(
                    mapOf(
                        "productId" to item.product.id,
                        "productName" to item.product.name,
                        "quantity" to item.quantity,
                        "price" to item.product.price
                    )
                )
                totalPrice += item.product.price * item.quantity
            }

            val order = Order(
                orderId = orderId,
                buyerId = buyerId,
                sellerId = sellerId,
                items = itemsList,
                totalPrice = totalPrice + selectedDeliveryPrice,
                status = "waiting",
                deliveryOption = selectedDeliveryOption,
                deliveryAddress = selectedDeliveryAddress,
                deliveryPrice = selectedDeliveryPrice
            )

            ordersRef.child(orderId).setValue(order)

            // Update inventory
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

            // ⭐ Clear cart AFTER saving the first order
            CartManager.clear()
            refreshUI()

            // ⭐ Navigate to OrderTrackingFragment
            val fragment = OrderTrackingFragment.newInstance(orderId)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, fragment)
                .addToBackStack(null)
                .commit()

            Toast.makeText(requireContext(), "Order placed!", Toast.LENGTH_SHORT).show()

            return  // make sure we exit loop after first seller order
        }
    }

}
