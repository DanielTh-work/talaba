package com.example.talabat.Delivery

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.RegisterActivity
import com.example.talabat.databinding.ActivityDeliveryLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DeliveryLogin : AppCompatActivity() {

    private lateinit var binding: ActivityDeliveryLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    // Check if user exists in Buyers
                    database.getReference("Buyers").child(userId).get()
                        .addOnSuccessListener { buyerSnapshot ->
                            if (buyerSnapshot.exists()) {
                                Toast.makeText(this, "Welcome Delivery Volunteer!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, DeliveryHomeActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "User not found as Buyer.", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
