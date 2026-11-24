package com.example.englishapp.ui.conversation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.data.VocabularyCatalog
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.AvatarMood
import com.example.englishapp.model.ConversationTurn
import com.example.englishapp.model.Correction
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PronunciationTip
import com.example.englishapp.model.ScenarioCatalog
import com.example.englishapp.model.SessionSummary
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.TutorCharacter
import com.example.englishapp.ui.components.AudioWaveform
import com.example.englishapp.ui.components.AuroraScreenBackground
import com.example.englishapp.ui.components.GlowingCircleIconButton
import com.example.englishapp.ui.components.SpeakingMasteryCard
import com.example.englishapp.ui.components.SpeakingOrbVisualizer
import com.example.englishapp.ui.components.TutorAvatar
import com.example.englishapp.ui.components.TutorLiveBackdrop
import com.example.englishapp.ui.theme.FluentTheme
import com.example.englishapp.ui.theme.FluentVoiceTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
  uiState: ConversationUiState,
  onBack: () -> Unit,
  onToggleMic: (hasMicPermission: Boolean) -> Unit,
  onSendText: (String, Boolean) -> Unit,
  onSpeakTurn: (ConversationTurn, Boolean) -> Unit,
  onSpeakSimpleText: (String, String, Boolean) -> Unit,
  onSpeakSpanish: (String, String) -> Unit,
  onPronunciationChipClick: (String, PronunciationTip) -> Unit,
  onToggleTranslation: (String) -> Unit,
  onTranslateAndSpeak: (ConversationTurn) -> Unit,
  onSaveTurn: (ConversationTurn) -> Unit,
  onSaveCorrection: (Correction) -> Unit,
  onStartCorrectionPractice: (Correction) -> Unit,
  onCloseCorrectionPractice: () -> Unit,
  onToggleCorrectionPracticeMic: (Boolean) -> Unit,
  onCycleSpeechRate: () -> Unit,
  onToggleAccent: () -> Unit,
  onToggleHandsFree: () -> Unit,
  onToggleFaceToFaceMode: () -> Unit,
  onDismissError: () -> Unit,
  onFinishSession: () -> Unit,
  onDismissSummary: () -> Unit,
  onExitAfterSummary: () -> Unit,
  onScreenVisible: (Boolean) -> Unit,
  onDisposeAudio: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED,
    )
  }
  var showPermissionRationale by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    hasMicPermission = granted
    if (granted) {
      showPermissionRationale = false
      if (uiState.practiceCorrectionTarget != null) {
        onToggleCorrectionPracticeMic(true)
      } else {
        onToggleMic(true)
      }
    } else {
      showPermissionRationale = true
    }
  }

  LaunchedEffect(Unit) {
    onScreenVisible(hasMicPermission)
  }

  DisposableEffect(Unit) {
    onDispose { onDisposeAudio() }
  }

  fun requestMicOrRun(action: () -> Unit) {
    val grantedNow = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    hasMicPermission = grantedNow
    if (grantedNow) action() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
  }

  ConversationContent(
    uiState = uiState,
    showPermissionRationale = showPermissionRationale,
    onDismissPermissionRationale = { showPermissionRationale = false },
    onBack = {
      onDisposeAudio()
      onBack()
    },
    onMicClick = { requestMicOrRun { onToggleMic(true) } },
    onSendText = { txt -> onSendText(txt, hasMicPermission) },
    onSpeakTurn = onSpeakTurn,
    onSpeakSimpleText = onSpeakSimpleText,
    onSpeakSpanish = onSpeakSpanish,
    onPronunciationChipClick = onPronunciationChipClick,
    onToggleTranslation = onToggleTranslation,
    onTranslateAndSpeak = onTranslateAndSpeak,
    onSaveTurn = onSaveTurn,
    onSaveCorrection = onSaveCorrection,
    onStartCorrectionPractice = onStartCorrectionPractice,
    onCycleSpeechRate = onCycleSpeechRate,
    onToggleAccent = onToggleAccent,
    onToggleHandsFree = onToggleHandsFree,
    onToggleFaceToFaceMode = onToggleFaceToFaceMode,
    onDismissError = onDismissError,
    onFinishSession = onFinishSession,
    modifier = modifier,
  )

  uiState.practiceCorrectionTarget?.let { correction ->
    ModalBottomSheet(
      onDismissRequest = onCloseCorrectionPractice,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Practicar corrección en voz alta",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        SpeakingMasteryCard(
          targetPhrase = correction.better,
          spanishHint = correction.why,
          tipEs = correction.tip,
          analysis = uiState.correctionPracticeAnalysis,
          isListeningThis = uiState.isListeningForPractice,
          audioState = uiState.audio,
          onListenNormal = { onSpeakSimpleText(correction.better, "cp-norm", false) },
          onListenSlow = { onSpeakSimpleText(correction.better, "cp-slow", true) },
          onListenWord = { w -> onSpeakSimpleText(w, "cp-w-$w", true) },
          onToggleRecord = { requestMicOrRun { onToggleCorrectionPracticeMic(true) } },
        )
      }
    }
  }

  if (uiState.showSummarySheet) {
    ModalBottomSheet(
      onDismissRequest = onDismissSummary,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      SessionSummarySheetContent(
        uiState = uiState,
        onContinuePractice = onDismissSummary,
        onReturnHome = {
          onDismissSummary()
          onExitAfterSummary()
        },
      )
    }
  }
}

