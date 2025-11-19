package com.example.talabat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.buyer.CartFragment
import com.example.talabat.buyer.ShopsFragment
import com.example.talabat.buyer.MyOrdersFragment   // ⭐ FIXED IMPORT
import com.example.talabat.databinding.ActivityBuyerHomeBinding
import com.google.firebase.auth.FirebaseAuth

class BuyerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuyerHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using ViewBinding
        binding = ActivityBuyerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set toolbar as ActionBar
        setSupportActionBar(binding.toolbarBuyer)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Load ShopsFragment by default
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, ShopsFragment())
                .commit()
        }

        // ⭐ My Orders Button
        binding.btnMyOrders.setOnClickListener {
            val fragment = MyOrdersFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Set welcome message
        binding.tvWelcomeMessage.text = "Welcome Buyer!"

        // Go to Buyer Profile
        binding.btnGoToProfile.setOnClickListener {
            val intent = Intent(this, BuyerProfileActivity::class.java)
            startActivity(intent)
        }

        // Logout logic
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        // Cart button (bottom-right) click
        binding.btnCart.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, CartFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
