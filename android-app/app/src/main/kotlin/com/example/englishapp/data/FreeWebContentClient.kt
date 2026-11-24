package com.example.englishapp.data

import com.example.englishapp.chat.ChatClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class DictionaryMeaning(
  val partOfSpeech: String,
  val definitionEn: String,
  val exampleEn: String?,
)

data class DictionaryEntryResult(
  val word: String,
  val phonetic: String,
  val spanishTranslation: String?,
  val audioUrl: String?,
  val meanings: List<DictionaryMeaning>,
  val relatedWords: List<String>,
  val isFromLiveWeb: Boolean,
)

object FreeWebContentClient {

  private val json = ChatClient.json

  suspend fun lookupWord(rawWord: String): Result<DictionaryEntryResult> = withContext(Dispatchers.IO) {
    val clean = rawWord.trim().lowercase().replace(Regex("[^a-zA-Z\\-' ]"), "")
    if (clean.isBlank()) {
      return@withContext Result.failure(IllegalArgumentException("Escribe una palabra en inglés."))
    }

    val encoded = URLEncoder.encode(clean, "UTF-8")
    val liveResult = runCatching {
      coroutineScope {
        val dictDeferred = async {
          fetchText("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded")
        }
        val transDeferred = async {
          runCatching {
            fetchText("https://api.mymemory.translated.net/get?q=$encoded&langpair=en|es")
          }.getOrNull()
        }
        val relatedDeferred = async {
          runCatching {
            fetchText("https://api.datamuse.com/words?ml=$encoded&max=6")
          }.getOrNull()
        }

        val dictRaw = dictDeferred.await()
        val parsedDict = parseDictionaryJson(dictRaw)
          ?: error("No encontramos definición para \"$clean\".")
        val spanish = transDeferred.await()?.let { parseMyMemoryJson(it) }
        val related = relatedDeferred.await()?.let { parseDatamuseJson(it) }.orEmpty()

        parsedDict.copy(
          spanishTranslation = spanish ?: offlineFallback[clean]?.spanishTranslation,
          relatedWords = related.ifEmpty { offlineFallback[clean]?.relatedWords.orEmpty() },
          isFromLiveWeb = true,
        )
      }
    }

    if (liveResult.isSuccess) {
      return@withContext liveResult
    }

    val local = offlineFallback[clean]
    if (local != null) {
      return@withContext Result.success(local)
    }

    Result.failure(
      liveResult.exceptionOrNull()
        ?: IllegalStateException("Sin conexión o palabra no encontrada en el diccionario."),
    )
  }

