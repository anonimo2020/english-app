package com.example.englishapp.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.DailyActivity
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PlacementResult
import com.example.englishapp.model.SavedPhrase
import com.example.englishapp.model.SkillScores
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.UserProgress
import com.example.englishapp.ui.components.AuroraScreenBackground
import com.example.englishapp.ui.components.GlassCard
import com.example.englishapp.ui.components.GlowingCircleIconButton
import com.example.englishapp.ui.components.GlowingCircularGauge
import com.example.englishapp.ui.components.GradientProgressBar
import com.example.englishapp.ui.theme.FluentTheme
import com.example.englishapp.ui.theme.FluentVoiceTheme

private enum class ProgressTab(val label: String) {
  SUMMARY("Resumen"),
  SKILLS("Habilidades"),
  HISTORY("Historial"),
}

@Composable
fun ProgressScreen(
  progress: UserProgress,
  savedPhrases: List<SavedPhrase>,
  audioState: AudioEngineState,
  onPlayPhrase: (SavedPhrase, Boolean) -> Unit,
  onRemovePhrase: (String) -> Unit,
  modifier: Modifier = Modifier,
  settings: AppSettings = AppSettings(),
) {
  var selectedTab by rememberSaveable { mutableStateOf(ProgressTab.SUMMARY) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  val activeLevel = settings.activeLevel()

  val filteredPhrases = remember(savedPhrases, searchQuery) {
    val q = searchQuery.trim()
    if (q.isEmpty()) {
      savedPhrases
    } else {
      savedPhrases.filter {
        it.english.contains(q, ignoreCase = true) ||
          it.spanish.contains(q, ignoreCase = true) ||
          it.note.contains(q, ignoreCase = true)
      }
    }
  }

  val practiceMinutes = remember(progress) {
    (progress.totalSpokenTurns * 2 + progress.sessionsCompleted * 6 + progress.completedLessonIds.size * 5)
      .coerceAtLeast(progress.todayXp / 4)
  }
  val pronunciationPct = settings.placementResult?.skills?.speaking
    ?: ((progress.todayXp * 100) / progress.dailyGoalXp.coerceAtLeast(1)).coerceIn(0, 100)

  AuroraScreenBackground(modifier = modifier) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Top Header matching Panel 4
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
              modifier = Modifier.size(42.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.BarChart,
                  contentDescription = null,
                  tint = FluentTheme.colors.cyanGlow,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column {
              Text(
                text = "Progreso",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
              )
              Text(
                text = "Tu avance en inglés · Nivel ${activeLevel.cefr}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          GlowingCircleIconButton(
            icon = Icons.Rounded.CalendarMonth,
            contentDescription = "Ver resumen semanal",
            onClick = { selectedTab = ProgressTab.SUMMARY },
            size = 42.dp,
          )
        }
      }

      // 3-Segment Glass Switcher: Resumen | Habilidades | Historial
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, FluentTheme.colors.cardBorder),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            ProgressTab.entries.forEach { tab ->
              val selected = tab == selectedTab
              val pillBrush = if (selected) {
                Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF6366F1)))
              } else {
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
              }
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(999.dp))
                  .background(pillBrush)
                  .clickable { selectedTab = tab }
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = tab.label,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      when (selectedTab) {
        ProgressTab.SUMMARY -> {
          // 2x2 Bento Stat Grid matching Panel 4
          item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                // Minutos de práctica
                GlassCard(
                  modifier = Modifier
                    .weight(1f)
                    .height(142.dp),
                  shape = RoundedCornerShape(22.dp),
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = FluentTheme.colors.cyanGlow,
                        modifier = Modifier.size(18.dp),
                      )
                      Text(
                        text = "Práctica",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Text(
                      text = "$practiceMinutes min",
                      style = MaterialTheme.typography.headlineMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = "▲ +${progress.todayXp} XP hoy",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = FluentTheme.colors.success,
                    )
                  }
                }

                // Pronunciación Ring Gauge
                GlassCard(
                  modifier = Modifier
                    .weight(1f)
                    .height(142.dp),
                  shape = RoundedCornerShape(22.dp),
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(17.dp),
                      )
                      Text(
                        text = "Pronunciación",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    GlowingCircularGauge(
                      percentage = pronunciationPct,
                      size = 76.dp,
                      strokeWidth = 7.dp,
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                // Vocabulario aprendido
                GlassCard(
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(22.dp),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FluentTheme.colors.cyanGlow.copy(alpha = 0.16f)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = FluentTheme.colors.cyanGlow,
                        modifier = Modifier.size(21.dp),
                      )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = "Vocabulario",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Text(
                        text = "${progress.masteredVocabIds.size} palabras",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                  }
                }

                // Racha actual
                GlassCard(
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(22.dp),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FluentTheme.colors.streakFlame.copy(alpha = 0.16f)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = FluentTheme.colors.streakFlame,
                        modifier = Modifier.size(22.dp),
                      )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = "Racha actual",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Text(
                        text = "${progress.streakDays} días",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                  }
                }
              }
            }
          }

          // Weekly Bar Chart Card matching Panel 4
          item {
            WeeklyActivityCard(progress = progress)
          }

          // Motivational Footer Card matching Panel 4
          item {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(22.dp),
              color = Color.Transparent,
              border = BorderStroke(1.dp, Color(0x6660A5FA)),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(
                    Brush.horizontalGradient(
                      colors = listOf(
                        Color(0xFF1D4ED8).copy(alpha = 0.55f),
                        Color(0xFF6D28D9).copy(alpha = 0.65f),
                      ),
                    ),
                  )
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = Color(0xFF312E81),
                  border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                  modifier = Modifier.size(48.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.EmojiEvents,
                      contentDescription = null,
                      tint = Color(0xFFFBBF24),
                      modifier = Modifier.size(26.dp),
                    )
                  }
                }
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Text(
                    text = "¡Vas muy bien!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                  )
                  Text(
                    text = "Llevas ${progress.totalSpokenTurns} turnos hablados y ${progress.completedLessonIds.size} lecciones en ${activeLevel.cefr}. ¡Sigue practicando!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE0E7FF),
                  )
                }
              }
            }
          }
        }

        ProgressTab.SKILLS -> {
          item {
            val skills = settings.placementResult?.skills ?: SkillScores(
              listening = 65,
              grammar = 65,
              vocabulary = 65,
              speaking = 65,
            )
            GlassCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(22.dp),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Text(
                  text = "Habilidades evaluadas (${activeLevel.labelEs} · ${activeLevel.cefr})",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                SkillProgressRow("Comprensión auditiva", skills.listening)
                SkillProgressRow("Gramática en uso", skills.grammar)
                SkillProgressRow("Vocabulario y contexto", skills.vocabulary)
                SkillProgressRow("Pronunciación y fluidez", skills.speaking)
              }
            }
          }

          item {
            GlassCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(22.dp),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "Logros acumulados",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                MetricLine("Lecciones completadas", "${progress.completedLessonIds.size}")
                MetricLine("Escenarios practicados", "${progress.completedScenarioIds.size}")
                MetricLine("Palabras dominadas", "${progress.masteredVocabIds.size}")
                MetricLine("Ejercicios de voz", "${progress.masteredDrillIds.size}")
                MetricLine("XP acumulado", "${progress.totalXp} XP")
              }
            }
          }
        }

        ProgressTab.HISTORY -> {
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Cuaderno de frases",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                  text = "${savedPhrases.size} guardadas",
                  style = MaterialTheme.typography.labelMedium,
                  color = FluentTheme.colors.cyanGlow,
                )
              }

              OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar frase en inglés o español...") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                  )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          if (filteredPhrases.isEmpty()) {
            item {
              GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                  )
                  Text(
                    text = if (savedPhrases.isEmpty()) {
                      "Tu cuaderno aún está vacío. Toca 'Guardar' en cualquier frase o lección."
                    } else {
                      "No encontramos frases que coincidan con \"$searchQuery\"."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            }
          } else {
            items(filteredPhrases, key = { it.id }) { phrase ->
              val playingNormal = audioState.isSpeaking &&
                audioState.speakingMessageId == "phrase-${phrase.id}"
              val playingSlow = audioState.isSpeaking &&
                audioState.speakingMessageId == "phrase-slow-${phrase.id}"

              SavedPhraseCard(
                phrase = phrase,
                playingNormal = playingNormal,
                playingSlow = playingSlow,
                onPlayNormal = { onPlayPhrase(phrase, false) },
                onPlaySlow = { onPlayPhrase(phrase, true) },
                onDelete = { onRemovePhrase(phrase.id) },
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun SkillProgressRow(label: String, score: Int) {
  Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "$score%",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = FluentTheme.colors.cyanGlow,
      )
    }
    GradientProgressBar(progress = score / 100f, height = 7.dp)
  }
}

