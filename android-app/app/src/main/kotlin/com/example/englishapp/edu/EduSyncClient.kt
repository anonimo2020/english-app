package com.example.englishapp.edu

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente de sincronización incremental con `/api/v1/edu` y consultas seguras
 * a APIs abiertas gratuitas (Tatoeba, Datamuse, Simple English Wikipedia).
 * No almacena ninguna clave privada dentro de la APK.
 */
object EduSyncClient {

  val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
  }

  @Serializable
  data class SyncApiResponse(
    @SerialName("server_time") val serverTime: String = "",
    val total: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
    val resources: List<SyncResourceDto> = emptyList(),
    val providers: List<EduProvider> = emptyList(),
    val licenses: List<EduLicense> = emptyList(),
    val attributions: List<EduAttribution> = emptyList(),
  )

  @Serializable
  data class SyncResourceDto(
    val id: String,
    val title: String,
    @SerialName("title_es") val titleEs: String = "",
    val description: String = "",
    val language: String = "en",
    @SerialName("cefr_level") val cefrLevel: String = "A2",
    @SerialName("category_id") val categoryId: String = "reading_library",
    @SerialName("primary_skill") val primarySkill: String = "reading",
    @SerialName("secondary_skills") val secondarySkills: List<String> = emptyList(),
    val topic: String = "general_english",
    val difficulty: Int = 3,
    @SerialName("estimated_duration_min") val estimatedDurationMin: Int = 5,
    @SerialName("source_type") val sourceType: String = "open_reusable",
    @SerialName("provider_id") val providerId: String = "fluentvoice_internal",
    val author: String = "",
    @SerialName("original_url") val originalUrl: String = "",
    @SerialName("content_type") val contentType: String = "reading",
    @SerialName("license_id") val licenseId: String = "FLUENTVOICE-OWNED",
    @SerialName("attribution_id") val attributionId: String? = null,
    @SerialName("added_at") val addedAt: String = "",
    @SerialName("last_verified_at") val lastVerifiedAt: String = "",
    @SerialName("availability_status") val availabilityStatus: String = "available",
    @SerialName("validation_status") val validationStatus: String = "validated",
    @SerialName("cefr_confidence") val cefrConfidence: Double = 1.0,
    @SerialName("cefr_classification_source") val cefrClassificationSource: String = "curated_internal",
    @SerialName("learning_objective_es") val learningObjectiveEs: String = "",
    @SerialName("exercise_type") val exerciseType: String = "practice",
    @SerialName("offline_compatible") val offlineCompatible: Boolean = true,
    @SerialName("updated_at") val updatedAt: String = "",
    val payload: JsonObject = JsonObject(emptyMap()),
  ) {
    fun toEduResource(): EduResource? {
      if (EduSecuritySanitizer.hasPromptInjection("$title $description")) return null
      if (originalUrl.isNotBlank() && !EduSecuritySanitizer.isSafeExternalUrl(originalUrl)) return null
      if (!EduSecuritySanitizer.isLicenseRecognized(licenseId)) return null
      return EduResource(
        id = id,
        title = EduSecuritySanitizer.sanitizeText(title, 240),
        titleEs = EduSecuritySanitizer.sanitizeText(titleEs.ifBlank { title }, 240),
        description = EduSecuritySanitizer.sanitizeText(description, 900),
        language = language,
        cefrLevel = cefrLevel,
        categoryId = categoryId,
        primarySkill = primarySkill,
        secondarySkills = secondarySkills,
        topic = topic,
        difficulty = difficulty.coerceIn(1, 10),
        estimatedDurationMin = estimatedDurationMin.coerceAtLeast(1),
        sourceType = sourceType,
        providerId = providerId,
        author = EduSecuritySanitizer.sanitizeText(author, 140),
        originalUrl = originalUrl,
        contentType = contentType,
        licenseId = licenseId,
        attributionId = attributionId,
        addedAt = addedAt,
        lastVerifiedAt = lastVerifiedAt,
        availabilityStatus = availabilityStatus,
        validationStatus = validationStatus,
        cefrConfidence = cefrConfidence,
        cefrClassificationSource = cefrClassificationSource,
        learningObjectiveEs = EduSecuritySanitizer.sanitizeText(learningObjectiveEs, 400),
        exerciseType = exerciseType,
        offlineCompatible = offlineCompatible,
        entityTable = "educational_resources",
        entityId = id,
        updatedAt = updatedAt,
        payloadJson = json.encodeToString(payload),
      )
    }
  }

  @Serializable
  private data class ProgressSyncDto(
    @SerialName("user_id") val userId: String = "default_learner",
    @SerialName("resource_id") val resourceId: String,
    val status: String,
    val score: Float,
    @SerialName("time_spent_sec") val timeSpentSec: Int,
    @SerialName("error_tags") val errorTags: List<String> = emptyList(),
    @SerialName("xp_earned") val xpEarned: Int = 15,
  )

  @Serializable
  private data class FavoriteSyncDto(
    @SerialName("user_id") val userId: String = "default_learner",
    @SerialName("resource_id") val resourceId: String,
  )

  @Serializable
  private data class RemoteFavoriteItemDto(
    val id: String,
  )

  @Serializable
  private data class RemoteFavoritesResponseDto(
    @SerialName("user_id") val userId: String = "default_learner",
    val total: Int = 0,
    val favorites: List<RemoteFavoriteItemDto> = emptyList(),
  )

  @Serializable
  private data class RemoteProgressItemDto(
    @SerialName("resource_id") val resourceId: String,
    val status: String,
    val score: Float = 0f,
    val attempts: Int = 1,
    @SerialName("time_spent_sec") val timeSpentSec: Int = 0,
    @SerialName("last_position") val lastPosition: String = "",
    @SerialName("error_tags_json") val errorTagsJson: String = "[]",
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String = "",
  ) {
    fun toRecord(): EduUserProgressRecord {
      val tags = runCatching { json.decodeFromString<List<String>>(errorTagsJson) }.getOrDefault(emptyList())
      return EduUserProgressRecord(
        resourceId = resourceId,
        status = status,
        score = score,
        attempts = attempts,
        timeSpentSec = timeSpentSec,
        lastPosition = lastPosition,
        errorTags = tags,
        completedAt = completedAt,
        updatedAt = updatedAt,
      )
    }
  }

  @Serializable
  private data class RemoteProgressResponseDto(
    @SerialName("user_id") val userId: String = "default_learner",
    val progress: List<RemoteProgressItemDto> = emptyList(),
  )

  fun parseSyncJson(raw: String): SyncApiResponse = json.decodeFromString(raw)

  private fun sanitizeBackendBaseUrl(rawUrl: String): String {
    val cleanBase = rawUrl.trim().trimEnd('/')
    require(cleanBase.startsWith("https://") || cleanBase.startsWith("http://")) {
      "URL del backend inválida (debe iniciar con http:// o https://)."
    }
    return cleanBase
  }

  suspend fun fetchIncrementalSync(
    baseUrl: String,
    since: String?,
    appToken: String = "",
  ): Result<SyncApiResponse> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanBase = sanitizeBackendBaseUrl(baseUrl)
      val sinceParam = if (!since.isNullOrBlank()) "since=${URLEncoder.encode(since, "UTF-8")}&" else ""
      val mergedResources = mutableListOf<SyncResourceDto>()
      var offset = 0
      val pageLimit = 250
      var latestResponse = SyncApiResponse()

      for (page in 0 until 25) {
        val url = "$cleanBase/api/v1/edu/sync?${sinceParam}limit=$pageLimit&offset=$offset"
        val text = httpGet(url, appToken, allowConfiguredBackend = true)
        val parsed = parseSyncJson(text)
        latestResponse = parsed
        mergedResources.addAll(parsed.resources)
        if (!parsed.hasMore || parsed.resources.isEmpty()) break
        offset += parsed.resources.size
      }

      latestResponse.copy(
        total = mergedResources.size,
        hasMore = false,
        resources = mergedResources.distinctBy { it.id },
      )
    }
  }

  suspend fun pushProgressBestEffort(
    baseUrl: String,
    appToken: String,
    resourceId: String,
    status: String,
    score: Float,
    timeSpentSec: Int,
    errorTags: List<String>,
    userId: String = "default_learner",
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanBase = sanitizeBackendBaseUrl(baseUrl)
      val body = json.encodeToString(
        ProgressSyncDto(
          userId = userId,
          resourceId = resourceId,
          status = status,
          score = score,
          timeSpentSec = timeSpentSec,
          errorTags = errorTags,
        ),
      )
      httpPost("$cleanBase/api/v1/edu/progress", body, appToken)
      Unit
    }
  }

  suspend fun pushFavoriteBestEffort(
    baseUrl: String,
    appToken: String,
    resourceId: String,
    userId: String = "default_learner",
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanBase = sanitizeBackendBaseUrl(baseUrl)
      val body = json.encodeToString(FavoriteSyncDto(userId = userId, resourceId = resourceId))
      httpPost("$cleanBase/api/v1/edu/favorites", body, appToken)
      Unit
    }
  }

  suspend fun deleteFavoriteBestEffort(
    baseUrl: String,
    appToken: String,
    resourceId: String,
    userId: String = "default_learner",
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanBase = sanitizeBackendBaseUrl(baseUrl)
      val u = URLEncoder.encode(userId, "UTF-8")
      val r = URLEncoder.encode(resourceId, "UTF-8")
      httpDelete("$cleanBase/api/v1/edu/favorites/$u/$r", appToken)
      Unit
    }
  }

  suspend fun fetchRemoteFavorites(
    baseUrl: String,
    appToken: String,
    userId: String = "default_learner",
  ): Result<Set<String>> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanBase = sanitizeBackendBaseUrl(baseUrl)
      val u = URLEncoder.encode(userId, "UTF-8")
      val text = httpGet("$cleanBase/api/v1/edu/favorites/$u", appToken, allowConfiguredBackend = true)
      json.decodeFromString<RemoteFavoritesResponseDto>(text).favorites.map { it.id }.toSet()
    }
  }

  suspend fun fetchRemoteProgress(
    baseUrl: String,
    appToken: String,
    userId: String = "default_learner",
  ): Result<List<EduUserProgressRecord>> = withContext(Dispatchers.IO) {
    runCatching {
      val cleanBase = sanitizeBackendBaseUrl(baseUrl)
      val u = URLEncoder.encode(userId, "UTF-8")
      val text = httpGet("$cleanBase/api/v1/edu/progress/$u", appToken, allowConfiguredBackend = true)
      json.decodeFromString<RemoteProgressResponseDto>(text).progress.map { it.toRecord() }
    }
  }

  /**
   * Consulta en vivo segura a Tatoeba + Datamuse + Simple English Wikipedia
   * con sanitización estricta, sin claves secretas ni scraping.
   */
  suspend fun searchLiveOpenProviders(rawQuery: String): Result<List<EduLiveQueryResult>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val term = rawQuery.trim()
        require(term.isNotEmpty()) { "Ingresa una palabra o tema para consultar." }
        val enc = URLEncoder.encode(term, "UTF-8")
        val results = mutableListOf<EduLiveQueryResult>()

        // 1. Tatoeba bilingual examples (CC BY 2.0 FR)
        runCatching {
          val tatRaw = httpGet("https://tatoeba.org/en/api_v0/search?from=eng&to=spa&query=$enc&trans_filter=limit&trans_to=spa&trans_link=direct")
          val root = json.parseToJsonElement(tatRaw).jsonObject
          val arr = root["results"]?.jsonArray ?: JsonArray(emptyList())
          arr.take(4).forEach { el ->
            val obj = el.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content.orEmpty()
            val en = EduSecuritySanitizer.sanitizeText(obj["text"]?.jsonPrimitive?.content, 240)
            var es = ""
            val trans = obj["translations"]?.jsonArray ?: JsonArray(emptyList())
            for (g in trans) {
              if (g is JsonArray && g.isNotEmpty()) {
                es = EduSecuritySanitizer.sanitizeText(g[0].jsonObject["text"]?.jsonPrimitive?.content, 240)
                if (es.isNotBlank()) break
              }
            }
            if (en.isNotBlank() && es.isNotBlank() && !EduSecuritySanitizer.hasPromptInjection(en)) {
              results.add(
                EduLiveQueryResult(
                  providerName = "Tatoeba",
                  title = en,
                  subtitleEs = "Oración real bilingüe",
                  englishText = en,
                  spanishText = es,
                  attributionText = "Tatoeba Sentence #$id (CC BY 2.0 FR)",
                  licenseCode = "CC-BY-2.0-FR",
                  originalUrl = "https://tatoeba.org/en/sentences/show/$id",
                ),
              )
            }
          }
        }

        // 2. Datamuse synonyms & associations
        runCatching {
          val singleWord = term.split(" ").first()
          val encWord = URLEncoder.encode(singleWord, "UTF-8")
          val synRaw = httpGet("https://api.datamuse.com/words?rel_syn=$encWord&max=6")
          val synWords = json.parseToJsonElement(synRaw).jsonArray
            .mapNotNull { it.jsonObject["word"]?.jsonPrimitive?.content }
            .map { EduSecuritySanitizer.sanitizeText(it, 40) }
          if (synWords.isNotEmpty()) {
            results.add(
              EduLiveQueryResult(
                providerName = "Datamuse API",
                title = "Sinónimos y afines de '$singleWord'",
                subtitleEs = "Asociaciones semánticas en tiempo real",
                englishText = synWords.joinToString(", "),
                spanishText = "Palabras relacionadas útiles para evitar repeticiones en Speaking y Writing.",
                attributionText = "Datamuse API (uso educativo gratuito)",
                licenseCode = "FREE-API-NONCOMM",
                originalUrl = "https://www.datamuse.com/api/",
              ),
            )
          }
        }

        // 3. Simple English Wikipedia summary (CC BY-SA 4.0)
        runCatching {
          val slug = URLEncoder.encode(term.replace(' ', '_'), "UTF-8")
          val wikiRaw = httpGet("https://simple.wikipedia.org/api/rest_v1/page/summary/$slug")
          val obj = json.parseToJsonElement(wikiRaw).jsonObject
          val title = EduSecuritySanitizer.sanitizeText(obj["title"]?.jsonPrimitive?.content, 120)
          val extract = EduSecuritySanitizer.sanitizeText(obj["extract"]?.jsonPrimitive?.content, 650)
          val pageUrl = obj["content_urls"]?.jsonObject?.get("desktop")?.jsonObject?.get("page")?.jsonPrimitive?.content
            ?: "https://simple.wikipedia.org/wiki/$slug"
          if (title.isNotBlank() && extract.isNotBlank() && EduSecuritySanitizer.isSafeExternalUrl(pageUrl)) {
            results.add(
              EduLiveQueryResult(
                providerName = "Simple English Wikipedia",
                title = title,
                subtitleEs = "Lectura enciclopédica clara",
                englishText = extract,
                spanishText = "Fuente abierta simplificada ideal para lectura rápida y vocabulario temático.",
                attributionText = "Wikipedia contributors — '$title' (CC BY-SA 4.0)",
                licenseCode = "CC-BY-SA-4.0",
                originalUrl = pageUrl,
              ),
            )
          }
        }

        require(results.isNotEmpty()) { "No se encontraron resultados abiertos o sin conexión para '$term'." }
        results
      }
    }

  private fun httpGet(url: String, appToken: String = "", allowConfiguredBackend: Boolean = false): String {
    require(allowConfiguredBackend || EduSecuritySanitizer.isSafeExternalUrl(url)) {
      "URL no segura bloqueada"
    }
    if (allowConfiguredBackend) {
      require(url.startsWith("https://") || url.startsWith("http://")) {
        "Esquema de URL no permitido"
      }
    }
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 9_000
      readTimeout = 15_000
      setRequestProperty("User-Agent", "FluentVoiceAndroid/2.0 (Educational-App)")
      if (appToken.isNotBlank()) setRequestProperty("X-App-Token", appToken)
    }
    return try {
      val code = conn.responseCode
      val stream = if (code in 200..299) conn.inputStream else conn.errorStream
      val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
      if (code !in 200..299) error("HTTP $code")
      text
    } finally {
      conn.disconnect()
    }
  }

  private fun httpPost(url: String, body: String, appToken: String = ""): String {
    require(url.startsWith("https://") || url.startsWith("http://")) {
      "Esquema de URL no permitido"
    }
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      connectTimeout = 9_000
      readTimeout = 15_000
      doOutput = true
      setRequestProperty("Content-Type", "application/json; charset=utf-8")
      if (appToken.isNotBlank()) setRequestProperty("X-App-Token", appToken)
    }
    return try {
      conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
      val code = conn.responseCode
      val stream = if (code in 200..299) conn.inputStream else conn.errorStream
      val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
      if (code !in 200..299) error("HTTP $code")
      text
    } finally {
      conn.disconnect()
    }
  }

  private fun httpDelete(url: String, appToken: String = ""): String {
    require(url.startsWith("https://") || url.startsWith("http://")) {
      "Esquema de URL no permitido"
    }
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
      requestMethod = "DELETE"
      connectTimeout = 9_000
      readTimeout = 15_000
      setRequestProperty("User-Agent", "FluentVoiceAndroid/2.0 (Educational-App)")
      if (appToken.isNotBlank()) setRequestProperty("X-App-Token", appToken)
    }
    return try {
      val code = conn.responseCode
      val stream = if (code in 200..299) conn.inputStream else conn.errorStream
      val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
      if (code !in 200..299) error("HTTP $code")
      text
    } finally {
      conn.disconnect()
    }
  }
}
