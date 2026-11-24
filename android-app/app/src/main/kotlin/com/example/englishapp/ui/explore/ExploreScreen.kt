package com.example.englishapp.ui.explore

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.englishapp.DictionaryUiState
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.data.AudioStory
import com.example.englishapp.data.DictionaryEntryResult
import com.example.englishapp.data.FreeWebContentClient
import com.example.englishapp.data.FreeWebPortal
import com.example.englishapp.data.LearningLibrary
import com.example.englishapp.data.LessonsCatalog
import com.example.englishapp.data.PronunciationDrill
import com.example.englishapp.data.SpeakingAnalysis
import com.example.englishapp.data.SpeakingCoachEngine
import com.example.englishapp.data.StoryVocab
import com.example.englishapp.data.VocabularyCatalog
import com.example.englishapp.data.curriculum.CourseUnit
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.UserProgress
import com.example.englishapp.ui.components.AuroraScreenBackground
import com.example.englishapp.ui.components.GlassCard
import com.example.englishapp.ui.components.GradientProgressBar
import com.example.englishapp.ui.components.LuminousOpenBookGraphic
import com.example.englishapp.ui.components.SpeakingMasteryCard
import com.example.englishapp.ui.theme.FluentTheme
import com.example.englishapp.ui.theme.FluentVoiceTheme

