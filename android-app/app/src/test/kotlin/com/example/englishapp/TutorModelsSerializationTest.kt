package com.example.englishapp

import com.example.englishapp.chat.ChatClient
import com.example.englishapp.data.FreeWebContentClient
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.DailyActivity
import com.example.englishapp.model.EnglishAccent
import com.example.englishapp.model.LearnerLevel
import com.example.englishapp.model.PlacementResult
import com.example.englishapp.model.SavedPhrase
import com.example.englishapp.model.SkillScores
import com.example.englishapp.model.ThemePreference
import com.example.englishapp.model.TutorCharacter
import com.example.englishapp.model.UserProgress
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorModelsSerializationTest {

  private val json = ChatClient.json

  @Test
  fun roundTripAppSettingsProgressAndPhrases() {
    val settings = AppSettings(
      offlineDemoMode = false,
      backendUrl = "https://english-tutor-api.onrender.com",
      appToken = "tok-123",
      selectedCharacter = TutorCharacter.EDDY,
      selectedLevel = LearnerLevel.INTERMEDIO,
      accent = EnglishAccent.UK,
      speechRateOverride = 0.85f,
      autoPlayReplies = false,
      autoListenHandsFree = true,
      showTranslationsByDefault = true,
      themePreference = ThemePreference.LIGHT,
      hasCompletedOnboarding = true,
      selfEstimatedLevel = LearnerLevel.JUNIOR,
      verifiedLevel = LearnerLevel.INTERMEDIO,
      placementResult = PlacementResult(
        estimatedLevel = LearnerLevel.JUNIOR,
        verifiedLevel = LearnerLevel.INTERMEDIO,
        overallScore = 82,
        skills = SkillScores(80, 84, 81, 82),
        recommendedFocusEs = "Enfócate en fluidez B1.",
        completedAtMs = 1700000000000L,
      ),
      preferFaceToFaceMode = true,
    )
    val decodedSettings = json.decodeFromString<AppSettings>(json.encodeToString(settings))
    assertEquals(settings, decodedSettings)

    val progress = UserProgress(
      totalXp = 240,
      streakDays = 4,
      lastActiveDate = "2026-11-21",
      sessionsCompleted = 5,
      totalSpokenTurns = 19,
      todayXp = 55,
      dailyGoalXp = 100,
      weeklyHistory = listOf(
        DailyActivity("2026-11-21", "Sáb", xp = 55, spokenTurns = 4),
      ),
      completedScenarioIds = setOf("coffee_shop", "free_chat"),
      completedLessonIds = setOf("lesson_a1_1", "lesson_a2_1"),
      masteredDrillIds = setOf("drill_th_1"),
      masteredVocabIds = setOf("v_a1_01", "v_c2_48"),
    )
    val decodedProgress = json.decodeFromString<UserProgress>(json.encodeToString(progress))
    assertEquals(progress, decodedProgress)

    // Verify new B2 and C2 levels serialize/deserialize cleanly
    for (level in listOf(LearnerLevel.INTERMEDIO_ALTO, LearnerLevel.MAESTRIA)) {
      val levelSettings = settings.copy(selectedLevel = level, verifiedLevel = level)
      val roundTrip = json.decodeFromString<AppSettings>(json.encodeToString(levelSettings))
      assertEquals(level, roundTrip.selectedLevel)
      assertEquals(level, roundTrip.verifiedLevel)
    }

    val phrases = listOf(
      SavedPhrase(
        id = "p1",
        english = "Could I have a coffee, please?",
        spanish = "¿Me da un café, por favor?",
        note = "Cortesía",
        source = "Cafetería",
        createdAtMs = 1700000000000L,
      ),
    )
    val decodedPhrases = json.decodeFromString<List<SavedPhrase>>(json.encodeToString(phrases))
    assertEquals(phrases, decodedPhrases)
  }

  @Test
  fun parsesBothFullAndLegacyBackendReplies() {
    val fullPayload = """
      {
        "reply": "Sounds great! What time does your flight leave?",
        "translation_es": "¡Suena genial! ¿A qué hora sale tu vuelo?",
        "coach_note_es": "Para hablar de vuelos programados en el futuro cercano, usa presente simple o 'What time does... leave?'.",
        "correction": {
          "better": "I am flying to London tomorrow.",
          "why": "Usa presente continuo para planes cercanos.",
          "tip": "I'm flying..."
        },
        "suggested_replies": ["It leaves at ten a.m.", "Around noon."],
        "pronunciation_focus": [
          {"word": "flight", "phonetic": "/flaɪt/", "tip_es": "La GH es muda."}
        ],
        "xp_earned": 18
      }
    """.trimIndent()

    val full = ChatClient.parseReplyJson(fullPayload)
    assertEquals("Sounds great! What time does your flight leave?", full.reply)
    assertEquals("¡Suena genial! ¿A qué hora sale tu vuelo?", full.translationEs)
    assertEquals(
      "Para hablar de vuelos programados en el futuro cercano, usa presente simple o 'What time does... leave?'.",
      full.coachNoteEs,
    )
    assertNotNull(full.correction)
    assertEquals("I am flying to London tomorrow.", full.correction?.better)
    assertEquals(listOf("It leaves at ten a.m.", "Around noon."), full.suggestedReplies)
    assertEquals(1, full.pronunciationFocus.size)
    assertEquals(18, full.xpEarned)

    val legacyPayload = """{"reply": "Nice! Tell me more.", "correction": null}"""
    val legacy = ChatClient.parseReplyJson(legacyPayload)
    assertEquals("Nice! Tell me more.", legacy.reply)
    assertNull(legacy.translationEs)
    assertNull(legacy.correction)
    assertTrue(legacy.suggestedReplies.isEmpty())
    assertTrue(legacy.pronunciationFocus.isEmpty())
    assertEquals(12, legacy.xpEarned)
  }

  @Test
  fun parsesFreeWebApiPayloads() {
    val dictJson = """
      [
        {
          "word": "fluent",
          "phonetic": "/ˈfluːənt/",
          "phonetics": [
            {"text": "/ˈfluənt/", "audio": "//api.dictionaryapi.dev/media/pronunciations/en/fluent-us.mp3"}
          ],
          "meanings": [
            {
              "partOfSpeech": "adjective",
              "definitions": [
                {"definition": "Able to speak a language easily and accurately.", "example": "She is fluent in English."}
              ]
            }
          ]
        }
      ]
    """.trimIndent()
    val parsedDict = FreeWebContentClient.parseDictionaryJson(dictJson)
    assertNotNull(parsedDict)
    assertEquals("fluent", parsedDict?.word)
    assertEquals("https://api.dictionaryapi.dev/media/pronunciations/en/fluent-us.mp3", parsedDict?.audioUrl)
    assertEquals(1, parsedDict?.meanings?.size)

    val transJson = """{"responseData": {"translatedText": "fluido"}}"""
    assertEquals("fluido", FreeWebContentClient.parseMyMemoryJson(transJson))

    val datamuseJson = """[{"word": "articulate", "score": 100}, {"word": "eloquent", "score": 90}]"""
    assertEquals(listOf("articulate", "eloquent"), FreeWebContentClient.parseDatamuseJson(datamuseJson))
  }
}
