package com.surendramaran.yolov8tflite

import android.os.Bundle
import android.widget.Switch
import android.widget.Button
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class DemoActivity : AppCompatActivity() {

    private lateinit var actuator1Switch: Switch
    private lateinit var actuator2Switch: Switch
    private lateinit var actuator3Switch: Switch
    private lateinit var actuator4Switch: Switch
    private lateinit var activateActuatorsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        actuator1Switch = findViewById(R.id.actuator1Switch)
        actuator2Switch = findViewById(R.id.actuator2Switch)
        actuator3Switch = findViewById(R.id.actuator3Switch)
        actuator4Switch = findViewById(R.id.actuator4Switch)
        activateActuatorsButton = findViewById(R.id.activateActuatorsButton)

        activateActuatorsButton.setOnClickListener {
            // Get switch states
            val peltierState = if (actuator1Switch.isChecked) "1" else "0"
            val mistMakerState = if (actuator2Switch.isChecked) "1" else "0"
            val waterPumpState = if (actuator3Switch.isChecked) "1" else "0"
            val solenoidValveState = if (actuator4Switch.isChecked) "1" else "0"

            val actuatorPayload = "$peltierState$mistMakerState$waterPumpState$solenoidValveState"
            Log.d("DemoActivity", "Sending actuator data: $actuatorPayload")

            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("actuator_data", actuatorPayload)
            startActivity(intent)
        }
    }
}

