package com.example.englishapp.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class WordMatchStatus {
  EXACT,
  CLOSE,
  MISSED,
}

data class WordFeedback(
  val word: String,
  val status: WordMatchStatus,
  val tipEs: String? = null,
)

data class SpeakingAnalysis(
  val expected: String,
  val spoken: String,
  val overallScore: Int,
  val passed: Boolean,
  val attemptNumber: Int,
  val previousScore: Int? = null,
  val wordBreakdown: List<WordFeedback>,
  val headlineEs: String,
  val actionableTipsEs: List<String>,
)

object SpeakingCoachEngine {

  const val DEFAULT_PASS_THRESHOLD = 75

  fun analyzePronunciation(
    expected: String,
    spoken: String,
    attemptNumber: Int = 1,
    previousScore: Int? = null,
    passThreshold: Int = DEFAULT_PASS_THRESHOLD,
  ): SpeakingAnalysis {
    val cleanExpectedTokens = expected.trim()
      .split(Regex("\\s+"))
      .map { it.trim(',', '.', '!', '?', ';', ':', '"', '\'') }
      .filter { it.isNotBlank() }

    val spokenTokens = normalize(spoken)
      .split(" ")
      .filter { it.isNotBlank() }

    val availableSpoken = spokenTokens.toMutableList()
    val breakdown = mutableListOf<WordFeedback>()
    var wordCredit = 0f

    for (rawWord in cleanExpectedTokens) {
      val target = normalize(rawWord)
      if (target.isEmpty()) continue

      val exactIdx = availableSpoken.indexOf(target)
      if (exactIdx >= 0) {
        availableSpoken.removeAt(exactIdx)
        breakdown.add(WordFeedback(rawWord, WordMatchStatus.EXACT, phoneticTipForWord(target)))
        wordCredit += 1.0f
      } else {
        val bestClose = availableSpoken
          .mapIndexed { idx, candidate -> idx to tokenSimilarity(target, candidate) }
          .maxByOrNull { it.second }

        if (bestClose != null && bestClose.second >= 0.62f) {
          availableSpoken.removeAt(bestClose.first)
          breakdown.add(WordFeedback(rawWord, WordMatchStatus.CLOSE, phoneticTipForWord(target)))
          wordCredit += 0.65f
        } else {
          breakdown.add(WordFeedback(rawWord, WordMatchStatus.MISSED, phoneticTipForWord(target)))
        }
      }
    }

    val tokenScore = if (cleanExpectedTokens.isEmpty()) {
      0f
    } else {
      wordCredit / cleanExpectedTokens.size.toFloat()
    }
    val globalMatch = PlacementEngine.evaluateSpokenMatch(expected, spoken)
    val overallScore = ((tokenScore * 0.55f + globalMatch * 0.45f) * 100f)
      .roundToInt()
      .coerceIn(0, 100)
    val passed = overallScore >= passThreshold

    val missedWords = breakdown.filter { it.status == WordMatchStatus.MISSED }
    val closeWords = breakdown.filter { it.status == WordMatchStatus.CLOSE }
    val delta = previousScore?.let { overallScore - it }

    val headlineEs = when {
      overallScore >= 90 -> "¡Pronunciación sobresaliente ($overallScore%)!"
      passed -> "¡Muy bien ($overallScore%)! Ya dominas esta frase."
      delta != null && delta > 0 ->
        "Subiste +$delta% (ahora $overallScore%). ¡Estás cerca del $passThreshold%!"
      overallScore >= 55 ->
        "Buen intento ($overallScore%). Afina las palabras marcadas y repite."
      else ->
        "Obtuviste $overallScore%. Escúchala a 0.65x y vuelve a intentarlo."
    }

    val tips = buildList {
      if (spokenTokens.size < (cleanExpectedTokens.size * 0.6f).roundToInt()) {
        add("Di la frase completa sin cortar el final y haz una micro-pausa antes de parar el micrófono.")
      }
      if (missedWords.isNotEmpty()) {
        val sample = missedWords.take(3).joinToString(", ") { "'${it.word}'" }
        add("No detectamos claro: $sample. Toca cada palabra abajo para oírla despacio.")
      }
      if (closeWords.isNotEmpty()) {
        val sample = closeWords.take(2).joinToString(", ") { "'${it.word}'" }
        add("Estuviste cerca en $sample; marca un poco más sus vocales y consonantes finales.")
      }
      val specificPhoneticTip = (missedWords + closeWords)
        .mapNotNull { it.tipEs }
        .distinct()
        .firstOrNull()
      if (specificPhoneticTip != null) {
        add(specificPhoneticTip)
      }
      if (!passed) {
        if (attemptNumber >= 2) {
          add("Consejo: escucha primero el botón '0.65x Lento', imita el ritmo y vuelve a grabar.")
        } else {
          add("Habla un poco más cerca del micrófono con voz clara y ritmo constante.")
        }
      } else if (isEmpty()) {
        add("Tu ritmo y articulación fueron claros. Puedes pasar al siguiente reto o guardar la frase.")
      }
    }

    return SpeakingAnalysis(
      expected = expected,
      spoken = spoken,
      overallScore = overallScore,
      passed = passed,
      attemptNumber = attemptNumber,
      previousScore = previousScore,
      wordBreakdown = breakdown,
      headlineEs = headlineEs,
      actionableTipsEs = tips.distinct().take(4),
    )
  }

