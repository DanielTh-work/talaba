package com.example.talabat.Delivery

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.talabat.LoginActivity
import com.example.talabat.databinding.ActivityDeliveryProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DeliveryProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeliveryProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = auth.currentUser!!.uid

        val buyerRef = db.child("Buyers").child(userId)
        val volunteerRef = db.child("DeliveryVolunteers").child(userId)

        // Fetch buyer info
        buyerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.inputName.setText("Name: ${snapshot.child("name").value ?: ""}")
                binding.inputEmail.setText("Email: ${snapshot.child("email").value ?: ""}")
                binding.inputPhone.setText("Phone: ${snapshot.child("phone").value ?: ""}")
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Fetch volunteer stats and set input filter for points
        volunteerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentPoints = snapshot.child("points").getValue(Int::class.java) ?: 0
                val totalOrders = snapshot.child("totalOrders").getValue(Int::class.java) ?: 0
                val balance = snapshot.child("balance").getValue(Int::class.java) ?: 0

                binding.inputPoints.setText("Points: $currentPoints")
                binding.inputOrders.setText("Total Orders: $totalOrders")
                binding.inputBalance.setText("Balance: $balance EGP")

                // Input filter: prevent entering more than current points
                binding.etConvertPoints.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
                    val result = (dest.substring(0, dstart) + source + dest.substring(dend)).toIntOrNull() ?: 0
                    if (result > currentPoints) "" else null
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        binding.btnConvert.setOnClickListener {
            convertPointsToBalance()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ -> logoutDelivery() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logoutDelivery() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun convertPointsToBalance() {
        val userId = auth.currentUser!!.uid
        val pointsToConvert = binding.etConvertPoints.text.toString().toIntOrNull()
        if (pointsToConvert == null || pointsToConvert <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val ref = db.child("DeliveryVolunteers").child(userId)

        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentPoints = currentData.child("points").getValue(Int::class.java) ?: 0
                val currentBalance = currentData.child("balance").getValue(Int::class.java) ?: 0

                return if (pointsToConvert > currentPoints) {
                    // Abort transaction if amount exceeds points
                    Transaction.abort()
                } else {
                    currentData.child("points").value = currentPoints - pointsToConvert
                    currentData.child("balance").value = currentBalance + pointsToConvert
                    Transaction.success(currentData)
                }
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, data: DataSnapshot?) {
                if (committed) {
                    Toast.makeText(this@DeliveryProfileActivity, "Converted successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@DeliveryProfileActivity,
                        "The amount entered exceeds your available points. Please enter a correct amount.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }
}
