package com.applock.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.applock.app.data.PinManager
import com.applock.app.ui.navigation.AppNavGraph
import com.applock.app.ui.theme.AppLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            AppLockTheme {
                val navController = rememberNavController()
                val pinManager    = remember { PinManager(this) }
                AppNavGraph(
                    navController = navController,
                    pinManager    = pinManager,
                    activity      = this
                )
            }
        }
    }
}
