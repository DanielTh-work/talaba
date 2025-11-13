package com.example.talabat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.databinding.ActivitySellerHomeBinding
import com.example.talabat.seller.ProductsFragment
import com.example.talabat.seller.ShopFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SellerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellerHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySellerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val userUID = auth.currentUser?.uid

        val welcomeMessage = "Welcome Seller!"
        binding.tvWelcomeMessage.text = welcomeMessage

        // Decide which fragment to show initially
        if (savedInstanceState == null && userUID != null) {
            FirebaseDatabase.getInstance().reference.child("sellers").child(userUID).get()
                .addOnSuccessListener { snapshot ->
                    val fragment = if (snapshot.child("shopName").exists()) {
                        // Shop exists → show products
                        ProductsFragment()
                    } else {
                        // No shop → show shop creation
                        ShopFragment()
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                }
        }

        // Button to edit shop
        binding.btnGoToProfile.setOnClickListener {
            // Navigate to ShopFragment for editing
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ShopFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnManageData.setOnClickListener {
            val intent = Intent(this, SellerProfileActivity::class.java)
            startActivity(intent)
        }
    }
}
