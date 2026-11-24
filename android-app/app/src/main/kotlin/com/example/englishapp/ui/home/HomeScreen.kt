package com.example.englishapp.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.data.CurriculumEngine
import com.example.englishapp.data.LessonsCatalog
import com.example.englishapp.data.RecommendedScenarioItem
import com.example.englishapp.data.VocabularyCatalog
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PlacementResult
import com.example.englishapp.model.SkillScores
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.TutorCharacter
import com.example.englishapp.model.UserProgress
import com.example.englishapp.ui.components.AuroraScreenBackground
import com.example.englishapp.ui.components.FluentVoiceBrandHeader
import com.example.englishapp.ui.components.GlassCard
import com.example.englishapp.ui.components.GradientPillButton
import com.example.englishapp.ui.components.GradientProgressBar
import com.example.englishapp.ui.components.StatusModeChip
import com.example.englishapp.ui.components.portraitDrawableRes
import com.example.englishapp.ui.theme.FluentTheme
import com.example.englishapp.ui.theme.FluentVoiceTheme

@Composable
fun HomeScreen(
  settings: AppSettings,
  progress: UserProgress,
  audioState: AudioEngineState,
  onSelectCharacter: (TutorCharacter) -> Unit,
  onPreviewCharacter: (TutorCharacter) -> Unit,
  onStartConversation: (scenarioId: String) -> Unit,
  onOpenExplore: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenVocabGame: () -> Unit = {},
  onOpenLearningHub: () -> Unit = {},
) {
  val scrollState = rememberScrollState()
  val activeCharacter = settings.selectedCharacter
  val ctaColor = FluentTheme.colors.accentFor(activeCharacter)
  val curriculum = remember(settings, progress) {
    CurriculumEngine.buildCurriculum(settings, progress)
  }
  val activeLevel = curriculum.activeLevel
  val levelUnitsCount = remember(activeLevel) { LessonsCatalog.unitsForLevel(activeLevel).size }
  val levelLessonsCount = remember(activeLevel) { LessonsCatalog.forLevel(activeLevel).size }
  val levelVocabCount = remember(activeLevel) { VocabularyCatalog.forLevel(activeLevel).size }
  val dailyGoalFraction = (progress.todayXp.toFloat() / progress.dailyGoalXp.coerceAtLeast(1)).coerceIn(0f, 1f)

  val primaryScenarioId = curriculum.recommendedForLevel.firstOrNull { !it.isCompleted }?.scenario?.id
    ?: curriculum.recommendedForLevel.firstOrNull()?.scenario?.id
    ?: "free_chat"

  AuroraScreenBackground(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 18.dp)
        .padding(top = 14.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Brand Header matching mockup
      FluentVoiceBrandHeader(
        onActionClick = onOpenSettings,
        actionIcon = Icons.Rounded.NotificationsNone,
        actionContentDescription = "Abrir perfil y ajustes",
      )

      // 2. Greeting + Verified Level Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.WavingHand,
              contentDescription = null,
              tint = Color(0xFFFBBF24),
              modifier = Modifier.size(22.dp),
            )
          }
          Column {
            Text(
              text = "¡Hola!",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "¿Listo para practicar?",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          StatusModeChip(
            offlineDemoMode = settings.offlineDemoMode,
            hasBackendUrl = settings.backendUrl.isNotBlank(),
          )
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .clickable(onClick = onOpenSettings),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = null,
                tint = FluentTheme.colors.cyanGlow,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = "Nivel ${activeLevel.cefr}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }

      // 3. Two-Column KPI Cards: Streak + XP/Level
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        GlassCard(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(20.dp),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(FluentTheme.colors.streakFlame.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = FluentTheme.colors.streakFlame,
                modifier = Modifier.size(26.dp),
              )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Racha",
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

        GlassCard(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(20.dp),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                border = BorderStroke(1.dp, FluentTheme.colors.cyanGlow.copy(alpha = 0.50f)),
                modifier = Modifier.size(34.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FluentTheme.colors.cyanGlow,
                  )
                }
              }
              Column {
                Text(
                  text = "${progress.totalXp} XP",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Nivel ${activeLevel.cefr} · ${activeLevel.labelEs}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            GradientProgressBar(
              progress = dailyGoalFraction,
              height = 6.dp,
            )
          }
        }
      }

      // 4. Hero "Practica Speaking" Card with Photorealistic Tutor Cards
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        highlighted = true,
        glowColor = MaterialTheme.colorScheme.primary,
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.Mic,
                  contentDescription = null,
                  tint = FluentTheme.colors.cyanGlow,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column {
              Text(
                text = "Practica Speaking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Conversa en inglés con tu tutor IA",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          // Side-by-Side Photorealistic Tutor Portrait Cards
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            listOf(TutorCharacter.EDDY, TutorCharacter.MANU).forEach { character ->
              val isSpeakingPreview = audioState.isSpeaking &&
                audioState.speakingMessageId == "preview-${character.id}"
              TutorPortraitSelectorCard(
                character = character,
                selected = character == activeCharacter,
                isPreviewPlaying = isSpeakingPreview,
                onSelect = { onSelectCharacter(character) },
                onPreviewVoice = { onPreviewCharacter(character) },
                modifier = Modifier.weight(1f),
              )
            }
          }

          GradientPillButton(
            text = "Iniciar conversación con ${activeCharacter.displayName}",
            icon = Icons.Rounded.PlayArrow,
            onClick = { onStartConversation(primaryScenarioId) },
            height = 50.dp,
          )
        }
      }

      // 5. Weekly Milestone Card ("Tu progreso semanal")
      WeeklyMilestonesCard(
        settings = settings,
        progress = progress,
      )

      // 6. Quick Learning Shortcuts strictly for Verified Level
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        GlassCard(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(20.dp),
          onClick = onOpenExplore,
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
            Text(
              text = "Curso ${activeLevel.cefr}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "$levelUnitsCount unidades · $levelLessonsCount lecciones",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        GlassCard(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(20.dp),
          onClick = onOpenVocabGame,
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FluentTheme.colors.cyanGlow.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.SportsEsports,
                contentDescription = null,
                tint = FluentTheme.colors.cyanGlow,
                modifier = Modifier.size(20.dp),
              )
            }
            Text(
              text = "Juego de Letras",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "$levelVocabCount palabras ${activeLevel.cefr} + ES",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // 6b. Learning Hub Studio Entry Banner (A1–C2 Expanded Database)
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        highlighted = true,
        glowColor = FluentTheme.colors.cyanGlow,
        onClick = onOpenLearningHub,
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(FluentTheme.colors.cyanGlow.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Public,
              contentDescription = null,
              tint = FluentTheme.colors.cyanGlow,
              modifier = Modifier.size(24.dp),
            )
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = "Learning Hub A1–C2",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Lecturas, dictados, gramática, escritura, rutas adaptativas y fuentes abiertas",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = FluentTheme.colors.cyanGlow,
          )
        }
      }

      // 7. Strict Active-Level Conversation Topics Grid
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Temas de conversación ${activeLevel.cefr}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "${curriculum.recommendedForLevel.size} temas",
            style = MaterialTheme.typography.labelMedium,
            color = FluentTheme.colors.cyanGlow,
          )
        }

        val recRows = curriculum.recommendedForLevel.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          recRows.forEach { pair ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              pair.forEach { item ->
                RecommendedScenarioCard(
                  item = item,
                  accentColor = ctaColor,
                  onClick = { onStartConversation(item.scenario.id) },
                  modifier = Modifier.weight(1f),
                )
              }
              if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TutorPortraitSelectorCard(
  character: TutorCharacter,
  selected: Boolean,
  isPreviewPlaying: Boolean,
  onSelect: () -> Unit,
  onPreviewVoice: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor by animateColorAsState(
    targetValue = if (selected) MaterialTheme.colorScheme.primary else FluentTheme.colors.cardBorder,
    label = "tutorCardBorder",
  )
  val flagText = if (character == TutorCharacter.EDDY) "US" else "UK"
  val shape = RoundedCornerShape(20.dp)

  Box(
    modifier = modifier
      .height(170.dp)
      .clip(shape)
      .border(
        width = if (selected) 2.dp else 1.dp,
        color = borderColor,
        shape = shape,
      )
      .clickable(onClick = onSelect),
  ) {
    Image(
      painter = painterResource(id = character.portraitDrawableRes()),
      contentDescription = character.displayName,
      contentScale = ContentScale.Crop,
      alignment = BiasAlignment(0f, -0.22f),
      modifier = Modifier.fillMaxSize(),
    )

    // Bottom dark gradient scrim for crisp name & badge readability
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colorStops = arrayOf(
              0.0f to Color(0x33070918),
              0.52f to Color.Transparent,
              1.0f to Color(0xE0070918),
            ),
          ),
        ),
    )

    // Top bar: voice preview button on left + selection ring/check on right
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier = Modifier
          .size(30.dp)
          .clip(CircleShape)
          .clickable(onClick = onPreviewVoice),
        shape = CircleShape,
        color = Color(0xAA0B0E20),
        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isPreviewPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = "Escuchar voz de ${character.displayName}",
            tint = if (isPreviewPlaying) FluentTheme.colors.cyanGlow else Color.White,
            modifier = Modifier.size(16.dp),
          )
        }
      }

      if (selected) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = "Seleccionado",
              tint = Color.White,
              modifier = Modifier.size(15.dp),
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .size(24.dp)
            .border(1.5.dp, Color(0x88FFFFFF), CircleShape),
        )
      }
    }

    // Bottom info overlay: online dot + name + accent badge
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(Color(0xFF34D399)),
        )
        Text(
          text = character.displayName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xAA0B0E20),
        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
      ) {
        Text(
          text = flagText,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFE2E8F0),
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
    }
  }
}

