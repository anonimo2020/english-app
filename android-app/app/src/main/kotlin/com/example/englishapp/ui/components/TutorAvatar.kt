package com.example.englishapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.englishapp.R
import com.example.englishapp.model.AvatarMood
import com.example.englishapp.model.TutorCharacter
import com.example.englishapp.ui.theme.FluentTheme
import kotlinx.coroutines.delay
import kotlin.math.sin

fun TutorCharacter.portraitDrawableRes(): Int = when (this) {
  TutorCharacter.MANU -> R.drawable.tutor_manu_portrait
  TutorCharacter.EDDY -> R.drawable.tutor_eddy_portrait
}

fun TutorCharacter.expressionDrawableRes(expression: TutorExpression): Int = when (this) {
  TutorCharacter.MANU -> when (expression) {
    TutorExpression.IDLE -> R.drawable.tutor_manu_portrait
    TutorExpression.BLINK -> R.drawable.tutor_manu_blink
    TutorExpression.SPEAK_SOFT -> R.drawable.tutor_manu_speak_mid
    TutorExpression.SPEAK_OPEN -> R.drawable.tutor_manu_speak_wide
    TutorExpression.HAPPY -> R.drawable.tutor_manu_happy
  }
  TutorCharacter.EDDY -> when (expression) {
    TutorExpression.IDLE -> R.drawable.tutor_eddy_portrait
    TutorExpression.BLINK -> R.drawable.tutor_eddy_blink
    TutorExpression.SPEAK_SOFT -> R.drawable.tutor_eddy_speak_mid
    TutorExpression.SPEAK_OPEN -> R.drawable.tutor_eddy_speak_wide
    TutorExpression.HAPPY -> R.drawable.tutor_eddy_happy
  }
}

@Composable
private fun rememberSmoothExpressionWeights(
  mood: AvatarMood,
  speakingText: String,
  spokenCharRange: IntRange?,
  isCelebrating: Boolean,
): TutorExpressionWeights {
  var isBlinking by remember { mutableStateOf(false) }
  var speechPhaseStep by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) {
    var cycle = 0
    while (true) {
      delay(TutorExpressionEngine.nextBlinkDelayMs(cycle))
      isBlinking = true
      delay(55L)
      isBlinking = false
      cycle++
    }
  }

  LaunchedEffect(mood) {
    speechPhaseStep = 0
    if (mood == AvatarMood.SPEAKING) {
      while (true) {
        delay(185L)
        speechPhaseStep++
      }
    }
  }

  val target = TutorExpressionEngine.resolveWeights(
    mood = mood,
    isBlinking = isBlinking,
    isCelebrating = isCelebrating,
    speakingText = speakingText,
    spokenCharRange = spokenCharRange,
    speechPhaseStep = speechPhaseStep,
  )

  val blinkAlpha by animateFloatAsState(
    targetValue = target.blinkAlpha,
    animationSpec = tween(
      durationMillis = if (target.blinkAlpha > 0.5f) 65 else 90,
      easing = FastOutSlowInEasing,
    ),
    label = "blinkAlphaSmooth",
  )
  val speakSoftAlpha by animateFloatAsState(
    targetValue = target.speakSoftAlpha,
    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
    label = "speakSoftAlphaSmooth",
  )
  val speakOpenAlpha by animateFloatAsState(
    targetValue = target.speakOpenAlpha,
    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
    label = "speakOpenAlphaSmooth",
  )
  val happyAlpha by animateFloatAsState(
    targetValue = target.happyAlpha,
    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
    label = "happyAlphaSmooth",
  )

  return TutorExpressionWeights(
    blinkAlpha = blinkAlpha,
    speakSoftAlpha = speakSoftAlpha,
    speakOpenAlpha = speakOpenAlpha,
    happyAlpha = happyAlpha,
  )
}

/**
 * Renders the pixel-locked calm IDLE portrait with continuously feathered facial layers
 * blended smoothly on top so hair, headphones, and background stay rock-solid.
 */
@Composable
private fun AnimatedExpressionPortrait(
  character: TutorCharacter,
  weights: TutorExpressionWeights,
  alignment: Alignment,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    Image(
      painter = painterResource(id = character.expressionDrawableRes(TutorExpression.IDLE)),
      contentDescription = character.displayName,
      contentScale = ContentScale.Crop,
      alignment = alignment,
      modifier = Modifier.fillMaxSize(),
    )

    val layers = listOf(
      TutorExpression.HAPPY to weights.happyAlpha,
      TutorExpression.SPEAK_SOFT to weights.speakSoftAlpha,
      TutorExpression.SPEAK_OPEN to weights.speakOpenAlpha,
      TutorExpression.BLINK to weights.blinkAlpha,
    )
    layers.forEach { (expression, layerAlpha) ->
      Image(
        painter = painterResource(id = character.expressionDrawableRes(expression)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = alignment,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = layerAlpha.coerceIn(0f, 1f) },
      )
    }
  }
}

