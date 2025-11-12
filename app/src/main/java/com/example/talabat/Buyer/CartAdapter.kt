package com.example.talabat.buyer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.talabat.databinding.ItemCartBinding

class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit,
    private val onRemove: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.tvCartName.text = item.product.name
            binding.tvCartQty.text = "Qty: ${item.quantity}"
            binding.tvCartPrice.text = "Price: ${item.product.price}"
            if (!item.product.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root).load(item.product.imageUrl).into(binding.imgCart)
            }
            binding.btnIncrease.setOnClickListener { onIncrease(item); notifyItemChanged(bindingAdapterPosition) }
            binding.btnDecrease.setOnClickListener { onDecrease(item); if (items.contains(item)) notifyItemChanged(bindingAdapterPosition) else notifyDataSetChanged() }
            binding.btnRemove.setOnClickListener { onRemove(item); notifyDataSetChanged() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
