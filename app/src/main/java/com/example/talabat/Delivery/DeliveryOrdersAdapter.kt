package com.example.talabat.Delivery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.talabat.databinding.OrderItemDeliveryBinding
import com.example.talabat.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DeliveryOrdersAdapter(
    private val orders: List<Order>,
    private val onTakeOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<DeliveryOrdersAdapter.OrderViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    inner class OrderViewHolder(val binding: OrderItemDeliveryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.tvOrderTitle.text = "Order #${order.orderId}"

            // Fetch shop name from Firebase
            if (!order.sellerId.isNullOrEmpty()) {
                FirebaseDatabase.getInstance().reference
                    .child("sellers")
                    .child(order.sellerId)
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

            binding.tvOrderDetails.text =
                "Pickup: ${if (order.deliveryOption == "pickup") "Restaurant" else "Shop"}\n" +
                        "Dropoff: ${order.deliveryAddress},${order.deliveryExactLocation}\n" +
                        "Total: ${order.totalPrice} EGP"

            binding.tvRewardPoints.text = "Reward: ${order.rewardPoints} points"

            when {
                order.deliveryVolunteerId.isNullOrEmpty() -> {
                    binding.btnTakeOrder.text = "Take Order"
                    binding.btnTakeOrder.isEnabled = true
                    binding.btnTakeOrder.alpha = 1f
                }
                order.deliveryVolunteerId == currentUserId -> {
                    binding.btnTakeOrder.text = "Taken by You"
                    binding.btnTakeOrder.isEnabled = false
                    binding.btnTakeOrder.alpha = 0.6f
                }
                else -> {
                    binding.btnTakeOrder.text = "Taken"
                    binding.btnTakeOrder.isEnabled = false
                    binding.btnTakeOrder.alpha = 0.6f
                }
            }

            binding.btnTakeOrder.setOnClickListener {
                if (order.deliveryVolunteerId.isNullOrEmpty()) {
                    onTakeOrderClick(order)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding =
            OrderItemDeliveryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun getItemCount(): Int = orders.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) =
        holder.bind(orders[position])
}
