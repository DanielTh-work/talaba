package com.example.talabat.buyer

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.talabat.databinding.ItemProductBuyerBinding

class ProductAdapter(
    private val products: List<Product>,
    private val onAddToCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemProductBuyerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = "Price: \$${product.price}"
            binding.tvProductQuantity.text = "Qty: ${product.quantity}"

            // Load product image
            if (!product.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(product.imageUrl)
                    .into(binding.imgProduct)
            } else {
                binding.imgProduct.setImageResource(android.R.color.darker_gray) // placeholder
            }

            // Add to cart button
            binding.btnAddToCart.setOnClickListener {
                if (product.quantity <= 0) {
                    Toast.makeText(binding.root.context, "Out of stock", Toast.LENGTH_SHORT).show()
                } else {
                    onAddToCart(product)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBuyerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size
}
