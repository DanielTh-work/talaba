package com.example.talabat

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.buyer.CartFragment
import com.example.talabat.buyer.ShopsFragment
import com.example.talabat.buyer.MyOrdersFragment
import com.example.talabat.databinding.ActivityBuyerHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.talabat.buyer.NotificationHelper

class BuyerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuyerHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBuyerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ⭐ Create notification channel
        NotificationHelper.createChannel(this)

        // ⭐ Ask for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setSupportActionBar(binding.toolbarBuyer)

        auth = FirebaseAuth.getInstance()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, ShopsFragment())
                .commit()
        }

        binding.btnMyOrders.setOnClickListener {
            val fragment = MyOrdersFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.tvWelcomeMessage.text = "Welcome Buyer!"

        binding.btnGoToProfile.setOnClickListener {
            val intent = Intent(this, BuyerProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
        binding.btnVoip.setOnClickListener {
            val intent = Intent(this, VoipActivity::class.java)
            startActivity(intent)
        }


        binding.btnCart.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_buyer, CartFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
