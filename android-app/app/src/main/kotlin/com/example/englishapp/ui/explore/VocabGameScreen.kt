package com.example.englishapp.ui.explore

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.data.LetterPuzzleState
import com.example.englishapp.data.SpeakingCoachEngine
import com.example.englishapp.data.VocabEntry
import com.example.englishapp.data.VocabLetterGameEngine
import com.example.englishapp.data.VocabularyCatalog
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.UserProgress
import com.example.englishapp.ui.theme.FluentTheme

private sealed interface VocabRoundStage {
  data object StudyCard : VocabRoundStage
  data object LetterGame : VocabRoundStage
  data object RoundVictory : VocabRoundStage
}

@Composable
fun VocabGameScreen(
  settings: AppSettings,
  progress: UserProgress,
  audioState: AudioEngineState,
  categoryFilter: String? = null,
  startWordId: String? = null,
  initialGameOnlyMode: Boolean = false,
  onPlayText: (text: String, tag: String, slow: Boolean) -> Unit,
  onPlaySpanishText: (text: String, tag: String) -> Unit,
  onSavePhrase: (english: String, spanish: String, note: String, source: String) -> Unit,
  onMarkVocabMastered: (wordIds: Collection<String>, bonusXp: Int) -> Unit,
  onStartSpeechCheck: (silenceMs: Long, onResult: (String) -> Unit) -> Unit,
  onStopSpeechCheck: () -> Unit,
  onStopAllAudio: () -> Unit,
  onExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeLevel = settings.activeLevel()
  val accentColor = FluentTheme.colors.accentFor(settings.selectedCharacter)

  var roundSeed by remember { mutableIntStateOf(1) }
  var gameOnlyMode by rememberSaveable { mutableStateOf(initialGameOnlyMode) }

  val roundWords = remember(activeLevel, categoryFilter, startWordId, roundSeed) {
    val levelPool = VocabularyCatalog.forLevel(activeLevel)
      .let { list ->
        if (categoryFilter.isNullOrBlank()) list else list.filter { it.category == categoryFilter }
      }
      .ifEmpty { VocabularyCatalog.forLevel(activeLevel) }

    val startEntry = startWordId?.let { id -> levelPool.firstOrNull { it.id == id } }
    val unmastered = levelPool.filter { it.id !in progress.masteredVocabIds && it.id != startWordId }
      .shuffled(kotlin.random.Random(roundSeed))
    val remainder = levelPool.filter { it.id in progress.masteredVocabIds && it.id != startWordId }
      .shuffled(kotlin.random.Random(roundSeed + 13))

    buildList {
      if (startEntry != null) add(startEntry)
      addAll(unmastered)
      addAll(remainder)
    }.take(8)
  }

  var wordIdx by remember(roundWords) { mutableIntStateOf(0) }
  val currentEntry = roundWords[wordIdx.coerceIn(0, roundWords.lastIndex)]
  var stage by remember(roundWords, gameOnlyMode) {
    mutableStateOf<VocabRoundStage>(
      if (gameOnlyMode) VocabRoundStage.LetterGame else VocabRoundStage.StudyCard,
    )
  }

  var puzzleState by remember(currentEntry.id, roundSeed) {
    mutableStateOf(
      VocabLetterGameEngine.createPuzzle(
        entry = currentEntry,
        level = activeLevel,
        seed = currentEntry.id.hashCode() + roundSeed * 17,
      ),
    )
  }

  var solvedIds by remember(roundSeed) { mutableStateOf<Set<String>>(emptySet()) }
  var streakCount by remember(roundSeed) { mutableIntStateOf(0) }
  var bestStreak by remember(roundSeed) { mutableIntStateOf(0) }
  var roundXp by remember(roundSeed) { mutableIntStateOf(0) }
  var lastWordXp by remember(currentEntry.id) { mutableIntStateOf(0) }
  var wordSpokenOk by remember(currentEntry.id) { mutableStateOf<Int?>(null) }
  var rewardRecorded by remember(roundSeed) { mutableStateOf(false) }

  val context = LocalContext.current
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      onStartSpeechCheck(2100L) { spoken ->
        val analysis = SpeakingCoachEngine.analyzePronunciation(
          expected = currentEntry.word,
          spoken = spoken,
          passThreshold = 70,
        )
        wordSpokenOk = analysis.overallScore
      }
    }
  }

  fun practiceCurrentWordVoice() {
    if (audioState.isListening) {
      onStopSpeechCheck()
      return
    }
    val granted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    if (granted) {
      onStartSpeechCheck(2100L) { spoken ->
        val analysis = SpeakingCoachEngine.analyzePronunciation(
          expected = currentEntry.word,
          spoken = spoken,
          passThreshold = 70,
        )
        wordSpokenOk = analysis.overallScore
      }
    } else {
      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  fun onPuzzleUpdated(updated: LetterPuzzleState) {
    val wasSolved = puzzleState.isSolved
    puzzleState = updated
    if (!wasSolved && updated.isSolved) {
      val cleanSolve = updated.wrongAttempts == 0 && updated.hintsUsed == 0
      val newStreak = if (cleanSolve) streakCount + 1 else 1
      streakCount = newStreak
      if (newStreak > bestStreak) bestStreak = newStreak
      val earned = VocabLetterGameEngine.computeWordXp(
        hintsUsed = updated.hintsUsed,
        wrongAttempts = updated.wrongAttempts,
        currentStreak = newStreak,
      )
      lastWordXp = earned
      roundXp += earned
      solvedIds = solvedIds + currentEntry.id
      onPlayText(currentEntry.word, "vg-win-${currentEntry.id}", false)
    }
  }

  fun goToNextWord() {
    onStopAllAudio()
    if (wordIdx + 1 < roundWords.size) {
      wordIdx++
      stage = if (gameOnlyMode) VocabRoundStage.LetterGame else VocabRoundStage.StudyCard
    } else {
      if (!rewardRecorded) {
        rewardRecorded = true
        onMarkVocabMastered(solvedIds, roundXp.coerceAtLeast(10))
      }
      stage = VocabRoundStage.RoundVictory
    }
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
    // Top Navigation + Streak Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .clickable {
            onStopAllAudio()
            if (solvedIds.isNotEmpty() && !rewardRecorded) {
              rewardRecorded = true
              onMarkVocabMastered(solvedIds, roundXp)
            }
            onExit()
          },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Cerrar vocabulario",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = FluentTheme.colors.correction.copy(alpha = 0.16f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.LocalFireDepartment,
              contentDescription = null,
              tint = FluentTheme.colors.correction,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = "$streakCount",
              style = MaterialTheme.typography.labelLarge,
              color = FluentTheme.colors.correction,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = accentColor.copy(alpha = 0.16f),
        ) {
          Text(
            text = "+$roundXp XP · ${activeLevel.cefr}",
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }
    }

    // Mode Toggle (Study+Game vs Fast Game) + Progress Bar
    if (stage != VocabRoundStage.RoundVictory) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ModeToggleChip(
            label = "Aprende + Letras",
            selected = !gameOnlyMode,
            accentColor = accentColor,
            onClick = {
              gameOnlyMode = false
              if (stage == VocabRoundStage.LetterGame && !puzzleState.isSolved) {
                stage = VocabRoundStage.StudyCard
              }
            },
            modifier = Modifier.weight(1f),
          )
          ModeToggleChip(
            label = "Desafío de Letras",
            selected = gameOnlyMode,
            accentColor = accentColor,
            onClick = {
              gameOnlyMode = true
              stage = VocabRoundStage.LetterGame
            },
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          LinearProgressIndicator(
            progress = { (wordIdx + 1).toFloat() / roundWords.size.toFloat() },
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .weight(1f)
              .height(6.dp)
              .clip(RoundedCornerShape(999.dp)),
          )
          Text(
            text = "${wordIdx + 1} / ${roundWords.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Stage Card Area
    AnimatedContent(
      targetState = stage to currentEntry.id,
      transitionSpec = {
        (slideInHorizontally { it / 2 } + fadeIn()).togetherWith(
          slideOutHorizontally { -it / 2 } + fadeOut(),
        )
      },
      modifier = Modifier.weight(1f),
      label = "vocabGameTransition",
    ) { (activeStage, _) ->
      when (activeStage) {
        VocabRoundStage.StudyCard -> {
          VocabStudyWindowCard(
            entry = currentEntry,
            accentColor = accentColor,
            audioState = audioState,
            onPlayEnglish = { slow ->
              onPlayText(currentEntry.word, "vg-study-${currentEntry.id}-${if (slow) "s" else "n"}", slow)
            },
            onPlayExample = {
              onPlayText(currentEntry.exampleEn, "vg-ex-${currentEntry.id}", false)
            },
            onPlaySpanish = {
              onPlaySpanishText(currentEntry.spanish, "vg-es-${currentEntry.id}")
            },
            onSavePhrase = {
              onSavePhrase(
                currentEntry.exampleEn,
                currentEntry.exampleEs,
                "${currentEntry.word} (${currentEntry.spanish}): ${currentEntry.tipEs}",
                "Vocabulario ${activeLevel.cefr}",
              )
            },
            onGoToGame = {
              onStopAllAudio()
              stage = VocabRoundStage.LetterGame
            },
          )
        }

        VocabRoundStage.LetterGame -> {
          LetterScrambleGameCard(
            puzzle = puzzleState,
            accentColor = accentColor,
            audioState = audioState,
            lastWordXp = lastWordXp,
            spokenScore = wordSpokenOk,
            isLastWord = wordIdx == roundWords.lastIndex,
            onPlayClueAudio = { slow ->
              onPlayText(currentEntry.word, "vg-clue-${currentEntry.id}-${if (slow) "s" else "n"}", slow)
            },
            onTapTile = { tileId ->
              onPuzzleUpdated(VocabLetterGameEngine.tapTile(puzzleState, tileId))
            },
            onTapSlot = { slotIdx ->
              onPuzzleUpdated(VocabLetterGameEngine.tapSlot(puzzleState, slotIdx))
            },
            onUseHint = {
              onPuzzleUpdated(VocabLetterGameEngine.useHint(puzzleState))
            },
            onReshuffle = {
              puzzleState = VocabLetterGameEngine.reshuffleAvailable(
                puzzleState,
                seed = System.nanoTime().toInt(),
              )
            },
            onClear = {
              puzzleState = VocabLetterGameEngine.clearSlots(puzzleState)
            },
            onPracticePronounce = { practiceCurrentWordVoice() },
            onNext = { goToNextWord() },
          )
        }

        VocabRoundStage.RoundVictory -> {
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
              text = "¡Ronda completada!",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "Dominaste ${solvedIds.size} palabras de nivel ${activeLevel.cefr}",
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
                value = "+${roundXp.coerceAtLeast(10)}",
                color = FluentTheme.colors.success,
                modifier = Modifier.weight(1f),
              )
              VictoryMetricBox(
                label = "Palabras",
                value = "${solvedIds.size}",
                color = accentColor,
                modifier = Modifier.weight(1f),
              )
              VictoryMetricBox(
                label = "Mejor racha",
                value = "${bestStreak}x",
                color = FluentTheme.colors.correction,
                modifier = Modifier.weight(1f),
              )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
              onClick = {
                wordIdx = 0
                roundSeed += 1
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
              shape = RoundedCornerShape(16.dp),
            ) {
              Text("Jugar otra ronda (${activeLevel.cefr})")
              Spacer(modifier = Modifier.width(8.dp))
              Icon(Icons.Rounded.Refresh, contentDescription = null)
            }
            OutlinedButton(
              onClick = onExit,
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
              shape = RoundedCornerShape(16.dp),
            ) {
              Text("Volver a Aprender")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ModeToggleChip(
  label: String,
  selected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = if (selected) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, if (selected) accentColor else FluentTheme.colors.cardBorder),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
    )
  }
}

@Composable
private fun VocabStudyWindowCard(
  entry: VocabEntry,
  accentColor: Color,
  audioState: AudioEngineState,
  onPlayEnglish: (slow: Boolean) -> Unit,
  onPlayExample: () -> Unit,
  onPlaySpanish: () -> Unit,
  onSavePhrase: () -> Unit,
  onGoToGame: () -> Unit,
) {
  var saved by rememberSaveable(entry.id) { mutableStateOf(false) }
  val playingWord = audioState.isSpeaking && audioState.speakingMessageId == "vg-study-${entry.id}-n"
  val playingSlow = audioState.isSpeaking && audioState.speakingMessageId == "vg-study-${entry.id}-s"
  val playingEx = audioState.isSpeaking && audioState.speakingMessageId == "vg-ex-${entry.id}"

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = accentColor.copy(alpha = 0.15f),
        ) {
          Text(
            text = "${entry.category} · ${entry.level.cefr}",
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }

        Text(
          text = entry.word,
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )
        Text(
          text = entry.ipa,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlaySpanish),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "Significado en español",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = entry.spanish,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = { onPlayEnglish(false) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = if (playingWord) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Oír EN")
          }
          OutlinedButton(
            onClick = { onPlayEnglish(true) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = if (playingSlow) Icons.Rounded.Stop else Icons.Rounded.SlowMotionVideo,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("0.65x")
          }
          IconButton(
            onClick = {
              saved = true
              onSavePhrase()
            },
          ) {
            Icon(
              imageVector = if (saved) Icons.Rounded.CheckCircle else Icons.Rounded.BookmarkBorder,
              contentDescription = "Guardar palabra",
              tint = if (saved) FluentTheme.colors.success else accentColor,
            )
          }
        }
      }
    }

    // Example Card
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onPlayExample),
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          imageVector = if (playingEx) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
          contentDescription = "Oír ejemplo",
          tint = accentColor,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = entry.exampleEn,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = entry.exampleEs,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Tip Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = FluentTheme.colors.correction.copy(alpha = 0.11f),
      border = BorderStroke(1.dp, FluentTheme.colors.correction.copy(alpha = 0.35f)),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Lightbulb,
          contentDescription = null,
          tint = FluentTheme.colors.correction,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = entry.tipEs,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Button(
      onClick = onGoToGame,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
      shape = RoundedCornerShape(16.dp),
    ) {
      Text("Practicar armando las letras")
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LetterScrambleGameCard(
  puzzle: LetterPuzzleState,
  accentColor: Color,
  audioState: AudioEngineState,
  lastWordXp: Int,
  spokenScore: Int?,
  isLastWord: Boolean,
  onPlayClueAudio: (slow: Boolean) -> Unit,
  onTapTile: (Int) -> Unit,
  onTapSlot: (Int) -> Unit,
  onUseHint: () -> Unit,
  onReshuffle: () -> Unit,
  onClear: () -> Unit,
  onPracticePronounce: () -> Unit,
  onNext: () -> Unit,
) {
  val entry = puzzle.entry
  val playingClue = audioState.isSpeaking && audioState.speakingMessageId == "vg-clue-${entry.id}-n"

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Clue Header showing Spanish meaning prominently
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      color = if (puzzle.isSolved) {
        FluentTheme.colors.success.copy(alpha = 0.14f)
      } else {
        MaterialTheme.colorScheme.surface
      },
      border = BorderStroke(
        1.5.dp,
        if (puzzle.isSolved) FluentTheme.colors.success else accentColor.copy(alpha = 0.45f),
      ),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "ARMA LA PALABRA EN INGLÉS (${entry.category})",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (puzzle.isSolved) FluentTheme.colors.success else accentColor,
        )
        Text(
          text = entry.spanish,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "Pronunciación guía: ${entry.ipa}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
          onClick = { onPlayClueAudio(false) },
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
            imageVector = if (playingClue) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (playingClue) "Pausar" else "Pista de audio")
        }
      }
    }

    // Target Letter Slots Grid
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      puzzle.targetChars.forEachIndexed { slotIdx, expectedChar ->
        if (slotIdx in puzzle.fixedIndices) {
          Box(
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .height(44.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (expectedChar == ' ') "  " else expectedChar.toString(),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          val placedId = puzzle.placedTileIds[slotIdx]
          val placedChar = placedId?.let { id -> puzzle.tiles.firstOrNull { it.id == id }?.char }
          val border = when {
            puzzle.isSolved -> FluentTheme.colors.success
            puzzle.lastErrorWord != null -> FluentTheme.colors.recording
            placedChar != null -> accentColor
            else -> FluentTheme.colors.cardBorder
          }
          Surface(
            modifier = Modifier
              .size(width = 38.dp, height = 46.dp)
              .clip(RoundedCornerShape(11.dp))
              .clickable(enabled = placedId != null && !puzzle.isSolved) {
                onTapSlot(slotIdx)
              },
            shape = RoundedCornerShape(11.dp),
            color = when {
              puzzle.isSolved -> FluentTheme.colors.success.copy(alpha = 0.2f)
              placedChar != null -> accentColor.copy(alpha = 0.16f)
              else -> MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(1.5.dp, border),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = placedChar?.toString().orEmpty(),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }

    AnimatedVisibility(visible = puzzle.lastErrorWord != null && !puzzle.isSolved) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FluentTheme.colors.recording.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, FluentTheme.colors.recording.copy(alpha = 0.4f)),
      ) {
        Text(
          text = "Esa combinación aún no coincide. Toca una casilla para sacarla o usa Pista.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(10.dp),
        )
      }
    }

    // Available Scrambled Letter Tiles
    if (!puzzle.isSolved) {
      FlowRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        puzzle.tiles.forEach { tile ->
          Surface(
            modifier = Modifier
              .size(width = 44.dp, height = 50.dp)
              .clip(RoundedCornerShape(13.dp))
              .clickable(enabled = !tile.isUsed) { onTapTile(tile.id) },
            shape = RoundedCornerShape(13.dp),
            color = if (tile.isUsed) {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
              MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
              1.2.dp,
              if (tile.isUsed) Color.Transparent else accentColor.copy(alpha = 0.55f),
            ),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = if (tile.isUsed) "" else tile.char.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onUseHint,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Pista")
        }
        OutlinedButton(
          onClick = onReshuffle,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Mezclar")
        }
        OutlinedButton(
          onClick = onClear,
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("Borrar")
        }
      }
    } else {
      // Solved Feedback & Bonus Voice Check
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = FluentTheme.colors.success.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, FluentTheme.colors.success.copy(alpha = 0.45f)),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "¡Correcto! +$lastWordXp XP",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = FluentTheme.colors.success,
            )
            Text(
              text = entry.word,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Text(
            text = "${entry.exampleEn} — ${entry.exampleEs}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Tip: ${entry.tipEs}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onPracticePronounce,
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(
                imageVector = if (audioState.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (audioState.isListening) "Detener..." else "Decirla en voz alta")
            }
            if (spokenScore != null) {
              Text(
                text = "Voz: $spokenScore%",
                style = MaterialTheme.typography.labelLarge,
                color = if (spokenScore >= 70) FluentTheme.colors.success else FluentTheme.colors.correction,
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    if (puzzle.isSolved) {
      Button(
        onClick = onNext,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text(if (isLastWord) "Ver resumen de ronda" else "Siguiente palabra →")
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
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
