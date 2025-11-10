package com.example.talabat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.btnRegister.setOnClickListener {
            val name = binding.inputName.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val phone = binding.inputPhone.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            val role = binding.roleSpinner.selectedItem.toString() // "Buyer" or "Seller"

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val userMap = mapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "phone" to phone,
                            "role" to role
                        )


                        val userRef = when (role) {
                            "Buyer" -> database.getReference("Buyers").child(uid)
                            "Seller" -> database.getReference("Sellers").child(uid)
                            else -> database.getReference("Users").child(uid)
                        }

                        userRef.setValue(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "$role account created successfully!", Toast.LENGTH_SHORT).show()


                                auth.signOut()


                                val intent = Intent(this, LoginActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
