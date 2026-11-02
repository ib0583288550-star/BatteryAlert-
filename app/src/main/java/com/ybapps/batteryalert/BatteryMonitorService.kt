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
   val plugged=i.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0
   alert(p,15,"a15"); alert(p,10,"a10"); alert(p,5,"a5")
   updatePersistentAlert(p,plugged)
  }}
  registerReceiver(receiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
 }
 private fun alert(p:Int,t:Int,k:String){
  if(p>t+2)prefs.edit().putBoolean(k,false).apply()
  if(p<=t&&!prefs.getBoolean(k,false)){
   prefs.edit().putBoolean(k,true).apply()
   val mode=prefs.getString("mode","both")?:"both"
   val locked=prefs.getBoolean("locked_alert",false)
   if(mode!="sound"&&!locked){ val n=NotificationCompat.Builder(this,"alerts").setSmallIcon(android.R.drawable.ic_lock_idle_low_battery).setContentTitle("התרעת סוללה").setContentText(messageFor(t)).setPriority(NotificationCompat.PRIORITY_MAX).setAutoCancel(true).build(); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(t,n) }
   if(mode!="screen")try{ToneGenerator(AudioManager.STREAM_ALARM,100).startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,900)}catch(_:Exception){}
  }
 }
 private fun messageFor(t:Int)=when(t){15->"חבר למטען כשיתאפשר";10->"כדאי לחבר למטען עכשיו";else->"חבר למטען מיד – הסוללה עומדת להתרוקן"}
 private fun updatePersistentAlert(p:Int,plugged:Boolean){
  val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  if(!prefs.getBoolean("locked_alert",false)||plugged||p>15){nm.cancel(99);return}
  val text=when{p<=5->messageFor(5);p<=10->messageFor(10);else->messageFor(15)}
  val n=NotificationCompat.Builder(this,"alerts")
   .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
   .setContentTitle("התרעת סוללה – $p%")
   .setContentText(text)
   .setStyle(NotificationCompat.BigTextStyle().bigText("$text. ההודעה תוסר לאחר חיבור למטען."))
   .setPriority(NotificationCompat.PRIORITY_MAX)
   .setOngoing(true)
   .setAutoCancel(false)
   .build()
  nm.notify(99,n)
 }
 override fun onStartCommand(i:Intent?,f:Int,id:Int)=START_STICKY
 override fun onDestroy(){receiver?.let{try{unregisterReceiver(it)}catch(_:Exception){}};super.onDestroy()}
 override fun onBind(i:Intent?)=null
}