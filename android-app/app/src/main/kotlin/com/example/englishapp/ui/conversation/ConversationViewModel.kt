package com.example.englishapp.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.audio.AudioTutorEngine
import com.example.englishapp.data.OfflineDemoTutor
import com.example.englishapp.data.SpeakingAnalysis
import com.example.englishapp.data.SpeakingCoachEngine
import com.example.englishapp.data.TutorRepository
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.AvatarMood
import com.example.englishapp.model.ChatMessage
import com.example.englishapp.model.ConversationScenario
import com.example.englishapp.model.ConversationTurn
import com.example.englishapp.model.Correction
import com.example.englishapp.model.EnglishAccent
import com.example.englishapp.model.PronunciationTip
import com.example.englishapp.model.ScenarioCatalog
import com.example.englishapp.model.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationUiState(
  val scenario: ConversationScenario,
  val settings: AppSettings,
  val audio: AudioEngineState,
  val turns: List<ConversationTurn> = emptyList(),
  val isThinking: Boolean = false,
  val errorBanner: String? = null,
  val revealedTranslations: Set<String> = emptySet(),
  val expandedTipKey: String? = null,
  val expandedTipText: PronunciationTip? = null,
  val sessionXp: Int = 0,
  val sessionTurnsCount: Int = 0,
  val correctionsCount: Int = 0,
  val isSummarizing: Boolean = false,
  val showSummarySheet: Boolean = false,
  val sessionSummary: SessionSummary? = null,
  val isFaceToFaceMode: Boolean = true,
  val practiceCorrectionTarget: Correction? = null,
  val correctionPracticeAnalysis: SpeakingAnalysis? = null,
  val correctionPracticeAttempts: Int = 0,
  val isListeningForPractice: Boolean = false,
) {
  val avatarMood: AvatarMood
    get() = when {
      audio.isListening -> AvatarMood.LISTENING
      isThinking -> AvatarMood.THINKING
      audio.isSpeaking -> AvatarMood.SPEAKING
      else -> AvatarMood.IDLE
    }

  val statusLabelEs: String
    get() = when (avatarMood) {
      AvatarMood.LISTENING -> "Te escucho en ${settings.accent.badge}..."
      AvatarMood.THINKING -> "${settings.selectedCharacter.displayName} está pensando..."
      AvatarMood.SPEAKING -> "${settings.selectedCharacter.displayName} está hablando..."
      AvatarMood.IDLE -> "Toca el micrófono y di lo que quieras"
    }

  val activeSuggestions: List<String>
    get() = turns.lastOrNull { it.role == "assistant" }?.suggestedReplies.orEmpty()

  val latestAssistantTurn: ConversationTurn?
    get() = turns.lastOrNull { it.role == "assistant" }

  val latestUserTurn: ConversationTurn?
    get() = turns.lastOrNull { it.role == "user" }
}

