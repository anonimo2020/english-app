package com.example.englishapp.chat

import com.example.englishapp.model.ChatMessage
import com.example.englishapp.model.Correction
import com.example.englishapp.model.PronunciationTip
import com.example.englishapp.model.SessionSummary
import com.example.englishapp.model.TutorReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class HealthStatus(
  val ok: Boolean,
  val aiConfigured: Boolean,
  val version: String,
  val latencyMs: Long,
)

/**
 * Cliente HTTP nativo hacia el backend FastAPI (Render o servidor local).
 * Utiliza kotlinx.serialization para mantener compatibilidad retroactiva con
 * respuestas simples o enriquecidas de `/chat`, `/health` y `/progress/summary`.
 */
class ChatClient(
  private val baseUrl: String,
  private val appToken: String = "",
) {
  private val cleanBaseUrl: String = baseUrl.trim().trimEnd('/')

  suspend fun send(
    character: String,
    level: String,
    history: List<ChatMessage>,
    scenario: String = "free_chat",
    accent: String = "US",
  ): Result<TutorReply> = withContext(Dispatchers.IO) {
    runCatching {
      require(cleanBaseUrl.isNotBlank()) { "Configura la URL del servidor en Ajustes." }
      val requestBody = json.encodeToString(
        ChatRequestDto(
          character = character,
          level = level,
          messages = history.map { MessageDto(it.role, it.content) },
          scenario = scenario,
          accent = accent,
        ),
      )
      val text = postJson("$cleanBaseUrl/chat", requestBody)
      parseReplyJson(text)
    }
  }

  suspend fun fetchSessionSummary(
    character: String,
    level: String,
    scenario: String,
    history: List<ChatMessage>,
  ): Result<SessionSummary> = withContext(Dispatchers.IO) {
    runCatching {
      require(cleanBaseUrl.isNotBlank()) { "URL no configurada" }
      val requestBody = json.encodeToString(
        SummaryRequestDto(
          character = character,
          level = level,
          scenario = scenario,
          messages = history.map { MessageDto(it.role, it.content) },
        ),
      )
      val text = postJson("$cleanBaseUrl/progress/summary", requestBody)
      parseSummaryJson(text)
    }
  }

  suspend fun checkHealth(): Result<HealthStatus> = withContext(Dispatchers.IO) {
    runCatching {
      require(cleanBaseUrl.isNotBlank()) { "Ingresa la URL del backend antes de probar." }
      val started = System.currentTimeMillis()
      val conn = (URL("$cleanBaseUrl/health").openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 12_000
        readTimeout = 45_000
      }
      try {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
          error(extractErrorDetail(text, code))
        }
        val dto = json.decodeFromString<HealthResponseDto>(text)
        HealthStatus(
          ok = dto.ok,
          aiConfigured = dto.aiConfigured,
          version = dto.version,
          latencyMs = System.currentTimeMillis() - started,
        )
      } catch (e: SocketTimeoutException) {
        error("El servidor tardó demasiado en responder (timeout). Si está en Render gratuito, intenta otra vez.")
      } catch (e: IOException) {
        error("No se pudo conectar a $cleanBaseUrl (${e.message ?: "error de red"}).")
      } finally {
        conn.disconnect()
      }
    }
  }

  suspend fun warmUp(): Result<Unit> = checkHealth().map { Unit }

  private fun postJson(url: String, body: String): String {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 15_000
      readTimeout = 60_000
      doOutput = true
      setRequestProperty("Content-Type", "application/json; charset=utf-8")
      if (appToken.isNotBlank()) {
        setRequestProperty("X-App-Token", appToken)
      }
    }
    try {
      conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
      val code = conn.responseCode
      val stream = if (code in 200..299) conn.inputStream else conn.errorStream
      val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
      if (code !in 200..299) {
        error(extractErrorDetail(text, code))
      }
      return text
    } catch (e: SocketTimeoutException) {
      error("Tiempo de espera agotado. Si usas Render gratuito, espera unos segundos y reintenta.")
    } catch (e: IOException) {
      error("Error de conexión con el servidor (${e.message ?: "sin acceso a red"}).")
    } finally {
      conn.disconnect()
    }
  }

  @Serializable
  private data class MessageDto(val role: String, val content: String)

  @Serializable
  private data class ChatRequestDto(
    val character: String,
    val level: String,
    val messages: List<MessageDto>,
    val scenario: String = "free_chat",
    val accent: String = "US",
  )

  @Serializable
  private data class SummaryRequestDto(
    val character: String,
    val level: String,
    val scenario: String = "free_chat",
    val messages: List<MessageDto>,
  )

  @Serializable
  private data class CorrectionDto(
    val better: String,
    val why: String = "",
    val tip: String? = null,
  )

  @Serializable
  private data class PronunciationTipDto(
    val word: String,
    val phonetic: String = "",
    @SerialName("tip_es") val tipEs: String = "",
  )

  @Serializable
  private data class ChatResponseDto(
    val reply: String,
    @SerialName("translation_es") val translationEs: String? = null,
    val correction: CorrectionDto? = null,
    @SerialName("suggested_replies") val suggestedReplies: List<String> = emptyList(),
    @SerialName("pronunciation_focus") val pronunciationFocus: List<PronunciationTipDto> = emptyList(),
    @SerialName("xp_earned") val xpEarned: Int = 12,
    @SerialName("coach_note_es") val coachNoteEs: String? = null,
  )

  @Serializable
  private data class SummaryResponseDto(
    @SerialName("strengths_es") val strengthsEs: List<String> = emptyList(),
    @SerialName("focus_areas_es") val focusAreasEs: List<String> = emptyList(),
    @SerialName("fluency_score") val fluencyScore: Int = 75,
    @SerialName("xp_bonus") val xpBonus: Int = 20,
  )

  @Serializable
  private data class HealthResponseDto(
    val ok: Boolean = false,
    @SerialName("ai_configured") val aiConfigured: Boolean = false,
    val version: String = "1.0",
  )

  @Serializable
  private data class ErrorResponseDto(val detail: String? = null)

  companion object {
    val json: Json = Json {
      ignoreUnknownKeys = true
      encodeDefaults = true
      isLenient = true
    }

    fun parseReplyJson(rawJson: String): TutorReply {
      val dto = json.decodeFromString<ChatResponseDto>(rawJson)
      return TutorReply(
        reply = dto.reply,
        translationEs = dto.translationEs?.takeIf { it.isNotBlank() },
        correction = dto.correction?.let {
          Correction(better = it.better, why = it.why, tip = it.tip?.takeIf { t -> t.isNotBlank() })
        },
        suggestedReplies = dto.suggestedReplies.filter { it.isNotBlank() },
        pronunciationFocus = dto.pronunciationFocus.map {
          PronunciationTip(word = it.word, phonetic = it.phonetic, tipEs = it.tipEs)
        },
        xpEarned = dto.xpEarned.coerceIn(5, 50),
        coachNoteEs = dto.coachNoteEs?.takeIf { it.isNotBlank() },
      )
    }

    fun parseSummaryJson(rawJson: String): SessionSummary {
      val dto = json.decodeFromString<SummaryResponseDto>(rawJson)
      return SessionSummary(
        strengthsEs = dto.strengthsEs,
        focusAreasEs = dto.focusAreasEs,
        fluencyScore = dto.fluencyScore.coerceIn(40, 100),
        xpBonus = dto.xpBonus.coerceIn(5, 100),
      )
    }

    private fun extractErrorDetail(body: String, code: Int): String {
      val detail = runCatching { json.decodeFromString<ErrorResponseDto>(body).detail }.getOrNull()
      if (!detail.isNullOrBlank()) return detail
      return when (code) {
        401 -> "Token de app inválido (X-App-Token)."
        429 -> "Demasiadas peticiones. Espera unos segundos e intenta otra vez."
        else -> "Error del servidor ($code)."
      }
    }
  }
}
