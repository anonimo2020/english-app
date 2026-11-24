package com.example.englishapp.data

import com.example.englishapp.model.LearnerLevel
import kotlin.random.Random

data class LetterTile(
  val id: Int,
  val char: Char,
  val isUsed: Boolean = false,
  val isDistractor: Boolean = false,
)

data class LetterPuzzleState(
  val entry: VocabEntry,
  val targetChars: List<Char>,
  val fixedIndices: Set<Int>,
  val placedTileIds: List<Int?>,
  val tiles: List<LetterTile>,
  val hintsUsed: Int = 0,
  val wrongAttempts: Int = 0,
  val isSolved: Boolean = false,
  val lastErrorWord: String? = null,
) {
  fun isCompleteAndFilled(): Boolean =
    targetChars.indices.all { idx -> idx in fixedIndices || placedTileIds[idx] != null }

  fun currentBuiltText(): String = buildString {
    targetChars.forEachIndexed { idx, expectedChar ->
      if (idx in fixedIndices) {
        append(expectedChar)
      } else {
        val tileId = placedTileIds[idx]
        val tileChar = tileId?.let { id -> tiles.firstOrNull { it.id == id }?.char }
        append(tileChar ?: '_')
      }
    }
  }
}

object VocabLetterGameEngine {

  fun createPuzzle(
    entry: VocabEntry,
    level: LearnerLevel = entry.level,
    seed: Int = entry.id.hashCode() + entry.word.length * 31,
  ): LetterPuzzleState {
    val cleanWord = entry.word.trim()
    val targetChars = cleanWord.map { it.uppercaseChar() }
    val fixedIndices = mutableSetOf<Int>()
    val letterIndices = mutableListOf<Int>()

    targetChars.forEachIndexed { index, ch ->
      if (ch.isLetter()) {
        letterIndices.add(index)
      } else {
        fixedIndices.add(index)
      }
    }

    val rawTiles = mutableListOf<LetterTile>()
    var nextId = 0
    letterIndices.forEach { idx ->
      rawTiles.add(
        LetterTile(
          id = nextId++,
          char = targetChars[idx],
          isUsed = false,
          isDistractor = false,
        ),
      )
    }

    //niveles avanzados incluyen letras distractoras para mayor reto
    val distractorCount = when (level) {
      LearnerLevel.NOVATO, LearnerLevel.JUNIOR -> 0
      LearnerLevel.INTERMEDIO -> 1
      LearnerLevel.INTERMEDIO_ALTO, LearnerLevel.PRO, LearnerLevel.MAESTRIA -> 2
    }
    if (distractorCount > 0) {
      val rng = Random(seed)
      val candidates = "AEIOURSTLNMPBCDG".filter { ch ->
        rawTiles.count { it.char == ch } < 2
      }
      val pool = candidates.ifEmpty { "AEIOURST" }
      repeat(distractorCount) {
        val distractorChar = pool[rng.nextInt(pool.length)]
        rawTiles.add(
          LetterTile(
            id = nextId++,
            char = distractorChar,
            isUsed = false,
            isDistractor = true,
          ),
        )
      }
    }

    val shuffled = shuffleAvoidingOriginalOrder(rawTiles, targetChars, fixedIndices, Random(seed))
    val initialPlaced = List(targetChars.size) { null }

    return LetterPuzzleState(
      entry = entry,
      targetChars = targetChars,
      fixedIndices = fixedIndices,
      placedTileIds = initialPlaced,
      tiles = shuffled,
    )
  }

  fun tapTile(state: LetterPuzzleState, tileId: Int): LetterPuzzleState {
    if (state.isSolved) return state
    val tile = state.tiles.firstOrNull { it.id == tileId } ?: return state
    if (tile.isUsed) return state

    val emptySlotIdx = state.targetChars.indices.firstOrNull { idx ->
      idx !in state.fixedIndices && state.placedTileIds[idx] == null
    } ?: return state

    val newPlaced = state.placedTileIds.toMutableList()
    newPlaced[emptySlotIdx] = tileId
    val newTiles = state.tiles.map { if (it.id == tileId) it.copy(isUsed = true) else it }

    val updated = state.copy(
      placedTileIds = newPlaced,
      tiles = newTiles,
      lastErrorWord = null,
    )
    return evaluateIfFull(updated)
  }

  fun tapSlot(state: LetterPuzzleState, slotIndex: Int): LetterPuzzleState {
    if (state.isSolved) return state
    if (slotIndex !in state.targetChars.indices || slotIndex in state.fixedIndices) return state
    val removedTileId = state.placedTileIds[slotIndex] ?: return state

    val newPlaced = state.placedTileIds.toMutableList()
    newPlaced[slotIndex] = null
    val newTiles = state.tiles.map { if (it.id == removedTileId) it.copy(isUsed = false) else it }

    return state.copy(
      placedTileIds = newPlaced,
      tiles = newTiles,
      lastErrorWord = null,
    )
  }

