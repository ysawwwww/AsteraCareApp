package com.surendramaran.yolov8tflite
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    private lateinit var capturedFlowerImage: ImageView
    private lateinit var modeIndicator: TextView
    private lateinit var temperatureValue: TextView
    private lateinit var humidityValue: TextView
    private lateinit var waterLevelFlowerValue: TextView
    private lateinit var waterLevelStorageValue: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Reference UI Elements
        val openCameraButton: Button = findViewById(R.id.openCameraButton)
        capturedFlowerImage = findViewById(R.id.capturedFlowerImage)
        modeIndicator = findViewById(R.id.modeIndicator)
        temperatureValue = findViewById(R.id.temperatureValue)
        humidityValue = findViewById(R.id.humidityValue)
        waterLevelFlowerValue = findViewById(R.id.waterLevelFlowerValue)
        waterLevelStorageValue = findViewById(R.id.waterLevelStorageValue)
        val detectedFlowersTextView: TextView = findViewById(R.id.detectedFlowersTextView)

        val detectedFlower = intent.getStringExtra("detectedFlower") ?: "Unknown Flower"
        detectedFlowersTextView.text = "Detected Flower: $detectedFlower"

        // Receive and display flower-specific image and parameters
        val flowerImageResId = intent.getIntExtra("flowerImage", R.drawable.default_flower)
        capturedFlowerImage.setImageResource(flowerImageResId)


        // Set click listener to open MainActivity
        openCameraButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Receive and display captured image if available
        val byteArray = intent.getByteArrayExtra("capturedImage")
        val detectedFlowers = intent.getStringExtra("detectedFlowers")
        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            capturedFlowerImage.setImageBitmap(bitmap)
        }

        // Receive and display parameter values
        val mode = intent.getStringExtra("mode") ?: "Unknown"
        val temperature = intent.getFloatExtra("temperature", -1f)
        val humidity = intent.getFloatExtra("humidity", -1f)
        val waterLevelFlower = intent.getFloatExtra("waterLevelFlower", -1f)
        val waterLevelStorage = intent.getFloatExtra("waterLevelStorage", -1f)

        // Update TextViews with received values
        modeIndicator.text = "Mode: $mode"
        temperatureValue.text = "Temperature: ${if (temperature >= 0) "$temperature°C" else "--°C"}"
        humidityValue.text = "Humidity: ${if (humidity >= 0) "$humidity%" else "--%"}"
        waterLevelFlowerValue.text = "Flower Water Level: ${if (waterLevelFlower >= 0) "$waterLevelFlower%" else "--%"}"
        waterLevelStorageValue.text = "Storage Water Level: ${if (waterLevelStorage >= 0) "$waterLevelStorage%" else "--%"}"
    }
}