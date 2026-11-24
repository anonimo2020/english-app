package com.example.englishapp.data

import com.example.englishapp.chat.ChatClient
import com.example.englishapp.chat.HealthStatus
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.ChatMessage
import com.example.englishapp.model.ConversationTurn
import com.example.englishapp.model.EnglishAccent
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PlacementResult
import com.example.englishapp.model.SavedPhrase
import com.example.englishapp.model.SessionSummary
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.TutorCharacter
import com.example.englishapp.model.TutorReply
import com.example.englishapp.model.UserProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TutorRepository(private val store: AppPreferencesStore) {

  private val _settings = MutableStateFlow(store.loadSettings())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  private val _progress = MutableStateFlow(store.loadProgress())
  val progress: StateFlow<UserProgress> = _progress.asStateFlow()

  private val _savedPhrases = MutableStateFlow(store.loadSavedPhrases())
  val savedPhrases: StateFlow<List<SavedPhrase>> = _savedPhrases.asStateFlow()

  fun updateSettings(transform: (AppSettings) -> AppSettings) {
    val updated = transform(_settings.value)
    _settings.value = updated
    store.saveSettings(updated)
  }

  fun selectCharacter(character: TutorCharacter) {
    updateSettings { it.copy(selectedCharacter = character) }
  }

  fun selectLevel(level: LearnerLevel) {
    updateSettings { it.copy(selectedLevel = level, speechRateOverride = null) }
  }

  fun selectAccent(accent: EnglishAccent) {
    updateSettings { it.copy(accent = accent) }
  }

  fun setThemePreference(preference: ThemePreference) {
    updateSettings { it.copy(themePreference = preference) }
  }

  fun setSpeechRateOverride(rate: Float?) {
    updateSettings { it.copy(speechRateOverride = rate) }
  }

  fun toggleAutoListenHandsFree() {
    updateSettings { it.copy(autoListenHandsFree = !it.autoListenHandsFree) }
  }

  suspend fun sendTurn(
    scenarioId: String,
    history: List<ChatMessage>,
  ): Result<TutorReply> {
    val current = _settings.value
    if (current.offlineDemoMode) {
      delay(450)
      val reply = OfflineDemoTutor.generateReply(
        character = current.selectedCharacter,
        level = current.selectedLevel,
        scenarioId = scenarioId,
        accent = current.accent,
        history = history,
      )
      recordTurnProgress(reply.xpEarned)
      return Result.success(reply)
    }

    val url = current.backendUrl.trim()
    if (url.isBlank()) {
      return Result.failure(
        IllegalStateException("Configura la URL del backend en Ajustes o activa el Modo Demo Offline."),
      )
    }

    val client = ChatClient(baseUrl = url, appToken = current.appToken.trim())
    val result = client.send(
      character = current.selectedCharacter.id,
      level = current.selectedLevel.id,
      history = history,
      scenario = scenarioId,
      accent = current.accent.id,
    )
    result.onSuccess { reply ->
      recordTurnProgress(reply.xpEarned)
    }
    return result
  }

  suspend fun generateSessionSummary(
    scenarioId: String,
    turns: List<ConversationTurn>,
  ): SessionSummary {
    val current = _settings.value
    val fallback = OfflineDemoTutor.buildSessionSummary(
      character = current.selectedCharacter,
      level = current.selectedLevel,
      scenarioId = scenarioId,
      turns = turns,
    )
    val summary = if (!current.offlineDemoMode && current.backendUrl.isNotBlank()) {
      val client = ChatClient(baseUrl = current.backendUrl.trim(), appToken = current.appToken.trim())
      val history = turns.map { ChatMessage(it.role, it.content) }
      client.fetchSessionSummary(
        character = current.selectedCharacter.id,
        level = current.selectedLevel.id,
        scenario = scenarioId,
        history = history,
      ).getOrDefault(fallback)
    } else {
      fallback
    }

    val hasUserTurns = turns.any { it.role == "user" }
    val updatedProgress = store.applySessionCompleted(
      current = _progress.value,
      bonusXp = summary.xpBonus,
      scenarioId = scenarioId,
      markScenarioCompleted = hasUserTurns,
    )
    _progress.value = updatedProgress
    store.saveProgress(updatedProgress)
    return summary
  }

  fun savePlacementResult(estimatedLevel: LearnerLevel, result: PlacementResult) {
    updateSettings {
      it.copy(
        hasCompletedOnboarding = true,
        selfEstimatedLevel = estimatedLevel,
        verifiedLevel = result.verifiedLevel,
        selectedLevel = result.verifiedLevel,
        speechRateOverride = null,
        placementResult = result,
        showTranslationsByDefault = result.verifiedLevel in setOf(LearnerLevel.NOVATO, LearnerLevel.JUNIOR),
      )
    }
    awardXp(25, spokenTurnIncrement = 0)
  }

  fun markLessonCompleted(lessonId: String) {
    val updated = store.applyLessonCompleted(_progress.value, lessonId)
    _progress.value = updated
    store.saveProgress(updated)
  }

  fun markDrillMastered(drillId: String) {
    val updated = store.applyDrillMastered(_progress.value, drillId)
    _progress.value = updated
    store.saveProgress(updated)
  }

  fun markVocabMastered(wordIds: Collection<String>, bonusXp: Int) {
    val updated = store.applyVocabMastered(_progress.value, wordIds, bonusXp)
    _progress.value = updated
    store.saveProgress(updated)
  }

  fun resetOnboarding() {
    updateSettings { it.copy(hasCompletedOnboarding = false) }
  }

  fun awardXp(xpDelta: Int, spokenTurnIncrement: Int = 0) {
    val updated = store.applyTurnXp(_progress.value, xpDelta = xpDelta, spokenTurnIncrement = spokenTurnIncrement)
    _progress.value = updated
    store.saveProgress(updated)
  }

  suspend fun testConnection(url: String, token: String): Result<HealthStatus> {
    val client = ChatClient(baseUrl = url, appToken = token)
    return client.checkHealth()
  }

  fun savePhrase(
    english: String,
    spanish: String,
    note: String = "",
    source: String = "Conversación",
  ) {
    val cleanEn = english.trim()
    if (cleanEn.isEmpty()) return
    val current = _savedPhrases.value
    if (current.any { it.english.equals(cleanEn, ignoreCase = true) }) return
    val item = SavedPhrase(
      id = "phr-${System.currentTimeMillis()}-${current.size}",
      english = cleanEn,
      spanish = spanish.trim(),
      note = note.trim(),
      source = source,
    )
    val updated = listOf(item) + current
    _savedPhrases.value = updated
    store.savePhrases(updated)
  }

  fun removePhrase(phraseId: String) {
    val updated = _savedPhrases.value.filterNot { it.id == phraseId }
    _savedPhrases.value = updated
    store.savePhrases(updated)
  }

  private fun recordTurnProgress(xpEarned: Int) {
    val updated = store.applyTurnXp(_progress.value, xpDelta = xpEarned, spokenTurnIncrement = 1)
    _progress.value = updated
    store.saveProgress(updated)
  }
}