  fun parseDictionaryJson(rawJson: String): DictionaryEntryResult? {
    val root = runCatching { json.parseToJsonElement(rawJson) }.getOrNull() as? JsonArray ?: return null
    val entry = root.firstOrNull()?.jsonObject ?: return null

    val word = entry["word"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    if (word.isEmpty()) return null

    var phonetic = entry["phonetic"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    var audioUrl: String? = null
    val phoneticsArr = entry["phonetics"] as? JsonArray
    if (phoneticsArr != null) {
      for (item in phoneticsArr) {
        val obj = item as? JsonObject ?: continue
        if (phonetic.isBlank()) {
          phonetic = obj["text"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        }
        val rawAudio = obj["audio"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (rawAudio.isNotBlank()) {
          val normalized = if (rawAudio.startsWith("//")) "https:$rawAudio" else rawAudio
          if (audioUrl == null || normalized.contains("-us", ignoreCase = true)) {
            audioUrl = normalized
          }
        }
      }
    }

    val meaningsList = mutableListOf<DictionaryMeaning>()
    val meaningsArr = entry["meanings"] as? JsonArray
    if (meaningsArr != null) {
      for (mItem in meaningsArr) {
        val mObj = mItem as? JsonObject ?: continue
        val pos = mObj["partOfSpeech"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val defsArr = mObj["definitions"] as? JsonArray ?: continue
        for (dItem in defsArr.take(2)) {
          val dObj = dItem as? JsonObject ?: continue
          val defText = dObj["definition"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
          val example = dObj["example"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
          if (defText.isNotEmpty()) {
            meaningsList.add(
              DictionaryMeaning(
                partOfSpeech = pos,
                definitionEn = defText,
                exampleEn = example,
              ),
            )
          }
        }
      }
    }

    return DictionaryEntryResult(
      word = word,
      phonetic = phonetic,
      spanishTranslation = null,
      audioUrl = audioUrl,
      meanings = meaningsList.take(4),
      relatedWords = emptyList(),
      isFromLiveWeb = true,
    )
  }

  fun parseMyMemoryJson(rawJson: String): String? {
    val dto = runCatching { json.decodeFromString<MyMemoryResponseDto>(rawJson) }.getOrNull() ?: return null
    val text = dto.responseData?.translatedText?.trim().orEmpty()
    if (text.isBlank() || text.contains("MYMEMORY WARNING", ignoreCase = true)) return null
    return text
  }

  fun parseDatamuseJson(rawJson: String): List<String> {
    val list = runCatching { json.decodeFromString<List<DatamuseWordDto>>(rawJson) }.getOrNull().orEmpty()
    return list.mapNotNull { it.word?.trim()?.takeIf { w -> w.isNotBlank() } }.distinct().take(6)
  }

  private fun fetchText(urlString: String): String {
    val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 8_000
      readTimeout = 8_000
      setRequestProperty("Accept", "application/json")
    }
    try {
      val code = conn.responseCode
      val stream = if (code in 200..299) conn.inputStream else conn.errorStream
      val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
      if (code !in 200..299) {
        error("HTTP $code")
      }
      return text
    } finally {
      conn.disconnect()
    }
  }

  @Serializable
  private data class MyMemoryDataDto(val translatedText: String? = null)

  @Serializable
  private data class MyMemoryResponseDto(val responseData: MyMemoryDataDto? = null)

  @Serializable
  private data class DatamuseWordDto(val word: String? = null)

  val offlineFallback: Map<String, DictionaryEntryResult> = listOf(
    DictionaryEntryResult(
      word = "fluent",
      phonetic = "/ˈfluːənt/",
      spanishTranslation = "fluido / que habla con soltura",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "adjective",
          "Able to speak or write a language easily, accurately, and confidently.",
          "She is fluent in English and Spanish.",
        ),
      ),
      relatedWords = listOf("articulate", "eloquent", "smooth", "confident"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "schedule",
      phonetic = "/ˈskedʒuːl/",
      spanishTranslation = "horario / agenda / programar",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "noun",
          "A plan of events, classes, or tasks with times.",
          "Let me check my schedule for tomorrow.",
        ),
      ),
      relatedWords = listOf("calendar", "agenda", "timetable", "plan"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "comfortable",
      phonetic = "/ˈkʌmftərbəl/",
      spanishTranslation = "cómodo / a gusto",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "adjective",
          "Providing physical ease or feeling relaxed and confident.",
          "I feel much more comfortable speaking English now.",
        ),
      ),
      relatedWords = listOf("cozy", "relaxed", "pleasant", "at ease"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "receipt",
      phonetic = "/rɪˈsiːt/",
      spanishTranslation = "recibo / comprobante de pago",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "noun",
          "A written or digital proof that you paid for something.",
          "Would you like a printed receipt?",
        ),
      ),
      relatedWords = listOf("invoice", "bill", "ticket", "proof"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "aisle",
      phonetic = "/aɪl/",
      spanishTranslation = "pasillo (avión, tienda o cine)",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "noun",
          "A narrow passage between rows of seats or shelves.",
          "I prefer an aisle seat on long flights.",
        ),
      ),
      relatedWords = listOf("corridor", "row", "passage", "seat"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "challenge",
      phonetic = "/ˈtʃælɪndʒ/",
      spanishTranslation = "reto / desafío",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "noun",
          "Something new and difficult that tests your ability.",
          "Learning a new skill is an exciting challenge.",
        ),
      ),
      relatedWords = listOf("obstacle", "test", "goal", "milestone"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "accomplish",
      phonetic = "/əˈkɑːmplɪʃ/",
      spanishTranslation = "lograr / cumplir",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "verb",
          "To succeed in finishing or achieving something.",
          "You accomplished your daily speaking goal!",
        ),
      ),
      relatedWords = listOf("achieve", "complete", "fulfill", "reach"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "commute",
      phonetic = "/kəˈmjuːt/",
      spanishTranslation = "trayecto diario al trabajo o estudio",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "verb",
          "To travel regularly between home and work or school.",
          "My morning commute takes thirty minutes by train.",
        ),
      ),
      relatedWords = listOf("travel", "transit", "ride", "journey"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "resilient",
      phonetic = "/rɪˈzɪliənt/",
      spanishTranslation = "resiliente / que se recupera rápido",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "adjective",
          "Able to recover quickly from setbacks or tough situations.",
          "Great learners stay resilient even when they make mistakes.",
        ),
      ),
      relatedWords = listOf("adaptable", "strong", "persistent", "tough"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "feedback",
      phonetic = "/ˈfiːdbæk/",
      spanishTranslation = "retroalimentación / comentarios",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "noun",
          "Helpful comments about how well you did something.",
          "Instant feedback helps you fix small mistakes fast.",
        ),
      ),
      relatedWords = listOf("advice", "review", "critique", "guidance"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "luggage",
      phonetic = "/ˈlʌɡɪdʒ/",
      spanishTranslation = "equipaje",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "noun",
          "Suitcases and bags packed for a trip.",
          "You can check your luggage at counter four.",
        ),
      ),
      relatedWords = listOf("baggage", "suitcase", "carry-on", "backpack"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "curious",
      phonetic = "/ˈkjʊriəs/",
      spanishTranslation = "curioso / con ganas de saber",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "adjective",
          "Eager to learn or know more about something.",
          "Staying curious keeps conversations interesting.",
        ),
      ),
      relatedWords = listOf("inquisitive", "interested", "eager"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "deadline",
      phonetic = "/ˈdedlaɪn/",
      spanishTranslation = "fecha límite",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "noun",
          "The latest time or date by which something must be finished.",
          "We finished the presentation ahead of the deadline.",
        ),
      ),
      relatedWords = listOf("due date", "target", "limit"),
      isFromLiveWeb = false,
    ),
    DictionaryEntryResult(
      word = "reliable",
      phonetic = "/rɪˈlaɪəbəl/",
      spanishTranslation = "confiable",
      audioUrl = null,
      meanings = listOf(
        DictionaryMeaning(
          "adjective",
          "Consistently good in quality or performance; able to be trusted.",
          "She is the most reliable teammate on our squad.",
        ),
      ),
      relatedWords = listOf("dependable", "trustworthy", "solid"),
      isFromLiveWeb = false,
    ),
  ).associateBy { it.word }
}
