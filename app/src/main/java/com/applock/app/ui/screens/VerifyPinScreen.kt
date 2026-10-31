package com.applock.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applock.app.R
import com.applock.app.data.PinManager
import com.applock.app.ui.components.PinDots
import com.applock.app.ui.components.PinPad
import com.applock.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerifyPinScreen(
    pinManager: PinManager,
    onVerified: () -> Unit,
    appName: String = "",
    onForgotPin: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var pin        by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var shake      by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var locked     by remember { mutableStateOf(false) }

    val successAlpha by animateFloatAsState(
        targetValue = if (showSuccess) 1f else 0f,
        animationSpec = tween(300), label = "alpha"
    )
    val successScale by animateFloatAsState(
        targetValue = if (showSuccess) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    fun check() {
        locked = true
        statusText = "Memeriksa…"
        scope.launch {
            delay(300)
            val valid = pinManager.verifyPin(pin)
            if (valid) {
                showSuccess = true
                delay(700)
                onVerified()
            } else {
                shake = true
                statusText = "PIN salah, coba lagi"
                delay(700)
                shake = false
                pin = ""
                statusText = ""
                locked = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon gembok SVG
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(SurfaceDark2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = OrangeAccent,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = if (appName.isNotEmpty()) "Buka $appName" else "Masukan PIN Privasi",
                fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = TextPrimary, letterSpacing = 0.2.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "6 digit untuk masuk ke aplikasi",
                fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            PinDots(pin = pin, shake = shake, filledColor = OrangeAccent)

            Spacer(Modifier.height(48.dp))

            PinPad(
                pin = pin,
                onPinChange = { newPin ->
                    if (!locked) {
                        pin = newPin
                        statusText = ""
                        if (newPin.length == 6) check()
                    }
                }
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (statusText.contains("salah")) Color(0xFFFF453A) else OrangeAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(16.dp)
            )

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = { onForgotPin?.invoke() ?: run { statusText = "Hubungi admin untuk pemulihan" } }) {
                Text("Lupa PIN?", fontSize = 12.sp, color = Color(0xFF5A5A5E))
            }
        }

        // Overlay sukses
        if (showSuccess) {
            Box(
                modifier = Modifier.fillMaxSize().alpha(successAlpha).background(BgBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .scale(successScale).size(56.dp).clip(CircleShape)
                            .background(OrangeAccent.copy(alpha = 0.15f))
                            .border(2.dp, OrangeAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_circle),
                            contentDescription = null,
                            tint = OrangeAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Akses diberikan", fontSize = 14.sp, color = TextGray)
                }
            }
        }
    }
}
