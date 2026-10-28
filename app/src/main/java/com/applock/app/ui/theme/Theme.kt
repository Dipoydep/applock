package com.applock.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OrangeAccent  = Color(0xFFFF5C35)
val OrangeConfirm = Color(0xFFFF9F0A)
val GreenSuccess  = Color(0xFF34C759)
val BgBlack       = Color(0xFF000000)
val SurfaceDark   = Color(0xFF1C1C1E)
val SurfaceDark2  = Color(0xFF141414)
val BorderGray    = Color(0xFF3A3A3A)
val TextPrimary   = Color(0xFFE5E5E5)
val TextMuted     = Color(0xFF666666)
val TextGray      = Color(0xFF8E8E93)

private val AppColorScheme = darkColorScheme(
    primary        = OrangeAccent,
    secondary      = OrangeConfirm,
    tertiary       = GreenSuccess,
    background     = BgBlack,
    surface        = SurfaceDark,
    onPrimary      = Color.White,
    onBackground   = TextPrimary,
    onSurface      = Color.White,
    error          = Color(0xFFFF453A)
)

@Composable
fun AppLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography(),
        content     = content
    )
}