class ConversationViewModel(
  scenarioId: String,
  private val repository: TutorRepository,
  private val audioEngine: AudioTutorEngine,
) : ViewModel() {

  private val scenario = ScenarioCatalog.byId(scenarioId)

  private data class LocalConversationState(
    val turns: List<ConversationTurn>,
    val isThinking: Boolean = false,
    val errorBanner: String? = null,
    val revealedTranslations: Set<String> = emptySet(),
    val expandedTipKey: String? = null,
    val expandedTipText: PronunciationTip? = null,
    val sessionXp: Int = 0,
    val sessionTurnsCount: Int = 0,
    val correctionsCount: Int = 0,
    val isSummarizing: Boolean = false,
    val showSummarySheet: Boolean = false,
    val sessionSummary: SessionSummary? = null,
    val isFaceToFaceMode: Boolean = true,
    val practiceCorrectionTarget: Correction? = null,
    val correctionPracticeAnalysis: SpeakingAnalysis? = null,
    val correctionPracticeAttempts: Int = 0,
    val isListeningForPractice: Boolean = false,
  )

  private val openingTurn = OfflineDemoTutor.buildOpeningTurn(
    scenario = scenario,
    character = repository.settings.value.selectedCharacter,
    level = repository.settings.value.selectedLevel,
  )

  private val localState = MutableStateFlow(
    LocalConversationState(
      turns = listOf(openingTurn),
      isFaceToFaceMode = repository.settings.value.preferFaceToFaceMode,
    ),
  )

  val uiState: StateFlow<ConversationUiState> = combine(
    repository.settings,
    audioEngine.state,
    localState,
  ) { settings, audio, local ->
    ConversationUiState(
      scenario = scenario,
      settings = settings,
      audio = audio,
      turns = local.turns,
      isThinking = local.isThinking,
      errorBanner = local.errorBanner,
      revealedTranslations = local.revealedTranslations,
      expandedTipKey = local.expandedTipKey,
      expandedTipText = local.expandedTipText,
      sessionXp = local.sessionXp,
      sessionTurnsCount = local.sessionTurnsCount,
      correctionsCount = local.correctionsCount,
      isSummarizing = local.isSummarizing,
      showSummarySheet = local.showSummarySheet,
      sessionSummary = local.sessionSummary,
      isFaceToFaceMode = local.isFaceToFaceMode,
      practiceCorrectionTarget = local.practiceCorrectionTarget,
      correctionPracticeAnalysis = local.correctionPracticeAnalysis,
      correctionPracticeAttempts = local.correctionPracticeAttempts,
      isListeningForPractice = audio.isListening && local.isListeningForPractice,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = ConversationUiState(
      scenario = scenario,
      settings = repository.settings.value,
      audio = audioEngine.state.value,
      turns = listOf(openingTurn),
      isFaceToFaceMode = repository.settings.value.preferFaceToFaceMode,
    ),
  )

  private var autoPlayedOpening = false

  fun onScreenVisible(hasMicPermission: Boolean) {
    if (!autoPlayedOpening) {
      autoPlayedOpening = true
      val currentSettings = repository.settings.value
      if (currentSettings.autoPlayReplies) {
        speakTurn(openingTurn, slow = false, triggerAutoListen = hasMicPermission)
      }
    }
  }

  fun sendUserMessage(rawText: String, hasMicPermission: Boolean) {
    val text = rawText.trim()
    if (text.isEmpty() || localState.value.isThinking) return

    audioEngine.stopSpeaking()
    audioEngine.stopListening()
    audioEngine.clearError()

    val userTurn = ConversationTurn(
      id = "usr-${System.nanoTime()}",
      role = "user",
      content = text,
    )

    val updatedTurns = localState.value.turns + userTurn
    localState.update {
      it.copy(
        turns = updatedTurns,
        isThinking = true,
        errorBanner = null,
      )
    }

    val history = updatedTurns.map { ChatMessage(it.role, it.content) }

    viewModelScope.launch {
      val result = repository.sendTurn(scenario.id, history)
      result.fold(
        onSuccess = { reply ->
          val assistantTurn = ConversationTurn(
            id = "ast-${System.nanoTime()}",
            role = "assistant",
            content = reply.reply,
            translationEs = reply.translationEs,
            correction = reply.correction,
            pronunciationFocus = reply.pronunciationFocus,
            suggestedReplies = reply.suggestedReplies,
            coachNoteEs = reply.coachNoteEs,
          )
          localState.update { state ->
            state.copy(
              turns = state.turns + assistantTurn,
              isThinking = false,
              sessionXp = state.sessionXp + reply.xpEarned,
              sessionTurnsCount = state.sessionTurnsCount + 1,
              correctionsCount = state.correctionsCount + if (reply.correction != null) 1 else 0,
            )
          }
          if (repository.settings.value.autoPlayReplies) {
            speakTurn(assistantTurn, slow = false, triggerAutoListen = hasMicPermission)
          }
        },
        onFailure = { err ->
          localState.update {
            it.copy(
              isThinking = false,
              errorBanner = err.message ?: "No se pudo obtener respuesta del tutor.",
            )
          }
        },
      )
    }
  }

  fun toggleMicListening(hasMicPermission: Boolean) {
    val audio = audioEngine.state.value
    if (audio.isListening) {
      audioEngine.finishListeningAndSubmit()
      return
    }
    startListeningInternal(hasMicPermission)
  }

  private fun startListeningInternal(hasMicPermission: Boolean) {
    if (!hasMicPermission) return
    val currentSettings = repository.settings.value
    audioEngine.startListening(
      accent = currentSettings.accent,
      silenceMs = currentSettings.selectedLevel.sttSilenceMs,
    ) { recognizedText ->
      sendUserMessage(recognizedText, hasMicPermission = true)
    }
  }

  fun speakTurn(
    turn: ConversationTurn,
    slow: Boolean,
    triggerAutoListen: Boolean = false,
  ) {
    val messageId = if (slow) "${turn.id}-slow" else turn.id
    val currentAudio = audioEngine.state.value
    if (currentAudio.isSpeaking && currentAudio.speakingMessageId == messageId) {
      audioEngine.stopSpeaking()
      return
    }
    val currentSettings = repository.settings.value
    val speed = if (slow) 0.65f else currentSettings.effectiveSpeechRate()
    audioEngine.speak(
      text = turn.content,
      messageId = messageId,
      character = currentSettings.selectedCharacter,
      accent = currentSettings.accent,
      speedRate = speed,
      onDone = {
        val latest = repository.settings.value
        if (triggerAutoListen && latest.autoListenHandsFree && !localState.value.showSummarySheet) {
          startListeningInternal(hasMicPermission = true)
        }
      },
    )
  }

  fun speakSimpleText(text: String, tag: String, slow: Boolean = false) {
    val currentAudio = audioEngine.state.value
    if (currentAudio.isSpeaking && currentAudio.speakingMessageId == tag) {
      audioEngine.stopSpeaking()
      return
    }
    val currentSettings = repository.settings.value
    val speed = if (slow) 0.70f else currentSettings.effectiveSpeechRate()
    audioEngine.speak(
      text = text,
      messageId = tag,
      character = currentSettings.selectedCharacter,
      accent = currentSettings.accent,
      speedRate = speed,
    )
  }

  fun speakSpanishText(text: String, tag: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    val currentAudio = audioEngine.state.value
    if (currentAudio.isSpeaking && currentAudio.speakingMessageId == tag) {
      audioEngine.stopSpeaking()
      return
    }
    audioEngine.speakSpanish(
      text = clean,
      messageId = tag,
      character = repository.settings.value.selectedCharacter,
    )
  }

  /** Reveals the tutor's Spanish translation and speaks it aloud using the active tutor's voice. */
  fun translateAndSpeakTurn(turn: ConversationTurn) {
    val translation = turn.translationEs?.trim().orEmpty()
    val tag = "trans-es-${turn.id}"
    localState.update { state ->
      state.copy(revealedTranslations = state.revealedTranslations + turn.id)
    }
    val currentAudio = audioEngine.state.value
    if (currentAudio.isSpeaking && currentAudio.speakingMessageId == tag) {
      audioEngine.stopSpeaking()
      return
    }
    if (translation.isNotEmpty()) {
      audioEngine.speakSpanish(
        text = translation,
        messageId = tag,
        character = repository.settings.value.selectedCharacter,
      )
    }
  }

  fun startCorrectionPractice(correction: Correction) {
    audioEngine.stopSpeaking()
    audioEngine.cancelListening()
    localState.update {
      it.copy(
        practiceCorrectionTarget = correction,
        correctionPracticeAnalysis = null,
        correctionPracticeAttempts = 0,
        isListeningForPractice = false,
      )
    }
  }

  fun closeCorrectionPractice() {
    audioEngine.stopSpeaking()
    audioEngine.cancelListening()
    localState.update {
      it.copy(
        practiceCorrectionTarget = null,
        correctionPracticeAnalysis = null,
        correctionPracticeAttempts = 0,
        isListeningForPractice = false,
      )
    }
  }

  fun toggleCorrectionPracticeMic(hasMicPermission: Boolean) {
    val target = localState.value.practiceCorrectionTarget ?: return
    val audio = audioEngine.state.value
    if (audio.isListening) {
      audioEngine.finishListeningAndSubmit()
      return
    }
    if (!hasMicPermission) return
    val currentSettings = repository.settings.value
    localState.update { it.copy(isListeningForPractice = true) }
    audioEngine.startListening(
      accent = currentSettings.accent,
      silenceMs = currentSettings.selectedLevel.sttSilenceMs,
    ) { spoken ->
      val nextAttempt = localState.value.correctionPracticeAttempts + 1
      val prevScore = localState.value.correctionPracticeAnalysis?.overallScore
      val analysis = SpeakingCoachEngine.analyzePronunciation(
        expected = target.better,
        spoken = spoken,
        attemptNumber = nextAttempt,
        previousScore = prevScore,
      )
      localState.update {
        it.copy(
          correctionPracticeAttempts = nextAttempt,
          correctionPracticeAnalysis = analysis,
          isListeningForPractice = false,
        )
      }
      if (analysis.passed) {
        repository.awardXp(15, spokenTurnIncrement = 1)
      }
    }
  }

  fun onPronunciationChipClick(turnId: String, tip: PronunciationTip) {
    val key = "$turnId-${tip.word}"
    localState.update {
      if (it.expandedTipKey == key) {
        it.copy(expandedTipKey = null, expandedTipText = null)
      } else {
        it.copy(expandedTipKey = key, expandedTipText = tip)
      }
    }
    speakSimpleText(tip.word, tag = "word-$key", slow = true)
  }

  fun toggleTranslation(turnId: String) {
    val turn = localState.value.turns.firstOrNull { it.id == turnId }
    val wasRevealed = turnId in localState.value.revealedTranslations
    localState.update { state ->
      val next = if (wasRevealed) {
        state.revealedTranslations - turnId
      } else {
        state.revealedTranslations + turnId
      }
      state.copy(revealedTranslations = next)
    }
    val tag = "trans-es-$turnId"
    if (wasRevealed) {
      if (audioEngine.state.value.isSpeaking && audioEngine.state.value.speakingMessageId == tag) {
        audioEngine.stopSpeaking()
      }
    } else {
      val translation = turn?.translationEs?.trim().orEmpty()
      if (translation.isNotEmpty()) {
        audioEngine.speakSpanish(
          text = translation,
          messageId = tag,
          character = repository.settings.value.selectedCharacter,
        )
      }
    }
  }

  fun saveTurnPhrase(turn: ConversationTurn) {
    repository.savePhrase(
      english = turn.content,
      spanish = turn.translationEs ?: "",
      note = "Escenario: ${scenario.titleEs}",
      source = repository.settings.value.selectedCharacter.displayName,
    )
    localState.update { state ->
      state.copy(
        turns = state.turns.map {
          if (it.id == turn.id) it.copy(isSaved = true) else it
        },
      )
    }
  }

  fun saveCorrectionPhrase(correction: Correction) {
    repository.savePhrase(
      english = correction.better,
      spanish = correction.why,
      note = correction.tip ?: "Corrección de práctica",
      source = "Corrección",
    )
  }

  fun cycleSpeechRate() {
    val rates = listOf(0.70f, 0.85f, 1.00f, 1.15f)
    val current = repository.settings.value.effectiveSpeechRate()
    val next = rates.firstOrNull { it > current + 0.03f } ?: rates.first()
    repository.setSpeechRateOverride(next)
  }

  fun toggleAccent() {
    val next = if (repository.settings.value.accent == EnglishAccent.US) {
      EnglishAccent.UK
    } else {
      EnglishAccent.US
    }
    repository.selectAccent(next)
  }

  fun toggleHandsFree() {
    repository.toggleAutoListenHandsFree()
  }

  fun toggleFaceToFaceMode() {
    localState.update { it.copy(isFaceToFaceMode = !it.isFaceToFaceMode) }
  }

  fun dismissError() {
    localState.update { it.copy(errorBanner = null) }
    audioEngine.clearError()
  }

  fun finishSession() {
    audioEngine.stopSpeaking()
    audioEngine.stopListening()
    val turnsSnapshot = localState.value.turns
    localState.update { it.copy(isSummarizing = true, showSummarySheet = true) }
    viewModelScope.launch {
      val summary = repository.generateSessionSummary(scenario.id, turnsSnapshot)
      localState.update {
        it.copy(
          isSummarizing = false,
          sessionSummary = summary,
        )
      }
    }
  }

  fun dismissSummarySheet() {
    localState.update { it.copy(showSummarySheet = false) }
  }

  fun stopAudioOnExit() {
    audioEngine.stopSpeaking()
    audioEngine.stopListening()
  }

  class Factory(
    private val scenarioId: String,
    private val repository: TutorRepository,
    private val audioEngine: AudioTutorEngine,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return ConversationViewModel(scenarioId, repository, audioEngine) as T
    }
  }
}
