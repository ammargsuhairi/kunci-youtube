package com.bangkudepan.ytlock

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Lapisan transparan yang menelan semua sentuhan.
 * Cara buka: tahan pojok kiri atas DAN pojok kanan bawah bersamaan selama 2 detik.
 */
class TouchBlockerView(
    context: Context,
    private val onUnlock: () -> Unit
) : FrameLayout(context) {

    private val handler = Handler(Looper.getMainLooper())
    private val zone = 120f * resources.displayMetrics.density
    private val holdMs = 2000L
    private var holding = false

    private val unlockRunnable = Runnable {
        holding = false
        onUnlock()
    }

    init {
        isClickable = true
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelHold()
            else -> {
                val skip = if (e.actionMasked == MotionEvent.ACTION_POINTER_UP) e.actionIndex else -1
                if (cornersHeld(e, skip)) startHold() else cancelHold()
            }
        }
        return true
    }

    private fun cornersHeld(e: MotionEvent, skip: Int): Boolean {
        var topLeft = false
        var bottomRight = false
        for (i in 0 until e.pointerCount) {
            if (i == skip) continue
            val x = e.getX(i)
            val y = e.getY(i)
            if (x < zone && y < zone) topLeft = true
            if (x > width - zone && y > height - zone) bottomRight = true
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
}
