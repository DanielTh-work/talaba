package com.example.talabat.Delivery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.talabat.databinding.OrderItemDeliveryBinding
import com.example.talabat.models.Order
import com.google.firebase.auth.FirebaseAuth

class DeliveryOrdersAdapter(
    private val orders: List<Order>,
    private val onTakeOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<DeliveryOrdersAdapter.OrderViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    inner class OrderViewHolder(val binding: OrderItemDeliveryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.tvOrderTitle.text = "Order #${order.orderId}"
            binding.tvOrderDetails.text =
                "Pickup: ${if (order.deliveryOption == "pickup") "Restaurant" else "Shop"}\n" +
                        "Dropoff: ${order.deliveryAddress}\n" +
                        "Total: ${order.totalPrice } EGP"

            binding.tvRewardPoints.text = "Reward: ${order.rewardPoints} points"

            when {
                order.deliveryVolunteerId.isNullOrEmpty() -> {
                    // Order available
                    binding.btnTakeOrder.text = "Take Order"
                    binding.btnTakeOrder.isEnabled = true
                    binding.btnTakeOrder.alpha = 1f
                }
                order.deliveryVolunteerId == currentUserId -> {
                    // Already taken by this delivery volunteer
                    binding.btnTakeOrder.text = "Taken by You"
                    binding.btnTakeOrder.isEnabled = false
                    binding.btnTakeOrder.alpha = 0.6f
                }
                else -> {
                    // Taken by another volunteer
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
