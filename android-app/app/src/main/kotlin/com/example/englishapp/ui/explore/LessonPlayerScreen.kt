package com.example.englishapp.ui.explore

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.data.LessonsCatalog
import com.example.englishapp.data.SpeakingAnalysis
import com.example.englishapp.data.SpeakingCoachEngine
import com.example.englishapp.data.curriculum.CourseLesson
import com.example.englishapp.data.curriculum.LessonItemKind
import com.example.englishapp.data.curriculum.LessonStepItem
import com.example.englishapp.data.curriculum.MiniTestKind
import com.example.englishapp.model.AppSettings
import com.example.englishapp.ui.components.SpeakingMasteryCard
import com.example.englishapp.ui.theme.FluentTheme
import kotlin.random.Random

private sealed interface LessonPlayerStage {
  data class TeachItem(val itemIndex: Int) : LessonPlayerStage
  data class MiniTest(val itemIndex: Int) : LessonPlayerStage
  data object SpeakingCheck : LessonPlayerStage
  data object Victory : LessonPlayerStage
}

@Composable
fun LessonPlayerScreen(
  lessonId: String,
  settings: AppSettings,
  audioState: AudioEngineState,
  onPlayText: (text: String, tag: String, slow: Boolean) -> Unit,
  onPlaySpanishText: (text: String, tag: String) -> Unit,
  onSavePhrase: (english: String, spanish: String, note: String, source: String) -> Unit,
  onCompleteLesson: (lessonId: String) -> Unit,
  onStartSpeechDrill: (silenceMs: Long, onResult: (String) -> Unit) -> Unit,
  onStopSpeechDrill: () -> Unit,
  onStopAllAudio: () -> Unit,
  onOpenNextLesson: (nextLessonId: String) -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val lesson = remember(lessonId) {
    LessonsCatalog.byId(lessonId) ?: LessonsCatalog.forLevel(settings.activeLevel()).first()
  }
  val unit = remember(lesson.unitId) { LessonsCatalog.unitById(lesson.unitId) }
  val nextLesson = remember(lesson.id) { LessonsCatalog.nextLessonInUnit(lesson.id) }
  val accentColor = FluentTheme.colors.accentFor(settings.selectedCharacter)

  val totalSteps = lesson.items.size * 2 + 1
  var currentStage by remember(lesson.id) {
    mutableStateOf<LessonPlayerStage>(LessonPlayerStage.TeachItem(0))
  }
  var miniTestsCorrectFirstTry by remember(lesson.id) { mutableIntStateOf(0) }
  var speakingAnalysis by remember(lesson.id) { mutableStateOf<SpeakingAnalysis?>(null) }
  var speakingAttempts by remember(lesson.id) { mutableIntStateOf(0) }
  var completedRecorded by remember(lesson.id) { mutableStateOf(false) }

  val currentStepNumber = when (val st = currentStage) {
    is LessonPlayerStage.TeachItem -> st.itemIndex * 2 + 1
    is LessonPlayerStage.MiniTest -> st.itemIndex * 2 + 2
    LessonPlayerStage.SpeakingCheck -> totalSteps
    LessonPlayerStage.Victory -> totalSteps
  }

  val context = LocalContext.current
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      onStartSpeechDrill(2600L) { spoken ->
        val attempt = speakingAttempts + 1
        speakingAttempts = attempt
        val analysis = SpeakingCoachEngine.analyzePronunciation(
          expected = lesson.speakingTargetEn,
          spoken = spoken,
          attemptNumber = attempt,
          previousScore = speakingAnalysis?.overallScore,
        )
        speakingAnalysis = analysis
      }
    }
  }

  fun launchMicForLesson() {
    if (audioState.isListening) {
      onStopSpeechDrill()
      return
    }
    val granted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    if (granted) {
      onStartSpeechDrill(2600L) { spoken ->
        val attempt = speakingAttempts + 1
        speakingAttempts = attempt
        val analysis = SpeakingCoachEngine.analyzePronunciation(
          expected = lesson.speakingTargetEn,
          spoken = spoken,
          attemptNumber = attempt,
          previousScore = speakingAnalysis?.overallScore,
        )
        speakingAnalysis = analysis
      }
    } else {
      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  fun finishLessonVictory() {
    onStopAllAudio()
    if (!completedRecorded) {
      completedRecorded = true
      onCompleteLesson(lesson.id)
    }
    currentStage = LessonPlayerStage.Victory
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 20.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Top Progress Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .clickable {
            onStopAllAudio()
            onExit()
          },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Cerrar lección",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "${lesson.level.cefr} · ${unit?.titleEs ?: lesson.titleEs}",
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            maxLines = 1,
          )
          Text(
            text = if (currentStage == LessonPlayerStage.Victory) {
              "¡Completada!"
            } else {
              "Paso $currentStepNumber / $totalSteps"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        LinearProgressIndicator(
          progress = {
            if (currentStage == LessonPlayerStage.Victory) 1f
            else currentStepNumber.toFloat() / totalSteps.toFloat()
          },
          color = accentColor,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(999.dp)),
        )
      }
    }

    // Stepped Single-Card Content (NO webpage scroll)
    AnimatedContent(
      targetState = currentStage,
      transitionSpec = {
        (slideInHorizontally { it / 2 } + fadeIn()).togetherWith(
          slideOutHorizontally { -it / 2 } + fadeOut(),
        )
      },
      modifier = Modifier.weight(1f),
      label = "lessonStageTransition",
    ) { stage ->
      when (stage) {
        is LessonPlayerStage.TeachItem -> {
          val item = lesson.items[stage.itemIndex]
          TeachItemCard(
            item = item,
            itemNumber = stage.itemIndex + 1,
            totalItems = lesson.items.size,
            lessonTitleEs = lesson.titleEs,
            accentColor = accentColor,
            audioState = audioState,
            onPlayEnglish = { slow ->
              onPlayText(item.modelEn, "lp-en-${item.id}-${if (slow) "s" else "n"}", slow)
            },
            onPlaySpanish = {
              onPlaySpanishText(item.explanationEs, "lp-es-${item.id}")
            },
            onSaveExample = {
              onSavePhrase(item.modelEn, item.modelEs, item.tipEs, lesson.titleEs)
            },
            onProceedToQuiz = {
              onStopAllAudio()
              currentStage = LessonPlayerStage.MiniTest(stage.itemIndex)
            },
          )
        }

        is LessonPlayerStage.MiniTest -> {
          val item = lesson.items[stage.itemIndex]
          MiniTestItemCard(
            item = item,
            itemNumber = stage.itemIndex + 1,
            totalItems = lesson.items.size,
            accentColor = accentColor,
            audioState = audioState,
            onPlayPromptAudio = { txt, slow ->
              onPlayText(txt, "lp-quiz-${item.id}-${if (slow) "s" else "n"}", slow)
            },
            onSolved = { firstTry ->
              if (firstTry) miniTestsCorrectFirstTry++
              onStopAllAudio()
              val nextItem = stage.itemIndex + 1
              currentStage = if (nextItem < lesson.items.size) {
                LessonPlayerStage.TeachItem(nextItem)
              } else {
                LessonPlayerStage.SpeakingCheck
              }
            },
          )
        }

        LessonPlayerStage.SpeakingCheck -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = accentColor.copy(alpha = 0.14f),
            ) {
              Text(
                text = "RETO FINAL DE VOZ · ${lesson.level.cefr}",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
            Text(
              text = "Demuestra lo aprendido en voz alta",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "Pronuncia la frase clave obteniendo al menos 75% para completar esta lección.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SpeakingMasteryCard(
              targetPhrase = lesson.speakingTargetEn,
              spanishHint = lesson.speakingTargetEs,
              tipEs = lesson.speakingTipEs,
              analysis = speakingAnalysis,
              isListeningThis = audioState.isListening,
              audioState = audioState,
              passThreshold = 75,
              onListenNormal = { onPlayText(lesson.speakingTargetEn, "lp-spk-n-${lesson.id}", false) },
              onListenSlow = { onPlayText(lesson.speakingTargetEn, "lp-spk-s-${lesson.id}", true) },
              onListenWord = { word -> onPlayText(word, "lp-spk-w-$word", true) },
              onToggleRecord = { launchMicForLesson() },
            )

            Spacer(modifier = Modifier.weight(1f))

            if (speakingAnalysis?.passed == true) {
              Button(
                onClick = { finishLessonVictory() },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(54.dp),
                shape = RoundedCornerShape(16.dp),
              ) {
                Text("Completar lección (+35 XP)")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
              }
            } else if (speakingAttempts >= 3) {
              OutlinedButton(
                onClick = { finishLessonVictory() },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp),
                shape = RoundedCornerShape(16.dp),
              ) {
                Text("Finalizar por ahora sin micrófono / tras 3 intentos")
              }
            }
          }
        }

        LessonPlayerStage.Victory -> {
          LessonVictoryCard(
            lesson = lesson,
            miniTestsPerfect = miniTestsCorrectFirstTry,
            totalItems = lesson.items.size,
            speakingScore = speakingAnalysis?.overallScore,
            accentColor = accentColor,
            nextLesson = nextLesson,
            onNextLesson = { nxt -> onOpenNextLesson(nxt.id) },
            onExit = onExit,
          )
        }
      }
    }
  }
}