@Composable
private fun ConversationContent(
  uiState: ConversationUiState,
  showPermissionRationale: Boolean,
  onDismissPermissionRationale: () -> Unit,
  onBack: () -> Unit,
  onMicClick: () -> Unit,
  onSendText: (String) -> Unit,
  onSpeakTurn: (ConversationTurn, Boolean) -> Unit,
  onSpeakSimpleText: (String, String, Boolean) -> Unit,
  onSpeakSpanish: (String, String) -> Unit,
  onPronunciationChipClick: (String, PronunciationTip) -> Unit,
  onToggleTranslation: (String) -> Unit,
  onTranslateAndSpeak: (ConversationTurn) -> Unit,
  onSaveTurn: (ConversationTurn) -> Unit,
  onSaveCorrection: (Correction) -> Unit,
  onStartCorrectionPractice: (Correction) -> Unit,
  onCycleSpeechRate: () -> Unit,
  onToggleAccent: () -> Unit,
  onToggleHandsFree: () -> Unit,
  onToggleFaceToFaceMode: () -> Unit,
  onDismissError: () -> Unit,
  onFinishSession: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  var showKeyboardInput by rememberSaveable { mutableStateOf(false) }
  var showIdeasDrawer by rememberSaveable { mutableStateOf(false) }
  var textDraft by rememberSaveable { mutableStateOf("") }
  val character = uiState.settings.selectedCharacter
  val tutorAccent = FluentTheme.colors.accentFor(character)

  if (uiState.isFaceToFaceMode) {
    CinemaLiveSpeakingScreen(
      uiState = uiState,
      showPermissionRationale = showPermissionRationale,
      onDismissPermissionRationale = onDismissPermissionRationale,
      onBack = onBack,
      onMicClick = onMicClick,
      onSendText = onSendText,
      onSpeakTurn = onSpeakTurn,
      onSpeakSimpleText = onSpeakSimpleText,
      onSpeakSpanish = onSpeakSpanish,
      onPronunciationChipClick = onPronunciationChipClick,
      onToggleTranslation = onToggleTranslation,
      onTranslateAndSpeak = onTranslateAndSpeak,
      onSaveTurn = onSaveTurn,
      onSaveCorrection = onSaveCorrection,
      onStartCorrectionPractice = onStartCorrectionPractice,
      onCycleSpeechRate = onCycleSpeechRate,
      onToggleAccent = onToggleAccent,
      onToggleHandsFree = onToggleHandsFree,
      onToggleChatView = onToggleFaceToFaceMode,
      onDismissError = onDismissError,
      onFinishSession = onFinishSession,
      modifier = modifier,
    )
    return
  }

  LaunchedEffect(uiState.turns.size, uiState.isFaceToFaceMode) {
    if (!uiState.isFaceToFaceMode && uiState.turns.isNotEmpty()) {
      listState.animateScrollToItem(uiState.turns.lastIndex)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding(),
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        IconButton(onClick = onBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Volver",
            tint = MaterialTheme.colorScheme.onBackground,
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = uiState.scenario.titleEs,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${uiState.settings.activeLevel().cefr} · +${uiState.sessionXp} XP",
            style = MaterialTheme.typography.labelSmall,
            color = tutorAccent,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onToggleFaceToFaceMode),
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Videocam,
              contentDescription = "Modo estudio en vivo",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = "En vivo",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onToggleAccent),
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = uiState.settings.accent.badge,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
          )
        }

        IconButton(onClick = onToggleHandsFree, modifier = Modifier.size(36.dp)) {
          Icon(
            imageVector = Icons.Rounded.HeadsetMic,
            contentDescription = "Manos libres",
            tint = if (uiState.settings.autoListenHandsFree) {
              tutorAccent
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
          )
        }

        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onFinishSession),
          shape = RoundedCornerShape(999.dp),
          color = tutorAccent.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, tutorAccent.copy(alpha = 0.45f)),
        ) {
          Text(
            text = "Fin",
            style = MaterialTheme.typography.labelMedium,
            color = tutorAccent,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
          )
        }
      }
    }

    val errorText = uiState.errorBanner ?: uiState.audio.lastError
    if (errorText != null) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = FluentTheme.colors.recording.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, FluentTheme.colors.recording.copy(alpha = 0.4f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = errorText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          IconButton(onClick = onDismissError, modifier = Modifier.size(28.dp)) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Cerrar aviso",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }

    if (showPermissionRationale) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = FluentTheme.colors.correction.copy(alpha = 0.14f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Necesitamos acceso al micrófono para escucharte. También puedes escribir abajo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = onDismissPermissionRationale) {
            Text("OK")
          }
        }
      }
    }

    TutorCompactStageCard(
      uiState = uiState,
      tutorAccent = tutorAccent,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )

    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(uiState.turns, key = { it.id }) { turn ->
        if (turn.role == "user") {
          UserMessageBubble(turn = turn)
        } else {
          AssistantMessageBlock(
            turn = turn,
            uiState = uiState,
            tutorAccent = tutorAccent,
            onSpeakTurn = onSpeakTurn,
            onSpeakCorrection = { better -> onSpeakSimpleText(better, "corr-${turn.id}", false) },
            onSpeakSpanish = onSpeakSpanish,
            onSaveCorrection = onSaveCorrection,
            onStartCorrectionPractice = onStartCorrectionPractice,
            onToggleTranslation = { onToggleTranslation(turn.id) },
            onSaveTurn = { onSaveTurn(turn) },
            onPronunciationChipClick = { tip -> onPronunciationChipClick(turn.id, tip) },
          )
        }
      }

      if (uiState.isThinking) {
        item(key = "thinking-indicator") {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 4.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = tutorAccent,
            )
            Text(
              text = "${character.displayName} está preparando su respuesta...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    if (uiState.activeSuggestions.isNotEmpty() && !uiState.isThinking) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showIdeasDrawer = !showIdeasDrawer }
            .padding(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Habla libremente o toca aquí si necesitas una idea",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Icon(
            imageVector = if (showIdeasDrawer) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
            contentDescription = "Ideas",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
        AnimatedVisibility(visible = showIdeasDrawer) {
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(uiState.activeSuggestions) { suggestion ->
              val tag = "sug-${suggestion.hashCode()}"
              val playingThis = uiState.audio.isSpeaking && uiState.audio.speakingMessageId == tag
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
              ) {
                Row(
                  modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  IconButton(
                    onClick = { onSpeakSimpleText(suggestion, tag, false) },
                    modifier = Modifier.size(28.dp),
                  ) {
                    Icon(
                      imageVector = if (playingThis) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                      contentDescription = "Escuchar sugerencia",
                      tint = tutorAccent,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                  Text(
                    text = suggestion,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { onSendText(suggestion) },
                  )
                }
              }
            }
          }
        }
      }
    }

    VoiceControlDock(
      uiState = uiState,
      tutorAccent = tutorAccent,
      showKeyboardInput = showKeyboardInput,
      textDraft = textDraft,
      onTextDraftChange = { textDraft = it },
      onToggleKeyboard = { showKeyboardInput = !showKeyboardInput },
      onSendText = {
        val current = textDraft.trim()
        if (current.isNotEmpty()) {
          textDraft = ""
          onSendText(current)
        }
      },
      onMicClick = onMicClick,
      onCycleSpeechRate = onCycleSpeechRate,
    )
  }
}

