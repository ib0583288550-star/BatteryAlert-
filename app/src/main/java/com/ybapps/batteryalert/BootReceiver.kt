package com.ybapps.batteryalert
import android.content.*
import android.os.Build
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action==Intent.ACTION_BOOT_COMPLETED&&c.getSharedPreferences("battery_alert",0).getBoolean("enabled",true)){val s=Intent(c,BatteryMonitorService::class.java);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s) else c.startService(s)}}}