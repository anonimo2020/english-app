package com.example.englishapp.edu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EduCefrLevel(
  val code: String,
  @SerialName("level_id") val levelId: String,
  val rank: Int,
  @SerialName("label_es") val labelEs: String,
  @SerialName("description_es") val descriptionEs: String,
  @SerialName("speech_rate") val speechRate: Float,
)

@Serializable
data class EduCategory(
  val id: String,
  val slug: String,
  @SerialName("name_en") val nameEn: String,
  @SerialName("name_es") val nameEs: String,
  @SerialName("description_es") val descriptionEs: String,
  @SerialName("icon_name") val iconName: String,
  @SerialName("display_order") val displayOrder: Int,
)

@Serializable
data class EduLicense(
  val id: String,
  @SerialName("spdx_code") val spdxCode: String,
  val name: String,
  val url: String,
  @SerialName("allows_storage") val allowsStorage: Int = 1,
  @SerialName("allows_commercial") val allowsCommercial: Int = 0,
  @SerialName("requires_attribution") val requiresAttribution: Int = 1,
  @SerialName("requires_share_alike") val requiresShareAlike: Int = 0,
  @SerialName("storage_policy") val storagePolicy: String = "store_full",
  val notes: String = "",
)

@Serializable
data class EduProvider(
  val id: String,
  val name: String,
  @SerialName("base_url") val baseUrl: String,
  @SerialName("integration_mode") val integrationMode: String,
  @SerialName("default_license_id") val defaultLicenseId: String,
  @SerialName("requires_auth") val requiresAuth: Int = 0,
  @SerialName("rate_limit_rpm") val rateLimitRpm: Int = 20,
  @SerialName("is_enabled") val isEnabled: Int = 1,
  @SerialName("terms_url") val termsUrl: String = "",
  @SerialName("description_es") val descriptionEs: String = "",
  @SerialName("last_sync_at") val lastSyncAt: String? = null,
  @SerialName("last_status") val lastStatus: String = "ready",
)

