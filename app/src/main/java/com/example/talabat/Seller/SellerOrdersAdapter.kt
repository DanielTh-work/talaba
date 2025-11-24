package com.example.talabat.seller

import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.example.talabat.databinding.ItemSellerOrderBinding
import com.example.talabat.models.Order
import com.example.talabat.buyer.OrderTrackingFragment

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
            if (order.deliveryOption == "delivery") "Address: ${order.deliveryAddress}, ${order.deliveryExactLocation}"
            else "Pickup"

        // --- Track Order button ---
        b.btnTrackOrder.setOnClickListener {
            val fragment = OrderTrackingFragment.newInstance(order.orderId)
            val activity = b.root.context as? FragmentActivity
            activity?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(com.example.talabat.R.id.fragment_container, fragment)
                ?.addToBackStack(null)
                ?.commit()
        }

        // --- Status Buttons ---
        b.layoutButtons.removeAllViews()

        when (order.status) {
            "waiting" -> {
                addButton(b, "Approve") { onStatusChange(order.orderId, "preparing") }
                addButton(b, "Reject") { onStatusChange(order.orderId, "rejected") }
            }
            "preparing" -> addButton(b, "Mark Ready") { onStatusChange(order.orderId, "ready") }
            "ready" -> {
                if (order.deliveryOption == "delivery") {
                    addButton(b, "Start Delivering") { onStatusChange(order.orderId, "delivering") }
                    addButton(b, "Volunteer Needed") { onStatusChange(order.orderId, "waiting for volunteer") }
                } else {
                    addButton(b, "Mark Delivered") { onStatusChange(order.orderId, "delivered") }
                }
            }
            "delivering" -> addButton(b, "Delivered") { onStatusChange(order.orderId, "delivered") }
        }
    }

    private fun addButton(binding: ItemSellerOrderBinding, text: String, onClick: () -> Unit) {
        val btn = Button(binding.root.context)
        btn.text = text
        btn.setOnClickListener { onClick() }
        binding.layoutButtons.addView(btn)
    }

    override fun getItemCount() = orders.size
}
