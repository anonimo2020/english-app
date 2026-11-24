package com.example.englishapp.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.data.PlacementAnswerRecord
import com.example.englishapp.data.PlacementEngine
import com.example.englishapp.data.PlacementQuestion
import com.example.englishapp.data.PlacementQuestionType
import com.example.englishapp.data.SpeakingAnalysis
import com.example.englishapp.data.SpeakingCoachEngine
import com.example.englishapp.model.AvatarMood
import com.example.englishapp.model.EnglishAccent
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PlacementResult
import com.example.englishapp.model.ScenarioCatalog
import com.example.englishapp.model.SkillScores
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.TutorCharacter
import com.example.englishapp.ui.components.AudioWaveform
import com.example.englishapp.ui.components.SpeakingMasteryCard
import com.example.englishapp.ui.components.TutorAvatar
import com.example.englishapp.ui.theme.FluentTheme
import com.example.englishapp.ui.theme.FluentVoiceTheme

enum class OnboardingStep {
  CHOOSE_LEVEL,
  PLACEMENT_TEST,
  RESULT_REVEAL,
}

@Composable
fun OnboardingScreen(
  initialCharacter: TutorCharacter,
  initialLevel: LearnerLevel,
  accent: EnglishAccent,
  audioState: AudioEngineState,
  onSelectCharacter: (TutorCharacter) -> Unit,
  onSpeakPrompt: (text: String, tag: String, slow: Boolean) -> Unit,
  onStartSpeechCheck: (silenceMs: Long, onResult: (String) -> Unit) -> Unit,
  onStopSpeechCheck: () -> Unit,
  onStopAllAudio: () -> Unit,
  onCompleteOnboarding: (estimated: LearnerLevel, result: PlacementResult) -> Unit,
  modifier: Modifier = Modifier,
  targetLevelOverride: LearnerLevel? = null,
  skipLevelSelection: Boolean = false,
  onCancel: (() -> Unit)? = null,
) {
  val startingLevel = targetLevelOverride ?: initialLevel
  var step by remember(skipLevelSelection, startingLevel) {
    mutableStateOf(
      if (skipLevelSelection && targetLevelOverride != null) {
        OnboardingStep.PLACEMENT_TEST
      } else {
        OnboardingStep.CHOOSE_LEVEL
      },
    )
  }
  var selectedCharacter by remember { mutableStateOf(initialCharacter) }
  var estimatedLevel by remember(startingLevel) { mutableStateOf(startingLevel) }

  // Placement state
  var stepIndex by remember(startingLevel, skipLevelSelection) { mutableIntStateOf(0) }
  var ability by remember(startingLevel, skipLevelSelection) {
    mutableDoubleStateOf(PlacementEngine.initialAbilityFor(startingLevel))
  }
  var answeredIds by remember(startingLevel, skipLevelSelection) { mutableStateOf(emptySet<String>()) }
  var records by remember(startingLevel, skipLevelSelection) { mutableStateOf(emptyList<PlacementAnswerRecord>()) }
  var allowSpeaking by remember { mutableStateOf(true) }
  var currentQuestion by remember(startingLevel, skipLevelSelection) {
    mutableStateOf(
      PlacementEngine.nextQuestion(
        ability = PlacementEngine.initialAbilityFor(startingLevel),
        answeredIds = emptySet(),
        stepIndex = 0,
        allowSpeaking = true,
        targetLevel = startingLevel,
      ),
    )
  }
  var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
  var speakingAnalysis by remember { mutableStateOf<SpeakingAnalysis?>(null) }
  var bestSpeakingAnalysis by remember { mutableStateOf<SpeakingAnalysis?>(null) }
  var speakingAttempts by remember { mutableIntStateOf(0) }
  var questionSubmitted by remember { mutableStateOf(false) }
  var placementResult by remember { mutableStateOf<PlacementResult?>(null) }

  val passThresholdPct = (PlacementEngine.SPEAKING_PASS_THRESHOLD * 100).toInt()

  fun handleSpokenAttempt(transcript: String) {
    val nextAttempt = speakingAttempts + 1
    speakingAttempts = nextAttempt
    val prevScore = speakingAnalysis?.overallScore
    val analysis = SpeakingCoachEngine.analyzePronunciation(
      expected = currentQuestion.promptEn,
      spoken = transcript,
      attemptNumber = nextAttempt,
      previousScore = prevScore,
      passThreshold = passThresholdPct,
    )
    speakingAnalysis = analysis
    val currentBest = bestSpeakingAnalysis
    if (currentBest == null || analysis.overallScore >= currentBest.overallScore) {
      bestSpeakingAnalysis = analysis
    }
    if (analysis.passed || nextAttempt >= 2) {
      val chosen = bestSpeakingAnalysis ?: analysis
      val frac = (chosen.overallScore / 100f).coerceIn(0f, 1f)
      val passed = chosen.passed
      questionSubmitted = true
      records = records + PlacementAnswerRecord(currentQuestion, passed, frac)
      ability = PlacementEngine.updateAbility(ability, currentQuestion.difficulty, passed, estimatedLevel)
    }
  }

  val context = LocalContext.current
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      onStartSpeechCheck(estimatedLevel.sttSilenceMs) { transcript ->
        handleSpokenAttempt(transcript)
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose { onStopAllAudio() }
  }

  LaunchedEffect(step, currentQuestion.id) {
    if (step == OnboardingStep.PLACEMENT_TEST && currentQuestion.type == PlacementQuestionType.LISTENING) {
      onSpeakPrompt(currentQuestion.promptEn, "place-${currentQuestion.id}", false)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    when (step) {
      OnboardingStep.CHOOSE_LEVEL -> {
        ChooseLevelStage(
          selectedCharacter = selectedCharacter,
          estimatedLevel = estimatedLevel,
          audioState = audioState,
          onSelectCharacter = {
            selectedCharacter = it
            onSelectCharacter(it)
            onSpeakPrompt(it.sampleGreetingEn, "onb-char-${it.id}", false)
          },
          onSelectEstimatedLevel = { estimatedLevel = it },
          onStartEvaluation = {
            onStopAllAudio()
            val startAbility = PlacementEngine.initialAbilityFor(estimatedLevel)
            ability = startAbility
            stepIndex = 0
            answeredIds = emptySet()
            records = emptyList()
            allowSpeaking = true
            selectedOptionIndex = null
            speakingAnalysis = null
            bestSpeakingAnalysis = null
            speakingAttempts = 0
            questionSubmitted = false
            currentQuestion = PlacementEngine.nextQuestion(
              ability = startAbility,
              answeredIds = emptySet(),
              stepIndex = 0,
              allowSpeaking = true,
              targetLevel = estimatedLevel,
            )
            step = OnboardingStep.PLACEMENT_TEST
          },
        )
      }

      OnboardingStep.PLACEMENT_TEST -> {
        PlacementTestStage(
          character = selectedCharacter,
          accent = accent,
          targetGoalLabel = if (skipLevelSelection && targetLevelOverride != null) {
            "Objetivo: ${targetLevelOverride.labelEs} (${targetLevelOverride.cefr})"
          } else {
            null
          },
          stepIndex = stepIndex,
          totalSteps = PlacementEngine.DEFAULT_TOTAL_STEPS,
          currentEstimatedCefr = minOf(PlacementEngine.levelForAbility(ability), estimatedLevel).cefr,
          question = currentQuestion,
          selectedOptionIndex = selectedOptionIndex,
          speakingAnalysis = speakingAnalysis,
          questionSubmitted = questionSubmitted,
          audioState = audioState,
          passThresholdPct = passThresholdPct,
          onCancel = onCancel,
          onPlayQuestionAudio = { slow ->
            onSpeakPrompt(currentQuestion.promptEn, "place-${currentQuestion.id}", slow)
          },
          onPlayWord = { word ->
            onSpeakPrompt(word, "place-word-$word", true)
          },
          onSelectOption = { idx ->
            if (questionSubmitted) return@PlacementTestStage
            selectedOptionIndex = idx
            questionSubmitted = true
            val isCorrect = idx == currentQuestion.correctIndex
            records = records + PlacementAnswerRecord(currentQuestion, isCorrect)
            ability = PlacementEngine.updateAbility(ability, currentQuestion.difficulty, isCorrect, estimatedLevel)
          },
          onToggleSpeakMic = {
            if (questionSubmitted) return@PlacementTestStage
            if (audioState.isListening) {
              onStopSpeechCheck()
            } else {
              val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
              ) == PackageManager.PERMISSION_GRANTED
              if (granted) {
                onStartSpeechCheck(2400L) { transcript ->
                  handleSpokenAttempt(transcript)
                }
              } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            }
          },
          onAcceptSpeakingScoreAnyway = {
            val chosen = bestSpeakingAnalysis ?: speakingAnalysis
            if (chosen != null && !questionSubmitted) {
              val frac = (chosen.overallScore / 100f).coerceIn(0f, 1f)
              questionSubmitted = true
              records = records + PlacementAnswerRecord(currentQuestion, chosen.passed, frac)
              ability = PlacementEngine.updateAbility(ability, currentQuestion.difficulty, chosen.passed, estimatedLevel)
            }
          },
          onSwapSpeakingForWritten = {
            onStopAllAudio()
            allowSpeaking = false
            selectedOptionIndex = null
            speakingAnalysis = null
            bestSpeakingAnalysis = null
            speakingAttempts = 0
            questionSubmitted = false
            currentQuestion = PlacementEngine.nextQuestion(
              ability = ability,
              answeredIds = answeredIds + currentQuestion.id,
              stepIndex = stepIndex,
              allowSpeaking = false,
              targetLevel = estimatedLevel,
            )
          },
          onNextQuestion = {
            onStopAllAudio()
            val updatedAnswered = answeredIds + currentQuestion.id
            answeredIds = updatedAnswered
            val nextIdx = stepIndex + 1
            if (nextIdx >= PlacementEngine.DEFAULT_TOTAL_STEPS) {
              placementResult = PlacementEngine.buildResult(
                estimatedLevel = estimatedLevel,
                finalAbility = ability,
                records = records,
              )
              step = OnboardingStep.RESULT_REVEAL
            } else {
              stepIndex = nextIdx
              selectedOptionIndex = null
              speakingAnalysis = null
              bestSpeakingAnalysis = null
              speakingAttempts = 0
              questionSubmitted = false
              currentQuestion = PlacementEngine.nextQuestion(
                ability = ability,
                answeredIds = updatedAnswered,
                stepIndex = nextIdx,
                allowSpeaking = allowSpeaking,
                targetLevel = estimatedLevel,
              )
            }
          },
        )
      }

      OnboardingStep.RESULT_REVEAL -> {
        val result = placementResult ?: PlacementEngine.buildResult(estimatedLevel, ability, records)
        PlacementResultStage(
          character = selectedCharacter,
          result = result,
          onFinish = {
            onStopAllAudio()
            onCompleteOnboarding(estimatedLevel, result)
          },
        )
      }
    }
  }
}

