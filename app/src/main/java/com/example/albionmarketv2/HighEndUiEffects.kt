package com.example.albionmarketv2

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    this.clickable(enabled = enabled) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } catch (_: Exception) {}
        onClick()
    }
}

@Composable
fun ShimmerLoadingCard(modifier: Modifier = Modifier, height: Dp = 90.dp) {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val shimmerColors = listOf(
        Color(0xFF1E293B).copy(alpha = 0.6f),
        Color(0xFF334155).copy(alpha = 0.9f),
        Color(0xFF1E293B).copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
    )
}

@Composable
fun RainbowBlinkingText(
    textPrefix: String = "",
    rainbowName: String = "AlbionDataPro",
    textSuffix: String = "",
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight = FontWeight.ExtraBold
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RainbowBlink")
    val hueAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RainbowHue"
    )

    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    val rainbowColor = Color.hsv(hueAnim, 0.85f, 1.0f).copy(alpha = blinkAlpha)

    Text(
        text = buildAnnotatedString {
            append(textPrefix)
            append(rainbowName)
            append(textSuffix)
        },
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = rainbowColor,
        modifier = modifier
    )
}
