package com.example.englishapp.data.curriculum

import com.example.englishapp.model.LearnerLevel

enum class LessonItemKind(val badgeEs: String) {
  CONCEPT("Concepto clave"),
  VOCAB_FOCUS("Vocabulario en uso"),
  SPANISH_TRAP("Trampa Español → Inglés"),
  CONTEXT_DIALOGUE("Uso en contexto"),
  SPECIALIZED_SKILL("Mini-ítem especializado"),
}

enum class MiniTestKind {
  MULTIPLE_CHOICE,
  WORD_BUILDER,
  LISTENING_CHOICE,
}

data class MiniTestQuestion(
  val kind: MiniTestKind,
  val promptEs: String,
  val audioPromptEn: String? = null,
  val options: List<String> = emptyList(),
  val correctIndex: Int = 0,
  val targetTokens: List<String> = emptyList(),
  val distractionTokens: List<String> = emptyList(),
  val explanationEs: String,
)

data class LessonStepItem(
  val id: String,
  val kind: LessonItemKind,
  val titleEs: String,
  val titleEn: String,
  val explanationEs: String,
  val modelEn: String,
  val modelEs: String,
  val tipEs: String,
  val miniTest: MiniTestQuestion,
)

data class CourseLesson(
  val id: String,
  val unitId: String,
  val orderInUnit: Int,
  val level: LearnerLevel,
  val titleEs: String,
  val titleEn: String,
  val durationMin: Int,
  val items: List<LessonStepItem>,
  val speakingTargetEn: String,
  val speakingTargetEs: String,
  val speakingTipEs: String,
)

data class CourseUnit(
  val id: String,
  val level: LearnerLevel,
  val orderInLevel: Int,
  val titleEs: String,
  val titleEn: String,
  val descriptionEs: String,
  val specializedFocusEs: String,
  val iconName: String,
  val lessons: List<CourseLesson>,
)
