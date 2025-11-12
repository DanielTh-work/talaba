package com.example.talabat.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.talabat.R
import com.example.talabat.databinding.FragmentShopBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ShopFragment : Fragment() {

    private lateinit var binding: FragmentShopBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private var shopExists = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShopBinding.inflate(inflater, container, false)

        val userUID = auth.currentUser!!.uid

        // Check if shop exists
        db.child("sellers").child(userUID).get().addOnSuccessListener { snapshot ->
            if (snapshot.child("shopName").exists()) {
                shopExists = true
                val name = snapshot.child("shopName").getValue(String::class.java) ?: ""
                val location = snapshot.child("shopLocation").getValue(String::class.java) ?: ""
                binding.etShopName.setText(name)
                binding.etShopLocation.setText(location)
                binding.btnCreateShop.text = "Update Shop"
            }
        }

        binding.btnCreateShop.setOnClickListener {
            val name = binding.etShopName.text.toString().trim()
            val location = binding.etShopLocation.text.toString().trim()

            if (name.isEmpty() || location.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val shopData = hashMapOf(
                "shopName" to name,
                "shopLocation" to location
            )

            val ref = db.child("sellers").child(userUID)
            if (shopExists) {
                ref.updateChildren(shopData as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Shop updated!", Toast.LENGTH_SHORT).show()
                        // Navigate to products after update
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, ProductsFragment())
                            .commit()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                ref.setValue(shopData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Shop created!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, ProductsFragment())
                            .commit()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        return binding.root
    }
}
