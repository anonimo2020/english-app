package com.example.englishapp.data

import android.content.Context
import android.content.SharedPreferences
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.DailyActivity
import com.example.englishapp.model.SavedPhrase
import com.example.englishapp.model.UserProgress
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AppPreferencesStore(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun loadSettings(): AppSettings {
    val raw = prefs.getString(KEY_SETTINGS, null) ?: return AppSettings()
    return runCatching { json.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
  }

  fun saveSettings(settings: AppSettings) {
    prefs.edit().putString(KEY_SETTINGS, json.encodeToString(settings)).apply()
  }

  fun loadProgress(): UserProgress {
    val raw = prefs.getString(KEY_PROGRESS, null)
    val loaded = if (raw != null) {
      runCatching { json.decodeFromString<UserProgress>(raw) }.getOrNull()
    } else {
      null
    }
    return normalizeProgressForToday(loaded ?: UserProgress())
  }

  fun saveProgress(progress: UserProgress) {
    prefs.edit().putString(KEY_PROGRESS, json.encodeToString(progress)).apply()
  }

  fun loadSavedPhrases(): List<SavedPhrase> {
    val raw = prefs.getString(KEY_PHRASES, null)
    if (raw != null) {
      return runCatching { json.decodeFromString<List<SavedPhrase>>(raw) }
        .getOrDefault(emptyList())
    }
    if (!prefs.getBoolean(KEY_PHRASES_SEEDED, false)) {
      val starter = starterPhrases()
      prefs.edit()
        .putBoolean(KEY_PHRASES_SEEDED, true)
        .putString(KEY_PHRASES, json.encodeToString(starter))
        .apply()
      return starter
    }
    return emptyList()
  }

  fun savePhrases(phrases: List<SavedPhrase>) {
    prefs.edit()
      .putBoolean(KEY_PHRASES_SEEDED, true)
      .putString(KEY_PHRASES, json.encodeToString(phrases))
      .apply()
  }

  fun normalizeProgressForToday(progress: UserProgress): UserProgress {
    val todayKey = DateHelper.todayKey()
    val rollingKeys = DateHelper.lastSevenDays()
    val byDate = progress.weeklyHistory.associateBy { it.dateKey }
    val rebuiltHistory = rollingKeys.map { (key, label) ->
      val existing = byDate[key]
      DailyActivity(
        dateKey = key,
        dayLabel = label,
        xp = existing?.xp ?: 0,
        spokenTurns = existing?.spokenTurns ?: 0,
      )
    }

    val updatedStreak = when {
      progress.lastActiveDate.isBlank() -> 0
      progress.lastActiveDate == todayKey -> progress.streakDays.coerceAtLeast(1)
      progress.lastActiveDate == DateHelper.yesterdayKey() -> progress.streakDays.coerceAtLeast(1)
      else -> 0
    }

    val todayXp = if (progress.lastActiveDate == todayKey) {
      rebuiltHistory.lastOrNull()?.xp ?: progress.todayXp
    } else {
      0
    }

    return progress.copy(
      streakDays = updatedStreak,
      todayXp = todayXp,
      weeklyHistory = rebuiltHistory,
    )
  }

  fun applyTurnXp(current: UserProgress, xpDelta: Int, spokenTurnIncrement: Int): UserProgress {
    val base = normalizeProgressForToday(current)
    val todayKey = DateHelper.todayKey()
    val yesterdayKey = DateHelper.yesterdayKey()

    val newStreak = when {
      base.lastActiveDate == todayKey -> base.streakDays.coerceAtLeast(1)
      base.lastActiveDate == yesterdayKey -> base.streakDays + 1
      else -> 1
    }

    val updatedHistory = base.weeklyHistory.map { day ->
      if (day.dateKey == todayKey) {
        day.copy(
          xp = day.xp + xpDelta,
          spokenTurns = day.spokenTurns + spokenTurnIncrement,
        )
      } else {
        day
      }
    }

    return base.copy(
      totalXp = base.totalXp + xpDelta,
      todayXp = base.todayXp + xpDelta,
      streakDays = newStreak,
      lastActiveDate = todayKey,
      totalSpokenTurns = base.totalSpokenTurns + spokenTurnIncrement,
      weeklyHistory = updatedHistory,
    )
  }

  fun applySessionCompleted(
    current: UserProgress,
    bonusXp: Int,
    scenarioId: String? = null,
    markScenarioCompleted: Boolean = true,
  ): UserProgress {
    val withXp = applyTurnXp(current, bonusXp, spokenTurnIncrement = 0)
    val completedSet = if (markScenarioCompleted && !scenarioId.isNullOrBlank()) {
      withXp.completedScenarioIds + scenarioId
    } else {
      withXp.completedScenarioIds
    }
    return withXp.copy(
      sessionsCompleted = withXp.sessionsCompleted + 1,
      completedScenarioIds = completedSet,
    )
  }

  fun applyLessonCompleted(current: UserProgress, lessonId: String, bonusXp: Int = 35): UserProgress {
    val withXp = applyTurnXp(current, bonusXp, spokenTurnIncrement = 1)
    return withXp.copy(completedLessonIds = withXp.completedLessonIds + lessonId)
  }

  fun applyDrillMastered(current: UserProgress, drillId: String, bonusXp: Int = 20): UserProgress {
    val withXp = applyTurnXp(current, bonusXp, spokenTurnIncrement = 1)
    return withXp.copy(masteredDrillIds = withXp.masteredDrillIds + drillId)
  }

  fun applyVocabMastered(current: UserProgress, wordIds: Collection<String>, bonusXp: Int): UserProgress {
    val withXp = applyTurnXp(current, bonusXp.coerceAtLeast(5), spokenTurnIncrement = 0)
    return withXp.copy(masteredVocabIds = withXp.masteredVocabIds + wordIds)
  }

  private fun starterPhrases(): List<SavedPhrase> = listOf(
    SavedPhrase(
      id = "seed-1",
      english = "Could I have a medium iced latte, please?",
      spanish = "¿Me podría dar un latte frío mediano, por favor?",
      note = "Sonará súper cortés en cualquier cafetería o restaurante.",
      source = "Cafetería",
    ),
    SavedPhrase(
      id = "seed-2",
      english = "I am twenty-eight years old and I work in design.",
      spanish = "Tengo veintiocho años y trabajo en diseño.",
      note = "Recuerda usar 'I am ... years old', nunca 'I have'.",
      source = "Gramática clave",
    ),
    SavedPhrase(
      id = "seed-3",
      english = "Could you speak a little more slowly, please?",
      spanish = "¿Podrías hablar un poco más despacio, por favor?",
      note = "Frase salvavidas en viajes, llamadas y entrevistas.",
      source = "Supervivencia",
    ),
  )

  private object DateHelper {
    private fun formatter(): SimpleDateFormat =
      SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayKey(): String = formatter().format(Calendar.getInstance().time)

    fun yesterdayKey(): String {
      val cal = Calendar.getInstance()
      cal.add(Calendar.DAY_OF_YEAR, -1)
      return formatter().format(cal.time)
    }

    fun lastSevenDays(): List<Pair<String, String>> {
      val fmt = formatter()
      val list = mutableListOf<Pair<String, String>>()
      for (offset in -6..0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offset)
        val key = fmt.format(cal.time)
        val label = when (cal.get(Calendar.DAY_OF_WEEK)) {
          Calendar.MONDAY -> "Lun"
          Calendar.TUESDAY -> "Mar"
          Calendar.WEDNESDAY -> "Mié"
          Calendar.THURSDAY -> "Jue"
          Calendar.FRIDAY -> "Vie"
          Calendar.SATURDAY -> "Sáb"
          else -> "Dom"
        }
        list.add(key to label)
      }
      return list
    }
  }

  companion object {
    private const val PREFS_NAME = "fluentvoice_prefs_v1"
    private const val KEY_SETTINGS = "settings_json"
    private const val KEY_PROGRESS = "progress_json"
    private const val KEY_PHRASES = "phrases_json"
    private const val KEY_PHRASES_SEEDED = "phrases_seeded"
  }
}
