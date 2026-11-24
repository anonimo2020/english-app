package com.example.englishapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.englishapp.data.CurriculumEngine
import com.example.englishapp.edu.EduSecuritySanitizer
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.ScenarioCatalog
import com.example.englishapp.ui.components.FluentGlassBottomBar
import com.example.englishapp.ui.conversation.ConversationScreen
import com.example.englishapp.ui.conversation.ConversationViewModel
import com.example.englishapp.ui.explore.ExploreScreen
import com.example.englishapp.ui.explore.LessonPlayerScreen
import com.example.englishapp.ui.explore.VocabGameScreen
import com.example.englishapp.ui.home.HomeScreen
import com.example.englishapp.ui.hub.EduResourceDetailScreen
import com.example.englishapp.ui.hub.LearningHubScreen
import com.example.englishapp.ui.onboarding.OnboardingScreen
import com.example.englishapp.ui.progress.ProgressScreen
import com.example.englishapp.ui.settings.SettingsScreen

@Composable
fun MainNavigation(mainViewModel: MainViewModel) {
  val settings by mainViewModel.settings.collectAsState()
  val progress by mainViewModel.progress.collectAsState()
  val savedPhrases by mainViewModel.savedPhrases.collectAsState()
  val audioState by mainViewModel.audioState.collectAsState()
  val connectionTestState by mainViewModel.connectionTestState.collectAsState()
  val dictionaryState by mainViewModel.dictionaryState.collectAsState()
  val learningHubState by mainViewModel.learningHubState.collectAsState()
  val context = LocalContext.current

  val initialRoute: NavKey = if (settings.hasCompletedOnboarding) HomeRoute else OnboardingRoute()
  val backStack = rememberNavBackStack(initialRoute)
  val currentRoute = backStack.lastOrNull()
  val showBottomBar = currentRoute !is ConversationRoute &&
    currentRoute !is OnboardingRoute &&
    currentRoute !is LessonPlayerRoute &&
    currentRoute !is VocabPracticeRoute &&
    currentRoute !is EduResourceDetailRoute

  fun switchTopLevelTab(target: NavKey) {
    mainViewModel.stopAudio()
    backStack.clear()
    if (target == HomeRoute) {
      backStack.add(HomeRoute)
    } else {
      backStack.add(HomeRoute)
      backStack.add(target)
    }
  }

  fun launchTargetedEvaluation(targetLevel: LearnerLevel) {
    mainViewModel.stopAudio()
    backStack.add(
      OnboardingRoute(
        targetLevelId = targetLevel.id,
        skipLevelSelection = true,
      ),
    )
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      if (showBottomBar) {
        val selectedBottomIndex = when (currentRoute) {
          is ExploreRoute, is LearningHubRoute -> 1
          is ProgressRoute -> 3
          is SettingsRoute -> 4
          else -> 0
        }
        FluentGlassBottomBar(
          selectedIndex = selectedBottomIndex,
          onSelectTab = { index ->
            when (index) {
              0 -> switchTopLevelTab(HomeRoute)
              1 -> switchTopLevelTab(ExploreRoute)
              2 -> {
                mainViewModel.stopAudio()
                val recommended = CurriculumEngine.buildCurriculum(settings, progress)
                  .recommendedForLevel
                val nextScenarioId = recommended.firstOrNull { !it.isCompleted }?.scenario?.id
                  ?: recommended.firstOrNull()?.scenario?.id
                  ?: "free_chat"
                backStack.add(
                  ConversationRoute(
                    scenarioId = nextScenarioId,
                    sessionId = System.currentTimeMillis(),
                  ),
                )
              }
              3 -> switchTopLevelTab(ProgressRoute)
              4 -> switchTopLevelTab(SettingsRoute)
            }
          },
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp,
          top = if (showBottomBar) innerPadding.calculateTopPadding() else 0.dp,
        ),
    ) {
      NavDisplay(
        backStack = backStack,
        onBack = {
          if (backStack.size > 1) {
            mainViewModel.stopAudio()
            backStack.removeLastOrNull()
          }
        },
        entryProvider = entryProvider {
          entry<OnboardingRoute> { route ->
            val targetOverride = route.targetLevelId?.let { LearnerLevel.fromId(it) }
            OnboardingScreen(
              initialCharacter = settings.selectedCharacter,
              initialLevel = settings.activeLevel(),
              accent = settings.accent,
              audioState = audioState,
              targetLevelOverride = targetOverride,
              skipLevelSelection = route.skipLevelSelection,
              onCancel = if (backStack.size > 1) {
                {
                  mainViewModel.stopAudio()
                  backStack.removeLastOrNull()
                }
              } else {
                null
              },
              onSelectCharacter = mainViewModel::selectCharacter,
              onSpeakPrompt = mainViewModel::playExploreText,
              onStartSpeechCheck = mainViewModel::startSpeechCheck,
              onStopSpeechCheck = mainViewModel::stopSpeechCheck,
              onStopAllAudio = mainViewModel::stopAudio,
              onCompleteOnboarding = { estimated, result ->
                mainViewModel.savePlacementResult(estimated, result)
                backStack.clear()
                backStack.add(HomeRoute)
              },
            )
          }

          entry<HomeRoute> {
            HomeScreen(
              settings = settings,
              progress = progress,
              audioState = audioState,
              onSelectCharacter = mainViewModel::selectCharacter,
              onPreviewCharacter = mainViewModel::previewCharacterGreeting,
              onStartConversation = { scenarioId ->
                mainViewModel.stopAudio()
                backStack.add(
                  ConversationRoute(
                    scenarioId = scenarioId,
                    sessionId = System.currentTimeMillis(),
                  ),
                )
              },
              onOpenExplore = { switchTopLevelTab(ExploreRoute) },
              onOpenVocabGame = {
                mainViewModel.stopAudio()
                backStack.add(VocabPracticeRoute())
              },
              onOpenLearningHub = {
                mainViewModel.stopAudio()
                backStack.add(LearningHubRoute)
              },
              onOpenSettings = { switchTopLevelTab(SettingsRoute) },
            )
          }

          entry<ExploreRoute> {
            ExploreScreen(
              settings = settings,
              progress = progress,
              audioState = audioState,
              dictionaryState = dictionaryState,
              onLookupWord = mainViewModel::lookupDictionaryWord,
              onPlayText = mainViewModel::playExploreText,
              onPlaySpanishText = mainViewModel::playSpanishText,
              onPlayDictionaryAudio = mainViewModel::playDictionaryAudio,
              onSavePhrase = mainViewModel::savePhrase,
              onOpenLesson = { lessonId ->
                mainViewModel.stopAudio()
                backStack.add(LessonPlayerRoute(lessonId))
              },
              onOpenVocabGame = { cat, startWordId, gameOnly ->
                mainViewModel.stopAudio()
                backStack.add(VocabPracticeRoute(cat, startWordId, gameOnly))
              },
              onMasterDrill = mainViewModel::markDrillMastered,
              onCompleteStoryQuiz = { mainViewModel.awardStoryCompleted() },
              onStartSpeechDrill = mainViewModel::startSpeechCheck,
              onStopSpeechDrill = mainViewModel::stopSpeechCheck,
              onOpenLearningHub = {
                mainViewModel.stopAudio()
                backStack.add(LearningHubRoute)
              },
            )
          }

          entry<LearningHubRoute> {
            LearningHubScreen(
              hubState = learningHubState,
              settings = settings,
              onBack = {
                if (backStack.size > 1) {
                  mainViewModel.stopAudio()
                  backStack.removeLastOrNull()
                }
              },
              onInitializeHub = mainViewModel::initLearningHub,
              onSearchChange = mainViewModel::updateHubSearch,
              onSelectLevel = mainViewModel::selectHubLevel,
              onSelectCategory = mainViewModel::selectHubCategory,
              onSelectSkill = mainViewModel::selectHubSkill,
              onSelectSourceType = mainViewModel::selectHubSourceType,
              onToggleOfflineOnly = mainViewModel::toggleHubOfflineOnly,
              onResetFilters = mainViewModel::resetHubFilters,
              onToggleFavorite = mainViewModel::toggleHubFavorite,
              onOpenResource = { resource ->
                mainViewModel.stopAudio()
                when {
                  resource.contentType == "lesson" -> {
                    mainViewModel.recordHubOpened(resource, false)
                    backStack.add(LessonPlayerRoute(resource.entityId.ifBlank { resource.id }))
                  }
                  resource.contentType == "scenario" &&
                    ScenarioCatalog.all.any { it.id == resource.entityId.ifBlank { resource.id } } -> {
                    mainViewModel.recordHubOpened(resource, false)
                    backStack.add(
                      ConversationRoute(
                        scenarioId = resource.entityId.ifBlank { resource.id },
                        sessionId = System.currentTimeMillis(),
                      ),
                    )
                  }
                  else -> backStack.add(EduResourceDetailRoute(resource.id))
                }
              },
              onSyncNow = mainViewModel::syncHubWithBackend,
              onSearchLiveOpenProviders = mainViewModel::searchLiveOpenWebProviders,
              onOpenExternalUrl = { live ->
                if (EduSecuritySanitizer.isSafeExternalUrl(live.originalUrl)) {
                  runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(live.originalUrl))) }
                }
              },
              onPlayText = mainViewModel::playExploreText,
              onSavePhrase = mainViewModel::savePhrase,
            )
          }

          entry<EduResourceDetailRoute> { route ->
            val resource = mainViewModel.eduRepository.resourceById(route.resourceId)
            val lic = resource?.let { learningHubState.licensesById[it.licenseId] }
            val attr = resource?.attributionId?.let { learningHubState.attributionsById[it] }
            EduResourceDetailScreen(
              resource = resource,
              license = lic,
              attribution = attr,
              isFavorite = resource?.id in learningHubState.favoriteIds,
              settings = settings,
              audioState = audioState,
              onBack = {
                mainViewModel.stopAudio()
                backStack.removeLastOrNull()
              },
              onRecordOpened = mainViewModel::recordHubOpened,
              onToggleFavorite = mainViewModel::toggleHubFavorite,
              onCompleteOutcome = mainViewModel::completeHubResource,
              onPlayText = mainViewModel::playExploreText,
              onPlaySpanishText = mainViewModel::playSpanishText,
              onSavePhrase = mainViewModel::savePhrase,
              onStartSpeechCheck = mainViewModel::startSpeechCheck,
              onStopSpeechCheck = mainViewModel::stopSpeechCheck,
            )
          }

          entry<LessonPlayerRoute> { route ->
            LessonPlayerScreen(
              lessonId = route.lessonId,
              settings = settings,
              audioState = audioState,
              onPlayText = mainViewModel::playExploreText,
              onPlaySpanishText = mainViewModel::playSpanishText,
              onSavePhrase = mainViewModel::savePhrase,
              onCompleteLesson = mainViewModel::markLessonCompleted,
              onStartSpeechDrill = mainViewModel::startSpeechCheck,
              onStopSpeechDrill = mainViewModel::stopSpeechCheck,
              onStopAllAudio = mainViewModel::stopAudio,
              onOpenNextLesson = { nextLessonId ->
                mainViewModel.stopAudio()
                backStack.removeLastOrNull()
                backStack.add(LessonPlayerRoute(nextLessonId))
              },
              onExit = {
                mainViewModel.stopAudio()
                backStack.removeLastOrNull()
              },
            )
          }

          entry<VocabPracticeRoute> { route ->
            VocabGameScreen(
              settings = settings,
              progress = progress,
              audioState = audioState,
              categoryFilter = route.categoryFilter,
              startWordId = route.startWordId,
              initialGameOnlyMode = route.gameOnlyMode,
              onPlayText = mainViewModel::playExploreText,
              onPlaySpanishText = mainViewModel::playSpanishText,
              onSavePhrase = mainViewModel::savePhrase,
              onMarkVocabMastered = mainViewModel::markVocabMastered,
              onStartSpeechCheck = mainViewModel::startSpeechCheck,
              onStopSpeechCheck = mainViewModel::stopSpeechCheck,
              onStopAllAudio = mainViewModel::stopAudio,
              onExit = {
                mainViewModel.stopAudio()
                backStack.removeLastOrNull()
              },
            )
          }

          entry<ProgressRoute> {
            ProgressScreen(
              progress = progress,
              savedPhrases = savedPhrases,
              audioState = audioState,
              settings = settings,
              onPlayPhrase = mainViewModel::playSavedPhrase,
              onRemovePhrase = mainViewModel::removeSavedPhrase,
            )
          }

          entry<SettingsRoute> {
            SettingsScreen(
              settings = settings,
              progress = progress,
              connectionTestState = connectionTestState,
              onToggleOfflineDemo = mainViewModel::updateOfflineDemoMode,
              onSaveCredentials = mainViewModel::updateBackendCredentials,
              onTestConnection = mainViewModel::testConnection,
              onSelectAccent = mainViewModel::selectAccent,
              onToggleAutoPlay = mainViewModel::updateAutoPlayReplies,
              onToggleAutoListen = mainViewModel::updateAutoListenHandsFree,
              onToggleShowTranslations = mainViewModel::updateShowTranslationsByDefault,
              onToggleFaceToFaceDefault = mainViewModel::updatePreferFaceToFaceMode,
              onEvaluateForLevel = { targetLvl -> launchTargetedEvaluation(targetLvl) },
              onSelectTheme = mainViewModel::setThemePreference,
            )
          }

          entry<ConversationRoute> { route ->
            val convViewModel: ConversationViewModel = viewModel(
              key = "conv-${route.scenarioId}-${route.sessionId}",
              factory = ConversationViewModel.Factory(
                scenarioId = route.scenarioId,
                repository = mainViewModel.repository,
                audioEngine = mainViewModel.audioEngine,
              ),
            )
            val uiState by convViewModel.uiState.collectAsState()
            ConversationScreen(
              uiState = uiState,
              onBack = {
                if (backStack.size > 1) {
                  backStack.removeLastOrNull()
                }
              },
              onToggleMic = convViewModel::toggleMicListening,
              onSendText = convViewModel::sendUserMessage,
              onSpeakTurn = { turn, slow -> convViewModel.speakTurn(turn, slow) },
              onSpeakSimpleText = convViewModel::speakSimpleText,
              onSpeakSpanish = convViewModel::speakSpanishText,
              onPronunciationChipClick = convViewModel::onPronunciationChipClick,
              onToggleTranslation = convViewModel::toggleTranslation,
              onTranslateAndSpeak = convViewModel::translateAndSpeakTurn,
              onSaveTurn = convViewModel::saveTurnPhrase,
              onSaveCorrection = convViewModel::saveCorrectionPhrase,
              onStartCorrectionPractice = convViewModel::startCorrectionPractice,
              onCloseCorrectionPractice = convViewModel::closeCorrectionPractice,
              onToggleCorrectionPracticeMic = convViewModel::toggleCorrectionPracticeMic,
              onCycleSpeechRate = convViewModel::cycleSpeechRate,
              onToggleAccent = convViewModel::toggleAccent,
              onToggleHandsFree = convViewModel::toggleHandsFree,
              onToggleFaceToFaceMode = convViewModel::toggleFaceToFaceMode,
              onDismissError = convViewModel::dismissError,
              onFinishSession = convViewModel::finishSession,
              onDismissSummary = convViewModel::dismissSummarySheet,
              onExitAfterSummary = {
                backStack.clear()
                backStack.add(HomeRoute)
              },
              onScreenVisible = convViewModel::onScreenVisible,
              onDisposeAudio = convViewModel::stopAudioOnExit,
            )
          }
        },
      )
    }
  }
}
