"""
Motor de personalización y generación de rutas de aprendizaje (Fase 7).

Señales utilizadas:
- Nivel CEFR actual del estudiante
- Puntajes de evaluación diagnóstica (listening, grammar, vocabulary, speaking)
- Resultados de mini-tests, pronunciación y errores frecuentes
- Vocabulario conocido y tiempo de práctica semanal
- Historial y exclusión de recursos ya completados o dominados
"""

from __future__ import annotations

import json
import sqlite3
from dataclasses import dataclass, field
from typing import Any

from edu_db.migrations import utc_now_iso
from edu_db.schema import CEFR_CODES

SKILL_TO_PLAN_CLUSTER = {
    "listening": {
        "title_es": "Refuerzo integral de Comprensión Auditiva",
        "types": ["listening", "audio_clip", "story", "scenario", "vocabulary"],
        "related_skills": ["listening", "comprehension", "speaking", "vocabulary", "pronunciation"],
    },
    "grammar": {
        "title_es": "Precisión Gramatical y Construcción",
        "types": ["grammar", "lesson", "sentence_pair", "writing", "quiz"],
        "related_skills": ["grammar", "writing", "comprehension"],
    },
    "vocabulary": {
        "title_es": "Expansión Léxica en Contexto",
        "types": ["vocabulary", "word_game", "reading", "story", "sentence_pair"],
        "related_skills": ["vocabulary", "idiomatic_expressions", "reading", "comprehension"],
    },
    "speaking": {
        "title_es": "Fluidez Oral y Pronunciación",
        "types": ["scenario", "pronunciation", "lesson", "listening"],
        "related_skills": ["speaking", "pronunciation", "everyday_english", "listening"],
    },
    "reading": {
        "title_es": "Lectura Activa y Comprensión",
        "types": ["reading", "story", "book", "quiz"],
        "related_skills": ["reading", "comprehension", "academic_english", "vocabulary"],
    },
    "writing": {
        "title_es": "Taller de Escritura Estructurada",
        "types": ["writing", "grammar", "reading"],
        "related_skills": ["writing", "grammar", "professional_english", "academic_english"],
    },
}


@dataclass
class LearnerProfileInput:
    user_id: str = "default_learner"
    cefr_level: str = "A2"
    listening_score: int = 75
    grammar_score: int = 75
    vocabulary_score: int = 75
    speaking_score: int = 75
    pronunciation_score: int = 75
    weak_skills: list[str] = field(default_factory=list)
    error_tags: list[str] = field(default_factory=list)
    completed_ids: set[str] = field(default_factory=set)
    mastered_ids: set[str] = field(default_factory=set)
    known_vocab_count: int = 0
    weekly_practice_minutes: int = 0
    offline_only: bool = False


def _detect_weakest_skills(profile: LearnerProfileInput, db_errors: list[str]) -> list[str]:
    scores = {
        "listening": profile.listening_score,
        "grammar": profile.grammar_score,
        "vocabulary": profile.vocabulary_score,
        "speaking": min(profile.speaking_score, profile.pronunciation_score),
    }
    ordered = [k for k, _ in sorted(scores.items(), key=lambda kv: kv[1])]
    explicit = [s for s in profile.weak_skills if s]
    combined: list[str] = []
    for s in explicit + ordered:
        if s not in combined:
            combined.append(s)

    # Ajustar por errores registrados en BD si existen
    error_blob = " ".join(profile.error_tags + db_errors).lower()
    if any(k in error_blob for k in ("listening", "dictation", "audio")) and "listening" in combined:
        combined.remove("listening")
        combined.insert(0, "listening")
    elif any(k in error_blob for k in ("grammar", "tense", "conditional", "prep")) and "grammar" in combined:
        combined.remove("grammar")
        combined.insert(0, "grammar")
    return combined[:3]


