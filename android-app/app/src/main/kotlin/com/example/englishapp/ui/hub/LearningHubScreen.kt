package com.example.englishapp.ui.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.englishapp.edu.EduCategory
import com.example.englishapp.edu.EduLiveQueryResult
import com.example.englishapp.edu.EduProvider
import com.example.englishapp.edu.EduResource
import com.example.englishapp.edu.EduSkillCatalog
import com.example.englishapp.edu.LearningHubUiState
import com.example.englishapp.model.AppSettings
import com.example.englishapp.ui.components.AuroraScreenBackground
import com.example.englishapp.ui.components.GlassCard
import com.example.englishapp.ui.components.GlowingCircleIconButton
import com.example.englishapp.ui.components.GradientPillButton
import com.example.englishapp.ui.components.GradientProgressBar
import com.example.englishapp.ui.theme.FluentTheme

private enum class HubSection(val label: String) {
  EXPLORE("Explorar"),
  FOR_YOU("Para ti"),
  FAVORITES("Favoritos"),
  HISTORY("Historial"),
  PROVIDERS("Fuentes"),
}

@Composable
fun LearningHubScreen(
  hubState: LearningHubUiState,
  settings: AppSettings,
  onBack: () -> Unit,
  onInitializeHub: () -> Unit,
  onSearchChange: (String) -> Unit,
  onSelectLevel: (String?) -> Unit,
  onSelectCategory: (String?) -> Unit,
  onSelectSkill: (String?) -> Unit,
  onSelectSourceType: (String?) -> Unit,
  onToggleOfflineOnly: () -> Unit,
  onResetFilters: () -> Unit,
  onToggleFavorite: (String) -> Unit,
  onOpenResource: (EduResource) -> Unit,
  onSyncNow: () -> Unit,
  onSearchLiveOpenProviders: (String) -> Unit,
  onOpenExternalUrl: (EduLiveQueryResult) -> Unit,
  onPlayText: (text: String, tag: String, slow: Boolean) -> Unit,
  onSavePhrase: (english: String, spanish: String, note: String, source: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(Unit) { onInitializeHub() }

  var activeSection by rememberSaveable { mutableStateOf(HubSection.EXPLORE) }
  var pageLimit by rememberSaveable(
    hubState.searchQuery,
    hubState.selectedLevel,
    hubState.selectedCategory,
    hubState.selectedSkill,
    hubState.selectedSourceType,
    hubState.offlineOnly,
  ) { mutableIntStateOf(24) }

  val accentColor = FluentTheme.colors.accentFor(settings.selectedCharacter)
  val cyan = FluentTheme.colors.cyanGlow
  val violet = FluentTheme.colors.violetGlow

  AuroraScreenBackground(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top studio bar
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp)
          .padding(top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            GlowingCircleIconButton(
              icon = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Volver",
              onClick = onBack,
              size = 38.dp,
            )
            Column {
              Text(
                text = "Learning Hub",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
              )
              Text(
                text = "${hubState.allResourcesCount} recursos A1–C2 · Nivel activo ${settings.activeLevel().cefr}",
                style = MaterialTheme.typography.labelSmall,
                color = cyan,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowingCircleIconButton(
              icon = if (hubState.offlineOnly) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
              contentDescription = "Solo offline",
              onClick = onToggleOfflineOnly,
              size = 38.dp,
              borderColor = if (hubState.offlineOnly) cyan else FluentTheme.colors.cardBorder,
              iconTint = if (hubState.offlineOnly) cyan else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GlowingCircleIconButton(
              icon = Icons.Rounded.Sync,
              contentDescription = "Sincronizar catálogo",
              onClick = onSyncNow,
              size = 38.dp,
              borderColor = violet.copy(alpha = 0.65f),
              iconTint = if (hubState.isSyncing) cyan else Color.White,
            )
          }
        }

        if (hubState.isSyncing) {
          GradientProgressBar(progress = 0.65f, height = 4.dp)
        }
        hubState.syncStatusMessage?.let { msg ->
          Text(
            text = msg,
            style = MaterialTheme.typography.labelSmall,
            color = cyan,
          )
        }

        // Section selector tabs
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          HubSection.entries.forEach { sec ->
            val selected = sec == activeSection
            val badgeCount = when (sec) {
              HubSection.FAVORITES -> hubState.favoriteResources.size
              HubSection.HISTORY -> hubState.recentHistory.size
              HubSection.PROVIDERS -> hubState.providers.size
              HubSection.FOR_YOU -> hubState.recommendedItems.size
              HubSection.EXPLORE -> hubState.filteredResources.size
            }
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { activeSection = sec },
              shape = RoundedCornerShape(14.dp),
              color = Color.Transparent,
              border = BorderStroke(
                1.dp,
                if (selected) Color(0xFFDDD6FE) else FluentTheme.colors.cardBorder,
              ),
            ) {
              Box(
                modifier = Modifier
                  .background(
                    if (selected) {
                      Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF4F46E5)))
                    } else {
                      Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                    },
                  )
                  .padding(horizontal = 13.dp, vertical = 8.dp),
              ) {
                Text(
                  text = "${sec.label} ($badgeCount)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }

      if (hubState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CircularProgressIndicator(color = cyan)
            Text(
              text = "Cargando ecosistema educativo A1–C2...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        return@Column
      }

      when (activeSection) {
        HubSection.EXPLORE -> {
          ExploreHubTab(
            hubState = hubState,
            pageLimit = pageLimit,
            onLoadMore = { pageLimit += 24 },
            onSearchChange = onSearchChange,
            onSelectLevel = onSelectLevel,
            onSelectCategory = onSelectCategory,
            onSelectSkill = onSelectSkill,
            onSelectSourceType = onSelectSourceType,
            onResetFilters = onResetFilters,
            onToggleFavorite = onToggleFavorite,
            onOpenResource = onOpenResource,
            onSearchLiveOpenProviders = onSearchLiveOpenProviders,
            onOpenExternalUrl = onOpenExternalUrl,
            onPlayText = onPlayText,
            onSavePhrase = onSavePhrase,
          )
        }

        HubSection.FOR_YOU -> {
          ForYouHubTab(
            hubState = hubState,
            accentColor = accentColor,
            onToggleFavorite = onToggleFavorite,
            onOpenResource = onOpenResource,
          )
        }

        HubSection.FAVORITES -> {
          SimpleResourceListTab(
            titleEs = "Tus recursos guardados",
            emptyMessageEs = "Aún no tienes favoritos. Toca el corazón en cualquier lectura, audio o ejercicio.",
            resources = hubState.favoriteResources,
            hubState = hubState,
            onToggleFavorite = onToggleFavorite,
            onOpenResource = onOpenResource,
          )
        }

        HubSection.HISTORY -> {
          HistoryHubTab(
            hubState = hubState,
            onOpenResource = onOpenResource,
          )
        }

        HubSection.PROVIDERS -> {
          ProvidersHubTab(
            providers = hubState.providers,
            hubState = hubState,
          )
        }
      }
    }
  }
}

@Composable
private fun ExploreHubTab(
  hubState: LearningHubUiState,
  pageLimit: Int,
  onLoadMore: () -> Unit,
  onSearchChange: (String) -> Unit,
  onSelectLevel: (String?) -> Unit,
  onSelectCategory: (String?) -> Unit,
  onSelectSkill: (String?) -> Unit,
  onSelectSourceType: (String?) -> Unit,
  onResetFilters: () -> Unit,
  onToggleFavorite: (String) -> Unit,
  onOpenResource: (EduResource) -> Unit,
  onSearchLiveOpenProviders: (String) -> Unit,
  onOpenExternalUrl: (EduLiveQueryResult) -> Unit,
  onPlayText: (text: String, tag: String, slow: Boolean) -> Unit,
  onSavePhrase: (english: String, spanish: String, note: String, source: String) -> Unit,
) {
  val cyan = FluentTheme.colors.cyanGlow
  val visibleSlice = remember(hubState.filteredResources, pageLimit) {
    hubState.filteredResources.take(pageLimit)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Search box + Open API live button
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = hubState.searchQuery,
          onValueChange = onSearchChange,
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          placeholder = { Text("Buscar lectura, gramática, vocabulario o tema...") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = cyan) },
          trailingIcon = {
            if (hubState.searchQuery.isNotBlank()) {
              IconButton(onClick = { onSearchChange("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "Limpiar")
              }
            }
          },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(
            onSearch = {
              if (hubState.searchQuery.isNotBlank()) onSearchLiveOpenProviders(hubState.searchQuery)
            },
          ),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = {
              val term = hubState.searchQuery.ifBlank { "resilient" }
              onSearchLiveOpenProviders(term)
            },
            enabled = !hubState.liveQueryLoading,
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = cyan)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (hubState.liveQueryLoading) "Consultando..." else "Buscar en Tatoeba · Datamuse · Wiki",
              style = MaterialTheme.typography.labelMedium,
            )
          }
          TextButton(onClick = onResetFilters) {
            Text("Ver todo")
          }
        }
      }
    }

    // Live query results if any
    if (hubState.liveQueryError != null) {
      item {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = hubState.liveQueryError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(14.dp),
          )
        }
      }
    }

    if (hubState.liveQueryResults.isNotEmpty()) {
      item {
        Text(
          text = "Resultados en vivo de fuentes abiertas",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = cyan,
        )
      }
      items(hubState.liveQueryResults) { live ->
        GlassCard(modifier = Modifier.fillMaxWidth(), highlighted = true, glowColor = cyan) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${live.providerName} · ${live.licenseCode}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = cyan,
              )
              Row {
                IconButton(
                  onClick = { onPlayText(live.englishText, "live-${live.providerName}-${live.title}", false) },
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Escuchar", tint = cyan, modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { onSavePhrase(live.englishText, live.spanishText, live.attributionText, live.providerName) },
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Guardar", modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { onOpenExternalUrl(live) },
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Abrir origen", modifier = Modifier.size(18.dp))
                }
              }
            }
            Text(
              text = live.englishText,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = live.spanishText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = live.attributionText,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // CEFR Filter strip
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Nivel CEFR",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterPill(
            label = "Todos",
            selected = hubState.selectedLevel == null,
            onClick = { onSelectLevel(null) },
          )
          listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { code ->
            FilterPill(
              label = code,
              selected = hubState.selectedLevel == code,
              onClick = { onSelectLevel(if (hubState.selectedLevel == code) null else code) },
            )
          }
        }

        // 12 Categories strip
        Text(
          text = "Categorías (12)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterPill(
            label = "Todas",
            selected = hubState.selectedCategory == null,
            onClick = { onSelectCategory(null) },
          )
          hubState.categories.forEach { cat ->
            FilterPill(
              label = cat.nameEs,
              selected = hubState.selectedCategory == cat.id,
              onClick = { onSelectCategory(if (hubState.selectedCategory == cat.id) null else cat.id) },
            )
          }
        }

        // 12 Skills strip
        Text(
          text = "Habilidades (12)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterPill(
            label = "Todas",
            selected = hubState.selectedSkill == null,
            onClick = { onSelectSkill(null) },
          )
          EduSkillCatalog.all.forEach { (id, label) ->
            FilterPill(
              label = label,
              selected = hubState.selectedSkill == id,
              onClick = { onSelectSkill(if (hubState.selectedSkill == id) null else id) },
            )
          }
        }

        // Source types strip
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(
            null to "Todas las fuentes",
            "owned" to "Propio",
            "open_reusable" to "Abierto CC/PD",
            "live_api" to "API Gratis",
            "external_link" to "Enlaces",
          ).forEach { (key, label) ->
            FilterPill(
              label = label,
              selected = hubState.selectedSourceType == key,
              onClick = { onSelectSourceType(if (hubState.selectedSourceType == key) null else key) },
            )
          }
        }
      }
    }

    item {
      Text(
        text = "Mostrando ${visibleSlice.size} de ${hubState.filteredResources.size} recursos",
        style = MaterialTheme.typography.labelMedium,
        color = cyan,
      )
    }

    if (visibleSlice.isEmpty()) {
      item {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text("Sin coincidencias con los filtros actuales.", style = MaterialTheme.typography.titleSmall)
            Button(onClick = onResetFilters) { Text("Restablecer filtros") }
          }
        }
      }
    } else {
      items(visibleSlice, key = { it.id }) { resource ->
        EduResourceCard(
          resource = resource,
          categoryNameEs = hubState.categories.firstOrNull { it.id == resource.categoryId }?.nameEs ?: resource.categoryId,
          providerName = hubState.providers.firstOrNull { it.id == resource.providerId }?.name ?: resource.providerId,
          isFavorite = resource.id in hubState.favoriteIds,
          progressStatus = hubState.progressByResourceId[resource.id]?.status,
          progressScore = hubState.progressByResourceId[resource.id]?.score,
          onToggleFavorite = { onToggleFavorite(resource.id) },
          onClick = { onOpenResource(resource) },
        )
      }

      if (visibleSlice.size < hubState.filteredResources.size) {
        item {
          GradientPillButton(
            text = "Mostrar más (${hubState.filteredResources.size - visibleSlice.size} restantes)",
            onClick = onLoadMore,
            height = 46.dp,
          )
        }
      }
    }
  }
}

