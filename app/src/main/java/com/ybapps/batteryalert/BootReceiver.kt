package com.ybapps.batteryalert

import android.content.*
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val shouldStart = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (shouldStart && context.getSharedPreferences("battery_alert", 0).getBoolean("enabled", true)) {
            val service = Intent(context, BatteryMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service) else context.startService(service)
        }
    }
}
