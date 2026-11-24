package com.example.englishapp.ui.hub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.englishapp.audio.AudioEngineState
import com.example.englishapp.data.PlacementEngine
import com.example.englishapp.data.SpeakingCoachEngine
import com.example.englishapp.edu.EduAttribution
import com.example.englishapp.edu.EduLicense
import com.example.englishapp.edu.EduPayloadDetail
import com.example.englishapp.edu.EduQuizQuestionPayload
import com.example.englishapp.edu.EduResource
import com.example.englishapp.edu.EduSecuritySanitizer
import com.example.englishapp.edu.EduSkillCatalog
import com.example.englishapp.edu.EduSyncClient
import com.example.englishapp.model.AppSettings
import com.example.englishapp.ui.components.AuroraScreenBackground
import com.example.englishapp.ui.components.GlassCard
import com.example.englishapp.ui.components.GlowingCircleIconButton
import com.example.englishapp.ui.components.GradientPillButton
import com.example.englishapp.ui.components.SpeakingMasteryCard
import com.example.englishapp.ui.theme.FluentTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EduResourceDetailScreen(
  resource: EduResource?,
  license: EduLicense?,
  attribution: EduAttribution?,
  isFavorite: Boolean,
  settings: AppSettings,
  audioState: AudioEngineState,
  onBack: () -> Unit,
  onRecordOpened: (EduResource, Boolean) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onCompleteOutcome: (EduResource, scorePct: Float, timeSpentSec: Int, errorTags: List<String>) -> Unit,
  onPlayText: (text: String, tag: String, slow: Boolean) -> Unit,
  onPlaySpanishText: (text: String, tag: String) -> Unit,
  onSavePhrase: (english: String, spanish: String, note: String, source: String) -> Unit,
  onStartSpeechCheck: (silenceMs: Long, onResult: (String) -> Unit) -> Unit,
  onStopSpeechCheck: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val cyan = FluentTheme.colors.cyanGlow

  if (resource == null) {
    AuroraScreenBackground(modifier = modifier) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlowingCircleIconButton(
          icon = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Volver",
          onClick = onBack,
        )
        Text("Recurso no encontrado.", style = MaterialTheme.typography.titleMedium)
      }
    }
    return
  }

  val payload = remember(resource.id, resource.payloadJson) {
    runCatching { EduSyncClient.json.decodeFromString<EduPayloadDetail>(resource.payloadJson) }
      .getOrDefault(EduPayloadDetail())
  }
  val openedAtMs = remember(resource.id) { System.currentTimeMillis() }

  LaunchedEffect(resource.id) {
    onRecordOpened(resource, resource.sourceType == "external_link")
  }

  var completedNotice by remember(resource.id) { mutableStateOf<String?>(null) }
  fun markDone(scorePct: Float, errorTags: List<String> = emptyList()) {
    val elapsed = ((System.currentTimeMillis() - openedAtMs) / 1000L).toInt().coerceIn(5, 1800)
    onCompleteOutcome(resource, scorePct, elapsed, errorTags)
    completedNotice = if (scorePct >= 60f) {
      "¡Avance guardado! Puntuación: ${scorePct.toInt()}% · Progreso actualizado"
    } else {
      "Intento registrado (${scorePct.toInt()}%). El recomendador reforzará este tema."
    }
  }

  AuroraScreenBackground(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        GlowingCircleIconButton(
          icon = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Volver",
          onClick = onBack,
          size = 40.dp,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
          Text(
            text = resource.titleEs,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
          )
          Text(
            text = "${resource.cefrLevel} · ${EduSkillCatalog.labelFor(resource.primarySkill)} · ${resource.estimatedDurationMin} min",
            style = MaterialTheme.typography.labelSmall,
            color = cyan,
          )
        }
        GlowingCircleIconButton(
          icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
          contentDescription = "Favorito",
          onClick = { onToggleFavorite(resource.id) },
          size = 40.dp,
          iconTint = if (isFavorite) Color(0xFFF472B6) else MaterialTheme.colorScheme.onSurface,
        )
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp)
          .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        completedNotice?.let { notice ->
          GlassCard(modifier = Modifier.fillMaxWidth(), highlighted = true, glowColor = Color(0xFF34D399)) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF34D399))
              Text(notice, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
          }
        }

        // Metadata, License & Attribution Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (resource.learningObjectiveEs.isNotBlank()) {
              Text(
                text = "Objetivo: ${resource.learningObjectiveEs}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            val classNote = if (resource.cefrClassificationSource == "auto_estimated") {
              "Clasificación CEFR automática estimada (${(resource.cefrConfidence * 100).toInt()}% confianza · en revisión)"
            } else {
              "Nivel ${resource.cefrLevel} (${resource.validationStatus})"
            }
            Text(
              text = classNote,
              style = MaterialTheme.typography.labelSmall,
              color = cyan,
            )
            Text(
              text = "Fuente: ${resource.author} · Licencia: ${license?.spdxCode ?: resource.licenseId}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val attrText = attribution?.attributionText ?: payload.attribution
            if (attrText.isNotBlank()) {
              Text(
                text = attrText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            val targetUrl = resource.originalUrl.ifBlank { payload.pageUrl.ifBlank { payload.bookUrl.ifBlank { payload.url } } }
            if (targetUrl.isNotBlank() && EduSecuritySanitizer.isSafeExternalUrl(targetUrl)) {
              OutlinedButton(
                onClick = {
                  onRecordOpened(resource, true)
                  runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))) }
                },
                shape = RoundedCornerShape(12.dp),
              ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir recurso original en sitio oficial")
              }
            }
          }
        }

        // Specialized interactive workspace based on contentType
        when (resource.contentType) {
          "reading", "story" -> ReadingWorkspace(
            resource = resource,
            payload = payload,
            audioState = audioState,
            onPlayText = onPlayText,
            onPlaySpanishText = onPlaySpanishText,
            onSavePhrase = onSavePhrase,
            onSubmitScore = { score, errs -> markDone(score, errs) },
          )

          "listening", "audio_clip" -> ListeningWorkspace(
            resource = resource,
            payload = payload,
            audioState = audioState,
            onPlayText = onPlayText,
            onPlaySpanishText = onPlaySpanishText,
            onOpenAudioUrl = { audioUrl ->
              if (EduSecuritySanitizer.isSafeExternalUrl(audioUrl)) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(audioUrl))) }
              }
            },
            onSubmitScore = { score, errs -> markDone(score, errs) },
          )

          "grammar" -> GrammarWorkspace(
            resource = resource,
            payload = payload,
            onPlayText = onPlayText,
            onSubmitScore = { score, errs -> markDone(score, errs) },
          )

          "writing" -> WritingWorkspace(
            resource = resource,
            payload = payload,
            onPlayText = onPlayText,
            onSubmitScore = { score -> markDone(score, emptyList()) },
          )

          "quiz", "word_game" -> MultiQuestionQuizRunner(
            titleEs = if (resource.contentType == "word_game") "Reto de palabras" else "Evaluación del simulacro",
            questions = payload.questions,
            audioTagPrefix = resource.id,
            onPlayText = onPlayText,
            onGraded = { score, errs -> markDone(score, errs) },
          )

          "vocabulary", "pronunciation", "sentence_pair" -> VocabularyAndSpeechWorkspace(
            resource = resource,
            payload = payload,
            settings = settings,
            audioState = audioState,
            onPlayText = onPlayText,
            onPlaySpanishText = onPlaySpanishText,
            onSavePhrase = onSavePhrase,
            onStartSpeechCheck = onStartSpeechCheck,
            onStopSpeechCheck = onStopSpeechCheck,
            onSubmitScore = { score -> markDone(score, emptyList()) },
          )

          "scenario" -> VocabularyAndSpeechWorkspace(
            resource = resource,
            payload = payload.copy(
              word = resource.title,
              spanish = payload.openingEs.ifBlank { resource.description },
              exampleEn = payload.starterSuggestions.firstOrNull().orEmpty().ifBlank { payload.openingEn },
              tipEs = payload.coachIntroEs.ifBlank { payload.openingEn },
              collocations = payload.starterSuggestions,
            ),
            settings = settings,
            audioState = audioState,
            onPlayText = onPlayText,
            onPlaySpanishText = onPlaySpanishText,
            onSavePhrase = onSavePhrase,
            onStartSpeechCheck = onStartSpeechCheck,
            onStopSpeechCheck = onStopSpeechCheck,
            onSubmitScore = { score -> markDone(score, emptyList()) },
          )

          else -> {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(resource.description, style = MaterialTheme.typography.bodyMedium)
                GradientPillButton(
                  text = "Marcar como revisado y ganar XP",
                  onClick = { markDone(90f) },
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
private fun ReadingWorkspace(
  resource: EduResource,
  payload: EduPayloadDetail,
  audioState: AudioEngineState,
  onPlayText: (String, String, Boolean) -> Unit,
  onPlaySpanishText: (String, String) -> Unit,
  onSavePhrase: (String, String, String, String) -> Unit,
  onSubmitScore: (Float, List<String>) -> Unit,
) {
  val bodyEn = payload.bodyEn.ifBlank { resource.description }
  val bodyEs = payload.bodyEs.ifBlank { payload.summaryEs }
  var showSpanish by remember(resource.id) { mutableStateOf(false) }
  val playTag = "read-${resource.id}"
  val isPlaying = audioState.isSpeaking && audioState.speakingMessageId == playTag

  GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Texto en inglés", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row {
          IconButton(onClick = { onPlayText(bodyEn, playTag, false) }) {
            Icon(
              imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
              contentDescription = "Escuchar lectura",
              tint = FluentTheme.colors.cyanGlow,
            )
          }
          IconButton(onClick = { onPlayText(bodyEn, "$playTag-slow", true) }) {
            Icon(Icons.Rounded.SlowMotionVideo, contentDescription = "Escuchar lento")
          }
          if (bodyEs.isNotBlank()) {
            IconButton(onClick = { showSpanish = !showSpanish }) {
              Icon(Icons.Rounded.Translate, contentDescription = "Mostrar español")
            }
          }
          IconButton(onClick = { onSavePhrase(bodyEn.take(180), bodyEs.take(180), resource.titleEs, "Learning Hub") }) {
            Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Guardar")
          }
        }
      }

      Text(text = bodyEn, style = MaterialTheme.typography.bodyLarge)

      if (showSpanish && bodyEs.isNotBlank()) {
        HorizontalDivider()
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Traducción / resumen en español", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
          IconButton(onClick = { onPlaySpanishText(bodyEs, "read-es-${resource.id}") }) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Oír en español")
          }
        }
        Text(text = bodyEs, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      if (payload.keyVocabulary.isNotEmpty()) {
        HorizontalDivider()
        Text("Vocabulario clave", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          payload.keyVocabulary.forEach { kv ->
            val meaning = kv.es.ifBlank { kv.spanish }
            Surface(
              modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                onPlayText(kv.word, "kv-${kv.word}", false)
              },
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            ) {
              Text(
                text = "${kv.word} → $meaning",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
      }
    }
  }

  if (payload.questions.isNotEmpty()) {
    MultiQuestionQuizRunner(
      titleEs = "Comprensión lectora",
      questions = payload.questions,
      audioTagPrefix = resource.id,
      onPlayText = onPlayText,
      onGraded = onSubmitScore,
    )
  } else {
    GradientPillButton(
      text = "Completar lectura (${resource.cefrLevel})",
      onClick = { onSubmitScore(90f, emptyList()) },
    )
  }
}

@Composable
private fun ListeningWorkspace(
  resource: EduResource,
  payload: EduPayloadDetail,
  audioState: AudioEngineState,
  onPlayText: (String, String, Boolean) -> Unit,
  onPlaySpanishText: (String, String) -> Unit,
  onOpenAudioUrl: (String) -> Unit,
  onSubmitScore: (Float, List<String>) -> Unit,
) {
  val transcript = payload.transcriptEn.ifBlank { payload.sentence.ifBlank { resource.description } }
  val dictationTarget = payload.dictationTargetEn.ifBlank { transcript }
  var dictationInput by remember(resource.id) { mutableStateOf("") }
  var dictationAccuracy by remember(resource.id) { mutableStateOf<Int?>(null) }
  var showTranscript by remember(resource.id) { mutableStateOf(false) }

  val playTag = "listen-${resource.id}"
  val isPlaying = audioState.isSpeaking && audioState.speakingMessageId == playTag

  GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text("Reproductor de Audio y Dictado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      if (payload.instructionsEs.isNotBlank()) {
        Text(payload.instructionsEs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = { onPlayText(transcript, playTag, false) },
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isPlaying) "Detener" else "Escuchar normal")
        }
        OutlinedButton(
          onClick = { onPlayText(transcript, "$playTag-slow", true) },
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Rounded.SlowMotionVideo, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Lento 0.65x")
        }
      }

      if (dictationTarget != transcript) {
        OutlinedButton(
          onClick = { onPlayText(dictationTarget, "$playTag-dict", false) },
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Escuchar frase exacta del dictado")
        }
      }

      val extAudio = payload.listenUrl.ifBlank { payload.audioUrl }
      if (extAudio.isNotBlank()) {
        OutlinedButton(onClick = { onOpenAudioUrl(extAudio) }, shape = RoundedCornerShape(12.dp)) {
          Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Reproducir audio abierto original")
        }
      }

      if (payload.hintsEs.isNotBlank()) {
        Text("Pista: ${payload.hintsEs}", style = MaterialTheme.typography.labelSmall, color = FluentTheme.colors.cyanGlow)
      }

      OutlinedTextField(
        value = dictationInput,
        onValueChange = { dictationInput = it },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        shape = RoundedCornerShape(14.dp),
        label = { Text("Escribe el dictado en inglés") },
        placeholder = { Text("Escribe exactamente la frase que escuchaste...") },
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
          onClick = {
            val pct = (PlacementEngine.evaluateSpokenMatch(dictationTarget, dictationInput) * 100).toInt()
            dictationAccuracy = pct
            val tags = if (pct < 70) listOf("listening", "dictation") else emptyList()
            onSubmitScore(pct.toFloat(), tags)
          },
          enabled = dictationInput.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("Comprobar dictado")
        }
        TextButton(onClick = { showTranscript = !showTranscript }) {
          Text(if (showTranscript) "Ocultar transcripción" else "Ver transcripción")
        }
      }

      dictationAccuracy?.let { pct ->
        Text(
          text = "Coincidencia exacta del dictado: $pct%",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = if (pct >= 75) Color(0xFF34D399) else Color(0xFFFBBF24),
        )
      }

      if (showTranscript) {
        HorizontalDivider()
        Text("Objetivo: $dictationTarget", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        if (payload.translationEs.isNotBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(payload.translationEs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton(onClick = { onPlaySpanishText(payload.translationEs, "listen-es-${resource.id}") }) {
              Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Oír traducción")
            }
          }
        }
      }
    }
  }

  if (payload.questions.isNotEmpty()) {
    MultiQuestionQuizRunner(
      titleEs = "Comprensión auditiva",
      questions = payload.questions,
      audioTagPrefix = resource.id,
      onPlayText = onPlayText,
      onGraded = onSubmitScore,
    )
  }
}

