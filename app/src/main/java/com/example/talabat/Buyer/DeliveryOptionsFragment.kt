package com.example.talabat.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.talabat.databinding.FragmentDeliveryOptionsBinding

class DeliveryOptionsFragment : Fragment() {

    private lateinit var binding: FragmentDeliveryOptionsBinding
    private var deliveryOption = "pickup"
    private var deliveryPrice = 0.0

    private val locations = listOf(
        "Nasr City",
        "Rehab",
        "5th Settlement",
        "Masr el gedida",
        "Shobra",
        "Maadi"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeliveryOptionsBinding.inflate(inflater, container, false)

        setupListeners()
        setupLocationSpinner()

        return binding.root
    }

    private fun setupListeners() {

        binding.radioGroupDelivery.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbDelivery.id) {
                deliveryOption = "delivery"

                binding.spinnerAddress.visibility = View.VISIBLE
                binding.tvDeliveryFee.visibility = View.VISIBLE
                binding.etExactLocation.visibility = View.VISIBLE

                deliveryPrice = calculateDeliveryPrice()
                binding.tvDeliveryFee.text = "Delivery Fee: $deliveryPrice EGP"

            } else {
                deliveryOption = "pickup"

                binding.spinnerAddress.visibility = View.GONE
                binding.tvDeliveryFee.visibility = View.GONE
                binding.etExactLocation.visibility = View.GONE
            }
        }

        binding.btnConfirm.setOnClickListener {
            val address = binding.spinnerAddress.selectedItem?.toString() ?: ""
            val exactLocation = binding.etExactLocation.text.toString().trim()

            if (deliveryOption == "delivery") {
                if (address.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (exactLocation.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter the exact location", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            sendResult(deliveryOption, address, deliveryPrice, exactLocation)
        }
    }

    private fun setupLocationSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, locations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAddress.adapter = adapter

        binding.spinnerAddress.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (deliveryOption == "delivery") {
                    deliveryPrice = calculateDeliveryPrice()
                    binding.tvDeliveryFee.text = "Delivery Fee: $deliveryPrice EGP"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun calculateDeliveryPrice(): Double {
        val selected = binding.spinnerAddress.selectedItem?.toString() ?: ""
        return when (selected) {
            "Nasr City" -> 15.0
            "Rehab" -> 20.0
            "5th Settlement" -> 25.0
            "Masr el gedida" -> 18.0
            "Shobra" -> 22.0
            "Maadi" -> 30.0
            else -> 40.0
        }
    }

    private fun sendResult(option: String, address: String, fee: Double, exactLocation: String) {
        val bundle = Bundle().apply {
            putString("deliveryOption", option)
            putString("deliveryAddress", address)
            putDouble("deliveryPrice", fee)
            putString("deliveryExactLocation", exactLocation)
        }

        parentFragmentManager.setFragmentResult("deliveryData", bundle)
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }
}