private enum class LiveStageTab(val labelEs: String, val icon: ImageVector) {
  SPEAKING("Speaking", Icons.Rounded.ChatBubbleOutline),
  VOCABULARY("Vocab", Icons.AutoMirrored.Rounded.MenuBook),
  GRAMMAR("Guía", Icons.Rounded.Layers),
  FEEDBACK("Feedback", Icons.Rounded.Equalizer),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CinemaLiveSpeakingScreen(
  uiState: ConversationUiState,
  showPermissionRationale: Boolean,
  onDismissPermissionRationale: () -> Unit,
  onBack: () -> Unit,
  onMicClick: () -> Unit,
  onSendText: (String) -> Unit,
  onSpeakTurn: (ConversationTurn, Boolean) -> Unit,
  onSpeakSimpleText: (String, String, Boolean) -> Unit,
  onSpeakSpanish: (String, String) -> Unit,
  onPronunciationChipClick: (String, PronunciationTip) -> Unit,
  onToggleTranslation: (String) -> Unit,
  onTranslateAndSpeak: (ConversationTurn) -> Unit,
  onSaveTurn: (ConversationTurn) -> Unit,
  onSaveCorrection: (Correction) -> Unit,
  onStartCorrectionPractice: (Correction) -> Unit,
  onCycleSpeechRate: () -> Unit,
  onToggleAccent: () -> Unit,
  onToggleHandsFree: () -> Unit,
  onToggleChatView: () -> Unit,
  onDismissError: () -> Unit,
  onFinishSession: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val character = uiState.settings.selectedCharacter
  val activeLevel = uiState.settings.activeLevel()
  val latestAssistant = uiState.latestAssistantTurn
  val latestUser = uiState.latestUserTurn
  val isBeginner = activeLevel in setOf(LearnerLevel.NOVATO, LearnerLevel.JUNIOR)
  val cyanGlow = FluentTheme.colors.cyanGlow
  val violetGlow = FluentTheme.colors.violetGlow

  var selectedRailTab by rememberSaveable { mutableStateOf(LiveStageTab.SPEAKING) }
  var showHintSheet by rememberSaveable { mutableStateOf(false) }
  var showOptionsSheet by rememberSaveable { mutableStateOf(false) }
  var showTextComposer by rememberSaveable { mutableStateOf(false) }
  var draftText by rememberSaveable { mutableStateOf("") }

  var isCelebrating by remember { mutableStateOf(false) }
  LaunchedEffect(latestAssistant?.id, uiState.correctionPracticeAnalysis?.attemptNumber) {
    val goodPractice = uiState.correctionPracticeAnalysis?.passed == true
    val goodUserTurn = uiState.sessionTurnsCount > 0 && latestAssistant?.correction == null
    if (goodPractice || goodUserTurn) {
      isCelebrating = true
      delay(2600L)
      isCelebrating = false
    } else {
      isCelebrating = false
    }
  }

  val isListening = uiState.audio.isListening && !uiState.isListeningForPractice
  val levelVocab = remember(activeLevel) {
    VocabularyCatalog.forLevel(activeLevel).take(8)
  }

  val showTranslationBubble = latestAssistant?.let {
    uiState.settings.showTranslationsByDefault || (it.id in uiState.revealedTranslations)
  } ?: false
  val isSpeakingTranslation = latestAssistant != null &&
    uiState.audio.isSpeaking &&
    uiState.audio.speakingMessageId == "trans-es-${latestAssistant.id}"

  val hasNewFeedback = latestAssistant?.correction != null ||
    !latestAssistant?.pronunciationFocus.isNullOrEmpty()

  AuroraScreenBackground(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // 1. Studio Header matching Panel 2
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          GlowingCircleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Volver",
            onClick = onBack,
            size = 40.dp,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Speaking",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Conversación con IA · ${uiState.scenario.titleEs}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          GlowingCircleIconButton(
            icon = Icons.Rounded.ChatBubbleOutline,
            contentDescription = "Ver chat completo",
            onClick = onToggleChatView,
            size = 38.dp,
          )
          GlowingCircleIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Opciones de voz",
            onClick = { showOptionsSheet = !showOptionsSheet },
            size = 38.dp,
            borderColor = if (showOptionsSheet) cyanGlow else null,
          )
        }
      }

      AnimatedVisibility(visible = showOptionsSheet) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = Color(0xDF13172D),
          border = BorderStroke(1.dp, violetGlow.copy(alpha = 0.55f)),
        ) {
          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val rateText = String.format(Locale.US, "%.2gx", uiState.settings.effectiveSpeechRate())
            CinemaGlassPill(
              icon = Icons.Rounded.Speed,
              label = "Velocidad $rateText",
              highlighted = false,
              onClick = onCycleSpeechRate,
            )
            CinemaGlassPill(
              icon = Icons.Rounded.Translate,
              label = "Acento ${uiState.settings.accent.badge}",
              highlighted = false,
              onClick = onToggleAccent,
            )
            CinemaGlassPill(
              icon = Icons.Rounded.HeadsetMic,
              label = if (uiState.settings.autoListenHandsFree) "Manos libres ON" else "Manos libres OFF",
              highlighted = uiState.settings.autoListenHandsFree,
              onClick = onToggleHandsFree,
            )
          }
        }
      }

      val errorText = uiState.errorBanner ?: uiState.audio.lastError
      if (errorText != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xD97F1D1D),
          border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = errorText,
              style = MaterialTheme.typography.bodySmall,
              color = Color.White,
              modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      if (showPermissionRationale) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xD978350F),
          border = BorderStroke(1.dp, Color(0xFFFCD34D).copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Activa el micrófono para hablar, o toca el lápiz abajo para escribir.",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White,
              modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismissPermissionRationale) {
              Text("OK", color = Color(0xFFFDE68A))
            }
          }
        }
      }

      // 2. Framed Live Tutor Video Stage matching Panel 2
      val videoFrameShape = RoundedCornerShape(26.dp)
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(videoFrameShape)
          .border(
            width = 1.4.dp,
            brush = Brush.linearGradient(
              listOf(
                violetGlow.copy(alpha = 0.72f),
                cyanGlow.copy(alpha = 0.50f),
                violetGlow.copy(alpha = 0.72f),
              ),
            ),
            shape = videoFrameShape,
          ),
      ) {
        TutorLiveBackdrop(
          character = character,
          mood = uiState.avatarMood,
          rmsLevel = uiState.audio.rmsNormalized,
          speakingText = uiState.audio.speakingText,
          spokenCharRange = uiState.audio.spokenCharRange,
          isCelebrating = isCelebrating,
        )

        // Top overlay inside video frame: tutor online status + CEFR pill
        Row(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0x88080C1C),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF34D399)),
              )
              Column {
                Text(
                  text = character.displayName,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text = if (character == TutorCharacter.MANU) "Tu tutora de inglés" else "Tu tutor de inglés",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFCBD5E1),
                )
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color(0x9912162E),
            border = BorderStroke(1.dp, Color(0x44FFFFFF)),
          ) {
            Text(
              text = "${activeLevel.cefr} · ${activeLevel.labelEs}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
          }
        }

        // Bottom floating speech bubble / tab overlay inside video frame
        Box(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(12.dp),
        ) {
          when (selectedRailTab) {
            LiveStageTab.SPEAKING -> {
              if (uiState.isThinking) {
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = Color(0xD610152B),
                  border = BorderStroke(1.dp, violetGlow.copy(alpha = 0.65f)),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(16.dp),
                      strokeWidth = 2.dp,
                      color = cyanGlow,
                    )
                    Text(
                      text = "${character.displayName} está pensando...",
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.White,
                    )
                  }
                }
              } else if (latestAssistant != null) {
                val isSpeakingNormal = uiState.audio.isSpeaking &&
                  uiState.audio.speakingMessageId == latestAssistant.id
                val isSpeakingSlow = uiState.audio.isSpeaking &&
                  uiState.audio.speakingMessageId == "${latestAssistant.id}-slow"
                val isSpeakingThis = isSpeakingNormal || isSpeakingSlow

                Surface(
                  modifier = Modifier.widthIn(max = 310.dp),
                  shape = RoundedCornerShape(20.dp),
                  color = Color(0xC911162C),
                  border = BorderStroke(
                    width = if (isSpeakingThis) 1.5.dp else 1.dp,
                    color = if (isSpeakingThis) cyanGlow else violetGlow.copy(alpha = 0.55f),
                  ),
                ) {
                  Column(
                    modifier = Modifier
                      .padding(horizontal = 13.dp, vertical = 11.dp)
                      .heightIn(max = 225.dp)
                      .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Surface(
                          modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSpeakTurn(latestAssistant, false) },
                          shape = CircleShape,
                          color = if (isSpeakingNormal) cyanGlow.copy(alpha = 0.30f) else violetGlow.copy(alpha = 0.28f),
                        ) {
                          Icon(
                            imageVector = if (isSpeakingNormal) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Repetir",
                            tint = Color.White,
                            modifier = Modifier
                              .padding(6.dp)
                              .size(15.dp),
                          )
                        }
                        Surface(
                          modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onSpeakTurn(latestAssistant, true) },
                          shape = RoundedCornerShape(999.dp),
                          color = if (isSpeakingSlow) cyanGlow.copy(alpha = 0.28f) else Color(0x28FFFFFF),
                        ) {
                          Text(
                            text = if (isSpeakingSlow) "Stop" else "0.65x",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          )
                        }
                      }

                      Icon(
                        imageVector = if (latestAssistant.isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Guardar frase",
                        tint = if (latestAssistant.isSaved) Color(0xFF34D399) else Color(0xFFBCC7DD),
                        modifier = Modifier
                          .size(18.dp)
                          .clickable { onSaveTurn(latestAssistant) },
                      )
                    }

                    val activeRange = if (isSpeakingThis) uiState.audio.spokenCharRange else null
                    val annotated = remember(latestAssistant.content, activeRange, cyanGlow) {
                      buildKaraokeText(latestAssistant.content, activeRange, cyanGlow)
                    }
                    Text(
                      text = annotated,
                      fontSize = 15.sp,
                      lineHeight = 20.sp,
                      color = Color.White,
                    )

                    AnimatedVisibility(
                      visible = (showTranslationBubble && !latestAssistant.translationEs.isNullOrBlank()) ||
                        (isBeginner && !latestAssistant.coachNoteEs.isNullOrBlank() && showTranslationBubble),
                    ) {
                      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val translationText = latestAssistant.translationEs
                        if (!translationText.isNullOrBlank()) {
                          val spanishRange = if (isSpeakingTranslation) uiState.audio.spokenCharRange else null
                          val annotatedEs = remember(translationText, spanishRange, cyanGlow) {
                            buildKaraokeText(translationText, spanishRange, cyanGlow)
                          }
                          Surface(
                            modifier = Modifier
                              .fillMaxWidth()
                              .clip(RoundedCornerShape(10.dp))
                              .clickable { onTranslateAndSpeak(latestAssistant) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSpeakingTranslation) Color(0x3338BDF8) else Color(0x1CFFFFFF),
                          ) {
                            Row(
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                              verticalAlignment = Alignment.Top,
                              horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                              Icon(
                                imageVector = if (isSpeakingTranslation) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = if (isSpeakingTranslation) "Detener traducción" else "Oír traducción en español",
                                tint = if (isSpeakingTranslation) cyanGlow else Color(0xFF93C5FD),
                                modifier = Modifier
                                  .padding(top = 2.dp)
                                  .size(15.dp),
                              )
                              Text(
                                text = annotatedEs,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.weight(1f),
                              )
                              if (!uiState.settings.showTranslationsByDefault) {
                                Icon(
                                  imageVector = Icons.Rounded.Close,
                                  contentDescription = "Ocultar traducción",
                                  tint = Color(0xFF94A3B8),
                                  modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onToggleTranslation(latestAssistant.id) },
                                )
                              }
                            }
                          }
                        }
                        latestAssistant.coachNoteEs?.let { note ->
                          Surface(
                            modifier = Modifier
                              .fillMaxWidth()
                              .clip(RoundedCornerShape(10.dp))
                              .clickable { onSpeakSpanish(note, "live-es-${latestAssistant.id}") },
                            shape = RoundedCornerShape(10.dp),
                            color = cyanGlow.copy(alpha = 0.14f),
                          ) {
                            Row(
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                              Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Oír guía en español",
                                tint = cyanGlow,
                                modifier = Modifier.size(14.dp),
                              )
                              Text(
                                text = note,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE0F2FE),
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            LiveStageTab.VOCABULARY -> {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xE010152B),
                border = BorderStroke(1.dp, cyanGlow.copy(alpha = 0.45f)),
              ) {
                Column(
                  modifier = Modifier
                    .padding(13.dp)
                    .heightIn(max = 225.dp)
                    .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "Vocabulario útil (${activeLevel.cefr})",
                      style = MaterialTheme.typography.titleSmall,
                      color = cyanGlow,
                    )
                    Icon(
                      imageVector = Icons.Rounded.Close,
                      contentDescription = "Cerrar panel",
                      tint = Color(0xFF94A3B8),
                      modifier = Modifier
                        .size(18.dp)
                        .clickable { selectedRailTab = LiveStageTab.SPEAKING },
                    )
                  }
                  levelVocab.forEach { entry ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x18FFFFFF))
                        .clickable { onSpeakSimpleText(entry.word, "lv-voc-${entry.id}", false) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = "${entry.word}  ·  ${entry.ipa}",
                          style = MaterialTheme.typography.labelLarge,
                          color = Color.White,
                        )
                        Text(
                          text = entry.spanish,
                          style = MaterialTheme.typography.bodySmall,
                          color = Color(0xFFCBD5E1),
                        )
                      }
                      Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Oír",
                        tint = cyanGlow,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                }
              }
            }

            LiveStageTab.GRAMMAR -> {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xE010152B),
                border = BorderStroke(1.dp, cyanGlow.copy(alpha = 0.45f)),
              ) {
                Column(
                  modifier = Modifier
                    .padding(13.dp)
                    .heightIn(max = 225.dp)
                    .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "Guía en español (${activeLevel.cefr})",
                      style = MaterialTheme.typography.titleSmall,
                      color = cyanGlow,
                    )
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      val guideSpeech = latestAssistant?.coachNoteEs ?: uiState.scenario.coachIntroEs
                      if (guideSpeech.isNotBlank()) {
                        Surface(
                          modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onSpeakSpanish(guideSpeech, "gram-es-${latestAssistant?.id ?: "intro"}") },
                          shape = RoundedCornerShape(999.dp),
                          color = cyanGlow.copy(alpha = 0.22f),
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                          ) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = cyanGlow, modifier = Modifier.size(13.dp))
                            Text("Oír ES", style = MaterialTheme.typography.labelSmall, color = Color.White)
                          }
                        }
                      }
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar panel",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                          .size(18.dp)
                          .clickable { selectedRailTab = LiveStageTab.SPEAKING },
                      )
                    }
                  }
                  if (uiState.scenario.coachIntroEs.isNotBlank()) {
                    Text(
                      text = "Objetivo: ${uiState.scenario.coachIntroEs}",
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFFE2E8F0),
                    )
                  }
                  latestAssistant?.coachNoteEs?.let { note ->
                    HorizontalDivider(color = Color(0x28FFFFFF))
                    Text(
                      text = note,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color.White,
                    )
                  }
                }
              }
            }

            LiveStageTab.FEEDBACK -> {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xE010152B),
                border = BorderStroke(1.dp, cyanGlow.copy(alpha = 0.45f)),
              ) {
                Column(
                  modifier = Modifier
                    .padding(13.dp)
                    .heightIn(max = 225.dp)
                    .verticalScroll(rememberScrollState()),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "Retroalimentación (+${uiState.sessionXp} XP)",
                      style = MaterialTheme.typography.titleSmall,
                      color = cyanGlow,
                    )
                    Icon(
                      imageVector = Icons.Rounded.Close,
                      contentDescription = "Cerrar panel",
                      tint = Color(0xFF94A3B8),
                      modifier = Modifier
                        .size(18.dp)
                        .clickable { selectedRailTab = LiveStageTab.SPEAKING },
                    )
                  }
                  val correction = latestAssistant?.correction
                  if (correction != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(
                        text = "Mejor así: ${correction.better}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFDE68A),
                      )
                      if (correction.why.isNotBlank()) {
                        Text(
                          text = correction.why,
                          style = MaterialTheme.typography.bodySmall,
                          color = Color(0xFFE2E8F0),
                        )
                      }
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CinemaGlassPill(
                          icon = Icons.AutoMirrored.Rounded.VolumeUp,
                          label = "Oír",
                          highlighted = false,
                          onClick = { onSpeakSimpleText(correction.better, "corr-${latestAssistant.id}", false) },
                        )
                        CinemaGlassPill(
                          icon = Icons.Rounded.Mic,
                          label = "Practicar voz (75%+)",
                          highlighted = true,
                          onClick = { onStartCorrectionPractice(correction) },
                        )
                      }
                    }
                  } else {
                    Text(
                      text = "Habla por el micrófono para recibir correcciones en vivo y práctica de pronunciación.",
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFFCBD5E1),
                    )
                  }

                  val tips = latestAssistant?.pronunciationFocus.orEmpty()
                  if (tips.isNotEmpty()) {
                    HorizontalDivider(color = Color(0x26FFFFFF))
                    Text("Palabras clave:", style = MaterialTheme.typography.labelSmall, color = cyanGlow)
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      tips.forEach { tip ->
                        CinemaGlassPill(
                          icon = Icons.Rounded.RecordVoiceOver,
                          label = "${tip.word} ${tip.phonetic}",
                          highlighted = uiState.expandedTipKey == "${latestAssistant?.id}-${tip.word}",
                          onClick = { latestAssistant?.let { onPronunciationChipClick(it.id, tip) } },
                        )
                      }
                    }
                    uiState.expandedTipText?.let { sel ->
                      Text(
                        text = "Tip (${sel.word}): ${sel.tipEs}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE0F2FE),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 3. Bottom Studio Audio Deck matching Panel 2
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Live status indicator
        val statusDotColor = when {
          isListening -> Color(0xFF34D399)
          uiState.isThinking -> Color(0xFFFBBF24)
          uiState.audio.isSpeaking -> cyanGlow
          else -> Color(0xFF34D399)
        }
        val statusText = when {
          isListening -> "Escuchando..."
          uiState.isThinking -> "Pensando..."
          uiState.audio.isSpeaking && uiState.audio.speakingLanguage == "es" -> "${character.displayName} traduce en español..."
          uiState.audio.isSpeaking -> "${character.displayName} habla..."
          else -> "Toca el micrófono y habla en inglés"
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(statusDotColor),
          )
          Text(
            text = statusText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        // Full-Width Cyan-to-Violet Audio Spectrum Waveform matching Panel 2
        StudioSpectrumWaveform(
          isActive = isListening || uiState.audio.isSpeaking,
          rmsLevel = uiState.audio.rmsNormalized,
          modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        )

        // 3 Iconic Call Control Buttons matching Panel 2 (Pause/Replay | Mic Orb | Red End Call)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          GlowingCircleIconButton(
            icon = if (uiState.audio.isSpeaking) Icons.Rounded.Pause else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = if (uiState.audio.isSpeaking) "Pausar audio" else "Repetir audio",
            onClick = { latestAssistant?.let { onSpeakTurn(it, false) } },
            size = 50.dp,
            borderColor = violetGlow.copy(alpha = 0.55f),
          )

          Spacer(modifier = Modifier.width(24.dp))

          SpeakingOrbVisualizer(
            isListening = isListening,
            isSpeaking = uiState.audio.isSpeaking,
            passed = false,
            rmsLevel = uiState.audio.rmsNormalized,
            onClick = { if (!uiState.isThinking) onMicClick() },
            size = 76.dp,
            accentColor = violetGlow,
          )

          Spacer(modifier = Modifier.width(24.dp))

          GlowingCircleIconButton(
            icon = Icons.Rounded.CallEnd,
            contentDescription = "Finalizar sesión",
            onClick = onFinishSession,
            size = 50.dp,
            containerColor = FluentTheme.colors.callRed,
            borderColor = Color(0xFFFCA5A5).copy(alpha = 0.5f),
            iconTint = Color.White,
          )
        }

        // Quick studio action strip
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item {
            CinemaGlassPill(
              icon = if (isSpeakingTranslation) Icons.Rounded.Stop else Icons.Rounded.Translate,
              label = if (isSpeakingTranslation) "Detener ES" else "Traducción",
              highlighted = showTranslationBubble || isSpeakingTranslation,
              onClick = { latestAssistant?.let(onTranslateAndSpeak) },
            )
          }
          item {
            CinemaGlassPill(
              icon = Icons.Rounded.Lightbulb,
              label = "Pista",
              highlighted = showHintSheet,
              onClick = { showHintSheet = !showHintSheet },
            )
          }
          item {
            CinemaGlassPill(
              icon = LiveStageTab.VOCABULARY.icon,
              label = "Vocab (${activeLevel.cefr})",
              highlighted = selectedRailTab == LiveStageTab.VOCABULARY,
              onClick = {
                selectedRailTab = if (selectedRailTab == LiveStageTab.VOCABULARY) LiveStageTab.SPEAKING else LiveStageTab.VOCABULARY
              },
            )
          }
          item {
            CinemaGlassPill(
              icon = LiveStageTab.GRAMMAR.icon,
              label = "Guía ES",
              highlighted = selectedRailTab == LiveStageTab.GRAMMAR,
              onClick = {
                selectedRailTab = if (selectedRailTab == LiveStageTab.GRAMMAR) LiveStageTab.SPEAKING else LiveStageTab.GRAMMAR
              },
            )
          }
          item {
            CinemaGlassPill(
              icon = LiveStageTab.FEEDBACK.icon,
              label = if (hasNewFeedback) "Feedback •" else "Feedback",
              highlighted = selectedRailTab == LiveStageTab.FEEDBACK,
              onClick = {
                selectedRailTab = if (selectedRailTab == LiveStageTab.FEEDBACK) LiveStageTab.SPEAKING else LiveStageTab.FEEDBACK
              },
            )
          }
        }

        AnimatedVisibility(visible = showHintSheet && uiState.activeSuggestions.isNotEmpty()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xDF10152B),
            border = BorderStroke(1.dp, cyanGlow.copy(alpha = 0.45f)),
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "Ideas en inglés (toca para oír o responder):",
                style = MaterialTheme.typography.labelSmall,
                color = cyanGlow,
              )
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.activeSuggestions) { suggestion ->
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x28FFFFFF),
                    border = BorderStroke(1.dp, Color(0x38FFFFFF)),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Escuchar idea",
                        tint = cyanGlow,
                        modifier = Modifier
                          .size(16.dp)
                          .clickable { onSpeakSimpleText(suggestion, "hs-${suggestion.hashCode()}", false) },
                      )
                      Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.clickable {
                          showHintSheet = false
                          onSendText(suggestion)
                        },
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Bottom Transcript / Text Composer Box matching Panel 2 ("Tú (transcripción)")
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = Color(0xD912172E),
          border = BorderStroke(
            1.dp,
            if (isListening || showTextComposer) cyanGlow.copy(alpha = 0.65f) else FluentTheme.colors.cardBorder,
          ),
        ) {
          if (showTextComposer) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                placeholder = { Text("Escribe en inglés...", color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                  onSend = {
                    val clean = draftText.trim()
                    if (clean.isNotEmpty() && !uiState.isThinking) {
                      draftText = ""
                      showTextComposer = false
                      onSendText(clean)
                    }
                  },
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                  focusedBorderColor = cyanGlow,
                  unfocusedBorderColor = Color(0x55FFFFFF),
                ),
                modifier = Modifier.weight(1f),
              )
              Surface(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .clickable(enabled = draftText.isNotBlank() && !uiState.isThinking) {
                    val clean = draftText.trim()
                    draftText = ""
                    showTextComposer = false
                    onSendText(clean)
                  },
                shape = CircleShape,
                color = violetGlow,
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
              }
              IconButton(
                onClick = { showTextComposer = false },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar teclado", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
              }
            }
          } else {
            val transcriptText = when {
              isListening && uiState.audio.partialTranscript.isNotBlank() -> uiState.audio.partialTranscript
              latestUser != null -> latestUser.content
              else -> "Tu mensaje aparecerá aquí al hablar o escribir..."
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showTextComposer = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x28FFFFFF),
                modifier = Modifier.size(32.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "Aa",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Tú (transcripción)",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF94A3B8),
                )
                Text(
                  text = transcriptText,
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Escribir respuesta",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CinemaGlassPill(
  icon: ImageVector,
  label: String,
  highlighted: Boolean,
  onClick: () -> Unit,
) {
  val cyanGlow = FluentTheme.colors.cyanGlow
  val violetGlow = FluentTheme.colors.violetGlow
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(999.dp),
    color = if (highlighted) violetGlow.copy(alpha = 0.34f) else Color(0xC2131830),
    border = BorderStroke(1.dp, if (highlighted) cyanGlow else Color(0x44FFFFFF)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (highlighted) cyanGlow else Color(0xFFE2E8F0),
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        maxLines = 1,
      )
    }
  }
}

