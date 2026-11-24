package com.example.englishapp

import com.example.englishapp.data.PlacementAnswerRecord
import com.example.englishapp.data.PlacementEngine
import com.example.englishapp.model.LearnerLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementEngineTest {

  private fun simulateEvaluation(
    targetLevel: LearnerLevel,
    correctPredicate: (Int) -> Boolean,
  ): Pair<List<PlacementAnswerRecord>, com.example.englishapp.model.PlacementResult> {
    var ability = PlacementEngine.initialAbilityFor(targetLevel)
    val answered = mutableSetOf<String>()
    val records = mutableListOf<PlacementAnswerRecord>()

    for (step in 0 until PlacementEngine.DEFAULT_TOTAL_STEPS) {
      val q = PlacementEngine.nextQuestion(
        ability = ability,
        answeredIds = answered,
        stepIndex = step,
        allowSpeaking = false,
        targetLevel = targetLevel,
      )
      answered.add(q.id)
      val isCorrect = correctPredicate(step)
      records.add(PlacementAnswerRecord(q, isCorrect = isCorrect))
      ability = PlacementEngine.updateAbility(
        currentAbility = ability,
        questionDifficulty = q.difficulty,
        isCorrect = isCorrect,
        targetLevel = targetLevel,
      )
    }

    val result = PlacementEngine.buildResult(targetLevel, ability, records)
    return records to result
  }

  @Test
  fun novatoPerfectScoreReceivesOnlyA1QuestionsAndCertifiesAsNovatoNotC1() {
    val (records, result) = simulateEvaluation(LearnerLevel.NOVATO) { true }

    assertEquals(6, records.size)
    assertTrue(
      "Expected all questions <= 1.5 difficulty for NOVATO, got ${records.map { it.question.id to it.question.difficulty }}",
      records.all { PlacementEngine.levelForAbility(it.question.difficulty) == LearnerLevel.NOVATO },
    )
    assertEquals(LearnerLevel.NOVATO, result.verifiedLevel)
    assertTrue("Expected high level-relative score >= 90, got ${result.overallScore}", result.overallScore >= 90)
    assertTrue(result.skills.listening >= 90)
    assertTrue(result.skills.grammar >= 90)
    assertTrue(result.recommendedFocusEs.contains("A1"))
  }

  @Test
  fun intermedioNeverSeesHigherLevelQuestionsAndProCertifiesC1OnPerfectScore() {
    val (b1Records, b1Result) = simulateEvaluation(LearnerLevel.INTERMEDIO) { true }
    assertTrue(
      "INTERMEDIO user must never receive B2/C1/C2 questions: ${b1Records.map { it.question.id }}",
      b1Records.all { PlacementEngine.levelForAbility(it.question.difficulty).ordinal <= LearnerLevel.INTERMEDIO.ordinal },
    )
    assertEquals(LearnerLevel.INTERMEDIO, b1Result.verifiedLevel)
    assertTrue(b1Result.overallScore >= 90)

    val (c1Records, c1Result) = simulateEvaluation(LearnerLevel.PRO) { true }
    assertTrue(
      "PRO user should stay within <= C1: ${c1Records.map { it.question.id }}",
      c1Records.all { PlacementEngine.levelForAbility(it.question.difficulty).ordinal <= LearnerLevel.PRO.ordinal },
    )
    assertEquals(LearnerLevel.PRO, c1Result.verifiedLevel)
  }

  @Test
  fun lowAccuracyDropsOverEstimatedUserAppropriately() {
    val (_, zeroFromMastery) = simulateEvaluation(LearnerLevel.MAESTRIA) { false }
    assertEquals(LearnerLevel.NOVATO, zeroFromMastery.verifiedLevel)
    assertTrue(zeroFromMastery.recommendedFocusEs.contains("A1"))

    val (_, twoOfSixFromPro) = simulateEvaluation(LearnerLevel.PRO) { step -> step < 2 }
    assertTrue(
      "Expected PRO with 2/6 right to drop below PRO, got ${twoOfSixFromPro.verifiedLevel}",
      twoOfSixFromPro.verifiedLevel.ordinal < LearnerLevel.PRO.ordinal,
    )
  }

  @Test
  fun mapsAbilityThresholdsAndQuestionBankAcrossAllSixCefrLevels() {
    val levels = listOf(
      LearnerLevel.NOVATO,
      LearnerLevel.JUNIOR,
      LearnerLevel.INTERMEDIO,
      LearnerLevel.INTERMEDIO_ALTO,
      LearnerLevel.PRO,
      LearnerLevel.MAESTRIA,
    )
    val mappedFromInitial = levels.map { est ->
      PlacementEngine.levelForAbility(PlacementEngine.initialAbilityFor(est))
    }
    assertEquals(levels, mappedFromInitial)

    assertEquals(36, PlacementEngine.bank.size)
    for (level in levels) {
      assertEquals(6, PlacementEngine.bank.count { PlacementEngine.levelForAbility(it.difficulty) == level })
    }
  }

  @Test
  fun spokenMatchEvaluatesAccuracyAccurately() {
    val exact = PlacementEngine.evaluateSpokenMatch(
      expected = "I would like a hot coffee, please.",
      spoken = "I would like a hot coffee please",
    )
    assertTrue("Expected high score for exact match, got $exact", exact >= 0.90f)

    val partial = PlacementEngine.evaluateSpokenMatch(
      expected = "I would like a window seat, please.",
      spoken = "I would like window seat please",
    )
    assertTrue("Expected passing score >= 0.65, got $partial", partial >= PlacementEngine.SPEAKING_PASS_THRESHOLD)

    val wrong = PlacementEngine.evaluateSpokenMatch(
      expected = "We cut onboarding time in half.",
      spoken = "hello my friend",
    )
    assertTrue("Expected low score for mismatch, got $wrong", wrong < 0.35f)
  }
}
