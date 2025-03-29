package com.surendramaran.yolov8tflite
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
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
import java.io.IOException
import java.util.UUID

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

    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_DISCOVERABLE_BT = 2
//    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var buttonConnectChamber: Button

    private val TAG = "BluetoothServer"
    private val NAME = "BluetoothApp"
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Replace with your own UUID
    private var acceptThread: AcceptThread? = null
    private var communicationThread: CommunicationThread? = null  // Declare at class leve

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


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
        sendManualButton.setOnClickListener {
            val messageToSend = "Manual parameters updated!"
            communicationThread?.write(messageToSend.toByteArray())
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

        // Send a test message to the connected device
        val testMessage = "Hello from Android!"
        communicationThread?.write(testMessage.toByteArray())

    }

    private inner class CommunicationThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream = socket.inputStream
        private val outputStream = socket.outputStream

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                try {
                    // Read incoming data
                    bytes = inputStream.read(buffer)
                    val receivedMessage = String(buffer, 0, bytes)

                    Log.d(TAG, "Received: $receivedMessage")

                    // Update UI with received data
                    runOnUiThread {
                        Toast.makeText(this@HomeActivity, "Received: $receivedMessage", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading data", e)
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
                Log.d(TAG, "Sent: ${String(bytes)}")
            } catch (e: IOException) {
                Log.e(TAG, "Error sending data", e)
            }
        }
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

    private fun connectToToasterACARE() {
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
                        return
                    } catch (e: Exception) {
                        Log.e("Bluetooth", "Error connecting to AsteraCare: ${e.message}")
                        Toast.makeText(this, "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.d("Bluetooth", "AsteraCare device not found in paired devices.")
            Toast.makeText(this, "AsteraCare not paired. Please pair it first.", Toast.LENGTH_SHORT).show()
        }
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