package com.ybapps.batteryalert
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.BatteryManager
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
class MainActivity:ComponentActivity(){
 private val prefs by lazy{getSharedPreferences("battery_alert",MODE_PRIVATE)}
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun onCreate(savedInstanceState:android.os.Bundle?){super.onCreate(savedInstanceState)
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS)
  setContent{
   var active by remember{mutableStateOf(prefs.getBoolean("enabled",true))}
   var mode by remember{mutableStateOf(prefs.getString("mode","both")?:"both")}
   var level by remember{mutableIntStateOf(battery())}
   LaunchedEffect(Unit){while(true){level=battery();kotlinx.coroutines.delay(4000)}}
   MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF55E6A5),background=Color(0xFF0B1220),surface=Color(0xFF1B3045))){
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF12333A),Color(0xFF0B1220)))).padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){
     Spacer(Modifier.height(24.dp));Icon(Icons.Default.BatteryChargingFull,null,tint=Color(0xFF55E6A5),modifier=Modifier.size(48.dp))
     Text("התרעת סוללה",fontSize=29.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("לא תיתפס בלי סוללה",color=Color(0xFF9DB4C9))
     Spacer(Modifier.height(25.dp))
     Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF1B3045)),shape=RoundedCornerShape(26.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("רמת הסוללה כרגע",color=Color.LightGray);Text("$level%",fontSize=58.sp,fontWeight=FontWeight.ExtraBold,color=if(level<=15)Color(0xFFFFC857) else Color(0xFF55E6A5));LinearProgressIndicator(progress={level/100f},modifier=Modifier.fillMaxWidth().height(8.dp))}}
     Spacer(Modifier.height(22.dp));Text("נקודות התרעה",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.Start))
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf("15%" to Color(0xFF55E6A5),"10%" to Color(0xFFFFC857),"5%" to Color(0xFFFF647C)).forEach{(v,c)->Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=Color(0xFF1B3045)),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(v,color=c,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("התראה",color=Color.LightGray,fontSize=12.sp)}}}}
     Spacer(Modifier.height(20.dp));Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF1B3045)),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("איך לקבל התראה?",color=Color.White,fontWeight=FontWeight.Bold);listOf("both" to "צליל + הודעה במסך","sound" to "צליל בלבד","screen" to "הודעה במסך בלבד").forEach{(v,l)->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(mode==v,{mode=v;prefs.edit().putString("mode",v).apply()});Text(l,color=Color.White)}}}}
     Spacer(Modifier.weight(1f));Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(active)"המעקב פעיל" else "המעקב מושהה",color=Color.White,fontWeight=FontWeight.Bold,fontSize=18.sp);Text("מעקב אוטומטי ברקע",color=Color.LightGray,fontSize=12.sp)};Switch(active,{active=it;prefs.edit().putBoolean("enabled",it).apply();val s=Intent(this@MainActivity,BatteryMonitorService::class.java);if(it){if(Build.VERSION.SDK_INT>=26)startForegroundService(s)else startService(s)}else stopService(s)})}
     Spacer(Modifier.height(12.dp));Text("yb apps",color=Color(0xFF71899D),fontSize=11.sp)
    }
   }
  }
  if(prefs.getBoolean("enabled",true)){val s=Intent(this,BatteryMonitorService::class.java);if(Build.VERSION.SDK_INT>=26)startForegroundService(s)else startService(s)}
 }
 private fun battery():Int=(getSystemService(BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0,100)
}