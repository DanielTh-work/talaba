package com.example.talabat.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.talabat.databinding.FragmentProductsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.talabat.R


class ProductsFragment : Fragment() {

    private lateinit var binding: FragmentProductsBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private val productList = mutableListOf<Product>()
    private lateinit var adapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductsBinding.inflate(inflater, container, false)

        // Initialize adapter
        adapter = ProductsAdapter(
            productList,
            onEdit = { product ->
                val fragment = AddEditProductFragment()
                val bundle = Bundle()
                bundle.putString("productId", product.id ?: "")
                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment) // Ensure fragment_container exists in SellerHomeActivity XML
                    .addToBackStack(null)
                    .commit()
            },
            onDelete = { product ->
                val userUID = auth.currentUser?.uid ?: return@ProductsAdapter
                db.child("sellers").child(userUID).child("products").child(product.id ?: "")
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Product deleted!", Toast.LENGTH_SHORT).show()
                        productList.remove(product)
                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete product", Toast.LENGTH_SHORT).show()
                    }
            }
        )

        // RecyclerView setup
        binding.rvProducts.layoutManager = GridLayoutManager(context, 2)
        binding.rvProducts.adapter = adapter

        // Add new product button
        // Fetch products from Firebase
        fetchProducts()

        return binding.root
    }

    private fun fetchProducts() {
        val userUID = auth.currentUser?.uid ?: return
        db.child("sellers").child(userUID).child("products")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    productList.clear()
                    for (child in snapshot.children) {
                        val product = child.getValue(Product::class.java)
                        if (product != null) {
                            product.id =
                                child.key.toString() // Ensure Product has a nullable id property
                            productList.add(product)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load products", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
