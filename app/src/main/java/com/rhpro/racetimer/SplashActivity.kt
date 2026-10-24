package com.rhpro.racetimer

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 8, 13)
        window.navigationBarColor = Color.rgb(8, 8, 13)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(SpeedometerSplashView(this))
        handler.postDelayed({
            if (!isFinishing && !launched) {
                launched = true
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }, 2600L)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

private class SpeedometerSplashView(context: android.content.Context) : View(context) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(8, 8, 13) }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 10f
        color = Color.rgb(168, 85, 247)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 20f
        color = Color.argb(45, 168, 85, 247)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.rgb(220, 224, 235)
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 7f
        color = Color.WHITE
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(168, 85, 247) }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(190, 145, 255); textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(47, 37, 62); style = Paint.Style.STROKE; strokeWidth = 5f
    }
    private val startTime = android.os.SystemClock.uptimeMillis()
    private var angle = -135f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(8, 8, 13))
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.40f
        val radius = min(w * 0.34f, h * 0.24f)
        val bounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // Subtle diagonal racing streaks
        val streak = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(38, 168, 85, 247); strokeWidth = 3f }
        for (i in 0..7) {
            val x = w * (i / 8f)
            canvas.drawLine(x, h * 0.12f, x + w * 0.13f, h * 0.02f, streak)
            canvas.drawLine(x, h * 0.79f, x + w * 0.10f, h * 0.74f, streak)
        }

        canvas.drawArc(bounds, 135f, 270f, false, glowPaint)
        canvas.drawArc(bounds, 135f, 270f, false, arcPaint)
        canvas.drawArc(bounds, 135f, 270f, false, trackPaint)

        for (i in 0..12) {
            val a = Math.toRadians((135 + i * (270.0 / 12.0)))
            val inner = radius * 0.76f
            val outer = radius * 0.91f
            val x1 = cx + cos(a).toFloat() * inner
            val y1 = cy + sin(a).toFloat() * inner
            val x2 = cx + cos(a).toFloat() * outer
            val y2 = cy + sin(a).toFloat() * outer
            tickPaint.strokeWidth = if (i % 3 == 0) 4f else 2f
            tickPaint.color = if (i % 3 == 0) Color.WHITE else Color.rgb(173, 128, 235)
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        val elapsed = (android.os.SystemClock.uptimeMillis() - startTime).coerceAtMost(2300L) / 2300f
        // Needle sweeps across the dial and returns to the lower-left resting position.
        val sweep = if (elapsed < 0.72f) elapsed / 0.72f else (1f - elapsed) / 0.28f
        angle = -135f + 270f * sweep.coerceIn(0f, 1f)
        val radians = Math.toRadians(angle.toDouble())
        canvas.drawLine(cx, cy, cx + cos(radians).toFloat() * radius * 0.69f,
            cy + sin(radians).toFloat() * radius * 0.69f, needlePaint)
        canvas.drawCircle(cx, cy, radius * 0.075f, hubPaint)

        titlePaint.textSize = min(w * 0.105f, 54f)
        canvas.drawText("RH PRO", cx, h * 0.72f, titlePaint)
        titlePaint.textSize = min(w * 0.047f, 25f)
        canvas.drawText("RACING TIMER", cx, h * 0.77f, titlePaint)
        subPaint.textSize = min(w * 0.031f, 16f)
        canvas.drawText("INITIALIZING GPS SYSTEM", cx, h * 0.84f, subPaint)

        // Animated loading rail
        val rail = RectF(w * 0.22f, h * 0.88f, w * 0.78f, h * 0.88f + 6f)
        canvas.drawRoundRect(rail, 5f, 5f, trackPaint.apply { strokeWidth = 6f })
        val progress = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(168, 85, 247) }
        val end = rail.left + rail.width() * elapsed
        canvas.drawRoundRect(RectF(rail.left, rail.top, maxOf(rail.left + 4f, end), rail.bottom), 5f, 5f, progress)
        if (elapsed < 1f) postInvalidateDelayed(16L)
    }
}
