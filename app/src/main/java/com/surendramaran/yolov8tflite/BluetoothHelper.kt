//package com.surendramaran.yolov8tflite
//
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothManager
//import android.content.Context
//import android.content.Intent
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//
//class BluetoothHelper(private val activity: AppCompatActivity) {
//    private val bluetoothAdapter: BluetoothAdapter? by lazy {
//        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothManager.adapter
//    }
//
//    fun checkBluetoothSupport(): Boolean {
//        if (bluetoothAdapter == null) {
//            Toast.makeText(activity, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
//            return false
//        }
//        return true
//    }
//
//    fun enableBluetooth(requestCode: Int) {
//        if (bluetoothAdapter?.isEnabled == false) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            activity.startActivityForResult(enableBtIntent, requestCode)
//        }
//    }
//}