@Composable
private fun MetricLine(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun WeeklyActivityCard(progress: UserProgress) {
  val goalFraction = (progress.todayXp.toFloat() / progress.dailyGoalXp.coerceAtLeast(1)).coerceIn(0f, 1f)
  val cyan = FluentTheme.colors.cyanGlow
  val violet = FluentTheme.colors.violetGlow
  val trackColor = if (FluentTheme.colors.isDark) Color(0xFF222748) else MaterialTheme.colorScheme.surfaceVariant
  val history = progress.weeklyHistory

  GlassCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.BarChart,
          contentDescription = null,
          tint = violet,
          modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Actividad semanal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "XP de práctica por día · Meta diaria: ${progress.todayXp}/${progress.dailyGoalXp} XP",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      GradientProgressBar(progress = goalFraction, height = 6.dp)

      if (history.isNotEmpty()) {
        val maxXp = history.maxOf { it.xp }.coerceAtLeast(progress.dailyGoalXp)
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        ) {
          val count = history.size
          val slotW = size.width / count.toFloat()
          val barW = (slotW * 0.44f).coerceIn(16f, 36f)
          val usableH = size.height - 8f

          history.forEachIndexed { idx, day ->
            val barX = idx * slotW + (slotW - barW) / 2f
            drawRoundRect(
              color = trackColor,
              topLeft = Offset(barX, 4f),
              size = Size(barW, usableH),
              cornerRadius = CornerRadius(barW / 3f, barW / 3f),
            )
            val ratio = (day.xp.toFloat() / maxXp.toFloat()).coerceIn(0f, 1f)
            if (ratio > 0f) {
              val fillH = (usableH * ratio).coerceAtLeast(barW * 0.8f)
              val topY = 4f + (usableH - fillH)
              val brush = if (idx == history.lastIndex) {
                Brush.verticalGradient(listOf(cyan, Color(0xFF3B82F6)), startY = topY, endY = topY + fillH)
              } else {
                Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFF5B21B6)), startY = topY, endY = topY + fillH)
              }
              drawRoundRect(
                brush = brush,
                topLeft = Offset(barX, topY),
                size = Size(barW, fillH),
                cornerRadius = CornerRadius(barW / 3f, barW / 3f),
              )
            }
          }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
          history.forEachIndexed { idx, day ->
            Column(
              modifier = Modifier.weight(1f),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = day.dayLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (idx == history.lastIndex) FontWeight.Bold else FontWeight.Normal,
                color = if (idx == history.lastIndex) cyan else MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${day.xp}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SavedPhraseCard(
  phrase: SavedPhrase,
  playingNormal: Boolean,
  playingSlow: Boolean,
  onPlayNormal: () -> Unit,
  onPlaySlow: () -> Unit,
  onDelete: () -> Unit,
) {
  val accent = MaterialTheme.colorScheme.primary
  GlassCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    highlighted = playingNormal || playingSlow,
    glowColor = accent,
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
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = accent.copy(alpha = 0.16f),
        ) {
          Text(
            text = phrase.source,
            style = MaterialTheme.typography.labelSmall,
            color = FluentTheme.colors.cyanGlow,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
          )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Eliminar frase",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Text(
        text = phrase.english,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (phrase.spanish.isNotBlank()) {
        Text(
          text = phrase.spanish,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (phrase.note.isNotBlank()) {
        Text(
          text = phrase.note,
          style = MaterialTheme.typography.bodySmall,
          color = FluentTheme.colors.correction,
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PhraseAudioPill(
          icon = if (playingNormal) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
          label = if (playingNormal) "Detener" else "Escuchar",
          active = playingNormal,
          onClick = onPlayNormal,
        )
        PhraseAudioPill(
          icon = if (playingSlow) Icons.Rounded.Stop else Icons.Rounded.SlowMotionVideo,
          label = if (playingSlow) "Detener" else "Lento 0.65x",
          active = playingSlow,
          onClick = onPlaySlow,
        )
      }
    }
  }
}

@Composable
private fun PhraseAudioPill(
  icon: ImageVector,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
) {
  val primary = MaterialTheme.colorScheme.primary
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(999.dp),
    color = if (active) primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (active) FluentTheme.colors.cyanGlow else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun ProgressScreenPreview() {
  FluentVoiceTheme(themePreference = ThemePreference.DARK) {
    ProgressScreen(
      settings = AppSettings(
        verifiedLevel = LearnerLevel.JUNIOR,
        selectedLevel = LearnerLevel.JUNIOR,
        placementResult = PlacementResult(
          estimatedLevel = LearnerLevel.JUNIOR,
          verifiedLevel = LearnerLevel.JUNIOR,
          overallScore = 82,
          skills = SkillScores(80, 78, 84, 82),
          recommendedFocusEs = "A2",
        ),
      ),
      progress = UserProgress(
        totalXp = 1850,
        streakDays = 7,
        todayXp = 75,
        dailyGoalXp = 100,
        totalSpokenTurns = 32,
        sessionsCompleted = 7,
        weeklyHistory = listOf(
          DailyActivity("2026-11-15", "Lun", 45, 3),
          DailyActivity("2026-11-16", "Mar", 80, 6),
          DailyActivity("2026-11-17", "Mié", 95, 7),
          DailyActivity("2026-11-18", "Jue", 40, 3),
          DailyActivity("2026-11-19", "Vie", 85, 6),
          DailyActivity("2026-11-20", "Sáb", 95, 7),
          DailyActivity("2026-11-21", "Dom", 120, 9),
        ),
      ),
      savedPhrases = listOf(
        SavedPhrase(
          id = "1",
          english = "Could I have a medium iced latte, please?",
          spanish = "¿Me podría dar un latte frío mediano, por favor?",
          note = "Muy útil en cafeterías.",
          source = "Cafetería",
        ),
      ),
      audioState = AudioEngineState(),
      onPlayPhrase = { _, _ -> },
      onRemovePhrase = {},
    )
  }
}
