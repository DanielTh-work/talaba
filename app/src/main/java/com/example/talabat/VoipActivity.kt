package com.example.talabat

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class VoipActivity : AppCompatActivity() {

    private var isCalling = false
    private var micThread: Thread? = null
    private var speakerThread: Thread? = null

    private var sendSocket: DatagramSocket? = null
    private var receiveSocket: DatagramSocket? = null

    private var callStartTime: Long = 0
    private var callEndTime: Long = 0

    // last time we received a packet
    private var lastPacketTime: Long = 0

    private val sampleRate = 16000
    private val minBuf = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voip)

        val ipInput = findViewById<EditText>(R.id.ipInput)
        val portInput = findViewById<EditText>(R.id.portInput)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val btnHangup = findViewById<Button>(R.id.btnHangup)

        checkPermissions()

        btnConnect.setOnClickListener {
            startCall(
                ipInput.text.toString(),
                portInput.text.toString().toInt()
            )
        }

        btnHangup.setOnClickListener { stopCall() }
    }

    private fun formatTime(ms: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(ms))
    }

    private fun startCall(ip: String, port: Int) {
        if (isCalling) return

        isCalling = true
        callStartTime = System.currentTimeMillis()

        val readableStart = formatTime(callStartTime)
        Toast.makeText(this, "Call started at: $readableStart", Toast.LENGTH_SHORT).show()

        val remoteAddr = InetAddress.getByName(ip)
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()

        sendSocket = DatagramSocket()
        receiveSocket = DatagramSocket(port).apply {
            reuseAddress = true
            soTimeout = 2000
        }

        // initialize timeout timer
        lastPacketTime = System.currentTimeMillis()

        // TIMEOUT WATCHER: if no packets for 30 sec, end call
        thread {
            while (isCalling) {
                val elapsed = System.currentTimeMillis() - lastPacketTime
                if (elapsed > 30000) { // 30 seconds
                    runOnUiThread {
                        Toast.makeText(this, "No one connected", Toast.LENGTH_LONG).show()
                    }
                    stopCall()
                    break
                }
                Thread.sleep(1000)
            }
        }

        // 🎤 MIC THREAD — send audio
        micThread = thread {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                runOnUiThread {
                    Toast.makeText(this, "Mic permission denied", Toast.LENGTH_SHORT).show()
                }
                isCalling = false
                return@thread
            }

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf
            )

            val buffer = ByteArray(minBuf * 2)
            recorder.startRecording()

            while (isCalling) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    try {
                        sendSocket?.send(
                            DatagramPacket(buffer, read, remoteAddr, port)
                        )
                    } catch (e: Exception) {
                        Log.e("VoIP", "Send error: $e")
                    }
                }
            }

            recorder.stop()
            recorder.release()
        }

        // 🔊 SPEAKER THREAD — receive audio
        speakerThread = thread {
            val player = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setSampleRate(sampleRate)
                    .build(),
                minBuf * 4,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            player.play()
            val buffer = ByteArray(minBuf * 4)

            while (isCalling) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    receiveSocket?.receive(packet)

                    // reset timeout
                    lastPacketTime = System.currentTimeMillis()

                    Log.d("VoIP", "Received ${packet.length} bytes")
                    player.write(packet.data, 0, packet.length)
                } catch (e: Exception) {
                    Log.e("VoIP", "Receive error: $e")
                }
            }

            player.stop()
            player.release()
        }
    }

    private fun stopCall() {
        if (!isCalling) return

        isCalling = false
        callEndTime = System.currentTimeMillis()

        val readableEnd = formatTime(callEndTime)
        Toast.makeText(this, "Call ended at: $readableEnd", Toast.LENGTH_LONG).show()

        sendSocket?.close()
        receiveSocket?.close()

        val durationSeconds = (callEndTime - callStartTime) / 1000
        Toast.makeText(this, "Call duration: $durationSeconds seconds", Toast.LENGTH_LONG).show()
    }

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty())
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
    }
}