@Composable
private fun GrammarWorkspace(
  resource: EduResource,
  payload: EduPayloadDetail,
  onPlayText: (String, String, Boolean) -> Unit,
  onSubmitScore: (Float, List<String>) -> Unit,
) {
  var selectedIdx by remember(resource.id) { mutableIntStateOf(-1) }
  var checked by remember(resource.id) { mutableStateOf(false) }
  val options = remember(resource.id, payload.options) {
    val orig = payload.options
    if (orig.size > 1) orig.shuffled(kotlin.random.Random(resource.id.hashCode())) else orig
  }
  val correctIdx = options.indexOfFirst { it.equals(payload.expectedAnswerEn, ignoreCase = true) }.coerceAtLeast(0)

  GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      if (payload.grammarPoint.isNotBlank()) {
        Text(
          text = "Punto gramatical: ${payload.grammarPoint}",
          style = MaterialTheme.typography.labelLarge,
          color = FluentTheme.colors.cyanGlow,
        )
      }
      Text(payload.instructionsEs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(payload.questionPromptEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

      options.forEachIndexed { idx, opt ->
        val isChosen = selectedIdx == idx
        val isCorrectOpt = idx == correctIdx
        val borderColor = when {
          !checked && isChosen -> MaterialTheme.colorScheme.primary
          checked && isCorrectOpt -> Color(0xFF34D399)
          checked && isChosen && !isCorrectOpt -> MaterialTheme.colorScheme.error
          else -> FluentTheme.colors.cardBorder
        }
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !checked) { selectedIdx = idx },
          shape = RoundedCornerShape(12.dp),
          color = if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.2.dp, borderColor),
        ) {
          Text(opt, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
      }

      if (!checked) {
        GradientPillButton(
          text = "Comprobar respuesta",
          enabled = selectedIdx >= 0,
          onClick = {
            checked = true
            val right = selectedIdx == correctIdx
            onSubmitScore(if (right) 100f else 40f, if (right) emptyList() else listOf("grammar", payload.grammarPoint))
          },
          height = 46.dp,
        )
      } else {
        val right = selectedIdx == correctIdx
        Text(
          text = if (right) "¡Exacto! Respuesta correcta." else "Respuesta esperada: ${payload.expectedAnswerEn}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = if (right) Color(0xFF34D399) else Color(0xFFFBBF24),
        )
        Text(payload.explanationEs, style = MaterialTheme.typography.bodySmall)
        if (payload.spanishTrapEs.isNotBlank()) {
          Text("Trampa común: ${payload.spanishTrapEs}", style = MaterialTheme.typography.labelSmall, color = FluentTheme.colors.cyanGlow)
        }
        OutlinedButton(onClick = { onPlayText(payload.expectedAnswerEn, "gram-audio-${resource.id}", false) }) {
          Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Escuchar oración correcta")
        }
      }
    }
  }
}

