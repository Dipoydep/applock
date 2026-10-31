package com.applock.app.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.applock.app.R
import com.applock.app.data.PinManager
import com.applock.app.ui.screens.LockActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppLockService : Service() {

    companion object {
        private const val CHANNEL_ID    = "applock_channel"
        private const val NOTIF_ID      = 1001
        private const val CHECK_MS      = 300L  // lebih cepat = lebih responsive
    }

    private val scope   = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var pinManager: PinManager
    private lateinit var usageManager: UsageStatsManager

    // Track state biar tidak spam launch LockActivity
    private var lastForeground  = ""
    private var lastLocked      = ""
    private var isShowingLock   = false

    private val checker = object : Runnable {
        override fun run() {
            scope.launch { checkAndLock() }
            handler.postDelayed(this, CHECK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        pinManager   = PinManager(this)
        usageManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        handler.post(checker)
    }

    private suspend fun checkAndLock() {
        val now       = System.currentTimeMillis()
        val stats     = usageManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 3000, now)
        val current   = stats
            ?.filter { it.lastTimeUsed > 0 && it.packageName != packageName }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName ?: return

        // Reset flag kalau user sudah pindah app
        if (current != lastForeground) {
            lastForeground = current
            isShowingLock  = false
        }

        // Jangan lock kalau sedang di lock screen atau app kita sendiri
        if (current == packageName) return
        if (current.startsWith("com.applock")) return

        val locked = pinManager.getLockedApps().first()

        if (locked.contains(current) && !isShowingLock) {
            isShowingLock = true
            lastLocked    = current
            withContext(Dispatchers.Main) { showLock(current) }
        }
    }

    fun onUnlocked() {
        // Dipanggil dari LockActivity setelah PIN benar
        isShowingLock = false
    }

    private fun showLock(pkg: String) {
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) { pkg }

        startActivity(Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LockActivity.EXTRA_PACKAGE_NAME, pkg)
            putExtra(LockActivity.EXTRA_APP_NAME, appName)
        })
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "AppLock", NotificationManager.IMPORTANCE_LOW).apply {
            description = "AppLock aktif di background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotif() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("AppLock Aktif")
        .setContentText("Melindungi app kamu")
        .setSmallIcon(R.drawable.ic_lock_notif)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checker)
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
