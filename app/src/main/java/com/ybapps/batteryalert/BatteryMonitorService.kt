package com.ybapps.batteryalert
import android.app.*
import android.content.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import androidx.core.app.NotificationCompat
class BatteryMonitorService : Service() {
 private var receiver: BroadcastReceiver? = null
 private val prefs by lazy { getSharedPreferences("battery_alert", MODE_PRIVATE) }
 override fun onCreate() {
  super.onCreate()
  if (Build.VERSION.SDK_INT >= 26) { val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager; nm.createNotificationChannel(NotificationChannel("monitor","מעקב סוללה",NotificationManager.IMPORTANCE_LOW)); nm.createNotificationChannel(NotificationChannel("alerts","התראות סוללה",NotificationManager.IMPORTANCE_HIGH)) }
  val ongoing=NotificationCompat.Builder(this,"monitor").setSmallIcon(android.R.drawable.ic_lock_idle_charging).setContentTitle("התרעת סוללה פעילה").setContentText("עוקב אחרי 15%, 10% ו־5%").setOngoing(true).build()
  startForeground(10,ongoing)
  receiver=object:BroadcastReceiver(){ override fun onReceive(c:Context,i:Intent){ if(i.action!=Intent.ACTION_BATTERY_CHANGED)return
   val p=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)*100/i.getIntExtra(BatteryManager.EXTRA_SCALE,100).coerceAtLeast(1)
   alert(p,15,"a15"); alert(p,10,"a10"); alert(p,5,"a5")
  }}
  registerReceiver(receiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
 }
 private fun alert(p:Int,t:Int,k:String){
  if(p>t+2)prefs.edit().putBoolean(k,false).apply()
  if(p<=t&&!prefs.getBoolean(k,false)){
   prefs.edit().putBoolean(k,true).apply()
   val mode=prefs.getString("mode","both")?:"both"
   if(mode!="sound"){ val n=NotificationCompat.Builder(this,"alerts").setSmallIcon(android.R.drawable.ic_lock_idle_low_battery).setContentTitle("התרעת סוללה").setContentText("הסוללה הגיעה ל־$t%").setPriority(NotificationCompat.PRIORITY_MAX).setAutoCancel(true).build(); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(t,n) }
   if(mode!="screen")try{ToneGenerator(AudioManager.STREAM_ALARM,100).startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,900)}catch(_:Exception){}
  }
 }
 override fun onStartCommand(i:Intent?,f:Int,id:Int)=START_STICKY
 override fun onDestroy(){receiver?.let{try{unregisterReceiver(it)}catch(_:Exception){}};super.onDestroy()}
 override fun onBind(i:Intent?)=null
}