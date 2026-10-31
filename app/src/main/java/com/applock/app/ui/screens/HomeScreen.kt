package com.applock.app.ui.screens

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applock.app.R
import com.applock.app.data.PinManager
import com.applock.app.service.AppLockService
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    activity: Activity,
    pinManager: PinManager,
    onNavigateToApps: () -> Unit
) {
    var lockedCount by remember { mutableStateOf(0) }
    var serviceOn   by remember {
        mutableStateOf(isAppLockServiceRunning(activity) ||
            activity.getSharedPreferences("applock_state", Activity.MODE_PRIVATE)
                .getBoolean("protection_enabled", false))
    }
    var hasUsage    by remember { mutableStateOf(false) }
    var hasOverlay  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        pinManager.getLockedApps().collectLatest { lockedCount = it.size }
    }
    LaunchedEffect(Unit) {
        hasUsage   = checkUsage(activity)
        hasOverlay = Settings.canDrawOverlays(activity)
    }

    // Sinkronkan switch setiap kali pengguna kembali ke AppLock.
    // Jika perlindungan sebelumnya aktif tetapi service mati, hidupkan kembali.
    DisposableEffect(activity) {
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(resumedActivity: Activity) {
                if (resumedActivity !== activity) return

                val prefs = activity.getSharedPreferences("applock_state", Activity.MODE_PRIVATE)
                val shouldProtect = prefs.getBoolean("protection_enabled", false)
                val running = isAppLockServiceRunning(activity)

                if (shouldProtect && !running) {
                    activity.startForegroundService(
                        Intent(activity, AppLockService::class.java)
                    )
                }
                serviceOn = shouldProtect || running
            }

            override fun onActivityCreated(a: Activity, state: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, state: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        }

        activity.application.registerActivityLifecycleCallbacks(callbacks)
        onDispose {
            activity.application.unregisterActivityLifecycleCallbacks(callbacks)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0xFF2C2C2E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = Color(0xFFFF5C35),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("AppLock", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE5E5E5))
                Text("$lockedCount app terkunci", fontSize = 12.sp, color = Color(0xFF666666))
            }
        }

        Divider(color = Color(0xFF1C1C1E), thickness = 1.dp)

        // Toggle service
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (serviceOn) Color(0xFFFF5C35).copy(alpha = 0.15f)
                            else Color(0xFF1C1C1E)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield),
                        contentDescription = null,
                        tint = if (serviceOn) Color(0xFFFF5C35) else Color(0xFF666666),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (serviceOn) "Perlindungan Aktif" else "Perlindungan Nonaktif",
                        fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFFE5E5E5)
                    )
                    Text(
                        if (serviceOn) "Semua app terkunci terjaga" else "Nyalakan untuk mulai mengunci",
                        fontSize = 11.sp, color = Color(0xFF666666)
                    )
                }
                Switch(
                    checked = serviceOn,
                    onCheckedChange = { on ->
                        if (on) {
                            if (!hasUsage) {
                                activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            } else if (!hasOverlay) {
                                activity.startActivity(Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${activity.packageName}")
                                ))
                            } else {
                                activity.getSharedPreferences("applock_state", Activity.MODE_PRIVATE)
                                    .edit().putBoolean("protection_enabled", true).apply()
                                activity.startForegroundService(Intent(activity, AppLockService::class.java))
                                serviceOn = true
                            }
                        } else {
                            activity.getSharedPreferences("applock_state", Activity.MODE_PRIVATE)
                                .edit().putBoolean("protection_enabled", false).apply()
                            activity.stopService(Intent(activity, AppLockService::class.java))
                            serviceOn = false
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = Color(0xFFFF5C35),
                        uncheckedThumbColor = Color(0xFF8E8E93),
                        uncheckedTrackColor = Color(0xFF2C2C2E)
                    )
                )
            }
        }

        // Warning izin
        if (!hasUsage) PermissionBanner(
            title = "Izin Usage Access",
            desc  = "Dibutuhkan untuk mendeteksi app aktif"
        ) { activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }

        if (!hasOverlay) PermissionBanner(
            title = "Izin Overlay",
            desc  = "Dibutuhkan untuk tampilkan layar PIN"
        ) {
            activity.startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            ))
        }

        Button(
            onClick = onNavigateToApps,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5C35), contentColor = Color.White)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_apps),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("Pilih App yang Dikunci", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cara kerja", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFFE5E5E5))
                Spacer(Modifier.height(12.dp))
                StepRow("1", "Nyalakan perlindungan di atas")
                Spacer(Modifier.height(8.dp))
                StepRow("2", "Pilih app yang ingin dikunci")
                Spacer(Modifier.height(8.dp))
                StepRow("3", "Setiap buka app itu, PIN diminta")
            }
        }
    }
}

@Composable
private fun PermissionBanner(title: String, desc: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1010))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = Color(0xFFFF453A),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFF453A))
                Text(desc, fontSize = 11.sp, color = Color(0xFF666666))
            }
            TextButton(onClick = onClick) {
                Text("Beri Izin", fontSize = 12.sp, color = Color(0xFFFF5C35))
            }
        }
    }
}

@Composable
private fun StepRow(num: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFFF5C35).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text(num, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5C35)) }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFF8E8E93))
    }
}

private fun checkUsage(activity: Activity): Boolean = try {
    val am = activity.getSystemService(android.app.AppOpsManager::class.java)
    am.checkOpNoThrow(
        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(), activity.packageName
    ) == android.app.AppOpsManager.MODE_ALLOWED
} catch (e: Exception) { false }


private fun isAppLockServiceRunning(activity: Activity): Boolean {
    return try {
        val manager = activity.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == AppLockService::class.java.name
        }
    } catch (_: Exception) {
        false
    }
}