enum class ExploreTab(val icon: ImageVector) {
  LESSONS(Icons.Rounded.School),
  VOCABULARY(Icons.Rounded.Translate),
  DRILLS(Icons.Rounded.RecordVoiceOver),
  STORIES(Icons.Rounded.AutoStories),
  DICT_WEB(Icons.AutoMirrored.Rounded.MenuBook),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
  settings: AppSettings,
  progress: UserProgress,
  audioState: AudioEngineState,
  dictionaryState: DictionaryUiState,
  onLookupWord: (String) -> Unit,
  onPlayText: (text: String, tag: String, slow: Boolean) -> Unit,
  onPlaySpanishText: (text: String, tag: String) -> Unit,
  onPlayDictionaryAudio: (DictionaryEntryResult, slow: Boolean) -> Unit,
  onSavePhrase: (english: String, spanish: String, note: String, source: String) -> Unit,
  onOpenLesson: (lessonId: String) -> Unit,
  onOpenVocabGame: (categoryFilter: String?, startWordId: String?, gameOnly: Boolean) -> Unit,
  onMasterDrill: (drillId: String) -> Unit,
  onCompleteStoryQuiz: (storyId: String) -> Unit,
  onStartSpeechDrill: (silenceMs: Long, onResult: (String) -> Unit) -> Unit,
  onStopSpeechDrill: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenLearningHub: () -> Unit = {},
) {
  val activeLevel = settings.activeLevel()
  val accentColor = FluentTheme.colors.accentFor(settings.selectedCharacter)

  val levelUnits = remember(activeLevel) { LessonsCatalog.unitsForLevel(activeLevel) }
  val levelVocab = remember(activeLevel) { VocabularyCatalog.forLevel(activeLevel) }
  val levelDrills = remember(activeLevel) { LearningLibrary.drillsForLevel(activeLevel) }
  val levelStories = remember(activeLevel) { LearningLibrary.storiesForLevel(activeLevel) }

  var activeTab by rememberSaveable { mutableStateOf(ExploreTab.LESSONS) }
  var openStory by remember { mutableStateOf<AudioStory?>(null) }
  var completedStories by remember { mutableStateOf(emptySet<String>()) }

  fun labelForTab(tab: ExploreTab): String = when (tab) {
    ExploreTab.LESSONS -> "Unidades (${levelUnits.size})"
    ExploreTab.VOCABULARY -> "Vocabulario (${levelVocab.size})"
    ExploreTab.DRILLS -> "Voz (${levelDrills.size})"
    ExploreTab.STORIES -> "Historias (${levelStories.size})"
    ExploreTab.DICT_WEB -> "Diccionario"
  }

  AuroraScreenBackground(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(top = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
              modifier = Modifier.size(42.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                  contentDescription = null,
                  tint = FluentTheme.colors.cyanGlow,
                  modifier = Modifier.size(21.dp),
                )
              }
            }
            Column {
              Text(
                text = "Lecciones",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
              )
              Text(
                text = "Aprende paso a paso · ${activeLevel.labelEs}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
          ) {
            Text(
              text = activeLevel.cefr,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = FluentTheme.colors.cyanGlow,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
          }
        }

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenLearningHub),
          shape = RoundedCornerShape(16.dp),
          color = FluentTheme.colors.cyanGlow.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, FluentTheme.colors.cyanGlow.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Rounded.Public, contentDescription = null, tint = FluentTheme.colors.cyanGlow, modifier = Modifier.size(18.dp))
              Text(
                text = "Learning Hub: +900 lecturas, dictados, simulacros y APIs abiertas",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = FluentTheme.colors.cyanGlow, modifier = Modifier.size(16.dp))
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ExploreTab.entries.forEach { tab ->
            val selected = tab == activeTab
            val chipBg = if (selected) {
              Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF6366F1)))
            } else {
              Brush.horizontalGradient(
                listOf(
                  MaterialTheme.colorScheme.surface,
                  MaterialTheme.colorScheme.surface,
                ),
              )
            }
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { activeTab = tab },
              shape = RoundedCornerShape(14.dp),
              color = Color.Transparent,
              border = BorderStroke(
                1.dp,
                if (selected) Color(0xFFDDD6FE) else FluentTheme.colors.cardBorder,
              ),
            ) {
              Row(
                modifier = Modifier
                  .background(chipBg)
                  .padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = tab.icon,
                  contentDescription = null,
                  tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = labelForTab(tab),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }

    when (activeTab) {
      ExploreTab.LESSONS -> {
        UnitsCurriculumList(
          activeLevel = activeLevel,
          units = levelUnits,
          completedLessonIds = progress.completedLessonIds,
          accentColor = accentColor,
          onOpenLesson = onOpenLesson,
        )
      }

      ExploreTab.VOCABULARY -> {
        VocabularyBankSection(
          activeLevel = activeLevel,
          levelVocab = levelVocab,
          masteredVocabIds = progress.masteredVocabIds,
          audioState = audioState,
          accentColor = accentColor,
          onPlayText = onPlayText,
          onSavePhrase = onSavePhrase,
          onOpenVocabGame = onOpenVocabGame,
        )
      }

      ExploreTab.DRILLS -> {
        PronunciationDrillsSection(
          activeLevel = activeLevel,
          drills = levelDrills,
          masteredDrillIds = progress.masteredDrillIds,
          audioState = audioState,
          accentColor = accentColor,
          onPlayText = onPlayText,
          onStartSpeechDrill = onStartSpeechDrill,
          onStopSpeechDrill = onStopSpeechDrill,
          onMasterDrill = onMasterDrill,
          onSavePhrase = onSavePhrase,
        )
      }

      ExploreTab.STORIES -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Text(
              text = "Lecturas interactivas con audio para nivel ${activeLevel.cefr}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          items(levelStories, key = { it.id }) { story ->
            StoryListCard(
              story = story,
              isDone = story.id in completedStories,
              accentColor = accentColor,
              onOpen = { openStory = story },
            )
          }
          item { Spacer(modifier = Modifier.height(20.dp)) }
        }
      }

      ExploreTab.DICT_WEB -> {
        DictionaryAndWebSection(
          activeLevel = activeLevel,
          audioState = audioState,
          dictionaryState = dictionaryState,
          accentColor = accentColor,
          onLookupWord = onLookupWord,
          onPlayDictionaryAudio = onPlayDictionaryAudio,
          onSavePhrase = onSavePhrase,
        )
      }
    }
    }
  }

  openStory?.let { story ->
    ModalBottomSheet(
      onDismissRequest = { openStory = null },
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      SteppedStoryReaderSheet(
        story = story,
        audioState = audioState,
        accentColor = accentColor,
        alreadyAwarded = story.id in completedStories,
        onPlayStory = { slow ->
          onPlayText(story.bodyEn, "story-${story.id}", slow)
        },
        onPlayVocab = { word ->
          onPlayText(word, "sv-$word", false)
        },
        onSaveVocab = { vocab ->
          onSavePhrase(vocab.word, vocab.spanish, vocab.phonetic, story.titleEs)
        },
        onPassQuiz = {
          if (story.id !in completedStories) {
            completedStories = completedStories + story.id
            onCompleteStoryQuiz(story.id)
          }
        },
      )
    }
  }
}

