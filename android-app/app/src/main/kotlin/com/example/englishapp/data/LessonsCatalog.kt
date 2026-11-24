package com.example.englishapp.data

import com.example.englishapp.data.curriculum.CourseLesson
import com.example.englishapp.data.curriculum.CourseUnit
import com.example.englishapp.data.curriculum.CurriculumA1
import com.example.englishapp.data.curriculum.CurriculumA2
import com.example.englishapp.data.curriculum.CurriculumB1
import com.example.englishapp.data.curriculum.CurriculumB2
import com.example.englishapp.data.curriculum.CurriculumC1
import com.example.englishapp.data.curriculum.CurriculumC2
import com.example.englishapp.model.LearnerLevel

// Alias público para compatibilidad clara en UI/tests
typealias LessonUnit = CourseLesson

/**
 * Catálogo maestro CEFR A1–C2: 24 unidades temáticas (4 por nivel) con
 * 96 lecciones multi-paso (16 por nivel), 288 tarjetas de enseñanza,
 * 288 mini-tests inmediatos y 96 retos de speaking guiado.
 */
object LessonsCatalog {

  val allUnits: List<CourseUnit> = (
    CurriculumA1.units +
      CurriculumA2.units +
      CurriculumB1.units +
      CurriculumB2.units +
      CurriculumC1.units +
      CurriculumC2.units
    )

  val all: List<CourseLesson> = allUnits.flatMap { it.lessons }

  fun unitsForLevel(level: LearnerLevel): List<CourseUnit> =
    allUnits.filter { it.level == level }

  fun forLevel(level: LearnerLevel): List<CourseLesson> =
    all.filter { it.level == level }

  fun byId(lessonId: String): CourseLesson? =
    all.firstOrNull { it.id == lessonId }

  fun unitById(unitId: String): CourseUnit? =
    allUnits.firstOrNull { it.id == unitId }

  fun nextLessonInUnit(currentLessonId: String): CourseLesson? {
    val current = byId(currentLessonId) ?: return null
    val unit = unitById(current.unitId) ?: return null
    val idx = unit.lessons.indexOfFirst { it.id == currentLessonId }
    return if (idx in 0 until unit.lessons.lastIndex) unit.lessons[idx + 1] else null
  }
}
