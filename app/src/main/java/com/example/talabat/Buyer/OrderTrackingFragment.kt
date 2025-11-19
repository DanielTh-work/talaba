package com.example.talabat.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.talabat.databinding.FragmentOrderTrackingBinding
import com.google.firebase.database.*

class OrderTrackingFragment : Fragment() {

    private lateinit var binding: FragmentOrderTrackingBinding
    private lateinit var dbRef: DatabaseReference
    private var orderId: String? = null

    companion object {
        fun newInstance(orderId: String): OrderTrackingFragment {
            val fragment = OrderTrackingFragment()
            val args = Bundle()
            args.putString("orderId", orderId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderTrackingBinding.inflate(inflater, container, false)

        orderId = arguments?.getString("orderId")

        if (orderId == null) {
            binding.tvStatus.text = "Invalid order"
            return binding.root
        }

        dbRef = FirebaseDatabase.getInstance().reference
            .child("orders")
            .child(orderId!!)

        listenForOrderUpdates()

        return binding.root
    }

    private fun listenForOrderUpdates() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (!snapshot.exists()) return

                val status = snapshot.child("status").getValue(String::class.java) ?: "waiting"
                val deliveryOption = snapshot.child("deliveryOption").getValue(String::class.java) ?: ""
                val address = snapshot.child("deliveryAddress").getValue(String::class.java) ?: ""

                binding.tvStatus.text = "Status: $status"

                binding.tvDeliveryOption.text = "Delivery Option: $deliveryOption"
                binding.tvDeliveryAddress.text =
                    if (deliveryOption == "delivery") "Address: $address"
                    else "Pickup from shop"

            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
