package com.applock.app.ui.screens

import android.os.Bundle
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

        // Disable back — user wajib masukkan PIN
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* tidak bisa bypass */ }
        })

        setContent {
            AppLockTheme {
                VerifyPinScreen(
                    pinManager = pinManager,
                    appName    = appName,
                    onVerified = { finish() }
                )
            }
        }
    }
}