  fun clearSlots(state: LetterPuzzleState): LetterPuzzleState {
    if (state.isSolved) return state
    return state.copy(
      placedTileIds = List(state.targetChars.size) { null },
      tiles = state.tiles.map { it.copy(isUsed = false) },
      lastErrorWord = null,
    )
  }

  fun reshuffleAvailable(state: LetterPuzzleState, seed: Int): LetterPuzzleState {
    val unused = state.tiles.filter { !it.isUsed }.shuffled(Random(seed))
    var unusedPtr = 0
    val reordered = state.tiles.map { tile ->
      if (!tile.isUsed && unusedPtr < unused.size) unused[unusedPtr++] else tile
    }
    return state.copy(tiles = reordered)
  }

  fun useHint(state: LetterPuzzleState): LetterPuzzleState {
    if (state.isSolved) return state

    // First look for an empty slot OR a wrongly placed slot
    val targetSlot = state.targetChars.indices.firstOrNull { idx ->
      if (idx in state.fixedIndices) return@firstOrNull false
      val placedId = state.placedTileIds[idx]
      val placedChar = placedId?.let { id -> state.tiles.firstOrNull { it.id == id }?.char }
      placedChar != state.targetChars[idx]
    } ?: return state

    val neededChar = state.targetChars[targetSlot]
    val newPlaced = state.placedTileIds.toMutableList()
    var newTiles = state.tiles.toMutableList()

    // Return any wrong tile sitting in targetSlot
    newPlaced[targetSlot]?.let { wrongId ->
      newTiles = newTiles.map { if (it.id == wrongId) it.copy(isUsed = false) else it }.toMutableList()
      newPlaced[targetSlot] = null
    }

    // Find an unused tile matching neededChar (prefer non-distractor)
    var matchingTile = newTiles.firstOrNull { !it.isUsed && !it.isDistractor && it.char == neededChar }
      ?: newTiles.firstOrNull { !it.isUsed && it.char == neededChar }

    // If neededChar is currently misplaced inside another slot, free it from there first
    if (matchingTile == null) {
      val stolenSlot = state.targetChars.indices.firstOrNull { idx ->
        idx != targetSlot && idx !in state.fixedIndices && newPlaced[idx]?.let { id ->
          val t = newTiles.firstOrNull { it.id == id }
          t?.char == neededChar
        } == true
      }
      if (stolenSlot != null) {
        val freedId = newPlaced[stolenSlot]!!
        newPlaced[stolenSlot] = null
        newTiles = newTiles.map { if (it.id == freedId) it.copy(isUsed = false) else it }.toMutableList()
        matchingTile = newTiles.firstOrNull { it.id == freedId }
      }
    }

    if (matchingTile == null) return state

    newPlaced[targetSlot] = matchingTile.id
    val finalTiles = newTiles.map { if (it.id == matchingTile.id) it.copy(isUsed = true) else it }
    val updated = state.copy(
      placedTileIds = newPlaced,
      tiles = finalTiles,
      hintsUsed = state.hintsUsed + 1,
      lastErrorWord = null,
    )
    return evaluateIfFull(updated)
  }

  fun computeWordXp(
    hintsUsed: Int,
    wrongAttempts: Int,
    currentStreak: Int,
  ): Int {
    val base = 15
    val streakBonus = (currentStreak.coerceAtLeast(0) * 2).coerceAtMost(10)
    val penalty = hintsUsed * 3 + wrongAttempts * 2
    return (base + streakBonus - penalty).coerceIn(5, 25)
  }

  private fun evaluateIfFull(state: LetterPuzzleState): LetterPuzzleState {
    if (!state.isCompleteAndFilled()) return state

    val built = state.currentBuiltText()
    val expected = state.targetChars.joinToString("")
    return if (built.equals(expected, ignoreCase = true)) {
      state.copy(isSolved = true, lastErrorWord = null)
    } else {
      state.copy(
        isSolved = false,
        wrongAttempts = state.wrongAttempts + 1,
        lastErrorWord = built,
      )
    }
  }

  private fun shuffleAvoidingOriginalOrder(
    tiles: List<LetterTile>,
    targetChars: List<Char>,
    fixedIndices: Set<Int>,
    rng: Random,
  ): List<LetterTile> {
    if (tiles.size <= 1) return tiles
    val expectedNonFixed = targetChars.filterIndexed { idx, _ -> idx !in fixedIndices }.joinToString("")
    var attempt = tiles.shuffled(rng)
    var guard = 0
    while (guard < 6 && attempt.take(expectedNonFixed.length).joinToString("") { it.char.toString() } == expectedNonFixed) {
      attempt = tiles.shuffled(Random(rng.nextInt() + guard + 1))
      guard++
    }
    return attempt
  }
}
