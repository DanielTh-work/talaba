package com.example.talabat.buyer

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat      // ⭐ REQUIRED
import androidx.fragment.app.Fragment
import com.example.talabat.R
import com.example.talabat.databinding.FragmentOrderTrackingBinding
import com.google.firebase.database.*

class OrderTrackingFragment : Fragment() {

    private lateinit var binding: FragmentOrderTrackingBinding
    private lateinit var dbRef: DatabaseReference
    private var orderId: String? = null
    private var lastStatus: String = ""    // ⭐ prevents duplicate notifications

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

    // ⭐ This sends the notification
    private fun sendStatusNotification(status: String) {

        val manager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(requireContext(), NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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

                binding.tvStatus.text = "Status: $status"
                binding.tvDeliveryOption.text = "Delivery Option: $deliveryOption"
                binding.tvDeliveryAddress.text =
                    if (deliveryOption == "delivery") "Address: $address"
                    else "Pickup from shop"

                // ⭐ Send notification only if status changed
                if (lastStatus != status) {
                    if (lastStatus != "") {  // skip first load
                        sendStatusNotification(status)
                    }
                    lastStatus = status
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
