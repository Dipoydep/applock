package com.applock.app.ui.navigation

import android.app.Activity
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.applock.app.data.PinManager
import com.applock.app.ui.screens.*
import kotlinx.coroutines.runBlocking

sealed class Screen(val route: String) {
    object SetPin : Screen("set_pin")
    object VerifyPin : Screen("verify_pin")
    object Home : Screen("home")
    object AppSelector : Screen("app_selector")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    pinManager: PinManager,
    activity: Activity
) {
    val hasPin = remember { runBlocking { pinManager.hasPin() } }
    val startDest = if (hasPin) Screen.VerifyPin.route else Screen.SetPin.route

    NavHost(navController = navController, startDestination = startDest) {

        composable(Screen.SetPin.route) {
            SetPinScreen(
                pinManager = pinManager,
                onPinSet = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SetPin.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.VerifyPin.route) {
            VerifyPinScreen(
                pinManager = pinManager,
                onVerified = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.VerifyPin.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                activity = activity,
                pinManager = pinManager,
                onNavigateToApps = {
                    navController.navigate(Screen.AppSelector.route)
                }
            )
        }

        composable(Screen.AppSelector.route) {
            AppSelectorScreen(
                activity = activity,
                pinManager = pinManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
