package com.example.talabat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.inputEmail.text.toString()
            val password = binding.inputPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener


                    database.getReference("Buyers").child(userId).get()
                        .addOnSuccessListener { buyerSnapshot ->
                            if (buyerSnapshot.exists()) {
                                Toast.makeText(this, "Welcome Buyer!", Toast.LENGTH_SHORT).show()

                                startActivity(Intent(this, BuyerHomeActivity::class.java))
                                finish()
                            } else {

                                database.getReference("Sellers").child(userId).get()
                                    .addOnSuccessListener { sellerSnapshot ->
                                        if (sellerSnapshot.exists()) {
                                            Toast.makeText(this, "Welcome Seller!", Toast.LENGTH_SHORT).show()

                                            startActivity(Intent(this, SellerHomeActivity::class.java))
                                            finish()
                                        } else {
                                            Toast.makeText(this, "User role not found.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        }
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }


        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}