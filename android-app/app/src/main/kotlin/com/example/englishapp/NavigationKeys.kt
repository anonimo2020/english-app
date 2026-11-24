package com.example.englishapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data class OnboardingRoute(
  val targetLevelId: String? = null,
  val skipLevelSelection: Boolean = false,
) : NavKey

@Serializable data object HomeRoute : NavKey

@Serializable data object ExploreRoute : NavKey

@Serializable data object ProgressRoute : NavKey

@Serializable data object SettingsRoute : NavKey

@Serializable data class ConversationRoute(
  val scenarioId: String,
  val sessionId: Long = 0L,
) : NavKey

@Serializable data class LessonPlayerRoute(
  val lessonId: String,
) : NavKey

@Serializable data class VocabPracticeRoute(
  val categoryFilter: String? = null,
  val startWordId: String? = null,
  val gameOnlyMode: Boolean = false,
) : NavKey

@Serializable data object LearningHubRoute : NavKey

@Serializable data class EduResourceDetailRoute(
  val resourceId: String,
) : NavKey
