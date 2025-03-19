package com.surendramaran.yolov8tflite
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    private lateinit var capturedFlowerImage: ImageView
    private lateinit var modeIndicator: TextView
    private lateinit var temperatureValue: TextView
    private lateinit var humidityValue: TextView
    private lateinit var waterLevelFlowerValue: TextView
    private lateinit var waterLevelStorageValue: TextView
    private lateinit var modeToggleButton: Button
    private var isManualMode = false
    private lateinit var manualModeLayout: LinearLayout
    private lateinit var sendManualButton: Button
    private lateinit var editParametersButton: Button
    private var tempInput = 0f
    private var humidityInput = 0f
    private var waterLevelFlowerInput = 0f
    private var waterLevelStorageInput = 0f


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

        modeToggleButton = findViewById(R.id.modeToggleButton)
        manualModeLayout = findViewById(R.id.manualModeLayout)
        sendManualButton = findViewById(R.id.sendManualButton)

        modeToggleButton.setOnClickListener {
            showModeConfirmationDialog()
        }

        editParametersButton = findViewById(R.id.editParametersButton)
        editParametersButton.setOnClickListener {
            showManualModeForm()
        }

    }

    private fun showModeConfirmationDialog() {
        val newMode = if (isManualMode) "Automatic" else "Manual"
        val message = "Are you sure you want to switch to $newMode mode?"

        AlertDialog.Builder(this)
            .setTitle("Confirm Mode Change")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                switchMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun switchMode() {
        if (isManualMode) {
            // Switch to Automatic Mode
            isManualMode = false
            modeIndicator.text = "Mode: Automatic"
            modeToggleButton.text = "Switch to Manual"
            editParametersButton.visibility = View.GONE
        } else {
            // Switch to Manual Mode
            isManualMode = true
            modeIndicator.text = "Mode: Manual"
            modeToggleButton.text = "Switch to Automatic"
            editParametersButton.visibility = View.VISIBLE
            showManualModeForm() // Open form immediately when switching to manual
        }

        Toast.makeText(this, "Mode switched to ${if (isManualMode) "Manual" else "Automatic"}", Toast.LENGTH_SHORT).show()
    }
    private fun showManualModeForm() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_parameters, null, false)

        // Temperature Increment/Decrement
        val tempValue = dialogView.findViewById<TextView>(R.id.tempValue)
        val decreaseTemp = dialogView.findViewById<Button>(R.id.decreaseTemp)
        val increaseTemp = dialogView.findViewById<Button>(R.id.increaseTemp)

        // Humidity Increment/Decrement
        val humidityValue = dialogView.findViewById<TextView>(R.id.humidityValue)
        val decreaseHumidity = dialogView.findViewById<Button>(R.id.decreaseHumidity)
        val increaseHumidity = dialogView.findViewById<Button>(R.id.increaseHumidity)

        // Flower Water Level Increment/Decrement
        val waterLevelFlowerValue = dialogView.findViewById<TextView>(R.id.waterLevelFlowerValue)
        val decreasewaterLevelFlowerValue = dialogView.findViewById<Button>(R.id.decreasewaterLevelFlowerValue)
        val increasewaterLevelFlowerValue = dialogView.findViewById<Button>(R.id.increasewaterLevelFlowerValue)

        // Initial values
        var tempInput = 25f
        var humidityInput = 50f
        var waterLevelFlowerValueInput = 50f


        // Temperature controls
        decreaseTemp.setOnClickListener {
            if (tempInput > 0) {
                tempInput -= 1
                tempValue.text = "$tempInput°C"
            }
        }

        increaseTemp.setOnClickListener {
            if (tempInput < 50) { // Max range 50°C
                tempInput += 1
                tempValue.text = "$tempInput°C"
            }
        }

        // Humidity controls
        decreaseHumidity.setOnClickListener {
            if (humidityInput > 0) {
                humidityInput -= 1
                humidityValue.text = "$humidityInput%"
            }
        }

        increaseHumidity.setOnClickListener {
            if (humidityInput < 100) { // Max 100%
                humidityInput += 1
                humidityValue.text = "$humidityInput%"
            }
        }

        // flower water level
        decreasewaterLevelFlowerValue.setOnClickListener {
            if (waterLevelFlowerValueInput > 0) {
                waterLevelFlowerValueInput -= 1
                waterLevelFlowerValue.text = "$waterLevelFlowerValueInput%"
            }
        }

        increasewaterLevelFlowerValue.setOnClickListener {
            if (waterLevelFlowerValueInput < 100) { // Max 100%
                waterLevelFlowerValueInput += 1
                waterLevelFlowerValue.text = "$waterLevelFlowerValueInput%"
            }
        }


        // Create dialog
        AlertDialog.Builder(this)
            .setTitle("Edit Chamber Parameters")
            .setView(dialogView)
            .setPositiveButton("Send to Chamber") { _, _ ->
                saveManualParameters(tempInput, humidityInput, waterLevelFlowerValueInput, 60f) // Set dummy values for water level for now
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun saveManualParameters(temp: Float, humidity: Float, waterLevelFlower: Float, waterLevelStorage: Float) {
        tempInput = temp
        humidityInput = humidity
        waterLevelFlowerInput = waterLevelFlower
        waterLevelStorageInput = waterLevelStorage

        // Update UI with new values
        temperatureValue.text = "Temperature: ${if (temp > 0) "$temp°C" else "--°C"}"
        humidityValue.text = "Humidity: ${if (humidity > 0) "$humidity%" else "--%"}"
        waterLevelFlowerValue.text = "Flower Water Level: ${if (waterLevelFlower > 0) "$waterLevelFlower%" else "--%"}"
        waterLevelStorageValue.text = "Storage Water Level: ${if (waterLevelStorage > 0) "$waterLevelStorage%" else "--%"}"

        Toast.makeText(this, "Parameters updated successfully", Toast.LENGTH_SHORT).show()
    }

}