@Composable
private fun ForYouHubTab(
  hubState: LearningHubUiState,
  accentColor: Color,
  onToggleFavorite: (String) -> Unit,
  onOpenResource: (EduResource) -> Unit,
) {
  val cyan = FluentTheme.colors.cyanGlow
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    hubState.activeLearningPath?.let { path ->
      item {
        GlassCard(modifier = Modifier.fillMaxWidth(), highlighted = true, glowColor = accentColor) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = cyan, modifier = Modifier.size(18.dp))
              Text(
                text = path.titleEs,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = path.rationaleEs,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "${path.steps.size} pasos sugeridos · ~${path.estimatedMinutes} min",
              style = MaterialTheme.typography.labelMedium,
              color = cyan,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    if (hubState.continueLearning.isNotEmpty()) {
      item {
        Text(
          text = "Continuar aprendiendo",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
      items(hubState.continueLearning, key = { "cont-${it.id}" }) { res ->
        EduResourceCard(
          resource = res,
          categoryNameEs = hubState.categories.firstOrNull { it.id == res.categoryId }?.nameEs ?: res.categoryId,
          providerName = hubState.providers.firstOrNull { it.id == res.providerId }?.name ?: res.providerId,
          isFavorite = res.id in hubState.favoriteIds,
          progressStatus = hubState.progressByResourceId[res.id]?.status ?: "in_progress",
          progressScore = hubState.progressByResourceId[res.id]?.score,
          onToggleFavorite = { onToggleFavorite(res.id) },
          onClick = { onOpenResource(res) },
        )
      }
    }

    item {
      Text(
        text = "Recomendados para ti",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }

    items(hubState.recommendedItems, key = { "rec-${it.resource.id}" }) { item ->
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "• ${item.reasonEs}",
          style = MaterialTheme.typography.labelSmall,
          color = cyan,
        )
        EduResourceCard(
          resource = item.resource,
          categoryNameEs = hubState.categories.firstOrNull { it.id == item.resource.categoryId }?.nameEs ?: item.resource.categoryId,
          providerName = hubState.providers.firstOrNull { it.id == item.resource.providerId }?.name ?: item.resource.providerId,
          isFavorite = item.resource.id in hubState.favoriteIds,
          progressStatus = hubState.progressByResourceId[item.resource.id]?.status,
          progressScore = hubState.progressByResourceId[item.resource.id]?.score,
          onToggleFavorite = { onToggleFavorite(item.resource.id) },
          onClick = { onOpenResource(item.resource) },
        )
      }
    }
  }
}

@Composable
private fun SimpleResourceListTab(
  titleEs: String,
  emptyMessageEs: String,
  resources: List<EduResource>,
  hubState: LearningHubUiState,
  onToggleFavorite: (String) -> Unit,
  onOpenResource: (EduResource) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(text = titleEs, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    if (resources.isEmpty()) {
      item {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = emptyMessageEs,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
    } else {
      items(resources, key = { it.id }) { res ->
        EduResourceCard(
          resource = res,
          categoryNameEs = hubState.categories.firstOrNull { it.id == res.categoryId }?.nameEs ?: res.categoryId,
          providerName = hubState.providers.firstOrNull { it.id == res.providerId }?.name ?: res.providerId,
          isFavorite = res.id in hubState.favoriteIds,
          progressStatus = hubState.progressByResourceId[res.id]?.status,
          progressScore = hubState.progressByResourceId[res.id]?.score,
          onToggleFavorite = { onToggleFavorite(res.id) },
          onClick = { onOpenResource(res) },
        )
      }
    }
  }
}

@Composable
private fun HistoryHubTab(
  hubState: LearningHubUiState,
  onOpenResource: (EduResource) -> Unit,
) {
  val cyan = FluentTheme.colors.cyanGlow
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Text("Actividad reciente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    if (hubState.recentHistory.isEmpty()) {
      item {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Tu historial aparecerá aquí conforme abras lecturas, dictados o escenarios.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
    } else {
      items(hubState.recentHistory, key = { it.first.id }) { (hist, res) ->
        GlassCard(
          modifier = Modifier.fillMaxWidth(),
          onClick = { onOpenResource(res) },
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = cyan, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(res.titleEs, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              val actionLabel = when (hist.action) {
                "completed" -> "Completado"
                "practiced" -> "Practicado"
                "favorited" -> "Guardado en favoritos"
                "external_opened" -> "Enlace abierto"
                else -> "Consultado"
              }
              val scoreLabel = hist.score?.let { " · ${it.toInt()}%" } ?: ""
              Text(
                text = "${res.cefrLevel} · $actionLabel$scoreLabel · ${hist.createdAt.take(10)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun ProvidersHubTab(
  providers: List<EduProvider>,
  hubState: LearningHubUiState,
) {
  val cyan = FluentTheme.colors.cyanGlow
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Text(
        text = "Proveedores, licencias y atribuciones",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "FluentVoice separa claramente contenido propio, abierto reutilizable, enlaces externos autorizados y consultas API.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    items(providers, key = { it.id }) { prov ->
      val lic = hubState.licensesById[prov.defaultLicenseId]
      GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(prov.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = cyan.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, cyan.copy(alpha = 0.45f)),
            ) {
              Text(
                text = prov.defaultLicenseId,
                style = MaterialTheme.typography.labelSmall,
                color = cyan,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
          Text(
            text = prov.descriptionEs,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Modo: ${prov.integrationMode} · Política: ${lic?.storagePolicy ?: "verificada"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun FilterPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(999.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(999.dp),
    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      1.dp,
      if (selected) Color(0xFFDDD6FE) else FluentTheme.colors.cardBorder,
    ),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
  }
}

@Composable
private fun EduResourceCard(
  resource: EduResource,
  categoryNameEs: String,
  providerName: String,
  isFavorite: Boolean,
  progressStatus: String?,
  progressScore: Float?,
  onToggleFavorite: () -> Unit,
  onClick: () -> Unit,
) {
  val cyan = FluentTheme.colors.cyanGlow
  val sourceLabel = when (resource.sourceType) {
    "owned" -> "Propio"
    "open_reusable" -> "Abierto"
    "live_api" -> "API"
    else -> "Enlace"
  }
  val isDone = progressStatus == "completed" || progressStatus == "mastered"

  GlassCard(
    modifier = Modifier.fillMaxWidth(),
    highlighted = isDone,
    glowColor = if (isDone) Color(0xFF34D399) else MaterialTheme.colorScheme.primary,
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
          ) {
            Text(
              text = resource.cefrLevel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = cyan,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            )
          }
          Text(
            text = "$categoryNameEs · ${EduSkillCatalog.labelFor(resource.primarySkill)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isDone) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = "Completado",
              tint = Color(0xFF34D399),
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(28.dp),
          ) {
            Icon(
              imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
              contentDescription = "Favorito",
              tint = if (isFavorite) Color(0xFFF472B6) else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Text(
        text = resource.titleEs,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (resource.title != resource.titleEs) {
        Text(
          text = resource.title,
          style = MaterialTheme.typography.labelSmall,
          color = cyan,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = resource.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Verified,
            contentDescription = null,
            tint = if (resource.validationStatus == "validated") Color(0xFF34D399) else Color(0xFFFBBF24),
            modifier = Modifier.size(13.dp),
          )
          val confLabel = if (resource.cefrClassificationSource == "auto_estimated") {
            "$sourceLabel · CEFR est. ${(resource.cefrConfidence * 100).toInt()}%"
          } else {
            "$sourceLabel · $providerName"
          }
          Text(
            text = confLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        val scoreTxt = progressScore?.takeIf { it > 0f }?.let { " · ${it.toInt()}%" } ?: ""
        Text(
          text = "${resource.estimatedDurationMin} min$scoreTxt",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = cyan,
        )
      }
    }
  }
}
