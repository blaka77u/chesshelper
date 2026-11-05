package com.chesshelper

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.*

data class Mark(val c: Int, val r: Int, val layer: Int, val color: Int)

/** Рисует подсветку клеток. Пока — демо-данные; этап 2 подставит реальные из распознавания. */
class BoardView(ctx: Context) : View(ctx) {
    private val on = BooleanArray(3)
    private val p = Paint()
    private val marks = listOf(
        Mark(3, 5, 0, 0xFFFF3B30.toInt()), Mark(5, 2, 0, 0xFFFF3B30.toInt()),   // угрозы
        Mark(4, 4, 1, 0xFFFFD60A.toInt()), Mark(4, 3, 1, 0xFFFFD60A.toInt()),   // ходы
        Mark(5, 3, 1, 0xFF34C759.toInt()),                                      // можно съесть
        Mark(3, 3, 1, 0xFFAF52DE.toInt()),                                      // съесть, но съедят
        Mark(2, 5, 2, 0xFF00B8FF.toInt()), Mark(2, 4, 2, 0xFF00B8FF.toInt())    // лучший ход
    )
    fun setLayer(i: Int, v: Boolean) { on[i] = v; invalidate() }
    override fun onDraw(cv: Canvas) {
        val cell = width / 8f
        val top = (height - width) / 2f
        for (m in marks) if (on[m.layer]) {
            p.color = m.color; p.alpha = 120
            cv.drawRect(m.c * cell, top + m.r * cell, (m.c + 1) * cell, top + (m.r + 1) * cell, p)
        }
    }
}

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var board: BoardView
    private lateinit var panel: LinearLayout
    private lateinit var tab: TextView
    private lateinit var panelLp: LayoutParams

    override fun onBind(i: Intent?): IBinder? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp(w: Int, h: Int, touch: Boolean): LayoutParams {
        var f = LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!touch) f = f or LayoutParams.FLAG_NOT_TOUCHABLE
        return LayoutParams(w, h, LayoutParams.TYPE_APPLICATION_OVERLAY, f, PixelFormat.TRANSLUCENT)
    }

    private fun glass(r: Int, fill: Int) = GradientDrawable().apply {
        cornerRadius = dp(r).toFloat(); setColor(fill); setStroke(dp(1), 0x66FFFFFF)
    }

    private fun txt(t: String, size: Float) = TextView(this).apply {
        text = t; textSize = size; setTextColor(Color.WHITE)
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("ch", "Chess Helper", NotificationManager.IMPORTANCE_LOW))
        startForeground(1, Notification.Builder(this, "ch")
            .setContentTitle("Chess Helper работает")
            .setSmallIcon(android.R.drawable.ic_menu_compass).build())

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        board = BoardView(this)
        wm.addView(board, lp(-1, -1, false))

        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = glass(28, 0x44FFFFFF)
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val title = txt("♞ Помощник", 16f)
        val min = txt("  ›  ", 22f).apply { setOnClickListener { collapse(true) } }
        val close = txt("  ✕", 18f).apply { setOnClickListener { stopSelf() } }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(min); header.addView(close)
        panel.addView(header)
        panel.addView(row("Угрозы мне", 0xFFFF3B30.toInt(), 0))
        panel.addView(row("Мои ходы", 0xFFFFD60A.toInt(), 1))
        panel.addView(row("Лучший ход", 0xFF00B8FF.toInt(), 2))

        panelLp = lp(-2, -2, true).apply {
            gravity = Gravity.TOP or Gravity.START; x = dp(24); y = dp(120)
            if (Build.VERSION.SDK_INT >= 31) {
                flags = flags or LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = 40
            }
        }
        title.setOnTouchListener(object : View.OnTouchListener {
            var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = panelLp.x; oy = panelLp.y }
                    MotionEvent.ACTION_MOVE -> {
                        panelLp.x = ox + (e.rawX - sx).toInt(); panelLp.y = oy + (e.rawY - sy).toInt()
                        wm.updateViewLayout(panel, panelLp)
                    }
                }
                return true
            }
        })
        wm.addView(panel, panelLp)

        tab = txt("♞", 24f).apply {
            setPadding(dp(10), dp(14), dp(10), dp(14))
            background = glass(16, 0x66FFFFFF)
            visibility = View.GONE
            setOnClickListener { collapse(false) }
        }
        wm.addView(tab, lp(-2, -2, true).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL })
    }

    private fun collapse(c: Boolean) {
        panel.visibility = if (c) View.GONE else View.VISIBLE
        tab.visibility = if (c) View.VISIBLE else View.GONE
    }

    private fun row(label: String, color: Int, layer: Int) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(6), 0, dp(6))
        addView(View(context).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
        }, LinearLayout.LayoutParams(dp(12), dp(12)).apply { rightMargin = dp(10) })
        addView(txt(label, 14f), LinearLayout.LayoutParams(0, -2, 1f))
        addView(Switch(context).apply { setOnCheckedChangeListener { _, v -> board.setLayer(layer, v) } })
    }

    override fun onDestroy() {
        for (v in listOf<View>(board, panel, tab)) try { wm.removeView(v) } catch (_: Exception) {}
        super.onDestroy()
    }
}
