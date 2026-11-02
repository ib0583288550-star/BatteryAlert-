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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Mint = Color(0xFF42F5C5)
private val Cyan = Color(0xFF63D9FF)
private val Ink = Color(0xFF071522)
private val Panel = Color(0xFF10283A)
private val Muted = Color(0xFFA7C0D1)

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            var enabled by remember {
                mutableStateOf(getSharedPreferences("battery", MODE_PRIVATE).getBoolean("enabled", true))
            }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Mint, secondary = Cyan, background = Ink, surface = Panel
            )) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0xFF06121F), Color(0xFF0B2638), Color(0xFF102F42)))
                    )
                ) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("y.b apps  /  SMART TOOLS", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text("התרעת סוללה", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF15394A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BatteryChargingFull, null, tint = Mint, modifier = Modifier.size(29.dp))
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(30.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                Modifier.fillMaxWidth().background(
                                    Brush.linearGradient(listOf(Color(0xFF145C61), Color(0xFF123A52), Color(0xFF172C4A)))
                                ).padding(22.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Color(0x3328F0C0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.BatteryAlert, null, tint = Mint, modifier = Modifier.size(23.dp))
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text("שומרים על הסוללה שלך", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(18.dp))
                                    Text("בלי הפתעות.", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("מקבלים התראה בדיוק כשהסוללה נחלשת.", color = Color(0xFFD1E8F1), fontSize = 14.sp)
                                    Spacer(Modifier.height(20.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f).height(1.dp).background(Color(0x557FFFE0)))
                                        Spacer(Modifier.width(10.dp))
                                        Text("3 נקודות התרעה חכמות", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("מדרגות ההתרעה", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("התראה אוטומטית", color = Muted, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(12.dp))

                        ThresholdCard("15%", "כדאי להתחיל להטעין", "התראה מוקדמת", Cyan, "01")
                        Spacer(Modifier.height(9.dp))
                        ThresholdCard("10%", "הסוללה כבר נמוכה", "התראת אזהרה", Color(0xFFFFC66D), "02")
                        Spacer(Modifier.height(9.dp))
                        ThresholdCard("5%", "כדאי לחבר מטען עכשיו", "התראה קריטית", Color(0xFFFF7185), "03")

                        Spacer(Modifier.height(20.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Panel)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(17.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
                                        .background(if (enabled) Color(0x3328DDB0) else Color(0x33A7C0D1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.NotificationsActive, null, tint = if (enabled) Mint else Muted)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("מעקב ברקע", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(if (enabled) "פעיל ומוכן להתריע" else "המעקב מושהה", color = if (enabled) Mint else Muted, fontSize = 12.sp)
                                }
                                Switch(checked = enabled, onCheckedChange = {
                                    enabled = it
                                    getSharedPreferences("battery", MODE_PRIVATE).edit().putBoolean("enabled", it).apply()
                                    val serviceIntent = Intent(this@MainActivity, BatteryMonitorService::class.java)
                                    if (it) {
                                        if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
                                    } else stopService(serviceIntent)
                                })
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x3318B98E)).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, null, tint = Mint, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(9.dp))
                            Text("המעקב ממשיך גם כשהאפליקציה ברקע", color = Color(0xFFD2E7EF), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Mint, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("נוצר עם", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("y.b apps", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
        if (getSharedPreferences("battery", MODE_PRIVATE).getBoolean("enabled", true)) {
            val serviceIntent = Intent(this, BatteryMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
        }
    }
}

@Composable
private fun ThresholdCard(percent: String, description: String, label: String, accent: Color, number: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Text(percent, color = accent, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(description, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(label, color = Muted, fontSize = 11.sp)
            }
            Text(number, color = Color(0xFF668398), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
