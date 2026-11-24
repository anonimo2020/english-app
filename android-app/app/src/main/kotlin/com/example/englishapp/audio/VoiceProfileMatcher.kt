package com.example.englishapp.audio

import com.example.englishapp.model.TutorCharacter

/** Metadata snapshot of an Android TextToSpeech `Voice`, kept decoupled from framework classes for testing. */
data class VoiceCandidate(
  val name: String,
  val language: String,
  val country: String,
  val isNetworkRequired: Boolean,
  val quality: Int,
  val features: Set<String> = emptySet(),
)

/** Result of choosing the most gender- and locale-congruent voice for a tutor persona. */
data class VoiceSelection(
  val voiceName: String?,
  val matchedExpectedGender: Boolean,
  val recommendedPitch: Float,
)

enum class DetectedVoiceGender {
  FEMALE,
  MALE,
  UNKNOWN,
}

/**
 * Selects a TTS voice matching the tutor's persona gender (Manu = female, Eddy = male)
 * across both English and Spanish locales, avoiding substring false positives like `"female"` matching `"male"`.
 */
object VoiceProfileMatcher {

  private val femaleWordRegex = Regex("""(^|[^a-z])(female|woman|girl|feminine)([^a-z]|$)""")
  private val maleWordRegex = Regex("""(^|[^a-z])(male|man|boy|masculine)([^a-z]|$)""")
  private val femaleCodeRegex = Regex("""(^|[-_#])(f|sfg|sfb|tpc|iob|iog|gba|gbc|fis|esc|esg|eec|eee|ana)([-_#\d]|$)""")
  private val maleCodeRegex = Regex("""(^|[-_#])(m|iol|iom|tpd|rjs|gbb|gbd|esd|esf|ese|eed|eef|eeb)([-_#\d]|$)""")

  private val femaleNames = setOf(
    "samantha", "karen", "victoria", "monica", "paulina", "lucia", "elena",
    "sofia", "paloma", "laura", "carmen", "jenny", "aria", "emma", "marisol",
    "isabela", "valeria", "jimena", "tessa", "kate", "serena", "helena",
  )

  private val maleNames = setOf(
    "daniel", "alex", "fred", "arthur", "jorge", "carlos", "diego", "pablo",
    "juan", "enrique", "alvaro", "alonso", "miguel", "mateo", "ryan",
    "matthew", "guy", "oliver", "george", "edward", "danny", "raúl", "raul",
  )

  /** Classifies a TTS voice candidate as female, male, or unknown, checking female indicators first. */
  fun detectGender(candidate: VoiceCandidate): DetectedVoiceGender {
    val combined = buildString {
      append(candidate.name.lowercase())
      if (candidate.features.isNotEmpty()) {
        append(' ')
        append(candidate.features.joinToString(" ").lowercase())
      }
    }

    if (isFemaleSignature(combined)) return DetectedVoiceGender.FEMALE
    if (isMaleSignature(combined)) return DetectedVoiceGender.MALE
    return DetectedVoiceGender.UNKNOWN
  }

  private fun isFemaleSignature(normalized: String): Boolean {
    if (femaleWordRegex.containsMatchIn(normalized)) return true
    if (femaleCodeRegex.containsMatchIn(normalized)) return true
    return containsWordName(normalized, femaleNames)
  }

  private fun isMaleSignature(normalized: String): Boolean {
    if (maleWordRegex.containsMatchIn(normalized)) return true
    if (maleCodeRegex.containsMatchIn(normalized)) return true
    return containsWordName(normalized, maleNames)
  }

  private fun containsWordName(text: String, names: Set<String>): Boolean {
    val tokens = text.split(Regex("""[^a-záéíóúñ]+""")).filter { it.isNotEmpty() }
    return tokens.any { it in names }
  }

  /**
   * Picks the best voice candidate for [languageCode] and [character].
   * Expected gender outranks region preference so a male voice in another Spanish/English region
   * always beats an opposite-gender voice in the primary country.
   */
  fun selectBestVoice(
    candidates: List<VoiceCandidate>,
    languageCode: String,
    preferredCountries: List<String>,
    character: TutorCharacter,
  ): VoiceSelection {
    val expectedGender = if (character == TutorCharacter.MANU) {
      DetectedVoiceGender.FEMALE
    } else {
      DetectedVoiceGender.MALE
    }

    val sameLanguage = candidates.filter { it.language.equals(languageCode, ignoreCase = true) }
    val installed = sameLanguage.filter { c ->
      c.features.none { it.equals("notInstalled", ignoreCase = true) }
    }.ifEmpty { sameLanguage }
    val pool = installed.filter { !it.isNetworkRequired }.ifEmpty { installed }

    if (pool.isEmpty()) {
      return VoiceSelection(
        voiceName = null,
        matchedExpectedGender = false,
        recommendedPitch = fallbackPitch(character, matchedExpectedGender = false),
      )
    }

    val best = pool.minWith(
      compareBy<VoiceCandidate>(
        { genderPriorityRank(detectGender(it), expectedGender) },
        { countryRank(it.country, preferredCountries) },
        { -it.quality },
        { it.name },
      ),
    )

    val matched = detectGender(best) == expectedGender
    return VoiceSelection(
      voiceName = best.name,
      matchedExpectedGender = matched,
      recommendedPitch = fallbackPitch(character, matched),
    )
  }

  private fun genderPriorityRank(
    actual: DetectedVoiceGender,
    expected: DetectedVoiceGender,
  ): Int = when (actual) {
    expected -> 0
    DetectedVoiceGender.UNKNOWN -> 1
    else -> 2
  }

  private fun countryRank(country: String, preferredCountries: List<String>): Int {
    val idx = preferredCountries.indexOfFirst { it.equals(country, ignoreCase = true) }
    return if (idx >= 0) idx else preferredCountries.size
  }

  private fun fallbackPitch(character: TutorCharacter, matchedExpectedGender: Boolean): Float = when (character) {
    TutorCharacter.EDDY -> if (matchedExpectedGender) 0.95f else 0.76f
    TutorCharacter.MANU -> if (matchedExpectedGender) 1.04f else 1.14f
  }
}
