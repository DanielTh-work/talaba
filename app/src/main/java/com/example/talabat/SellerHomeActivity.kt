package com.example.talabat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.databinding.ActivitySellerHomeBinding
import com.google.firebase.auth.FirebaseAuth

class SellerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellerHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser


        val welcomeMessage = "Welcome Seller!"

        binding.tvWelcomeMessage.text = welcomeMessage


        binding.btnGoToProfile.setOnClickListener {

            val intent = Intent(this, SellerProfileActivity::class.java)
            startActivity(intent)
        }


        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
