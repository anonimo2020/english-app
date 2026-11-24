"""
Sembrador maestro del ecosistema interno y registros de proveedores/licencias.

Preserva íntegro todo el currículo original de FluentVoice (6 niveles CEFR,
24 unidades, 96 lecciones, 288 tarjetas, 288 mini-tests, 48 escenarios, 288 vocabulario,
18 historias, 30 drills, 36 preguntas diagnósticas, 8 portales) y siembra la expansión
masiva A1–C2 sin duplicados e idempotentemente.
"""

from __future__ import annotations

import json
import sqlite3
from pathlib import Path
from typing import Any

from edu_db.baseline_extractor import extract_baseline_inventory
from edu_db.classifier import classify_resource
from edu_db.content_grammar_writing import (
    GRAMMAR_EXERCISES_EXPANSION,
    SPECIALIZED_SCENARIOS_EXPANSION,
    WRITING_EXERCISES_EXPANSION,
)
from edu_db.content_reading_listening import (
    ADDITIONAL_STORIES,
    LISTENING_MATERIALS_EXPANSION,
    READING_MATERIALS_EXPANSION,
)
from edu_db.content_vocab_exams import (
    EXAM_PREPARATION_QUIZZES,
    PHONETIC_EXPANSION,
    VOCABULARY_EXPANSION,
    WORD_GAMES_EXPANSION,
)
from edu_db.migrations import get_connection, init_db, utc_now_iso
from edu_db.security import STANDARD_LICENSES, compute_content_hash, sanitize_external_text

SEED_TIMESTAMP = "2026-11-24T00:00:00+00:00"

CEFR_LEVEL_ROWS = [
    ("A1", "NOVATO", 1, "Principiante", "Frases cortas, vocabulario esencial y explicaciones paso a paso.", 0.78),
    ("A2", "JUNIOR", 2, "Elemental", "Conversación cotidiana en presente, pasado simple y planes.", 0.86),
    ("B1", "INTERMEDIO", 3, "Intermedio", "Inglés natural, phrasal verbs, presente perfecto y fluidez práctica.", 0.95),
    ("B2", "INTERMEDIO_ALTO", 4, "Intermedio Alto", "Debates fluidos, voz pasiva, condicionales mixtos y vocabulario sólido.", 1.00),
    ("C1", "PRO", 5, "Avanzado", "Inglés ejecutivo y académico, inversiones formales y matices finos.", 1.06),
    ("C2", "MAESTRIA", 6, "Maestría", "Dominio retórico cercano al nativo, registro culto e ironía sutil.", 1.10),
]

CATEGORY_ROWS = [
    ("reading_library", "reading-library", "Reading Library", "Biblioteca de Lectura", "Lecturas graduadas A1–C2, literatura clásica y comprensión lectora.", "auto_stories", 1),
    ("listening_lab", "listening-lab", "Listening Lab", "Laboratorio de Escucha", "Audios graduados, dictados interactivos y audiolibros abiertos.", "headphones", 2),
    ("vocabulary_master", "vocabulary-master", "Vocabulary Master", "Maestro de Vocabulario", "Vocabulario por frecuencia, phrasal verbs, modismos, colocaciones y sinónimos.", "translate", 3),
    ("grammar_academy", "grammar-academy", "Grammar Academy", "Academia de Gramática", "Ejercicios guiados, corrección de errores, traducción contextual y sintaxis.", "spellcheck", 4),
    ("pronunciation_studio", "pronunciation-studio", "Pronunciation Studio", "Estudio de Pronunciación", "Pares mínimos, letras mudas, enlace rítmico y audio IPA abierto.", "record_voice_over", 5),
    ("writing_workshop", "writing-workshop", "Writing Workshop", "Taller de Escritura", "Redacción guiada paso a paso con listas de verificación y modelos.", "edit_note", 6),
    ("real_conversations", "real-conversations", "Real Conversations", "Conversaciones Reales", "Escenarios de rol cotidianos, laborales, de viaje y diplomacia con Eddy y Manu.", "forum", 7),
    ("word_games", "word-games", "Word Games", "Juegos de Palabras", "Retos ágiles de antónimos, sinónimos, colocaciones y decodificación léxica.", "sports_esports", 8),
    ("english_news", "english-news", "English News", "Actualidad en Inglés", "Artículos informativos accesibles y crónicas críticas de actualidad.", "newspaper", 9),
    ("academic_english", "academic-english", "Academic English", "Inglés Académico", "Textos científicos, metodología, ensayos y seminarios universitarios.", "school", 10),
    ("professional_english", "professional-english", "Professional English", "Inglés Profesional", "Correos ejecutivos, entrevistas, negociación, retroalimentación y liderazgo.", "work", 11),
    ("exam_preparation", "exam-preparation", "Exam Preparation", "Preparación de Exámenes", "Simulacros integrales multi-habilidad calibrados para niveles A1–C2.", "quiz", 12),
]

PROVIDER_ROWS = [
    ("fluentvoice_internal", "FluentVoice Pedagógico", "https://fluentvoice.local/", "internal", "FLUENTVOICE-OWNED", 0, 120, 1, "https://fluentvoice.local/terms", "Contenido curricular y ampliado propio creado para hispanohablantes."),
    ("tatoeba", "Tatoeba Project", "https://tatoeba.org/", "api", "CC-BY-2.0-FR", 0, 20, 1, "https://tatoeba.org/en/terms_of_use", "Corpus colaborativo bilingüe inglés–español (CC BY 2.0 FR con atribución)."),
    ("mozilla_common_voice", "Mozilla Common Voice (Data Collective)", "https://datacollective.mozillafoundation.org/", "dataset", "CC0-1.0", 1, 10, 1, "https://commonvoice.mozilla.org/en/terms", "Corpus de voz abierto CC0; soporta importación TSV local o descarga con credencial MDC."),
    ("gutenberg_gutendex", "Project Gutenberg / Gutendex", "https://gutendex.com/", "api", "PDM-1.0", 0, 20, 1, "https://www.gutenberg.org/policy/permission.html", "Catálogo de más de 75,000 obras de dominio público vía Gutendex API."),
    ("openlearn", "The Open University — OpenLearn", "https://www.open.edu/openlearn/", "external_link", "CC-BY-NC-SA-4.0", 0, 10, 1, "https://www.open.edu/openlearn/about-openlearn/frequently-asked-questions-on-openlearn", "Cursos abiertos universitarios CC BY-NC-SA 4.0 integrados por ficha y enlace canónico."),
    ("british_council", "British Council LearnEnglish", "https://learnenglish.britishcouncil.org/", "external_link", "ALL-RIGHTS-RESERVED-LINK-ONLY", 0, 10, 1, "https://www.britishcouncil.org/terms", "Portales oficiales por nivel CEFR integrados exclusivamente como enlaces externos autorizados."),
    ("datamuse", "Datamuse API", "https://api.datamuse.com/", "api", "FREE-API-NONCOMM", 0, 60, 1, "https://www.datamuse.com/api/", "Relaciones léxicas gratuitas: sinónimos, antónimos, rimas y contexto semántico."),
    ("wikimedia_commons", "Wikimedia Commons", "https://commons.wikimedia.org/", "api", "CC-BY-SA-4.0", 0, 30, 1, "https://commons.wikimedia.org/wiki/Commons:Reusing_content_outside_Wikimedia", "Audios de pronunciación e imágenes educativas con validación individual de licencia."),
    ("simple_wikipedia", "Simple English Wikipedia", "https://simple.wikipedia.org/", "api", "CC-BY-SA-4.0", 0, 30, 1, "https://foundation.wikimedia.org/wiki/Policy:Terms_of_Use", "Resúmenes enciclopédicos claros bajo licencia CC BY-SA 4.0."),
    ("librivox", "LibriVox Audiobooks", "https://librivox.org/", "api", "PDM-1.0", 0, 20, 1, "https://librivox.org/pages/public-domain-information/", "Audiolibros y capítulos de dominio público con URL de audio autorizado."),
    ("free_dictionary", "Free Dictionary API", "https://api.dictionaryapi.dev/", "api", "CC-BY-SA-3.0", 0, 40, 1, "https://dictionaryapi.dev/", "Definiciones, transcripciones IPA y audio fonético abierto."),
    ("ngsl_nawl", "NGSL & NAWL Open Word Lists", "https://www.newgeneralservicelist.com/", "external_link", "CC-BY-SA-4.0", 0, 10, 1, "https://www.newgeneralservicelist.com/", "Listas léxicas científicas de alta frecuencia y vocabulario académico."),
    ("voa_learning_english", "VOA Learning English", "https://learningenglish.voanews.com/", "external_link", "PDM-1.0", 0, 10, 1, "https://www.voanews.com/p/6021.html", "Noticias y reportajes narrados pausadamente para aprendizaje de inglés."),
    ("bbc_learning_english", "BBC Learning English", "https://www.bbc.co.uk/learningenglish/", "external_link", "ALL-RIGHTS-RESERVED-LINK-ONLY", 0, 10, 1, "https://www.bbc.co.uk/terms", "Programas educativos 6 Minute English enlazados oficialmente."),
    ("randalls_esl_lab", "Randall's ESL Cyber Listening Lab", "https://www.esl-lab.com/", "external_link", "ALL-RIGHTS-RESERVED-LINK-ONLY", 0, 10, 1, "https://www.esl-lab.com/", "Audios y cuestionarios por nivel enlazados directamente al sitio oficial."),
    ("youglish", "YouGlish", "https://youglish.com/", "external_link", "ALL-RIGHTS-RESERVED-LINK-ONLY", 0, 10, 1, "https://youglish.com/terms", "Pronunciación real en contexto de charlas reales mediante enlace directo."),
    ("cambridge_dictionary", "Cambridge Dictionary", "https://dictionary.cambridge.org/", "external_link", "ALL-RIGHTS-RESERVED-LINK-ONLY", 0, 10, 1, "https://dictionary.cambridge.org/", "Consulta externa de definiciones US/UK y ejemplos."),
]