@Composable
private fun WeeklyMilestonesCard(
  settings: AppSettings,
  progress: UserProgress,
) {
  val placement = settings.placementResult
  val milestones = listOf(
    "Vocabulario" to (progress.masteredVocabIds.isNotEmpty() || (placement?.skills?.vocabulary ?: 0) >= 70),
    "Gramática" to (progress.completedLessonIds.isNotEmpty() || (placement?.skills?.grammar ?: 0) >= 70),
    "Speaking" to (progress.totalSpokenTurns > 0),
    "Escucha" to (progress.masteredDrillIds.isNotEmpty() || progress.sessionsCompleted > 0),
    "Lectura" to (progress.completedScenarioIds.isNotEmpty() || progress.todayXp >= progress.dailyGoalXp),
  )
  val completedCount = milestones.count { it.second }

  GlassCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Tu progreso semanal",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Text(
          text = "$completedCount / ${milestones.size} objetivos",
          style = MaterialTheme.typography.labelMedium,
          color = FluentTheme.colors.cyanGlow,
        )
      }

      GradientProgressBar(
        progress = completedCount.toFloat() / milestones.size.toFloat(),
        height = 8.dp,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        milestones.forEach { (label, done) ->
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                  if (done) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                  } else {
                    Color.Transparent
                  },
                )
                .border(
                  width = 1.dp,
                  color = if (done) {
                    Color(0xFFDDD6FE)
                  } else {
                    FluentTheme.colors.cardBorder
                  },
                  shape = CircleShape,
                ),
              contentAlignment = Alignment.Center,
            ) {
              if (done) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "$label completado",
                  tint = Color.White,
                  modifier = Modifier.size(15.dp),
                )
              }
            }
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RecommendedScenarioCard(
  item: RecommendedScenarioItem,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scenario = item.scenario
  val icon = scenarioIcon(scenario.iconName)
  val badgeTint = if (item.isCompleted) FluentTheme.colors.success else accentColor

  GlassCard(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    highlighted = !item.isCompleted,
    glowColor = badgeTint,
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(badgeTint.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = badgeTint,
            modifier = Modifier.size(19.dp),
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (item.isCompleted) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = "Completado",
              tint = FluentTheme.colors.success,
              modifier = Modifier.size(15.dp),
            )
          }
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = badgeTint.copy(alpha = 0.16f),
          ) {
            Text(
              text = scenario.recommendedLevel.cefr,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = badgeTint,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = scenario.titleEs,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = scenario.subtitleEs,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = item.reasonTag,
          style = MaterialTheme.typography.labelSmall,
          color = badgeTint,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
          contentDescription = null,
          tint = badgeTint,
          modifier = Modifier.size(14.dp),
        )
      }
    }
  }
}

