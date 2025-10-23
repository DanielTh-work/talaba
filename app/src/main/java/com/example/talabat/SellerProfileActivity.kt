package com.example.talabat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.talabat.databinding.ActivitySellerProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import java.io.File
import java.io.FileOutputStream

class SellerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellerProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    // AWS
    private lateinit var s3Client: AmazonS3Client
    private lateinit var transferUtility: TransferUtility

    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance("https://talabat-cb6d9-default-rtdb.firebaseio.com/")
            .getReference("Sellers")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uid = currentUser.uid

        // --- 1️⃣ Initialize AWS S3 ---
        try {
            val awsCredentials = BasicAWSCredentials(
                "AKIA6GUTHW7WVCKNRBF4",      // ✅ replace with your team keys
                "DPKY9wEnRJSrLv5czCQTzJ42ZjMaw6HoBfAjnEXd"
            )
            s3Client = AmazonS3Client(awsCredentials, Region.getRegion(Regions.EU_NORTH_1))
            s3Client.setEndpoint("s3.eu-north-1.amazonaws.com")
            transferUtility = TransferUtility.builder()
                .context(applicationContext)
                .s3Client(s3Client)
                .build()
        } catch (e: Exception) {
            Toast.makeText(this, "AWS Initialization Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // --- 2️⃣ Load data from Firebase ---
        dbRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: currentUser.email
                    val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)

                    binding.inputName.setText(name)
                    binding.inputPhone.setText(phone)
                    binding.inputEmail.setText(email)

                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@SellerProfileActivity)
                            .load(imageUrl.replace("http://", "https://"))
                            .placeholder(R.drawable.ic_person)
                            .into(binding.profileImage)
                    }
                } else {
                    Toast.makeText(this@SellerProfileActivity, "No profile data found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SellerProfileActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        binding.btnUploadPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // --- 4️⃣ Save updated text fields ---
        binding.btnSave.setOnClickListener {
            val newName = binding.inputName.text.toString().trim()
            val newPhone = binding.inputPhone.text.toString().trim()

            if (newName.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf(
                "name" to newName,
                "phone" to newPhone
            )

            dbRef.child(uid).updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // --- 5️⃣ Back button ---
        binding.btnBack.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    // --- Handle Image Selection ---
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            imageUri?.let {
                binding.profileImage.setImageURI(it)
                uploadImageToS3(it)
            }
        }
    }

    // --- Upload to S3 and Save URL in Firebase (with old photo deletion) ---
    private fun uploadImageToS3(imageUri: Uri) {
        try {
            val uid = FirebaseAuth.getInstance().uid ?: return

            // 1️⃣ Delete old photo if exists
            dbRef.child(uid).child("imageUrl").get().addOnSuccessListener { snapshot ->
                val oldUrl = snapshot.value as? String
                if (!oldUrl.isNullOrEmpty()) {
                    val oldKey = oldUrl.substringAfter("yalla-eat-bkt.s3.eu-north-1.amazonaws.com/")
                    Thread {
                        try {
                            s3Client.deleteObject("yalla-eat-bkt", oldKey)
                        } catch (_: Exception) { }
                    }.start()
                }

                // 2️⃣ Upload new photo
                val fileName = "sellers/${uid}_${System.currentTimeMillis()}.jpg"
                val file = FileUtil.from(this, imageUri)

                val uploadObserver = transferUtility.upload(
                    "yalla-eat-bkt",
                    fileName,
                    file,
                    CannedAccessControlList.PublicRead
                )

                uploadObserver.setTransferListener(object : TransferListener {
                    override fun onStateChanged(id: Int, state: TransferState?) {
                        if (state == TransferState.COMPLETED) {
                            val imageUrl = s3Client.getUrl("yalla-eat-bkt", fileName).toString()
                            saveImageUrlToFirebase(imageUrl)
                        } else if (state == TransferState.FAILED) {
                            Toast.makeText(this@SellerProfileActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
                    override fun onError(id: Int, ex: Exception?) {
                        Toast.makeText(this@SellerProfileActivity, "Error: ${ex?.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageUrlToFirebase(url: String) {
        val uid = FirebaseAuth.getInstance().uid ?: return
        dbRef.child(uid).child("imageUrl").setValue(url)
            .addOnSuccessListener {
                Glide.with(this)
                    .load(url.replace("http://", "https://"))
                    .placeholder(R.drawable.ic_person)
                    .into(binding.profileImage)
                Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving URL: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Helper to convert Uri → File ---
    object FileUtil {
        fun from(context: Context, uri: Uri): File {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            return file
        }
    }
}
