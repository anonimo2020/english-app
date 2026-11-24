package com.example.englishapp.ui.components

import com.example.englishapp.model.AvatarMood

/** Pixel-locked facial expression layers composited on top of each tutor's calm idle portrait. */
enum class TutorExpression {
  IDLE,
  BLINK,
  SPEAK_SOFT,
  SPEAK_OPEN,
  HAPPY,
}

/** Continuous blend alphas (`0f..1f`) driving silky, non-abrupt facial animation over IDLE. */
data class TutorExpressionWeights(
  val blinkAlpha: Float = 0f,
  val speakSoftAlpha: Float = 0f,
  val speakOpenAlpha: Float = 0f,
  val happyAlpha: Float = 0f,
)

/**
 * Resolves smooth continuous facial blend weights for blinking, gentle listening warmth,
 * celebratory smiles, and natural conversational mouth articulation.
 */
object TutorExpressionEngine {

  private val openVowels = setOf('a', 'e', 'o', 'á', 'é', 'ó')
  private val narrowVowelsAndBilabials = setOf('i', 'u', 'í', 'ú', 'm', 'p', 'b')
  private val clausePauseChars = setOf(',', '.', ';', ':', '!', '?', '…')

  private val fallbackOpennessCurve = listOf(0.42f, 0.78f, 0.52f, 0.88f, 0.22f, 0.65f, 0.48f, 0.0f)

  /**
   * Computes continuous mouth openness (`0f..1f`) for the active speech word slice and phase tick.
   * Uses gentle conversational modulation rather than harsh full-mouth snapping.
   */
  fun mouthOpennessForSpeech(
    text: String,
    range: IntRange?,
    phaseStep: Int,
  ): Float {
    val safeStep = phaseStep.coerceAtLeast(0)
    if (text.isBlank() || range == null || range.first !in text.indices) {
      return fallbackOpennessCurve[safeStep % fallbackOpennessCurve.size]
    }

    val end = range.last.coerceAtMost(text.lastIndex)
    val slice = text.substring(range.first, end + 1).trim()
    if (slice.isEmpty() || slice.all { it in clausePauseChars }) {
      return 0f
    }
    if (slice.last() in clausePauseChars && safeStep % 3 == 2) {
      return 0.08f
    }

    val lower = slice.lowercase()
    val openCount = lower.count { it in openVowels }
    val narrowCount = lower.count { it in narrowVowelsAndBilabials }

    return when {
      openCount > narrowCount -> if (safeStep % 2 == 0) 0.88f else 0.68f
      narrowCount > 0 -> when (safeStep % 3) {
        0 -> 0.45f
        1 -> 0.62f
        else -> 0.32f
      }
      else -> if (safeStep % 2 == 0) 0.50f else 0.72f
    }
  }

  /** Resolves continuous layer alphas from the tutor's conversational state. */
  fun resolveWeights(
    mood: AvatarMood,
    isBlinking: Boolean,
    isCelebrating: Boolean,
    speakingText: String,
    spokenCharRange: IntRange?,
    speechPhaseStep: Int,
  ): TutorExpressionWeights = when (mood) {
    AvatarMood.SPEAKING -> {
      val openness = mouthOpennessForSpeech(speakingText, spokenCharRange, speechPhaseStep)
      val soft = (openness * 1.55f).coerceIn(0f, 1f)
      val open = ((openness - 0.40f) / 0.60f).coerceIn(0f, 0.82f)
      val blink = if (isBlinking && openness < 0.35f) 1f else 0f
      TutorExpressionWeights(
        blinkAlpha = blink,
        speakSoftAlpha = soft,
        speakOpenAlpha = open,
        happyAlpha = 0f,
      )
    }
    AvatarMood.LISTENING -> TutorExpressionWeights(
      blinkAlpha = if (isBlinking) 1f else 0f,
      speakSoftAlpha = 0f,
      speakOpenAlpha = 0f,
      happyAlpha = 0.22f,
    )
    AvatarMood.THINKING -> TutorExpressionWeights(
      blinkAlpha = if (isBlinking) 1f else 0f,
      speakSoftAlpha = 0f,
      speakOpenAlpha = 0f,
      happyAlpha = 0f,
    )
    AvatarMood.IDLE -> TutorExpressionWeights(
      blinkAlpha = if (isBlinking && !isCelebrating) 1f else 0f,
      speakSoftAlpha = 0f,
      speakOpenAlpha = 0f,
      happyAlpha = if (isCelebrating) 0.90f else 0f,
    )
  }

  /** Calm human inter-blink delay in ms (periodic natural double-blink every 6th cycle). */
  fun nextBlinkDelayMs(blinkCount: Int): Long {
    val cycle = blinkCount.coerceAtLeast(0)
    if (cycle > 0 && cycle % 6 == 0) return 280L
    return when (cycle % 4) {
      0 -> 4200L
      1 -> 5600L
      2 -> 3600L
      else -> 4900L
    }
  }
}
