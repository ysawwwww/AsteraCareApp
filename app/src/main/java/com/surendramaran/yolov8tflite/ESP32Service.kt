package com.surendramaran.yolov8tflite

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.OutputStream
import java.net.Socket

object ESP32Service {
    fun sendDataToESP32(context: Context, data: String) {
        // Replace with your ESP32 IP address or endpoint
        val esp32Ip = "http://192.168.4.1/send"

        // Use an HTTP request to send data (you can use OkHttp or any HTTP client here)
        // Simulating sending data for now
        Log.d("ESP32Service", "Sending data: $data")

        // Display confirmation
        Toast.makeText(context, "Data sent: $data", Toast.LENGTH_SHORT).show()
    }
}