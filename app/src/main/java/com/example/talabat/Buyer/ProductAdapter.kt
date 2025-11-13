package com.example.talabat.buyer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.talabat.databinding.ItemProductBuyerBinding

class ProductAdapter(
    private val products: List<Product>,
    private val onAddToCart: (Product, Int) -> Unit,
    private val onUpdateCart: (Product, Int) -> Unit,
    private val onRemoveFromCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // Keeps track of quantities locally
    private val quantities = mutableMapOf<String, Int>() // productId → qty

    inner class ProductViewHolder(val binding: ItemProductBuyerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {

            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = "Price: \$${product.price}"
            binding.tvProductQuantity.text = "Qty: ${product.quantity}"

            // Load image
            if (product.imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(product.imageUrl)
                    .into(binding.imgProduct)
            }

            // Out of stock
            if (product.quantity <= 0) {
                binding.btnAddToCart.text = "Out of Stock"
                binding.btnAddToCart.isEnabled = false
                binding.btnAddToCart.setBackgroundColor(Color.GRAY)
                binding.qtyLayout.visibility = View.GONE
                return
            }

            // Restore quantity
            val currentQty = quantities[product.id] ?: 0

            if (currentQty > 0) {
                binding.btnAddToCart.visibility = View.GONE
                binding.qtyLayout.visibility = View.VISIBLE
                binding.tvQty.text = currentQty.toString()
            } else {
                binding.btnAddToCart.visibility = View.VISIBLE
                binding.qtyLayout.visibility = View.GONE
            }

            // Add to cart
            binding.btnAddToCart.setOnClickListener {
                val qty = 1
                quantities[product.id] = qty
                binding.tvQty.text = qty.toString()

                binding.btnAddToCart.visibility = View.GONE
                binding.qtyLayout.visibility = View.VISIBLE

                onAddToCart(product, qty)
            }

            // PLUS
            binding.btnPlus.setOnClickListener {
                var qty = quantities[product.id] ?: 1
                qty++
                quantities[product.id] = qty

                binding.tvQty.text = qty.toString()
                onUpdateCart(product, qty)
            }

            // MINUS
            binding.btnMinus.setOnClickListener {
                var qty = quantities[product.id] ?: 1

                if (qty > 1) {
                    qty--
                    quantities[product.id] = qty
                    binding.tvQty.text = qty.toString()
                    onUpdateCart(product, qty)
                } else {
                    // Remove completely
                    quantities.remove(product.id)
                    binding.qtyLayout.visibility = View.GONE
                    binding.btnAddToCart.visibility = View.VISIBLE

                    onRemoveFromCart(product)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBuyerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size
}
