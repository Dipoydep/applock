package com.applock.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.app.usage.UsageStatsManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.applock.app.R
import com.applock.app.data.PinManager
import com.applock.app.ui.screens.LockActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppLockService : Service() {

    companion object {
        private const val CHANNEL_ID = "applock_service_channel"
        private const val NOTIF_ID = 1001
        private const val CHECK_INTERVAL_MS = 500L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var pinManager: PinManager
    private var lastLockedPackage = ""
    private var checkRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        pinManager = PinManager(this)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        startMonitoring()
    }

    private fun startMonitoring() {
        checkRunnable = object : Runnable {
            override fun run() {
                scope.launch {
                    checkForegroundApp()
                }
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
        handler.post(checkRunnable!!)
    }

    private suspend fun checkForegroundApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 5000,
            time
        )

        val foregroundApp = stats
            ?.filter { it.lastTimeUsed > 0 }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
            ?: return

        // Jangan lock diri sendiri atau lock screen
        if (foregroundApp == packageName) return
        if (foregroundApp == "com.applock.app") return

        val lockedApps = pinManager.getLockedApps().first()

        if (lockedApps.contains(foregroundApp) && foregroundApp != lastLockedPackage) {
            lastLockedPackage = foregroundApp
            showLockScreen(foregroundApp)
        } else if (!lockedApps.contains(foregroundApp)) {
            lastLockedPackage = ""
        }
    }

    private fun showLockScreen(packageName: String) {
        val pm = packageManager
        val appName = try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }

        val intent = Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LockActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(LockActivity.EXTRA_APP_NAME, appName)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AppLock Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "AppLock sedang aktif melindungi app kamu"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AppLock Aktif")
            .setContentText("App kamu sedang dilindungi")
            .setSmallIcon(R.drawable.ic_lock_notif)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        checkRunnable?.let { handler.removeCallbacks(it) }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
