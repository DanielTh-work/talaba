package com.example.talabat.seller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.talabat.databinding.FragmentShopBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

class ShopFragment : Fragment() {

    private lateinit var binding: FragmentShopBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private var imageUri: Uri? = null
    private var imageUrl: String? = null
    private var shopExists = false

    private lateinit var s3Client: AmazonS3Client
    private lateinit var transferUtility: TransferUtility
    private val PICK_IMAGE_REQUEST = 100

    private val locations = listOf("Nasr City", "Rehab", "5th Settlement", "Masr el gedida", "Shobra", "Maadi")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShopBinding.inflate(inflater, container, false)

        // Initialize Spinner Adapter
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            locations
        )
        binding.spLocation.adapter = adapter

        // Initialize AWS
        try {
            val awsCredentials = BasicAWSCredentials(
                "AKIA6GUTHW7WVCKNRBF4",
                "DPKY9wEnRJSrLv5czCQTzJ42ZjMaw6HoBfAjnEXd"
            )
            s3Client = AmazonS3Client(awsCredentials, Region.getRegion(Regions.EU_NORTH_1))
            s3Client.setEndpoint("s3.eu-north-1.amazonaws.com")

            transferUtility = TransferUtility.builder()
                .context(requireContext())
                .s3Client(s3Client)
                .build()
        } catch (e: Exception) {
            Toast.makeText(context, "AWS init failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        val uid = auth.currentUser!!.uid

        // Back button
        binding.btnBackNav.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Load shop if exists
        db.child("sellers").child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists() && snapshot.child("shopName").exists()) {
                shopExists = true
                val name = snapshot.child("shopName").value.toString()
                val location = snapshot.child("shopLocation").value.toString()
                val photo = snapshot.child("shopImageUrl").getValue(String::class.java)

                binding.etShopName.setText(name)

                val index = locations.indexOf(location)
                if (index != -1) binding.spLocation.setSelection(index)

                binding.btnCreateShop.text = "Update Shop"

                if (!photo.isNullOrEmpty()) {
                    imageUrl = photo
                    Glide.with(this).load(photo).into(binding.ivShopImage)
                }
            }
        }

        binding.btnSelectImage.setOnClickListener { openGallery() }

        binding.btnCreateShop.setOnClickListener {
            val name = binding.etShopName.text.toString().trim()
            val location = binding.spLocation.selectedItem.toString()

            if (name.isEmpty()) {
                Toast.makeText(context, "Please enter shop name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveShop(name, location)
        }

        return binding.root
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            imageUri?.let {
                binding.ivShopImage.setImageURI(it)
                uploadImageToS3(it)
            }
        }
    }

    private fun uploadImageToS3(uri: Uri) {
        try {
            val uid = auth.currentUser!!.uid
            val fileName = "shops/${uid}_${System.currentTimeMillis()}.jpg"
            val file = FileUtil.from(requireContext(), uri)

            val uploadObserver = transferUtility.upload(
                "yalla-eat-bkt",
                fileName,
                file,
                CannedAccessControlList.PublicRead
            )

            uploadObserver.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState?) {
                    if (state == TransferState.COMPLETED) {
                        imageUrl = s3Client.getUrl("yalla-eat-bkt", fileName).toString()
                        Toast.makeText(context, "Image uploaded!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
                override fun onError(id: Int, ex: Exception?) {
                    Toast.makeText(context, "Upload error: ${ex?.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveShop(name: String, location: String) {
        val uid = auth.currentUser!!.uid
        val shopData = hashMapOf(
            "shopName" to name,
            "shopLocation" to location,
            "shopImageUrl" to (imageUrl ?: "")
        )

        val ref = db.child("sellers").child(uid)
        if (shopExists) {
            ref.updateChildren(shopData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(context, "Shop updated!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            ref.setValue(shopData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Shop created!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

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