@Serializable
data class EduAttribution(
  val id: String,
  @SerialName("provider_id") val providerId: String,
  @SerialName("license_id") val licenseId: String,
  val author: String,
  @SerialName("attribution_text") val attributionText: String,
  @SerialName("source_url") val sourceUrl: String,
  @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class EduResource(
  val id: String,
  val title: String,
  @SerialName("title_es") val titleEs: String,
  val description: String,
  val language: String = "en",
  @SerialName("cefr_level") val cefrLevel: String,
  @SerialName("category_id") val categoryId: String,
  @SerialName("primary_skill") val primarySkill: String,
  @SerialName("secondary_skills") val secondarySkills: List<String> = emptyList(),
  val topic: String = "general_english",
  val difficulty: Int = 3,
  @SerialName("estimated_duration_min") val estimatedDurationMin: Int = 5,
  @SerialName("source_type") val sourceType: String = "owned",
  @SerialName("provider_id") val providerId: String = "fluentvoice_internal",
  val author: String = "FluentVoice",
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
  @SerialName("entity_table") val entityTable: String = "educational_resources",
  @SerialName("entity_id") val entityId: String = "",
  @SerialName("updated_at") val updatedAt: String = "",
  @SerialName("payload_json") val payloadJson: String = "{}",
)

@Serializable
data class EduCatalogBundle(
  @SerialName("schema_version") val schemaVersion: Int = 2,
  @SerialName("generated_at") val generatedAt: String = "",
  val levels: List<EduCefrLevel> = emptyList(),
  val categories: List<EduCategory> = emptyList(),
  val licenses: List<EduLicense> = emptyList(),
  val providers: List<EduProvider> = emptyList(),
  val attributions: List<EduAttribution> = emptyList(),
  val resources: List<EduResource> = emptyList(),
)

@Serializable
data class EduQuizQuestionPayload(
  val id: String = "",
  val kind: String = "MULTIPLE_CHOICE",
  @SerialName("prompt_es") val promptEs: String = "",
  @SerialName("audio_prompt_en") val audioPromptEn: String? = null,
  val options: List<String> = emptyList(),
  @SerialName("correct_index") val correctIndex: Int = 0,
  @SerialName("expected_answer") val expectedAnswer: String = "",
  @SerialName("explanation_es") val explanationEs: String = "",
)

@Serializable
data class EduVocabInline(
  val word: String = "",
  val es: String = "",
  val spanish: String = "",
  val phonetic: String = "",
)

@Serializable
data class EduPayloadDetail(
  val title: String = "",
  @SerialName("title_en") val titleEn: String = "",
  @SerialName("title_es") val titleEs: String = "",
  @SerialName("body_en") val bodyEn: String = "",
  @SerialName("body_es") val bodyEs: String = "",
  @SerialName("summary_es") val summaryEs: String = "",
  @SerialName("transcript_en") val transcriptEn: String = "",
  @SerialName("translation_es") val translationEs: String = "",
  @SerialName("dictation_target_en") val dictationTargetEn: String = "",
  @SerialName("hints_es") val hintsEs: String = "",
  @SerialName("accent_hint") val accentHint: String = "US",
  @SerialName("instructions_es") val instructionsEs: String = "",
  @SerialName("grammar_point") val grammarPoint: String = "",
  @SerialName("question_prompt_en") val questionPromptEn: String = "",
  val options: List<String> = emptyList(),
  @SerialName("expected_answer_en") val expectedAnswerEn: String = "",
  @SerialName("explanation_es") val explanationEs: String = "",
  @SerialName("spanish_trap_es") val spanishTrapEs: String = "",
  @SerialName("prompt_en") val promptEn: String = "",
  @SerialName("structure_steps") val structureSteps: List<String> = emptyList(),
  @SerialName("evaluation_checklist") val evaluationChecklist: List<String> = emptyList(),
  @SerialName("model_answer_en") val modelAnswerEn: String = "",
  @SerialName("model_explanation_es") val modelExplanationEs: String = "",
  @SerialName("target_min_words") val targetMinWords: Int = 20,
  @SerialName("target_max_words") val targetMaxWords: Int = 140,
  val word: String = "",
  val headword: String = "",
  val ipa: String = "",
  val spanish: String = "",
  @SerialName("example_en") val exampleEn: String = "",
  @SerialName("example_es") val exampleEs: String = "",
  @SerialName("tip_es") val tipEs: String = "",
  val synonyms: List<String> = emptyList(),
  val antonyms: List<String> = emptyList(),
  val collocations: List<String> = emptyList(),
  @SerialName("target_phrase") val targetPhrase: String = "",
  @SerialName("example_sentence") val exampleSentence: String = "",
  @SerialName("spanish_tip") val spanishTip: String = "",
  val english: String = "",
  val url: String = "",
  @SerialName("audio_url") val audioUrl: String = "",
  @SerialName("listen_url") val listenUrl: String = "",
  @SerialName("page_url") val pageUrl: String = "",
  @SerialName("book_url") val bookUrl: String = "",
  val attribution: String = "",
  val license: String = "",
  @SerialName("lesson_id") val lessonId: String = "",
  @SerialName("opening_en") val openingEn: String = "",
  @SerialName("opening_es") val openingEs: String = "",
  @SerialName("coach_intro_es") val coachIntroEs: String = "",
  @SerialName("starter_suggestions") val starterSuggestions: List<String> = emptyList(),
  @SerialName("lesson_ids") val lessonIds: List<String> = emptyList(),
  val sentence: String = "",
  val accent: String = "",
  @SerialName("key_vocabulary") val keyVocabulary: List<EduVocabInline> = emptyList(),
  val questions: List<EduQuizQuestionPayload> = emptyList(),
)

data class EduUserProgressRecord(
  val resourceId: String,
  val status: String, // not_started | in_progress | completed | mastered
  val score: Float = 0f,
  val attempts: Int = 0,
  val timeSpentSec: Int = 0,
  val lastPosition: String = "",
  val errorTags: List<String> = emptyList(),
  val completedAt: String? = null,
  val updatedAt: String = "",
)

data class EduHistoryItem(
  val id: Long,
  val resourceId: String,
  val action: String,
  val durationSec: Int,
  val score: Float?,
  val createdAt: String,
)

data class EduRecommendationItem(
  val resource: EduResource,
  val score: Double,
  val reasonEs: String,
)

data class EduLearningPath(
  val id: String,
  val cefrLevel: String,
  val focusSkill: String,
  val titleEs: String,
  val rationaleEs: String,
  val estimatedMinutes: Int,
  val steps: List<EduRecommendationItem>,
)

data class EduSyncResult(
  val importedOrUpdated: Int,
  val serverTime: String,
  val providersCount: Int,
)

data class EduLiveQueryResult(
  val providerName: String,
  val title: String,
  val subtitleEs: String,
  val englishText: String,
  val spanishText: String,
  val attributionText: String,
  val licenseCode: String,
  val originalUrl: String,
)

data class LearningHubUiState(
  val isLoading: Boolean = true,
  val isSyncing: Boolean = false,
  val syncStatusMessage: String? = null,
  val searchQuery: String = "",
  val selectedLevel: String? = null,
  val selectedCategory: String? = null,
  val selectedSkill: String? = null,
  val selectedSourceType: String? = null,
  val offlineOnly: Boolean = false,
  val levels: List<EduCefrLevel> = emptyList(),
  val categories: List<EduCategory> = emptyList(),
  val providers: List<EduProvider> = emptyList(),
  val licensesById: Map<String, EduLicense> = emptyMap(),
  val attributionsById: Map<String, EduAttribution> = emptyMap(),
  val allResourcesCount: Int = 0,
  val filteredResources: List<EduResource> = emptyList(),
  val continueLearning: List<EduResource> = emptyList(),
  val recommendedItems: List<EduRecommendationItem> = emptyList(),
  val activeLearningPath: EduLearningPath? = null,
  val favoriteIds: Set<String> = emptySet(),
  val favoriteResources: List<EduResource> = emptyList(),
  val recentHistory: List<Pair<EduHistoryItem, EduResource>> = emptyList(),
  val progressByResourceId: Map<String, EduUserProgressRecord> = emptyMap(),
  val liveQueryLoading: Boolean = false,
  val liveQueryError: String? = null,
  val liveQueryResults: List<EduLiveQueryResult> = emptyList(),
)

object EduSkillCatalog {
  val all: List<Pair<String, String>> = listOf(
    "reading" to "Reading",
    "listening" to "Listening",
    "speaking" to "Speaking",
    "writing" to "Writing",
    "grammar" to "Grammar",
    "vocabulary" to "Vocabulary",
    "pronunciation" to "Pronunciation",
    "comprehension" to "Comprehension",
    "everyday_english" to "Everyday English",
    "academic_english" to "Academic English",
    "professional_english" to "Professional English",
    "idiomatic_expressions" to "Idiomatic Expressions",
  )

  fun labelFor(skillId: String): String =
    all.firstOrNull { it.first == skillId }?.second ?: skillId.replace('_', ' ')
}
