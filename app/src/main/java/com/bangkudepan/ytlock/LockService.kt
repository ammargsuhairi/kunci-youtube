package com.bangkudepan.ytlock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

class LockService : AccessibilityService() {

    private val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val prefs: SharedPreferences by lazy { getSharedPreferences(Prefs.NAME, MODE_PRIVATE) }

    private var overlay: TouchBlockerView? = null

    // true kalau orang tua sudah membuka kunci; direset saat keluar dari YouTube
    private var unlockedSession = false
    private var lastTarget = "com.google.android.youtube"
    private var lastReturnAt = 0L

    private val targets = setOf(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.kids"
    )

    // Aplikasi telepon tidak boleh ditimpa YouTube saat balik otomatis
    private val callHints = listOf("dialer", "incallui", "telecom", "telephony", "com.android.phone", "emergency")

    // Dipegang sebagai field supaya tidak dibuang garbage collector
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        // Kalau YouTube sudah terbuka saat service diaktifkan, langsung kunci
        evaluate(foregroundPackage())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        var fg = foregroundPackage()
        if (fg == null && type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Cadangan kalau daftar jendela tidak tersedia
            val p = event.packageName?.toString()
            if (p != null && !isSystemNoise(p)) fg = p
        }
        evaluate(fg)
    }

    override fun onInterrupt() {
        // Sengaja kosong: jangan buka kunci hanya karena sistem meminta interupsi
    }

    override fun onUnbind(intent: Intent?): Boolean {
        try {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        } catch (_: Exception) {
        }
        hideOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    /** Nama paket aplikasi yang sedang aktif di layar, atau null kalau tidak bisa dipastikan. */
    private fun foregroundPackage(): String? {
        return try {
            var found: String? = null
            for (w in windows) {
                if (w.type != AccessibilityWindowInfo.TYPE_APPLICATION || !w.isActive) continue
                val root = w.root ?: continue
                val pkg = root.packageName?.toString()
                root.recycle()
                if (pkg != null) {
                    found = pkg
                    break
                }
            }
            found
        } catch (e: Exception) {
            null
        }
    }

    private fun isSystemNoise(p: String): Boolean {
        return p == "com.android.systemui" || p == "android" ||
            p.contains("inputmethod") || p.contains("keyboard")
    }

    private fun isCallApp(p: String): Boolean = callHints.any { p.contains(it) }

    private fun evaluate(fg: String?) {
        if (!prefs.getBoolean(Prefs.ENABLED, true)) {
            hideOverlay()
            return
        }
        if (fg == null) return
        if (fg == packageName && overlay != null) return

        if (fg in targets) {
            lastTarget = fg
            if (!unlockedSession) showOverlay()
        } else {
            val wasLocked = overlay != null
            val shouldReturn = wasLocked &&
                fg != packageName &&
                prefs.getBoolean(Prefs.AUTO_RETURN, true) &&
                !isCallApp(fg)
            if (shouldReturn) returnToYouTube()
            hideOverlay()
            unlockedSession = false
        }
    }

    private fun refresh() {
        overlay?.indicatorVisible = prefs.getBoolean(Prefs.INDICATOR, false)
        evaluate(foregroundPackage())
    }

    private fun returnToYouTube() {
        val now = System.currentTimeMillis()
        if (now - lastReturnAt < 800) return
        lastReturnAt = now
        val intent = packageManager.getLaunchIntentForPackage(lastTarget) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun showOverlay() {
        if (overlay != null) return

        val view = TouchBlockerView(this) {
            unlockedSession = true
            hideOverlay()
        }
        view.indicatorVisible = prefs.getBoolean(Prefs.INDICATOR, false)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        try {
            wm.addView(view, lp)
            overlay = view
        } catch (_: Exception) {
            overlay = null
        }
    }

    private fun hideOverlay() {
        val v = overlay ?: return
        overlay = null
        v.cancelHold()
        try {
            wm.removeView(v)
        } catch (_: Exception) {
        }
    }
}