  fun phoneticTipForWord(rawWord: String): String? {
    val w = normalize(rawWord)
    if (w.isEmpty()) return null
    return when {
      w in setOf("would", "could", "should") ->
        "En '$w' la letra L es muda: pronuncia '${w.first()}ud' suave."
      w in setOf("aisle", "island") ->
        "En '$w' la S es muda: suena '${if (w == "aisle") "áil" else "ái-land"}'."
      w == "receipt" ->
        "En 'receipt' la P es muda: suena 'ri-SÍT'."
      w == "salmon" ->
        "En 'salmon' la L es muda: suena 'sá-mon'."
      w == "schedule" ->
        "En 'schedule' pronuncia 'ské-dyul' (US) o 'shé-dyul' (UK)."
      w == "comfortable" ->
        "En 'comfortable' agrupa en 3 sílabas: 'kámf-tor-bol'."
      w in setOf("though", "through", "thought", "enough") ->
        "Cuidado con '-ough' en '$w': revisa si es 'dou' (though), 'zru' (through) o 'zot' (thought)."
      w.contains("th") ->
        "Para 'th' en '$w', apoya suavemente la punta de la lengua entre los dientes con aire."
      w.contains("gh") ->
        "En '$w' la combinación 'gh' suele ser muda o suave al final."
      w.startsWith("sh") ->
        "Marca 'sh' en '$w' como un sonido suave de silencio ('shhh'), sin 'ch'."
      w.startsWith("ch") ->
        "Pronuncia 'ch' en '$w' con un toque firme como en español."
      w.endsWith("tion") || w.endsWith("sion") ->
        "La terminación de '$w' suena '-shon' suave."
      w.endsWith("ed") && w.length >= 4 ->
        "En el pasado '$w' no agregues sílaba extra salvo que termine en sonido T o D."
      w.startsWith("h") && w.length >= 3 ->
        "Aspira suavemente la H inicial en '$w', como un suspiro ligero."
      w.startsWith("v") ->
        "Para 'v' en '$w', toca el labio inferior con los dientes superiores vibrando."
      w.startsWith("w") ->
        "Para 'w' en '$w', redondea los labios como una 'u' rápida sin decir 'gu'."
      else -> null
    }
  }

  private fun normalize(text: String): String = text.lowercase()
    .replace(Regex("[^a-z0-9\\s]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

  private fun tokenSimilarity(a: String, b: String): Float {
    if (a == b) return 1f
    val maxLen = max(a.length, b.length).coerceAtLeast(1)
    val dist = levenshtein(a, b)
    return (1f - dist.toFloat() / maxLen.toFloat()).coerceIn(0f, 1f)
  }

  private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    var prev = IntArray(b.length + 1) { it }
    var curr = IntArray(b.length + 1)
    for (i in 1..a.length) {
      curr[0] = i
      for (j in 1..b.length) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
      }
      val tmp = prev
      prev = curr
      curr = tmp
    }
    return prev[b.length]
  }
}
