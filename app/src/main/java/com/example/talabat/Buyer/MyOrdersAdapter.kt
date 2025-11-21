package com.example.talabat.buyer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.talabat.databinding.ItemOrderBinding
import com.example.talabat.models.Order

class MyOrdersAdapter(
    private val orders: List<Order>,
    private val onTrackClicked: (String) -> Unit
) : RecyclerView.Adapter<MyOrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ItemOrderBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.binding.tvOrderId.text = "Order: ${order.orderId}"
        holder.binding.tvOrderStatus.text = "Status: ${order.status}"
        holder.binding.tvDeliveryFee.text="Delivery Fee: ${order.deliveryPrice} EGP"
        holder.binding.tvOrderTotal.text = "Total: ${order.totalPrice} EGP"

        holder.binding.btnTrack.setOnClickListener {
            onTrackClicked(order.orderId)
        }
    }

    override fun getItemCount() = orders.size
}
