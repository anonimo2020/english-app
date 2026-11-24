package com.example.englishapp

import com.example.englishapp.data.VocabEntry
import com.example.englishapp.data.VocabLetterGameEngine
import com.example.englishapp.model.LearnerLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabLetterGameEngineTest {

  private fun sampleEntry(word: String, level: LearnerLevel = LearnerLevel.NOVATO): VocabEntry =
    VocabEntry(
      id = "test_${word.lowercase()}",
      level = level,
      category = "Pruebas",
      word = word,
      ipa = "/test/",
      spanish = "prueba",
      exampleEn = "This is a test.",
      exampleEs = "Esto es una prueba.",
      tipEs = "Consejo de prueba.",
    )

  @Test
  fun createsPuzzleWithLevelScaledDistractorsAndPreservesSpaces() {
    val beginner = VocabLetterGameEngine.createPuzzle(sampleEntry("Wake up", LearnerLevel.NOVATO), LearnerLevel.NOVATO, seed = 42)
    assertEquals(listOf('W', 'A', 'K', 'E', ' ', 'U', 'P'), beginner.targetChars)
    assertEquals(setOf(4), beginner.fixedIndices)
    assertEquals(0, beginner.tiles.count { it.isDistractor })
    assertEquals(6, beginner.tiles.size)

    val intermediate = VocabLetterGameEngine.createPuzzle(sampleEntry("Schedule", LearnerLevel.INTERMEDIO), LearnerLevel.INTERMEDIO, seed = 42)
    assertEquals(1, intermediate.tiles.count { it.isDistractor })

    val mastery = VocabLetterGameEngine.createPuzzle(sampleEntry("Nuance", LearnerLevel.MAESTRIA), LearnerLevel.MAESTRIA, seed = 42)
    assertEquals(2, mastery.tiles.count { it.isDistractor })
  }

  @Test
  fun tappingWrongOrderRecordsErrorAndSlotRemovalAllowsFixingToSolve() {
    var state = VocabLetterGameEngine.createPuzzle(sampleEntry("Cat", LearnerLevel.NOVATO), LearnerLevel.NOVATO, seed = 10)

    // Intentionally place letters in reverse: T-A-C
    val tileT = state.tiles.first { it.char == 'T' }
    val tileA = state.tiles.first { it.char == 'A' }
    val tileC = state.tiles.first { it.char == 'C' }

    state = VocabLetterGameEngine.tapTile(state, tileT.id)
    state = VocabLetterGameEngine.tapTile(state, tileA.id)
    state = VocabLetterGameEngine.tapTile(state, tileC.id)

    assertFalse(state.isSolved)
    assertEquals(1, state.wrongAttempts)
    assertEquals("TAC", state.lastErrorWord)

    // Remove first and third slots, then tap C and T in the right order
    state = VocabLetterGameEngine.tapSlot(state, 0)
    state = VocabLetterGameEngine.tapSlot(state, 2)
    assertNull(state.lastErrorWord)

    state = VocabLetterGameEngine.tapTile(state, tileC.id)
    state = VocabLetterGameEngine.tapTile(state, tileT.id)

    assertTrue(state.isSolved)
    assertNull(state.lastErrorWord)
  }

  @Test
  fun useHintFillsWrongOrEmptySlotAndSolvesCompleteWord() {
    var state = VocabLetterGameEngine.createPuzzle(sampleEntry("Plan", LearnerLevel.PRO), LearnerLevel.PRO, seed = 7)
    // Tap first two tiles (could be distractors or wrong letters)
    state = VocabLetterGameEngine.tapTile(state, state.tiles[0].id)

    repeat(4) {
      if (!state.isSolved) {
        state = VocabLetterGameEngine.useHint(state)
      }
    }

    assertTrue(state.isSolved)
    assertEquals("PLAN", state.currentBuiltText())
    assertTrue(state.hintsUsed in 1..4)

    val shuffled = VocabLetterGameEngine.reshuffleAvailable(
      VocabLetterGameEngine.createPuzzle(sampleEntry("Travel", LearnerLevel.JUNIOR), LearnerLevel.JUNIOR, seed = 9),
      seed = 99,
    )
    assertNotNull(shuffled)
    assertEquals(6, shuffled.tiles.size)
  }

  @Test
  fun computesXpWithStreakBonusAndPenalties() {
    assertEquals(15, VocabLetterGameEngine.computeWordXp(hintsUsed = 0, wrongAttempts = 0, currentStreak = 0))
    assertEquals(25, VocabLetterGameEngine.computeWordXp(hintsUsed = 0, wrongAttempts = 0, currentStreak = 6))
    assertEquals(5, VocabLetterGameEngine.computeWordXp(hintsUsed = 5, wrongAttempts = 5, currentStreak = 0))
  }
}
