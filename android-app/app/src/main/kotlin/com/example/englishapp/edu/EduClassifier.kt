package com.example.englishapp.edu

data class EduClassification(
  val cefrLevel: String,
  val primarySkill: String,
  val secondarySkills: List<String>,
  val topic: String,
  val difficulty: Int,
  val learningObjectiveEs: String,
  val confidence: Double,
  val classificationSource: String, // curated_internal | provider_declared | auto_estimated
  val validationStatus: String,     // validated | pending_review
)

/**
 * Clasificador educativo inteligente en Android.
 * Nunca marca como validado oficialmente un nivel CEFR inferido heurísticamente.
 */
object EduClassifier {

  private val validCefr = setOf("A1", "A2", "B1", "B2", "C1", "C2")
  private val wordRegex = Regex("[a-zA-Z']+")
  private val sentenceRegex = Regex("[.!?]+")

  private val c2Markers = setOf("notwithstanding", "lest", "tantamount", "dichotomy", "scarcely", "impasse", "candor", "conflate", "ubiquitous")
  private val c1Markers = setOf("leverage", "benchmark", "rapport", "nuance", "resilient", "stakeholder", "nevertheless", "mitigate", "oversight")
  private val b2Markers = setOf("whereas", "nonetheless", "hypothesis", "perspective", "mortgage", "asynchronous", "evidence", "empirical")
  private val b1Markers = setOf("experience", "interview", "however", "improve", "schedule", "solution", "comfortable", "symptom")
  private val a2Markers = setOf("yesterday", "tomorrow", "airport", "luggage", "passport", "restaurant", "weekend", "vacation")

  fun classify(
    title: String,
    text: String,
    declaredCefr: String? = null,
    skillHint: String = "reading",
    curatedInternal: Boolean = false,
  ): EduClassification {
    val combined = "$title $text".trim()
    val words = wordRegex.findAll(combined).map { it.value.lowercase() }.toList()

    val (cefr, confidence, source, status) = when {
      curatedInternal && declaredCefr?.uppercase() in validCefr -> {
        Quad(declaredCefr!!.uppercase(), 1.0, "curated_internal", "validated")
      }
      declaredCefr?.uppercase() in validCefr -> {
        Quad(declaredCefr!!.uppercase(), 0.84, "provider_declared", "validated")
      }
      else -> {
        val (est, conf) = estimateCefr(words, combined)
        Quad(est, conf, "auto_estimated", "pending_review")
      }
    }

    val diffBase = when (cefr) {
      "A1" -> 2
      "A2" -> 3
      "B1" -> 5
      "B2" -> 7
      "C1" -> 8
      else -> 10
    }
    val secondary = when (skillHint) {
      "listening" -> listOf("comprehension", "pronunciation")
      "grammar" -> listOf("writing", "comprehension")
      "vocabulary" -> listOf("reading", "idiomatic_expressions")
      "speaking" -> listOf("pronunciation", "everyday_english")
      else -> listOf("comprehension", "vocabulary")
    }
    return EduClassification(
      cefrLevel = cefr,
      primarySkill = skillHint,
      secondarySkills = secondary,
      topic = detectTopic(words.toSet()),
      difficulty = diffBase,
      learningObjectiveEs = "Practicar ${EduSkillCatalog.labelFor(skillHint)} en nivel $cefr.",
      confidence = confidence,
      classificationSource = source,
      validationStatus = status,
    )
  }

  private fun estimateCefr(words: List<String>, raw: String): Pair<String, Double> {
    if (words.isEmpty()) return "A2" to 0.50
    val avgWordLen = words.sumOf { it.length }.toDouble() / words.size
    val sentences = sentenceRegex.split(raw).count { it.isNotBlank() }.coerceAtLeast(1)
    val avgSentLen = words.size.toDouble() / sentences
    val wset = words.toSet()

    val c2Hits = wset.count { it in c2Markers }
    val c1Hits = wset.count { it in c1Markers }
    val b2Hits = wset.count { it in b2Markers }
    val b1Hits = wset.count { it in b1Markers }
    val a2Hits = wset.count { it in a2Markers }

    val score = (avgWordLen - 3.6).coerceAtLeast(0.0) * 1.35 +
      (avgSentLen - 7.0).coerceAtLeast(0.0) * 0.16 +
      c2Hits * 1.4 + c1Hits * 0.9 + b2Hits * 0.55 + b1Hits * 0.25 - a2Hits * 0.25

    return when {
      c2Hits >= 1 || score >= 5.2 -> "C2" to 0.68
      c1Hits >= 1 || score >= 3.8 -> "C1" to 0.70
      b2Hits >= 1 || score >= 2.6 -> "B2" to 0.72
      b1Hits >= 1 || score >= 1.5 || avgSentLen >= 12.0 -> "B1" to 0.74
      a2Hits >= 1 || avgSentLen >= 8.0 || avgWordLen >= 4.2 -> "A2" to 0.75
      else -> "A1" to 0.76
    }
  }

  private fun detectTopic(tokens: Set<String>): String = when {
    tokens.any { it in setOf("job", "interview", "salary", "client", "office") } -> "business_career"
    tokens.any { it in setOf("science", "solar", "sun", "energy", "research") } -> "science_tech"
    tokens.any { it in setOf("airport", "flight", "hotel", "train", "passport") } -> "travel_culture"
    tokens.any { it in setOf("doctor", "health", "sleep", "habit", "medicine") } -> "health_wellness"
    tokens.any { it in setOf("novel", "story", "poem", "author", "classic") } -> "literature_arts"
    else -> "general_english"
  }

  private data class Quad(val a: String, val b: Double, val c: String, val d: String)
}
