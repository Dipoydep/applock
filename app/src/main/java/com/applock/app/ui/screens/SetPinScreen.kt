package com.applock.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applock.app.data.PinManager
import com.applock.app.ui.components.PinDots
import com.applock.app.ui.components.PinPad
import com.applock.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Stage: CREATE → CONFIRM → DONE
private enum class Stage { CREATE, CONFIRM, DONE }

@Composable
fun SetPinScreen(pinManager: PinManager, onPinSet: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stage   by remember { mutableStateOf(Stage.CREATE) }
    var pin     by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var subText  by remember { mutableStateOf("Masukan 6 digit PIN baru") }
    var subError by remember { mutableStateOf(false) }
    var shake    by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var locked   by remember { mutableStateOf(false) }

    val isConfirm = stage == Stage.CONFIRM
    val dotColor  = if (isConfirm) OrangeConfirm else OrangeAccent

    // Animasi overlay sukses
    val successAlpha by animateFloatAsState(
        targetValue = if (showSuccess) 1f else 0f,
        animationSpec = tween(300),
        label = "success_alpha"
    )
    val successScale by animateFloatAsState(
        targetValue = if (showSuccess) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "success_scale"
    )

    fun handleComplete() {
        locked = true
        scope.launch {
            when (stage) {
                Stage.CREATE -> {
                    firstPin = pin
                    delay(220)
                    pin = ""
                    stage = Stage.CONFIRM
                    subText = "Ulangi PIN untuk konfirmasi"
                    subError = false
                    locked = false
                }
                Stage.CONFIRM -> {
                    if (pin == firstPin) {
                        stage = Stage.DONE
                        subText = "PIN cocok"
                        delay(350)
                        showSuccess = true
                        pinManager.savePin(firstPin)
                        delay(1200)
                        onPinSet()
                    } else {
                        shake = true
                        subText = "PIN tidak cocok, ulangi"
                        subError = true
                        delay(700)
                        shake = false
                        pin = ""
                        subText = "Ulangi 6 digit PIN yang sama"
                        subError = false
                        locked = false
                    }
                }
                Stage.DONE -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
    ) {
        // Konten utama
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ikon gembok
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark2),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (stage == Stage.DONE) "✓" else "🔒",
                    fontSize = 22.sp,
                    color = if (stage == Stage.DONE) GreenSuccess else OrangeAccent
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = if (stage == Stage.CONFIRM) "Konfirmasi PIN" else "Buat PIN Privasi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                letterSpacing = 0.2.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = subText,
                fontSize = 12.sp,
                color = if (subError) Color(0xFFFF453A) else TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // Titik PIN
            PinDots(
                pin = pin,
                shake = shake,
                filledColor = dotColor
            )

            Spacer(Modifier.height(48.dp))

            // Numpad
            PinPad(
                pin = pin,
                onPinChange = { newPin ->
                    if (!locked) {
                        pin = newPin
                        subError = false
                        if (!isConfirm) subText = "Masukan 6 digit PIN baru"
                        else subText = "Ulangi PIN untuk konfirmasi"
                        if (newPin.length == 6) handleComplete()
                    }
                }
            )

            Spacer(Modifier.height(36.dp))

            // Step indicator — 2 bar
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { i ->
                    val isActive = (i == 0 && stage == Stage.CREATE) || (i == 1 && stage == Stage.CONFIRM)
                    val isDone   = (i == 0 && (stage == Stage.CONFIRM || stage == Stage.DONE)) ||
                                   (i == 1 && stage == Stage.DONE)
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    isDone   -> GreenSuccess
                                    isActive -> OrangeAccent
                                    else     -> Color(0xFF2C2C2E)
                                }
                            )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "PIN ini akan digunakan setiap kali membuka aplikasi",
                fontSize = 11.sp,
                color = Color(0xFF4A4A4E),
                textAlign = TextAlign.Center
            )
        }

        // Overlay sukses
        if (showSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(successAlpha)
                    .background(BgBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .scale(successScale)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(GreenSuccess.copy(alpha = 0.15f))
                            .border(2.dp, GreenSuccess, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", fontSize = 26.sp, color = GreenSuccess)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("PIN berhasil dibuat", fontSize = 14.sp, color = TextGray)
                }
            }
        }
    }
}