@Composable
private fun TeachItemCard(
  item: LessonStepItem,
  itemNumber: Int,
  totalItems: Int,
  lessonTitleEs: String,
  accentColor: Color,
  audioState: AudioEngineState,
  onPlayEnglish: (slow: Boolean) -> Unit,
  onPlaySpanish: () -> Unit,
  onSaveExample: () -> Unit,
  onProceedToQuiz: () -> Unit,
) {
  var saved by rememberSaveable(item.id) { mutableStateOf(false) }
  val isPlayingEs = audioState.isSpeaking && audioState.speakingMessageId == "lp-es-${item.id}"
  val isPlayingEnNorm = audioState.isSpeaking && audioState.speakingMessageId == "lp-en-${item.id}-n"
  val isPlayingEnSlow = audioState.isSpeaking && audioState.speakingMessageId == "lp-en-${item.id}-s"

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (item.kind == LessonItemKind.SPANISH_TRAP || item.kind == LessonItemKind.SPECIALIZED_SKILL) {
          FluentTheme.colors.correction.copy(alpha = 0.16f)
        } else {
          accentColor.copy(alpha = 0.15f)
        },
      ) {
        Text(
          text = item.kind.badgeEs,
          style = MaterialTheme.typography.labelMedium,
          color = if (item.kind == LessonItemKind.SPANISH_TRAP || item.kind == LessonItemKind.SPECIALIZED_SKILL) {
            FluentTheme.colors.correction
          } else {
            accentColor
          },
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
      Text(
        text = "Punto $itemNumber de $totalItems",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Text(
      text = item.titleEs,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
      text = "$lessonTitleEs · ${item.titleEn}",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Spanish Explanation Card + Audio
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Explicación en español",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .clickable(onClick = onPlaySpanish),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = if (isPlayingEs) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Oír explicación",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = if (isPlayingEs) "Pausar" else "Oír ES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
        Text(
          text = item.explanationEs,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    // Bilingual Model Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = accentColor.copy(alpha = 0.10f),
      border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "EJEMPLO EN ACCIÓN",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = accentColor,
        )
        Text(
          text = item.modelEn,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = item.modelEs,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = { onPlayEnglish(false) },
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = if (isPlayingEnNorm) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isPlayingEnNorm) "Pausar" else "Escuchar EN")
          }
          OutlinedButton(
            onClick = { onPlayEnglish(true) },
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = if (isPlayingEnSlow) Icons.Rounded.Stop else Icons.Rounded.SlowMotionVideo,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("0.65x")
          }
          Spacer(modifier = Modifier.weight(1f))
          IconButton(
            onClick = {
              saved = true
              onSaveExample()
            },
          ) {
            Icon(
              imageVector = if (saved) Icons.Rounded.CheckCircle else Icons.Rounded.BookmarkBorder,
              contentDescription = "Guardar ejemplo",
              tint = if (saved) FluentTheme.colors.success else accentColor,
            )
          }
        }
      }
    }

    // Extra Practical Tip
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = FluentTheme.colors.correction.copy(alpha = 0.11f),
      border = BorderStroke(1.dp, FluentTheme.colors.correction.copy(alpha = 0.35f)),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          imageVector = Icons.Rounded.Lightbulb,
          contentDescription = null,
          tint = FluentTheme.colors.correction,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = item.tipEs,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Button(
      onClick = onProceedToQuiz,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
      shape = RoundedCornerShape(16.dp),
    ) {
      Text("Ir al Mini-Test de este punto")
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MiniTestItemCard(
  item: LessonStepItem,
  itemNumber: Int,
  totalItems: Int,
  accentColor: Color,
  audioState: AudioEngineState,
  onPlayPromptAudio: (String, Boolean) -> Unit,
  onSolved: (firstTry: Boolean) -> Unit,
) {
  val test = item.miniTest
  var selectedIndex by rememberSaveable(item.id) { mutableStateOf<Int?>(null) }
  var hadWrongAttempt by rememberSaveable(item.id) { mutableStateOf(false) }
  var isSolved by rememberSaveable(item.id) { mutableStateOf(false) }

  // Word builder state (unique indexed tokens to handle repeated words like "the")
  val shuffledPool = remember(item.id) {
    (test.targetTokens + test.distractionTokens)
      .mapIndexed { idx, txt -> idx to txt }
      .shuffled(Random(item.id.hashCode()))
  }
  var chosenPoolIndices by remember(item.id) { mutableStateOf<List<Int>>(emptyList()) }
  var builderError by remember(item.id) { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
      ) {
        Text(
          text = "MINI-TEST INMEDIATO · PUNTO $itemNumber/$totalItems",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
      Text(
        text = item.titleEs,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Text(
      text = test.promptEs,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )

    if (test.kind == MiniTestKind.LISTENING_CHOICE && !test.audioPromptEn.isNullOrBlank()) {
      val audioTxt = test.audioPromptEn
      val playingNorm = audioState.isSpeaking && audioState.speakingMessageId == "lp-quiz-${item.id}-n"
      val playingSlow = audioState.isSpeaking && audioState.speakingMessageId == "lp-quiz-${item.id}-s"
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = { onPlayPromptAudio(audioTxt, false) },
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = if (playingNorm) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
              contentDescription = null,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (playingNorm) "Pausar" else "Escuchar audio")
          }
          OutlinedButton(
            onClick = { onPlayPromptAudio(audioTxt, true) },
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = if (playingSlow) Icons.Rounded.Stop else Icons.Rounded.SlowMotionVideo,
              contentDescription = null,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("0.65x")
          }
        }
      }
    }

    if (test.kind == MiniTestKind.WORD_BUILDER) {
      // Built sentence box
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
          1.5.dp,
          when {
            isSolved -> FluentTheme.colors.success
            builderError -> FluentTheme.colors.recording
            else -> FluentTheme.colors.cardBorder
          },
        ),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Toca las palabras abajo para formar la frase (toca una colocada para quitarla):",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            if (chosenPoolIndices.isEmpty()) {
              Text(
                text = "Tu frase aparecerá aquí...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 8.dp),
              )
            } else {
              chosenPoolIndices.forEachIndexed { pos, poolIdx ->
                val wordTxt = shuffledPool.first { it.first == poolIdx }.second
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isSolved) {
                      chosenPoolIndices = chosenPoolIndices.toMutableList().apply { removeAt(pos) }
                      builderError = false
                    },
                  shape = RoundedCornerShape(12.dp),
                  color = if (isSolved) FluentTheme.colors.success.copy(alpha = 0.16f) else accentColor.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, if (isSolved) FluentTheme.colors.success else accentColor),
                ) {
                  Text(
                    text = wordTxt,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  )
                }
              }
            }
          }
        }
      }

      // Scrambled Word Bank
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        shuffledPool.forEach { (poolIdx, wordTxt) ->
          val used = poolIdx in chosenPoolIndices
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable(enabled = !used && !isSolved) {
                chosenPoolIndices = chosenPoolIndices + poolIdx
                builderError = false
              },
            shape = RoundedCornerShape(12.dp),
            color = if (used) {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
              MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
              1.dp,
              if (used) Color.Transparent else FluentTheme.colors.cardBorder,
            ),
          ) {
            Text(
              text = wordTxt,
              style = MaterialTheme.typography.titleSmall,
              color = if (used) Color.Transparent else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
          }
        }
      }

      if (!isSolved) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              chosenPoolIndices = emptyList()
              builderError = false
            },
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Limpiar")
          }
          Button(
            onClick = {
              val built = chosenPoolIndices.map { idx -> shuffledPool.first { it.first == idx }.second }
              if (built == test.targetTokens) {
                isSolved = true
                builderError = false
                onPlayPromptAudio(test.targetTokens.joinToString(" "), false)
              } else {
                hadWrongAttempt = true
                builderError = true
              }
            },
            enabled = chosenPoolIndices.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f),
          ) {
            Text("Comprobar frase")
          }
        }
      }

      if (builderError && !isSolved) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = FluentTheme.colors.recording.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, FluentTheme.colors.recording.copy(alpha = 0.4f)),
        ) {
          Text(
            text = "Casi—revisa el orden o quita la palabra extra. Pista: ${test.explanationEs}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    } else {
      // MULTIPLE_CHOICE or LISTENING_CHOICE
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        test.options.forEachIndexed { idx, opt ->
          val picked = selectedIndex == idx
          val isCorrectOpt = idx == test.correctIndex
          val border = when {
            picked && isCorrectOpt -> FluentTheme.colors.success
            picked && !isCorrectOpt -> FluentTheme.colors.recording
            else -> FluentTheme.colors.cardBorder
          }
          val bg = when {
            picked && isCorrectOpt -> FluentTheme.colors.success.copy(alpha = 0.14f)
            picked && !isCorrectOpt -> FluentTheme.colors.recording.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.surface
          }
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable(enabled = !isSolved) {
                selectedIndex = idx
                if (idx == test.correctIndex) {
                  isSolved = true
                } else {
                  hadWrongAttempt = true
                }
              },
            shape = RoundedCornerShape(16.dp),
            color = bg,
            border = BorderStroke(if (picked) 1.6.dp else 1.dp, border),
          ) {
            Row(
              modifier = Modifier.padding(15.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = opt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
              )
              if (picked) {
                Icon(
                  imageVector = if (isCorrectOpt) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                  contentDescription = null,
                  tint = if (isCorrectOpt) FluentTheme.colors.success else FluentTheme.colors.recording,
                )
              }
            }
          }
        }
      }

      if (selectedIndex != null && !isSolved) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = FluentTheme.colors.recording.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, FluentTheme.colors.recording.copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = FluentTheme.colors.recording)
            Text(
              text = "Intenta otra opción. Recuerda: ${test.explanationEs}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    if (isSolved) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = FluentTheme.colors.success.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, FluentTheme.colors.success.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "¡Correcto!",
            style = MaterialTheme.typography.titleSmall,
            color = FluentTheme.colors.success,
          )
          Text(
            text = test.explanationEs,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Button(
      onClick = { onSolved(!hadWrongAttempt) },
      enabled = isSolved,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
      shape = RoundedCornerShape(16.dp),
    ) {
      Text(
        if (itemNumber < totalItems) "Siguiente punto →" else "Ir al reto de voz →",
      )
    }
  }
}

