package com.bangkudepan.ytlock

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private var pad = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        pad = (20 * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Kunci YouTube"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
        })

        status = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, pad / 2, 0, pad / 2)
        }
        root.addView(status)

        addSwitch(root, "Kunci aktif", Prefs.ENABLED, true)
        addSwitch(
            root,
            "Balik otomatis ke YouTube kalau keluar saat masih terkunci",
            Prefs.AUTO_RETURN,
            true
        )
        addSwitch(
            root,
            "Tampilkan titik kecil di pojok kiri bawah saat terkunci (untuk pengecekan)",
            Prefs.INDICATOR,
            false
        )

        root.addView(TextView(this).apply {
            setPadding(0, pad, 0, pad / 2)
            textSize = 15f
            text = "Cara pakai:\n" +
                "1. Aktifkan layanan di Pengaturan Aksesibilitas (tombol di bawah).\n" +
                "2. Buka YouTube. Layar otomatis terkunci, video tetap jalan.\n" +
                "3. Buka kunci: tahan area kiri atas dan area kanan bawah bersamaan selama 2,5 detik. " +
                "Taruh jari agak ke dalam dari tepi layar (sekitar satu ruas jari), jangan digeser. " +
                "HP akan bergetar sekali kalau kunci terbuka.\n" +
                "4. Kunci aktif lagi setiap YouTube dibuka ulang.\n" +
                "5. Kalau balik otomatis aktif, keluar dari YouTube saat terkunci akan langsung membuka YouTube lagi. " +
                "Setelah kunci dibuka, fitur ini tidak berlaku sampai YouTube dibuka ulang.\n\n" +
                "Kalau tombol aktifkan berwarna abu-abu (Android 13 ke atas): buka Info Aplikasi, " +
                "tekan titik tiga kanan atas, pilih \"Izinkan setelan terbatas\", lalu aktifkan lagi.\n\n" +
                "Kalau layanan sering mati sendiri, set baterai app ini ke \"Tanpa pembatasan\" " +
                "dan aktifkan Autostart (Xiaomi, Oppo, Vivo, Realme)."
        })

        addButton(root, "Buka Pengaturan Aksesibilitas") {
            safeStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        addButton(root, "Buka Info Aplikasi") {
            safeStart(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        }
        addButton(root, "Buka Pengaturan Baterai") {
            safeStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        if (enabled.contains(packageName)) {
            status.text = "Status layanan: AKTIF"
            status.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            status.text = "Status layanan: belum aktif"
            status.setTextColor(Color.parseColor("#C62828"))
        }
    }

    private fun addSwitch(root: LinearLayout, label: String, key: String, def: Boolean) {
        val prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
        root.addView(Switch(this).apply {
            text = label
            textSize = 15f
            setPadding(0, pad / 3, 0, pad / 3)
            isChecked = prefs.getBoolean(key, def)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
            }
        })
    }

    private fun addButton(root: LinearLayout, label: String, onClick: () -> Unit) {
        root.addView(Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        })
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
