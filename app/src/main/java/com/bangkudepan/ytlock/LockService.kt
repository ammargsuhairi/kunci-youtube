package com.bangkudepan.ytlock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class LockService : AccessibilityService() {

    private val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var overlay: TouchBlockerView? = null

    private val prefs by lazy { getSharedPreferences("prefs", MODE_PRIVATE) }

    // true kalau orang tua sudah membuka kunci; reset saat keluar dari YouTube
    private var unlockedSession = false
    private var lastTarget = "com.google.android.youtube"
    private var lastReturnAt = 0L

    // Paket yang diabaikan supaya status kunci tidak berubah (keyboard, telepon masuk)
    private val ignoredHints = listOf("inputmethod", "keyboard", "dialer", "incallui", "telecom", "telephony")

    private val targets = setOf(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.kids"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Abaikan panel notifikasi, keyboard, dan telepon masuk
        if (pkg == "com.android.systemui" || ignoredHints.any { pkg.contains(it) }) return

        if (pkg in targets) {
            lastTarget = pkg
            if (!unlockedSession) showOverlay()
        } else {
            val wasLocked = overlay != null
            hideOverlay()
            unlockedSession = false
            if (wasLocked && pkg != packageName && prefs.getBoolean("auto_return", false)) {
                returnToYouTube()
            }
        }
    }

    // Dipanggil kalau anak keluar dari YouTube saat masih terkunci (mis. swipe home)
    private fun returnToYouTube() {
        val now = System.currentTimeMillis()
        if (now - lastReturnAt < 1000) return
        lastReturnAt = now
        val intent = packageManager.getLaunchIntentForPackage(lastTarget) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    private fun showOverlay() {
        if (overlay != null) return

        val view = TouchBlockerView(this) {
            unlockedSession = true
            hideOverlay()
            Toast.makeText(this, "Kunci layar dibuka", Toast.LENGTH_SHORT).show()
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        try {
            wm.addView(view, lp)
            overlay = view
            Toast.makeText(
                this,
                "Layar terkunci. Buka: tahan pojok kiri atas + kanan bawah 2 detik",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            overlay = null
        }
    }

    private fun hideOverlay() {
        overlay?.let {
            it.cancelHold()
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlay = null
    }
}
