"""
Clasificador inteligente educativo (Fase 6).

Asigna a cada recurso:
- Nivel CEFR (A1–C2)
- Habilidad principal y habilidades secundarias (de las 12 habilidades)
- Categoría educativa (de las 12 categorías)
- Tema, dificultad (1–10), tipo de ejercicio y objetivo de aprendizaje
- Nivel de confianza (0.0–1.0) y origen/estado de validación

Regla de integridad: Nunca atribuye nivel CEFR oficial ni validación definitiva
a recursos clasificados automáticamente sin evidencia curatorial suficiente.
"""

from __future__ import annotations

import re
from dataclasses import dataclass

from edu_db.schema import CEFR_CODES, EDUCATIONAL_CATEGORY_IDS, EDUCATIONAL_SKILLS

_WORD_RE = re.compile(r"[a-zA-Z']+")
_SENTENCE_SPLIT_RE = re.compile(r"[.!?]+")

_C2_MARKERS = {
    "notwithstanding", "lest", "tantamount", "dichotomy", "whereupon", "scarcely",
    "spurious", "impasse", "candor", "entrenched", "litigate", "conflate", "epistemic",
    "unequivocal", "juxtaposition", "ubiquitous", "pragmatic", "ameliorate",
}
_C1_MARKERS = {
    "leverage", "benchmark", "rapport", "nuance", "resilient", "accountable",
    "stakeholder", "nevertheless", "consequently", "moreover", "unprecedented",
    "sustainability", "transparency", "paradigm", "comprehensive", "mitigate",
}
_B2_MARKERS = {
    "whereas", "nonetheless", "hypothesis", "retrospective", "perspective", "mortgage",
    "voucher", "asynchronous", "handoff", "controversial", "furthermore", "despite",
    "although", "environment", "technology", "evidence",
}
_B1_MARKERS = {
    "experience", "interview", "because", "however", "decide", "improve", "schedule",
    "healthy", "habit", "solution", "comfortable", "symptom", "appointment", "opinion",
}
_A2_MARKERS = {
    "yesterday", "tomorrow", "airport", "luggage", "passport", "restaurant", "recipe",
    "weekend", "vacation", "museum", "pharmacy", "ticket", "usually", "sometimes",
}

_TOPIC_KEYWORDS: list[tuple[str, set[str]]] = [
    ("business_career", {"job", "interview", "salary", "office", "meeting", "client", "startup", "economy", "finance", "career"}),
    ("science_tech", {"science", "algorithm", "software", "planet", "sun", "earth", "space", "energy", "biology", "research"}),
    ("travel_culture", {"airport", "flight", "hotel", "train", "museum", "country", "city", "travel", "passport", "culture"}),
    ("daily_life", {"coffee", "breakfast", "family", "morning", "market", "home", "friend", "weather", "dinner", "shopping"}),
    ("health_wellness", {"doctor", "clinic", "headache", "health", "sleep", "habit", "walk", "exercise", "medicine", "hospital"}),
    ("literature_arts", {"novel", "poem", "chapter", "story", "author", "classic", "fiction", "drama", "literature", "film"}),
    ("academic_discourse", {"thesis", "argument", "essay", "academic", "evidence", "analysis", "theory", "philosophy", "university"}),
]

_SKILL_TO_DEFAULT_CATEGORY = {
    "reading": "reading_library",
    "listening": "listening_lab",
    "speaking": "real_conversations",
    "writing": "writing_workshop",
    "grammar": "grammar_academy",
    "vocabulary": "vocabulary_master",
    "pronunciation": "pronunciation_studio",
    "comprehension": "reading_library",
    "everyday_english": "real_conversations",
    "academic_english": "academic_english",
    "professional_english": "professional_english",
    "idiomatic_expressions": "vocabulary_master",
}


@dataclass(frozen=True)
class ClassificationResult:
    cefr_level: str
    primary_skill: str
    secondary_skills: list[str]
    category_id: str
    topic: str
    difficulty: int
    exercise_type: str
    learning_objective_es: str
    confidence: float
    classification_source: str  # curated_internal | provider_declared | auto_estimated
    validation_status: str      # validated | pending_review


def _estimate_cefr_from_text(text: str) -> tuple[str, float]:
    words = [w.lower() for w in _WORD_RE.findall(text)]
    if not words:
        return "A2", 0.50

    avg_word_len = sum(len(w) for w in words) / len(words)
    sentences = [s.strip() for s in _SENTENCE_SPLIT_RE.split(text) if s.strip()]
    avg_sentence_len = len(words) / max(1, len(sentences))

    word_set = set(words)
    c2_hits = len(word_set & _C2_MARKERS)
    c1_hits = len(word_set & _C1_MARKERS)
    b2_hits = len(word_set & _B2_MARKERS)
    b1_hits = len(word_set & _B1_MARKERS)
    a2_hits = len(word_set & _A2_MARKERS)

    score = 0.0
    score += max(0.0, (avg_word_len - 3.6)) * 1.35
    score += max(0.0, (avg_sentence_len - 7.0)) * 0.16
    score += c2_hits * 1.4 + c1_hits * 0.9 + b2_hits * 0.55 + b1_hits * 0.25 - a2_hits * 0.25

    if c2_hits >= 1 or score >= 5.2:
        return "C2", 0.68
    if c1_hits >= 1 or score >= 3.8:
        return "C1", 0.70
    if b2_hits >= 1 or score >= 2.6:
        return "B2", 0.72
    if b1_hits >= 1 or score >= 1.5 or avg_sentence_len >= 12:
        return "B1", 0.74
    if a2_hits >= 1 or avg_sentence_len >= 8 or avg_word_len >= 4.2:
        return "A2", 0.75
    return "A1", 0.76


