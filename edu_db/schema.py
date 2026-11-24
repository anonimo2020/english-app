"""
Definición del esquema relacional unificado para el ecosistema educativo FluentVoice.

Implementa las 24 entidades educativas solicitadas más la tabla maestra unificada
`educational_resources`, control de agregaciones (`aggregation_runs`) y registro de
migraciones (`schema_migrations`).
"""

SCHEMA_VERSION = 2

CEFR_CODES = ("A1", "A2", "B1", "B2", "C1", "C2")

EDUCATIONAL_CATEGORY_IDS = (
    "reading_library",
    "listening_lab",
    "vocabulary_master",
    "grammar_academy",
    "pronunciation_studio",
    "writing_workshop",
    "real_conversations",
    "word_games",
    "english_news",
    "academic_english",
    "professional_english",
    "exam_preparation",
)

EDUCATIONAL_SKILLS = (
    "reading",
    "listening",
    "speaking",
    "writing",
    "grammar",
    "vocabulary",
    "pronunciation",
    "comprehension",
    "everyday_english",
    "academic_english",
    "professional_english",
    "idiomatic_expressions",
)

SOURCE_TYPES = ("owned", "open_reusable", "external_link", "live_api")

DDL_V1_STATEMENTS: list[str] = [
    """
    CREATE TABLE IF NOT EXISTS schema_migrations (
        version INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        applied_at TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS cefr_levels (
        code TEXT PRIMARY KEY,
        level_id TEXT NOT NULL UNIQUE,
        rank INTEGER NOT NULL UNIQUE,
        label_es TEXT NOT NULL,
        description_es TEXT NOT NULL,
        speech_rate REAL NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS educational_categories (
        id TEXT PRIMARY KEY,
        slug TEXT NOT NULL UNIQUE,
        name_en TEXT NOT NULL,
        name_es TEXT NOT NULL,
        description_es TEXT NOT NULL,
        icon_name TEXT NOT NULL,
        display_order INTEGER NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS content_licenses (
        id TEXT PRIMARY KEY,
        spdx_code TEXT NOT NULL,
        name TEXT NOT NULL,
        url TEXT NOT NULL,
        allows_storage INTEGER NOT NULL DEFAULT 0,
        allows_commercial INTEGER NOT NULL DEFAULT 0,
        requires_attribution INTEGER NOT NULL DEFAULT 1,
        requires_share_alike INTEGER NOT NULL DEFAULT 0,
        storage_policy TEXT NOT NULL CHECK (
            storage_policy IN ('store_full', 'store_metadata_only', 'link_only', 'blocked')
        ),
        notes TEXT NOT NULL DEFAULT ''
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS content_providers (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        base_url TEXT NOT NULL,
        integration_mode TEXT NOT NULL CHECK (
            integration_mode IN ('internal', 'api', 'authorized_download', 'external_link', 'authorized_player', 'dataset')
        ),
        default_license_id TEXT NOT NULL REFERENCES content_licenses(id),
        requires_auth INTEGER NOT NULL DEFAULT 0,
        rate_limit_rpm INTEGER NOT NULL DEFAULT 30,
        is_enabled INTEGER NOT NULL DEFAULT 1,
        terms_url TEXT NOT NULL DEFAULT '',
        description_es TEXT NOT NULL DEFAULT '',
        last_sync_at TEXT,
        last_status TEXT NOT NULL DEFAULT 'ready'
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS content_attributions (
        id TEXT PRIMARY KEY,
        provider_id TEXT NOT NULL REFERENCES content_providers(id),
        license_id TEXT NOT NULL REFERENCES content_licenses(id),
        author TEXT NOT NULL,
        attribution_text TEXT NOT NULL,
        source_url TEXT NOT NULL,
        created_at TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS learning_units (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        order_in_level INTEGER NOT NULL,
        title_es TEXT NOT NULL,
        title_en TEXT NOT NULL,
        description_es TEXT NOT NULL,
        specialized_focus_es TEXT NOT NULL,
        icon_name TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS lessons (
        id TEXT PRIMARY KEY,
        unit_id TEXT NOT NULL REFERENCES learning_units(id) ON DELETE CASCADE,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        order_in_unit INTEGER NOT NULL,
        title_es TEXT NOT NULL,
        title_en TEXT NOT NULL,
        duration_min INTEGER NOT NULL,
        speaking_target_en TEXT NOT NULL,
        speaking_target_es TEXT NOT NULL,
        speaking_tip_es TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS teaching_cards (
        id TEXT PRIMARY KEY,
        lesson_id TEXT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
        order_in_lesson INTEGER NOT NULL,
        kind TEXT NOT NULL,
        title_es TEXT NOT NULL,
        title_en TEXT NOT NULL,
        explanation_es TEXT NOT NULL,
        model_en TEXT NOT NULL,
        model_es TEXT NOT NULL,
        tip_es TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS quizzes (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        category_id TEXT NOT NULL REFERENCES educational_categories(id),
        skill TEXT NOT NULL,
        title_en TEXT NOT NULL,
        title_es TEXT NOT NULL,
        instructions_es TEXT NOT NULL,
        source_entity_type TEXT NOT NULL,
        source_entity_id TEXT NOT NULL,
        passing_score INTEGER NOT NULL DEFAULT 70
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS quiz_questions (
        id TEXT PRIMARY KEY,
        quiz_id TEXT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
        order_index INTEGER NOT NULL DEFAULT 0,
        kind TEXT NOT NULL,
        prompt_es TEXT NOT NULL,
        prompt_en TEXT NOT NULL DEFAULT '',
        audio_prompt_en TEXT,
        options_json TEXT NOT NULL,
        correct_index INTEGER NOT NULL DEFAULT 0,
        expected_answer TEXT NOT NULL DEFAULT '',
        target_tokens_json TEXT NOT NULL DEFAULT '[]',
        distraction_tokens_json TEXT NOT NULL DEFAULT '[]',
        explanation_es TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS vocabulary (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        category TEXT NOT NULL,
        subcategory TEXT NOT NULL DEFAULT 'general',
        word TEXT NOT NULL,
        ipa TEXT NOT NULL,
        spanish TEXT NOT NULL,
        example_en TEXT NOT NULL,
        example_es TEXT NOT NULL,
        tip_es TEXT NOT NULL,
        synonyms_json TEXT NOT NULL DEFAULT '[]',
        antonyms_json TEXT NOT NULL DEFAULT '[]',
        collocations_json TEXT NOT NULL DEFAULT '[]',
        is_baseline INTEGER NOT NULL DEFAULT 0,
        source_provider_id TEXT NOT NULL DEFAULT 'fluentvoice_internal' REFERENCES content_providers(id)
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS phonetic_exercises (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        title TEXT NOT NULL,
        category TEXT NOT NULL,
        ipa TEXT NOT NULL,
        target_phrase TEXT NOT NULL,
        example_sentence TEXT NOT NULL,
        spanish_tip TEXT NOT NULL,
        contrast_pair TEXT NOT NULL DEFAULT '',
        is_baseline INTEGER NOT NULL DEFAULT 0,
        source_provider_id TEXT NOT NULL DEFAULT 'fluentvoice_internal' REFERENCES content_providers(id)
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS stories (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        title_es TEXT NOT NULL,
        title_en TEXT NOT NULL,
        duration_label TEXT NOT NULL,
        body_en TEXT NOT NULL,
        body_es TEXT NOT NULL,
        vocab_json TEXT NOT NULL,
        questions_json TEXT NOT NULL,
        is_baseline INTEGER NOT NULL DEFAULT 0,
        source_provider_id TEXT NOT NULL DEFAULT 'fluentvoice_internal' REFERENCES content_providers(id)
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS reading_materials (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        category_id TEXT NOT NULL REFERENCES educational_categories(id),
        skill TEXT NOT NULL,
        title_en TEXT NOT NULL,
        title_es TEXT NOT NULL,
        summary_es TEXT NOT NULL,
        body_en TEXT NOT NULL,
        body_es TEXT NOT NULL,
        word_count INTEGER NOT NULL,
        estimated_duration_min INTEGER NOT NULL,
        topic TEXT NOT NULL,
        key_vocabulary_json TEXT NOT NULL DEFAULT '[]',
        comprehension_quiz_id TEXT REFERENCES quizzes(id),
        provider_id TEXT NOT NULL DEFAULT 'fluentvoice_internal' REFERENCES content_providers(id),
        license_id TEXT NOT NULL DEFAULT 'FLUENTVOICE-OWNED' REFERENCES content_licenses(id),
        original_url TEXT NOT NULL DEFAULT ''
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS listening_materials (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        category_id TEXT NOT NULL REFERENCES educational_categories(id),
        title_en TEXT NOT NULL,
        title_es TEXT NOT NULL,
        instructions_es TEXT NOT NULL,
        transcript_en TEXT NOT NULL,
        translation_es TEXT NOT NULL,
        dictation_target_en TEXT NOT NULL,
        accent_hint TEXT NOT NULL DEFAULT 'US',
        estimated_duration_min INTEGER NOT NULL,
        audio_url TEXT NOT NULL DEFAULT '',
        tts_fallback_allowed INTEGER NOT NULL DEFAULT 1,
        hints_es TEXT NOT NULL DEFAULT '',
        comprehension_quiz_id TEXT REFERENCES quizzes(id),
        provider_id TEXT NOT NULL DEFAULT 'fluentvoice_internal' REFERENCES content_providers(id),
        license_id TEXT NOT NULL DEFAULT 'FLUENTVOICE-OWNED' REFERENCES content_licenses(id)
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS conversation_scenarios (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        title_es TEXT NOT NULL,
        title_en TEXT NOT NULL,
        subtitle_es TEXT NOT NULL,
        opening_en TEXT NOT NULL,
        opening_es TEXT NOT NULL,
        starter_suggestions_json TEXT NOT NULL,
        icon_name TEXT NOT NULL,
        focus_skill_es TEXT NOT NULL,
        coach_intro_es TEXT NOT NULL,
        domain TEXT NOT NULL DEFAULT 'everyday',
        is_baseline INTEGER NOT NULL DEFAULT 0
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS writing_exercises (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        category_id TEXT NOT NULL REFERENCES educational_categories(id),
        title_en TEXT NOT NULL,
        title_es TEXT NOT NULL,
        prompt_en TEXT NOT NULL,
        instructions_es TEXT NOT NULL,
        structure_steps_json TEXT NOT NULL,
        evaluation_checklist_json TEXT NOT NULL,
        model_answer_en TEXT NOT NULL,
        model_explanation_es TEXT NOT NULL,
        target_min_words INTEGER NOT NULL DEFAULT 40,
        target_max_words INTEGER NOT NULL DEFAULT 140
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS grammar_exercises (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        category_id TEXT NOT NULL REFERENCES educational_categories(id),
        exercise_type TEXT NOT NULL,
        title_en TEXT NOT NULL,
        title_es TEXT NOT NULL,
        grammar_point TEXT NOT NULL,
        instructions_es TEXT NOT NULL,
        question_prompt_en TEXT NOT NULL,
        options_json TEXT NOT NULL DEFAULT '[]',
        expected_answer_en TEXT NOT NULL,
        explanation_es TEXT NOT NULL,
        spanish_trap_es TEXT NOT NULL DEFAULT ''
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS external_resources (
        id TEXT PRIMARY KEY,
        provider_id TEXT NOT NULL REFERENCES content_providers(id),
        external_id TEXT NOT NULL,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        category_id TEXT NOT NULL REFERENCES educational_categories(id),
        primary_skill TEXT NOT NULL,
        title TEXT NOT NULL,
        title_es TEXT NOT NULL DEFAULT '',
        description TEXT NOT NULL,
        language TEXT NOT NULL DEFAULT 'en',
        original_url TEXT NOT NULL,
        content_type TEXT NOT NULL,
        access_mode TEXT NOT NULL CHECK (
            access_mode IN ('open_reusable', 'external_link', 'live_api')
        ),
        license_id TEXT NOT NULL REFERENCES content_licenses(id),
        attribution_id TEXT REFERENCES content_attributions(id),
        author TEXT NOT NULL,
        excerpt_or_payload_json TEXT NOT NULL DEFAULT '{}',
        cefr_confidence REAL NOT NULL DEFAULT 0.65,
        cefr_classification_source TEXT NOT NULL DEFAULT 'auto_estimated',
        validation_status TEXT NOT NULL DEFAULT 'pending_review',
        availability_status TEXT NOT NULL DEFAULT 'available',
        offline_compatible INTEGER NOT NULL DEFAULT 0,
        added_at TEXT NOT NULL,
        last_verified_at TEXT NOT NULL,
        content_hash TEXT NOT NULL,
        UNIQUE(provider_id, external_id)
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS educational_resources (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        title_es TEXT NOT NULL,
        description TEXT NOT NULL,
        language TEXT NOT NULL DEFAULT 'en',
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        category_id TEXT NOT NULL REFERENCES educational_categories(id),
        primary_skill TEXT NOT NULL,
        secondary_skills_json TEXT NOT NULL DEFAULT '[]',
        topic TEXT NOT NULL,
        difficulty INTEGER NOT NULL CHECK (difficulty BETWEEN 1 AND 10),
        estimated_duration_min INTEGER NOT NULL DEFAULT 5,
        source_type TEXT NOT NULL CHECK (
            source_type IN ('owned', 'open_reusable', 'external_link', 'live_api')
        ),
        provider_id TEXT NOT NULL REFERENCES content_providers(id),
        author TEXT NOT NULL,
        original_url TEXT NOT NULL DEFAULT '',
        content_type TEXT NOT NULL,
        license_id TEXT NOT NULL REFERENCES content_licenses(id),
        attribution_id TEXT REFERENCES content_attributions(id),
        added_at TEXT NOT NULL,
        last_verified_at TEXT NOT NULL,
        availability_status TEXT NOT NULL DEFAULT 'available',
        validation_status TEXT NOT NULL DEFAULT 'validated',
        cefr_confidence REAL NOT NULL DEFAULT 1.0,
        cefr_classification_source TEXT NOT NULL DEFAULT 'curated_internal',
        learning_objective_es TEXT NOT NULL DEFAULT '',
        exercise_type TEXT NOT NULL DEFAULT 'practice',
        offline_compatible INTEGER NOT NULL DEFAULT 1,
        content_hash TEXT NOT NULL UNIQUE,
        entity_table TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        payload_json TEXT NOT NULL DEFAULT '{}',
        updated_at TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS learning_paths (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL DEFAULT 'default_learner',
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        focus_skill TEXT NOT NULL,
        title_es TEXT NOT NULL,
        description_es TEXT NOT NULL,
        resource_ids_json TEXT NOT NULL,
        rationale_es TEXT NOT NULL,
        estimated_minutes INTEGER NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS user_favorites (
        user_id TEXT NOT NULL DEFAULT 'default_learner',
        resource_id TEXT NOT NULL REFERENCES educational_resources(id) ON DELETE CASCADE,
        created_at TEXT NOT NULL,
        PRIMARY KEY (user_id, resource_id)
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS user_progress (
        user_id TEXT NOT NULL DEFAULT 'default_learner',
        resource_id TEXT NOT NULL REFERENCES educational_resources(id) ON DELETE CASCADE,
        status TEXT NOT NULL CHECK (status IN ('not_started', 'in_progress', 'completed', 'mastered')),
        score REAL NOT NULL DEFAULT 0.0,
        attempts INTEGER NOT NULL DEFAULT 0,
        time_spent_sec INTEGER NOT NULL DEFAULT 0,
        last_position TEXT NOT NULL DEFAULT '',
        error_tags_json TEXT NOT NULL DEFAULT '[]',
        completed_at TEXT,
        updated_at TEXT NOT NULL,
        PRIMARY KEY (user_id, resource_id)
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS user_activity (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL DEFAULT 'default_learner',
        activity_date TEXT NOT NULL,
        skill TEXT NOT NULL,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        minutes_practiced INTEGER NOT NULL DEFAULT 0,
        xp_earned INTEGER NOT NULL DEFAULT 0,
        resources_completed INTEGER NOT NULL DEFAULT 0,
        accuracy_pct REAL NOT NULL DEFAULT 0.0,
        created_at TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS content_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL DEFAULT 'default_learner',
        resource_id TEXT NOT NULL REFERENCES educational_resources(id) ON DELETE CASCADE,
        action TEXT NOT NULL CHECK (
            action IN ('viewed', 'started', 'completed', 'favorited', 'practiced', 'external_opened')
        ),
        duration_sec INTEGER NOT NULL DEFAULT 0,
        score REAL,
        created_at TEXT NOT NULL
    );
    """,
    """
    CREATE TABLE IF NOT EXISTS aggregation_runs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        provider_id TEXT NOT NULL REFERENCES content_providers(id),
        started_at TEXT NOT NULL,
        finished_at TEXT NOT NULL,
        fetched_count INTEGER NOT NULL DEFAULT 0,
        imported_count INTEGER NOT NULL DEFAULT 0,
        skipped_duplicates INTEGER NOT NULL DEFAULT 0,
        rejected_license_count INTEGER NOT NULL DEFAULT 0,
        error_count INTEGER NOT NULL DEFAULT 0,
        status TEXT NOT NULL,
        details TEXT NOT NULL DEFAULT ''
    );
    """,
    # Índices de rendimiento
    "CREATE INDEX IF NOT EXISTS idx_edu_res_level_cat ON educational_resources(cefr_level, category_id);",
    "CREATE INDEX IF NOT EXISTS idx_edu_res_skill_level ON educational_resources(primary_skill, cefr_level);",
    "CREATE INDEX IF NOT EXISTS idx_edu_res_source_avail ON educational_resources(source_type, availability_status);",
    "CREATE INDEX IF NOT EXISTS idx_edu_res_updated ON educational_resources(updated_at);",
    "CREATE INDEX IF NOT EXISTS idx_edu_res_provider ON educational_resources(provider_id);",
    "CREATE INDEX IF NOT EXISTS idx_ext_res_hash ON external_resources(content_hash);",
    "CREATE INDEX IF NOT EXISTS idx_progress_user_updated ON user_progress(user_id, updated_at);",
    "CREATE INDEX IF NOT EXISTS idx_history_user_created ON content_history(user_id, created_at DESC);",
    "CREATE INDEX IF NOT EXISTS idx_activity_user_date ON user_activity(user_id, activity_date);",
]

DDL_V2_STATEMENTS: list[str] = [
    """
    CREATE TABLE IF NOT EXISTS diagnostic_questions (
        id TEXT PRIMARY KEY,
        cefr_level TEXT NOT NULL REFERENCES cefr_levels(code),
        question_type TEXT NOT NULL,
        difficulty REAL NOT NULL,
        prompt_es TEXT NOT NULL,
        prompt_en TEXT NOT NULL,
        options_json TEXT NOT NULL,
        correct_index INTEGER NOT NULL,
        explanation_es TEXT NOT NULL
    );
    """,
    "CREATE INDEX IF NOT EXISTS idx_edu_res_title ON educational_resources(title, title_es);",
    "CREATE INDEX IF NOT EXISTS idx_vocab_level_word ON vocabulary(cefr_level, word);",
]
