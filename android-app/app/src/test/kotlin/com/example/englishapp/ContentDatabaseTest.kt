package com.example.englishapp

import com.example.englishapp.data.LearningLibrary
import com.example.englishapp.data.LessonsCatalog
import com.example.englishapp.data.VocabularyCatalog
import com.example.englishapp.data.curriculum.MiniTestKind
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.ScenarioCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentDatabaseTest {

  @Test
  fun scenariosHave8PerLevelAcrossAll6CefrLevels() {
    assertEquals(48, ScenarioCatalog.all.size)
    assertEquals(48, ScenarioCatalog.all.map { it.id }.toSet().size)

    for (level in LearnerLevel.entries) {
      val forLevel = ScenarioCatalog.all.filter { it.recommendedLevel == level }
      assertEquals("Expected 8 scenarios for ${level.id}", 8, forLevel.size)
    }

    for (scenario in ScenarioCatalog.all) {
      assertTrue("Missing coachIntroEs in ${scenario.id}", scenario.coachIntroEs.isNotBlank())
      assertTrue("Missing openingEn in ${scenario.id}", scenario.openingEn.isNotBlank())
      assertTrue("Missing openingEs in ${scenario.id}", scenario.openingEs.isNotBlank())
    }
  }

  @Test
  fun curriculumHas24Units96LessonsAnd288StepItemsWithImmediateMiniTests() {
    assertEquals(24, LessonsCatalog.allUnits.size)
    assertEquals(24, LessonsCatalog.allUnits.map { it.id }.toSet().size)
    assertEquals(96, LessonsCatalog.all.size)
    assertEquals(96, LessonsCatalog.all.map { it.id }.toSet().size)

    for (level in LearnerLevel.entries) {
      val units = LessonsCatalog.unitsForLevel(level)
      assertEquals("Expected 4 units for ${level.id}", 4, units.size)
      val lessons = LessonsCatalog.forLevel(level)
      assertEquals("Expected 16 lessons for ${level.id}", 16, lessons.size)
    }

    for (unit in LessonsCatalog.allUnits) {
      assertEquals("Expected 4 lessons in unit ${unit.id}", 4, unit.lessons.size)
      assertTrue("Blank specializedFocusEs in ${unit.id}", unit.specializedFocusEs.isNotBlank())
      assertTrue("Blank descriptionEs in ${unit.id}", unit.descriptionEs.isNotBlank())
    }

    for (lesson in LessonsCatalog.all) {
      assertEquals("Expected 3 sequential items in ${lesson.id}", 3, lesson.items.size)
      assertTrue("Missing speakingTargetEn for ${lesson.id}", lesson.speakingTargetEn.isNotBlank())
      assertTrue("Missing speakingTargetEs for ${lesson.id}", lesson.speakingTargetEs.isNotBlank())
      assertTrue("Missing speakingTipEs for ${lesson.id}", lesson.speakingTipEs.isNotBlank())

      for (item in lesson.items) {
        assertTrue("Blank titleEs in ${lesson.id}/${item.id}", item.titleEs.isNotBlank())
        assertTrue("Blank explanationEs in ${lesson.id}/${item.id}", item.explanationEs.isNotBlank())
        assertTrue("Blank modelEn in ${lesson.id}/${item.id}", item.modelEn.isNotBlank())
        assertTrue("Blank modelEs in ${lesson.id}/${item.id}", item.modelEs.isNotBlank())
        assertTrue("Blank tipEs in ${lesson.id}/${item.id}", item.tipEs.isNotBlank())

        val q = item.miniTest
        assertTrue("Blank miniTest promptEs in ${lesson.id}/${item.id}", q.promptEs.isNotBlank())
        assertTrue("Blank miniTest explanationEs in ${lesson.id}/${item.id}", q.explanationEs.isNotBlank())
        when (q.kind) {
          MiniTestKind.WORD_BUILDER -> {
            assertTrue("Empty targetTokens in ${lesson.id}/${item.id}", q.targetTokens.isNotEmpty())
          }
          MiniTestKind.LISTENING_CHOICE -> {
            assertFalse("Missing audioPromptEn in ${lesson.id}/${item.id}", q.audioPromptEn.isNullOrBlank())
            assertTrue("Too few options in ${lesson.id}/${item.id}", q.options.size >= 2)
            assertTrue("Invalid correctIndex in ${lesson.id}/${item.id}", q.correctIndex in q.options.indices)
          }
          MiniTestKind.MULTIPLE_CHOICE -> {
            assertTrue("Too few options in ${lesson.id}/${item.id}", q.options.size >= 2)
            assertTrue("Invalid correctIndex in ${lesson.id}/${item.id}", q.correctIndex in q.options.indices)
          }
        }
      }
    }
  }

  @Test
  fun vocabularyHas288CuratedWordsAcrossAll6Levels() {
    assertEquals(288, VocabularyCatalog.all.size)
    assertEquals(288, VocabularyCatalog.all.map { it.id }.toSet().size)

    for (level in LearnerLevel.entries) {
      val forLevel = VocabularyCatalog.forLevel(level)
      assertEquals("Expected 48 words for ${level.id}", 48, forLevel.size)
      assertTrue(
        "Expected multiple categories for ${level.id}",
        VocabularyCatalog.categoriesForLevel(level).size >= 3,
      )
    }

    for (entry in VocabularyCatalog.all) {
      assertTrue("Blank word in ${entry.id}", entry.word.isNotBlank())
      assertTrue("Blank IPA in ${entry.id}", entry.ipa.isNotBlank())
      assertTrue("Blank Spanish in ${entry.id}", entry.spanish.isNotBlank())
      assertTrue("Blank exampleEn in ${entry.id}", entry.exampleEn.isNotBlank())
      assertTrue("Blank exampleEs in ${entry.id}", entry.exampleEs.isNotBlank())
      assertTrue("Blank tipEs in ${entry.id}", entry.tipEs.isNotBlank())
    }
  }

  @Test
  fun learningLibraryHas18Stories30DrillsAnd8WebPortalsAcross6Levels() {
    assertEquals(18, LearningLibrary.stories.size)
    assertEquals(18, LearningLibrary.stories.map { it.id }.toSet().size)
    for (level in LearnerLevel.entries) {
      assertEquals(
        "Expected 3 stories for ${level.id}",
        3,
        LearningLibrary.storiesForLevel(level).size,
      )
    }
    for (story in LearningLibrary.stories) {
      assertTrue(story.bodyEn.isNotBlank())
      assertTrue(story.bodyEs.isNotBlank())
      assertTrue(story.questions.isNotEmpty())
      story.questions.forEach { q ->
        assertTrue(q.correctIndex in q.options.indices)
      }
    }

    assertEquals(30, LearningLibrary.drills.size)
    assertEquals(30, LearningLibrary.drills.map { it.id }.toSet().size)
    for (level in LearnerLevel.entries) {
      assertEquals(
        "Expected 5 drills for ${level.id}",
        5,
        LearningLibrary.drillsForLevel(level).size,
      )
    }

    assertEquals(8, LearningLibrary.webPortals.size)
    assertTrue(LearningLibrary.webPortals.all { it.url.startsWith("https://") })
  }
}
