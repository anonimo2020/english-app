package com.example.englishapp.edu

import android.content.Context
import com.example.englishapp.data.TutorRepository
import com.example.englishapp.model.AppSettings
import com.example.englishapp.model.UserProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repositorio unificado del Learning Hub en Android.
 * Coordina almacenamiento SQLite offline-first, filtros globales, favoritos, historial,
 * progreso personal, motor de recomendaciones y sincronización incremental con FastAPI.
 */
class EduRepository(
  private val context: Context,
  private val tutorRepository: TutorRepository,
) {
  private val db = EduDatabaseHelper(context)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val _state = MutableStateFlow(LearningHubUiState())
  val state: StateFlow<LearningHubUiState> = _state.asStateFlow()

  private var allResources: List<EduResource> = emptyList()
  private var allHistory: List<EduHistoryItem> = emptyList()
  private var initialized = false

  fun ensureInitialized() {
    if (initialized) return
    initialized = true
    scope.launch {
      loadFromAssetsAndDb()
    }
  }

  private suspend fun loadFromAssetsAndDb() {
    withContext(Dispatchers.IO) {
      val rawBundle = context.assets.open("edu_catalog_bundle.json").bufferedReader().use { it.readText() }
      val bundle = EduSyncClient.json.decodeFromString<EduCatalogBundle>(rawBundle)
      if (db.needsBundleImport(bundle.generatedAt)) {
        db.importCatalogBundle(bundle)
      }
    }
    val initialLevel = tutorRepository.settings.value.activeLevel().cefr
    _state.value = _state.value.copy(selectedLevel = initialLevel)
    reloadSnapshot()
  }

  private suspend fun reloadSnapshot() {
    val snap = withContext(Dispatchers.IO) {
      val levels = db.loadLevels()
      val categories = db.loadCategories()
      val providers = db.loadProviders()
      val licenses = db.loadLicenses().associateBy { it.id }
      val attributions = db.loadAttributions().associateBy { it.id }
      val resources = db.loadAllResources()
      val favorites = db.loadFavoriteIds()
      val progressMap = db.loadProgressMap()
      val history = db.loadRecentHistory(40)
      SnapshotData(levels, categories, providers, licenses, attributions, resources, favorites, progressMap, history)
    }

    allResources = snap.resources
    allHistory = snap.history

    val resById = allResources.associateBy { it.id }
    val cur = _state.value
    val activeSettings = tutorRepository.settings.value
    val activeProgress = tutorRepository.progress.value
    val recentIds = allHistory.map { it.resourceId }.toSet()

    val (path, recs) = EduRecommendationEngine.buildRecommendations(
      resources = allResources,
      settings = activeSettings,
      userProgress = activeProgress,
      hubProgress = snap.progressMap,
      recentResourceIds = recentIds,
      offlineOnly = cur.offlineOnly,
      limit = 10,
    )

    val inProgressList = snap.progressMap.values
      .filter { it.status == "in_progress" }
      .sortedByDescending { it.updatedAt }
      .mapNotNull { resById[it.resourceId] }
    val recentlyViewed = allHistory.mapNotNull { resById[it.resourceId] }
      .filter { res -> snap.progressMap[res.id]?.status !in setOf("completed", "mastered") }
    val continueList = (inProgressList + recentlyViewed).distinctBy { it.id }.take(6)

    val favResources = snap.favorites.mapNotNull { resById[it] }
    val histPairs = allHistory.mapNotNull { h -> resById[h.resourceId]?.let { h to it } }

    _state.value = cur.copy(
      isLoading = false,
      levels = snap.levels,
      categories = snap.categories,
      providers = snap.providers,
      licensesById = snap.licenses,
      attributionsById = snap.attributions,
      allResourcesCount = allResources.size,
      continueLearning = continueList,
      recommendedItems = recs,
      activeLearningPath = path,
      favoriteIds = snap.favorites,
      favoriteResources = favResources,
      recentHistory = histPairs,
      progressByResourceId = snap.progressMap,
    )
    applyFilters()
  }

  fun refreshRecommendations() {
    val cur = _state.value
    if (allResources.isEmpty()) return
    val recentIds = allHistory.map { it.resourceId }.toSet()
    val (path, recs) = EduRecommendationEngine.buildRecommendations(
      resources = allResources,
      settings = tutorRepository.settings.value,
      userProgress = tutorRepository.progress.value,
      hubProgress = cur.progressByResourceId,
      recentResourceIds = recentIds,
      offlineOnly = cur.offlineOnly,
      limit = 10,
    )
    _state.value = cur.copy(activeLearningPath = path, recommendedItems = recs)
  }

  fun updateSearch(query: String) {
    _state.value = _state.value.copy(searchQuery = query)
    applyFilters()
  }

  fun selectLevelFilter(cefrLevel: String?) {
    _state.value = _state.value.copy(selectedLevel = cefrLevel)
    applyFilters()
  }

  fun selectCategoryFilter(categoryId: String?) {
    _state.value = _state.value.copy(selectedCategory = categoryId)
    applyFilters()
  }

  fun selectSkillFilter(skillId: String?) {
    _state.value = _state.value.copy(selectedSkill = skillId)
    applyFilters()
  }

  fun selectSourceTypeFilter(sourceType: String?) {
    _state.value = _state.value.copy(selectedSourceType = sourceType)
    applyFilters()
  }

  fun toggleOfflineOnly() {
    _state.value = _state.value.copy(offlineOnly = !_state.value.offlineOnly)
    applyFilters()
    refreshRecommendations()
  }

  fun resetAllFilters(keepActiveLevel: Boolean = false) {
    val lvl = if (keepActiveLevel) tutorRepository.settings.value.activeLevel().cefr else null
    _state.value = _state.value.copy(
      searchQuery = "",
      selectedLevel = lvl,
      selectedCategory = null,
      selectedSkill = null,
      selectedSourceType = null,
      offlineOnly = false,
    )
    applyFilters()
  }

  private fun applyFilters() {
    val s = _state.value
    val q = s.searchQuery.trim()
    val filtered = allResources.filter { r ->
      (s.selectedLevel == null || r.cefrLevel == s.selectedLevel) &&
        (s.selectedCategory == null || r.categoryId == s.selectedCategory) &&
        (s.selectedSkill == null || r.primarySkill == s.selectedSkill || s.selectedSkill in r.secondarySkills) &&
        (s.selectedSourceType == null || r.sourceType == s.selectedSourceType) &&
        (!s.offlineOnly || r.offlineCompatible) &&
        (q.isBlank() ||
          r.title.contains(q, ignoreCase = true) ||
          r.titleEs.contains(q, ignoreCase = true) ||
          r.description.contains(q, ignoreCase = true) ||
          r.topic.contains(q, ignoreCase = true) ||
          r.author.contains(q, ignoreCase = true) ||
          r.learningObjectiveEs.contains(q, ignoreCase = true))
    }
    _state.value = s.copy(filteredResources = filtered)
  }

  fun resourceById(id: String): EduResource? = allResources.firstOrNull { it.id == id }

  fun toggleFavorite(resourceId: String) {
    scope.launch {
      val nowFavorite = withContext(Dispatchers.IO) { db.toggleFavorite(resourceId) }
      val settings = tutorRepository.settings.value
      if (settings.backendUrl.isNotBlank()) {
        if (nowFavorite) {
          EduSyncClient.pushFavoriteBestEffort(
            baseUrl = settings.backendUrl,
            appToken = settings.appToken,
            resourceId = resourceId,
          )
        } else {
          EduSyncClient.deleteFavoriteBestEffort(
            baseUrl = settings.backendUrl,
            appToken = settings.appToken,
            resourceId = resourceId,
          )
        }
      }
      reloadSnapshot()
    }
  }

  fun recordResourceOpened(resource: EduResource, isExternalLink: Boolean = false) {
    scope.launch {
      withContext(Dispatchers.IO) {
        db.recordHistory(
          resourceId = resource.id,
          action = if (isExternalLink) "external_opened" else "viewed",
          durationSec = 0,
          score = null,
        )
        val existing = db.loadProgressMap()[resource.id]
        if (existing == null && !isExternalLink) {
          db.upsertProgress(
            resourceId = resource.id,
            status = "in_progress",
            score = 0f,
            timeSpentSec = 10,
            cefrLevel = resource.cefrLevel,
            skill = resource.primarySkill,
            xpEarned = 0,
          )
        }
      }
      reloadSnapshot()
    }
  }

  fun recordActivityOutcome(
    resource: EduResource,
    scorePct: Float,
    timeSpentSec: Int,
    errorTags: List<String> = emptyList(),
  ) {
    val cleanScore = scorePct.coerceIn(0f, 100f)
    val status = when {
      cleanScore >= 85f -> "mastered"
      cleanScore >= 60f -> "completed"
      else -> "in_progress"
    }
    val xp = when (status) {
      "mastered" -> 25
      "completed" -> 18
      else -> 8
    }
    scope.launch {
      withContext(Dispatchers.IO) {
        db.upsertProgress(
          resourceId = resource.id,
          status = status,
          score = cleanScore,
          timeSpentSec = timeSpentSec,
          errorTags = errorTags,
          cefrLevel = resource.cefrLevel,
          skill = resource.primarySkill,
          xpEarned = xp,
        )
      }
      tutorRepository.awardXp(xpDelta = xp, spokenTurnIncrement = if (resource.primarySkill == "speaking" || resource.primarySkill == "pronunciation") 1 else 0)
      val settings = tutorRepository.settings.value
      if (settings.backendUrl.isNotBlank()) {
        EduSyncClient.pushProgressBestEffort(
          baseUrl = settings.backendUrl,
          appToken = settings.appToken,
          resourceId = resource.id,
          status = status,
          score = cleanScore,
          timeSpentSec = timeSpentSec,
          errorTags = errorTags,
        )
      }
      reloadSnapshot()
    }
  }

  fun syncWithBackendNow() {
    val settings = tutorRepository.settings.value
    if (settings.backendUrl.isBlank()) {
      _state.value = _state.value.copy(
        syncStatusMessage = "Configura la URL del backend en Perfil/Ajustes para sincronizar nuevos recursos.",
      )
      return
    }
    scope.launch {
      _state.value = _state.value.copy(isSyncing = true, syncStatusMessage = null)
      val since = withContext(Dispatchers.IO) { db.getLastServerSync() }
      val result = EduSyncClient.fetchIncrementalSync(
        baseUrl = settings.backendUrl,
        since = since,
        appToken = settings.appToken,
      )
      result.fold(
        onSuccess = { payload ->
          val sanitizedResources = payload.resources.mapNotNull { it.toEduResource() }
          withContext(Dispatchers.IO) {
            db.applyIncrementalSync(
              resources = sanitizedResources,
              providers = payload.providers,
              licenses = payload.licenses,
              attributions = payload.attributions,
              serverTime = payload.serverTime,
            )
          }

          // Sincronización bidireccional de favoritos y progreso del aprendiz
          val (localFavs, localProgress) = withContext(Dispatchers.IO) {
            db.loadFavoriteIds() to db.loadProgressMap().values.toList()
          }
          localFavs.forEach { favId ->
            EduSyncClient.pushFavoriteBestEffort(settings.backendUrl, settings.appToken, favId)
          }
          localProgress.forEach { prog ->
            EduSyncClient.pushProgressBestEffort(
              baseUrl = settings.backendUrl,
              appToken = settings.appToken,
              resourceId = prog.resourceId,
              status = prog.status,
              score = prog.score,
              timeSpentSec = prog.timeSpentSec,
              errorTags = prog.errorTags,
            )
          }
          val remoteFavs = EduSyncClient.fetchRemoteFavorites(settings.backendUrl, settings.appToken).getOrDefault(emptySet())
          val remoteProg = EduSyncClient.fetchRemoteProgress(settings.backendUrl, settings.appToken).getOrDefault(emptyList())
          withContext(Dispatchers.IO) {
            db.mergeRemoteFavorites(remoteFavs)
            db.mergeRemoteProgress(remoteProg)
          }

          reloadSnapshot()
          _state.value = _state.value.copy(
            isSyncing = false,
            syncStatusMessage = "Sincronizado: ${sanitizedResources.size} recursos, ${remoteFavs.size} favoritos y ${remoteProg.size} progresos al día.",
          )
        },
        onFailure = { err ->
          _state.value = _state.value.copy(
            isSyncing = false,
            syncStatusMessage = "Error de sincronización: ${err.message ?: "red no disponible"}",
          )
        },
      )
    }
  }

  fun searchOpenProvidersLive(query: String) {
    val clean = query.trim()
    if (clean.isEmpty()) return
    scope.launch {
      _state.value = _state.value.copy(liveQueryLoading = true, liveQueryError = null)
      val res = EduSyncClient.searchLiveOpenProviders(clean)
      _state.value = res.fold(
        onSuccess = { list ->
          _state.value.copy(
            liveQueryLoading = false,
            liveQueryResults = list,
            liveQueryError = null,
          )
        },
        onFailure = { err ->
          _state.value.copy(
            liveQueryLoading = false,
            liveQueryError = err.message ?: "No se pudo consultar proveedores abiertos.",
          )
        },
      )
    }
  }

  private data class SnapshotData(
    val levels: List<EduCefrLevel>,
    val categories: List<EduCategory>,
    val providers: List<EduProvider>,
    val licenses: Map<String, EduLicense>,
    val attributions: Map<String, EduAttribution>,
    val resources: List<EduResource>,
    val favorites: Set<String>,
    val progressMap: Map<String, EduUserProgressRecord>,
    val history: List<EduHistoryItem>,
  )
}
