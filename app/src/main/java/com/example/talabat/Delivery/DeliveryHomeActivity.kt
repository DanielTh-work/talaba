package com.example.talabat.Delivery

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.talabat.databinding.ActivityDeliveryHomeBinding
import com.example.talabat.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DeliveryHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeliveryHomeBinding
    private val orders = mutableListOf<Order>()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = auth.currentUser!!.uid
        val buyerRef = db.child("Buyers").child(userId)
        val volunteerRef = db.child("DeliveryVolunteers").child(userId)

// Check if volunteer entry exists
        volunteerRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Fetch buyer data
                buyerRef.get().addOnSuccessListener { buyerSnap ->
                    val name = buyerSnap.child("name").value.toString()
                    val email = buyerSnap.child("email").value.toString()
                    val phone = buyerSnap.child("phone").value.toString()

                    // Create volunteer profile
                    val volunteerData = mapOf(
                        "points" to 0,
                        "totalOrders" to 0,
                        "balance" to 0
                    )

                    volunteerRef.setValue(volunteerData)
                }
            }
        }

        binding = ActivityDeliveryHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerOrders.layoutManager = LinearLayoutManager(this)
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, DeliveryProfileActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            finish()
        }


        loadPendingOrders()
    }

    private fun loadPendingOrders() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.child("orders")
            .orderByChild("status")
            .equalTo("waiting for volunteer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    orders.clear()
                    for (child in snapshot.children) {
                        val order = child.getValue(Order::class.java)
                        if (order != null) {
                            // Filter out orders where the delivery guy is the buyer
                            if (order.buyerId != currentUserId) {
                                orders.add(order)
                            }
                        }
                    }
                    binding.recyclerOrders.adapter = DeliveryOrdersAdapter(orders) { order ->
                        takeOrder(order)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun takeOrder(order: Order) {
        val userId = auth.currentUser?.uid ?: return

        if (!order.deliveryVolunteerId.isNullOrEmpty()) {
            Toast.makeText(this, "Order already taken", Toast.LENGTH_SHORT).show()
            return
        }

        val volunteerRef = db.child("DeliveryVolunteers").child(userId)

        // Update order entry
        val updates = mapOf(
            "deliveryVolunteerId" to userId,
            "status" to "delivered"
        )

        db.child("orders").child(order.orderId).updateChildren(updates)
            .addOnSuccessListener {
                // Add points
                volunteerRef.child("points").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val currentPoints = currentData.getValue(Int::class.java) ?: 0
                        currentData.value = currentPoints + order.rewardPoints
                        return Transaction.success(currentData)
                    }
                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
                })

                // Increment total orders + apply bonus if multiple of 5
                volunteerRef.child("totalOrders").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val currentOrders = currentData.getValue(Int::class.java) ?: 0
                        val newOrders = currentOrders + 1
                        currentData.value = newOrders
                        return Transaction.success(currentData)
                    }
                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        val newOrders = snapshot?.getValue(Int::class.java) ?: 0
                        if (newOrders % 5 == 0) {
                            // Bonus points
                            volunteerRef.child("points").runTransaction(object : Transaction.Handler {
                                override fun doTransaction(curData: MutableData): Transaction.Result {
                                    val pts = curData.getValue(Int::class.java) ?: 0
                                    curData.value = pts + 30
                                    return Transaction.success(curData)
                                }
                                override fun onComplete(err: DatabaseError?, committed: Boolean, snap: DataSnapshot?) {
                                    Toast.makeText(this@DeliveryHomeActivity, "Congrats! +30 Bonus Points!", Toast.LENGTH_LONG).show()
                                }
                            })
                        }
                    }
                })

                Toast.makeText(this, "Order taken! Good luck!", Toast.LENGTH_SHORT).show()
            }
    }

}
