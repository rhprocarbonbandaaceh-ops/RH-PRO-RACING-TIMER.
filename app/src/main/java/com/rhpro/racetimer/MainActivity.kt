package com.rhpro.racetimer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity(), LocationListener {
    private val bg = Color.rgb(8, 8, 13)
    private val panel = Color.rgb(23, 22, 31)
    private val purple = Color.rgb(168, 85, 247)
    private val silver = Color.rgb(220, 224, 235)
    private val muted = Color.rgb(158, 160, 178)
    private lateinit var locationManager: LocationManager
    private lateinit var status: TextView
    private lateinit var mainTimer: TextView
    private lateinit var distanceText: TextView
    private lateinit var speedText: TextView
    private lateinit var result201: TextView
    private lateinit var result402: TextView
    private lateinit var actionButton: Button
    private lateinit var historyBox: LinearLayout
    private lateinit var prefs: android.content.SharedPreferences

    private var armed = false
    private var running = false
    private var startLocation: Location? = null
    private var lastLocation: Location? = null
    private var raceStartNanos = 0L
    private var latestSpeed = 0.0
    private var travelled = 0.0
    private var elapsedAt201: Long? = null
    private var elapsedAt402: Long? = null
    private var lastFixTime = 0L
    private var best201 = "--.--- s"
    private var best402 = "--.--- s"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("rhpro_local_race_data", Context.MODE_PRIVATE)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        buildUi()
        refreshHistory()
        updateBestLabels()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 41)
        } else {
            status.text = "GPS SIAP • TEKAN PLAY"
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(14))
            background = gradient(intArrayOf(Color.rgb(5, 5, 13), Color.rgb(15, 5, 31), Color.rgb(4, 4, 12)), 0, 0)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(18))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.rhpro_logo_reference)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = "RH PRO Racing Team"
        }
        content.addView(logo, LinearLayout.LayoutParams(-1, dp(132)).apply {
            leftMargin = dp(2); rightMargin = dp(2); bottomMargin = dp(2)
        })
        val subtitle = label("G P S   D R A G   P E R F O R M A N C E   •   L O C A L   M O D E", 9f, silver, true)
        subtitle.gravity = Gravity.CENTER
        content.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
        status = label("◎  GPS SIAP  •  TEKAN PLAY", 13f, purple, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
        }
        content.addView(status, match())

        val timerCard = card().apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        timerCard.addView(label("///  ELAPSED TIME  ///", 12f, silver, true).apply { gravity = Gravity.CENTER }, match())
        mainTimer = label("00:00.000", 39f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC)
            letterSpacing = 0.03f
            setShadowLayer(dp(12).toFloat(), 0f, 0f, purple)
            setPadding(0, dp(8), 0, dp(8))
        }
        timerCard.addView(mainTimer, match())
        val metrics = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val distancePanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(5), dp(8), dp(5), dp(4)) }
        distanceText = label("JARAK\n0.0 m", 16f, Color.WHITE, true).apply { gravity = Gravity.CENTER; setLineSpacing(dp(3).toFloat(), 1f) }
        distancePanel.addView(distanceText, match())
        val speedPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(5), dp(8), dp(5), dp(4)) }
        speedText = label("KECEPATAN\n0 km/j", 16f, purple, true).apply { gravity = Gravity.CENTER; setLineSpacing(dp(3).toFloat(), 1f) }
        speedPanel.addView(speedText, match())
        metrics.addView(distancePanel, LinearLayout.LayoutParams(0, -2, 1f))
        val divider = View(this).apply { setBackgroundColor(Color.rgb(104, 45, 190)) }
        metrics.addView(divider, LinearLayout.LayoutParams(dp(1), dp(48)))
        metrics.addView(speedPanel, LinearLayout.LayoutParams(0, -2, 1f))
        timerCard.addView(metrics, match())
        content.addView(timerCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        val resultRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        result201 = resultCard("🏁  201 METER", "--.--- s")
        result402 = resultCard("🏁  402 METER", "--.--- s")
        resultRow.addView(result201, LinearLayout.LayoutParams(0, dp(94), 1f).apply { rightMargin = dp(5) })
        resultRow.addView(result402, LinearLayout.LayoutParams(0, dp(94), 1f).apply { leftMargin = dp(5) })
        content.addView(resultRow)

        actionButton = Button(this).apply {
            text = "▶   PLAY  •  SIAPKAN GPS"
            textSize = 17f
            letterSpacing = 0.04f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = gradient(intArrayOf(Color.rgb(118, 0, 255), Color.rgb(190, 35, 255), Color.rgb(82, 0, 210)), dp(15), Color.rgb(220, 90, 255))
            setOnClickListener { onPlayPressed() }
        }
        content.addView(actionButton, LinearLayout.LayoutParams(-1, dp(64)).apply { topMargin = dp(16); bottomMargin = dp(9) })
        val reset = Button(this).apply {
            text = "⟲   RESET / BATALKAN"
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(silver)
            background = gradient(intArrayOf(Color.rgb(16, 14, 29), Color.rgb(7, 7, 16)), dp(13), Color.rgb(92, 65, 160))
            setOnClickListener { resetRace() }
        }
        content.addView(reset, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(20) })

        val historyTitle = label("🏆  TOP SKOR LOKAL", 17f, Color.WHITE, true)
        content.addView(historyTitle, match())
        content.addView(label("Rekor terbaik dari hasil yang tersimpan di perangkat ini.", 11f, muted, false).apply { setPadding(0, dp(3), 0, dp(8)) }, match())
        historyBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = gradient(intArrayOf(Color.rgb(10, 9, 22), Color.rgb(13, 8, 27)), dp(12), Color.rgb(91, 37, 170))
        }
        content.addView(historyBox, match())
        content.addView(label("✦   R H   P R O   R A C I N G   T E A M   ✦", 10f, purple, true).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(18), 0, dp(4))
        }, match())
        setContentView(root)
    }

    private fun resultCard(title: String, initial: String): TextView = label("$title\n$initial", 16f, Color.WHITE, true).apply {
        gravity = Gravity.CENTER
        setLineSpacing(dp(5).toFloat(), 1f)
        background = gradient(intArrayOf(Color.rgb(13, 10, 27), Color.rgb(7, 7, 17)), dp(13), Color.rgb(132, 45, 230))
        setPadding(dp(5), dp(8), dp(5), dp(8))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = gradient(intArrayOf(Color.rgb(13, 11, 27), Color.rgb(7, 7, 17)), dp(16), Color.rgb(139, 45, 245))
        elevation = dp(4).toFloat()
    }

    private fun gradient(colors: IntArray, radius: Int, stroke: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = radius.toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun label(textValue: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = textValue; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

    private fun match() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun onPlayPressed() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 41)
            return
        }
        if (armed || running) {
            Toast.makeText(this, "Race sedang aktif. Tekan RESET untuk membatalkan.", Toast.LENGTH_SHORT).show()
            return
        }
        resetValues()
        armed = true
        status.text = "GPS AKTIF • MENUNGGU GERAKAN"
        actionButton.text = "MENUNGGU GERAKAN…"
        try {
            // GPS provider can also receive supported external/mock GNSS locations when configured by Android.
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200L, 0f, this)
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { onLocationChanged(it) }
        } catch (e: SecurityException) {
            status.text = "IZINKAN AKSES LOKASI"
        } catch (e: Exception) {
            status.text = "GPS TIDAK TERSEDIA"
            armed = false
        }
    }

    override fun onLocationChanged(location: Location) {
        val now = location.elapsedRealtimeNanos
        if (lastFixTime != 0L && now <= lastFixTime) return
        lastFixTime = now
        latestSpeed = if (location.hasSpeed()) max(0.0, location.speed.toDouble()) else {
            lastLocation?.let { old ->
                val seconds = (now - old.elapsedRealtimeNanos) / 1_000_000_000.0
                if (seconds > 0) old.distanceTo(location) / seconds else 0.0
            } ?: 0.0
        }
        if (armed && !running) {
            // Require a usable fix and two consecutive movement indications to reduce GPS drift triggers.
            val accuracyOk = !location.hasAccuracy() || location.accuracy <= 15f
            val moved = startLocation?.distanceTo(location) ?: 0f
            if (startLocation == null && accuracyOk) {
                startLocation = Location(location)
                status.text = "START TEREKAM • MENUNGGU MOTOR BERGERAK"
            } else if (accuracyOk && moved >= 3.0 && latestSpeed >= 1.8) {
                running = true
                armed = false
                raceStartNanos = now
                startLocation = lastLocation?.let { Location(it) } ?: Location(location)
                travelled = 0.0
                status.text = "RACE BERJALAN • GPS AKTIF"
                actionButton.text = "RACE BERJALAN"
            }
        } else if (running) {
            val start = startLocation
            if (start != null) {
                // Accumulate step distances to avoid straight-line displacement undercounting.
                val prev = lastLocation
                if (prev != null) travelled += prev.distanceTo(location).toDouble()
            }
            val elapsedMs = max(0L, (now - raceStartNanos) / 1_000_000L)
            if (elapsedAt201 == null && travelled >= 201.0) {
                elapsedAt201 = elapsedMs
                result201.text = "🏁  201 METER\n${formatTime(elapsedMs)}"
                savePartialBest("201", elapsedMs)
            }
            if (elapsedAt402 == null && travelled >= 402.0) {
                elapsedAt402 = elapsedMs
                result402.text = "🏁  402 METER\n${formatTime(elapsedMs)}"
                saveCompletedRace(elapsedAt201, elapsedAt402, latestSpeed, travelled)
                running = false
                status.text = "FINISH • HASIL TERSIMPAN DI HP INI"
                actionButton.text = "PLAY LAGI"
                try { locationManager.removeUpdates(this) } catch (_: Exception) {}
                refreshHistory()
                updateBestLabels()
            }
            mainTimer.text = formatTime(elapsedMs)
            distanceText.text = "JARAK\n${"%.1f".format(Locale.US, travelled)} m"
            speedText.text = "KECEPATAN\n${"%.1f".format(Locale.US, latestSpeed * 3.6)} km/j"
        }
        lastLocation = Location(location)
    }

    private fun savePartialBest(distance: String, time: Long) {
        val arr = loadRaces()
        val item = JSONObject().apply {
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
            put("t201", time)
            put("t402", JSONObject.NULL)
            put("maxSpeed", latestSpeed * 3.6)
            put("distance", 201)
        }
        arr.put(item)
        prefs.edit().putString("races", arr.toString()).apply()
        refreshHistory()
        updateBestLabels()
    }

    private fun saveCompletedRace(t201: Long?, t402: Long?, speed: Double, distance: Double) {
        val arr = loadRaces()
        val item = JSONObject().apply {
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
            put("t201", t201 ?: JSONObject.NULL)
            put("t402", t402 ?: JSONObject.NULL)
            put("maxSpeed", speed * 3.6)
            put("distance", distance)
        }
        arr.put(item)
        prefs.edit().putString("races", arr.toString()).apply()
    }

    private fun loadRaces(): JSONArray = try {
        JSONArray(prefs.getString("races", "[]") ?: "[]")
    } catch (_: Exception) { JSONArray() }

    private fun refreshHistory() {
        if (!::historyBox.isInitialized) return
        historyBox.removeAllViews()
        val arr = loadRaces()
        if (arr.length() == 0) {
            historyBox.addView(label("Belum ada hasil race.", 13f, muted, false))
            return
        }
        val items = (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i) }
            .sortedByDescending { it.optLong("t402", -1L) }
        items.take(10).forEachIndexed { index, item ->
            val t201 = item.optLong("t201", -1L)
            val t402 = item.optLong("t402", -1L)
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(if (index % 2 == 0) panel else bg)
            }
            line.addView(label("#${index + 1}  ${item.optString("date")}", 12f, muted, true))
            line.addView(label("201 m: ${if (t201 >= 0) formatTime(t201) else "--"}   •   402 m: ${if (t402 >= 0) formatTime(t402) else "--"}", 14f, silver, true))
            historyBox.addView(line, match().apply { bottomMargin = dp(4) })
        }
    }

    private fun updateBestLabels() {
        val arr = loadRaces()
        var b201 = Long.MAX_VALUE
        var b402 = Long.MAX_VALUE
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val a = o.optLong("t201", -1L)
            val b = o.optLong("t402", -1L)
            if (a > 0 && a < b201) b201 = a
            if (b > 0 && b < b402) b402 = b
        }
        best201 = if (b201 == Long.MAX_VALUE) "--.--- s" else formatTime(b201)
        best402 = if (b402 == Long.MAX_VALUE) "--.--- s" else formatTime(b402)
    }

    private fun resetValues() {
        startLocation = null; lastLocation = null; raceStartNanos = 0L
        latestSpeed = 0.0; travelled = 0.0; elapsedAt201 = null; elapsedAt402 = null
        lastFixTime = 0L
        mainTimer.text = "00:00.000"
        distanceText.text = "JARAK\n0.0 m"
        speedText.text = "KECEPATAN\n0 km/j"
        result201.text = "🏁  201 METER\n--.--- s"; result402.text = "🏁  402 METER\n--.--- s"
    }

    private fun resetRace() {
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
        armed = false; running = false
        resetValues()
        status.text = "DIBATALKAN • TEKAN PLAY"
        actionButton.text = "PLAY • SIAPKAN GPS"
    }

    private fun formatTime(ms: Long): String {
        val minutes = ms / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            status.text = "GPS SIAP • TEKAN PLAY"
        } else {
            status.text = "AKSES LOKASI DIBUTUHKAN"
        }
    }

    override fun onProviderEnabled(provider: String) { status.text = "GPS AKTIF" }
    override fun onProviderDisabled(provider: String) { status.text = "AKTIFKAN GPS HP" }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onPause() {
        super.onPause()
        // Safety: don't silently keep a race running if the app is backgrounded.
        if (running || armed) {
            try { locationManager.removeUpdates(this) } catch (_: Exception) {}
            armed = false; running = false
            status.text = "RACE DIHENTIKAN • APLIKASI KELUAR DARI DEPAN"
            actionButton.text = "PLAY • SIAPKAN GPS"
        }
    }
}
