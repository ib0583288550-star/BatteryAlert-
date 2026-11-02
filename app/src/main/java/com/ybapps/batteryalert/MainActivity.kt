package com.ybapps.batteryalert

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            var enabled by remember { mutableStateOf(getSharedPreferences("battery", MODE_PRIVATE).getBoolean("enabled", true)) }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF24D6A2), secondary = Color(0xFF66E0FF),
                background = Color(0xFF081B2B), surface = Color(0xFF102A43)
            )) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF071827), Color(0xFF12334A))))) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(24.dp))
                        Icon(Icons.Default.BatteryChargingFull, null, tint = Color(0xFF24D6A2), modifier = Modifier.size(58.dp))
                        Text("התרעת סוללה", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("תמיד יודעים מתי הגיע הזמן להטעין", color = Color(0xFFB9D2E3), fontSize = 14.sp)
                        Spacer(Modifier.height(28.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF173B54)), shape = RoundedCornerShape(26.dp)) {
                            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("התרעות הסוללה שלך", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(18.dp))
                                listOf("15%" to "התרעה ראשונה", "10%" to "הסוללה נמוכה", "5%" to "טעינה דחופה").forEachIndexed { index, item ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.first, color = listOf(Color(0xFF66E0FF), Color(0xFFFFC857), Color(0xFFFF6B6B))[index], fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(16.dp))
                                        Text(item.second, color = Color.White, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF173B54)), shape = RoundedCornerShape(22.dp)) {
                            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFF24D6A2), modifier = Modifier.size(30.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("מעקב אחר הסוללה", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(if (enabled) "המעקב פעיל" else "המעקב מושהה", color = Color(0xFFB9D2E3), fontSize = 13.sp)
                                }
                                Switch(checked = enabled, onCheckedChange = {
                                    enabled = it
                                    getSharedPreferences("battery", MODE_PRIVATE).edit().putBoolean("enabled", it).apply()
                                    val intent = Intent(this@MainActivity, BatteryMonitorService::class.java)
                                    if (it) {
                                        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
                                    } else stopService(intent)
                                })
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("y.b apps", color = Color(0xFF7FA5BB), fontSize = 12.sp)
                    }
                }
            }
        }
        if (getSharedPreferences("battery", MODE_PRIVATE).getBoolean("enabled", true)) {
            val intent = Intent(this, BatteryMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        }
    }
}
