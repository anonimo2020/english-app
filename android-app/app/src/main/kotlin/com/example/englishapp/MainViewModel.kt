package com.example.englishapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.audio.AudioTutorEngine
import com.example.englishapp.data.AppPreferencesStore
import com.example.englishapp.data.DictionaryEntryResult
import com.example.englishapp.data.FreeWebContentClient
import com.example.englishapp.data.TutorRepository
import com.example.englishapp.edu.EduRepository
import com.example.englishapp.edu.EduResource
import com.example.englishapp.edu.LearningHubUiState
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.EnglishAccent
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PlacementResult
import com.example.englishapp.model.SavedPhrase
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.TutorCharacter
import com.example.englishapp.model.UserProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConnectionTestState {
  data object Idle : ConnectionTestState
  data object Testing : ConnectionTestState
  data class Success(val message: String) : ConnectionTestState
  data class Error(val message: String) : ConnectionTestState
}

data class DictionaryUiState(
  val isLoading: Boolean = false,
  val result: DictionaryEntryResult? = FreeWebContentClient.offlineFallback["fluent"],
  val error: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val store = AppPreferencesStore(application)
  val repository = TutorRepository(store)
  val eduRepository = EduRepository(application, repository)
  val audioEngine = AudioTutorEngine(application)

  val settings: StateFlow<AppSettings> = repository.settings
  val progress: StateFlow<UserProgress> = repository.progress
  val savedPhrases: StateFlow<List<SavedPhrase>> = repository.savedPhrases
  val audioState: StateFlow<AudioEngineState> = audioEngine.state
  val learningHubState: StateFlow<LearningHubUiState> = eduRepository.state

  private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
  val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

  private val _dictionaryState = MutableStateFlow(DictionaryUiState())
  val dictionaryState: StateFlow<DictionaryUiState> = _dictionaryState.asStateFlow()

  fun selectCharacter(character: TutorCharacter) {
    repository.selectCharacter(character)
  }

  fun previewCharacterGreeting(character: TutorCharacter) {
    val previewId = "preview-${character.id}"
    val currentAudio = audioState.value
    if (currentAudio.isSpeaking && currentAudio.speakingMessageId == previewId) {
      audioEngine.stopSpeaking()
      return
    }
    repository.selectCharacter(character)
    val currentSettings = settings.value
    audioEngine.speak(
      text = character.sampleGreetingEn,
      messageId = previewId,
      character = character,
      accent = currentSettings.accent,
      speedRate = currentSettings.effectiveSpeechRate(),
    )
  }

  fun selectLevel(level: LearnerLevel) {
    repository.selectLevel(level)
    eduRepository.selectLevelFilter(level.cefr)
    eduRepository.refreshRecommendations()
  }

  fun selectAccent(accent: EnglishAccent) {
    repository.selectAccent(accent)
  }

  fun setThemePreference(preference: ThemePreference) {
    repository.setThemePreference(preference)
  }

  fun updateOfflineDemoMode(enabled: Boolean) {
    repository.updateSettings { it.copy(offlineDemoMode = enabled) }
  }

  fun updateBackendCredentials(url: String, token: String) {
    repository.updateSettings {
      it.copy(
        backendUrl = url.trim(),
        appToken = token.trim(),
      )
    }
  }

  fun updateAutoPlayReplies(enabled: Boolean) {
    repository.updateSettings { it.copy(autoPlayReplies = enabled) }
  }

  fun updateAutoListenHandsFree(enabled: Boolean) {
    repository.updateSettings { it.copy(autoListenHandsFree = enabled) }
  }

  fun updateShowTranslationsByDefault(enabled: Boolean) {
    repository.updateSettings { it.copy(showTranslationsByDefault = enabled) }
  }

  fun updatePreferFaceToFaceMode(enabled: Boolean) {
    repository.updateSettings { it.copy(preferFaceToFaceMode = enabled) }
  }

  fun savePlacementResult(estimatedLevel: LearnerLevel, result: PlacementResult) {
    repository.savePlacementResult(estimatedLevel, result)
    eduRepository.selectLevelFilter(result.verifiedLevel.cefr)
    eduRepository.refreshRecommendations()
  }

  fun resetOnboarding() {
    stopAudio()
    repository.resetOnboarding()
  }

  fun testConnection(url: String, token: String) {
    updateBackendCredentials(url, token)
    viewModelScope.launch {
      _connectionTestState.value = ConnectionTestState.Testing
      val result = repository.testConnection(url, token)
      _connectionTestState.value = result.fold(
        onSuccess = { health ->
          val aiText = if (health.aiConfigured) "IA lista" else "falta AI_API_KEY en el servidor"
          ConnectionTestState.Success("Conectado (${health.latencyMs} ms) · v${health.version} · $aiText")
        },
        onFailure = { err ->
          ConnectionTestState.Error(err.message ?: "No se pudo conectar al servidor.")
        },
      )
    }
  }

  fun lookupDictionaryWord(rawWord: String) {
    val clean = rawWord.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      _dictionaryState.value = _dictionaryState.value.copy(isLoading = true, error = null)
      val res = FreeWebContentClient.lookupWord(clean)
      _dictionaryState.value = res.fold(
        onSuccess = { entry ->
          DictionaryUiState(isLoading = false, result = entry, error = null)
        },
        onFailure = { err ->
          _dictionaryState.value.copy(
            isLoading = false,
            error = err.message ?: "No se pudo consultar la palabra.",
          )
        },
      )
    }
  }

  fun playDictionaryAudio(entry: DictionaryEntryResult, slow: Boolean) {
    val msgId = "dict-${entry.word}"
    val currentAudio = audioState.value
    if (currentAudio.isSpeaking && currentAudio.speakingMessageId == msgId) {
      audioEngine.stopSpeaking()
      return
    }
    val currentSettings = settings.value
    if (slow || entry.audioUrl.isNullOrBlank()) {
      audioEngine.speak(
        text = entry.word,
        messageId = msgId,
        character = currentSettings.selectedCharacter,
        accent = currentSettings.accent,
        speedRate = if (slow) 0.65f else currentSettings.effectiveSpeechRate(),
      )
    } else {
      audioEngine.playAudioUrlOrTts(
        audioUrl = entry.audioUrl,
        fallbackText = entry.word,
        messageId = msgId,
        character = currentSettings.selectedCharacter,
        accent = currentSettings.accent,
        speedRate = currentSettings.effectiveSpeechRate(),
      )
    }
  }

  fun playExploreText(text: String, tag: String, slow: Boolean) {
    if (text.isBlank()) {
      audioEngine.stopSpeaking()
      return
    }
    val currentAudio = audioState.value
    if (currentAudio.isSpeaking && currentAudio.speakingMessageId == tag) {
      audioEngine.stopSpeaking()
      return
    }
    val currentSettings = settings.value
    audioEngine.speak(
      text = text,
      messageId = tag,
      character = currentSettings.selectedCharacter,
      accent = currentSettings.accent,
      speedRate = if (slow) 0.65f else currentSettings.effectiveSpeechRate(),
    )
  }

  fun playSpanishText(text: String, tag: String) {
    if (text.isBlank()) {
      audioEngine.stopSpeaking()
      return
    }
    val currentAudio = audioState.value
    if (currentAudio.isSpeaking && currentAudio.speakingMessageId == tag) {
      audioEngine.stopSpeaking()
      return
    }
    audioEngine.speakSpanish(
      text = text,
      messageId = tag,
      character = settings.value.selectedCharacter,
    )
  }

  fun startSpeechCheck(silenceMs: Long, onResult: (String) -> Unit) {
    audioEngine.startListening(
      accent = settings.value.accent,
      silenceMs = silenceMs,
      onFinalResult = onResult,
    )
  }

  fun stopSpeechCheck() {
    audioEngine.finishListeningAndSubmit()
  }

  fun savePhrase(
    english: String,
    spanish: String,
    note: String,
    source: String,
  ) {
    repository.savePhrase(english, spanish, note, source)
  }

  fun awardStoryCompleted() {
    repository.awardXp(20, spokenTurnIncrement = 0)
  }

  fun markLessonCompleted(lessonId: String) {
    repository.markLessonCompleted(lessonId)
    eduRepository.refreshRecommendations()
  }

  fun markDrillMastered(drillId: String) {
    repository.markDrillMastered(drillId)
    eduRepository.refreshRecommendations()
  }

  fun markVocabMastered(wordIds: Collection<String>, bonusXp: Int) {
    repository.markVocabMastered(wordIds, bonusXp)
    eduRepository.refreshRecommendations()
  }

  fun initLearningHub() = eduRepository.ensureInitialized()

  fun updateHubSearch(query: String) = eduRepository.updateSearch(query)

  fun selectHubLevel(level: String?) = eduRepository.selectLevelFilter(level)

  fun selectHubCategory(categoryId: String?) = eduRepository.selectCategoryFilter(categoryId)

  fun selectHubSkill(skillId: String?) = eduRepository.selectSkillFilter(skillId)

  fun selectHubSourceType(sourceType: String?) = eduRepository.selectSourceTypeFilter(sourceType)

  fun toggleHubOfflineOnly() = eduRepository.toggleOfflineOnly()

  fun resetHubFilters() = eduRepository.resetAllFilters(keepActiveLevel = false)

  fun toggleHubFavorite(resourceId: String) = eduRepository.toggleFavorite(resourceId)

  fun recordHubOpened(resource: EduResource, isExternal: Boolean = false) =
    eduRepository.recordResourceOpened(resource, isExternal)

  fun completeHubResource(
    resource: EduResource,
    scorePct: Float,
    timeSpentSec: Int,
    errorTags: List<String> = emptyList(),
  ) = eduRepository.recordActivityOutcome(resource, scorePct, timeSpentSec, errorTags)

  fun syncHubWithBackend() = eduRepository.syncWithBackendNow()

  fun searchLiveOpenWebProviders(query: String) = eduRepository.searchOpenProvidersLive(query)

  fun playSavedPhrase(phrase: SavedPhrase, slow: Boolean) {
    val msgId = if (slow) "phrase-slow-${phrase.id}" else "phrase-${phrase.id}"
    val currentAudio = audioState.value
    if (currentAudio.isSpeaking && currentAudio.speakingMessageId == msgId) {
      audioEngine.stopSpeaking()
      return
    }
    val currentSettings = settings.value
    val speed = if (slow) 0.65f else currentSettings.effectiveSpeechRate()
    audioEngine.speak(
      text = phrase.english,
      messageId = msgId,
      character = currentSettings.selectedCharacter,
      accent = currentSettings.accent,
      speedRate = speed,
    )
  }

  fun removeSavedPhrase(id: String) {
    repository.removePhrase(id)
  }

  fun stopAudio() {
    audioEngine.stopSpeaking()
    audioEngine.cancelListening()
  }

  override fun onCleared() {
    super.onCleared()
    audioEngine.release()
  }
}
