package com.example.englishapp.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Nota: El proyecto Android completo vive en ./android-app/ (con Audio STT/TTS, Compose UI y kotlinx.serialization).
// Este archivo se mantiene sincronizado para referencia rápida o uso independiente con org.json.

data class ChatMessage(val role: String, val content: String) // role: "user" | "assistant"
data class Correction(val better: String, val why: String, val tip: String? = null)
data class PronunciationTip(val word: String, val phonetic: String, val tipEs: String)
data class TutorReply(
    val reply: String,
    val translationEs: String? = null,
    val correction: Correction? = null,
    val suggestedReplies: List<String> = emptyList(),
    val pronunciationFocus: List<PronunciationTip> = emptyList(),
    val xpEarned: Int = 12
)

/**
 * Habla con el backend de Render.
 * baseUrl: por ejemplo "https://english-tutor-api.onrender.com" (sin barra final).
 * appToken: el mismo APP_TOKEN que pusiste en Render.
 */
class ChatClient(private val baseUrl: String, private val appToken: String) {

    /** character: "manu" | "eddy". level: "NOVATO" | "JUNIOR" | "INTERMEDIO" | "PRO". */
    suspend fun send(
        character: String,
        level: String,
        history: List<ChatMessage>,
        scenario: String = "free_chat",
        accent: String = "US"
    ): Result<TutorReply> = withContext(Dispatchers.IO) {
        runCatching {
            val messages = JSONArray()
            history.forEach {
                messages.put(JSONObject().put("role", it.role).put("content", it.content))
            }
            val body = JSONObject()
                .put("character", character)
                .put("level", level)
                .put("scenario", scenario)
                .put("accent", accent)
                .put("messages", messages)

            val conn = (URL("$baseUrl/chat").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000 // el plan gratuito de Render tarda en "despertar"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-App-Token", appToken)
            }
            try {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val detail = runCatching { JSONObject(text).optString("detail") }.getOrNull()
                    error(if (detail.isNullOrBlank()) "Error $code" else detail)
                }
                val json = JSONObject(text)
                val c = json.optJSONObject("correction")
                val suggestionsArr = json.optJSONArray("suggested_replies")
                val suggestions = buildList {
                    if (suggestionsArr != null) {
                        for (i in 0 until suggestionsArr.length()) {
                            val item = suggestionsArr.optString(i)
                            if (item.isNotBlank()) add(item)
                        }
                    }
                }
                val pronArr = json.optJSONArray("pronunciation_focus")
                val pronunciation = buildList {
                    if (pronArr != null) {
                        for (i in 0 until pronArr.length()) {
                            val obj = pronArr.optJSONObject(i) ?: continue
                            val word = obj.optString("word")
                            if (word.isNotBlank()) {
                                add(
                                    PronunciationTip(
                                        word = word,
                                        phonetic = obj.optString("phonetic"),
                                        tipEs = obj.optString("tip_es")
                                    )
                                )
                            }
                        }
                    }
                }
                TutorReply(
                    reply = json.getString("reply"),
                    translationEs = json.optString("translation_es").takeIf { it.isNotBlank() },
                    correction = c?.let {
                        Correction(
                            better = it.getString("better"),
                            why = it.optString("why"),
                            tip = it.optString("tip").takeIf { t -> t.isNotBlank() }
                        )
                    },
                    suggestedReplies = suggestions,
                    pronunciationFocus = pronunciation,
                    xpEarned = json.optInt("xp_earned", 12)
                )
            } finally {
                conn.disconnect()
            }
        }
    }

    /** Despierta el servidor gratuito para que la primera respuesta no tarde. */
    suspend fun warmUp() = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("$baseUrl/health").openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.responseCode
            conn.disconnect()
        }
    }
}
