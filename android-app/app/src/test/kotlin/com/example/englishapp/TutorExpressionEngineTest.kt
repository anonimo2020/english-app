package com.example.englishapp

import com.example.englishapp.model.AvatarMood
import com.example.englishapp.ui.components.TutorExpressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorExpressionEngineTest {

  @Test
  fun mouthOpennessReflectsOpenVowelsNarrowVowelsAndClausePauses() {
    val sentence = "Hello! How are you doing today?"
    val openIdx = sentence.indexOf("are")
    val openValue = TutorExpressionEngine.mouthOpennessForSpeech(
      text = sentence,
      range = openIdx until (openIdx + 3),
      phaseStep = 0,
    )
    assertTrue("Expected open vowel score >= 0.68, got $openValue", openValue >= 0.68f)

    val narrowIdx = sentence.indexOf("doing")
    val narrowValue = TutorExpressionEngine.mouthOpennessForSpeech(
      text = sentence,
      range = narrowIdx until (narrowIdx + 5),
      phaseStep = 0,
    )
    assertTrue("Expected open vowel > narrow vowel ($openValue > $narrowValue)", openValue > narrowValue)

    val pauseIdx = sentence.indexOf("!")
    val pauseValue = TutorExpressionEngine.mouthOpennessForSpeech(
      text = sentence,
      range = pauseIdx..pauseIdx,
      phaseStep = 0,
    )
    assertEquals(0f, pauseValue, 0.001f)
  }

  @Test
  fun fallbackCurveVariesSmoothlyWhenNoCharacterRangeExists() {
    val samples = (0..7).map { step ->
      TutorExpressionEngine.mouthOpennessForSpeech("", null, step)
    }
    assertTrue(samples.max() > 0.7f)
    assertTrue(samples.min() == 0f)
    assertTrue(samples.any { it in 0.2f..0.66f })
  }

  @Test
  fun resolveWeightsLayersMouthsProgressivelyAndKeepsListeningCalm() {
    val wideWeights = TutorExpressionEngine.resolveWeights(
      mood = AvatarMood.SPEAKING,
      isBlinking = true,
      isCelebrating = false,
      speakingText = "great",
      spokenCharRange = 0..4,
      speechPhaseStep = 0,
    )
    assertTrue(wideWeights.speakSoftAlpha >= 0.9f)
    assertTrue(wideWeights.speakOpenAlpha in 0.2f..0.85f)
    assertEquals(0f, wideWeights.blinkAlpha, 0.001f)
    assertEquals(0f, wideWeights.happyAlpha, 0.001f)

    val listeningCalm = TutorExpressionEngine.resolveWeights(
      mood = AvatarMood.LISTENING,
      isBlinking = false,
      isCelebrating = false,
      speakingText = "",
      spokenCharRange = null,
      speechPhaseStep = 0,
    )
    assertEquals(0f, listeningCalm.speakSoftAlpha, 0.001f)
    assertEquals(0f, listeningCalm.speakOpenAlpha, 0.001f)
    assertTrue(listeningCalm.happyAlpha in 0.15f..0.3f)

    val listeningBlink = TutorExpressionEngine.resolveWeights(
      mood = AvatarMood.LISTENING,
      isBlinking = true,
      isCelebrating = false,
      speakingText = "",
      spokenCharRange = null,
      speechPhaseStep = 0,
    )
    assertEquals(1f, listeningBlink.blinkAlpha, 0.001f)

    val celebrating = TutorExpressionEngine.resolveWeights(
      mood = AvatarMood.IDLE,
      isBlinking = true,
      isCelebrating = true,
      speakingText = "",
      spokenCharRange = null,
      speechPhaseStep = 0,
    )
    assertTrue(celebrating.happyAlpha >= 0.85f)
    assertEquals(0f, celebrating.blinkAlpha, 0.001f)
  }

  @Test
  fun blinkIntervalsStayCalmWithPeriodicDoubleBlink() {
    assertEquals(280L, TutorExpressionEngine.nextBlinkDelayMs(6))
    assertTrue(TutorExpressionEngine.nextBlinkDelayMs(1) >= 3500L)
  }
}
