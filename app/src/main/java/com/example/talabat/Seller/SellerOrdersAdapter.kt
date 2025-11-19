package com.example.talabat.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.talabat.databinding.ItemSellerOrderBinding
import com.example.talabat.models.Order

class SellerOrdersAdapter(
    private val orders: List<Order>,
    private val onStatusChange: (String, String) -> Unit
) : RecyclerView.Adapter<SellerOrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ItemSellerOrderBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemSellerOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        val b = holder.binding

        b.tvOrderId.text = "Order ID: ${order.orderId}"
        b.tvStatus.text = "Status: ${order.status}"
        b.tvTotal.text = "Total: ${order.totalPrice} EGP"
        b.tvDeliveryOption.text = "Delivery: ${order.deliveryOption}"
        b.tvDeliveryAddress.text =
            if (order.deliveryOption == "delivery") "Address: ${order.deliveryAddress}"
            else "Pickup"

        // Clear previous buttons
        b.layoutButtons.removeAllViews()

        when (order.status) {

            "waiting" -> {
                addButton(b, "Approve") {
                    onStatusChange(order.orderId, "preparing")
                }
                addButton(b, "Reject") {
                    onStatusChange(order.orderId, "rejected")
                }
            }

            "preparing" -> {
                addButton(b, "Mark Ready") {
                    onStatusChange(order.orderId, "ready")
                }
            }

            "ready" -> {
                if (order.deliveryOption == "delivery") {
                    addButton(b, "Start Delivering") {
                        onStatusChange(order.orderId, "delivering")
                    }
                } else {
                    addButton(b, "Mark Delivered") {
                        onStatusChange(order.orderId, "delivered")
                    }
                }
            }

            "delivering" -> {
                addButton(b, "Delivered") {
                    onStatusChange(order.orderId, "delivered")
                }
            }
        }
    }

    private fun addButton(binding: ItemSellerOrderBinding, text: String, onClick: () -> Unit) {
        val btn = android.widget.Button(binding.root.context)
        btn.text = text
        btn.setOnClickListener { onClick() }
        binding.layoutButtons.addView(btn)
    }

    override fun getItemCount() = orders.size
}
