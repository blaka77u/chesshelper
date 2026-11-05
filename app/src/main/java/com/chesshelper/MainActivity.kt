package com.chesshelper

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF0F2027.toInt(), 0xFF203A43.toInt(), 0xFF2C5364.toInt())
            )
        }
        fun label(t: String, size: Float, c: Int) = TextView(this).apply {
            text = t; textSize = size; setTextColor(c); gravity = Gravity.CENTER
        }
        root.addView(label("♞", 90f, Color.WHITE))
        root.addView(label("Chess Helper", 30f, Color.WHITE))
        root.addView(label("Умный помощник поверх шахмат", 14f, 0xAAFFFFFF.toInt()).apply {
            setPadding(0, dp(8), 0, dp(48))
        })
        val btn = label("Начать", 20f, Color.WHITE).apply {
            setPadding(dp(64), dp(16), dp(64), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(32).toFloat()
                setColor(0x33FFFFFF); setStroke(dp(1), 0x88FFFFFF.toInt())
            }
            setOnClickListener { start() }
        }
        root.addView(btn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
    }

    private fun start() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Разреши «Поверх других приложений» и нажми «Начать» снова", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        startForegroundService(Intent(this, OverlayService::class.java))
        finish() // приложение закрывается, остаётся только меню
    }
}
