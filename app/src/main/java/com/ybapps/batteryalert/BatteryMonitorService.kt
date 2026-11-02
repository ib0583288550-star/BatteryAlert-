package com.ybapps.batteryalert

import android.app.*
import android.content.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class BatteryMonitorService : Service() {
    private var receiver: BroadcastReceiver? = null
    private var overlayView: TextView? = null
    private val prefs by lazy { getSharedPreferences("battery_alert", MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel("monitor", "מעקב סוללה", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel("alerts", "התראות סוללה", NotificationManager.IMPORTANCE_HIGH))
        }
        val ongoing = NotificationCompat.Builder(this, "monitor")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("התרעת סוללה פעילה")
            .setContentText("מעקב אחר אחוזי הסוללה")
            .setOngoing(true).build()
        startForeground(10, ongoing)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.action != Intent.ACTION_BATTERY_CHANGED) return
                val p = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 /
                    i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                alert(p, 15, "a15")
                alert(p, 10, "a10")
                alert(p, 5, "a5")
                val selectedEmergencyLevels = prefs.getStringSet("emergency_levels", emptySet()) ?: emptySet()
                if (selectedEmergencyLevels.contains("4")) alert(p, 4, "a4")
                if (selectedEmergencyLevels.contains("3")) alert(p, 3, "a3")
                if (selectedEmergencyLevels.contains("2")) alert(p, 2, "a2")
                if (selectedEmergencyLevels.contains("1")) alert(p, 1, "a1")
                updatePersistentAlert(p, plugged)
            }
        }
        registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun alert(p: Int, threshold: Int, key: String) {
        if (p > threshold + 2) prefs.edit().putBoolean(key, false).apply()
        if (p <= threshold && !prefs.getBoolean(key, false)) {
            prefs.edit().putBoolean(key, true).apply()
            val mode = prefs.getString("mode", "both") ?: "both"
            val locked = prefs.getBoolean("locked_alert", false)
            if (mode != "sound" && !locked) {
                val n = NotificationCompat.Builder(this, "alerts")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
                    .setContentTitle("התרעת סוללה")
                    .setContentText(messageFor(threshold))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true).build()
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(threshold, n)
            }
            val soundEnabled = if (prefs.contains("sound_levels")) {
                prefs.getStringSet("sound_levels", emptySet())?.contains(threshold.toString()) == true
            } else true
            if (mode != "screen" && soundEnabled) {
                try {
                    ToneGenerator(AudioManager.STREAM_ALARM, 100)
                        .startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)
                } catch (_: Exception) {}
            }
        }
    }

    private fun messageFor(t: Int) = when (t) {
        15 -> "חבר למטען כשיתאפשר"
        10 -> "כדאי לחבר למטען עכשיו"
        5 -> "חבר למטען מיד – הסוללה עומדת להתרוקן"
        else -> "סוללה קריטית – חבר למטען עכשיו! ($t%)"
    }

    private fun updatePersistentAlert(p: Int, plugged: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val overlayEnabled = prefs.getBoolean("overlay_alert", false) && Build.VERSION.SDK_INT >= 23 && android.provider.Settings.canDrawOverlays(this)
        if (plugged || p > 15) {
            removeOverlay()
            nm.cancel(99)
            return
        }
        if (overlayEnabled) {
            nm.cancel(99)
            showOverlay(p)
            return
        } else {
            removeOverlay()
        }
        if (!prefs.getBoolean("locked_alert", false)) {
            nm.cancel(99)
            return
        }
        val text = when {
            p <= 5 -> messageFor(5)
            p <= 10 -> messageFor(10)
            else -> messageFor(15)
        }
        val n = NotificationCompat.Builder(this, "alerts")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentTitle("התרעת סוללה – $p%")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text. ההודעה תוסר לאחר חיבור למטען."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        nm.notify(99, n)
    }

    private fun showOverlay(p: Int) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val text = when {
            p <= 5 -> "סוללה קריטית – $p% | חבר למטען מיד"
            p <= 10 -> "נותרו $p% סוללה – כדאי לחבר למטען"
            else -> "נותרו $p% סוללה – חבר למטען כשיתאפשר"
        }
        val view = overlayView ?: TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(22, 16, 22, 16)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.rgb(20, 43, 55))
                cornerRadius = 28f
                setStroke(2, Color.rgb(85, 230, 165))
            }
            isClickable = false
            isFocusable = false
        }.also { overlayView = it }
        view.text = text
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 36 }
        try {
            if (view.parent == null) wm.addView(view, params) else wm.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view) } catch (_: Exception) {}
        overlayView = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onDestroy() {
        removeOverlay()
        receiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
}
