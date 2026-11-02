package com.ybapps.batteryalert

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat

class BatteryMonitorService : Service() {
    private var lastAlert = -1
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED) return
            if (!getSharedPreferences("battery", MODE_PRIVATE).getBoolean("enabled", true)) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val percent = (level * 100 / scale)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            if (plugged) { lastAlert = -1; return }
            val threshold = when {
                percent <= 5 -> 5
                percent <= 10 -> 10
                percent <= 15 -> 15
                else -> -1
            }
            if (threshold > 0 && threshold != lastAlert) {
                lastAlert = threshold
                showAlert(threshold, percent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("monitor", "מעקב סוללה", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel("alerts", "התרעות סוללה", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) })
        startForeground(10, NotificationCompat.Builder(this, "monitor")
            .setSmallIcon(R.drawable.ic_battery_alert).setContentTitle("התרעת סוללה")
            .setContentText("מעקב אחר רמת הסוללה פעיל").setOngoing(true).build())
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun showAlert(threshold: Int, current: Int) {
        val notification = NotificationCompat.Builder(this, "alerts")
            .setSmallIcon(R.drawable.ic_battery_alert)
            .setContentTitle("הסוללה הגיעה ל־$threshold%")
            .setContentText("רמת הסוללה כרגע: $current%")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true).build()
        getSystemService(NotificationManager::class.java).notify(threshold, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onDestroy() {
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) { }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
}