@Composable
private fun UnitsCurriculumList(
  activeLevel: LearnerLevel,
  units: List<CourseUnit>,
  completedLessonIds: Set<String>,
  accentColor: Color,
  onOpenLesson: (String) -> Unit,
) {
  var expandedUnitId by rememberSaveable(activeLevel.id) {
    val firstIncompleteUnit = units.firstOrNull { u ->
      u.lessons.any { it.id !in completedLessonIds }
    } ?: units.firstOrNull()
    mutableStateOf(firstIncompleteUnit?.id)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      val totalLessons = units.sumOf { it.lessons.size }
      val doneCount = units.sumOf { u -> u.lessons.count { it.id in completedLessonIds } }
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        highlighted = true,
        glowColor = MaterialTheme.colorScheme.primary,
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "Explora, aprende y practica",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Lecciones diseñadas para tu nivel ${activeLevel.labelEs} (${activeLevel.cefr}).",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            GradientProgressBar(
              progress = if (totalLessons == 0) 0f else doneCount.toFloat() / totalLessons.toFloat(),
              height = 6.dp,
            )
            Text(
              text = "$doneCount / $totalLessons lecciones completadas",
              style = MaterialTheme.typography.labelSmall,
              color = FluentTheme.colors.cyanGlow,
            )
          }
          LuminousOpenBookGraphic(
            modifier = Modifier.size(88.dp),
          )
        }
      }
    }

    items(units, key = { it.id }) { unit ->
      val expanded = expandedUnitId == unit.id
      val unitDoneCount = unit.lessons.count { it.id in completedLessonIds }
      val unitTotal = unit.lessons.size.coerceAtLeast(1)
      val unitFraction = unitDoneCount.toFloat() / unitTotal.toFloat()
      val unitPct = (unitFraction * 100).toInt()
      val unitAllDone = unitDoneCount == unit.lessons.size

      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        highlighted = expanded || unitAllDone,
        glowColor = if (unitAllDone) FluentTheme.colors.success else MaterialTheme.colorScheme.primary,
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { expandedUnitId = if (expanded) null else unit.id },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (unitAllDone) {
                FluentTheme.colors.success.copy(alpha = 0.18f)
              } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
              },
              border = BorderStroke(
                1.dp,
                if (unitAllDone) {
                  FluentTheme.colors.success.copy(alpha = 0.55f)
                } else {
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                },
              ),
              modifier = Modifier.size(46.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "U${unit.orderInLevel}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (unitAllDone) FluentTheme.colors.success else FluentTheme.colors.cyanGlow,
                )
              }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "${unit.titleEs} (${unit.titleEn})",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                ) {
                  Text(
                    text = activeLevel.cefr,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = FluentTheme.colors.cyanGlow,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  )
                }
              }
              Text(
                text = unit.descriptionEs,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = FluentTheme.colors.correction.copy(alpha = 0.12f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Extension,
                contentDescription = null,
                tint = FluentTheme.colors.correction,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = unit.specializedFocusEs,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { expandedUnitId = if (expanded) null else unit.id },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            GradientProgressBar(
              progress = unitFraction,
              height = 6.dp,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = if (unitAllDone) "✓ Completada" else "$unitPct%",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (unitAllDone) FluentTheme.colors.success else FluentTheme.colors.cyanGlow,
            )
            Icon(
              imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
              contentDescription = "Expandir unidad",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }

          AnimatedVisibility(visible = expanded) {
            Column(
              modifier = Modifier.padding(top = 4.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              unit.lessons.forEach { lesson ->
                val done = lesson.id in completedLessonIds
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenLesson(lesson.id) },
                  shape = RoundedCornerShape(16.dp),
                  color = if (done) {
                    FluentTheme.colors.success.copy(alpha = 0.12f)
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  },
                  border = BorderStroke(
                    1.dp,
                    if (done) FluentTheme.colors.success.copy(alpha = 0.45f) else FluentTheme.colors.cardBorder,
                  ),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Box(
                      modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                          if (done) FluentTheme.colors.success else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        ),
                      contentAlignment = Alignment.Center,
                    ) {
                      if (done) {
                        Icon(
                          imageVector = Icons.Rounded.CheckCircle,
                          contentDescription = null,
                          tint = Color.White,
                          modifier = Modifier.size(18.dp),
                        )
                      } else {
                        Text(
                          text = "${lesson.orderInUnit}",
                          style = MaterialTheme.typography.labelLarge,
                          color = FluentTheme.colors.cyanGlow,
                        )
                      }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = lesson.titleEs,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = "${lesson.items.size} pasos + ${lesson.items.size} mini-tests + voz · ${lesson.durationMin} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }

                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                      contentDescription = "Iniciar lección",
                      tint = accentColor,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(20.dp)) }
  }
}

@Composable
private fun VocabularyBankSection(
  activeLevel: LearnerLevel,
  levelVocab: List<com.example.englishapp.data.VocabEntry>,
  masteredVocabIds: Set<String>,
  audioState: AudioEngineState,
  accentColor: Color,
  onPlayText: (String, String, Boolean) -> Unit,
  onSavePhrase: (String, String, String, String) -> Unit,
  onOpenVocabGame: (categoryFilter: String?, startWordId: String?, gameOnly: Boolean) -> Unit,
) {
  var search by rememberSaveable(activeLevel.id) { mutableStateOf("") }
  var selectedCategory by rememberSaveable(activeLevel.id) { mutableStateOf("Todas") }

  val categories = remember(levelVocab) {
    listOf("Todas") + levelVocab.map { it.category }.distinct()
  }
  val filtered = remember(levelVocab, selectedCategory, search) {
    val q = search.trim()
    levelVocab.filter { item ->
      val catOk = selectedCategory == "Todas" || item.category == selectedCategory
      val qOk = q.isEmpty() ||
        item.word.contains(q, ignoreCase = true) ||
        item.spanish.contains(q, ignoreCase = true)
      catOk && qOk
    }
  }
  val masteredHere = levelVocab.count { it.id in masteredVocabIds }
  val activeCategoryParam = if (selectedCategory == "Todas") null else selectedCategory

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Launch Cards for Interactive Vocabulary Studio & Letter Game
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenVocabGame(activeCategoryParam, null, false) },
          shape = RoundedCornerShape(18.dp),
          color = accentColor.copy(alpha = 0.16f),
          border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.5f)),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.MenuBook,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(24.dp),
            )
            Text(
              text = "Aprende + Letras",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Estudia significado en español y arma la palabra",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenVocabGame(activeCategoryParam, null, true) },
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
          border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.SportsEsports,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp),
            )
            Text(
              text = "Juego de Letras",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Reto rápido de ortografía con fichas y rachas",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    item {
      OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        placeholder = { Text("Buscar en las ${levelVocab.size} palabras de ${activeLevel.cefr}...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      )
    }

    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        categories.forEach { cat ->
          FilterPill(
            label = cat,
            isSelected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
          )
        }
      }
    }

    item {
      Text(
        text = "Vocabulario NGSL/NAWL ${activeLevel.cefr} · $masteredHere/${levelVocab.size} dominadas (toca una tarjeta para jugar)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    items(filtered, key = { it.id }) { item ->
      val isMastered = item.id in masteredVocabIds
      val playing = audioState.isSpeaking && audioState.speakingMessageId == "voc-${item.id}"
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .clickable { onOpenVocabGame(activeCategoryParam, item.id, false) },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
          1.dp,
          if (isMastered) FluentTheme.colors.success.copy(alpha = 0.55f) else FluentTheme.colors.cardBorder,
        ),
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
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${item.word}  ·  ${item.ipa}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = item.spanish,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              if (isMastered) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = "Dominada",
                  tint = FluentTheme.colors.success,
                  modifier = Modifier.size(18.dp),
                )
              }
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Text(
                  text = item.category,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
              }
            }
          }

          Text(
            text = "\"${item.exampleEn}\" — ${item.exampleEs}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Tip: ${item.tipEs}",
            style = MaterialTheme.typography.labelSmall,
            color = FluentTheme.colors.correction,
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { onPlayText(item.exampleEn, "voc-${item.id}", false) },
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(
                imageVector = if (playing) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Oír")
            }
            Button(
              onClick = { onOpenVocabGame(activeCategoryParam, item.id, false) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Rounded.SportsEsports,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Practicar & Letras")
            }
            IconButton(
              onClick = {
                onSavePhrase(item.word, item.spanish, "${item.ipa} · ${item.exampleEn}", "Vocabulario ${item.level.cefr}")
              },
            ) {
              Icon(
                imageVector = Icons.Rounded.BookmarkBorder,
                contentDescription = "Guardar",
                tint = FluentTheme.colors.success,
              )
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(20.dp)) }
  }
}