def _upsert_educational_resource(
    conn: sqlite3.Connection,
    *,
    res_id: str,
    title: str,
    title_es: str,
    description: str,
    cefr_level: str,
    category_id: str,
    primary_skill: str,
    secondary_skills: list[str],
    topic: str,
    difficulty: int,
    estimated_duration_min: int,
    source_type: str,
    provider_id: str,
    author: str,
    original_url: str,
    content_type: str,
    license_id: str,
    attribution_id: str | None,
    availability_status: str,
    validation_status: str,
    cefr_confidence: float,
    cefr_classification_source: str,
    learning_objective_es: str,
    exercise_type: str,
    offline_compatible: int,
    entity_table: str,
    entity_id: str,
    payload: dict[str, Any],
    language: str = "en",
) -> None:
    content_hash = compute_content_hash(provider_id, res_id, f"{title}|{cefr_level}|{content_type}")
    conn.execute(
        """
        INSERT INTO educational_resources (
            id, title, title_es, description, language, cefr_level, category_id,
            primary_skill, secondary_skills_json, topic, difficulty, estimated_duration_min,
            source_type, provider_id, author, original_url, content_type, license_id,
            attribution_id, added_at, last_verified_at, availability_status, validation_status,
            cefr_confidence, cefr_classification_source, learning_objective_es, exercise_type,
            offline_compatible, content_hash, entity_table, entity_id, payload_json, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            title = excluded.title,
            title_es = excluded.title_es,
            description = excluded.description,
            language = excluded.language,
            cefr_level = excluded.cefr_level,
            category_id = excluded.category_id,
            primary_skill = excluded.primary_skill,
            secondary_skills_json = excluded.secondary_skills_json,
            topic = excluded.topic,
            difficulty = excluded.difficulty,
            estimated_duration_min = excluded.estimated_duration_min,
            source_type = excluded.source_type,
            provider_id = excluded.provider_id,
            author = excluded.author,
            original_url = excluded.original_url,
            content_type = excluded.content_type,
            license_id = excluded.license_id,
            attribution_id = excluded.attribution_id,
            last_verified_at = excluded.last_verified_at,
            availability_status = excluded.availability_status,
            validation_status = excluded.validation_status,
            cefr_confidence = excluded.cefr_confidence,
            cefr_classification_source = excluded.cefr_classification_source,
            learning_objective_es = excluded.learning_objective_es,
            exercise_type = excluded.exercise_type,
            offline_compatible = excluded.offline_compatible,
            content_hash = excluded.content_hash,
            entity_table = excluded.entity_table,
            entity_id = excluded.entity_id,
            payload_json = excluded.payload_json,
            updated_at = CASE
                WHEN educational_resources.payload_json != excluded.payload_json
                  OR educational_resources.title != excluded.title
                  OR educational_resources.cefr_level != excluded.cefr_level
                  OR educational_resources.validation_status != excluded.validation_status
                THEN excluded.updated_at
                ELSE educational_resources.updated_at
            END;
        """,
        (
            res_id,
            title,
            title_es,
            description,
            language,
            cefr_level,
            category_id,
            primary_skill,
            json.dumps(secondary_skills, ensure_ascii=False),
            topic,
            difficulty,
            estimated_duration_min,
            source_type,
            provider_id,
            author,
            original_url,
            content_type,
            license_id,
            attribution_id,
            SEED_TIMESTAMP,
            SEED_TIMESTAMP,
            availability_status,
            validation_status,
            cefr_confidence,
            cefr_classification_source,
            learning_objective_es,
            exercise_type,
            offline_compatible,
            content_hash,
            entity_table,
            entity_id,
            json.dumps(payload, ensure_ascii=False),
            SEED_TIMESTAMP,
        ),
    )


def _upsert_attribution(
    conn: sqlite3.Connection,
    *,
    attr_id: str,
    provider_id: str,
    license_id: str,
    author: str,
    attribution_text: str,
    source_url: str,
) -> str:
    conn.execute(
        """
        INSERT INTO content_attributions(
            id, provider_id, license_id, author, attribution_text, source_url, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            provider_id = excluded.provider_id,
            license_id = excluded.license_id,
            author = excluded.author,
            attribution_text = excluded.attribution_text,
            source_url = excluded.source_url;
        """,
        (attr_id, provider_id, license_id, author, attribution_text, source_url, SEED_TIMESTAMP),
    )
    return attr_id