@Composable
private fun WritingWorkspace(
  resource: EduResource,
  payload: EduPayloadDetail,
  onPlayText: (String, String, Boolean) -> Unit,
  onSubmitScore: (Float) -> Unit,
) {
  var draftText by remember(resource.id) { mutableStateOf("") }
  val checklistChecked = remember(resource.id) { mutableStateMapOf<Int, Boolean>() }
  var showModel by remember(resource.id) { mutableStateOf(false) }
  val wordCount = remember(draftText) { draftText.trim().split(Regex("\\s+")).count { it.isNotBlank() } }

  GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text("Consigna de escritura", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Text(payload.promptEn, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
      Text(payload.instructionsEs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

      if (payload.structureSteps.isNotEmpty()) {
        Text("Estructura paso a paso:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        payload.structureSteps.forEachIndexed { idx, step ->
          Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
        }
      }

      OutlinedTextField(
        value = draftText,
        onValueChange = { draftText = it },
        modifier = Modifier.fillMaxWidth(),
        minLines = 5,
        shape = RoundedCornerShape(14.dp),
        label = { Text("Tu borrador en inglés ($wordCount / ${payload.targetMinWords}–${payload.targetMaxWords} palabras)") },
      )

      if (payload.evaluationChecklist.isNotEmpty()) {
        Text("Lista de autoevaluación:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        payload.evaluationChecklist.forEachIndexed { idx, chk ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = checklistChecked[idx] == true,
              onCheckedChange = { checklistChecked[idx] = it },
            )
            Text(chk, style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GradientPillButton(
          text = "Guardar escritura",
          enabled = wordCount >= 10,
          onClick = {
            val metLength = wordCount >= payload.targetMinWords
            val checks = if (payload.evaluationChecklist.isNotEmpty()) {
              checklistChecked.values.count { it }.toFloat() / payload.evaluationChecklist.size.toFloat()
            } else {
              1f
            }
            val score = ((if (metLength) 60f else 40f) + checks * 40f).coerceIn(50f, 100f)
            onSubmitScore(score)
          },
          modifier = Modifier.weight(1f),
          height = 46.dp,
        )
        OutlinedButton(onClick = { showModel = !showModel }, shape = RoundedCornerShape(14.dp)) {
          Text(if (showModel) "Ocultar modelo" else "Ver modelo")
        }
      }

      if (showModel && payload.modelAnswerEn.isNotBlank()) {
        HorizontalDivider()
        Text("Modelo de referencia:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = FluentTheme.colors.cyanGlow)
        Text(payload.modelAnswerEn, style = MaterialTheme.typography.bodyMedium)
        Text(payload.modelExplanationEs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { onPlayText(payload.modelAnswerEn, "write-model-${resource.id}", false) }) {
          Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Escuchar modelo")
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VocabularyAndSpeechWorkspace(
  resource: EduResource,
  payload: EduPayloadDetail,
  settings: AppSettings,
  audioState: AudioEngineState,
  onPlayText: (String, String, Boolean) -> Unit,
  onPlaySpanishText: (String, String) -> Unit,
  onSavePhrase: (String, String, String, String) -> Unit,
  onStartSpeechCheck: (Long, (String) -> Unit) -> Unit,
  onStopSpeechCheck: () -> Unit,
  onSubmitScore: (Float) -> Unit,
) {
  val context = LocalContext.current
  val headword = payload.word.ifBlank { payload.headword.ifBlank { payload.targetPhrase.ifBlank { payload.english.ifBlank { resource.title } } } }
  val sentence = payload.exampleEn.ifBlank { payload.targetPhrase.ifBlank { payload.english.ifBlank { headword } } }
  val translation = payload.spanish.ifBlank { payload.exampleEs.ifBlank { resource.description } }

  var isRecording by remember(resource.id) { mutableStateOf(false) }
  var attemptCount by remember(resource.id) { mutableIntStateOf(0) }
  var speechFeedback by remember(resource.id) { mutableStateOf<com.example.englishapp.data.SpeakingAnalysis?>(null) }

  fun startPronunciationCapture() {
    isRecording = true
    onStartSpeechCheck(settings.activeLevel().sttSilenceMs) { transcript ->
      isRecording = false
      attemptCount += 1
      val analysis = SpeakingCoachEngine.analyzePronunciation(
        expected = sentence,
        spoken = transcript,
        attemptNumber = attemptCount,
        previousScore = speechFeedback?.overallScore,
      )
      speechFeedback = analysis
      onSubmitScore(analysis.overallScore.toFloat())
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      startPronunciationCapture()
    }
  }

  GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(headword, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      if (payload.ipa.isNotBlank()) {
        Text(payload.ipa, style = MaterialTheme.typography.labelLarge, color = FluentTheme.colors.cyanGlow)
      }
      Text(translation, style = MaterialTheme.typography.bodyMedium)
      if (sentence != headword) {
        Text("Ejemplo: $sentence", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
      }
      val tip = payload.tipEs.ifBlank { payload.spanishTip }
      if (tip.isNotBlank()) {
        Text("Tip: $tip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      if (payload.synonyms.isNotEmpty() || payload.antonyms.isNotEmpty()) {
        HorizontalDivider()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          payload.synonyms.forEach { syn ->
            Surface(shape = RoundedCornerShape(8.dp), color = FluentTheme.colors.cyanGlow.copy(alpha = 0.14f)) {
              Text("Sin: $syn", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
          }
          payload.antonyms.forEach { ant ->
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
              Text("Ant: $ant", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onPlayText(sentence, "vocab-${resource.id}", false) }, shape = RoundedCornerShape(12.dp)) {
          Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("EN")
        }
        OutlinedButton(onClick = { onPlayText(sentence, "vocab-slow-${resource.id}", true) }, shape = RoundedCornerShape(12.dp)) {
          Text("0.65x")
        }
        OutlinedButton(onClick = { onPlaySpanishText(translation, "vocab-es-${resource.id}") }, shape = RoundedCornerShape(12.dp)) {
          Text("ES")
        }
        OutlinedButton(onClick = { onSavePhrase(sentence, translation, tip, "Learning Hub") }, shape = RoundedCornerShape(12.dp)) {
          Icon(Icons.Rounded.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
        }
      }

      HorizontalDivider()
      Text("Práctica oral con evaluación", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      SpeakingMasteryCard(
        targetPhrase = sentence,
        spanishHint = translation,
        tipEs = tip.ifBlank { null },
        analysis = speechFeedback,
        isListeningThis = isRecording,
        audioState = audioState,
        onListenNormal = { onPlayText(sentence, "vocab-card-${resource.id}", false) },
        onListenSlow = { onPlayText(sentence, "vocab-card-slow-${resource.id}", true) },
        onListenWord = { word -> onPlayText(word, "vocab-word-$word", true) },
        onToggleRecord = {
          if (isRecording) {
            isRecording = false
            onStopSpeechCheck()
          } else {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) {
              startPronunciationCapture()
            } else {
              permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          }
        },
      )

      GradientPillButton(
        text = "Marcar dominado",
        onClick = { onSubmitScore(90f) },
        height = 46.dp,
      )
    }
  }
}

@Composable
private fun MultiQuestionQuizRunner(
  titleEs: String,
  questions: List<EduQuizQuestionPayload>,
  audioTagPrefix: String,
  onPlayText: (String, String, Boolean) -> Unit,
  onGraded: (scorePct: Float, errorTags: List<String>) -> Unit,
) {
  val selections = remember(audioTagPrefix) { mutableStateMapOf<Int, Int>() }
  var isSubmitted by remember(audioTagPrefix) { mutableStateOf(false) }
  val shuffledQuestions = remember(audioTagPrefix, questions) {
    questions.mapIndexed { qIdx, q ->
      val indexed = q.options.mapIndexed { origIdx, label -> origIdx to label }
      val reordered = if (indexed.size > 1) {
        indexed.shuffled(kotlin.random.Random("$audioTagPrefix-$qIdx".hashCode()))
      } else {
        indexed
      }
      val newCorrectIdx = reordered.indexOfFirst { it.first == q.correctIndex }.coerceAtLeast(0)
      q.copy(
        options = reordered.map { it.second },
        correctIndex = newCorrectIdx,
      )
    }
  }

  GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(titleEs, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      shuffledQuestions.forEachIndexed { qIdx, q ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("${qIdx + 1}. ${q.promptEs}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          q.audioPromptEn?.takeIf { it.isNotBlank() }?.let { audioText ->
            OutlinedButton(onClick = { onPlayText(audioText, "$audioTagPrefix-q$qIdx", false) }, shape = RoundedCornerShape(10.dp)) {
              Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Reproducir audio")
            }
          }
          q.options.forEachIndexed { optIdx, optText ->
            val picked = selections[qIdx] == optIdx
            val isCorrectOpt = optIdx == q.correctIndex
            val borderColor = when {
              !isSubmitted && picked -> MaterialTheme.colorScheme.primary
              isSubmitted && isCorrectOpt -> Color(0xFF34D399)
              isSubmitted && picked && !isCorrectOpt -> MaterialTheme.colorScheme.error
              else -> FluentTheme.colors.cardBorder
            }
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = !isSubmitted) { selections[qIdx] = optIdx },
              shape = RoundedCornerShape(10.dp),
              color = if (picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, borderColor),
            ) {
              Text(optText, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
            }
          }
          if (isSubmitted && q.explanationEs.isNotBlank()) {
            Text(
              text = q.explanationEs,
              style = MaterialTheme.typography.labelSmall,
              color = FluentTheme.colors.cyanGlow,
            )
          }
        }
      }

      if (!isSubmitted) {
        GradientPillButton(
          text = "Calificar evaluación (${selections.size}/${shuffledQuestions.size})",
          enabled = shuffledQuestions.isNotEmpty() && selections.size == shuffledQuestions.size,
          onClick = {
            isSubmitted = true
            var hits = 0
            val wrongTags = mutableListOf<String>()
            shuffledQuestions.forEachIndexed { idx, q ->
              if (selections[idx] == q.correctIndex) {
                hits++
              } else {
                wrongTags.add(q.promptEs.take(40))
              }
            }
            val scorePct = if (shuffledQuestions.isEmpty()) 100f else (hits.toFloat() / shuffledQuestions.size.toFloat()) * 100f
            onGraded(scorePct, wrongTags)
          },
          height = 48.dp,
        )
      }
    }
  }
}
