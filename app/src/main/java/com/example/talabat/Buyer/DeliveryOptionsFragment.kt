package com.example.talabat.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import com.example.talabat.databinding.FragmentDeliveryOptionsBinding

class DeliveryOptionsFragment : Fragment() {

    private lateinit var binding: FragmentDeliveryOptionsBinding
    private var deliveryOption = "pickup"
    private var deliveryPrice = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeliveryOptionsBinding.inflate(inflater, container, false)

        setupListeners()

        return binding.root
    }

    private fun setupListeners() {

        // When user picks delivery or pickup
        binding.radioGroupDelivery.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbDelivery.id) {

                deliveryOption = "delivery"
                binding.etAddress.visibility = View.VISIBLE
                binding.tvDeliveryFee.visibility = View.VISIBLE

                deliveryPrice = calculateDeliveryPrice()
                binding.tvDeliveryFee.text = "Delivery Fee: $deliveryPrice EGP"

            } else {
                deliveryOption = "pickup"
                binding.etAddress.visibility = View.GONE
                binding.tvDeliveryFee.visibility = View.GONE
            }
        }

        binding.btnConfirm.setOnClickListener {

            val address = binding.etAddress.text.toString().trim()

            if (deliveryOption == "delivery" && address.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a valid address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendResult(deliveryOption, address, deliveryPrice)
        }
    }

    private fun sendResult(option: String, address: String, fee: Double) {
        val bundle = Bundle().apply {
            putString("deliveryOption", option)
            putString("deliveryAddress", address)
            putDouble("deliveryPrice", fee)
        }

        parentFragmentManager.setFragmentResult("deliveryData", bundle)

        requireActivity().onBackPressedDispatcher.onBackPressed() // go back
    }

    private fun calculateDeliveryPrice(): Double {
        // Random fee example (you can change the logic)
        return (10..30).random().toDouble()
    }
}