@Composable
private fun LessonVictoryCard(
  lesson: CourseLesson,
  miniTestsPerfect: Int,
  totalItems: Int,
  speakingScore: Int?,
  accentColor: Color,
  nextLesson: CourseLesson?,
  onNextLesson: (CourseLesson) -> Unit,
  onExit: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier = Modifier
        .size(92.dp)
        .clip(CircleShape)
        .background(FluentTheme.colors.success.copy(alpha = 0.16f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.EmojiEvents,
        contentDescription = null,
        tint = FluentTheme.colors.success,
        modifier = Modifier.size(48.dp),
      )
    }

    Text(
      text = "¡Lección dominada!",
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )
    Text(
      text = "${lesson.titleEs} (${lesson.level.cefr})",
      style = MaterialTheme.typography.titleMedium,
      color = accentColor,
      textAlign = TextAlign.Center,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      VictoryMetricBox(
        label = "XP Ganado",
        value = "+35 XP",
        color = FluentTheme.colors.success,
        modifier = Modifier.weight(1f),
      )
      VictoryMetricBox(
        label = "Mini-Tests",
        value = "$miniTestsPerfect/$totalItems a la 1.ª",
        color = accentColor,
        modifier = Modifier.weight(1f),
      )
      VictoryMetricBox(
        label = "Voz",
        value = if (speakingScore != null) "$speakingScore%" else "OK",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    if (nextLesson != null) {
      Button(
        onClick = { onNextLesson(nextLesson) },
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text("Siguiente: ${nextLesson.titleEs}")
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
      }
      OutlinedButton(
        onClick = onExit,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text("Volver al mapa de unidades")
      }
    } else {
      Button(
        onClick = onExit,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text("Volver a mis unidades")
      }
    }
  }
}

@Composable
private fun VictoryMetricBox(
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
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}
