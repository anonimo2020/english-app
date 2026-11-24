package com.example.englishapp.edu

import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.UserProgress
import kotlin.math.abs

/**
 * Motor local de recomendaciones personalizadas y rutas adaptativas (Fase 7).
 * Combina nivel activo, puntajes de diagnóstico, progreso, errores frecuentes,
 * historial y vocabulario conocido para recomendar el siguiente material óptimo.
 */
object EduRecommendationEngine {

  private val cefrOrder = listOf("A1", "A2", "B1", "B2", "C1", "C2")

  private data class SkillCluster(
    val titleEs: String,
    val preferredTypes: List<String>,
    val relatedSkills: Set<String>,
  )

  private val clusters = mapOf(
    "listening" to SkillCluster(
      titleEs = "Refuerzo integral de Listening",
      preferredTypes = listOf("listening", "audio_clip", "story", "scenario", "vocabulary"),
      relatedSkills = setOf("listening", "comprehension", "speaking", "vocabulary", "pronunciation"),
    ),
    "grammar" to SkillCluster(
      titleEs = "Ruta de Precisión Gramatical",
      preferredTypes = listOf("grammar", "lesson", "sentence_pair", "writing", "quiz"),
      relatedSkills = setOf("grammar", "writing", "comprehension"),
    ),
    "vocabulary" to SkillCluster(
      titleEs = "Ruta de Vocabulario Activo",
      preferredTypes = listOf("vocabulary", "word_game", "reading", "story", "sentence_pair"),
      relatedSkills = setOf("vocabulary", "idiomatic_expressions", "reading", "comprehension"),
    ),
    "speaking" to SkillCluster(
      titleEs = "Ruta de Fluidez y Pronunciación",
      preferredTypes = listOf("scenario", "pronunciation", "lesson", "listening"),
      relatedSkills = setOf("speaking", "pronunciation", "everyday_english", "listening"),
    ),
  )

