package com.example.englishapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.englishapp.model.AvatarMood
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioWaveform(
  mood: AvatarMood,
  rmsLevel: Float,
  activeColor: Color,
  idleColor: Color,
  modifier: Modifier = Modifier,
  height: Dp = 32.dp,
  barCount: Int = 22,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "waveAnim")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.283185f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "wavePhase",
  )

  val smoothRms by animateFloatAsState(
    targetValue = rmsLevel.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 80),
    label = "waveRms",
  )

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(height),
  ) {
    val w = size.width
    val h = size.height
    if (barCount <= 0 || w <= 0f) return@Canvas

    val slotWidth = w / barCount.toFloat()
    val barWidth = (slotWidth * 0.46f).coerceIn(3f, 10f)
    val mid = (barCount - 1) / 2f

    for (i in 0 until barCount) {
      val centerFalloff = 1f - (abs(i - mid) / (mid + 1f)) * 0.65f
      val normalizedAmp = when (mood) {
        AvatarMood.LISTENING -> {
          val ripple = abs(sin(phase * 1.4f + i * 0.55f)) * 0.35f
          (0.18f + (smoothRms * 0.72f + ripple * (0.35f + smoothRms)) * centerFalloff)
            .coerceIn(0.14f, 1f)
        }
        AvatarMood.SPEAKING -> {
          val harmonic = abs(sin(phase * 1.3f + i * 0.48f)) * 0.55f +
            abs(sin(phase * 0.8f - i * 0.32f)) * 0.35f
          (0.20f + harmonic * centerFalloff).coerceIn(0.15f, 0.96f)
        }
        AvatarMood.THINKING -> {
          val wave = sin(phase + i * 0.42f) * 0.5f + 0.5f
          (0.14f + wave * 0.32f * centerFalloff)
        }
        AvatarMood.IDLE -> {
          val gentle = sin(phase * 0.5f + i * 0.35f) * 0.5f + 0.5f
          0.12f + gentle * 0.08f * centerFalloff
        }
      }

      val barH = (h * normalizedAmp).coerceAtLeast(barWidth)
      val x = i * slotWidth + (slotWidth - barWidth) / 2f
      val y = (h - barH) / 2f
      val color = when (mood) {
        AvatarMood.IDLE -> idleColor.copy(alpha = 0.45f)
        else -> activeColor.copy(alpha = 0.55f + 0.45f * normalizedAmp)
      }
      drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(barWidth, barH),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
      )
    }
  }
}