def generate_personalized_recommendations(
    conn: sqlite3.Connection,
    profile: LearnerProfileInput,
    limit: int = 10,
    save_path: bool = True,
) -> dict[str, Any]:
    target_cefr = profile.cefr_level if profile.cefr_level in CEFR_CODES else "A2"
    target_idx = CEFR_CODES.index(target_cefr)

    # Cargar progreso e historial real del usuario en BD para no repetir dominados
    db_progress_rows = conn.execute(
        "SELECT resource_id, status, score, error_tags_json FROM user_progress WHERE user_id = ?;",
        (profile.user_id,),
    ).fetchall()
    db_mastered: set[str] = set()
    db_completed: set[str] = set()
    db_in_progress: set[str] = set()
    db_errors: list[str] = []

    for pr in db_progress_rows:
        rid = pr["resource_id"]
        st = pr["status"]
        if st == "mastered":
            db_mastered.add(rid)
        elif st == "completed":
            db_completed.add(rid)
        elif st == "in_progress":
            db_in_progress.add(rid)
        try:
            tags = json.loads(pr["error_tags_json"] or "[]")
            if isinstance(tags, list):
                db_errors.extend(str(t) for t in tags)
        except json.JSONDecodeError:
            pass

    recent_rows = conn.execute(
        """
        SELECT DISTINCT resource_id FROM content_history
        WHERE user_id = ? ORDER BY created_at DESC LIMIT 12;
        """,
        (profile.user_id,),
    ).fetchall()
    recent_ids = {r["resource_id"] for r in recent_rows}

    excluded_mastered = profile.mastered_ids | db_mastered
    completed_all = profile.completed_ids | db_completed

    weak_skills = _detect_weakest_skills(profile, db_errors)
    primary_weak = weak_skills[0] if weak_skills else "listening"
    cluster = SKILL_TO_PLAN_CLUSTER.get(primary_weak, SKILL_TO_PLAN_CLUSTER["listening"])

    query = "SELECT * FROM educational_resources WHERE validation_status != 'rejected'"
    params: list[Any] = []
    if profile.offline_only:
        query += " AND offline_compatible = 1"

    rows = conn.execute(query, params).fetchall()

    # Evitar inundar recomendaciones con vocabulario suelto si hay actividades completas
    scored: list[tuple[float, dict[str, Any], str]] = []
    error_terms = {t.lower() for t in (profile.error_tags + db_errors) if len(t) >= 3}

    for r in rows:
        rid = r["id"]
        if rid in excluded_mastered:
            continue

        row_cefr = r["cefr_level"]
        if row_cefr not in CEFR_CODES:
            continue
        row_idx = CEFR_CODES.index(row_cefr)
        level_diff = abs(row_idx - target_idx)
        # Priorizar nivel exacto; permitir +1 suave solo si está cerca
        if level_diff > 1 or row_idx < target_idx - 1:
            continue

        score = 0.0
        reasons: list[str] = []

        if row_cefr == target_cefr:
            score += 45.0
            reasons.append(f"Ajustado a tu nivel {target_cefr}")
        else:
            score += 12.0

        pskill = r["primary_skill"]
        ctype = r["content_type"]

        if pskill == primary_weak:
            score += 36.0
            reasons.append(f"Refuerza tu habilidad prioritaria ({primary_weak})")
        elif pskill in cluster["related_skills"]:
            score += 24.0
            reasons.append(f"Apoya directamente {primary_weak}")
        elif pskill in weak_skills:
            score += 18.0
            reasons.append(f"Fortalece {pskill}")

        if ctype in cluster["types"]:
            priority_idx = cluster["types"].index(ctype)
            score += max(8.0, 22.0 - priority_idx * 3.0)

        if ctype == "unit":
            score -= 10.0  # Mejor recomendar actividades accionables concretas que unidades contenedoras
        if ctype == "vocabulary":
            score -= 8.0   # Deja vocabulario estructurado complementario sin tapar audios/lecturas

        if rid in db_in_progress:
            score += 18.0
            reasons.append("Retomar actividad iniciada")
        elif rid in completed_all:
            score -= 35.0
        elif rid in recent_ids:
            score -= 8.0

        # Afinidad con errores frecuentes registrados
        blob = f"{r['title']} {r['title_es']} {r['description']} {r['learning_objective_es']}".lower()
        if any(err in blob for err in error_terms):
            score += 20.0
            reasons.append("Atiende errores frecuentes detectados")

        if profile.weekly_practice_minutes < 40 and int(r["estimated_duration_min"]) <= 5:
            score += 8.0

        if r["source_type"] == "owned":
            score += 6.0

        item_dict = {
            "id": rid,
            "title": r["title"],
            "title_es": r["title_es"],
            "description": r["description"],
            "cefr_level": row_cefr,
            "category_id": r["category_id"],
            "primary_skill": pskill,
            "content_type": ctype,
            "source_type": r["source_type"],
            "provider_id": r["provider_id"],
            "estimated_duration_min": r["estimated_duration_min"],
            "offline_compatible": bool(r["offline_compatible"]),
            "license_id": r["license_id"],
            "original_url": r["original_url"],
            "cefr_confidence": r["cefr_confidence"],
            "validation_status": r["validation_status"],
            "recommendation_score": round(score, 2),
            "reason_es": reasons[0] if reasons else f"Recomendado para nivel {target_cefr}",
        }
        scored.append((score, item_dict, item_dict["reason_es"]))

    scored.sort(key=lambda t: t[0], reverse=True)

    # Asegurar diversidad pedagógica dentro del clúster (listening + dictado + lectura/historia + conversación + vocabulario)
    selected: list[dict[str, Any]] = []
    seen_types: dict[str, int] = {}
    for _, item, _ in scored:
        ct = item["content_type"]
        cap = 3 if ct in {cluster["types"][0], "listening", "reading", "grammar"} else 2
        if seen_types.get(ct, 0) >= cap:
            continue
        selected.append(item)
        seen_types[ct] = seen_types.get(ct, 0) + 1
        if len(selected) >= limit:
            break

    if len(selected) < limit:
        picked_ids = {s["id"] for s in selected}
        for _, item, _ in scored:
            if item["id"] not in picked_ids:
                selected.append(item)
                picked_ids.add(item["id"])
                if len(selected) >= limit:
                    break

    path_steps = selected[:6]
    total_min = sum(int(s["estimated_duration_min"]) for s in path_steps)
    path_id = f"path_{profile.user_id}_{target_cefr}_{primary_weak}"
    path_title = f"{cluster['title_es']} · Nivel {target_cefr}"
    rationale_es = (
        f"Ruta calibrada para {target_cefr} priorizando '{primary_weak}' según tu diagnóstico "
        f"(Listening {profile.listening_score}%, Gramática {profile.grammar_score}%, "
        f"Vocabulario {profile.vocabulary_score}%, Speaking {profile.speaking_score}%), "
        f"excluyendo {len(excluded_mastered)} recursos ya dominados."
    )
    now_ts = utc_now_iso()
    if save_path:
        with conn:
            conn.execute(
                """
                INSERT OR REPLACE INTO learning_paths(
                    id, user_id, cefr_level, focus_skill, title_es, description_es,
                    resource_ids_json, rationale_es, estimated_minutes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """,
                (
                    path_id,
                    profile.user_id,
                    target_cefr,
                    primary_weak,
                    path_title,
                    "Secuencia recomendada combinando práctica guiada, comprensión, conversación y vocabulario relacionado.",
                    json.dumps([s["id"] for s in path_steps], ensure_ascii=False),
                    rationale_es,
                    total_min,
                    now_ts,
                    now_ts,
                ),
            )

    return {
        "user_id": profile.user_id,
        "cefr_level": target_cefr,
        "weakest_skills": weak_skills,
        "excluded_mastered_count": len(excluded_mastered),
        "learning_path": {
            "id": path_id,
            "title_es": path_title,
            "focus_skill": primary_weak,
            "estimated_minutes": total_min,
            "rationale_es": rationale_es,
            "steps": path_steps,
        },
        "recommendations": selected,
    }