  fun buildRecommendations(
    resources: List<EduResource>,
    settings: AppSettings,
    userProgress: UserProgress,
    hubProgress: Map<String, EduUserProgressRecord>,
    recentResourceIds: Set<String>,
    offlineOnly: Boolean = false,
    limit: Int = 10,
  ): Pair<EduLearningPath?, List<EduRecommendationItem>> {
    if (resources.isEmpty()) return null to emptyList()

    val targetCefr = settings.activeLevel().cefr
    val targetIdx = cefrOrder.indexOf(targetCefr).coerceAtLeast(0)
    val skills = settings.placementResult?.skills
    val listeningScore = skills?.listening ?: 75
    val grammarScore = skills?.grammar ?: 75
    val vocabScore = skills?.vocabulary ?: 75
    val speakingScore = skills?.speaking ?: 75

    val errorTags = hubProgress.values.flatMap { it.errorTags }.map { it.lowercase() }
    val skillPairs = mutableListOf(
      "listening" to listeningScore,
      "grammar" to grammarScore,
      "vocabulary" to vocabScore,
      "speaking" to speakingScore,
    ).sortedBy { it.second }

    var weakestSkill = skillPairs.first().first
    val errorBlob = errorTags.joinToString(" ")
    if (errorBlob.contains("listening") || errorBlob.contains("dictation")) {
      weakestSkill = "listening"
    } else if (errorBlob.contains("grammar") || errorBlob.contains("tense")) {
      weakestSkill = "grammar"
    }

    val cluster = clusters[weakestSkill] ?: clusters.getValue("listening")
    val excludedMastered = buildSet {
      addAll(userProgress.completedLessonIds)
      addAll(userProgress.masteredDrillIds)
      addAll(userProgress.masteredVocabIds.map { "vocab_$it" })
      addAll(userProgress.completedScenarioIds)
      hubProgress.values.filter { it.status == "mastered" }.forEach { add(it.resourceId) }
    }
    val completedHubIds = hubProgress.values.filter { it.status == "completed" }.map { it.resourceId }.toSet()
    val inProgressHubIds = hubProgress.values.filter { it.status == "in_progress" }.map { it.resourceId }.toSet()

    val weeklyXp = userProgress.weeklyHistory.sumOf { it.xp }

    val scored = resources.asSequence()
      .filter { res ->
        res.validationStatus != "rejected" &&
          (!offlineOnly || res.offlineCompatible) &&
          res.id !in excludedMastered
      }
      .mapNotNull { res ->
        val resIdx = cefrOrder.indexOf(res.cefrLevel)
        if (resIdx < 0) return@mapNotNull null
        val diff = abs(resIdx - targetIdx)
        if (diff > 1 || resIdx < targetIdx - 1) return@mapNotNull null

        var score = 0.0
        var primaryReason = "Recomendado para tu nivel $targetCefr"

        if (res.cefrLevel == targetCefr) {
          score += 46.0
        } else {
          score += 12.0
        }

        if (res.primarySkill == weakestSkill) {
          score += 36.0
          primaryReason = "Refuerza ${EduSkillCatalog.labelFor(weakestSkill)} ($targetCefr)"
        } else if (res.primarySkill in cluster.relatedSkills) {
          score += 24.0
          primaryReason = "Complementa tu meta de ${EduSkillCatalog.labelFor(weakestSkill)}"
        }

        val typeRank = cluster.preferredTypes.indexOf(res.contentType)
        if (typeRank >= 0) {
          score += (22.0 - typeRank * 3.0).coerceAtLeast(8.0)
        }

        if (res.contentType == "unit") score -= 12.0
        if (res.contentType == "vocabulary") score -= 8.0

        if (res.id in inProgressHubIds) {
          score += 20.0
          primaryReason = "Continúa donde lo dejaste"
        } else if (res.id in completedHubIds) {
          score -= 35.0
        } else if (res.id in recentResourceIds) {
          score -= 7.0
        }

        if (errorTags.any { tag ->
            tag.length >= 3 && (
              res.title.contains(tag, ignoreCase = true) ||
                res.description.contains(tag, ignoreCase = true) ||
                res.learningObjectiveEs.contains(tag, ignoreCase = true)
              )
          }
        ) {
          score += 18.0
          primaryReason = "Refuerza errores recientes detectados"
        }

        if (weeklyXp < 150 && res.estimatedDurationMin <= 5) {
          score += 7.0
        }
        if (res.sourceType == "owned") score += 5.0

        EduRecommendationItem(resource = res, score = score, reasonEs = primaryReason)
      }
      .sortedByDescending { it.score }
      .toList()

    val selected = mutableListOf<EduRecommendationItem>()
    val typeCounts = mutableMapOf<String, Int>()
    for (item in scored) {
      val ct = item.resource.contentType
      val cap = if (ct == cluster.preferredTypes.firstOrNull() || ct in setOf("listening", "reading", "grammar")) 3 else 2
      if ((typeCounts[ct] ?: 0) >= cap) continue
      selected.add(item)
      typeCounts[ct] = (typeCounts[ct] ?: 0) + 1
      if (selected.size >= limit) break
    }
    if (selected.size < limit) {
      val existingIds = selected.mapTo(mutableSetOf()) { it.resource.id }
      for (item in scored) {
        if (item.resource.id !in existingIds) {
          selected.add(item)
          existingIds.add(item.resource.id)
          if (selected.size >= limit) break
        }
      }
    }

    val steps = selected.take(6)
    val path = if (steps.isNotEmpty()) {
      EduLearningPath(
        id = "local_path_${targetCefr}_$weakestSkill",
        cefrLevel = targetCefr,
        focusSkill = weakestSkill,
        titleEs = "${cluster.titleEs} · $targetCefr",
        rationaleEs = "Plan adaptado a tu diagnóstico (${EduSkillCatalog.labelFor(weakestSkill)} prioritario en $targetCefr), excluyendo ${excludedMastered.size} elementos ya dominados.",
        estimatedMinutes = steps.sumOf { it.resource.estimatedDurationMin },
        steps = steps,
      )
    } else {
      null
    }

    return path to selected
  }
}
