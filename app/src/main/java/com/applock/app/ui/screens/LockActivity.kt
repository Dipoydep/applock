package com.applock.app.ui.screens

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.applock.app.data.PinManager
import com.applock.app.ui.theme.AppLockTheme

class LockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_NAME     = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appName    = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        val pinManager = PinManager(this)

        // Tidak bisa bypass dengan tombol back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Bawa user ke home screen daripada ke app yang dikunci
                val home = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_HOME)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(home)
            }
        })

        setContent {
            AppLockTheme {
                VerifyPinScreen(
                    pinManager = pinManager,
                    appName    = appName,
                    onVerified = {
                        finish()
                    }
                )
            }
        }
    }
}