/** Full-width glowing cyan-to-violet audio spectrum wave matching Panel 2. */
@Composable
private fun StudioSpectrumWaveform(
  isActive: Boolean,
  rmsLevel: Float,
  modifier: Modifier = Modifier,
) {
  val inf = rememberInfiniteTransition(label = "studioWave")
  val phase by inf.animateFloat(
    initialValue = 0f,
    targetValue = 6.28318f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 860, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "studioWavePhase",
  )
  val smoothRms by animateFloatAsState(
    targetValue = rmsLevel.coerceIn(0f, 1f),
    animationSpec = tween(80),
    label = "studioWaveRms",
  )
  val cyan = FluentTheme.colors.cyanGlow
  val violet = FluentTheme.colors.violetGlow

  Canvas(modifier = modifier) {
    val count = 40
    val spacing = size.width / count.toFloat()
    val barW = spacing * 0.48f
    val midY = size.height / 2f
    for (i in 0 until count) {
      val norm = i.toFloat() / (count - 1).toFloat()
      val centerEnvelope = 1f - abs(norm - 0.5f) * 1.5f
      val amp = if (isActive) {
        ((0.28f + (sin(phase + i * 0.55f) + 1f) * 0.34f) * (0.65f + smoothRms * 0.55f)).coerceIn(0.16f, 1f)
      } else {
        (0.16f + (sin(i * 0.7f) + 1f) * 0.14f)
      }
      val barH = size.height * amp * centerEnvelope.coerceIn(0.22f, 1f)
      val barColor = if (norm < 0.45f) cyan else if (norm < 0.7f) Color(0xFF818CF8) else violet
      drawRoundRect(
        color = barColor.copy(alpha = if (isActive) 0.92f else 0.55f),
        topLeft = Offset(i * spacing + (spacing - barW) / 2f, midY - barH / 2f),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(barW / 2f, barW / 2f),
      )
    }
  }
}

