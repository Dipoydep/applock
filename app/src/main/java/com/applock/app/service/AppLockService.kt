package com.applock.app.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.applock.app.R
import com.applock.app.data.PinManager
import com.applock.app.ui.screens.LockActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean

class AppLockService : Service() {

    companion object {
        private const val CHANNEL_ID = "applock_channel"
        private const val NOTIF_ID = 1001
        private const val CHECK_MS = 300L
        private const val EVENT_LOOKBACK_MS = 10_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunning = AtomicBoolean(false)

    private lateinit var pinManager: PinManager
    private lateinit var usageManager: UsageStatsManager

    private var lastForeground = ""
    private var isShowingLock = false

    private val checker = object : Runnable {
        override fun run() {
            if (checkRunning.compareAndSet(false, true)) {
                scope.launch {
                    try {
                        checkAndLock()
                    } finally {
                        checkRunning.set(false)
                    }
                }
            }
            handler.postDelayed(this, CHECK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        pinManager = PinManager(this)
        usageManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        handler.removeCallbacks(checker)
        handler.post(checker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep the monitoring service independent from the Activity/Recent Apps task.
        return START_STICKY
    }

    /**
     * Clear All in Recent Apps removes the Activity task, not the user's protection setting.
     * The service is explicitly configured with stopWithTask=false in the manifest.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Do not stop the foreground service and do not clear protection state here.
    }

    private fun findForegroundPackage(now: Long): String? {
        val events = usageManager.queryEvents(now - EVENT_LOOKBACK_MS, now)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForegroundEvent =
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)

            if (isForegroundEvent && event.timeStamp >= latestTime) {
                latestTime = event.timeStamp
                latestPackage = event.packageName
            }
        }

        // Fallback for devices that do not return UsageEvents consistently.
        if (latestPackage == null) {
            val stats = usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - EVENT_LOOKBACK_MS,
                now
            )
            latestPackage = stats
                ?.filter { it.lastTimeUsed > 0L && it.packageName != packageName }
                ?.maxByOrNull { it.lastTimeUsed }
                ?.packageName
        }

        return latestPackage
    }

    private suspend fun checkAndLock() {
        val current = findForegroundPackage(System.currentTimeMillis()) ?: return

        if (current != lastForeground) {
            lastForeground = current
            isShowingLock = false
        }

        // Ignore AppLock itself and its PIN screen.
        if (current == packageName || current.startsWith("com.applock")) return

        val locked = pinManager.getLockedApps().first()
        if (locked.contains(current) && !isShowingLock) {
            isShowingLock = true
            withContext(Dispatchers.Main) {
                showLock(current)
            }
        }
    }

    private fun showLock(pkg: String) {
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (_: Exception) {
            pkg
        }

        startActivity(Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LockActivity.EXTRA_PACKAGE_NAME, pkg)
            putExtra(LockActivity.EXTRA_APP_NAME, appName)
        })
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AppLock",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "AppLock aktif di background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotif() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("AppLock Aktif")
        .setContentText("Melindungi app kamu")
        .setSmallIcon(R.drawable.ic_lock_notif)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        handler.removeCallbacks(checker)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
