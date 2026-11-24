package com.example.englishapp

import com.example.englishapp.data.CurriculumEngine
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PlacementResult
import com.example.englishapp.model.SkillScores
import com.example.englishapp.model.UserProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumEngineTest {

  @Test
  fun filtersStrictlyToActiveLevelAcrossAllCefrTiersAndPrioritizesUncompleted() {
    val settings = AppSettings(
      hasCompletedOnboarding = true,
      selfEstimatedLevel = LearnerLevel.NOVATO,
      verifiedLevel = LearnerLevel.INTERMEDIO,
      selectedLevel = LearnerLevel.INTERMEDIO,
      placementResult = PlacementResult(
        estimatedLevel = LearnerLevel.NOVATO,
        verifiedLevel = LearnerLevel.INTERMEDIO,
        overallScore = 79,
        skills = SkillScores(listening = 60, grammar = 82, vocabulary = 80, speaking = 76),
        recommendedFocusEs = "Priorizaremos comprensión auditiva.",
      ),
    )
    val progress = UserProgress(
      completedScenarioIds = setOf("job_interview"),
    )

    val curriculum = CurriculumEngine.buildCurriculum(settings, progress)
    assertEquals(LearnerLevel.INTERMEDIO, curriculum.activeLevel)
    assertEquals(8, curriculum.recommendedForLevel.size)
    assertTrue(curriculum.recommendedForLevel.all { it.scenario.recommendedLevel == LearnerLevel.INTERMEDIO })

    // Completed scenario must appear AFTER uncompleted ones
    assertFalse(curriculum.recommendedForLevel.first().isCompleted)
    assertEquals("job_interview", curriculum.recommendedForLevel.last().scenario.id)
    assertTrue(curriculum.recommendedForLevel.last().isCompleted)

    // Strict active-level filtering: never leak other levels' scenarios
    assertNull(curriculum.stretchChallenge)
    assertNull(curriculum.confidenceBuilder)
    assertEquals(8, curriculum.allScenarios.size)
    assertTrue(curriculum.allScenarios.all { it.recommendedLevel == LearnerLevel.INTERMEDIO })

    // Verify C1 user never sees beginner levels and vice versa
    for (level in LearnerLevel.entries) {
      val levelCurriculum = CurriculumEngine.buildCurriculum(
        settings.copy(verifiedLevel = level, selectedLevel = level),
        UserProgress(),
      )
      assertEquals(level, levelCurriculum.activeLevel)
      assertEquals(8, levelCurriculum.recommendedForLevel.size)
      assertEquals(8, levelCurriculum.allScenarios.size)
      assertTrue(levelCurriculum.allScenarios.all { it.recommendedLevel == level })
      assertNull(levelCurriculum.stretchChallenge)
      assertNull(levelCurriculum.confidenceBuilder)
    }
  }
}
