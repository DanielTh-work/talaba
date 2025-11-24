package com.example.talabat.buyer

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.example.talabat.R
import com.example.talabat.databinding.FragmentOrderTrackingBinding
import com.google.firebase.database.*

class OrderTrackingFragment : Fragment() {

    private lateinit var binding: FragmentOrderTrackingBinding
    private lateinit var dbRef: DatabaseReference
    private var orderId: String? = null
    private var lastStatus: String = "" // Prevent duplicate notifications

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
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        dbRef = FirebaseDatabase.getInstance().reference
            .child("orders")
            .child(orderId!!)

        listenForOrderUpdates()

        return binding.root

    }

    private fun sendStatusNotification(status: String) {
        val manager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(requireContext(), NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Order Update")
            .setContentText("Your order is now: $status")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(1001, builder.build())
    }

    private fun listenForOrderUpdates() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val status = snapshot.child("status").getValue(String::class.java) ?: "waiting"
                val deliveryOption = snapshot.child("deliveryOption").getValue(String::class.java) ?: ""
                val address = snapshot.child("deliveryAddress").getValue(String::class.java) ?: ""
                val exactLocation = snapshot.child("deliveryExactLocation").getValue(String::class.java) ?: ""
                val totalPrice = snapshot.child("totalPrice").getValue(Double::class.java) ?: 0.0
                val itemsSnapshot = snapshot.child("items")
                val sellerId = snapshot.child("sellerId").getValue(String::class.java) ?: ""

                // Update status & delivery info
                binding.tvStatus.text = "Status: $status"

                // Fetch shop name
                if (sellerId.isNotEmpty()) {
                    FirebaseDatabase.getInstance().reference
                        .child("sellers")
                        .child(sellerId)
                        .child("shopName") // Adjust field name to match your database
                        .get()
                        .addOnSuccessListener { snap ->
                            val shopName = snap.getValue(String::class.java) ?: "Unknown Shop"
                            binding.tvShopName.text = "Shop: $shopName"
                        }
                        .addOnFailureListener {
                            binding.tvShopName.text = "Shop: Unknown"
                        }
                } else {
                    binding.tvShopName.text = "Shop: Unknown"
                }

                binding.tvDeliveryOption.text = "Delivery Option: $deliveryOption"
                binding.tvDeliveryAddress.text =
                    if (deliveryOption == "delivery") "Address: $address, $exactLocation"
                    else "Pickup from shop"

                // Notification on status change
                if (lastStatus != status) {
                    if (lastStatus.isNotEmpty()) sendStatusNotification(status)
                    lastStatus = status
                }

                // Clear previous items
                binding.llOrderItems.removeAllViews()

                if (itemsSnapshot.exists() && sellerId.isNotEmpty()) {
                    for (item in itemsSnapshot.children) {
                        val productId = item.child("productId").getValue(String::class.java) ?: ""
                        val quantity = item.child("quantity").getValue(Int::class.java) ?: 0
                        val price = item.child("price").getValue(Double::class.java) ?: 0.0

                        if (productId.isNotEmpty()) {
                            // Fetch product name from seller's products
                            FirebaseDatabase.getInstance().reference
                                .child("sellers")
                                .child(sellerId)
                                .child("products")
                                .child(productId)
                                .child("name")
                                .get()
                                .addOnSuccessListener { productNameSnapshot ->
                                    val productName = productNameSnapshot.getValue(String::class.java) ?: "Unknown"

                                    val row = LinearLayout(requireContext()).apply {
                                        orientation = LinearLayout.HORIZONTAL
                                        layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        )
                                        setPadding(0, 6, 0, 6)
                                    }

                                    val tvName = TextView(requireContext()).apply {
                                        text = productName
                                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
                                        textSize = 16f
                                    }

                                    val tvQtyPrice = TextView(requireContext()).apply {
                                        text = "$quantity x $price"
                                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                        textSize = 16f
                                    }

                                    val tvItemTotal = TextView(requireContext()).apply {
                                        val itemTotal = quantity * price
                                        text = "$itemTotal"
                                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                        textSize = 16f
                                    }

                                    row.addView(tvName)
                                    row.addView(tvQtyPrice)
                                    row.addView(tvItemTotal)

                                    binding.llOrderItems.addView(row)
                                }
                        }
                    }
                }

                // Display total price directly from Firebase
                val tvTotal = TextView(requireContext()).apply {
                    text = "Total: $totalPrice EGP"
                    textSize = 18f
                    setPadding(0, 12, 0, 0)
                }

                binding.llOrderItems.addView(tvTotal)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