@Composable
private fun ChooseLevelStage(
  selectedCharacter: TutorCharacter,
  estimatedLevel: LearnerLevel,
  audioState: AudioEngineState,
  onSelectCharacter: (TutorCharacter) -> Unit,
  onSelectEstimatedLevel: (LearnerLevel) -> Unit,
  onStartEvaluation: () -> Unit,
) {
  val accent = FluentTheme.colors.accentFor(selectedCharacter)
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
      ) {
        Text(
          text = "BIENVENIDO A FLUENTVOICE",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }

      Text(
        text = "Personalicemos tu inglés",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "Elige tu tutor, cuéntanos tu nivel aproximado y haremos 6 retos rápidos con explicaciones en español.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      TutorCharacter.entries.forEach { character ->
        val selected = character == selectedCharacter
        val charAccent = FluentTheme.colors.accentFor(character)
        val speakingThis = audioState.isSpeaking && audioState.speakingMessageId == "onb-char-${character.id}"
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelectCharacter(character) },
          shape = RoundedCornerShape(20.dp),
          color = if (selected) charAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
          border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) charAccent else FluentTheme.colors.cardBorder),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            TutorAvatar(
              character = character,
              mood = if (speakingThis) AvatarMood.SPEAKING else AvatarMood.IDLE,
              size = 52.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = character.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = if (speakingThis) "Hablando..." else "Tocar para oír",
                style = MaterialTheme.typography.labelSmall,
                color = charAccent,
              )
            }
          }
        }
      }
    }

    Text(
      text = "¿Qué nivel crees tener hoy?",
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      LearnerLevel.entries.forEach { level ->
        val selected = level == estimatedLevel
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelectEstimatedLevel(level) },
          shape = RoundedCornerShape(18.dp),
          color = if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
          border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) accent else FluentTheme.colors.cardBorder,
          ),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = level.cefr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = level.labelEs,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = level.descriptionEs,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (selected) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Seleccionado",
                tint = accent,
              )
            }
          }
        }
      }
    }

    Button(
      onClick = onStartEvaluation,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
    ) {
      Text(
        text = "Evaluar mi nivel real (6 retos)",
        style = MaterialTheme.typography.labelLarge,
      )
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun PlacementTestStage(
  character: TutorCharacter,
  accent: EnglishAccent,
  targetGoalLabel: String?,
  stepIndex: Int,
  totalSteps: Int,
  currentEstimatedCefr: String,
  question: PlacementQuestion,
  selectedOptionIndex: Int?,
  speakingAnalysis: SpeakingAnalysis?,
  questionSubmitted: Boolean,
  audioState: AudioEngineState,
  passThresholdPct: Int,
  onCancel: (() -> Unit)?,
  onPlayQuestionAudio: (slow: Boolean) -> Unit,
  onPlayWord: (String) -> Unit,
  onSelectOption: (Int) -> Unit,
  onToggleSpeakMic: () -> Unit,
  onAcceptSpeakingScoreAnyway: () -> Unit,
  onSwapSpeakingForWritten: () -> Unit,
  onNextQuestion: () -> Unit,
) {
  val tutorAccent = FluentTheme.colors.accentFor(character)
  val progressFraction by animateFloatAsState(
    targetValue = (stepIndex + 1).toFloat() / totalSteps.toFloat(),
    animationSpec = tween(350),
    label = "placementProgress",
  )
  val isSpeakingThis = audioState.isSpeaking && audioState.speakingMessageId == "place-${question.id}"
  val mood = when {
    audioState.isListening -> AvatarMood.LISTENING
    isSpeakingThis -> AvatarMood.SPEAKING
    else -> AvatarMood.IDLE
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Reto ${stepIndex + 1} de $totalSteps",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          if (targetGoalLabel != null) {
            Text(
              text = targetGoalLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = tutorAccent.copy(alpha = 0.16f),
          ) {
            Text(
              text = "Calibrando: $currentEstimatedCefr · ${accent.badge}",
              style = MaterialTheme.typography.labelMedium,
              color = tutorAccent,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
          if (onCancel != null) {
            TextButton(onClick = onCancel) {
              Text("Cancelar")
            }
          }
        }
      }
      LinearProgressIndicator(
        progress = { progressFraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(999.dp)),
        color = tutorAccent,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )
    }

    if (question.type == PlacementQuestionType.SPEAKING) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = question.promptEs,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        SpeakingMasteryCard(
          targetPhrase = question.promptEn,
          tipEs = question.explanationEs,
          analysis = speakingAnalysis,
          isListeningThis = audioState.isListening,
          audioState = audioState,
          passThreshold = passThresholdPct,
          onListenNormal = { onPlayQuestionAudio(false) },
          onListenSlow = { onPlayQuestionAudio(true) },
          onListenWord = onPlayWord,
          onToggleRecord = onToggleSpeakMic,
        )
        if (!questionSubmitted) {
          if (speakingAnalysis != null && !speakingAnalysis.passed) {
            TextButton(
              onClick = onAcceptSpeakingScoreAnyway,
              modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
              Text("Continuar con ${speakingAnalysis.overallScore}% sin repetir")
            }
          }
          TextButton(
            onClick = onSwapSpeakingForWritten,
            modifier = Modifier.align(Alignment.CenterHorizontally),
          ) {
            Text("No puedo usar micrófono ahora (cambiar a reto escrito)")
          }
        }
      }
    } else {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
              Text(
                text = question.type.labelEs,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
            TutorAvatar(
              character = character,
              mood = mood,
              size = 64.dp,
              rmsLevel = audioState.rmsNormalized,
            )
          }

          Text(
            text = question.promptEs,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
          )

          if (question.type != PlacementQuestionType.LISTENING) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Text(
                text = question.promptEn,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp),
              )
            }
          } else {
            AudioWaveform(
              mood = mood,
              rmsLevel = audioState.rmsNormalized,
              activeColor = tutorAccent,
              idleColor = MaterialTheme.colorScheme.onSurfaceVariant,
              height = 24.dp,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = { onPlayQuestionAudio(false) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
              ) {
                Icon(
                  imageVector = if (isSpeakingThis) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSpeakingThis) "Detener" else "Escuchar")
              }
              OutlinedButton(
                onClick = { onPlayQuestionAudio(true) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.SlowMotionVideo,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lento 0.7x")
              }
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.options.forEachIndexed { idx, option ->
          val isSelected = selectedOptionIndex == idx
          val isCorrect = idx == question.correctIndex
          val borderColor = when {
            !questionSubmitted -> FluentTheme.colors.cardBorder
            isCorrect -> FluentTheme.colors.success
            isSelected && !isCorrect -> FluentTheme.colors.recording
            else -> FluentTheme.colors.cardBorder
          }
          val bgColor = when {
            !questionSubmitted -> MaterialTheme.colorScheme.surface
            isCorrect -> FluentTheme.colors.success.copy(alpha = 0.14f)
            isSelected && !isCorrect -> FluentTheme.colors.recording.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.surface
          }

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable(enabled = !questionSubmitted) { onSelectOption(idx) },
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = BorderStroke(if (questionSubmitted && (isCorrect || isSelected)) 1.5.dp else 1.dp, borderColor),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
              )
              if (questionSubmitted && isCorrect) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = "Correcta",
                  tint = FluentTheme.colors.success,
                  modifier = Modifier.size(20.dp),
                )
              } else if (questionSubmitted && isSelected && !isCorrect) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = "Incorrecta",
                  tint = FluentTheme.colors.recording,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }

    AnimatedVisibility(visible = questionSubmitted) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Rounded.Lightbulb,
              contentDescription = null,
              tint = FluentTheme.colors.correction,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = question.explanationEs,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Button(
          onClick = onNextQuestion,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
          shape = RoundedCornerShape(16.dp),
        ) {
          Text(
            text = if (stepIndex + 1 >= totalSteps) "Ver mi diagnóstico" else "Siguiente reto",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun PlacementResultStage(
  character: TutorCharacter,
  result: PlacementResult,
  onFinish: () -> Unit,
) {
  val accent = FluentTheme.colors.accentFor(character)
  val levelTopics = remember(result.verifiedLevel) {
    ScenarioCatalog.all.filter { it.recommendedLevel == result.verifiedLevel }.take(4)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      TutorAvatar(
        character = character,
        mood = AvatarMood.IDLE,
        size = 92.dp,
      )
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "NIVEL VERIFICADO: ${result.verifiedLevel.labelEs.uppercase()} · ${result.verifiedLevel.cefr}",
            style = MaterialTheme.typography.labelLarge,
            color = accent,
          )
        }
      }
      Text(
        text = "Puntaje global: ${result.overallScore}/100",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = "Objetivo: ${result.estimatedLevel.labelEs} (${result.estimatedLevel.cefr}) → Evaluado como ${result.verifiedLevel.labelEs} (${result.verifiedLevel.cefr})",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Mapa de habilidades",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        SkillBarRow("Comprensión auditiva", result.skills.listening, accent)
        SkillBarRow("Gramática en uso", result.skills.grammar, MaterialTheme.colorScheme.primary)
        SkillBarRow("Vocabulario y contexto", result.skills.vocabulary, FluentTheme.colors.eddyAccent)
        SkillBarRow("Habla y pronunciación", result.skills.speaking, FluentTheme.colors.success)

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = result.recommendedFocusEs,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "Temas recomendados para tu nivel",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      levelTopics.forEach { topic ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = topic.titleEs,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = topic.subtitleEs,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text = topic.recommendedLevel.cefr,
              style = MaterialTheme.typography.labelMedium,
              color = accent,
            )
          }
        }
      }
    }

    Button(
      onClick = onFinish,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
      shape = RoundedCornerShape(18.dp),
    ) {
      Text(
        text = "Ir a mi plan personalizado (+25 XP)",
        style = MaterialTheme.typography.labelLarge,
      )
    }
  }
}