@Composable
private fun TutorCompactStageCard(
  uiState: ConversationUiState,
  tutorAccent: Color,
  modifier: Modifier = Modifier,
) {
  val character = uiState.settings.selectedCharacter
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      TutorAvatar(
        character = character,
        mood = uiState.avatarMood,
        size = 56.dp,
        rmsLevel = uiState.audio.rmsNormalized,
        speakingText = uiState.audio.speakingText,
        spokenCharRange = uiState.audio.spokenCharRange,
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = character.displayName,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = uiState.statusLabelEs,
          style = MaterialTheme.typography.bodySmall,
          color = if (uiState.avatarMood == AvatarMood.LISTENING) {
            FluentTheme.colors.recording
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )
      }
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "+${uiState.sessionXp} XP",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun UserMessageBubble(turn: ConversationTurn) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End,
  ) {
    Surface(
      modifier = Modifier.widthIn(max = 300.dp),
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp),
      color = Color.Transparent,
    ) {
      Box(
        modifier = Modifier
          .background(
            Brush.linearGradient(
              listOf(
                FluentTheme.colors.userBubbleGradientStart,
                FluentTheme.colors.userBubbleGradientEnd,
              ),
            ),
          )
          .padding(horizontal = 15.dp, vertical = 11.dp),
      ) {
        Text(
          text = turn.content,
          style = MaterialTheme.typography.bodyLarge,
          color = Color.White,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantMessageBlock(
  turn: ConversationTurn,
  uiState: ConversationUiState,
  tutorAccent: Color,
  onSpeakTurn: (ConversationTurn, Boolean) -> Unit,
  onSpeakCorrection: (String) -> Unit,
  onSpeakSpanish: (String, String) -> Unit,
  onSaveCorrection: (Correction) -> Unit,
  onStartCorrectionPractice: (Correction) -> Unit,
  onToggleTranslation: () -> Unit,
  onSaveTurn: () -> Unit,
  onPronunciationChipClick: (PronunciationTip) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    turn.correction?.let { correction ->
      CorrectionFeedbackCard(
        correction = correction,
        onListen = { onSpeakCorrection(correction.better) },
        onSave = { onSaveCorrection(correction) },
        onPracticeSpeaking = { onStartCorrectionPractice(correction) },
      )
    }

    val isSpeakingNormal = uiState.audio.isSpeaking && uiState.audio.speakingMessageId == turn.id
    val isSpeakingSlow = uiState.audio.isSpeaking && uiState.audio.speakingMessageId == "${turn.id}-slow"
    val isSpeakingThis = isSpeakingNormal || isSpeakingSlow
    val showTranslation = uiState.settings.showTranslationsByDefault ||
      (turn.id in uiState.revealedTranslations)

    Surface(
      modifier = Modifier.widthIn(max = 328.dp),
      shape = RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(
        width = if (isSpeakingThis) 1.5.dp else 1.dp,
        color = if (isSpeakingThis) tutorAccent else FluentTheme.colors.cardBorder,
      ),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val activeRange = if (isSpeakingThis) uiState.audio.spokenCharRange else null
        val annotated = remember(turn.content, activeRange, tutorAccent) {
          buildKaraokeText(turn.content, activeRange, tutorAccent)
        }
        Text(
          text = annotated,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )

        val isSpeakingEsTranslation = uiState.audio.isSpeaking &&
          uiState.audio.speakingMessageId == "trans-es-${turn.id}"
        AnimatedVisibility(visible = showTranslation && !turn.translationEs.isNullOrBlank()) {
          val translationEs = turn.translationEs.orEmpty()
          val spanishRange = if (isSpeakingEsTranslation) uiState.audio.spokenCharRange else null
          val annotatedEs = remember(translationEs, spanishRange, tutorAccent) {
            buildKaraokeText(translationEs, spanishRange, tutorAccent)
          }
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onSpeakSpanish(translationEs, "trans-es-${turn.id}") },
            shape = RoundedCornerShape(12.dp),
            color = if (isSpeakingEsTranslation) {
              tutorAccent.copy(alpha = 0.14f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant
            },
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = if (isSpeakingEsTranslation) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = if (isSpeakingEsTranslation) "Detener traducción" else "Oír traducción en español",
                tint = if (isSpeakingEsTranslation) tutorAccent else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                  .padding(top = 2.dp)
                  .size(16.dp),
              )
              Text(
                text = annotatedEs,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        if (!turn.coachNoteEs.isNullOrBlank()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
              .clickable { onSpeakSpanish(turn.coachNoteEs, "chat-es-${turn.id}") }
              .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.School,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = turn.coachNoteEs,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
          }
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          BubbleActionPill(
            icon = if (isSpeakingNormal) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
            label = if (isSpeakingNormal) "Pausar" else "Escuchar",
            highlighted = isSpeakingNormal,
            accentColor = tutorAccent,
            onClick = { onSpeakTurn(turn, false) },
          )
          BubbleActionPill(
            icon = if (isSpeakingSlow) Icons.Rounded.Stop else Icons.Rounded.SlowMotionVideo,
            label = if (isSpeakingSlow) "Detener" else "0.65x",
            highlighted = isSpeakingSlow,
            accentColor = tutorAccent,
            onClick = { onSpeakTurn(turn, true) },
          )
          if (!turn.translationEs.isNullOrBlank()) {
            BubbleActionPill(
              icon = Icons.Rounded.Translate,
              label = if (showTranslation) "Ocultar ES" else "Traducir",
              highlighted = showTranslation,
              accentColor = MaterialTheme.colorScheme.primary,
              onClick = onToggleTranslation,
            )
          }
          BubbleActionPill(
            icon = if (turn.isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            label = if (turn.isSaved) "Guardada" else "Guardar",
            highlighted = turn.isSaved,
            accentColor = FluentTheme.colors.success,
            onClick = onSaveTurn,
          )
        }
      }
    }

    if (turn.pronunciationFocus.isNotEmpty()) {
      PronunciationTipsRow(
        turnId = turn.id,
        tips = turn.pronunciationFocus,
        uiState = uiState,
        tutorAccent = tutorAccent,
        onChipClick = onPronunciationChipClick,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PronunciationTipsRow(
  turnId: String,
  tips: List<PronunciationTip>,
  uiState: ConversationUiState,
  tutorAccent: Color,
  onChipClick: (PronunciationTip) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      tips.forEach { tip ->
        val chipKey = "$turnId-${tip.word}"
        val expanded = uiState.expandedTipKey == chipKey
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onChipClick(tip) },
          shape = RoundedCornerShape(12.dp),
          color = if (expanded) {
            tutorAccent.copy(alpha = 0.16f)
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
          },
          border = BorderStroke(
            1.dp,
            if (expanded) tutorAccent else FluentTheme.colors.cardBorder,
          ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.RecordVoiceOver,
              contentDescription = "Pronunciación",
              tint = tutorAccent,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = if (tip.phonetic.isNotBlank()) "${tip.word} · ${tip.phonetic}" else tip.word,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    val selectedTip = uiState.expandedTipText
    val isThisTurnTipExpanded = selectedTip != null &&
      uiState.expandedTipKey?.startsWith("$turnId-") == true
    AnimatedVisibility(visible = isThisTurnTipExpanded) {
      if (selectedTip != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = tutorAccent.copy(alpha = 0.10f),
          border = BorderStroke(1.dp, tutorAccent.copy(alpha = 0.35f)),
        ) {
          Text(
            text = "Tip (${selectedTip.word}): ${selectedTip.tipEs}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun CorrectionFeedbackCard(
  correction: Correction,
  onListen: () -> Unit,
  onSave: () -> Unit,
  onPracticeSpeaking: () -> Unit,
) {
  val corrColor = FluentTheme.colors.correction
  var saved by rememberSaveable(correction.better) { mutableStateOf(false) }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = corrColor.copy(alpha = 0.10f),
    border = BorderStroke(1.dp, corrColor.copy(alpha = 0.45f)),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Lightbulb,
            contentDescription = null,
            tint = corrColor,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Mejor así",
            style = MaterialTheme.typography.labelMedium,
            color = corrColor,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IconButton(onClick = onListen, modifier = Modifier.size(28.dp)) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
              contentDescription = "Escuchar corrección",
              tint = corrColor,
              modifier = Modifier.size(16.dp),
            )
          }
          IconButton(
            onClick = {
              saved = true
              onSave()
            },
            modifier = Modifier.size(28.dp),
          ) {
            Icon(
              imageVector = if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
              contentDescription = "Guardar corrección",
              tint = corrColor,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      Text(
        text = correction.better,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (correction.why.isNotBlank()) {
        Text(
          text = correction.why,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (!correction.tip.isNullOrBlank()) {
        Text(
          text = "Tip: ${correction.tip}",
          style = MaterialTheme.typography.labelSmall,
          color = corrColor,
        )
      }

      Surface(
        modifier = Modifier
          .padding(top = 2.dp)
          .clip(RoundedCornerShape(999.dp))
          .clickable(onClick = onPracticeSpeaking),
        shape = RoundedCornerShape(999.dp),
        color = corrColor.copy(alpha = 0.20f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = null,
            tint = corrColor,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "Practicar diciendo esta corrección",
            style = MaterialTheme.typography.labelSmall,
            color = corrColor,
          )
        }
      }
    }
  }
}

@Composable
private fun BubbleActionPill(
  icon: ImageVector,
  label: String,
  highlighted: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(999.dp),
    color = if (highlighted) {
      accentColor.copy(alpha = 0.20f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant
    },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (highlighted) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (highlighted) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun VoiceControlDock(
  uiState: ConversationUiState,
  tutorAccent: Color,
  showKeyboardInput: Boolean,
  textDraft: String,
  onTextDraftChange: (String) -> Unit,
  onToggleKeyboard: () -> Unit,
  onSendText: () -> Unit,
  onMicClick: () -> Unit,
  onCycleSpeechRate: () -> Unit,
) {
  val recordingColor = FluentTheme.colors.recording
  val isListening = uiState.audio.isListening && !uiState.isListeningForPractice

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      AnimatedVisibility(visible = isListening) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = recordingColor.copy(alpha = 0.12f),
        ) {
          Text(
            text = if (uiState.audio.partialTranscript.isNotBlank()) {
              "\"${uiState.audio.partialTranscript}\" · Toca Stop para enviar"
            } else {
              "Habla libremente en inglés... Toca Stop al terminar"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      }

      AudioWaveform(
        mood = uiState.avatarMood,
        rmsLevel = uiState.audio.rmsNormalized,
        activeColor = if (isListening) recordingColor else tutorAccent,
        idleColor = MaterialTheme.colorScheme.onSurfaceVariant,
        height = 20.dp,
      )

      AnimatedVisibility(visible = showKeyboardInput) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = textDraft,
            onValueChange = onTextDraftChange,
            placeholder = { Text("Escribe lo que quieras en inglés...") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendText() }),
            modifier = Modifier.weight(1f),
          )
          Surface(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .clickable(enabled = textDraft.isNotBlank() && !uiState.isThinking, onClick = onSendText),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Enviar",
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        val rateText = String.format(Locale.US, "%.2gx", uiState.settings.effectiveSpeechRate())
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onCycleSpeechRate),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Speed,
              contentDescription = "Velocidad de voz",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = rateText,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        SpeakingOrbVisualizer(
          isListening = isListening,
          isSpeaking = uiState.audio.isSpeaking,
          passed = false,
          rmsLevel = uiState.audio.rmsNormalized,
          onClick = { if (!uiState.isThinking) onMicClick() },
          size = 86.dp,
          accentColor = MaterialTheme.colorScheme.primary,
        )

        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggleKeyboard),
          shape = RoundedCornerShape(14.dp),
          color = if (showKeyboardInput) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
          } else {
            MaterialTheme.colorScheme.surfaceVariant
          },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Keyboard,
              contentDescription = "Teclado",
              tint = if (showKeyboardInput) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "Escribir",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SessionSummarySheetContent(
  uiState: ConversationUiState,
  onContinuePractice: () -> Unit,
  onReturnHome: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 22.dp)
      .padding(bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = "Resumen de tu práctica",
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )

    if (uiState.isSummarizing) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 16.dp),
      ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp))
        Text(
          text = "Calculando tu puntaje de fluidez...",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      val summary = uiState.sessionSummary ?: SessionSummary(
        strengthsEs = listOf("Excelente energía al practicar hoy."),
        focusAreasEs = listOf("Escucha las frases guardadas en tu cuaderno."),
        fluencyScore = 78,
        xpBonus = 20,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SummaryStatBox(
          label = "Fluidez",
          value = "${summary.fluencyScore}%",
          color = FluentTheme.colors.success,
          modifier = Modifier.weight(1f),
        )
        SummaryStatBox(
          label = "Turnos",
          value = "${uiState.sessionTurnsCount}",
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        SummaryStatBox(
          label = "XP Ganado",
          value = "+${uiState.sessionXp + summary.xpBonus}",
          color = FluentTheme.colors.manuAccent,
          modifier = Modifier.weight(1f),
        )
      }

      HorizontalDivider(color = FluentTheme.colors.cardBorder)

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Lo que hiciste bien",
          style = MaterialTheme.typography.titleSmall,
          color = FluentTheme.colors.success,
        )
        summary.strengthsEs.forEach { line ->
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = FluentTheme.colors.success,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = line,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Siguiente paso",
          style = MaterialTheme.typography.titleSmall,
          color = FluentTheme.colors.correction,
        )
        summary.focusAreasEs.forEach { line ->
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
              imageVector = Icons.Rounded.Lightbulb,
              contentDescription = null,
              tint = FluentTheme.colors.correction,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = line,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onContinuePractice,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text("Seguir")
        }
        Button(
          onClick = onReturnHome,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text("Volver al inicio")
        }
      }
    }
  }
}

@Composable
private fun SummaryStatBox(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = color.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = color,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun buildKaraokeText(
  text: String,
  activeRange: IntRange?,
  accentColor: Color,
) = buildAnnotatedString {
  if (activeRange == null || text.isEmpty()) {
    append(text)
    return@buildAnnotatedString
  }
  val start = activeRange.first.coerceIn(0, text.length)
  val endExclusive = (activeRange.last + 1).coerceIn(start, text.length)
  if (start >= endExclusive) {
    append(text)
    return@buildAnnotatedString
  }
  append(text.substring(0, start))
  withStyle(
    SpanStyle(
      background = accentColor.copy(alpha = 0.28f),
      fontWeight = FontWeight.Bold,
    ),
  ) {
    append(text.substring(start, endExclusive))
  }
  append(text.substring(endExclusive))
}

@Preview(showBackground = true)
@Composable
private fun ConversationFaceToFaceDarkPreview() {
  FluentVoiceTheme(themePreference = ThemePreference.DARK) {
    ConversationContent(
      uiState = ConversationUiState(
        scenario = ScenarioCatalog.byId("free_chat"),
        settings = AppSettings(selectedLevel = LearnerLevel.NOVATO),
        audio = AudioEngineState(),
        isFaceToFaceMode = true,
        turns = listOf(
          ConversationTurn(
            id = "1",
            role = "user",
            content = "How are you doing today Manu?",
          ),
          ConversationTurn(
            id = "2",
            role = "assistant",
            content = "I am doing wonderfully, thanks for asking! How are you feeling today?",
            translationEs = "¡Estoy muy bien, gracias por preguntar! ¿Cómo te sientes tú hoy?",
            coachNoteEs = "Guía rápida (A1): Te respondí que estoy muy bien y te pregunté cómo te sientes tú.",
            suggestedReplies = listOf("I am doing great today!", "A little tired but excited."),
            pronunciationFocus = listOf(
              PronunciationTip("wonderfully", "/ˈwʌndərfəli/", "Acento al inicio: 'wán-der-fu-li'."),
            ),
          ),
        ),
        sessionXp = 18,
      ),
      showPermissionRationale = false,
      onDismissPermissionRationale = {},
      onBack = {},
      onMicClick = {},
      onSendText = {},
      onSpeakTurn = { _, _ -> },
      onSpeakSimpleText = { _, _, _ -> },
      onSpeakSpanish = { _, _ -> },
      onPronunciationChipClick = { _, _ -> },
      onToggleTranslation = {},
      onTranslateAndSpeak = {},
      onSaveTurn = {},
      onSaveCorrection = {},
      onStartCorrectionPractice = {},
      onCycleSpeechRate = {},
      onToggleAccent = {},
      onToggleHandsFree = {},
      onToggleFaceToFaceMode = {},
      onDismissError = {},
      onFinishSession = {},
    )
  }
}
