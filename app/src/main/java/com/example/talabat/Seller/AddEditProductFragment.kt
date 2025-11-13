package com.example.talabat.seller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.talabat.databinding.FragmentAddEditProductBinding
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

class AddEditProductFragment : Fragment() {

    private lateinit var binding: FragmentAddEditProductBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private var imageUri: Uri? = null
    private var imageUrl: String? = null
    private var isEditMode = false
    private var productId: String? = null

    private lateinit var s3Client: AmazonS3Client
    private lateinit var transferUtility: TransferUtility
    private val PICK_IMAGE_REQUEST = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddEditProductBinding.inflate(inflater, container, false)

        // --- Back button ---
        binding.btnBackNav.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // AWS Initialization
        try {
            val awsCredentials = BasicAWSCredentials(
                "AKIA6GUTHW7WVCKNRBF4",  // replace with your keys
                "DPKY9wEnRJSrLv5czCQTzJ42ZjMaw6HoBfAjnEXd"
            )
            s3Client = AmazonS3Client(awsCredentials, Region.getRegion(Regions.EU_NORTH_1))
            s3Client.setEndpoint("s3.eu-north-1.amazonaws.com")
            transferUtility = TransferUtility.builder()
                .context(requireContext())
                .s3Client(s3Client)
                .build()
        } catch (e: Exception) {
            Toast.makeText(context, "AWS Initialization Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Check edit mode
        productId = arguments?.getString("productId")
        if (!productId.isNullOrEmpty()) {
            isEditMode = true
            loadProduct(productId!!)
        }

        // Pick image
        binding.btnPickImage.setOnClickListener { openGallery() }

        // Save product
        binding.btnSaveProduct.setOnClickListener {
            if (imageUri != null && imageUrl == null) {
                Toast.makeText(context, "Uploading image, please wait...", Toast.LENGTH_SHORT).show()
            } else {
                saveProduct()
            }
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
                binding.imgProduct.setImageURI(it)
                uploadImageToS3(it)
            }
        }
    }

    private fun uploadImageToS3(uri: Uri) {
        try {
            val uid = auth.currentUser!!.uid
            val fileName = "products/${uid}_${System.currentTimeMillis()}.jpg"
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

    private fun loadProduct(id: String) {
        val uid = auth.currentUser!!.uid
        db.child("sellers").child(uid).child("products").child(id)
            .get().addOnSuccessListener { snapshot ->
                val product = snapshot.getValue(Product::class.java)
                product?.let {
                    binding.etProductName.setText(it.name)
                    binding.etProductPrice.setText(it.price.toString())
                    binding.etProductQuantity.setText(it.quantity.toString())
                    if (!it.imageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(it.imageUrl).into(binding.imgProduct)
                        imageUrl = it.imageUrl
                    }
                }
            }
    }

    private fun saveProduct() {
        val name = binding.etProductName.text.toString().trim()
        val priceText = binding.etProductPrice.text.toString().trim()
        val quantityText = binding.etProductQuantity.text.toString().trim()

        if (name.isEmpty() || priceText.isEmpty() || quantityText.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        val quantity = quantityText.toIntOrNull()
        if (price == null || quantity == null) {
            Toast.makeText(context, "Invalid price or quantity", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser!!.uid
        val productData = hashMapOf(
            "name" to name,
            "price" to price,
            "quantity" to quantity,
            "imageUrl" to (imageUrl ?: "")
        )

        val productsRef = db.child("sellers").child(uid).child("products")
        if (isEditMode) {
            productsRef.child(productId!!).updateChildren(productData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(context, "Product updated!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            productsRef.push().setValue(productData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Product added!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Save failed: ${it.message}", Toast.LENGTH_SHORT).show()
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