@Composable
fun TutorAvatar(
  character: TutorCharacter,
  mood: AvatarMood,
  modifier: Modifier = Modifier,
  size: Dp = 68.dp,
  rmsLevel: Float = 0f,
  speakingText: String = "",
  spokenCharRange: IntRange? = null,
  isCelebrating: Boolean = false,
) {
  val accent = FluentTheme.colors.accentFor(character)
  val recordingColor = FluentTheme.colors.recording
  val infiniteTransition = rememberInfiniteTransition(label = "avatarHaloAnim")

  val pulsePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.283185f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "pulsePhase",
  )

  val spinAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1350, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "spinAngle",
  )

  val smoothRms by animateFloatAsState(
    targetValue = rmsLevel.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 85),
    label = "rmsSmooth",
  )

  val weights = rememberSmoothExpressionWeights(
    mood = mood,
    speakingText = speakingText,
    spokenCharRange = spokenCharRange,
    isCelebrating = isCelebrating,
  )

  val ringColor = if (mood == AvatarMood.LISTENING) recordingColor else accent

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val center = Offset(w / 2f, w / 2f)
      val baseRadius = w * 0.42f

      when (mood) {
        AvatarMood.LISTENING -> {
          val waveBoost = smoothRms * w * 0.08f
          drawCircle(
            color = ringColor.copy(alpha = 0.24f + smoothRms * 0.28f),
            radius = (baseRadius + w * 0.075f + waveBoost).coerceAtMost(w * 0.50f),
            center = center,
          )
          drawCircle(
            color = ringColor.copy(alpha = 0.90f),
            radius = (baseRadius + w * 0.032f).coerceAtMost(w * 0.48f),
            center = center,
            style = Stroke(width = w * 0.035f),
          )
        }
        AvatarMood.SPEAKING -> {
          val pulse = (sin(pulsePhase) + 1f) * 0.5f
          drawCircle(
            color = accent.copy(alpha = 0.16f + pulse * 0.20f),
            radius = baseRadius + w * (0.04f + pulse * 0.04f),
            center = center,
          )
          drawCircle(
            color = accent.copy(alpha = 0.85f),
            radius = baseRadius + w * 0.018f,
            center = center,
            style = Stroke(width = w * 0.03f),
          )
        }
        AvatarMood.THINKING -> {
          drawArc(
            color = accent,
            startAngle = spinAngle,
            sweepAngle = 115f,
            useCenter = false,
            style = Stroke(width = w * 0.04f, cap = StrokeCap.Round),
          )
        }
        AvatarMood.IDLE -> {
          drawCircle(
            color = accent.copy(alpha = 0.36f),
            radius = baseRadius + w * 0.012f,
            center = center,
            style = Stroke(width = w * 0.022f),
          )
        }
      }
    }

    AnimatedExpressionPortrait(
      character = character,
      weights = weights,
      alignment = BiasAlignment(0f, -0.34f),
      modifier = Modifier
        .fillMaxSize()
        .padding(size * 0.09f)
        .clip(CircleShape)
        .graphicsLayer {
          val sc = when (mood) {
            AvatarMood.SPEAKING -> 1.035f
            AvatarMood.LISTENING -> 1.03f + smoothRms * 0.015f
            else -> 1.02f
          }
          scaleX = sc
          scaleY = sc
        },
    )
  }
}

@Composable
fun TutorLiveBackdrop(
  character: TutorCharacter,
  mood: AvatarMood,
  rmsLevel: Float,
  modifier: Modifier = Modifier,
  speakingText: String = "",
  spokenCharRange: IntRange? = null,
  isCelebrating: Boolean = false,
) {
  val infinite = rememberInfiniteTransition(label = "livePortraitAnim")
  val subtleBreath by infinite.animateFloat(
    initialValue = 1.028f,
    targetValue = 1.042f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "breathZoom",
  )
  val idleSwayDeg by infinite.animateFloat(
    initialValue = -0.32f,
    targetValue = 0.32f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "idleSwayDeg",
  )
  val speakTiltDeg by infinite.animateFloat(
    initialValue = -0.55f,
    targetValue = 0.55f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "speakTiltDeg",
  )

  val smoothRms by animateFloatAsState(
    targetValue = rmsLevel.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 110),
    label = "liveRms",
  )

  val weights = rememberSmoothExpressionWeights(
    mood = mood,
    speakingText = speakingText,
    spokenCharRange = spokenCharRange,
    isCelebrating = isCelebrating,
  )

  val animatedRotation by animateFloatAsState(
    targetValue = when (mood) {
      AvatarMood.SPEAKING -> speakTiltDeg
      AvatarMood.LISTENING -> 0.45f
      AvatarMood.THINKING -> -0.35f
      AvatarMood.IDLE -> idleSwayDeg
    },
    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
    label = "liveHeadTilt",
  )

  val moodZoomBoost by animateFloatAsState(
    targetValue = when (mood) {
      AvatarMood.SPEAKING -> 0.008f
      AvatarMood.LISTENING -> 0.008f + smoothRms * 0.010f
      else -> if (isCelebrating) 0.010f else 0f
    },
    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
    label = "liveMoodZoom",
  )

  val activeZoom = subtleBreath + moodZoomBoost

  Box(modifier = modifier.fillMaxSize()) {
    AnimatedExpressionPortrait(
      character = character,
      weights = weights,
      alignment = BiasAlignment(0f, -0.22f),
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          scaleX = activeZoom
          scaleY = activeZoom
          rotationZ = animatedRotation
          translationY = this.size.height * (-0.0022f * weights.speakOpenAlpha)
        },
    )

    // Top studio vignette for crisp status + header readability
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colorStops = arrayOf(
              0.0f to Color(0xCC060916),
              0.18f to Color(0x44060916),
              0.52f to Color.Transparent,
              0.70f to Color(0x88070B19),
              1.0f to Color(0xEB050814),
            ),
          ),
        ),
    )
  }
}
