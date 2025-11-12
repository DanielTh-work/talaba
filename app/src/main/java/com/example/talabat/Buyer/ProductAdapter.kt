package com.example.talabat.buyer

import android.graphics.Color
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

            // 🧠 Check stock status
            if (product.quantity <= 0) {
                // Out of stock: disable button, change text and color
                binding.btnAddToCart.text = "Out of Stock"
                binding.btnAddToCart.isEnabled = false
                binding.btnAddToCart.setBackgroundColor(Color.GRAY)
            } else {
                // In stock: normal behavior
                binding.btnAddToCart.text = "Add to Cart"
                binding.btnAddToCart.isEnabled = true
                binding.btnAddToCart.setBackgroundColor(Color.parseColor("#FF6F00"))

                // Add to cart logic
                binding.btnAddToCart.setOnClickListener {
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