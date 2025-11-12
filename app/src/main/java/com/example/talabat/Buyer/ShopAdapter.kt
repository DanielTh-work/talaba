package com.example.talabat.buyer

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.talabat.R
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.talabat.databinding.ItemShopBinding

class ShopAdapter(
    private val shops: List<Shop>,
    private val onShopClick: (Shop) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    inner class ShopViewHolder(private val binding: ItemShopBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(shop: Shop) {
            binding.tvShopName.text = shop.name
            if (!shop.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root).load(shop.imageUrl).into(binding.imgShop)
            } else {
                binding.imgShop.setImageResource(R.drawable.product_item_bg) // placeholder if you want
            }
            binding.root.setOnClickListener { onShopClick(shop) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ItemShopBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        holder.bind(shops[position])
    }

    override fun getItemCount(): Int = shops.size
}
