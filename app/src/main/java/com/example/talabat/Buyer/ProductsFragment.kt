package com.example.talabat.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.talabat.databinding.FragmentProductsBinding
import com.google.firebase.database.FirebaseDatabase

class ProductsFragment : Fragment() {

    private lateinit var binding: FragmentProductsBinding
    private val products = mutableListOf<Product>()
    private lateinit var adapter: ProductAdapter
    private var sellerId: String = ""
    private val db = FirebaseDatabase.getInstance().reference

    companion object {
        fun newInstance(sellerId: String): ProductsFragment {
            val fragment = ProductsFragment()
            val args = Bundle()
            args.putString("sellerId", sellerId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductsBinding.inflate(inflater, container, false)

        binding.btnNav.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        sellerId = arguments?.getString("sellerId") ?: ""

        // -----------------------------------
        // ⭐ ProductAdapter with single-seller rule
        // -----------------------------------
        adapter = ProductAdapter(
            products,

            onAddToCart = { product, qty ->

                val productSellerId = product.sellerId

                // Case 1: Cart empty → lock seller + add
                if (CartManager.currentSellerId == null) {
                    CartManager.currentSellerId = productSellerId
                    CartManager.addItem(product, qty)
                    Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
                    adapter.notifyDataSetChanged()
                    return@ProductAdapter
                }

                // Case 2: Same seller → add normally
                if (CartManager.currentSellerId == productSellerId) {
                    CartManager.addItem(product, qty)
                    Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
                    adapter.notifyDataSetChanged()
                    return@ProductAdapter
                }

                // Case 3: Different seller → warning dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Different Shop")
                    .setMessage("Your cart contains items from another shop. Adding this item will clear your cart. Continue?")
                    .setPositiveButton("Yes") { _, _ ->
                        CartManager.clear()
                        CartManager.currentSellerId = productSellerId
                        CartManager.addItem(product, qty)

                        Toast.makeText(requireContext(), "Cart cleared and item added.", Toast.LENGTH_SHORT).show()
                        adapter.notifyDataSetChanged()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        adapter.notifyDataSetChanged() // Reset UI
                    }
                    .show()
            },

            // Update qty
            onUpdateCart = { product, qty ->
                CartManager.updateQuantity(product, qty)
            },

            // Remove
            onRemoveFromCart = { product ->
                CartManager.removeItem(product)
            }
        )

        // RecyclerView setup
        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = adapter

        loadProducts()

        return binding.root
    }

    private fun loadProducts() {
        if (sellerId.isEmpty()) return

        db.child("sellers").child(sellerId).child("products").get()
            .addOnSuccessListener { snapshot ->

                products.clear()

                for (child in snapshot.children) {
                    val p = child.getValue(Product::class.java)

                    if (p != null) {
                        val updated = p.copy(
                            id = child.key ?: "",
                            sellerId = sellerId
                        )
                        products.add(updated)
                    }
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
