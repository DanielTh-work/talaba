package com.example.talabat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.buyer.ShopsFragment
import com.example.talabat.databinding.ActivityBuyerHomeBinding
import com.google.firebase.auth.FirebaseAuth

class BuyerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuyerHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the binding and set the layout
        binding = ActivityBuyerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Load ShopsFragment into fragment_container_buyer
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
    }
}
