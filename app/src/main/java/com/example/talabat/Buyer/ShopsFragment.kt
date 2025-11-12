package com.example.talabat.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.talabat.R
import com.example.talabat.databinding.FragmentShopsBinding
import com.google.firebase.database.FirebaseDatabase

class ShopsFragment : Fragment() {

    private lateinit var binding: FragmentShopsBinding
    private val db = FirebaseDatabase.getInstance().reference.child("sellers")
    private val shops = mutableListOf<Shop>()
    private lateinit var adapter: ShopAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShopsBinding.inflate(inflater, container, false)

        binding.recyclerViewShops.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = ShopAdapter(shops) { shop ->
            val fragment = ProductsFragment.newInstance(shop.sellerId ?: shop.shopId ?: "")
            // Use activity's supportFragmentManager
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, fragment) // this is the container in BuyerHomeActivity
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerViewShops.adapter = adapter

        loadShops()
        return binding.root
    }

    private fun loadShops() {
        db.get().addOnSuccessListener { snapshot ->
            shops.clear()
            for (child in snapshot.children) {
                val sellerId = child.key
                val name = child.child("shopName").getValue(String::class.java)
                    ?: child.child("name").getValue(String::class.java)
                val imageUrl = child.child("shopImage").getValue(String::class.java) ?: ""
                shops.add(
                    Shop(
                        shopId = sellerId,
                        name = name,
                        imageUrl = imageUrl,
                        sellerId = sellerId
                    )
                )
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load shops: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
