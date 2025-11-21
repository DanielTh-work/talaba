package com.example.talabat.buyer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.talabat.databinding.FragmentVoipCallBinding
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class VoIPCallFragment : Fragment() {

    private var _binding: FragmentVoipCallBinding? = null
    private val binding get() = _binding!!

    // Networking
    private var socket: DatagramSocket? = null
    private var job: Job? = null
    private var isCalling = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoipCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartCall.setOnClickListener { startCall() }
        binding.btnEndCall.setOnClickListener { endCall() }
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun startCall() {
        val targetIp = binding.inputIp.text.toString()
        val targetPortText = binding.inputPort.text.toString()

        // Check if IP or Port are empty
        if (targetIp.isBlank() || targetPortText.isBlank()) {
            Toast.makeText(requireContext(), "You have to enter the data first", Toast.LENGTH_SHORT).show()
            return
        }

        // Safely parse port number
        val targetPort = targetPortText.toIntOrNull()
        if (targetPort == null) {
            Toast.makeText(requireContext(), "Enter a valid port number", Toast.LENGTH_SHORT).show()
            return
        }

        binding.txtStatus.text = "Calling..."
        isCalling = true

        job = CoroutineScope(Dispatchers.IO).launch {

            try {
                // Open socket for sending
                socket = DatagramSocket()
                val targetAddress = InetAddress.getByName(targetIp)

                // Simple signaling packet (0x01 = call request)
                val packetData = byteArrayOf(0x01)
                val packet = DatagramPacket(packetData, packetData.size, targetAddress, targetPort)

                socket?.send(packet)

                withContext(Dispatchers.Main) {
                    binding.txtStatus.text = "Call request sent to $targetIp:$targetPort"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtStatus.text = "Call failed: ${e.message}"
                    Toast.makeText(requireContext(), "Error sending call", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun endCall() {
        isCalling = false
        job?.cancel()
        socket?.close()
        binding.txtStatus.text = "Call Ended"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        endCall()
        _binding = null
    }
}
