package com.bangkudepan.ytlock

import android.app.Activity
import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        val pad = (20 * d).toInt()

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
            setPadding(0, pad / 2, 0, pad / 2)
        }
        root.addView(status)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        root.addView(Switch(this).apply {
            text = "Balik otomatis ke YouTube kalau keluar saat masih terkunci"
            textSize = 15f
            isChecked = prefs.getBoolean("auto_return", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("auto_return", checked).apply()
            }
        })

        root.addView(TextView(this).apply {
            setPadding(0, pad / 2, 0, pad / 2)
            text = "Cara pakai:\n" +
                "1. Tekan tombol di bawah, cari \"Kunci YouTube\" lalu aktifkan.\n" +
                "2. Buka YouTube. Layar otomatis terkunci, video tetap jalan.\n" +
                "3. Untuk membuka kunci, tahan pojok kiri atas dan pojok kanan bawah bersamaan selama 2 detik.\n" +
                "4. Kunci aktif lagi setiap YouTube dibuka ulang.\n" +
                "5. Kalau saklar balik otomatis aktif, keluar dari YouTube saat terkunci akan langsung membuka YouTube lagi. Setelah kunci dibuka, saklar ini tidak berlaku sampai YouTube dibuka ulang.\n\n" +
                "Kalau tombol aktifkan berwarna abu-abu (Android 13 ke atas), buka Info Aplikasi, tekan titik tiga di kanan atas, lalu pilih \"Izinkan setelan terbatas\"."
            textSize = 15f
        })

        root.addView(Button(this).apply {
            text = "Buka Pengaturan Aksesibilitas"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        root.addView(Button(this).apply {
            text = "Buka Info Aplikasi"
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                )
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        status.text = if (enabled.contains(packageName)) {
            "Status: AKTIF"
        } else {
            "Status: belum aktif"
        }
    }
}