@Composable
private fun SkillBarRow(
  label: String,
  score: Int,
  color: Color,
) {
  val frac by animateFloatAsState(
    targetValue = (score / 100f).coerceIn(0f, 1f),
    animationSpec = tween(600),
    label = "skillBar",
  )
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "$score%",
        style = MaterialTheme.typography.labelMedium,
        color = color,
      )
    }
    LinearProgressIndicator(
      progress = { frac },
      modifier = Modifier
        .fillMaxWidth()
        .height(7.dp)
        .clip(RoundedCornerShape(999.dp)),
      color = color,
      trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingChoosePreview() {
  FluentVoiceTheme(themePreference = ThemePreference.DARK) {
    ChooseLevelStage(
      selectedCharacter = TutorCharacter.MANU,
      estimatedLevel = LearnerLevel.JUNIOR,
      audioState = AudioEngineState(),
      onSelectCharacter = {},
      onSelectEstimatedLevel = {},
      onStartEvaluation = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingResultPreview() {
  FluentVoiceTheme(themePreference = ThemePreference.DARK) {
    PlacementResultStage(
      character = TutorCharacter.EDDY,
      result = PlacementResult(
        estimatedLevel = LearnerLevel.JUNIOR,
        verifiedLevel = LearnerLevel.INTERMEDIO,
        overallScore = 82,
        skills = SkillScores(listening = 85, grammar = 78, vocabulary = 84, speaking = 80),
        recommendedFocusEs = "¡Tu nivel real (B1) superó tu estimación inicial! Priorizaremos gramática en uso.",
      ),
      onFinish = {},
    )
  }
}