@Composable
private fun FilterPill(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  val primary = MaterialTheme.colorScheme.primary
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(999.dp),
    color = if (isSelected) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, if (isSelected) primary else FluentTheme.colors.cardBorder),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
    )
  }
}

@Composable
private fun PronunciationDrillsSection(
  activeLevel: LearnerLevel,
  drills: List<PronunciationDrill>,
  masteredDrillIds: Set<String>,
  audioState: AudioEngineState,
  accentColor: Color,
  onPlayText: (text: String, tag: String, slow: Boolean) -> Unit,
  onStartSpeechDrill: (silenceMs: Long, onResult: (String) -> Unit) -> Unit,
  onStopSpeechDrill: () -> Unit,
  onMasterDrill: (drillId: String) -> Unit,
  onSavePhrase: (english: String, spanish: String, note: String, source: String) -> Unit,
) {
  val context = LocalContext.current
  val drillAnalyses = remember { mutableStateMapOf<String, SpeakingAnalysis>() }
  val drillAttempts = remember { mutableStateMapOf<String, Int>() }
  var expandedDrillId by rememberSaveable(activeLevel.id) { mutableStateOf(drills.firstOrNull()?.id) }
  var pendingRecordDrill by remember { mutableStateOf<PronunciationDrill?>(null) }

  fun handleSpoken(drill: PronunciationDrill, spoken: String) {
    val nextAttempt = (drillAttempts[drill.id] ?: 0) + 1
    drillAttempts[drill.id] = nextAttempt
    val prev = drillAnalyses[drill.id]?.overallScore
    val analysis = SpeakingCoachEngine.analyzePronunciation(
      expected = drill.targetPhrase,
      spoken = spoken,
      attemptNumber = nextAttempt,
      previousScore = prev,
    )
    drillAnalyses[drill.id] = analysis
    pendingRecordDrill = null
    if (analysis.passed && drill.id !in masteredDrillIds) {
      onMasterDrill(drill.id)
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    val drill = pendingRecordDrill
    if (granted && drill != null) {
      onStartSpeechDrill(activeLevel.sttSilenceMs) { spoken ->
        handleSpoken(drill, spoken)
      }
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
        text = "Entrenamiento de voz específico para nivel ${activeLevel.cefr}. Obtén 75%+ para dominar cada reto.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    items(drills, key = { it.id }) { drill ->
      val isExpanded = expandedDrillId == drill.id
      val isListeningThis = audioState.isListening && pendingRecordDrill?.id == drill.id
      val isMastered = drill.id in masteredDrillIds || drillAnalyses[drill.id]?.passed == true
      val tag = "drill-${drill.id}"
      val playing = audioState.isSpeaking && audioState.speakingMessageId == tag

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
          1.dp,
          if (isMastered) FluentTheme.colors.success.copy(alpha = 0.55f) else FluentTheme.colors.cardBorder,
        ),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { expandedDrillId = if (isExpanded) null else drill.id },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = accentColor.copy(alpha = 0.14f),
              ) {
                Text(
                  text = drill.category,
                  style = MaterialTheme.typography.labelSmall,
                  color = accentColor,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
              }
              Text(
                text = drill.ipa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            if (isMastered) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Dominado",
                tint = FluentTheme.colors.success,
                modifier = Modifier.size(19.dp),
              )
            }
          }

          Text(
            text = drill.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = drill.spanishTip,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { onPlayText(drill.targetPhrase, tag, false) },
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(
                imageVector = if (playing) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Oír frase")
            }
            Button(
              onClick = {
                expandedDrillId = if (isExpanded) null else drill.id
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (isExpanded) "Ocultar coach" else "Practicar voz")
            }
            IconButton(
              onClick = {
                onSavePhrase(drill.targetPhrase, drill.spanishTip, drill.ipa, drill.title)
              },
            ) {
              Icon(
                imageVector = Icons.Rounded.BookmarkBorder,
                contentDescription = "Guardar",
                tint = FluentTheme.colors.success,
              )
            }
          }

          AnimatedVisibility(visible = isExpanded) {
            SpeakingMasteryCard(
              targetPhrase = drill.targetPhrase,
              spanishHint = drill.exampleSentence,
              tipEs = drill.spanishTip,
              analysis = drillAnalyses[drill.id],
              isListeningThis = isListeningThis,
              audioState = audioState,
              onListenNormal = { onPlayText(drill.targetPhrase, "${tag}-c", false) },
              onListenSlow = { onPlayText(drill.targetPhrase, "${tag}-cs", true) },
              onListenWord = { word -> onPlayText(word, "${tag}-w-$word", true) },
              onToggleRecord = {
                if (audioState.isListening) {
                  onStopSpeechDrill()
                } else {
                  pendingRecordDrill = drill
                  val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                  ) == PackageManager.PERMISSION_GRANTED
                  if (granted) {
                    onStartSpeechDrill(activeLevel.sttSilenceMs) { spoken ->
                      handleSpoken(drill, spoken)
                    }
                  } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                }
              },
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(20.dp)) }
  }
}

