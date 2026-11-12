package com.bangkudepan.ytlock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Lapisan transparan yang menelan semua sentuhan.
 * Buka kunci: tahan area kiri atas DAN area kanan bawah bersamaan selama 2,5 detik.
 * Kedua area sengaja digeser ke dalam dari tepi layar supaya tidak bentrok dengan gestur sistem.
 */
class TouchBlockerView(
    context: Context,
    private val onUnlock: () -> Unit
) : FrameLayout(context) {

    private val handler = Handler(Looper.getMainLooper())
    private val density = resources.displayMetrics.density
    private val inset = 40f * density
    private val size = 170f * density
    private val holdMs = 2500L
    private var holding = false

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 255, 255, 255)
    }

    var indicatorVisible: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val unlockRunnable = Runnable {
        holding = false
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onUnlock()
    }

    init {
        isClickable = true
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (indicatorVisible) {
            canvas.drawCircle(16f * density, height - 30f * density, 3f * density, dotPaint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelHold()
            else -> {
                val skip = if (e.actionMasked == MotionEvent.ACTION_POINTER_UP) e.actionIndex else -1
                if (zonesHeld(e, skip)) startHold() else cancelHold()
            }
        }
        return true
    }

    private fun zonesHeld(e: MotionEvent, skip: Int): Boolean {
        var topLeft = false
        var bottomRight = false
        val w = width.toFloat()
        val h = height.toFloat()
        for (i in 0 until e.pointerCount) {
            if (i == skip) continue
            val x = e.getX(i)
            val y = e.getY(i)
            if (x >= inset && x <= inset + size && y >= inset && y <= inset + size) topLeft = true
            if (x >= w - inset - size && x <= w - inset && y >= h - inset - size && y <= h - inset) bottomRight = true
        }
        return topLeft && bottomRight
    }

    private fun startHold() {
        if (holding) return
        holding = true
        handler.postDelayed(unlockRunnable, holdMs)
    }

    fun cancelHold() {
        holding = false
        handler.removeCallbacks(unlockRunnable)
    }

    override fun onDetachedFromWindow() {
        cancelHold()
        super.onDetachedFromWindow()
    }
}
