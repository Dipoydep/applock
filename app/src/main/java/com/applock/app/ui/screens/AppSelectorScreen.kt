package com.applock.app.ui.screens

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.applock.app.data.AppInfo
import com.applock.app.data.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorScreen(
    activity: Activity,
    pinManager: PinManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var apps       by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var lockedApps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var query      by remember { mutableStateOf("") }
    var isLoading  by remember { mutableStateOf(true) }
    var loadError  by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        pinManager.getLockedApps().collectLatest { lockedApps = it }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val pm = activity.packageManager
                // Pakai MATCH_ALL supaya semua app user terdeteksi
                val flags = PackageManager.GET_META_DATA
                val list = pm.getInstalledApplications(flags)
                    .filter { info ->
                        // Tampilkan semua aplikasi yang punya launcher,
                        // termasuk aplikasi bawaan/system yang bisa dibuka pengguna.
                        val launchIntent = pm.getLaunchIntentForPackage(info.packageName)
                        launchIntent != null &&
                        info.packageName != activity.packageName
                    }
                    .map { info ->
                        AppInfo(
                            packageName = info.packageName,
                            appName     = pm.getApplicationLabel(info).toString(),
                            icon        = pm.getApplicationIcon(info.packageName)
                        )
                    }
                    .sortedBy { it.appName.lowercase() }

                withContext(Dispatchers.Main) {
                    apps = list
                    isLoading = false
                    if (list.isEmpty()) loadError = "Tidak ada app yang ditemukan"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    loadError = "Gagal memuat: ${e.message}"
                }
            }
        }
    }

    val filtered = if (query.isEmpty()) apps
                   else apps.filter { it.appName.contains(query, ignoreCase = true) }

    Scaffold(
        containerColor = Color(0xFF000000),
        topBar = {
            TopAppBar(
                title = {
                    Text("Pilih App", fontWeight = FontWeight.SemiBold, color = Color(0xFFE5E5E5))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color(0xFFE5E5E5))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF000000))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari app...", color = Color(0xFF666666)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF666666)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = Color(0xFFFF5C35),
                    unfocusedBorderColor  = Color(0xFF2C2C2E),
                    focusedTextColor      = Color(0xFFE5E5E5),
                    unfocusedTextColor    = Color(0xFFE5E5E5),
                    cursorColor           = Color(0xFFFF5C35),
                    focusedContainerColor   = Color(0xFF1C1C1E),
                    unfocusedContainerColor = Color(0xFF1C1C1E)
                )
            )

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFFF5C35))
                            Spacer(Modifier.height(12.dp))
                            Text("Memuat daftar app...", color = Color(0xFF666666), fontSize = 13.sp)
                        }
                    }
                }
                loadError.isNotEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(loadError, color = Color(0xFFFF453A), fontSize = 13.sp)
                    }
                }
                else -> {
                    Text(
                        "${lockedApps.size} dari ${apps.size} app terkunci",
                        fontSize = 11.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            val isLocked = lockedApps.contains(app.packageName)
                            AppRow(
                                app      = app,
                                isLocked = isLocked,
                                onToggle = {
                                    scope.launch {
                                        if (isLocked) pinManager.removeLockedApp(app.packageName)
                                        else pinManager.addLockedApp(app.packageName)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, isLocked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isLocked) Color(0xFF1A0F0A) else Color(0xFF141414))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap: Bitmap? = remember(app.packageName) {
            runCatching { app.icon.toBitmap(48, 48) }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2C2C2E)))
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(app.appName, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFFE5E5E5), maxLines = 1)
            Text(app.packageName, fontSize = 10.sp, color = Color(0xFF3A3A3A), maxLines = 1)
        }

        Switch(
            checked = isLocked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = Color(0xFFFF5C35),
                uncheckedThumbColor = Color(0xFF3A3A3A),
                uncheckedTrackColor = Color(0xFF1C1C1E)
            )
        )
    }
}
