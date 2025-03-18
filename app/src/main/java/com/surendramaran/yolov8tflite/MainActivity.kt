package com.surendramaran.yolov8tflite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.surendramaran.yolov8tflite.Constants.LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.MODEL_PATH
import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Button
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.net.Socket


class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector
    private lateinit var captureButton: Button // Capture button variable

    private lateinit var cameraExecutor: ExecutorService

    // Define flower-specific parameters in a map
    private val flowerParameters = mapOf(
        "Sunflower" to FlowerParams(10f, 70f, 100f, 85f, R.drawable.sunflower),
        "Gerbera" to FlowerParams(10f, 90f, 100f, 80f, R.drawable.gerbera),
        "Aster" to FlowerParams(5f, 85f, 100f, 75f, R.drawable.aster),
        "Chrysanthemum" to FlowerParams(3f, 90f, 100f, 78f, R.drawable.chrysanthemum)
    )
    // Define a data class to hold flower parameters
    data class FlowerParams(
        val temperature: Float,
        val humidity: Float,
        val waterLevelFlower: Float,
        val waterLevelStorage: Float,
        val imageResId: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        // Pop-up Elements
        val popupBackground = findViewById<View>(R.id.popupBackground)
        val popupContainer = findViewById<LinearLayout>(R.id.popupContainer)
        val detectedFlowersTextView = findViewById<TextView>(R.id.detectedFlowers)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val closePopup = findViewById<Button>(R.id.closePopup)
        captureButton = findViewById(R.id.captureButton)

        //checks if the app has the permission to use the camera
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        //click listener for capture button
        captureButton.setOnClickListener {
            val boundingBoxes = binding.overlay.getResults()
            if (boundingBoxes.isNotEmpty()) {
                val capturedBitmap = binding.viewFinder.bitmap
                if (capturedBitmap == null) {
                    Toast.makeText(this, "Error capturing image.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (capturedBitmap != null) {
                    Log.d("CaptureButton", "Image captured successfully.")

                    // Get the detected flower name (first detected one)
                    val detectedFlower = boundingBoxes[0].clsName

                    // Retrieve correct flower parameters or fallback to default
                    val params = flowerParameters[detectedFlower] ?: FlowerParams(
                        22f, 60f, 70f, 80f, R.drawable.default_flower
                    )

                    // Send image and correct flower parameters to HomeActivity
                    captureAndSendImage(
                        capturedBitmap,
                        "Automatic", // Default mode
                        params.temperature,
                        params.humidity,
                        params.waterLevelFlower,
                        params.waterLevelStorage,
                        params.imageResId,
                        detectedFlower
                    )

                    showPopup(boundingBoxes.map { it.clsName }) // Show detected flowers in popup
                } else {
                    Log.e("CaptureButton", "Failed to capture image.")
                    Toast.makeText(this, "Error capturing image.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No objects detected.", Toast.LENGTH_SHORT).show()
            }
        }

        closePopup.setOnClickListener { hidePopup() }
        popupBackground.setOnClickListener { hidePopup() }

        //click listener for send button which sends the detected flowers to esp32
        sendButton.setOnClickListener {
            val detectedFlowers = detectedFlowersTextView.text.toString()
            if (detectedFlowers.isNotEmpty()) {
                sendDataToESP32(detectedFlowers)
            }
        }
    }

    private fun captureAndSendImage(
         bitmap: Bitmap,
         mode: String,
         temp: Float,
         humidity: Float,
         waterLevelFlower: Float,
         waterLevelStorage: Float,
         flowerImageResId: Int,
         detectedFlower: String
    ){
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        // Send data to HomeActivity
        val intent = Intent(this, HomeActivity::class.java).apply {
            intent.putExtra("capturedImage", byteArray)
            putExtra("mode", mode)
            putExtra("temperature", temp)
            putExtra("humidity", humidity)
            putExtra("waterLevelFlower", waterLevelFlower)
            putExtra("waterLevelStorage", waterLevelStorage)
            putExtra("flowerImage", flowerImageResId)
            putExtra("detectedFlower", detectedFlower)
        }
        startActivity(intent)
    }

    //sends the detected flowers to esp32 via wifi direct
    private fun sendDataToESP32(data: String) {
        Thread {
            try {
                val socket = Socket("192.168.4.1", 1234)
                val outputStream = socket.getOutputStream()
                outputStream.write((data + "\n").toByteArray())
                outputStream.flush()
                socket.close()
                runOnUiThread {
                    Toast.makeText(this, "Data Sent to ESP32", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to send data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    //popup when capture button is clicked. shows the detected flower
    //and gives the user the option to send the detected flowers to the esp32
    private fun showPopup(detectedFlowers: List<String>) {
        val popupContainer = findViewById<LinearLayout>(R.id.popupContainer)
        val popupBackground = findViewById<View>(R.id.popupBackground)
        val detectedFlowersTextView = findViewById<TextView>(R.id.detectedFlowers)

        val detectedText = if (detectedFlowers.isNotEmpty()) {
            detectedFlowers.joinToString("\n")
        } else {
            "No flowers detected."
        }

        detectedFlowersTextView.text = detectedText
        popupBackground.visibility = View.VISIBLE
        popupContainer.visibility = View.VISIBLE
    }

    private fun hidePopup() {
        findViewById<View>(R.id.popupBackground).visibility = View.GONE
        findViewById<LinearLayout>(R.id.popupContainer).visibility = View.GONE
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    //for debugging purposes. Used to see if the capture button is working properly
    private fun captureAndLogResults() {
        val boundingBoxes = binding.overlay.getResults()
        if (boundingBoxes.isNotEmpty()) {
            for (boundingBox in boundingBoxes) {
                Log.d("CaptureButton", "Detected: ${boundingBox.clsName} at (${boundingBox.x1}, ${boundingBox.y1}) to (${boundingBox.x2}, ${boundingBox.y2})")
            }
        } else {
            Log.d("CaptureButton", "No objects detected.")
        }
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview =  Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.clear()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            // Clear results only if they are not already empty to avoid unnecessary redraws
            if (binding.overlay.getResults().isNotEmpty()) {
                binding.overlay.setResults(emptyList())
                binding.overlay.invalidate() // Redraw only if bounding boxes were not empty
            }
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
            if (boundingBoxes.isNotEmpty()) {
                val detectedFlower = boundingBoxes[0].clsName // Get first detected flower
                val capturedBitmap = binding.viewFinder.bitmap ?: return@runOnUiThread

                // Get flower parameters based on detected flower
                val params = flowerParameters[detectedFlower] ?: FlowerParams(
                    22f, 60f, 70f, 80f, R.drawable.default_flower
                )

                // Send captured image and flower-specific parameters
                captureAndSendImage(
                    capturedBitmap,
                    "Automatic", // Or "Manual" if needed
                    params.temperature,
                    params.humidity,
                    params.waterLevelFlower,
                    params.waterLevelStorage,
                    params.imageResId,
                    detectedFlower
                )
            }
        }
    }

}