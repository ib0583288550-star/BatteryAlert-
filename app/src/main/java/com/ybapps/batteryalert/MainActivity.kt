package com.ybapps.batteryalert

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private data class BatteryInfo(
    val level: Int,
    val plugged: Boolean,
    val charging: Boolean,
    val currentMa: Int,
    val watts: Double,
    val eta: String
)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("battery_alert", MODE_PRIVATE) }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private var batteryReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            var active by remember { mutableStateOf(prefs.getBoolean("enabled", true)) }
            var mode by remember { mutableStateOf(prefs.getString("mode", "both") ?: "both") }
            var lockedAlert by remember { mutableStateOf(prefs.getBoolean("locked_alert", false)) }
            var emergency by remember { mutableStateOf(prefs.getBoolean("emergency_alerts", false)) }
            var info by remember { mutableStateOf(readBatteryInfo(null)) }

            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        info = readBatteryInfo(intent)
                    }
                }
                batteryReceiver = receiver
                registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                onDispose {
                    try { unregisterReceiver(receiver) } catch (_: Exception) {}
                    batteryReceiver = null
                }
            }

            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF55E6A5), background = Color(0xFF0B1220), surface = Color(0xFF1B3045))) {
                Column(
                    Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF12333A), Color(0xFF0B1220))))
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(18.dp))
                    Icon(Icons.Default.BatteryChargingFull, null, tint = Color(0xFF55E6A5), modifier = Modifier.size(48.dp))
                    Text("התרעת סוללה", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("לא תיתפס בלי סוללה", color = Color(0xFF9DB4C9))
                    Spacer(Modifier.height(22.dp))

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3045)), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("רמת הסוללה כרגע", color = Color.LightGray)
                            Text("${info.level}%", fontSize = 58.sp, fontWeight = FontWeight.ExtraBold, color = if (info.level <= 15) Color(0xFFFFC857) else Color(0xFF55E6A5))
                            LinearProgressIndicator(progress = { info.level / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(if (info.plugged) "מחובר למטען" else "לא מחובר למטען", color = if (info.plugged) Color(0xFF55E6A5) else Color(0xFFFFC857), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(if (info.charging) "הסוללה נטענת" else if (info.plugged) "מחובר – הטעינה אינה פעילה כרגע" else "הסוללה אינה נטענת", color = Color.LightGray, fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("זרם", color = Color.LightGray, fontSize = 12.sp)
                                    Text(if (info.currentMa > 0) "${info.currentMa} mA" else "לא זמין", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("הספק", color = Color.LightGray, fontSize = 12.sp)
                                    Text(if (info.watts > 0.01) String.format(Locale.US, "%.2f W", info.watts) else "לא זמין", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("זמן עד מלא", color = Color.LightGray, fontSize = 12.sp)
                                    Text(info.eta, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    Text("נקודות התרעה", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("15%" to Color(0xFF55E6A5), "10%" to Color(0xFFFFC857), "5%" to Color(0xFFFF647C)).forEach { (v, c) ->
                            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3045)), shape = RoundedCornerShape(18.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(v, color = c, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                    Text("התראה", color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3045)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("התראות חירום 4%–1%", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("התראה נוספת בכל אחוז: 4%, 3%, 2% ו־1%", color = Color.LightGray, fontSize = 12.sp)
                            }
                            Switch(emergency, {
                                emergency = it
                                prefs.edit().putBoolean("emergency_alerts", it).apply()
                            })
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3045)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("איך לקבל התרעה?", color = Color.White, fontWeight = FontWeight.Bold, style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl))
                            listOf("both" to "צליל + הודעה במסך", "sound" to "צליל בלבד", "screen" to "הודעה במסך בלבד").forEach { (v, label) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(mode == v, { mode = v; prefs.edit().putString("mode", v).apply() })
                                    Text(label, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3045)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("הודעה שלא נעלמת", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("ההודעה תישאר עד לחיבור הטלפון למטען", color = Color.LightGray, fontSize = 12.sp)
                            }
                            Switch(lockedAlert, {
                                lockedAlert = it
                                prefs.edit().putBoolean("locked_alert", it).apply()
                            })
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3045)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("מעקב ברקע", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(if (active) "המעקב פעיל — כולל אחרי הפעלה מחדש" else "המעקב מושהה", color = Color.LightGray, fontSize = 12.sp)
                            }
                            Switch(active, {
                                active = it
                                prefs.edit().putBoolean("enabled", it).apply()
                                val service = Intent(this@MainActivity, BatteryMonitorService::class.java)
                                if (it) {
                                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
                                } else stopService(service)
                            })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("yb apps", color = Color(0xFF71899D), fontSize = 11.sp)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        if (prefs.getBoolean("enabled", true)) startMonitor()
    }

    private fun startMonitor() {
        val service = Intent(this, BatteryMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
    }

    private fun readBatteryInfo(intent: Intent?): BatteryInfo {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val i = intent ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)) ?: batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val plugged = (i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val currentUa = abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
        val currentMa = currentUa / 1000
        val voltageMv = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val watts = currentUa.toDouble() * voltageMv.toDouble() / 1_000_000_000.0
        val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val averageUa = abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE))
        val eta = if (charging && level in 1..99 && chargeCounter > 0 && averageUa > 0) {
            val remainingMah = chargeCounter.toDouble() * (100 - level) / level / 1000.0
            val minutes = (remainingMah / (averageUa / 1000.0) * 60.0).roundToInt()
            if (minutes > 0) String.format(Locale.getDefault(), "%d:%02d שעות", minutes / 60, minutes % 60) else "כמעט מלא"
        } else if (level >= 100) "מלאה" else "לא זמין"
        return BatteryInfo(level.coerceIn(0, 100), plugged, charging, currentMa, watts, eta)
    }
}
