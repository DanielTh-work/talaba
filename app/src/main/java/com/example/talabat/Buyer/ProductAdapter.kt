package com.example.talabat.buyer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.talabat.databinding.ItemProductBuyerBinding

class ProductAdapter(
    private val products: List<Product>,
    private val onAddToCart: (Product, Int) -> Unit,
    private val onUpdateCart: (Product, Int) -> Unit,
    private val onRemoveFromCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val quantities = mutableMapOf<String, Int>()  // productId → qty

    inner class ProductViewHolder(val binding: ItemProductBuyerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {

            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = "Price: \$${product.price}"
            binding.tvProductQuantity.text = "Qty: ${product.quantity}"

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

            // Restore previous quantity
            val currentQty = quantities[product.id] ?: 0

            if (currentQty > 0) {
                binding.btnAddToCart.visibility = View.GONE
                binding.qtyLayout.visibility = View.VISIBLE
                binding.tvQty.text = currentQty.toString()
            } else {
                binding.btnAddToCart.visibility = View.VISIBLE
                binding.qtyLayout.visibility = View.GONE
            }

            // ADD TO CART
            binding.btnAddToCart.setOnClickListener {
                val qty = 1
                quantities[product.id] = qty

                binding.tvQty.text = qty.toString()
                binding.btnAddToCart.visibility = View.GONE
                binding.qtyLayout.visibility = View.VISIBLE

                onAddToCart(product, qty)
            }

            // PLUS BUTTON WITH STOCK CHECK
            binding.btnPlus.setOnClickListener {

                var qty = quantities[product.id] ?: 1

                if (qty >= product.quantity) {
                    Toast.makeText(
                        binding.root.context,
                        "Only ${product.quantity} items available",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                qty++
                quantities[product.id] = qty
                binding.tvQty.text = qty.toString()

                onUpdateCart(product, qty)
            }

            // MINUS BUTTON
            binding.btnMinus.setOnClickListener {

                var qty = quantities[product.id] ?: 1

                if (qty > 1) {
                    qty--
                    quantities[product.id] = qty
                    binding.tvQty.text = qty.toString()

                    onUpdateCart(product, qty)
                } else {
                    // remove from cart
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
