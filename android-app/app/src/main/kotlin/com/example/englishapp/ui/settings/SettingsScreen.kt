package com.example.englishapp.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.englishapp.ConnectionTestState
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.AvatarMood
import com.example.englishapp.model.EnglishAccent
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.UserProgress
import com.example.englishapp.ui.components.AuroraScreenBackground
import com.example.englishapp.ui.components.GlassCard
import com.example.englishapp.ui.components.TutorAvatar
import com.example.englishapp.ui.theme.FluentTheme
import com.example.englishapp.ui.theme.FluentVoiceTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  settings: AppSettings,
  connectionTestState: ConnectionTestState,
  onToggleOfflineDemo: (Boolean) -> Unit,
  onSaveCredentials: (url: String, token: String) -> Unit,
  onTestConnection: (url: String, token: String) -> Unit,
  onSelectAccent: (EnglishAccent) -> Unit,
  onToggleAutoPlay: (Boolean) -> Unit,
  onToggleAutoListen: (Boolean) -> Unit,
  onToggleShowTranslations: (Boolean) -> Unit,
  onToggleFaceToFaceDefault: (Boolean) -> Unit,
  onEvaluateForLevel: (LearnerLevel) -> Unit,
  onSelectTheme: (ThemePreference) -> Unit,
  modifier: Modifier = Modifier,
  progress: UserProgress = UserProgress(),
) {
  var urlDraft by remember(settings.backendUrl) { mutableStateOf(settings.backendUrl) }
  var tokenDraft by remember(settings.appToken) { mutableStateOf(settings.appToken) }
  var showToken by rememberSaveable { mutableStateOf(false) }
  var targetLevelForTest by remember(settings.activeLevel()) { mutableStateOf(settings.activeLevel()) }
  val activeLevel = settings.activeLevel()

  AuroraScreenBackground(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
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
              imageVector = Icons.Rounded.Person,
              contentDescription = null,
              tint = FluentTheme.colors.cyanGlow,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Column {
          Text(
            text = "Perfil y Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Tu cuenta, voz del tutor y conexión con IA",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Profile Summary Card
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        highlighted = true,
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          TutorAvatar(
            character = settings.selectedCharacter,
            mood = AvatarMood.IDLE,
            size = 64.dp,
          )
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = null,
                tint = FluentTheme.colors.cyanGlow,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "Nivel ${activeLevel.labelEs} (${activeLevel.cefr})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text(
              text = "Tutor activo: ${settings.selectedCharacter.displayName} · Acento ${settings.accent.badge}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.LocalFireDepartment,
                  contentDescription = null,
                  tint = FluentTheme.colors.streakFlame,
                  modifier = Modifier.size(15.dp),
                )
                Text(
                  text = "${progress.streakDays} días",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Text(
                text = "${progress.totalXp} XP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = FluentTheme.colors.cyanGlow,
              )
              Text(
                text = "${progress.completedLessonIds.size} lecciones",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

    // Connection Section
    SettingsSectionCard(title = "Conexión e IA") {
      SettingsToggleRow(
        title = "Modo Demo Offline",
        subtitle = "Practica al instante con Manu y Eddy sin servidor ni claves externas.",
        checked = settings.offlineDemoMode,
        onCheckedChange = onToggleOfflineDemo,
      )

      HorizontalDivider(color = FluentTheme.colors.cardBorder)

      OutlinedTextField(
        value = urlDraft,
        onValueChange = {
          urlDraft = it
          onSaveCredentials(it, tokenDraft)
        },
        label = { Text("URL del backend (Render o local)") },
        placeholder = { Text("https://english-tutor-api.onrender.com") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Surface(
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .clickable {
            val localUrl = "http://10.0.2.2:8000"
            urlDraft = localUrl
            onSaveCredentials(localUrl, tokenDraft)
          },
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Text(
          text = "Usar emulador local (http://10.0.2.2:8000)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }

      OutlinedTextField(
        value = tokenDraft,
        onValueChange = {
          tokenDraft = it
          onSaveCredentials(urlDraft, it)
        },
        label = { Text("App Token (X-App-Token)") },
        placeholder = { Text("Opcional si tu servidor no usa APP_TOKEN") },
        singleLine = true,
        visualTransformation = if (showToken) {
          VisualTransformation.None
        } else {
          PasswordVisualTransformation()
        },
        trailingIcon = {
          IconButton(onClick = { showToken = !showToken }) {
            Icon(
              imageVector = if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
              contentDescription = "Mostrar u ocultar token",
            )
          }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Button(
        onClick = { onTestConnection(urlDraft, tokenDraft) },
        enabled = connectionTestState !is ConnectionTestState.Testing,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
      ) {
        if (connectionTestState is ConnectionTestState.Testing) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(
            imageVector = Icons.Rounded.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text("Probar conexión")
      }

      when (connectionTestState) {
        is ConnectionTestState.Success -> {
          ConnectionStatusBanner(
            isSuccess = true,
            message = connectionTestState.message,
          )
        }
        is ConnectionTestState.Error -> {
          ConnectionStatusBanner(
            isSuccess = false,
            message = connectionTestState.message,
          )
        }
        else -> Unit
      }
    }

    // Audio & Voice Preferences
    SettingsSectionCard(title = "Audio y práctica") {
      Text(
        text = "Acento de voz y reconocimiento",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        EnglishAccent.entries.forEach { accent ->
          val selected = settings.accent == accent
          SegmentedOptionChip(
            label = "${accent.badge} · ${accent.labelEs}",
            selected = selected,
            onClick = { onSelectAccent(accent) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      HorizontalDivider(color = FluentTheme.colors.cardBorder)

      SettingsToggleRow(
        title = "Reproducir respuestas automáticamente",
        subtitle = "El tutor lee en voz alta cada nueva respuesta al instante.",
        checked = settings.autoPlayReplies,
        onCheckedChange = onToggleAutoPlay,
      )

      HorizontalDivider(color = FluentTheme.colors.cardBorder)

      SettingsToggleRow(
        title = "Modo manos libres (Auto-Mic)",
        subtitle = "Activa el micrófono automáticamente cuando el tutor termina de hablar.",
        checked = settings.autoListenHandsFree,
        onCheckedChange = onToggleAutoListen,
      )

      HorizontalDivider(color = FluentTheme.colors.cardBorder)

      SettingsToggleRow(
        title = "Mostrar traducción al español",
        subtitle = "Muestra la traducción debajo de cada frase sin tener que tocar el botón.",
        checked = settings.showTranslationsByDefault,
        onCheckedChange = onToggleShowTranslations,
      )

      HorizontalDivider(color = FluentTheme.colors.cardBorder)

      SettingsToggleRow(
        title = "Modo llamada cara a cara por defecto",
        subtitle = "Muestra al personaje en grande al hablar con subtítulos abajo.",
        checked = settings.preferFaceToFaceMode,
        onCheckedChange = onToggleFaceToFaceDefault,
      )
    }

    // Placement / Level Diagnostic Section
    SettingsSectionCard(title = "Nivel de inglés y evaluación") {
      val result = settings.placementResult
      val activeLvl = settings.activeLevel()
      if (result != null) {
        Text(
          text = "Nivel verificado: ${result.verifiedLevel.labelEs} (${result.verifiedLevel.cefr}) · Puntaje ${result.overallScore}/100",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Listening ${result.skills.listening}% · Gramática ${result.skills.grammar}% · Vocabulario ${result.skills.vocabulary}% · Speaking ${result.skills.speaking}%",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(
          text = "Nivel activo: ${activeLvl.labelEs} (${activeLvl.cefr}).",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = "Para cambiar de nivel debes presentar la evaluación adaptativa en el nivel al que deseas pasar:",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        LearnerLevel.entries.forEach { lvl ->
          SegmentedOptionChip(
            label = "${lvl.labelEs} ${lvl.cefr}",
            selected = targetLevelForTest == lvl,
            onClick = { targetLevelForTest = lvl },
          )
        }
      }
      Button(
        onClick = { onEvaluateForLevel(targetLevelForTest) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
      ) {
        Text("Hacer evaluación para ${targetLevelForTest.labelEs} (${targetLevelForTest.cefr})")
      }
    }

    // Appearance Section
    SettingsSectionCard(title = "Apariencia") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ThemePreference.entries.forEach { pref ->
          SegmentedOptionChip(
            label = pref.labelEs,
            selected = settings.themePreference == pref,
            onClick = { onSelectTheme(pref) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  content: @Composable () -> Unit,
) {
  GlassCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = FluentTheme.colors.cyanGlow,
      )
      content()
    }
  }
}

@Composable
private fun SettingsToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}

@Composable
private fun SegmentedOptionChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    color = if (selected) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(
      1.dp,
      if (selected) primary else FluentTheme.colors.cardBorder,
    ),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
    )
  }
}

@Composable
private fun ConnectionStatusBanner(
  isSuccess: Boolean,
  message: String,
) {
  val tint = if (isSuccess) FluentTheme.colors.success else FluentTheme.colors.recording
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = tint.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.4f)),
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
  FluentVoiceTheme(themePreference = ThemePreference.DARK) {
    SettingsScreen(
      settings = AppSettings(
        offlineDemoMode = false,
        backendUrl = "https://english-tutor-api.onrender.com",
      ),
      connectionTestState = ConnectionTestState.Success("Conectado (142 ms) · v2.0 · IA lista"),
      onToggleOfflineDemo = {},
      onSaveCredentials = { _, _ -> },
      onTestConnection = { _, _ -> },
      onSelectAccent = {},
      onToggleAutoPlay = {},
      onToggleAutoListen = {},
      onToggleShowTranslations = {},
      onToggleFaceToFaceDefault = {},
      onEvaluateForLevel = {},
      onSelectTheme = {},
    )
  }
}