def _seed_curated_open_and_external_links(conn: sqlite3.Connection, portals: list[dict[str, Any]]) -> None:
    """Siembra los 8 portales base + fichas externas autorizadas (British Council, OpenLearn, Common Voice)."""
    portal_provider_map = {
        "VOA Learning English": ("voa_learning_english", "PDM-1.0", "english_news", "listening", "A2"),
        "New General Service List (NGSL)": ("ngsl_nawl", "CC-BY-SA-4.0", "vocabulary_master", "vocabulary", "A2"),
        "Randall's ESL Listening Lab": ("randalls_esl_lab", "ALL-RIGHTS-RESERVED-LINK-ONLY", "listening_lab", "listening", "B1"),
        "British Council LearnEnglish": ("british_council", "ALL-RIGHTS-RESERVED-LINK-ONLY", "grammar_academy", "grammar", "B1"),
        "BBC Learning English": ("bbc_learning_english", "ALL-RIGHTS-RESERVED-LINK-ONLY", "english_news", "listening", "B2"),
        "YouGlish": ("youglish", "ALL-RIGHTS-RESERVED-LINK-ONLY", "pronunciation_studio", "pronunciation", "B1"),
        "Cambridge Dictionary": ("cambridge_dictionary", "ALL-RIGHTS-RESERVED-LINK-ONLY", "vocabulary_master", "vocabulary", "B1"),
        "Tatoeba & NAWL Academic": ("tatoeba", "CC-BY-2.0-FR", "academic_english", "vocabulary", "C1"),
    }

    for idx, portal in enumerate(portals, start=1):
        pid, lic_id, cat_id, skill, rep_cefr = portal_provider_map.get(
            portal["name"],
            ("fluentvoice_internal", "ALL-RIGHTS-RESERVED-LINK-ONLY", "reading_library", "reading", "B1"),
        )
        attr_id = _upsert_attribution(
            conn,
            attr_id=f"attr_portal_{idx}",
            provider_id=pid,
            license_id=lic_id,
            author=portal["name"],
            attribution_text=f"Portal oficial: {portal['name']} ({portal['cefr_badge']}) · Enlace externo autorizado.",
            source_url=portal["url"],
        )
        ext_id = f"ext_portal_{idx}"
        content_hash = compute_content_hash(pid, ext_id, portal["url"])
        conn.execute(
            """
            INSERT INTO external_resources (
                id, provider_id, external_id, cefr_level, category_id, primary_skill,
                title, title_es, description, language, original_url, content_type,
                access_mode, license_id, attribution_id, author, excerpt_or_payload_json,
                cefr_confidence, cefr_classification_source, validation_status,
                availability_status, offline_compatible, added_at, last_verified_at, content_hash
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'en', ?, 'external_portal', 'external_link', ?, ?, ?, ?, 0.85, 'provider_declared', 'validated', 'online_required', 0, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                cefr_level = excluded.cefr_level,
                category_id = excluded.category_id,
                primary_skill = excluded.primary_skill,
                title = excluded.title,
                title_es = excluded.title_es,
                description = excluded.description,
                original_url = excluded.original_url,
                license_id = excluded.license_id,
                attribution_id = excluded.attribution_id,
                excerpt_or_payload_json = excluded.excerpt_or_payload_json,
                last_verified_at = excluded.last_verified_at,
                content_hash = excluded.content_hash;
            """,
            (
                ext_id, pid, portal["url"], rep_cefr, cat_id, skill,
                portal["name"], portal["name"], portal["tagline_es"], portal["url"],
                lic_id, attr_id, portal["name"],
                json.dumps({"cefr_badge": portal["cefr_badge"], "category_es": portal["category_es"]}, ensure_ascii=False),
                SEED_TIMESTAMP, SEED_TIMESTAMP, content_hash,
            ),
        )
        _upsert_educational_resource(
            conn,
            res_id=ext_id,
            title=portal["name"],
            title_es=portal["name"],
            description=f"{portal['tagline_es']} ({portal['cefr_badge']}). Recurso externo enlazado directamente.",
            cefr_level=rep_cefr,
            category_id=cat_id,
            primary_skill=skill,
            secondary_skills=["comprehension"],
            topic="general_english",
            difficulty=4,
            estimated_duration_min=10,
            source_type="external_link",
            provider_id=pid,
            author=portal["name"],
            original_url=portal["url"],
            content_type="external_course",
            license_id=lic_id,
            attribution_id=attr_id,
            availability_status="online_required",
            validation_status="validated",
            cefr_confidence=0.85,
            cefr_classification_source="provider_declared",
            learning_objective_es=f"Explorar {portal['category_es'].lower()} en sitio oficial autorizado.",
            exercise_type="external_course_link",
            offline_compatible=0,
            entity_table="external_resources",
            entity_id=ext_id,
            payload={"url": portal["url"], "cefr_badge": portal["cefr_badge"], "notice_es": "Se abre en navegador respetando derechos de autor del sitio oficial."},
        )

    # Fichas curadas por enlace autorizado (British Council por nivel, OpenLearn OER, Mozilla Common Voice datasets)
    curated_external = [
        {
            "id": "ext_bc_grammar_a1_a2",
            "provider_id": "british_council",
            "license_id": "ALL-RIGHTS-RESERVED-LINK-ONLY",
            "access_mode": "external_link",
            "cefr": "A1",
            "category_id": "grammar_academy",
            "skill": "grammar",
            "title": "British Council — A1–A2 Grammar Practice",
            "title_es": "British Council — Gramática A1–A2 (Enlace oficial)",
            "desc": "Guías interactivas de gramática elemental A1–A2 en el sitio oficial del British Council.",
            "url": "https://learnenglish.britishcouncil.org/grammar/a1-a2-grammar",
            "author": "British Council",
            "attribution": "Enlace directo a British Council LearnEnglish (Todos los derechos reservados; acceso educativo gratuito sin afiliación).",
            "content_type": "external_course",
            "offline": 0,
        },
        {
            "id": "ext_bc_grammar_b1_b2",
            "provider_id": "british_council",
            "license_id": "ALL-RIGHTS-RESERVED-LINK-ONLY",
            "access_mode": "external_link",
            "cefr": "B1",
            "category_id": "grammar_academy",
            "skill": "grammar",
            "title": "British Council — B1–B2 Grammar Practice",
            "title_es": "British Council — Gramática B1–B2 (Enlace oficial)",
            "desc": "Condicionales, tiempos compuestos y modales B1–B2 en el sitio oficial del British Council.",
            "url": "https://learnenglish.britishcouncil.org/grammar/b1-b2-grammar",
            "author": "British Council",
            "attribution": "Enlace directo a British Council LearnEnglish (Todos los derechos reservados; sin copia de contenido protegido).",
            "content_type": "external_course",
            "offline": 0,
        },
        {
            "id": "ext_bc_skills_reading_c1",
            "provider_id": "british_council",
            "license_id": "ALL-RIGHTS-RESERVED-LINK-ONLY",
            "access_mode": "external_link",
            "cefr": "C1",
            "category_id": "reading_library",
            "skill": "reading",
            "title": "British Council — C1 Advanced Reading Zone",
            "title_es": "British Council — Lecturas C1 (Enlace oficial)",
            "desc": "Textos académicos y culturales C1 con ejercicios interactivos en el portal oficial.",
            "url": "https://learnenglish.britishcouncil.org/skills/reading/c1-reading",
            "author": "British Council",
            "attribution": "Enlace directo a British Council LearnEnglish Reading C1.",
            "content_type": "external_course",
            "offline": 0,
        },
        {
            "id": "ext_bc_listening_a2",
            "provider_id": "british_council",
            "license_id": "ALL-RIGHTS-RESERVED-LINK-ONLY",
            "access_mode": "external_link",
            "cefr": "A2",
            "category_id": "listening_lab",
            "skill": "listening",
            "title": "British Council — A2 Listening Practice",
            "title_es": "British Council — Audios A2 (Enlace oficial)",
            "desc": "Diálogos cotidianos y tareas de comprensión auditiva A2 en el sitio oficial.",
            "url": "https://learnenglish.britishcouncil.org/skills/listening/a2-listening",
            "author": "British Council",
            "attribution": "Enlace directo a British Council LearnEnglish Listening A2.",
            "content_type": "external_course",
            "offline": 0,
        },
        {
            "id": "ext_bc_business_english",
            "provider_id": "british_council",
            "license_id": "ALL-RIGHTS-RESERVED-LINK-ONLY",
            "access_mode": "external_link",
            "cefr": "B2",
            "category_id": "professional_english",
            "skill": "professional_english",
            "title": "British Council — Business Magazine & Emails",
            "title_es": "British Council — Inglés de Negocios (Enlace oficial)",
            "desc": "Artículos laborales y redacción ejecutiva de negocios B2–C1 en el sitio oficial.",
            "url": "https://learnenglish.britishcouncil.org/business-english",
            "author": "British Council",
            "attribution": "Enlace directo a British Council Business English.",
            "content_type": "external_course",
            "offline": 0,
        },
        {
            "id": "ext_openlearn_describing_lang",
            "provider_id": "openlearn",
            "license_id": "CC-BY-NC-SA-4.0",
            "access_mode": "external_link",
            "cefr": "B2",
            "cefr_source": "auto_estimated",
            "cefr_conf": 0.72,
            "validation_status": "pending_review",
            "category_id": "academic_english",
            "skill": "academic_english",
            "title": "OpenLearn — Describing Language",
            "title_es": "OpenLearn — Análisis lingüístico del inglés",
            "desc": "Curso abierto gratuito de The Open University sobre estructura, clases de palabras y gramática del inglés.",
            "url": "https://www.open.edu/openlearn/education-development/describing-language/content-section-0",
            "author": "The Open University",
            "attribution": "The Open University — OpenLearn (Licencia CC BY-NC-SA 4.0, enlaces canónicos; activos propietarios excluidos).",
            "content_type": "external_course",
            "offline": 0,
        },
        {
            "id": "ext_openlearn_essay_writing",
            "provider_id": "openlearn",
            "license_id": "CC-BY-NC-SA-4.0",
            "access_mode": "external_link",
            "cefr": "C1",
            "cefr_source": "auto_estimated",
            "cefr_conf": 0.72,
            "validation_status": "pending_review",
            "category_id": "writing_workshop",
            "skill": "writing",
            "title": "OpenLearn — Essay and Report Writing Skills",
            "title_es": "OpenLearn — Redacción de ensayos e informes",
            "desc": "Curso abierto universitario sobre estructura académica, argumentación y revisión textual.",
            "url": "https://www.open.edu/openlearn/education-development/essay-and-report-writing-skills/content-section-0",
            "author": "The Open University",
            "attribution": "The Open University — OpenLearn (CC BY-NC-SA 4.0).",
            "content_type": "external_course",
            "offline": 0,
        },
        {
            "id": "ext_mcv_english_dataset",
            "provider_id": "mozilla_common_voice",
            "license_id": "CC0-1.0",
            "access_mode": "external_link",
            "cefr": "B1",
            "cefr_source": "auto_estimated",
            "cefr_conf": 0.72,
            "validation_status": "pending_review",
            "category_id": "listening_lab",
            "skill": "listening",
            "title": "Mozilla Common Voice — English Scripted Speech Dataset (CC0)",
            "title_es": "Mozilla Common Voice — Corpus de Voz en Inglés (CC0)",
            "desc": "Dataset abierto global de frases leídas en múltiples acentos (US/UK/AU/IN) en Mozilla Data Collective. El motor permite importar sus archivos TSV localmente o con credencial MDC en servidor.",
            "url": "https://datacollective.mozillafoundation.org/datasets?q=Common%20Voice%20English",
            "author": "Mozilla Foundation Contributors",
            "attribution": "Mozilla Common Voice Corpus (Dedicación CC0 1.0 Dominio Público vía Mozilla Data Collective).",
            "content_type": "audio_clip",
            "offline": 0,
        },
        {
            "id": "ext_mcv_pronunciation_accents",
            "provider_id": "mozilla_common_voice",
            "license_id": "CC0-1.0",
            "access_mode": "external_link",
            "cefr": "B2",
            "cefr_source": "auto_estimated",
            "cefr_conf": 0.72,
            "validation_status": "pending_review",
            "category_id": "pronunciation_studio",
            "skill": "pronunciation",
            "title": "Mozilla Common Voice — Accents & Speech Diversity",
            "title_es": "Mozilla Common Voice — Diversidad de Acentos en Inglés",
            "desc": "Referencia de voz comunitaria para comparar articulación natural en distintos países angloparlantes mediante importación TSV local o descarga MDC con credencial.",
            "url": "https://commonvoice.mozilla.org/en/datasets",
            "author": "Mozilla Common Voice Community",
            "attribution": "Mozilla Common Voice CC0 1.0 Public Domain.",
            "content_type": "audio_clip",
            "offline": 0,
        },
    ]

    for item in curated_external:
        attr_id = _upsert_attribution(
            conn,
            attr_id=f"attr_{item['id']}",
            provider_id=item["provider_id"],
            license_id=item["license_id"],
            author=item["author"],
            attribution_text=item["attribution"],
            source_url=item["url"],
        )
        cefr_src = item.get("cefr_source", "provider_declared")
        cefr_conf = float(item.get("cefr_conf", 0.85))
        val_status = item.get("validation_status", "validated")
        chash = compute_content_hash(item["provider_id"], item["id"], item["url"])
        conn.execute(
            """
            INSERT INTO external_resources (
                id, provider_id, external_id, cefr_level, category_id, primary_skill,
                title, title_es, description, language, original_url, content_type,
                access_mode, license_id, attribution_id, author, excerpt_or_payload_json,
                cefr_confidence, cefr_classification_source, validation_status,
                availability_status, offline_compatible, added_at, last_verified_at, content_hash
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'en', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'online_required', ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                cefr_level = excluded.cefr_level,
                category_id = excluded.category_id,
                primary_skill = excluded.primary_skill,
                title = excluded.title,
                title_es = excluded.title_es,
                description = excluded.description,
                original_url = excluded.original_url,
                access_mode = excluded.access_mode,
                license_id = excluded.license_id,
                attribution_id = excluded.attribution_id,
                excerpt_or_payload_json = excluded.excerpt_or_payload_json,
                cefr_confidence = excluded.cefr_confidence,
                cefr_classification_source = excluded.cefr_classification_source,
                validation_status = excluded.validation_status,
                last_verified_at = excluded.last_verified_at,
                content_hash = excluded.content_hash;
            """,
            (
                item["id"], item["provider_id"], item["id"], item["cefr"],
                item["category_id"], item["skill"], item["title"], item["title_es"],
                item["desc"], item["url"], item["content_type"], item["access_mode"],
                item["license_id"], attr_id, item["author"],
                json.dumps({"url": item["url"], "attribution": item["attribution"]}, ensure_ascii=False),
                cefr_conf, cefr_src, val_status,
                item["offline"], SEED_TIMESTAMP, SEED_TIMESTAMP, chash,
            ),
        )
        _upsert_educational_resource(
            conn,
            res_id=item["id"],
            title=item["title"],
            title_es=item["title_es"],
            description=item["desc"],
            cefr_level=item["cefr"],
            category_id=item["category_id"],
            primary_skill=item["skill"],
            secondary_skills=["comprehension"],
            topic="general_english",
            difficulty=5,
            estimated_duration_min=15,
            source_type=item["access_mode"],
            provider_id=item["provider_id"],
            author=item["author"],
            original_url=item["url"],
            content_type=item["content_type"],
            license_id=item["license_id"],
            attribution_id=attr_id,
            availability_status="online_required",
            validation_status=val_status,
            cefr_confidence=cefr_conf,
            cefr_classification_source=cefr_src,
            learning_objective_es=f"Practicar {item['skill']} ({item['cefr']}) con recurso externo autorizado.",
            exercise_type="external_course_link",
            offline_compatible=item["offline"],
            entity_table="external_resources",
            entity_id=item["id"],
            payload={"url": item["url"], "attribution": item["attribution"], "license": item["license_id"]},
        )


