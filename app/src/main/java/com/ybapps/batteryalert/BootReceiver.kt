package com.ybapps.batteryalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            context.getSharedPreferences("battery", Context.MODE_PRIVATE).getBoolean("enabled", true)) {
            val service = Intent(context, BatteryMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service) else context.startService(service)
        }
    }
}
