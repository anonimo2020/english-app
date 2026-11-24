package com.example.englishapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.data.SpeakingAnalysis
import com.example.englishapp.data.WordMatchStatus
import com.example.englishapp.ui.theme.FluentTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeakingOrbVisualizer(
  isListening: Boolean,
  isSpeaking: Boolean,
  passed: Boolean,
  rmsLevel: Float,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 134.dp,
  accentColor: Color = MaterialTheme.colorScheme.primary,
) {
  val recordingColor = FluentTheme.colors.recording
  val successColor = FluentTheme.colors.success
  val activeColor = when {
    isListening -> recordingColor
    passed -> successColor
    else -> accentColor
  }

  val infinite = rememberInfiniteTransition(label = "orbAnim")
  val phase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "orbPhase",
  )
  val pulse by infinite.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.07f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "orbPulse",
  )
  val smoothRms by animateFloatAsState(
    targetValue = rmsLevel.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 80),
    label = "orbRms",
  )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val w = this.size.width
      val cx = w / 2f
      val cy = w / 2f
      val innerR = w * 0.27f

      // 3 Concentric Aurora Rings
      for (ring in 1..3) {
        val ringProgress = ((phase / (2f * PI.toFloat())) + ring * 0.33f) % 1f
        val boost = if (isListening) smoothRms * w * 0.08f else 0f
        val radius = innerR + ringProgress * (w * 0.21f) + boost
        val alpha = if (isListening || isSpeaking) {
          (1f - ringProgress) * 0.34f
        } else {
          0.10f * (4 - ring) / 3f
        }
        drawCircle(
          color = activeColor.copy(alpha = alpha),
          radius = radius.coerceAtMost(w * 0.49f),
          center = Offset(cx, cy),
          style = Stroke(width = w * 0.018f),
        )
      }

      // 28-bar radial audio spectrum orbiting center
      val barCount = 28
      val orbitR = innerR + w * 0.04f
      for (i in 0 until barCount) {
        val angle = (i.toFloat() / barCount) * (2.0 * PI)
        val wave = abs(sin(phase * 1.4 + i * 0.65)).toFloat()
        val amp = when {
          isListening -> 0.22f + smoothRms * 0.78f * (0.4f + wave * 0.6f)
          isSpeaking -> 0.18f + wave * 0.55f
          else -> 0.12f + wave * 0.08f
        }
        val barLen = w * 0.14f * amp
        val sx = cx + cos(angle).toFloat() * orbitR
        val sy = cy + sin(angle).toFloat() * orbitR
        val ex = cx + cos(angle).toFloat() * (orbitR + barLen)
        val ey = cy + sin(angle).toFloat() * (orbitR + barLen)
        drawLine(
          color = activeColor.copy(alpha = 0.45f + amp * 0.55f),
          start = Offset(sx, sy),
          end = Offset(ex, ey),
          strokeWidth = w * 0.022f,
          cap = StrokeCap.Round,
        )
      }
    }

    Surface(
      modifier = Modifier
        .size(size * 0.48f)
        .scale(if (isListening) pulse else 1f)
        .clip(CircleShape)
        .clickable(onClick = onClick),
      shape = CircleShape,
      color = activeColor,
      shadowElevation = 8.dp,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = when {
            isListening -> Icons.Rounded.Stop
            passed -> Icons.Rounded.CheckCircle
            else -> Icons.Rounded.Mic
          },
          contentDescription = if (isListening) "Enviar voz" else "Practicar speaking",
          tint = Color.White,
          modifier = Modifier.size(size * 0.22f),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeakingMasteryCard(
  targetPhrase: String,
  spanishHint: String? = null,
  tipEs: String? = null,
  analysis: SpeakingAnalysis?,
  isListeningThis: Boolean,
  audioState: AudioEngineState,
  passThreshold: Int = 75,
  allowSkipAfterRetries: Boolean = false,
  onListenNormal: () -> Unit,
  onListenSlow: () -> Unit,
  onListenWord: (String) -> Unit,
  onToggleRecord: () -> Unit,
  onSkipFallback: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  val successColor = FluentTheme.colors.success
  val warningColor = FluentTheme.colors.correction
  val errorColor = FluentTheme.colors.recording

  var tappedWordTip by remember(analysis?.attemptNumber, targetPhrase) {
    mutableStateOf<Pair<String, String>?>(null)
  }

  val passed = analysis?.passed == true
  val border = when {
    passed -> successColor.copy(alpha = 0.65f)
    analysis != null -> warningColor.copy(alpha = 0.55f)
    else -> FluentTheme.colors.cardBorder
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.3.dp, border),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Target phrase + optional Spanish meaning
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "\"$targetPhrase\"",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (!spanishHint.isNullOrBlank()) {
            Text(
              text = "ES: $spanishHint",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (!tipEs.isNullOrBlank()) {
            Text(
              text = "Tip previo: $tipEs",
              style = MaterialTheme.typography.labelSmall,
              color = warningColor,
            )
          }
        }
      }

      // Listen buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onListenNormal,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Escuchar")
        }
        OutlinedButton(
          onClick = onListenSlow,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.SlowMotionVideo,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("0.65x Lento")
        }
      }

      // Orbital Visualizer + Recording Prompt
      SpeakingOrbVisualizer(
        isListening = isListeningThis,
        isSpeaking = audioState.isSpeaking,
        passed = passed,
        rmsLevel = audioState.rmsNormalized,
        onClick = onToggleRecord,
        size = 132.dp,
        accentColor = primary,
      )

      Text(
        text = when {
          isListeningThis && audioState.partialTranscript.isNotBlank() ->
            "\"${audioState.partialTranscript}\" (toca Stop al terminar)"
          isListeningThis ->
            "Escuchando... pronuncia la frase completa y toca Stop"
          passed ->
            "¡Dominada! Puntaje mínimo requerido: $passThreshold%"
          analysis != null ->
            "Necesitas al menos $passThreshold% para dominarla. Toca el orbe y repite."
          else ->
            "Toca el micrófono para practicar (meta: $passThreshold%+)"
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (isListeningThis) errorColor else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      audioState.lastError?.let { err ->
        Text(
          text = err,
          style = MaterialTheme.typography.bodySmall,
          color = errorColor,
          textAlign = TextAlign.Center,
        )
      }

      // Analysis & Coaching Feedback
      AnimatedVisibility(visible = analysis != null) {
        if (analysis != null) {
          val scoreColor = when {
            analysis.passed -> successColor
            analysis.overallScore >= 55 -> warningColor
            else -> errorColor
          }
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp),
              color = scoreColor.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.45f)),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                DailyGoalRing(
                  todayXp = analysis.overallScore,
                  goalXp = 100,
                  size = 54.dp,
                  ringColor = scoreColor,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = analysis.headlineEs,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scoreColor,
                  )
                  Text(
                    text = "Escuchamos: \"${analysis.spoken}\" · Intento #${analysis.attemptNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            // Word-by-word tappable breakdown
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Análisis palabra por palabra (toca cualquiera para oírla):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                analysis.wordBreakdown.forEach { wf ->
                  val chipColor = when (wf.status) {
                    WordMatchStatus.EXACT -> successColor
                    WordMatchStatus.CLOSE -> warningColor
                    WordMatchStatus.MISSED -> errorColor
                  }
                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .clickable {
                        onListenWord(wf.word)
                        tappedWordTip = wf.word to (
                          wf.tipEs ?: when (wf.status) {
                            WordMatchStatus.EXACT -> "Bien pronunciada en este intento."
                            WordMatchStatus.CLOSE -> "Sonó parecida; pronuncia sus vocales más claras."
                            WordMatchStatus.MISSED -> "No se escuchó clara; practícala despacio."
                          }
                        )
                      },
                    shape = RoundedCornerShape(10.dp),
                    color = chipColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, chipColor.copy(alpha = 0.45f)),
                  ) {
                    Text(
                      text = wf.word,
                      style = MaterialTheme.typography.labelMedium,
                      color = chipColor,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                  }
                }
              }
            }

            tappedWordTip?.let { (word, tip) ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = primary.copy(alpha = 0.12f),
              ) {
                Text(
                  text = "Palabra '$word': $tip",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(10.dp),
                )
              }
            }

            // Coaching Tips Box
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = warningColor,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = "Cómo mejorar",
                    style = MaterialTheme.typography.labelMedium,
                    color = warningColor,
                  )
                }
                analysis.actionableTipsEs.forEach { tip ->
                  Text(
                    text = "• $tip",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }

            if (!analysis.passed) {
              Button(
                onClick = onToggleRecord,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = warningColor,
                  contentColor = Color.Black,
                ),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Refresh,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Repetir para superar $passThreshold% (Intento #${analysis.attemptNumber + 1})")
              }

              if (allowSkipAfterRetries && analysis.attemptNumber >= 3 && onSkipFallback != null) {
                TextButton(
                  onClick = onSkipFallback,
                  modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                  Text("Continuar por ahora (ya intentaste ${analysis.attemptNumber} veces)")
                }
              }
            }
          }
        }
      }
    }
  }
}
