package com.example.englishapp.edu

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Base de datos SQLite unificada en Android (`fluentvoice_edu.db`).
 * Soporta migraciones versionadas con respaldo automático y almacena catálogo propio/abierto,
 * proveedores, licencias, atribuciones, favoritos, progreso, actividad, historial y sync.
 */
class EduDatabaseHelper(private val context: Context) :
  SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
  }

  override fun onConfigure(db: SQLiteDatabase) {
    super.onConfigure(db)
    db.setForeignKeyConstraintsEnabled(true)
    db.enableWriteAheadLogging()
  }

  override fun onCreate(db: SQLiteDatabase) {
    applyV1Schema(db)
    applyV2Indexes(db)
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    createDatabaseBackupFile("pre_v$newVersion")
    if (oldVersion < 2) {
      applyV2Indexes(db)
    }
  }

  fun createDatabaseBackupFile(tag: String = "manual"): File? {
    val dbFile = context.getDatabasePath(DB_NAME)
    if (!dbFile.exists() || dbFile.length() == 0L) return null
    val backupDir = File(context.filesDir, "edu_backups").apply { mkdirs() }
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outFile = File(backupDir, "fluentvoice_edu_${tag}_$ts.db.bak")
    runCatching { dbFile.copyTo(outFile, overwrite = true) }.getOrNull() ?: return null
    return outFile
  }

  private fun applyV1Schema(db: SQLiteDatabase) {
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS cefr_levels (
        code TEXT PRIMARY KEY,
        level_id TEXT NOT NULL,
        rank INTEGER NOT NULL,
        label_es TEXT NOT NULL,
        description_es TEXT NOT NULL,
        speech_rate REAL NOT NULL
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS educational_categories (
        id TEXT PRIMARY KEY,
        slug TEXT NOT NULL,
        name_en TEXT NOT NULL,
        name_es TEXT NOT NULL,
        description_es TEXT NOT NULL,
        icon_name TEXT NOT NULL,
        display_order INTEGER NOT NULL
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS content_licenses (
        id TEXT PRIMARY KEY,
        spdx_code TEXT NOT NULL,
        name TEXT NOT NULL,
        url TEXT NOT NULL,
        allows_storage INTEGER NOT NULL DEFAULT 1,
        allows_commercial INTEGER NOT NULL DEFAULT 0,
        requires_attribution INTEGER NOT NULL DEFAULT 1,
        requires_share_alike INTEGER NOT NULL DEFAULT 0,
        storage_policy TEXT NOT NULL,
        notes TEXT NOT NULL DEFAULT ''
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS content_providers (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        base_url TEXT NOT NULL,
        integration_mode TEXT NOT NULL,
        default_license_id TEXT NOT NULL,
        requires_auth INTEGER NOT NULL DEFAULT 0,
        rate_limit_rpm INTEGER NOT NULL DEFAULT 20,
        is_enabled INTEGER NOT NULL DEFAULT 1,
        terms_url TEXT NOT NULL DEFAULT '',
        description_es TEXT NOT NULL DEFAULT '',
        last_sync_at TEXT,
        last_status TEXT NOT NULL DEFAULT 'ready'
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS content_attributions (
        id TEXT PRIMARY KEY,
        provider_id TEXT NOT NULL,
        license_id TEXT NOT NULL,
        author TEXT NOT NULL,
        attribution_text TEXT NOT NULL,
        source_url TEXT NOT NULL,
        created_at TEXT NOT NULL
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS educational_resources (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        title_es TEXT NOT NULL,
        description TEXT NOT NULL,
        language TEXT NOT NULL DEFAULT 'en',
        cefr_level TEXT NOT NULL,
        category_id TEXT NOT NULL,
        primary_skill TEXT NOT NULL,
        secondary_skills_json TEXT NOT NULL DEFAULT '[]',
        topic TEXT NOT NULL DEFAULT 'general_english',
        difficulty INTEGER NOT NULL DEFAULT 3,
        estimated_duration_min INTEGER NOT NULL DEFAULT 5,
        source_type TEXT NOT NULL,
        provider_id TEXT NOT NULL,
        author TEXT NOT NULL,
        original_url TEXT NOT NULL DEFAULT '',
        content_type TEXT NOT NULL,
        license_id TEXT NOT NULL,
        attribution_id TEXT,
        added_at TEXT NOT NULL,
        last_verified_at TEXT NOT NULL,
        availability_status TEXT NOT NULL,
        validation_status TEXT NOT NULL,
        cefr_confidence REAL NOT NULL DEFAULT 1.0,
        cefr_classification_source TEXT NOT NULL DEFAULT 'curated_internal',
        learning_objective_es TEXT NOT NULL DEFAULT '',
        exercise_type TEXT NOT NULL DEFAULT 'practice',
        offline_compatible INTEGER NOT NULL DEFAULT 1,
        entity_table TEXT NOT NULL DEFAULT 'educational_resources',
        entity_id TEXT NOT NULL DEFAULT '',
        updated_at TEXT NOT NULL,
        payload_json TEXT NOT NULL DEFAULT '{}'
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS user_favorites (
        resource_id TEXT PRIMARY KEY,
        created_at TEXT NOT NULL
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS user_progress (
        resource_id TEXT PRIMARY KEY,
        status TEXT NOT NULL,
        score REAL NOT NULL DEFAULT 0.0,
        attempts INTEGER NOT NULL DEFAULT 0,
        time_spent_sec INTEGER NOT NULL DEFAULT 0,
        last_position TEXT NOT NULL DEFAULT '',
        error_tags_json TEXT NOT NULL DEFAULT '[]',
        completed_at TEXT,
        updated_at TEXT NOT NULL
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS content_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        resource_id TEXT NOT NULL,
        action TEXT NOT NULL,
        duration_sec INTEGER NOT NULL DEFAULT 0,
        score REAL,
        created_at TEXT NOT NULL
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS user_activity (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        activity_date TEXT NOT NULL,
        skill TEXT NOT NULL,
        cefr_level TEXT NOT NULL,
        minutes_practiced INTEGER NOT NULL DEFAULT 1,
        xp_earned INTEGER NOT NULL DEFAULT 0,
        resources_completed INTEGER NOT NULL DEFAULT 0,
        accuracy_pct REAL NOT NULL DEFAULT 0.0,
        created_at TEXT NOT NULL
      );
      """.trimIndent(),
    )
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS sync_state (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      );
      """.trimIndent(),
    )
  }

  private fun applyV2Indexes(db: SQLiteDatabase) {
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_android_res_level_cat ON educational_resources(cefr_level, category_id);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_android_res_skill ON educational_resources(primary_skill, cefr_level);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_android_res_source ON educational_resources(source_type, availability_status);")
    db.execSQL("CREATE INDEX IF NOT EXISTS idx_android_hist_created ON content_history(created_at DESC);")
  }

  fun needsBundleImport(expectedGeneratedAt: String): Boolean {
    val db = readableDatabase
    db.rawQuery("SELECT value FROM sync_state WHERE key = 'bundle_generated_at';", null).use { c ->
      if (c.moveToFirst()) {
        val current = c.getString(0)
        if (current == expectedGeneratedAt) {
          db.rawQuery("SELECT COUNT(*) FROM educational_resources;", null).use { cnt ->
            if (cnt.moveToFirst() && cnt.getInt(0) > 0) return false
          }
        }
      }
    }
    return true
  }

  fun importCatalogBundle(bundle: EduCatalogBundle) {
    val db = writableDatabase
    db.beginTransaction()
    try {
      bundle.levels.forEach { lvl ->
        db.insertWithOnConflict(
          "cefr_levels",
          null,
          ContentValues().apply {
            put("code", lvl.code)
            put("level_id", lvl.levelId)
            put("rank", lvl.rank)
            put("label_es", lvl.labelEs)
            put("description_es", lvl.descriptionEs)
            put("speech_rate", lvl.speechRate)
          },
          SQLiteDatabase.CONFLICT_REPLACE,
        )
      }
      bundle.categories.forEach { cat ->
        db.insertWithOnConflict(
          "educational_categories",
          null,
          ContentValues().apply {
            put("id", cat.id)
            put("slug", cat.slug)
            put("name_en", cat.nameEn)
            put("name_es", cat.nameEs)
            put("description_es", cat.descriptionEs)
            put("icon_name", cat.iconName)
            put("display_order", cat.displayOrder)
          },
          SQLiteDatabase.CONFLICT_REPLACE,
        )
      }
      bundle.licenses.forEach { upsertLicense(db, it) }
      bundle.providers.forEach { upsertProvider(db, it) }
      bundle.attributions.forEach { upsertAttribution(db, it) }
      bundle.resources.forEach { upsertResource(db, it) }

      db.insertWithOnConflict(
        "sync_state",
        null,
        ContentValues().apply {
          put("key", "bundle_generated_at")
          put("value", bundle.generatedAt)
        },
        SQLiteDatabase.CONFLICT_REPLACE,
      )
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  fun applyIncrementalSync(
    resources: List<EduResource>,
    providers: List<EduProvider>,
    licenses: List<EduLicense>,
    attributions: List<EduAttribution>,
    serverTime: String,
  ) {
    val db = writableDatabase
    db.beginTransaction()
    try {
      licenses.forEach { upsertLicense(db, it) }
      providers.forEach { upsertProvider(db, it) }
      attributions.forEach { upsertAttribution(db, it) }
      resources.forEach { upsertResource(db, it) }
      db.insertWithOnConflict(
        "sync_state",
        null,
        ContentValues().apply {
          put("key", "last_server_sync")
          put("value", serverTime)
        },
        SQLiteDatabase.CONFLICT_REPLACE,
      )
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  fun getLastServerSync(): String? {
    readableDatabase.rawQuery("SELECT value FROM sync_state WHERE key = 'last_server_sync';", null).use { c ->
      return if (c.moveToFirst()) c.getString(0) else null
    }
  }

  private fun upsertLicense(db: SQLiteDatabase, lic: EduLicense) {
    db.insertWithOnConflict(
      "content_licenses",
      null,
      ContentValues().apply {
        put("id", lic.id)
        put("spdx_code", lic.spdxCode)
        put("name", lic.name)
        put("url", lic.url)
        put("allows_storage", lic.allowsStorage)
        put("allows_commercial", lic.allowsCommercial)
        put("requires_attribution", lic.requiresAttribution)
        put("requires_share_alike", lic.requiresShareAlike)
        put("storage_policy", lic.storagePolicy)
        put("notes", lic.notes)
      },
      SQLiteDatabase.CONFLICT_REPLACE,
    )
  }

  private fun upsertProvider(db: SQLiteDatabase, prov: EduProvider) {
    db.insertWithOnConflict(
      "content_providers",
      null,
      ContentValues().apply {
        put("id", prov.id)
        put("name", prov.name)
        put("base_url", prov.baseUrl)
        put("integration_mode", prov.integrationMode)
        put("default_license_id", prov.defaultLicenseId)
        put("requires_auth", prov.requiresAuth)
        put("rate_limit_rpm", prov.rateLimitRpm)
        put("is_enabled", prov.isEnabled)
        put("terms_url", prov.termsUrl)
        put("description_es", prov.descriptionEs)
        put("last_sync_at", prov.lastSyncAt)
        put("last_status", prov.lastStatus)
      },
      SQLiteDatabase.CONFLICT_REPLACE,
    )
  }

  private fun upsertAttribution(db: SQLiteDatabase, attr: EduAttribution) {
    db.insertWithOnConflict(
      "content_attributions",
      null,
      ContentValues().apply {
        put("id", attr.id)
        put("provider_id", attr.providerId)
        put("license_id", attr.licenseId)
        put("author", attr.author)
        put("attribution_text", attr.attributionText)
        put("source_url", attr.sourceUrl)
        put("created_at", attr.createdAt)
      },
      SQLiteDatabase.CONFLICT_REPLACE,
    )
  }

  private fun upsertResource(db: SQLiteDatabase, r: EduResource) {
    db.insertWithOnConflict(
      "educational_resources",
      null,
      ContentValues().apply {
        put("id", r.id)
        put("title", r.title)
        put("title_es", r.titleEs)
        put("description", r.description)
        put("language", r.language)
        put("cefr_level", r.cefrLevel)
        put("category_id", r.categoryId)
        put("primary_skill", r.primarySkill)
        put("secondary_skills_json", json.encodeToString(r.secondarySkills))
        put("topic", r.topic)
        put("difficulty", r.difficulty)
        put("estimated_duration_min", r.estimatedDurationMin)
        put("source_type", r.sourceType)
        put("provider_id", r.providerId)
        put("author", r.author)
        put("original_url", r.originalUrl)
        put("content_type", r.contentType)
        put("license_id", r.licenseId)
        put("attribution_id", r.attributionId)
        put("added_at", r.addedAt)
        put("last_verified_at", r.lastVerifiedAt)
        put("availability_status", r.availabilityStatus)
        put("validation_status", r.validationStatus)
        put("cefr_confidence", r.cefrConfidence)
        put("cefr_classification_source", r.cefrClassificationSource)
        put("learning_objective_es", r.learningObjectiveEs)
        put("exercise_type", r.exerciseType)
        put("offline_compatible", if (r.offlineCompatible) 1 else 0)
        put("entity_table", r.entityTable)
        put("entity_id", r.entityId)
        put("updated_at", r.updatedAt)
        put("payload_json", r.payloadJson)
      },
      SQLiteDatabase.CONFLICT_REPLACE,
    )
  }

  fun loadLevels(): List<EduCefrLevel> {
    val list = mutableListOf<EduCefrLevel>()
    readableDatabase.rawQuery("SELECT * FROM cefr_levels ORDER BY rank ASC;", null).use { c ->
      while (c.moveToNext()) {
        list.add(
          EduCefrLevel(
            code = c.getString(0),
            levelId = c.getString(1),
            rank = c.getInt(2),
            labelEs = c.getString(3),
            descriptionEs = c.getString(4),
            speechRate = c.getFloat(5),
          ),
        )
      }
    }
    return list
  }

  fun loadCategories(): List<EduCategory> {
    val list = mutableListOf<EduCategory>()
    readableDatabase.rawQuery("SELECT * FROM educational_categories ORDER BY display_order ASC;", null).use { c ->
      while (c.moveToNext()) {
        list.add(
          EduCategory(
            id = c.getString(0),
            slug = c.getString(1),
            nameEn = c.getString(2),
            nameEs = c.getString(3),
            descriptionEs = c.getString(4),
            iconName = c.getString(5),
            displayOrder = c.getInt(6),
          ),
        )
      }
    }
    return list
  }

  fun loadLicenses(): List<EduLicense> {
    val list = mutableListOf<EduLicense>()
    readableDatabase.rawQuery("SELECT * FROM content_licenses;", null).use { c ->
      while (c.moveToNext()) {
        list.add(
          EduLicense(
            id = c.getString(0),
            spdxCode = c.getString(1),
            name = c.getString(2),
            url = c.getString(3),
            allowsStorage = c.getInt(4),
            allowsCommercial = c.getInt(5),
            requiresAttribution = c.getInt(6),
            requiresShareAlike = c.getInt(7),
            storagePolicy = c.getString(8),
            notes = c.getString(9),
          ),
        )
      }
    }
    return list
  }

  fun loadProviders(): List<EduProvider> {
    val list = mutableListOf<EduProvider>()
    readableDatabase.rawQuery("SELECT * FROM content_providers ORDER BY name ASC;", null).use { c ->
      while (c.moveToNext()) {
        list.add(
          EduProvider(
            id = c.getString(0),
            name = c.getString(1),
            baseUrl = c.getString(2),
            integrationMode = c.getString(3),
            defaultLicenseId = c.getString(4),
            requiresAuth = c.getInt(5),
            rateLimitRpm = c.getInt(6),
            isEnabled = c.getInt(7),
            termsUrl = c.getString(8),
            descriptionEs = c.getString(9),
            lastSyncAt = c.getString(10),
            lastStatus = c.getString(11),
          ),
        )
      }
    }
    return list
  }

  fun loadAttributions(): List<EduAttribution> {
    val list = mutableListOf<EduAttribution>()
    readableDatabase.rawQuery("SELECT * FROM content_attributions;", null).use { c ->
      while (c.moveToNext()) {
        list.add(
          EduAttribution(
            id = c.getString(0),
            providerId = c.getString(1),
            licenseId = c.getString(2),
            author = c.getString(3),
            attributionText = c.getString(4),
            sourceUrl = c.getString(5),
            createdAt = c.getString(6),
          ),
        )
      }
    }
    return list
  }

  fun loadAllResources(): List<EduResource> {
    val list = mutableListOf<EduResource>()
    val sql =
      """
      SELECT id, title, title_es, description, language, cefr_level, category_id,
             primary_skill, secondary_skills_json, topic, difficulty, estimated_duration_min,
             source_type, provider_id, author, original_url, content_type, license_id,
             attribution_id, added_at, last_verified_at, availability_status, validation_status,
             cefr_confidence, cefr_classification_source, learning_objective_es, exercise_type,
             offline_compatible, entity_table, entity_id, updated_at, payload_json
      FROM educational_resources
      ORDER BY CASE WHEN content_type = 'vocabulary' THEN 1 ELSE 0 END,
               CASE cefr_level WHEN 'A1' THEN 1 WHEN 'A2' THEN 2 WHEN 'B1' THEN 3 WHEN 'B2' THEN 4 WHEN 'C1' THEN 5 ELSE 6 END,
               difficulty ASC, id ASC;
      """.trimIndent()
    readableDatabase.rawQuery(sql, null).use { c ->
      while (c.moveToNext()) {
        val sec = runCatching { json.decodeFromString<List<String>>(c.getString(8)) }.getOrDefault(emptyList())
        list.add(
          EduResource(
            id = c.getString(0),
            title = c.getString(1),
            titleEs = c.getString(2),
            description = c.getString(3),
            language = c.getString(4),
            cefrLevel = c.getString(5),
            categoryId = c.getString(6),
            primarySkill = c.getString(7),
            secondarySkills = sec,
            topic = c.getString(9),
            difficulty = c.getInt(10),
            estimatedDurationMin = c.getInt(11),
            sourceType = c.getString(12),
            providerId = c.getString(13),
            author = c.getString(14),
            originalUrl = c.getString(15),
            contentType = c.getString(16),
            licenseId = c.getString(17),
            attributionId = c.getString(18),
            addedAt = c.getString(19),
            lastVerifiedAt = c.getString(20),
            availabilityStatus = c.getString(21),
            validationStatus = c.getString(22),
            cefrConfidence = c.getDouble(23),
            cefrClassificationSource = c.getString(24),
            learningObjectiveEs = c.getString(25),
            exerciseType = c.getString(26),
            offlineCompatible = c.getInt(27) == 1,
            entityTable = c.getString(28),
            entityId = c.getString(29),
            updatedAt = c.getString(30),
            payloadJson = c.getString(31),
          ),
        )
      }
    }
    return list
  }

  fun loadFavoriteIds(): Set<String> {
    val set = linkedSetOf<String>()
    readableDatabase.rawQuery("SELECT resource_id FROM user_favorites ORDER BY created_at DESC;", null).use { c ->
      while (c.moveToNext()) set.add(c.getString(0))
    }
    return set
  }

  fun toggleFavorite(resourceId: String): Boolean {
    val db = writableDatabase
    val exists = db.rawQuery("SELECT 1 FROM user_favorites WHERE resource_id = ?;", arrayOf(resourceId)).use {
      it.moveToFirst()
    }
    val now = isoNow()
    return if (exists) {
      db.delete("user_favorites", "resource_id = ?", arrayOf(resourceId))
      false
    } else {
      db.insertWithOnConflict(
        "user_favorites",
        null,
        ContentValues().apply {
          put("resource_id", resourceId)
          put("created_at", now)
        },
        SQLiteDatabase.CONFLICT_REPLACE,
      )
      recordHistory(resourceId, "favorited", 0, null)
      true
    }
  }

  fun mergeRemoteFavorites(remoteIds: Set<String>) {
    if (remoteIds.isEmpty()) return
    val db = writableDatabase
    val now = isoNow()
    db.beginTransaction()
    try {
      remoteIds.forEach { id ->
        db.insertWithOnConflict(
          "user_favorites",
          null,
          ContentValues().apply {
            put("resource_id", id)
            put("created_at", now)
          },
          SQLiteDatabase.CONFLICT_IGNORE,
        )
      }
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  fun mergeRemoteProgress(remoteRecords: List<EduUserProgressRecord>) {
    if (remoteRecords.isEmpty()) return
    val db = writableDatabase
    val existing = loadProgressMap()
    db.beginTransaction()
    try {
      remoteRecords.forEach { rec ->
        val local = existing[rec.resourceId]
        val shouldApply = local == null || rec.updatedAt > local.updatedAt
        if (shouldApply) {
          db.insertWithOnConflict(
            "user_progress",
            null,
            ContentValues().apply {
              put("resource_id", rec.resourceId)
              put("status", rec.status)
              put("score", rec.score)
              put("attempts", rec.attempts.coerceAtLeast(local?.attempts ?: 1))
              put("time_spent_sec", rec.timeSpentSec.coerceAtLeast(local?.timeSpentSec ?: 0))
              put("last_position", rec.lastPosition)
              put("error_tags_json", json.encodeToString(rec.errorTags))
              put("completed_at", rec.completedAt)
              put("updated_at", rec.updatedAt.ifBlank { isoNow() })
            },
            SQLiteDatabase.CONFLICT_REPLACE,
          )
        }
      }
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  fun loadProgressMap(): Map<String, EduUserProgressRecord> {
    val map = mutableMapOf<String, EduUserProgressRecord>()
    readableDatabase.rawQuery(
      "SELECT resource_id, status, score, attempts, time_spent_sec, last_position, error_tags_json, completed_at, updated_at FROM user_progress;",
      null,
    ).use { c ->
      while (c.moveToNext()) {
        val tags = runCatching { json.decodeFromString<List<String>>(c.getString(6)) }.getOrDefault(emptyList())
        val rid = c.getString(0)
        map[rid] = EduUserProgressRecord(
          resourceId = rid,
          status = c.getString(1),
          score = c.getFloat(2),
          attempts = c.getInt(3),
          timeSpentSec = c.getInt(4),
          lastPosition = c.getString(5),
          errorTags = tags,
          completedAt = c.getString(7),
          updatedAt = c.getString(8),
        )
      }
    }
    return map
  }

  fun upsertProgress(
    resourceId: String,
    status: String,
    score: Float,
    timeSpentSec: Int,
    lastPosition: String = "",
    errorTags: List<String> = emptyList(),
    cefrLevel: String = "A2",
    skill: String = "reading",
    xpEarned: Int = 15,
  ) {
    val db = writableDatabase
    val now = isoNow()
    val existing = db.rawQuery(
      "SELECT attempts, time_spent_sec, completed_at FROM user_progress WHERE resource_id = ?;",
      arrayOf(resourceId),
    ).use { c ->
      if (c.moveToFirst()) Triple(c.getInt(0), c.getInt(1), c.getString(2)) else Triple(0, 0, null)
    }
    val completedAt = if (status == "completed" || status == "mastered") now else existing.third
    db.beginTransaction()
    try {
      db.insertWithOnConflict(
        "user_progress",
        null,
        ContentValues().apply {
          put("resource_id", resourceId)
          put("status", status)
          put("score", score)
          put("attempts", existing.first + 1)
          put("time_spent_sec", existing.second + timeSpentSec)
          put("last_position", lastPosition)
          put("error_tags_json", json.encodeToString(errorTags))
          put("completed_at", completedAt)
          put("updated_at", now)
        },
        SQLiteDatabase.CONFLICT_REPLACE,
      )
      db.insert(
        "content_history",
        null,
        ContentValues().apply {
          put("resource_id", resourceId)
          put("action", if (status == "completed" || status == "mastered") "completed" else "practiced")
          put("duration_sec", timeSpentSec)
          put("score", score)
          put("created_at", now)
        },
      )
      db.insert(
        "user_activity",
        null,
        ContentValues().apply {
          put("activity_date", now.take(10))
          put("skill", skill)
          put("cefr_level", cefrLevel)
          put("minutes_practiced", (timeSpentSec / 60).coerceAtLeast(1))
          put("xp_earned", xpEarned)
          put("resources_completed", if (status == "completed" || status == "mastered") 1 else 0)
          put("accuracy_pct", score)
          put("created_at", now)
        },
      )
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  fun recordHistory(resourceId: String, action: String, durationSec: Int = 0, score: Float? = null) {
    writableDatabase.insert(
      "content_history",
      null,
      ContentValues().apply {
        put("resource_id", resourceId)
        put("action", action)
        put("duration_sec", durationSec)
        if (score != null) put("score", score) else putNull("score")
        put("created_at", isoNow())
      },
    )
  }

  fun loadRecentHistory(limit: Int = 35): List<EduHistoryItem> {
    val list = mutableListOf<EduHistoryItem>()
    readableDatabase.rawQuery(
      "SELECT id, resource_id, action, duration_sec, score, created_at FROM content_history ORDER BY created_at DESC, id DESC LIMIT ?;",
      arrayOf(limit.toString()),
    ).use { c ->
      while (c.moveToNext()) {
        val score = if (c.isNull(4)) null else c.getFloat(4)
        list.add(
          EduHistoryItem(
            id = c.getLong(0),
            resourceId = c.getString(1),
            action = c.getString(2),
            durationSec = c.getInt(3),
            score = score,
            createdAt = c.getString(5),
          ),
        )
      }
    }
    return list
  }

  private fun isoNow(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

  companion object {
    const val DB_NAME = "fluentvoice_edu.db"
    const val DB_VERSION = 2
  }
}