def seed_all_internal_and_baseline(db_path: Path | str | None = None) -> dict[str, Any]:
    """Ejecuta migraciones y siembra todo el contenido base y propio ampliado."""
    mig = init_db(db_path=db_path)
    inv = extract_baseline_inventory()

    conn = get_connection(db_path=db_path)
    try:
        with conn:
            # 1. Niveles CEFR (6)
            for row in CEFR_LEVEL_ROWS:
                conn.execute(
                    """
                    INSERT INTO cefr_levels(code, level_id, rank, label_es, description_es, speech_rate)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(code) DO UPDATE SET
                        level_id = excluded.level_id,
                        rank = excluded.rank,
                        label_es = excluded.label_es,
                        description_es = excluded.description_es,
                        speech_rate = excluded.speech_rate;
                    """,
                    row,
                )

            # 2. Categorías (12)
            for row in CATEGORY_ROWS:
                conn.execute(
                    """
                    INSERT INTO educational_categories(id, slug, name_en, name_es, description_es, icon_name, display_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        slug = excluded.slug,
                        name_en = excluded.name_en,
                        name_es = excluded.name_es,
                        description_es = excluded.description_es,
                        icon_name = excluded.icon_name,
                        display_order = excluded.display_order;
                    """,
                    row,
                )

            # 3. Licencias y Proveedores
            for lic in STANDARD_LICENSES.values():
                conn.execute(
                    """
                    INSERT INTO content_licenses(
                        id, spdx_code, name, url, allows_storage, allows_commercial,
                        requires_attribution, requires_share_alike, storage_policy, notes
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        spdx_code = excluded.spdx_code,
                        name = excluded.name,
                        url = excluded.url,
                        allows_storage = excluded.allows_storage,
                        allows_commercial = excluded.allows_commercial,
                        requires_attribution = excluded.requires_attribution,
                        requires_share_alike = excluded.requires_share_alike,
                        storage_policy = excluded.storage_policy,
                        notes = excluded.notes;
                    """,
                    (
                        lic["id"], lic["spdx_code"], lic["name"], lic["url"],
                        lic["allows_storage"], lic["allows_commercial"],
                        lic["requires_attribution"], lic["requires_share_alike"],
                        lic["storage_policy"], lic["notes"],
                    ),
                )
            for prov in PROVIDER_ROWS:
                conn.execute(
                    """
                    INSERT INTO content_providers(
                        id, name, base_url, integration_mode, default_license_id,
                        requires_auth, rate_limit_rpm, is_enabled, terms_url, description_es
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        name = excluded.name,
                        base_url = excluded.base_url,
                        integration_mode = excluded.integration_mode,
                        default_license_id = excluded.default_license_id,
                        requires_auth = excluded.requires_auth,
                        rate_limit_rpm = excluded.rate_limit_rpm,
                        is_enabled = excluded.is_enabled,
                        terms_url = excluded.terms_url,
                        description_es = excluded.description_es;
                    """,
                    prov,
                )

            attr_internal = _upsert_attribution(
                conn,
                attr_id="attr_fluentvoice_internal",
                provider_id="fluentvoice_internal",
                license_id="FLUENTVOICE-OWNED",
                author="Equipo Pedagógico FluentVoice",
                attribution_text="Material original FluentVoice para enseñanza de inglés a hispanohablantes.",
                source_url="https://fluentvoice.local/",
            )

            # 4. Currículo base: 24 unidades, 96 lecciones, 288 tarjetas, 288 mini-tests
            for unit in inv["units"]:
                conn.execute(
                    """
                    INSERT INTO learning_units(
                        id, cefr_level, order_in_level, title_es, title_en, description_es, specialized_focus_es, icon_name
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                        cefr_level = excluded.cefr_level,
                        order_in_level = excluded.order_in_level,
                        title_es = excluded.title_es,
                        title_en = excluded.title_en,
                        description_es = excluded.description_es,
                        specialized_focus_es = excluded.specialized_focus_es,
                        icon_name = excluded.icon_name;
                    """,
                    (
                        unit["id"], unit["cefr_level"], unit["order_in_level"],
                        unit["title_es"], unit["title_en"], unit["description_es"],
                        unit["specialized_focus_es"], unit["icon_name"],
                    ),
                )
                cls_u = classify_resource(
                    unit["title_en"], unit["description_es"],
                    declared_cefr=unit["cefr_level"], category_hint="grammar_academy",
                    skill_hint="everyday_english", content_type="unit", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=unit["id"], title=unit["title_en"], title_es=unit["title_es"],
                    description=unit["description_es"], cefr_level=unit["cefr_level"],
                    category_id=cls_u.category_id, primary_skill=cls_u.primary_skill,
                    secondary_skills=cls_u.secondary_skills, topic=cls_u.topic,
                    difficulty=cls_u.difficulty, estimated_duration_min=18,
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="unit",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=unit["specialized_focus_es"], exercise_type="curriculum_unit",
                    offline_compatible=1, entity_table="learning_units", entity_id=unit["id"],
                    payload={"lesson_ids": [l["id"] for l in unit["lessons"]], "focus": unit["specialized_focus_es"]},
                )

                for lesson in unit["lessons"]:
                    conn.execute(
                        """
                        INSERT INTO lessons(
                            id, unit_id, cefr_level, order_in_unit, title_es, title_en,
                            duration_min, speaking_target_en, speaking_target_es, speaking_tip_es
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET
                            unit_id = excluded.unit_id,
                            cefr_level = excluded.cefr_level,
                            order_in_unit = excluded.order_in_unit,
                            title_es = excluded.title_es,
                            title_en = excluded.title_en,
                            duration_min = excluded.duration_min,
                            speaking_target_en = excluded.speaking_target_en,
                            speaking_target_es = excluded.speaking_target_es,
                            speaking_tip_es = excluded.speaking_tip_es;
                        """,
                        (
                            lesson["id"], lesson["unit_id"], lesson["cefr_level"],
                            lesson["order_in_unit"], lesson["title_es"], lesson["title_en"],
                            lesson["duration_min"], lesson["speaking_target_en"],
                            lesson["speaking_target_es"], lesson["speaking_tip_es"],
                        ),
                    )
                    cls_l = classify_resource(
                        lesson["title_en"], lesson["speaking_target_en"],
                        declared_cefr=lesson["cefr_level"], category_hint="grammar_academy",
                        skill_hint="everyday_english", content_type="lesson", source_is_curated=True,
                    )
                    _upsert_educational_resource(
                        conn,
                        res_id=lesson["id"], title=lesson["title_en"], title_es=lesson["title_es"],
                        description=f"{lesson['title_es']} · Objetivo oral: {lesson['speaking_target_en']}",
                        cefr_level=lesson["cefr_level"], category_id=cls_l.category_id,
                        primary_skill=cls_l.primary_skill, secondary_skills=["speaking", "grammar"],
                        topic=cls_l.topic, difficulty=cls_l.difficulty,
                        estimated_duration_min=int(lesson["duration_min"]),
                        source_type="owned", provider_id="fluentvoice_internal",
                        author="FluentVoice", original_url="", content_type="lesson",
                        license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                        availability_status="available", validation_status="validated",
                        cefr_confidence=1.0, cefr_classification_source="curated_internal",
                        learning_objective_es=lesson["speaking_tip_es"], exercise_type="interactive_lesson",
                        offline_compatible=1, entity_table="lessons", entity_id=lesson["id"],
                        payload={
                            "lesson_id": lesson["id"],
                            "unit_id": lesson["unit_id"],
                            "speaking_target_en": lesson["speaking_target_en"],
                            "speaking_target_es": lesson["speaking_target_es"],
                            "cards_count": len(lesson["items"]),
                        },
                    )

                    quiz_id = f"quiz_lesson_{lesson['id']}"
                    conn.execute(
                        """
                        INSERT INTO quizzes(
                            id, cefr_level, category_id, skill, title_en, title_es,
                            instructions_es, source_entity_type, source_entity_id, passing_score
                        ) VALUES (?, ?, 'grammar_academy', 'grammar', ?, ?, ?, 'lesson', ?, 70)
                        ON CONFLICT(id) DO UPDATE SET
                            cefr_level = excluded.cefr_level,
                            category_id = excluded.category_id,
                            skill = excluded.skill,
                            title_en = excluded.title_en,
                            title_es = excluded.title_es,
                            instructions_es = excluded.instructions_es,
                            source_entity_type = excluded.source_entity_type,
                            source_entity_id = excluded.source_entity_id,
                            passing_score = excluded.passing_score;
                        """,
                        (
                            quiz_id, lesson["cefr_level"],
                            f"Mini-test: {lesson['title_en']}",
                            f"Mini-evaluación: {lesson['title_es']}",
                            "Responde los 3 mini-tests paso a paso de la lección.",
                            lesson["id"],
                        ),
                    )
                    for item in lesson["items"]:
                        conn.execute(
                            """
                            INSERT OR REPLACE INTO teaching_cards(
                                id, lesson_id, order_in_lesson, kind, title_es, title_en,
                                explanation_es, model_en, model_es, tip_es
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                            """,
                            (
                                item["id"], lesson["id"], item["order_in_lesson"], item["kind"],
                                item["title_es"], item["title_en"], item["explanation_es"],
                                item["model_en"], item["model_es"], item["tip_es"],
                            ),
                        )
                        mt = item["mini_test"]
                        conn.execute(
                            """
                            INSERT OR REPLACE INTO quiz_questions(
                                id, quiz_id, order_index, kind, prompt_es, prompt_en,
                                audio_prompt_en, options_json, correct_index, expected_answer,
                                target_tokens_json, distraction_tokens_json, explanation_es
                            ) VALUES (?, ?, ?, ?, ?, '', ?, ?, ?, ?, ?, ?, ?);
                            """,
                            (
                                f"mt_{item['id']}", quiz_id, item["order_in_lesson"],
                                mt["kind"], mt["prompt_es"], mt["audio_prompt_en"],
                                json.dumps(mt["options"], ensure_ascii=False),
                                int(mt["correct_index"]),
                                " ".join(mt["target_tokens"]) if mt["target_tokens"] else (
                                    mt["options"][mt["correct_index"]] if mt["options"] else ""
                                ),
                                json.dumps(mt["target_tokens"], ensure_ascii=False),
                                json.dumps(mt["distraction_tokens"], ensure_ascii=False),
                                mt["explanation_es"],
                            ),
                        )

            # 5. Preguntas diagnósticas (36)
            for dq in inv["diagnostics"]:
                conn.execute(
                    """
                    INSERT OR REPLACE INTO diagnostic_questions(
                        id, cefr_level, question_type, difficulty, prompt_es, prompt_en,
                        options_json, correct_index, explanation_es
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                    """,
                    (
                        dq["id"], dq["cefr_level"], dq["question_type"], dq["difficulty"],
                        dq["prompt_es"], dq["prompt_en"],
                        json.dumps(dq["options"], ensure_ascii=False),
                        int(dq["correct_index"]), dq["explanation_es"],
                    ),
                )

            # 6. Escenarios base (48) + Escenarios especializados ampliados (12) = 60
            all_scenarios = [
                {**sc, "domain": "everyday", "is_baseline": 1} for sc in inv["scenarios"]
            ] + [
                {**sc, "is_baseline": 0} for sc in SPECIALIZED_SCENARIOS_EXPANSION
            ]
            for sc in all_scenarios:
                conn.execute(
                    """
                    INSERT OR REPLACE INTO conversation_scenarios(
                        id, cefr_level, title_es, title_en, subtitle_es, opening_en, opening_es,
                        starter_suggestions_json, icon_name, focus_skill_es, coach_intro_es, domain, is_baseline
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                    """,
                    (
                        sc["id"], sc["cefr_level"], sc["title_es"], sc["title_en"],
                        sc["subtitle_es"], sc["opening_en"], sc["opening_es"],
                        json.dumps(sc["starter_suggestions"], ensure_ascii=False),
                        sc["icon_name"], sc["focus_skill_es"], sc["coach_intro_es"],
                        sc["domain"], sc["is_baseline"],
                    ),
                )
                cat_for_sc = (
                    "academic_english" if sc["domain"] == "academic"
                    else "professional_english" if sc["domain"] == "professional"
                    else "real_conversations"
                )
                skill_for_sc = (
                    "academic_english" if sc["domain"] == "academic"
                    else "professional_english" if sc["domain"] == "professional"
                    else "speaking"
                )
                cls_sc = classify_resource(
                    sc["title_en"], sc["opening_en"],
                    declared_cefr=sc["cefr_level"], category_hint=cat_for_sc,
                    skill_hint=skill_for_sc, content_type="scenario", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=sc["id"], title=sc["title_en"], title_es=sc["title_es"],
                    description=sc["subtitle_es"], cefr_level=sc["cefr_level"],
                    category_id=cat_for_sc, primary_skill=skill_for_sc,
                    secondary_skills=["speaking", "everyday_english"], topic=cls_sc.topic,
                    difficulty=cls_sc.difficulty, estimated_duration_min=8,
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="scenario",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=sc["coach_intro_es"], exercise_type="roleplay_speaking",
                    offline_compatible=1, entity_table="conversation_scenarios", entity_id=sc["id"],
                    payload=sc,
                )

            # 7. Vocabulario: 288 base + 180 ampliación = 468
            all_vocab = [
                {
                    **v,
                    "id": f"vocab_{v['id']}",
                    "subcategory": "core",
                    "synonyms": [],
                    "antonyms": [],
                    "collocations": [],
                    "is_baseline": 1,
                }
                for v in inv["vocabulary"]
            ] + [
                {**v, "is_baseline": 0} for v in VOCABULARY_EXPANSION
            ]
            for v in all_vocab:
                conn.execute(
                    """
                    INSERT OR REPLACE INTO vocabulary(
                        id, cefr_level, category, subcategory, word, ipa, spanish,
                        example_en, example_es, tip_es, synonyms_json, antonyms_json,
                        collocations_json, is_baseline, source_provider_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'fluentvoice_internal');
                    """,
                    (
                        v["id"], v["cefr_level"], v["category"], v["subcategory"],
                        v["word"], v["ipa"], v["spanish"], v["example_en"],
                        v["example_es"], v["tip_es"],
                        json.dumps(v["synonyms"], ensure_ascii=False),
                        json.dumps(v["antonyms"], ensure_ascii=False),
                        json.dumps(v["collocations"], ensure_ascii=False),
                        v["is_baseline"],
                    ),
                )
                skill_v = (
                    "idiomatic_expressions" if v["subcategory"] in {"idiom", "phrasal_verb"}
                    else "academic_english" if v["subcategory"] == "academic"
                    else "professional_english" if v["subcategory"] == "professional"
                    else "vocabulary"
                )
                cls_v = classify_resource(
                    v["word"], v["example_en"],
                    declared_cefr=v["cefr_level"], category_hint="vocabulary_master",
                    skill_hint=skill_v, content_type="vocabulary", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=v["id"], title=v["word"], title_es=f"{v['word']} — {v['spanish']}",
                    description=f"{v['category']} ({v['ipa']}) · Ej.: {v['example_en']}",
                    cefr_level=v["cefr_level"], category_id="vocabulary_master",
                    primary_skill=skill_v, secondary_skills=["vocabulary", "pronunciation"],
                    topic=cls_v.topic, difficulty=cls_v.difficulty,
                    estimated_duration_min=2, source_type="owned",
                    provider_id="fluentvoice_internal", author="FluentVoice", original_url="",
                    content_type="vocabulary", license_id="FLUENTVOICE-OWNED",
                    attribution_id=attr_internal, availability_status="available",
                    validation_status="validated", cefr_confidence=1.0,
                    cefr_classification_source="curated_internal",
                    learning_objective_es=v["tip_es"], exercise_type="lexical_study",
                    offline_compatible=1, entity_table="vocabulary", entity_id=v["id"],
                    payload=v,
                )

            # 8. Ejercicios fonéticos: 30 base + 18 ampliación = 48
            all_drills = [
                {**d, "contrast_pair": "", "is_baseline": 1} for d in inv["drills"]
            ] + [
                {**d, "is_baseline": 0} for d in PHONETIC_EXPANSION
            ]
            for d in all_drills:
                conn.execute(
                    """
                    INSERT OR REPLACE INTO phonetic_exercises(
                        id, cefr_level, title, category, ipa, target_phrase, example_sentence,
                        spanish_tip, contrast_pair, is_baseline, source_provider_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'fluentvoice_internal');
                    """,
                    (
                        d["id"], d["cefr_level"], d["title"], d["category"],
                        d["ipa"], d["target_phrase"], d["example_sentence"],
                        d["spanish_tip"], d["contrast_pair"], d["is_baseline"],
                    ),
                )
                cls_d = classify_resource(
                    d["title"], d["target_phrase"],
                    declared_cefr=d["cefr_level"], category_hint="pronunciation_studio",
                    skill_hint="pronunciation", content_type="pronunciation", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=d["id"], title=d["title"], title_es=d["title"],
                    description=f"{d['ipa']} · {d['target_phrase']}",
                    cefr_level=d["cefr_level"], category_id="pronunciation_studio",
                    primary_skill="pronunciation", secondary_skills=["speaking", "listening"],
                    topic=cls_d.topic, difficulty=cls_d.difficulty, estimated_duration_min=3,
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="pronunciation",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=d["spanish_tip"], exercise_type="phonetic_drill",
                    offline_compatible=1, entity_table="phonetic_exercises", entity_id=d["id"],
                    payload=d,
                )

            # 9. Historias: 18 base + 6 ampliación = 24
            all_stories = [
                {**st, "is_baseline": 1} for st in inv["stories"]
            ] + [
                {**st, "is_baseline": 0} for st in ADDITIONAL_STORIES
            ]
            for st in all_stories:
                conn.execute(
                    """
                    INSERT OR REPLACE INTO stories(
                        id, cefr_level, title_es, title_en, duration_label, body_en, body_es,
                        vocab_json, questions_json, is_baseline, source_provider_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'fluentvoice_internal');
                    """,
                    (
                        st["id"], st["cefr_level"], st["title_es"], st["title_en"],
                        st["duration_label"], st["body_en"], st["body_es"],
                        json.dumps(st["vocab"], ensure_ascii=False),
                        json.dumps(st["questions"], ensure_ascii=False),
                        st["is_baseline"],
                    ),
                )
                cls_st = classify_resource(
                    st["title_en"], st["body_en"],
                    declared_cefr=st["cefr_level"], category_hint="reading_library",
                    skill_hint="comprehension", content_type="story", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=st["id"], title=st["title_en"], title_es=st["title_es"],
                    description=f"Historia interactiva con audio, vocabulario clave y quiz ({st['duration_label']}).",
                    cefr_level=st["cefr_level"], category_id="reading_library",
                    primary_skill="comprehension", secondary_skills=["reading", "listening"],
                    topic=cls_st.topic, difficulty=cls_st.difficulty, estimated_duration_min=4,
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="story",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=cls_st.learning_objective_es, exercise_type="interactive_story",
                    offline_compatible=1, entity_table="stories", entity_id=st["id"],
                    payload=st,
                )

            # 10. 36 Lecturas graduadas + 72 preguntas
            for rm in READING_MATERIALS_EXPANSION:
                quiz_id = f"quiz_{rm['id']}"
                conn.execute(
                    """
                    INSERT INTO quizzes(
                        id, cefr_level, category_id, skill, title_en, title_es,
                        instructions_es, source_entity_type, source_entity_id, passing_score
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'reading', ?, 70)
                    ON CONFLICT(id) DO UPDATE SET
                        cefr_level = excluded.cefr_level,
                        category_id = excluded.category_id,
                        skill = excluded.skill,
                        title_en = excluded.title_en,
                        title_es = excluded.title_es,
                        instructions_es = excluded.instructions_es,
                        source_entity_type = excluded.source_entity_type,
                        source_entity_id = excluded.source_entity_id,
                        passing_score = excluded.passing_score;
                    """,
                    (
                        quiz_id, rm["cefr_level"], rm["category_id"], rm["skill"],
                        f"Quiz: {rm['title_en']}", f"Comprensión: {rm['title_es']}",
                        "Lee o escucha la lectura y responde las preguntas de comprensión.",
                        rm["id"],
                    ),
                )
                for idx_q, q in enumerate(rm["questions"], start=1):
                    conn.execute(
                        """
                        INSERT OR REPLACE INTO quiz_questions(
                            id, quiz_id, order_index, kind, prompt_es, prompt_en,
                            audio_prompt_en, options_json, correct_index, expected_answer,
                            target_tokens_json, distraction_tokens_json, explanation_es
                        ) VALUES (?, ?, ?, 'MULTIPLE_CHOICE', ?, '', NULL, ?, ?, ?, '[]', '[]', ?);
                        """,
                        (
                            f"{quiz_id}_q{idx_q}", quiz_id, idx_q,
                            q["prompt_es"], json.dumps(q["options"], ensure_ascii=False),
                            int(q["correct_index"]), q["options"][q["correct_index"]],
                            q["explanation_es"],
                        ),
                    )
                word_count = len(rm["body_en"].split())
                est_min = max(3, min(10, (word_count // 35) + 2))
                conn.execute(
                    """
                    INSERT OR REPLACE INTO reading_materials(
                        id, cefr_level, category_id, skill, title_en, title_es, summary_es,
                        body_en, body_es, word_count, estimated_duration_min, topic,
                        key_vocabulary_json, comprehension_quiz_id, provider_id, license_id, original_url
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'fluentvoice_internal', 'FLUENTVOICE-OWNED', '');
                    """,
                    (
                        rm["id"], rm["cefr_level"], rm["category_id"], rm["skill"],
                        rm["title_en"], rm["title_es"], rm["summary_es"],
                        rm["body_en"], rm["body_es"], word_count, est_min,
                        rm["topic"], json.dumps(rm["key_vocabulary"], ensure_ascii=False), quiz_id,
                    ),
                )
                cls_r = classify_resource(
                    rm["title_en"], rm["body_en"],
                    declared_cefr=rm["cefr_level"], category_hint=rm["category_id"],
                    skill_hint=rm["skill"], content_type="reading", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=rm["id"], title=rm["title_en"], title_es=rm["title_es"],
                    description=rm["summary_es"], cefr_level=rm["cefr_level"],
                    category_id=rm["category_id"], primary_skill=rm["skill"],
                    secondary_skills=["comprehension", "vocabulary"], topic=rm["topic"],
                    difficulty=cls_r.difficulty, estimated_duration_min=est_min,
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="reading",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=cls_r.learning_objective_es, exercise_type="graded_reading",
                    offline_compatible=1, entity_table="reading_materials", entity_id=rm["id"],
                    payload={**rm, "quiz_id": quiz_id, "word_count": word_count},
                )

            # 11. 30 Materiales de Listening Lab & Dictados + 60 preguntas
            for lm in LISTENING_MATERIALS_EXPANSION:
                quiz_id = f"quiz_{lm['id']}"
                conn.execute(
                    """
                    INSERT INTO quizzes(
                        id, cefr_level, category_id, skill, title_en, title_es,
                        instructions_es, source_entity_type, source_entity_id, passing_score
                    ) VALUES (?, ?, 'listening_lab', 'listening', ?, ?, ?, 'listening', ?, 70)
                    ON CONFLICT(id) DO UPDATE SET
                        cefr_level = excluded.cefr_level,
                        category_id = excluded.category_id,
                        skill = excluded.skill,
                        title_en = excluded.title_en,
                        title_es = excluded.title_es,
                        instructions_es = excluded.instructions_es,
                        source_entity_type = excluded.source_entity_type,
                        source_entity_id = excluded.source_entity_id,
                        passing_score = excluded.passing_score;
                    """,
                    (
                        quiz_id, lm["cefr_level"],
                        f"Listening Quiz: {lm['title_en']}", f"Escucha: {lm['title_es']}",
                        lm["instructions_es"], lm["id"],
                    ),
                )
                for idx_q, q in enumerate(lm["questions"], start=1):
                    conn.execute(
                        """
                        INSERT OR REPLACE INTO quiz_questions(
                            id, quiz_id, order_index, kind, prompt_es, prompt_en,
                            audio_prompt_en, options_json, correct_index, expected_answer,
                            target_tokens_json, distraction_tokens_json, explanation_es
                        ) VALUES (?, ?, ?, 'LISTENING_CHOICE', ?, '', ?, ?, ?, ?, '[]', '[]', ?);
                        """,
                        (
                            f"{quiz_id}_q{idx_q}", quiz_id, idx_q,
                            q["prompt_es"], lm["transcript_en"],
                            json.dumps(q["options"], ensure_ascii=False),
                            int(q["correct_index"]), q["options"][q["correct_index"]],
                            q["explanation_es"],
                        ),
                    )
                conn.execute(
                    """
                    INSERT OR REPLACE INTO listening_materials(
                        id, cefr_level, category_id, title_en, title_es, instructions_es,
                        transcript_en, translation_es, dictation_target_en, accent_hint,
                        estimated_duration_min, audio_url, tts_fallback_allowed, hints_es,
                        comprehension_quiz_id, provider_id, license_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '', 1, ?, ?, 'fluentvoice_internal', 'FLUENTVOICE-OWNED');
                    """,
                    (
                        lm["id"], lm["cefr_level"], lm["category_id"], lm["title_en"], lm["title_es"],
                        lm["instructions_es"], lm["transcript_en"], lm["translation_es"],
                        lm["dictation_target_en"], lm["accent_hint"], lm["estimated_duration_min"],
                        lm["hints_es"], quiz_id,
                    ),
                )
                cls_lm = classify_resource(
                    lm["title_en"], lm["transcript_en"],
                    declared_cefr=lm["cefr_level"], category_hint="listening_lab",
                    skill_hint="listening", content_type="listening", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=lm["id"], title=lm["title_en"], title_es=lm["title_es"],
                    description=f"Audio + dictado guiado + comprensión ({lm['accent_hint']} · {lm['cefr_level']}).",
                    cefr_level=lm["cefr_level"], category_id="listening_lab",
                    primary_skill="listening", secondary_skills=["comprehension", "pronunciation"],
                    topic=cls_lm.topic, difficulty=cls_lm.difficulty,
                    estimated_duration_min=lm["estimated_duration_min"],
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="listening",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=lm["instructions_es"], exercise_type="listening_dictation",
                    offline_compatible=1, entity_table="listening_materials", entity_id=lm["id"],
                    payload={**lm, "quiz_id": quiz_id},
                )

            # 12. 60 Ejercicios de Grammar Academy
            for ge in GRAMMAR_EXERCISES_EXPANSION:
                conn.execute(
                    """
                    INSERT OR REPLACE INTO grammar_exercises(
                        id, cefr_level, category_id, exercise_type, title_en, title_es,
                        grammar_point, instructions_es, question_prompt_en, options_json,
                        expected_answer_en, explanation_es, spanish_trap_es
                    ) VALUES (?, ?, 'grammar_academy', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                    """,
                    (
                        ge["id"], ge["cefr_level"], ge["exercise_type"],
                        ge["title_en"], ge["title_es"], ge["grammar_point"],
                        ge["instructions_es"], ge["question_prompt_en"],
                        json.dumps(ge["options"], ensure_ascii=False),
                        ge["expected_answer_en"], ge["explanation_es"], ge["spanish_trap_es"],
                    ),
                )
                cls_g = classify_resource(
                    ge["title_en"], ge["question_prompt_en"],
                    declared_cefr=ge["cefr_level"], category_hint="grammar_academy",
                    skill_hint="grammar", content_type="grammar", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=ge["id"], title=ge["title_en"], title_es=ge["title_es"],
                    description=f"{ge['grammar_point']} · {ge['instructions_es']}",
                    cefr_level=ge["cefr_level"], category_id="grammar_academy",
                    primary_skill="grammar", secondary_skills=["writing", "comprehension"],
                    topic=cls_g.topic, difficulty=cls_g.difficulty, estimated_duration_min=3,
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="grammar",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=ge["explanation_es"], exercise_type=ge["exercise_type"],
                    offline_compatible=1, entity_table="grammar_exercises", entity_id=ge["id"],
                    payload=ge,
                )

            # 13. 24 Talleres de Writing Workshop
            for we in WRITING_EXERCISES_EXPANSION:
                conn.execute(
                    """
                    INSERT OR REPLACE INTO writing_exercises(
                        id, cefr_level, category_id, title_en, title_es, prompt_en,
                        instructions_es, structure_steps_json, evaluation_checklist_json,
                        model_answer_en, model_explanation_es, target_min_words, target_max_words
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                    """,
                    (
                        we["id"], we["cefr_level"], we["category_id"], we["title_en"], we["title_es"],
                        we["prompt_en"], we["instructions_es"],
                        json.dumps(we["structure_steps"], ensure_ascii=False),
                        json.dumps(we["evaluation_checklist"], ensure_ascii=False),
                        we["model_answer_en"], we["model_explanation_es"],
                        we["target_min_words"], we["target_max_words"],
                    ),
                )
                cls_w = classify_resource(
                    we["title_en"], we["prompt_en"],
                    declared_cefr=we["cefr_level"], category_hint=we["category_id"],
                    skill_hint="writing", content_type="writing", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=we["id"], title=we["title_en"], title_es=we["title_es"],
                    description=we["instructions_es"], cefr_level=we["cefr_level"],
                    category_id=we["category_id"], primary_skill="writing",
                    secondary_skills=["grammar", "vocabulary"], topic=cls_w.topic,
                    difficulty=cls_w.difficulty, estimated_duration_min=8,
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="writing",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=we["model_explanation_es"], exercise_type="guided_writing",
                    offline_compatible=1, entity_table="writing_exercises", entity_id=we["id"],
                    payload=we,
                )

            # 14. 6 Simulacros Exam Preparation (36 preguntas)
            for ex in EXAM_PREPARATION_QUIZZES:
                conn.execute(
                    """
                    INSERT INTO quizzes(
                        id, cefr_level, category_id, skill, title_en, title_es,
                        instructions_es, source_entity_type, source_entity_id, passing_score
                    ) VALUES (?, ?, 'exam_preparation', 'comprehension', ?, ?, ?, 'exam', ?, 70)
                    ON CONFLICT(id) DO UPDATE SET
                        cefr_level = excluded.cefr_level,
                        category_id = excluded.category_id,
                        skill = excluded.skill,
                        title_en = excluded.title_en,
                        title_es = excluded.title_es,
                        instructions_es = excluded.instructions_es,
                        source_entity_type = excluded.source_entity_type,
                        source_entity_id = excluded.source_entity_id,
                        passing_score = excluded.passing_score;
                    """,
                    (
                        ex["id"], ex["cefr_level"], ex["title_en"], ex["title_es"],
                        ex["instructions_es"], ex["id"],
                    ),
                )
                for idx_q, q in enumerate(ex["questions"], start=1):
                    conn.execute(
                        """
                        INSERT OR REPLACE INTO quiz_questions(
                            id, quiz_id, order_index, kind, prompt_es, prompt_en,
                            audio_prompt_en, options_json, correct_index, expected_answer,
                            target_tokens_json, distraction_tokens_json, explanation_es
                        ) VALUES (?, ?, ?, ?, ?, '', NULL, ?, ?, ?, '[]', '[]', ?);
                        """,
                        (
                            f"{ex['id']}_q{idx_q}", ex["id"], idx_q, q["kind"],
                            q["prompt_es"], json.dumps(q["options"], ensure_ascii=False),
                            int(q["correct_index"]), q["options"][q["correct_index"]],
                            q["explanation_es"],
                        ),
                    )
                cls_ex = classify_resource(
                    ex["title_en"], ex["instructions_es"],
                    declared_cefr=ex["cefr_level"], category_hint="exam_preparation",
                    skill_hint="comprehension", content_type="quiz", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=ex["id"], title=ex["title_en"], title_es=ex["title_es"],
                    description=ex["instructions_es"], cefr_level=ex["cefr_level"],
                    category_id="exam_preparation", primary_skill="comprehension",
                    secondary_skills=["grammar", "vocabulary"], topic="academic_discourse",
                    difficulty=cls_ex.difficulty, estimated_duration_min=10,
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="quiz",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=ex["instructions_es"], exercise_type="exam_simulation",
                    offline_compatible=1, entity_table="quizzes", entity_id=ex["id"],
                    payload=ex,
                )

            # 15. 12 Word Games (36 preguntas)
            for wg in WORD_GAMES_EXPANSION:
                conn.execute(
                    """
                    INSERT INTO quizzes(
                        id, cefr_level, category_id, skill, title_en, title_es,
                        instructions_es, source_entity_type, source_entity_id, passing_score
                    ) VALUES (?, ?, 'word_games', 'vocabulary', ?, ?, ?, 'word_game', ?, 70)
                    ON CONFLICT(id) DO UPDATE SET
                        cefr_level = excluded.cefr_level,
                        category_id = excluded.category_id,
                        skill = excluded.skill,
                        title_en = excluded.title_en,
                        title_es = excluded.title_es,
                        instructions_es = excluded.instructions_es,
                        source_entity_type = excluded.source_entity_type,
                        source_entity_id = excluded.source_entity_id,
                        passing_score = excluded.passing_score;
                    """,
                    (
                        wg["id"], wg["cefr_level"], wg["title_en"], wg["title_es"],
                        wg["instructions_es"], wg["id"],
                    ),
                )
                for idx_q, q in enumerate(wg["questions"], start=1):
                    conn.execute(
                        """
                        INSERT OR REPLACE INTO quiz_questions(
                            id, quiz_id, order_index, kind, prompt_es, prompt_en,
                            audio_prompt_en, options_json, correct_index, expected_answer,
                            target_tokens_json, distraction_tokens_json, explanation_es
                        ) VALUES (?, ?, ?, 'MULTIPLE_CHOICE', ?, '', NULL, ?, ?, ?, '[]', '[]', ?);
                        """,
                        (
                            f"{wg['id']}_q{idx_q}", wg["id"], idx_q,
                            q["prompt_es"], json.dumps(q["options"], ensure_ascii=False),
                            int(q["correct_index"]), q["options"][q["correct_index"]],
                            q["explanation_es"],
                        ),
                    )
                cls_wg = classify_resource(
                    wg["title_en"], wg["instructions_es"],
                    declared_cefr=wg["cefr_level"], category_hint="word_games",
                    skill_hint="vocabulary", content_type="word_game", source_is_curated=True,
                )
                _upsert_educational_resource(
                    conn,
                    res_id=wg["id"], title=wg["title_en"], title_es=wg["title_es"],
                    description=wg["instructions_es"], cefr_level=wg["cefr_level"],
                    category_id="word_games", primary_skill="vocabulary",
                    secondary_skills=["idiomatic_expressions", "comprehension"], topic="general_english",
                    difficulty=cls_wg.difficulty, estimated_duration_min=4,
                    source_type="owned", provider_id="fluentvoice_internal",
                    author="FluentVoice", original_url="", content_type="word_game",
                    license_id="FLUENTVOICE-OWNED", attribution_id=attr_internal,
                    availability_status="available", validation_status="validated",
                    cefr_confidence=1.0, cefr_classification_source="curated_internal",
                    learning_objective_es=wg["instructions_es"], exercise_type="semantic_game",
                    offline_compatible=1, entity_table="quizzes", entity_id=wg["id"],
                    payload=wg,
                )

            # 16. Portales base y enlaces externos curados iniciales
            _seed_curated_open_and_external_links(conn, inv["portals"])

        # Recopilar conteos reales verificados
        tables_to_count = [
            "cefr_levels", "educational_categories", "learning_units", "lessons",
            "teaching_cards", "quizzes", "quiz_questions", "vocabulary",
            "phonetic_exercises", "stories", "reading_materials", "listening_materials",
            "conversation_scenarios", "writing_exercises", "grammar_exercises",
            "external_resources", "content_providers", "content_licenses",
            "content_attributions", "educational_resources", "diagnostic_questions",
        ]
        counts = {
            tbl: int(conn.execute(f"SELECT COUNT(*) FROM {tbl};").fetchone()[0])
            for tbl in tables_to_count
        }
        return {"migration": mig, "counts": counts}
    finally:
        conn.close()