private fun scenarioIcon(name: String): ImageVector = when (name) {
  "waving" -> Icons.Rounded.WavingHand
  "coffee" -> Icons.Rounded.Coffee
  "schedule" -> Icons.Rounded.Schedule
  "shopping" -> Icons.Rounded.ShoppingBag
  "family" -> Icons.Rounded.Diversity3
  "grocery" -> Icons.Rounded.ShoppingCart
  "weather" -> Icons.Rounded.Cloud
  "hotel" -> Icons.Rounded.Hotel
  "flight" -> Icons.Rounded.FlightTakeoff
  "restaurant" -> Icons.Rounded.Restaurant
  "map" -> Icons.Rounded.Map
  "weekend" -> Icons.Rounded.Event
  "apartment" -> Icons.Rounded.HomeWork
  "pharmacy" -> Icons.Rounded.LocalPharmacy
  "camera" -> Icons.Rounded.PhotoCamera
  "work" -> Icons.Rounded.WorkOutline
  "medical" -> Icons.Rounded.LocalHospital
  "support" -> Icons.Rounded.SupportAgent
  "movie" -> Icons.Rounded.Movie
  "fitness" -> Icons.Rounded.FitnessCenter
  "handshake" -> Icons.Rounded.Handshake
  "networking" -> Icons.Rounded.AutoStories
  "briefcase" -> Icons.Rounded.WorkOutline
  "groups" -> Icons.Rounded.Groups
  "trending" -> Icons.AutoMirrored.Rounded.TrendingUp
  "psychology" -> Icons.Rounded.Psychology
  "campaign" -> Icons.Rounded.Campaign
  "crisis" -> Icons.Rounded.WarningAmber
  "executive" -> Icons.Rounded.WorkOutline
  "eco" -> Icons.Rounded.Public
  "globe" -> Icons.Rounded.Public
  else -> Icons.Rounded.Forum
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPersonalizedPreview() {
  FluentVoiceTheme(themePreference = ThemePreference.DARK) {
    HomeScreen(
      settings = AppSettings(
        hasCompletedOnboarding = true,
        verifiedLevel = LearnerLevel.NOVATO,
        selectedLevel = LearnerLevel.NOVATO,
        placementResult = PlacementResult(
          estimatedLevel = LearnerLevel.NOVATO,
          verifiedLevel = LearnerLevel.NOVATO,
          overallScore = 76,
          skills = SkillScores(listening = 78, grammar = 72, vocabulary = 79, speaking = 74),
          recommendedFocusEs = "Plan A1",
        ),
      ),
      progress = UserProgress(
        totalXp = 340,
        streakDays = 5,
        todayXp = 65,
        dailyGoalXp = 100,
        totalSpokenTurns = 24,
      ),
      audioState = AudioEngineState(),
      onSelectCharacter = {},
      onPreviewCharacter = {},
      onStartConversation = {},
      onOpenExplore = {},
      onOpenSettings = {},
    )
  }
}
