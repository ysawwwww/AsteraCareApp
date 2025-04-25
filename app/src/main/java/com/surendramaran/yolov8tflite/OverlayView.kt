package com.surendramaran.yolov8tflite

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // List to hold the bounding boxes of detected objects
    private var results = listOf<BoundingBox>()

    // Paint objects for drawing bounding boxes and text
    private val boxPaint = Paint()
    private val textBackgroundPaint = Paint()
    private val textPaint = Paint()

    // Rect for text bounds calculation
    private val bounds = Rect()

    init {
        initPaints()
    }

    private fun initPaints() {
        // Initialize paint for bounding boxes
        boxPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8f
        boxPaint.style = Paint.Style.STROKE

        // Initialize paint for background of text labels
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        // Initialize paint for text labels
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f
    }

    // Method to set the results (bounding boxes) from the detector
    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()  // Trigger a redraw
    }

    // Method to get the current detection results (for capturing and logging)
    fun getResults(): List<BoundingBox> {
        return results
    }

    // Method to clear the overlay view
    fun clear() {
        results = emptyList()
        invalidate()
    }

    // Draw the bounding boxes and labels on the canvas
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        results.forEach { boundingBox ->
            if (boundingBox.clsName == "Unknown") return@forEach

            val left = boundingBox.x1 * width
            val top = boundingBox.y1 * height
            val right = boundingBox.x2 * width
            val bottom = boundingBox.y2 * height

            // Draw bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint)

            val label = boundingBox.clsName

            // Draw label background
            textBackgroundPaint.getTextBounds(label, 0, label.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()

            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw label text
            canvas.drawText(label, left, top + bounds.height(), textPaint)
        }
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8 // Padding for text background
    }
}