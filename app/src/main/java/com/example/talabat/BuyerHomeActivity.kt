package com.example.talabat

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.buyer.CartFragment
import com.example.talabat.buyer.ShopsFragment
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

        // Set toolbar as ActionBar (very important for menu)
        setSupportActionBar(binding.toolbarBuyer)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Load ShopsFragment by default
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, ShopsFragment())
                .commit()
        }

        // Set welcome message
        val welcomeMessage = "Welcome Buyer!"
        binding.tvWelcomeMessage.text = welcomeMessage

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

        // ---- TEMP: Uncomment this line to test that CartFragment loads ----
        // supportFragmentManager.beginTransaction().replace(R.id.fragment_container_buyer, CartFragment()).commit()
    }

    // Inflate the cart menu in the toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_buyer, menu)
        return true
    }

    // Handle cart icon click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cart -> {
                // Show CartFragment
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_buyer, CartFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
