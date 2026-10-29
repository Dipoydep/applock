package com.applock.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applock.app.ui.theme.*

/* ─── Titik-titik PIN ─── */
@Composable
fun PinDots(
    pin: String,
    maxLength: Int = 6,
    shake: Boolean = false,
    filledColor: Color = OrangeAccent
) {
    val offsetX by animateFloatAsState(
        targetValue = if (shake) 1f else 0f,
        animationSpec = if (shake) keyframes {
            durationMillis = 400
            0f  at 0
            -8f at 80
            8f  at 160
            -5f at 240
            5f  at 320
            0f  at 400
        } else spring(),
        label = "shake"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.offset(x = offsetX.dp)
    ) {
        repeat(maxLength) { i ->
            val filled = i < pin.length
            val dotScale by animateFloatAsState(
                targetValue = if (filled) 1.05f else 1f,
                label = "dot_scale"
            )
            Box(
                modifier = Modifier
                    .scale(dotScale)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (filled) filledColor else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (filled) filledColor else BorderGray,
                        shape = CircleShape
                    )
            )
        }
    }
}

/* ─── Numpad bulat ─── */
@Composable
fun PinPad(
    pin: String,
    onPinChange: (String) -> Unit,
    maxLength: Int = 6
) {
    val haptic = LocalHapticFeedback.current

    val rows = listOf(
        listOf("1","2","3"),
        listOf("4","5","6"),
        listOf("7","8","9"),
        listOf("","0","DEL")
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(72.dp))

                        "DEL" -> Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (pin.isNotEmpty()) onPinChange(pin.dropLast(1))
                                },
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Backspace,
                                    contentDescription = "Hapus",
                                    tint = TextGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        else -> {
                            var pressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (pressed) 0.94f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                                label = "key_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .scale(scale)
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (pin.length < maxLength) onPinChange(pin + key)
                                    },
                                    modifier = Modifier.size(72.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SurfaceDark,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
