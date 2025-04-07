package com.surendramaran.yolov8tflite

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)


        val historyListView: ListView = findViewById(R.id.historyListView)
        val clearHistoryButton: Button = findViewById(R.id.clearHistoryButton)

        val prefs = getSharedPreferences("AsteraCarePrefs", MODE_PRIVATE)
        val historySet = prefs.getStringSet("detectionHistory", setOf()) ?: setOf()
        val historyList = historySet.toList().sortedDescending().toMutableList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyList)
        historyListView.adapter = adapter

        clearHistoryButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all detection history?")
                .setPositiveButton("Yes") { _, _ ->
                    prefs.edit().remove("detectionHistory").apply()
                    historyList.clear()
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}