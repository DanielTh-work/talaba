package com.example.talabat.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.talabat.databinding.FragmentVoipCallSellerBinding
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class VOIPCallFragmentSeller : Fragment() {

    private var _binding: FragmentVoipCallSellerBinding? = null
    private val binding get() = _binding!!

    private var socket: DatagramSocket? = null
    private var job: Job? = null
    private var isCalling = false

    // Timer for call duration
    private var time = 0
    private var timerJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoipCallSellerBinding.inflate(inflater, container, false)
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
        val targetIp = binding.inputIp.text.toString().trim()
        val targetPortText = binding.inputPort.text.toString().trim()

        if (targetIp.isBlank() || targetPortText.isBlank()) {
            // Show a Toast instead of changing the status TextView
            Toast.makeText(requireContext(), "Please fill in the IP and Port fields", Toast.LENGTH_SHORT).show()
            return
        }

        val targetPort = targetPortText.toIntOrNull()
        if (targetPort == null) {
            Toast.makeText(requireContext(), "Enter a valid port number", Toast.LENGTH_SHORT).show()
            return
        }

        binding.txtStatus.text = "Calling..."
        isCalling = true

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket()
                val targetAddress = InetAddress.getByName(targetIp)

                // Simple signaling packet (0x01 = call request)
                val packetData = byteArrayOf(0x01)
                val packet = DatagramPacket(packetData, packetData.size, targetAddress, targetPort)
                socket?.send(packet)

                withContext(Dispatchers.Main) {
                    binding.txtStatus.text = "Call request sent to $targetIp:$targetPort"
                    startTimer()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to start call: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.txtStatus.text = "Call Failed"
                }
            }
        }
    }


    private fun endCall() {
        isCalling = false
        job?.cancel()
        socket?.close()
        binding.txtStatus.text = "Call Ended"
        stopTimer()
    }

    private fun startTimer() {
        time = 0
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isCalling) {
                delay(1000)
                time++
                val minutes = time / 60
                val seconds = time % 60
                binding.txtCallTimer.text = String.format("%02d:%02d", minutes, seconds)
                binding.txtCallTimer.visibility = View.VISIBLE
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        time = 0
        binding.txtCallTimer.text = "00:00"
        binding.txtCallTimer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        endCall()
        _binding = null
    }
}
