package com.surendramaran.yolov8tflite
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class HomeActivity : AppCompatActivity() {
    private lateinit var capturedFlowerImage: ImageView
    private lateinit var modeIndicator: TextView
    private lateinit var temperatureValue: TextView
    private lateinit var humidityValue: TextView
    private lateinit var waterLevelFlowerValue: TextView
    private lateinit var waterLevelStorageValue: TextView
    private var currentTemperature = -1f
    private var currentHumidity = -1f
    private var currentWaterLevelFlower = -1f
    private var currentWaterLevelStorage = -1f
    private var currentDetectedFlower = "Unknown"
    private lateinit var modeToggleButton: Button
    private var isManualMode = false
    private lateinit var manualModeLayout: LinearLayout
    private lateinit var sendManualButton: Button
    private lateinit var editParametersButton: Button
    private var tempInput = 0f
    private var humidityInput = 0f
    private var waterLevelFlowerValueInput = 0f
    private var waterLevelStorageInput = 0f
    private val REQUEST_ENABLE_BT = 1
    private lateinit var buttonConnectChamber: Button
    private val TAG = "BluetoothServer"
    private val NAME = "BluetoothApp"
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Replace with your own UUID
    private var acceptThread: AcceptThread? = null
    private var communicationThread: CommunicationThread? = null  // Declare at class leve
    private lateinit var waterLevelLowIcon: ImageView
    private lateinit var waterLevelStorageCard: LinearLayout

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device?.name ?: "Unknown"
                val deviceAddress = device?.address // MAC Address
                Log.d("Bluetooth", "Discovered Device: $deviceName - $deviceAddress")
            }
        }
    }
    
    // Move the Companion Object **Outside** the CommunicationThread class
    private companion object {
        private const val TAG = "CommunicationThread"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, arrayOf(
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), 1001)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1002)
            }
        }

        waterLevelLowIcon = findViewById(R.id.waterLevelLowIcon)
        waterLevelStorageCard = findViewById(R.id.waterLevelStorageCard)

        // Example of displaying the in-app alert
        if (currentWaterLevelStorage < 20) {  // Example threshold for low water level
            waterLevelLowIcon.visibility = View.VISIBLE
        }

        waterLevelLowIcon.setOnClickListener {
            // Inflate the custom dialog layout
            val dialogView = layoutInflater.inflate(R.layout.water_level_warning_dialog, null)

            // Build the alert dialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)  // Set the custom layout
                .create()

            // Find the button in the custom layout and set a click listener
            dialogView.findViewById<Button>(R.id.btnGotItWaterLevel).setOnClickListener {
                dialog.dismiss()  // Close the dialog when "Got it" is clicked
            }

            // Show the dialog
            dialog.show()
        }

        val waterLevelStorageCard: LinearLayout = findViewById(R.id.waterLevelStorageCard)
        if (currentWaterLevelStorage < 20) {
            waterLevelStorageCard.setBackgroundResource(R.drawable.parameter_card_red)
            waterLevelLowIcon.visibility = View.VISIBLE
        } else {
            waterLevelStorageCard.setBackgroundResource(R.drawable.parameter_card)
            waterLevelLowIcon.visibility = View.GONE
        }

        val prefs = getSharedPreferences("AsteraCarePrefs", MODE_PRIVATE)
        val hasSeenDialog = prefs.getBoolean("hasSeenWelcome", false)

        if (!hasSeenDialog) {
            showWelcomeDialog()
            prefs.edit().putBoolean("hasSeenWelcome", true).apply()
        }

        val helpIcon = findViewById<ImageView>(R.id.helpIcon)
        helpIcon.setOnClickListener {
            showWelcomeDialog()
        }

        // Initialize Bluetooth
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        buttonConnectChamber = findViewById(R.id.buttonConnectChamber)

        buttonConnectChamber.setOnClickListener {
            Log.d("Bluetooth", "Connect Chamber button clicked")
            checkAndEnableBluetooth()
        }

        // Check if the device supports Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show()
        } else {
            checkAndEnableBluetooth()
        }

        startBluetoothServer()

        // Register receiver for device discovery
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

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
        val flowerImageResId = intent.getIntExtra("flowerImage", R.drawable.asteracare_logo)
        capturedFlowerImage.setImageResource(flowerImageResId)

        // Set click listener to open MainActivity
        openCameraButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        //        putting dis here just to be sure lolol
            connectToToasterACARE()
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

        currentTemperature = intent.getFloatExtra("temperature", -1f)
        currentHumidity = intent.getFloatExtra("humidity", -1f)
        currentWaterLevelFlower = intent.getFloatExtra("waterLevelFlower", -1f)
        currentWaterLevelStorage = intent.getFloatExtra("waterLevelStorage", -1f)
        currentDetectedFlower = intent.getStringExtra("detectedFlower") ?: "Unknown Flower"

        // Update TextViews with received values
        modeIndicator.text = "Mode: $mode"
        temperatureValue.text = "Temperature: ${if (temperature >= 0) "$temperature°C" else "--°C"}"
        humidityValue.text = "Humidity: ${if (humidity >= 0) "$humidity%" else "--%"}"
        waterLevelFlowerValue.text = "Flower Water Level: ${if (waterLevelFlower >= 0) "$waterLevelFlower%" else "--%"}"

        communicationThread?.write("FLOWER:$currentDetectedFlower\n".toByteArray())

        modeToggleButton = findViewById(R.id.modeToggleButton)
        manualModeLayout = findViewById(R.id.manualModeLayout)

        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_parameters, null)
        sendManualButton = dialogView.findViewById<Button>(R.id.sendManualButton)

        modeToggleButton.setOnClickListener {
            showModeConfirmationDialog()
        }

        editParametersButton = findViewById(R.id.editParametersButton)
        editParametersButton.setOnClickListener {
            showManualModeForm()
        }
        if (detectedFlower == "Unknown Flower") {
            modeToggleButton.visibility = View.GONE
            editParametersButton.visibility = View.GONE
            modeIndicator.visibility = View.GONE
        } else {
            modeToggleButton.visibility = View.VISIBLE
            modeIndicator.visibility = View.VISIBLE
            if (isManualMode) {
                editParametersButton.visibility = View.VISIBLE
            }
        }
    }

    private fun startBluetoothServer() {
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    private inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    Log.d(TAG, "Waiting for connection...")
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }

                socket?.also {
                    Log.d(TAG, "Device connected: ${it.remoteDevice.name}")
                    manageConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        Log.d(TAG, "Connected to device: ${socket.remoteDevice.name}")
        // Start a separate thread for communication
        communicationThread = CommunicationThread(socket) // Pass the socket here
        communicationThread?.start()
//        // Send a test message to the connected device
//        val testMessage = "ASTERACARE LETS GOOOOOOOOOOOOOOO!"
//        communicationThread?.write(testMessage.toByteArray())
    }

    private inner class CommunicationThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private val buffer = ByteArray(1024)
        private var isRunning = true

        override fun run() {
            while (isRunning) {
                try {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val receivedMessage = String(buffer, 0, bytes).trim()
                        Log.d(TAG, "Received: $receivedMessage")

                        if (receivedMessage.startsWith("STORAGE_WATER_LEVEL_LOW:")) {
                            val parsed = parseWaterLevel(receivedMessage)
                            if (parsed != "--") {
                                runOnUiThread {
                                    waterLevelStorageValue.text = "Storage Water Level: $parsed%"
                                    if (parsed.toFloat() < 20) {
                                        waterLevelStorageCard.setBackgroundResource(R.drawable.parameter_card_red)
                                        waterLevelLowIcon.visibility = View.VISIBLE
                                        showWaterLevelLowNotification(applicationContext)
                                    } else {
                                        waterLevelStorageCard.setBackgroundResource(R.drawable.parameter_card)
                                        waterLevelLowIcon.visibility = View.GONE
                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, "Ignored irrelevant or incomplete data: $receivedMessage")
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading data", e)
                    isRunning = false
                    break
                }
            }
        }
        // Parsing function specifically for storage water level
        private fun parseWaterLevel(data: String): String {
            return data.substringAfter("STORAGE_WATER_LEVEL_LOW:")
                .replace("%", "")
                .toFloatOrNull()?.let {
                    String.format("%.2f", it)
                } ?: "--"
        }
//        call to send data to remote device
        fun write(message: ByteArray) {
            try {
                Log.d("Bluetooth", "Writing to outputStream: ${String(message)}")
                outputStream.write(message)
                outputStream.flush()
                Log.d("Bluetooth", "Message sent successfully")
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error sending data", e)
            }
        }
//        call to shut down connection
        fun cancel() {
            try {
                isRunning = false
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing socket", e)
            }
        }
    }

    private fun showWaterLevelLowNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("low_water_level_channel", "Water Level Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "low_water_level_channel")
            .setContentTitle("Water Level Alert")
            .setContentText("The storage water level is low. Please refill the water storage!")
            .setSmallIcon(R.drawable.water_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        acceptThread?.cancel()
    }

    private fun getPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceAddress = device.address // MAC Address
            Log.d("Bluetooth", "Paired Device: $deviceName - $deviceAddress")
        }
    }

    private fun startDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
        bluetoothAdapter?.startDiscovery()
    }

    private fun checkAndEnableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        bluetoothAdapter?.let {
            if (!it.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        } ?: run {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
        }

        // Start discovering devices
        if (bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter!!.cancelDiscovery()
        }
        bluetoothAdapter!!.startDiscovery()

        // Register receiver for discovery results
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        // Try connecting to "ToasterACARE"
        connectToToasterACARE()
    }

    fun connectToToasterACARE() {
        bluetoothAdapter?.cancelDiscovery() // Stop discovery before connecting
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (!pairedDevices.isNullOrEmpty()) {
            for (device in pairedDevices) {
                if (device.name == "AsteraCare") {
                    Log.d("Bluetooth", "Attempting to connect to AsteraCare (${device.address})")
                    try {
                        val uuid = device.uuids?.firstOrNull()?.uuid ?: MY_UUID
                        Log.d("Bluetooth", "Using UUID: $uuid") // Log the UUID
                        val socket = device.createRfcommSocketToServiceRecord(uuid)
                        Log.d("Bluetooth", "Connecting to socket...")
                        socket.connect()
                        Log.d("Bluetooth", "Successfully connected to AsteraCare")
                        Toast.makeText(this, "Connected to AsteraCare", Toast.LENGTH_SHORT).show()

                        // Call manageConnectedSocket() - initiliaze communication thread
                        manageConnectedSocket(socket)

                        return
                    } catch (e: Exception) {
                        Log.e("Bluetooth", "Error connecting to AsteraCare: ${e.message}")
                       Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()

                        // Prompt user to connect to the chamber if connection fails
                        promptToConnectToChamber()
                    }
                }
            }
        } else {
            Log.d("Bluetooth", "AsteraCare device not found in paired devices.")
            Toast.makeText(this, "AsteraCare not paired. Please pair it first.", Toast.LENGTH_SHORT).show()
        }
    }

    fun promptToConnectToChamber() {
        // Show an AlertDialog to prompt the user to connect the chamber
        val dialog = AlertDialog.Builder(this)
            .setTitle("Chamber Connection Required")
            .setMessage("Please connect app to the chamber via Bluetooth. Click Connect to Chamber button.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // Optionally, trigger the pairing process or discovery again if needed
                startDiscovery()
            }
            .setCancelable(false) // Prevent dismissing without action
            .show()

        Handler(Looper.getMainLooper()).postDelayed({
            // Access the title and change the color
            val titleTextView = dialog.findViewById<TextView>(android.R.id.title)
            titleTextView?.setTextColor(Color.parseColor("#003421")) // Replace with your desired color

            // Access and change the message text color
            val messageTextView = dialog.findViewById<TextView>(android.R.id.message)
            messageTextView?.setTextColor(Color.parseColor("#003421")) // Replace with your desired color

            // Access and change the positive button text color
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(Color.parseColor("#003421")) // Replace with your desired color
        }, 100) // Delay of 100 ms to ensure dialog initialization
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                // Start discovery only after Bluetooth is enabled
                startDiscovery()
                connectToToasterACARE()
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
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
        // Sync values from auto mode to manual inputs
        tempInput = currentTemperature.toFloat()
        humidityInput = currentHumidity.toFloat()
        waterLevelFlowerValueInput = currentWaterLevelFlower.toFloat()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_parameters, null, false)
        // Temperature Increment/Decrement
        val temperatureValue = dialogView.findViewById<TextView>(R.id.tempValue)
        val decreaseTemp = dialogView.findViewById<Button>(R.id.decreaseTemp)
        val increaseTemp = dialogView.findViewById<Button>(R.id.increaseTemp)
        // Humidity Increment/Decrement
        val humidityValue = dialogView.findViewById<TextView>(R.id.humidityValue)
        val decreaseHumidity = dialogView.findViewById<Button>(R.id.decreaseHumidity)
        val increaseHumidity = dialogView.findViewById<Button>(R.id.increaseHumidity)
        // Flower Water Level Increment/Decrement
        val waterLevelFlowerValue = dialogView.findViewById<TextView>(R.id.waterLevelFlowerValue)
        val decreaseWaterLevelFlowerValue = dialogView.findViewById<Button>(R.id.decreasewaterLevelFlowerValue)
        val increaseWaterLevelFlowerValue = dialogView.findViewById<Button>(R.id.increasewaterLevelFlowerValue)
        // Temperature controls
        // Display current values
        temperatureValue.text = "$tempInput°C"
        humidityValue.text = "$humidityInput%"
        waterLevelFlowerValue.text = "$waterLevelFlowerValueInput%"
        decreaseTemp.setOnClickListener {
            if (tempInput > 0) {
                tempInput -= 1
                temperatureValue.text = "$tempInput°C"
            }
        }
        increaseTemp.setOnClickListener {
            if (tempInput < 50) { // Max range 50°C
                tempInput += 1
                temperatureValue.text = "$tempInput°C"
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
        // Flower water level
        decreaseWaterLevelFlowerValue.setOnClickListener {
            if (waterLevelFlowerValueInput > 0) {
                waterLevelFlowerValueInput -= 1
                waterLevelFlowerValue.text = "$waterLevelFlowerValueInput%"
            }
        }
        increaseWaterLevelFlowerValue.setOnClickListener {
            if (waterLevelFlowerValueInput < 100) { // Max 100%
                waterLevelFlowerValueInput += 1
                waterLevelFlowerValue.text = "$waterLevelFlowerValueInput%"
            }
        }

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        val sendManualButton = dialogView.findViewById<Button>(R.id.sendManualButton)

        sendManualButton.setOnClickListener {
            // Get the updated values before sending
            val messageToSend = "TEMPERATURE:$tempInput,HUMIDITY:$humidityInput,WATER_FLOWER:$waterLevelFlowerValueInput"
            currentTemperature = tempInput
            currentHumidity = humidityInput
            currentWaterLevelFlower = waterLevelFlowerValueInput

            if (communicationThread != null) {
                Log.d("Bluetooth", "Sending message: $messageToSend")
                communicationThread?.write(messageToSend.toByteArray())
                Toast.makeText(this, "Parameters successfully sent to chamber!", Toast.LENGTH_SHORT).show()

                // Pass values back to `HomeActivity`
                updateHomeScreen(tempInput, humidityInput, waterLevelFlowerValueInput)
                Log.d("update home screen", "Updated: $tempInput, $humidityInput, $waterLevelFlowerValueInput")

            } else {
                Log.e("Bluetooth", "Communication thread is not initialized!")
            }
            alertDialog.dismiss()
        }
    }
    private fun updateHomeScreen(temp: Float, humidity: Float, waterLevel: Float) {
        temperatureValue.text = "Temperature: $temp°C"
        humidityValue.text = "Humidity: $humidity%"
        waterLevelFlowerValue.text = "Flower Water Level: $waterLevel%"
    }
    private fun showWelcomeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.welcome_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val gotItButton = dialogView.findViewById<Button>(R.id.btnGotIt)
        gotItButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}