def _detect_topic(text: str) -> str:
    tokens = set(w.lower() for w in _WORD_RE.findall(text))
    best_topic = "general_english"
    best_hits = 0
    for topic, kws in _TOPIC_KEYWORDS:
        hits = len(tokens & kws)
        if hits > best_hits:
            best_hits = hits
            best_topic = topic
    return best_topic


def _difficulty_for_cefr(cefr: str, primary_skill: str) -> int:
    base = {"A1": 2, "A2": 3, "B1": 5, "B2": 7, "C1": 8, "C2": 10}.get(cefr, 4)
    if primary_skill in {"academic_english", "professional_english", "idiomatic_expressions"}:
        return min(10, base + 1)
    return base


def _objective_es(cefr: str, skill: str, topic: str) -> str:
    skill_labels = {
        "reading": "comprensión lectora y vocabulario en contexto",
        "listening": "comprensión auditiva, ritmo y dictado",
        "speaking": "fluidez oral e interacción espontánea",
        "writing": "redacción estructurada y coherencia escrita",
        "grammar": "precisión gramatical y construcción de oraciones",
        "vocabulary": "ampliación léxica, sinónimos y colocaciones",
        "pronunciation": "articulación fonética y entonación natural",
        "comprehension": "análisis e inferencia de ideas clave",
        "everyday_english": "comunicación práctica cotidiana",
        "academic_english": "lectura y argumentación académica formal",
        "professional_english": "comunicación ejecutiva y laboral",
        "idiomatic_expressions": "modismos, phrasal verbs y matices naturales",
    }
    desc = skill_labels.get(skill, "práctica integral del idioma")
    return f"Fortalecer {desc} en nivel {cefr} (tema: {topic.replace('_', ' ')})."


def classify_resource(
    title: str,
    text: str,
    declared_cefr: str | None = None,
    category_hint: str | None = None,
    skill_hint: str | None = None,
    content_type: str = "reading",
    source_is_curated: bool = False,
) -> ClassificationResult:
    """Clasifica un recurso interno o externo con transparencia en confianza CEFR."""
    combined = f"{title or ''} {text or ''}".strip()
    primary_skill = skill_hint if skill_hint in EDUCATIONAL_SKILLS else {
        "reading": "reading",
        "book": "reading",
        "listening": "listening",
        "audio_clip": "listening",
        "writing": "writing",
        "grammar": "grammar",
        "sentence_pair": "translation" if False else "grammar",
        "vocabulary": "vocabulary",
        "pronunciation": "pronunciation",
        "story": "comprehension",
        "scenario": "speaking",
        "lesson": "everyday_english",
        "unit": "everyday_english",
        "quiz": "comprehension",
        "word_game": "vocabulary",
    }.get(content_type, "reading")

    category_id = (
        category_hint
        if category_hint in EDUCATIONAL_CATEGORY_IDS
        else _SKILL_TO_DEFAULT_CATEGORY.get(primary_skill, "reading_library")
    )

    topic = _detect_topic(combined)

    secondary_candidates = {
        "reading": ["comprehension", "vocabulary"],
        "listening": ["comprehension", "pronunciation"],
        "speaking": ["pronunciation", "everyday_english"],
        "writing": ["grammar", "vocabulary"],
        "grammar": ["writing", "comprehension"],
        "vocabulary": ["reading", "idiomatic_expressions"],
        "pronunciation": ["listening", "speaking"],
        "comprehension": ["reading", "listening"],
        "everyday_english": ["speaking", "listening"],
        "academic_english": ["reading", "writing"],
        "professional_english": ["speaking", "writing"],
        "idiomatic_expressions": ["vocabulary", "speaking"],
    }
    secondary = [s for s in secondary_candidates.get(primary_skill, ["vocabulary", "comprehension"]) if s != primary_skill][:2]

    normalized_declared = declared_cefr.upper().strip() if isinstance(declared_cefr, str) else None
    if source_is_curated and normalized_declared in CEFR_CODES:
        cefr = normalized_declared
        confidence = 1.0
        source = "curated_internal"
        validation_status = "validated"
    elif normalized_declared in CEFR_CODES:
        cefr = normalized_declared
        confidence = 0.84
        source = "provider_declared"
        validation_status = "validated"
    else:
        cefr, confidence = _estimate_cefr_from_text(combined)
        source = "auto_estimated"
        validation_status = "pending_review"

    exercise_type = {
        "reading": "graded_reading",
        "book": "literature_reading",
        "listening": "listening_dictation",
        "audio_clip": "audio_comprehension",
        "grammar": "grammar_drill",
        "writing": "guided_writing",
        "vocabulary": "lexical_study",
        "pronunciation": "phonetic_drill",
        "story": "interactive_story",
        "scenario": "roleplay_speaking",
        "sentence_pair": "contextual_translation",
        "quiz": "exam_simulation",
        "word_game": "semantic_game",
        "external_course": "external_course_link",
    }.get(content_type, "guided_practice")

    return ClassificationResult(
        cefr_level=cefr,
        primary_skill=primary_skill,
        secondary_skills=secondary,
        category_id=category_id,
        topic=topic,
        difficulty=_difficulty_for_cefr(cefr, primary_skill),
        exercise_type=exercise_type,
        learning_objective_es=_objective_es(cefr, primary_skill, topic),
        confidence=round(confidence, 2),
        classification_source=source,
        validation_status=validation_status,
    )
