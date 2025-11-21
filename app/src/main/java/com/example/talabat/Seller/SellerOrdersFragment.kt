package com.example.talabat.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.talabat.databinding.FragmentSellerOrdersBinding
import com.example.talabat.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SellerOrdersFragment : Fragment() {

    private lateinit var binding: FragmentSellerOrdersBinding
    private val orders = mutableListOf<Order>()
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSellerOrdersBinding.inflate(inflater, container, false)

        binding.rvSellerOrders.layoutManager = LinearLayoutManager(requireContext())

        loadOrders()

        return binding.root
    }

    private fun loadOrders() {
        val sellerId = auth.currentUser?.uid ?: return

        db.child("orders")
            .orderByChild("sellerId")
            .equalTo(sellerId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    orders.clear()

                    for (child in snapshot.children) {
                        val order = child.getValue(Order::class.java)
                        if (order != null) orders.add(order)
                    }

                    binding.rvSellerOrders.adapter =
                        SellerOrdersAdapter(orders) { orderId, newStatus ->
                            updateOrderStatus(orderId, newStatus)
                        }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateOrderStatus(orderId: String, status: String) {

        db.child("orders").child(orderId).child("status").setValue(status)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Status updated: $status", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
