package com.example.talabat.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.talabat.databinding.FragmentMyOrdersBinding
import com.example.talabat.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.talabat.R


class MyOrdersFragment : Fragment() {

    private lateinit var binding: FragmentMyOrdersBinding
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val orders = mutableListOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyOrdersBinding.inflate(inflater, container, false)

        binding.rvMyOrders.layoutManager = LinearLayoutManager(requireContext())

        loadOrders()

        return binding.root
    }

    private fun loadOrders() {
        val buyerId = auth.currentUser?.uid ?: return

        db.child("orders")
            .orderByChild("buyerId")
            .equalTo(buyerId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    orders.clear()

                    for (child in snapshot.children) {
                        val order = child.getValue(Order::class.java)
                        if (order != null) orders.add(order)
                    }

                    binding.rvMyOrders.adapter =
                        MyOrdersAdapter(orders) { orderId ->
                            openTracking(orderId)
                        }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun openTracking(orderId: String) {
        val fragment = OrderTrackingFragment.newInstance(orderId)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_buyer, fragment)
            .addToBackStack(null)
            .commit()
    }
}
