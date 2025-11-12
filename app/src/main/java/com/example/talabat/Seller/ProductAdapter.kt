package com.example.talabat.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.talabat.databinding.ItemProductBinding

class ProductsAdapter(
    private val products: MutableList<Product>,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = "Price: ${product.price}"
            binding.tvProductQuantity.text = "Qty: ${product.quantity}"

            // Load image if available
            if (!product.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root).load(product.imageUrl).into(binding.imgProduct)
            } else {
                binding.imgProduct.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Options button
            binding.btnOptions.setOnClickListener {
                val popup = PopupMenu(binding.root.context, binding.btnOptions)
                popup.menu.add("Edit").setOnMenuItemClickListener {
                    onEdit(product)
                    true
                }
                popup.menu.add("Delete").setOnMenuItemClickListener {
                    onDelete(product)
                    true
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
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
