package com.example.talabat.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        // Initialize Adapter
        adapter = ProductAdapter(
            products,
            onAddToCart = { product, qty ->
                CartManager.addItem(product, qty)
                Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show()
            },
            onUpdateCart = { product, qty ->
                CartManager.updateQuantity(product, qty)
            },
            onRemoveFromCart = { product ->
                CartManager.removeItem(product)
            }
        )

        // RecyclerView Setup
        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = adapter

        // Load Products
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
                        val productWithIds = p.copy(
                            id = child.key ?: "",
                            sellerId = sellerId
                        )
                        products.add(productWithIds)
                    }
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
