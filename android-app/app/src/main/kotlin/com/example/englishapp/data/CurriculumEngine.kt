package com.example.englishapp.data

import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.ConversationScenario
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.ScenarioCatalog
import com.example.englishapp.model.UserProgress

data class RecommendedScenarioItem(
  val scenario: ConversationScenario,
  val reasonTag: String,
  val isCompleted: Boolean,
)

data class PersonalizedCurriculum(
  val activeLevel: LearnerLevel,
  val recommendedForLevel: List<RecommendedScenarioItem>,
  val stretchChallenge: RecommendedScenarioItem? = null,
  val confidenceBuilder: RecommendedScenarioItem? = null,
  val allScenarios: List<ConversationScenario>,
)

/**
 * Motor curricular estricto por nivel verificado: muestra exclusivamente
 * temas y habilidades acordes al nivel activo del usuario (nunca mezcla
 * niveles de principiante a usuarios avanzados ni viceversa).
 */
object CurriculumEngine {

  fun buildCurriculum(
    settings: AppSettings,
    progress: UserProgress,
  ): PersonalizedCurriculum {
    val level = settings.activeLevel()
    val completed = progress.completedScenarioIds
    val skills = settings.placementResult?.skills

    val levelScenarios = ScenarioCatalog.all
      .filter { it.recommendedLevel == level }
      .sortedWith(
        compareBy<ConversationScenario> { it.id in completed }
          .thenBy { scenarioSkillPriority(it, skills?.listening ?: 75, skills?.speaking ?: 75) },
      )
      .map { scenario ->
        val done = scenario.id in completed
        val tag = when {
          done -> "Completado ✓"
          skills != null && skills.listening < 65 && scenario.focusSkillEs.contains("Pedidos", ignoreCase = true) ->
            "Refuerza tu oído"
          else -> "Ideal para tu nivel ${level.cefr}"
        }
        RecommendedScenarioItem(
          scenario = scenario,
          reasonTag = tag,
          isCompleted = done,
        )
      }

    return PersonalizedCurriculum(
      activeLevel = level,
      recommendedForLevel = levelScenarios,
      stretchChallenge = null,
      confidenceBuilder = null,
      allScenarios = levelScenarios.map { it.scenario },
    )
  }

  private fun scenarioSkillPriority(
    scenario: ConversationScenario,
    listeningScore: Int,
    speakingScore: Int,
  ): Int {
    if (listeningScore < 65 && scenario.id in setOf("coffee_shop", "free_chat", "airport_travel")) {
      return 0
    }
    if (speakingScore < 65 && scenario.id in setOf("greetings_intro", "restaurant", "movies_tech_opinions")) {
      return 0
    }
    return 1
  }
}
