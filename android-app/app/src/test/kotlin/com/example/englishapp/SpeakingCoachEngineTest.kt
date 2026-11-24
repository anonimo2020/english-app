package com.example.englishapp

import com.example.englishapp.data.SpeakingCoachEngine
import com.example.englishapp.data.WordMatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakingCoachEngineTest {

  @Test
  fun exactMatchPassesWithFullBreakdownAndHighScore() {
    val result = SpeakingCoachEngine.analyzePronunciation(
      expected = "I would like a hot coffee, please.",
      spoken = "I would like a hot coffee please",
      attemptNumber = 1,
    )

    assertTrue("Expected pass score >= 75, got ${result.overallScore}", result.passed)
    assertTrue("Expected score >= 95, got ${result.overallScore}", result.overallScore >= 95)
    assertEquals(7, result.wordBreakdown.size)
    assertTrue(result.wordBreakdown.all { it.status == WordMatchStatus.EXACT })
    assertTrue(result.actionableTipsEs.isNotEmpty())
  }

  @Test
  fun lowAccuracyTriggersRetryWithActionableSpanishTips() {
    val result = SpeakingCoachEngine.analyzePronunciation(
      expected = "I would like the receipt for this shirt.",
      spoken = "I woud",
      attemptNumber = 1,
      previousScore = null,
    )

    assertFalse("Partial attempt should require retry (< 75)", result.passed)
    assertTrue("Score should be < 75, got ${result.overallScore}", result.overallScore < 75)
    assertTrue(result.wordBreakdown.any { it.status == WordMatchStatus.MISSED })
    assertTrue(result.wordBreakdown.any { it.status == WordMatchStatus.CLOSE })
    assertTrue(result.actionableTipsEs.isNotEmpty())
    assertTrue(
      "Expected tips to explain missed/close words or phonetic guidance: ${result.actionableTipsEs}",
      result.actionableTipsEs.any {
        it.contains("No detectamos", ignoreCase = true) ||
          it.contains("cerca", ignoreCase = true) ||
          it.contains("0.65x", ignoreCase = true) ||
          it.contains("micrófono", ignoreCase = true)
      },
    )
  }

  @Test
  fun retryImprovementUpdatesHeadlineDeltaAndSuggestsSlowPlayback() {
    val result = SpeakingCoachEngine.analyzePronunciation(
      expected = "I would like a large iced coffee please",
      spoken = "I would like coffee",
      attemptNumber = 2,
      previousScore = 25,
    )

    assertFalse("Half sentence should stay below 75%", result.passed)
    assertTrue("Expected improvement over 25%, got ${result.overallScore}", result.overallScore > 25)
    assertTrue(
      "Expected positive delta in headline: ${result.headlineEs}",
      result.headlineEs.contains("Subiste"),
    )
    assertTrue(
      "Expected 0.65x tip on attempt >= 2: ${result.actionableTipsEs}",
      result.actionableTipsEs.any { it.contains("0.65x") },
    )
  }

  @Test
  fun providesTargetedSpanishPhoneticTipsForTrickyWords() {
    val wouldTip = SpeakingCoachEngine.phoneticTipForWord("would")
    val thinkTip = SpeakingCoachEngine.phoneticTipForWord("think")
    val receiptTip = SpeakingCoachEngine.phoneticTipForWord("receipt")
    val workedTip = SpeakingCoachEngine.phoneticTipForWord("worked")
    val simpleTip = SpeakingCoachEngine.phoneticTipForWord("cat")

    assertNotNull(wouldTip)
    assertTrue(wouldTip!!.contains("L es muda"))
    assertNotNull(thinkTip)
    assertTrue(thinkTip!!.contains("th"))
    assertNotNull(receiptTip)
    assertTrue(receiptTip!!.contains("P es muda"))
    assertNotNull(workedTip)
    assertTrue(workedTip!!.contains("sílaba extra"))
    assertNull(simpleTip)
  }
}