@Composable
private fun StoryListCard(
  story: AudioStory,
  isDone: Boolean,
  accentColor: Color,
  onOpen: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onOpen),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      1.dp,
      if (isDone) FluentTheme.colors.success.copy(alpha = 0.55f) else accentColor.copy(alpha = 0.45f),
    ),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = accentColor.copy(alpha = 0.14f),
        ) {
          Text(
            text = story.durationLabel,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
          )
        }
        if (isDone) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "Completada",
            tint = FluentTheme.colors.success,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Text(
        text = story.titleEs,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = story.titleEn,
        style = MaterialTheme.typography.bodySmall,
        color = accentColor,
      )
      Text(
        text = story.bodyEn.take(98) + "...",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SteppedStoryReaderSheet(
  story: AudioStory,
  audioState: AudioEngineState,
  accentColor: Color,
  alreadyAwarded: Boolean,
  onPlayStory: (slow: Boolean) -> Unit,
  onPlayVocab: (String) -> Unit,
  onSaveVocab: (StoryVocab) -> Unit,
  onPassQuiz: () -> Unit,
) {
  var storyStep by rememberSaveable(story.id) { mutableIntStateOf(0) } // 0=Audio+Texto, 1=Vocabulario, 2=Quiz
  var showSpanish by rememberSaveable(story.id) { mutableStateOf(false) }
  val selectedAnswers = remember(story.id) { mutableStateMapOf<Int, Int>() }
  val isPlayingThis = audioState.isSpeaking && audioState.speakingMessageId == "story-${story.id}"
  val activeRange = if (isPlayingThis) audioState.spokenCharRange else null

  val storyAnnotated = remember(story.bodyEn, activeRange, accentColor) {
    buildAnnotatedString {
      if (activeRange == null) {
        append(story.bodyEn)
      } else {
        val start = activeRange.first.coerceIn(0, story.bodyEn.length)
        val end = (activeRange.last + 1).coerceIn(start, story.bodyEn.length)
        append(story.bodyEn.substring(0, start))
        withStyle(SpanStyle(background = accentColor.copy(alpha = 0.28f), fontWeight = FontWeight.Bold)) {
          append(story.bodyEn.substring(start, end))
        }
        append(story.bodyEn.substring(end))
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .padding(bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Column {
      Text(
        text = story.titleEs,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "${story.titleEn} · ${story.durationLabel}",
        style = MaterialTheme.typography.labelMedium,
        color = accentColor,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf("1. Historia", "2. Palabras", "3. Mini-Quiz").forEachIndexed { idx, label ->
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { storyStep = idx },
          shape = RoundedCornerShape(12.dp),
          color = if (storyStep == idx) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(
            1.dp,
            if (storyStep == idx) accentColor else Color.Transparent,
          ),
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (storyStep == idx) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
          )
        }
      }
    }

    AnimatedContent(targetState = storyStep, label = "storyStepAnim") { step ->
      when (step) {
        0 -> {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { onPlayStory(false) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = if (isPlayingThis) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isPlayingThis) "Pausar" else "Escuchar")
              }
              OutlinedButton(onClick = { onPlayStory(true) }, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.SlowMotionVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("0.65x")
              }
              OutlinedButton(onClick = { showSpanish = !showSpanish }, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
              }
            }
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(storyAnnotated, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                AnimatedVisibility(visible = showSpanish) {
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = FluentTheme.colors.cardBorder)
                    Text(story.bodyEs, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }
            Button(
              onClick = { storyStep = 1 },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Ver palabras clave →")
            }
          }
        }

        1 -> {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            story.vocab.forEach { vocab ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onPlayVocab(vocab.word) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "${vocab.word} · ${vocab.phonetic}",
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = vocab.spanish,
                      style = MaterialTheme.typography.bodySmall,
                      color = accentColor,
                    )
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onPlayVocab(vocab.word) }) {
                      Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Oír", tint = accentColor)
                    }
                    IconButton(onClick = { onSaveVocab(vocab) }) {
                      Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Guardar", tint = FluentTheme.colors.success)
                    }
                  }
                }
              }
            }
            Button(
              onClick = { storyStep = 2 },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Ir al Mini-Quiz (+20 XP) →")
            }
          }
        }

        else -> {
          Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            story.questions.forEachIndexed { qIdx, q ->
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${qIdx + 1}. ${q.questionEs}", style = MaterialTheme.typography.titleSmall)
                q.options.forEachIndexed { optIdx, opt ->
                  val chosen = selectedAnswers[qIdx]
                  val isChosen = chosen == optIdx
                  val isCorrect = optIdx == q.correctIndex
                  val bg = when {
                    chosen == null -> MaterialTheme.colorScheme.surfaceVariant
                    isCorrect -> FluentTheme.colors.success.copy(alpha = 0.16f)
                    isChosen -> FluentTheme.colors.recording.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                  }
                  Surface(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .clickable {
                        selectedAnswers[qIdx] = optIdx
                        val allCorrect = story.questions.indices.all { i ->
                          selectedAnswers[i] == story.questions[i].correctIndex
                        }
                        if (allCorrect) onPassQuiz()
                      },
                    shape = RoundedCornerShape(12.dp),
                    color = bg,
                  ) {
                    Text(opt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                  }
                }
              }
            }
            val allCorrectNow = story.questions.indices.all { i ->
              selectedAnswers[i] == story.questions[i].correctIndex
            }
            if (allCorrectNow || alreadyAwarded) {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = FluentTheme.colors.success.copy(alpha = 0.14f),
              ) {
                Text(
                  text = "¡Excelente comprensión! Ganaste +20 XP.",
                  style = MaterialTheme.typography.titleSmall,
                  color = FluentTheme.colors.success,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryAndWebSection(
  activeLevel: LearnerLevel,
  audioState: AudioEngineState,
  dictionaryState: DictionaryUiState,
  accentColor: Color,
  onLookupWord: (String) -> Unit,
  onPlayDictionaryAudio: (DictionaryEntryResult, slow: Boolean) -> Unit,
  onSavePhrase: (english: String, spanish: String, note: String, source: String) -> Unit,
) {
  val context = LocalContext.current
  var query by rememberSaveable { mutableStateOf("") }
  val quickWords = remember(activeLevel) { LearningLibrary.wordSuggestionsFor(activeLevel) }
  val portals = remember(activeLevel) { LearningLibrary.portalsForLevel(activeLevel) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("Buscar palabra (ej: fluent, schedule)...") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onLookupWord(query) }),
          modifier = Modifier.weight(1f),
        )
        Button(
          onClick = { onLookupWord(query) },
          enabled = query.isNotBlank() && !dictionaryState.isLoading,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.height(54.dp),
        ) {
          Text("Buscar")
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Sugeridas para tu nivel ${activeLevel.cefr}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          quickWords.forEach { w ->
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable {
                  query = w
                  onLookupWord(w)
                },
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
            ) {
              Text(
                text = w,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              )
            }
          }
        }
      }
    }

    if (dictionaryState.isLoading) {
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CircularProgressIndicator(modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text("Consultando diccionario en línea...", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }

    dictionaryState.error?.let { err ->
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = FluentTheme.colors.recording.copy(alpha = 0.14f),
        ) {
          Text(err, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
        }
      }
    }

    dictionaryState.result?.let { entry ->
      item {
        val playingNative = audioState.isSpeaking &&
          audioState.speakingMessageId == "dict-${entry.word}"
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
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(entry.word, style = MaterialTheme.typography.headlineSmall)
                if (entry.phonetic.isNotBlank()) {
                  Text(entry.phonetic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (!entry.spanishTranslation.isNullOrBlank()) {
                  Text("ES: ${entry.spanishTranslation}", style = MaterialTheme.typography.titleSmall, color = FluentTheme.colors.eddyAccent)
                }
              }
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Text(
                  text = if (entry.isFromLiveWeb) "Web en vivo" else "Offline",
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { onPlayDictionaryAudio(entry, false) },
                shape = RoundedCornerShape(12.dp),
              ) {
                Icon(
                  imageVector = if (playingNative) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (entry.audioUrl != null) "Voz nativa" else "Escuchar")
              }
              OutlinedButton(onClick = { onPlayDictionaryAudio(entry, true) }, shape = RoundedCornerShape(12.dp)) {
                Text("0.65x")
              }
              OutlinedButton(
                onClick = {
                  onSavePhrase(
                    entry.word,
                    entry.spanishTranslation ?: "",
                    entry.meanings.firstOrNull()?.definitionEn ?: "",
                    "Diccionario",
                  )
                },
                shape = RoundedCornerShape(12.dp),
              ) {
                Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Guardar", modifier = Modifier.size(18.dp))
              }
            }

            entry.meanings.forEach { m ->
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  if (m.partOfSpeech.isNotBlank()) {
                    Text(m.partOfSpeech.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                  }
                  Text(m.definitionEn, style = MaterialTheme.typography.bodyMedium)
                  if (!m.exampleEn.isNullOrBlank()) {
                    Text("\"${m.exampleEn}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }

            if (entry.relatedWords.isNotEmpty()) {
              FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entry.relatedWords.forEach { rel ->
                  TextButton(
                    onClick = {
                      query = rel
                      onLookupWord(rel)
                    },
                  ) {
                    Text(rel)
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      HorizontalDivider(color = FluentTheme.colors.cardBorder)
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Recursos gratuitos recomendados para ${activeLevel.cefr} (${portals.size})",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    items(portals, key = { it.url }) { portal ->
      PortalCard(
        portal = portal,
        accentColor = accentColor,
        onOpen = {
          runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portal.url))
            context.startActivity(intent)
          }
        },
      )
    }

    item { Spacer(modifier = Modifier.height(20.dp)) }
  }
}

@Composable
private fun PortalCard(
  portal: FreeWebPortal,
  accentColor: Color,
  onOpen: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onOpen),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Rounded.Public, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(portal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
          Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(
              text = portal.cefrBadge,
              style = MaterialTheme.typography.labelSmall,
              color = accentColor,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }
        Text(portal.categoryEs, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(portal.taglineEs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
        contentDescription = "Abrir en navegador",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun ExploreScreenPreview() {
  FluentVoiceTheme(themePreference = ThemePreference.DARK) {
    ExploreScreen(
      settings = AppSettings(verifiedLevel = LearnerLevel.INTERMEDIO_ALTO),
      progress = UserProgress(),
      audioState = AudioEngineState(),
      dictionaryState = DictionaryUiState(
        result = FreeWebContentClient.offlineFallback["fluent"],
      ),
      onLookupWord = {},
      onPlayText = { _, _, _ -> },
      onPlaySpanishText = { _, _ -> },
      onPlayDictionaryAudio = { _, _ -> },
      onSavePhrase = { _, _, _, _ -> },
      onOpenLesson = {},
      onOpenVocabGame = { _, _, _ -> },
      onMasterDrill = {},
      onCompleteStoryQuiz = {},
      onStartSpeechDrill = { _, _ -> },
      onStopSpeechDrill = {},
    )
  }
}
