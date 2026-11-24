"""
Router FastAPI (/api/v1/edu) para catálogo paginado, búsqueda, niveles, habilidades,
recomendaciones, favoritos, historial, progreso, sincronización incremental y agregación.
"""

from __future__ import annotations

import hmac
import json
import os
import sys
import time
from collections import defaultdict, deque
from pathlib import Path
from typing import Any, Literal

from fastapi import APIRouter, Header, HTTPException, Query, Request
from pydantic import BaseModel, Field

from edu_db.aggregator import ContentAggregationEngine, check_links_availability
from edu_db.cache import edu_cache
from edu_db.migrations import get_connection, utc_now_iso
from edu_db.recommender import LearnerProfileInput, generate_personalized_recommendations
from edu_db.schema import CEFR_CODES

router = APIRouter(prefix="/api/v1/edu", tags=["Educational Hub"])

_edu_hits: dict[str, deque[float]] = defaultdict(deque)
_EDU_RATE_LIMIT = int(os.getenv("EDU_RATE_LIMIT_PER_MIN", "120"))


def _db_path_override() -> Path | str | None:
    val = os.getenv("EDU_DB_PATH", "").strip()
    return val if val else None


def _check_edu_rate(request: Request) -> None:
    fwd = request.headers.get("x-forwarded-for")
    ip = fwd.split(",")[0].strip() if fwd else (request.client.host if request.client else "unknown")
    now = time.time()
    q = _edu_hits[ip]
    while q and now - q[0] > 60:
        q.popleft()
    if len(q) >= _EDU_RATE_LIMIT:
        raise HTTPException(429, "Demasiadas consultas al catálogo educativo. Espera un momento.")
    q.append(now)


def _verify_app_token(x_app_token: str | None) -> None:
    main_mod = sys.modules.get("main")
    if main_mod is not None and hasattr(main_mod, "APP_TOKEN"):
        app_token = getattr(main_mod, "APP_TOKEN") or ""
    else:
        app_token = os.getenv("APP_TOKEN", "")
    if app_token and not hmac.compare_digest(x_app_token or "", app_token):
        raise HTTPException(401, "No autorizado. Incluye un encabezado X-App-Token válido.")


_verify_admin_token = _verify_app_token


def _serialize_resource_row(r: Any, include_payload: bool = False) -> dict[str, Any]:
    out = {
        "id": r["id"],
        "title": r["title"],
        "title_es": r["title_es"],
        "description": r["description"],
        "language": r["language"],
        "cefr_level": r["cefr_level"],
        "category_id": r["category_id"],
        "primary_skill": r["primary_skill"],
        "secondary_skills": json.loads(r["secondary_skills_json"] or "[]"),
        "topic": r["topic"],
        "difficulty": int(r["difficulty"]),
        "estimated_duration_min": int(r["estimated_duration_min"]),
        "source_type": r["source_type"],
        "provider_id": r["provider_id"],
        "author": r["author"],
        "original_url": r["original_url"],
        "content_type": r["content_type"],
        "license_id": r["license_id"],
        "attribution_id": r["attribution_id"],
        "added_at": r["added_at"],
        "last_verified_at": r["last_verified_at"],
        "availability_status": r["availability_status"],
        "validation_status": r["validation_status"],
        "cefr_confidence": float(r["cefr_confidence"]),
        "cefr_classification_source": r["cefr_classification_source"],
        "learning_objective_es": r["learning_objective_es"],
        "exercise_type": r["exercise_type"],
        "offline_compatible": bool(r["offline_compatible"]),
        "updated_at": r["updated_at"],
    }
    if include_payload:
        try:
            out["payload"] = json.loads(r["payload_json"] or "{}")
        except json.JSONDecodeError:
            out["payload"] = {}
    return out


class RecommendationRequest(BaseModel):
    user_id: str = Field(default="default_learner", min_length=1, max_length=64)
    cefr_level: Literal["A1", "A2", "B1", "B2", "C1", "C2"] = "A2"
    listening_score: int = Field(default=75, ge=0, le=100)
    grammar_score: int = Field(default=75, ge=0, le=100)
    vocabulary_score: int = Field(default=75, ge=0, le=100)
    speaking_score: int = Field(default=75, ge=0, le=100)
    pronunciation_score: int = Field(default=75, ge=0, le=100)
    weak_skills: list[str] = Field(default_factory=list, max_length=6)
    error_tags: list[str] = Field(default_factory=list, max_length=20)
    completed_ids: list[str] = Field(default_factory=list, max_length=500)
    mastered_ids: list[str] = Field(default_factory=list, max_length=500)
    known_vocab_count: int = Field(default=0, ge=0)
    weekly_practice_minutes: int = Field(default=0, ge=0)
    offline_only: bool = False
    limit: int = Field(default=10, ge=1, le=40)


class ProgressUpsertRequest(BaseModel):
    user_id: str = Field(default="default_learner", min_length=1, max_length=64)
    resource_id: str = Field(min_length=1, max_length=120)
    status: Literal["not_started", "in_progress", "completed", "mastered"]
    score: float = Field(default=0.0, ge=0.0, le=100.0)
    time_spent_sec: int = Field(default=0, ge=0, le=86400)
    last_position: str = Field(default="", max_length=160)
    error_tags: list[str] = Field(default_factory=list, max_length=20)
    xp_earned: int = Field(default=10, ge=0, le=200)


class FavoriteRequest(BaseModel):
    user_id: str = Field(default="default_learner", min_length=1, max_length=64)
    resource_id: str = Field(min_length=1, max_length=120)


class HistoryRequest(BaseModel):
    user_id: str = Field(default="default_learner", min_length=1, max_length=64)
    resource_id: str = Field(min_length=1, max_length=120)
    action: Literal["viewed", "started", "completed", "favorited", "practiced", "external_opened"] = "viewed"
    duration_sec: int = Field(default=0, ge=0, le=86400)
    score: float | None = Field(default=None, ge=0.0, le=100.0)


class AggregateRequest(BaseModel):
    provider_ids: list[str] | None = None
    limit_per_provider: int = Field(default=6, ge=1, le=20)


@router.get("/stats")
async def edu_stats(request: Request):
    _check_edu_rate(request)
    cache_key = "stats"
    cached = edu_cache.get(cache_key)
    if cached:
        return {**cached, "cache": edu_cache.stats()}
    conn = get_connection(_db_path_override())
    try:
        counts = {}
        for tbl in (
            "cefr_levels", "educational_categories", "learning_units", "lessons",
            "teaching_cards", "quizzes", "quiz_questions", "vocabulary",
            "phonetic_exercises", "stories", "reading_materials", "listening_materials",
            "conversation_scenarios", "writing_exercises", "grammar_exercises",
            "external_resources", "content_providers", "content_licenses",
            "content_attributions", "learning_paths", "user_favorites",
            "user_progress", "user_activity", "content_history",
            "educational_resources", "diagnostic_questions",
        ):
            counts[tbl] = int(conn.execute(f"SELECT COUNT(*) FROM {tbl};").fetchone()[0])

        by_level = {
            r["cefr_level"]: int(r["c"])
            for r in conn.execute("SELECT cefr_level, COUNT(*) AS c FROM educational_resources GROUP BY cefr_level;").fetchall()
        }
        by_source = {
            r["source_type"]: int(r["c"])
            for r in conn.execute("SELECT source_type, COUNT(*) AS c FROM educational_resources GROUP BY source_type;").fetchall()
        }
        res = {"counts": counts, "by_level": by_level, "by_source": by_source}
        edu_cache.set(cache_key, res, ttl_sec=60)
        return {**res, "cache": edu_cache.stats()}
    finally:
        conn.close()


def _query_catalog(
    *,
    page: int,
    page_size: int,
    level: str | None = None,
    category: str | None = None,
    skill: str | None = None,
    source_type: str | None = None,
    provider: str | None = None,
    offline_only: bool = False,
    include_vocab: bool = True,
    q: str | None = None,
) -> dict[str, Any]:
    cache_key = f"cat:{page}:{page_size}:{level}:{category}:{skill}:{source_type}:{provider}:{offline_only}:{include_vocab}:{q}"
    cached = edu_cache.get(cache_key)
    if cached:
        return cached

    conn = get_connection(_db_path_override())
    try:
        where = ["1=1"]
        params: list[Any] = []
        if level:
            lvl = level.upper().strip()
            if lvl not in CEFR_CODES:
                raise HTTPException(400, f"Nivel CEFR inválido: {level}")
            where.append("cefr_level = ?")
            params.append(lvl)
        if category:
            where.append("category_id = ?")
            params.append(category.strip())
        if skill:
            where.append("(primary_skill = ? OR secondary_skills_json LIKE ?)")
            params.extend([skill.strip(), f'%"{skill.strip()}"%'])
        if source_type:
            where.append("source_type = ?")
            params.append(source_type.strip())
        if provider:
            where.append("provider_id = ?")
            params.append(provider.strip())
        if offline_only:
            where.append("offline_compatible = 1")
        if not include_vocab and not q and category != "vocabulary_master":
            where.append("content_type != 'vocabulary'")
        if q and q.strip():
            like = f"%{q.strip()}%"
            where.append("(title LIKE ? OR title_es LIKE ? OR description LIKE ? OR topic LIKE ? OR learning_objective_es LIKE ?)")
            params.extend([like, like, like, like, like])

        where_sql = " AND ".join(where)
        total = int(conn.execute(f"SELECT COUNT(*) FROM educational_resources WHERE {where_sql};", params).fetchone()[0])
        offset = (page - 1) * page_size
        order_clause = (
            "ORDER BY CASE WHEN content_type = 'vocabulary' THEN 1 ELSE 0 END, "
            "CASE cefr_level WHEN 'A1' THEN 1 WHEN 'A2' THEN 2 WHEN 'B1' THEN 3 WHEN 'B2' THEN 4 WHEN 'C1' THEN 5 ELSE 6 END, "
            "difficulty ASC, id ASC"
        )
        rows = conn.execute(
            f"SELECT * FROM educational_resources WHERE {where_sql} {order_clause} LIMIT ? OFFSET ?;",
            [*params, page_size, offset],
        ).fetchall()

        total_pages = max(1, (total + page_size - 1) // page_size)
        result = {
            "page": page,
            "page_size": page_size,
            "total": total,
            "total_pages": total_pages,
            "has_next": page < total_pages,
            "items": [_serialize_resource_row(r, include_payload=False) for r in rows],
        }
        edu_cache.set(cache_key, result, ttl_sec=60)
        return result
    finally:
        conn.close()


@router.get("/catalog")
async def get_catalog(
    request: Request,
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    level: str | None = None,
    category: str | None = None,
    skill: str | None = None,
    source_type: str | None = None,
    provider: str | None = None,
    offline_only: bool = False,
    include_vocab: bool = True,
    q: str | None = None,
):
    _check_edu_rate(request)
    return _query_catalog(
        page=page,
        page_size=page_size,
        level=level,
        category=category,
        skill=skill,
        source_type=source_type,
        provider=provider,
        offline_only=offline_only,
        include_vocab=include_vocab,
        q=q,
    )


@router.get("/search")
async def search_resources(
    request: Request,
    q: str = Query(min_length=1, max_length=120),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    level: str | None = None,
    category: str | None = None,
    skill: str | None = None,
):
    _check_edu_rate(request)
    return _query_catalog(
        page=page,
        page_size=page_size,
        level=level,
        category=category,
        skill=skill,
        q=q,
    )


@router.get("/levels")
async def list_levels(request: Request):
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        rows = conn.execute(
            """
            SELECT l.*, COALESCE(cnt.total, 0) AS resource_count
            FROM cefr_levels l
            LEFT JOIN (
                SELECT cefr_level, COUNT(*) AS total FROM educational_resources GROUP BY cefr_level
            ) cnt ON cnt.cefr_level = l.code
            ORDER BY l.rank ASC;
            """
        ).fetchall()
        return {"levels": [dict(r) for r in rows]}
    finally:
        conn.close()


@router.get("/levels/{cefr}")
async def get_by_level(
    cefr: str,
    request: Request,
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    category: str | None = None,
    skill: str | None = None,
):
    _check_edu_rate(request)
    return _query_catalog(page=page, page_size=page_size, level=cefr, category=category, skill=skill)


@router.get("/skills")
async def list_skills(request: Request):
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        rows = conn.execute(
            """
            SELECT primary_skill AS skill, COUNT(*) AS resource_count
            FROM educational_resources
            GROUP BY primary_skill
            ORDER BY resource_count DESC, primary_skill ASC;
            """
        ).fetchall()
        return {"skills": [dict(r) for r in rows]}
    finally:
        conn.close()


@router.get("/skills/{skill}")
async def get_by_skill(
    skill: str,
    request: Request,
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    level: str | None = None,
):
    _check_edu_rate(request)
    return _query_catalog(page=page, page_size=page_size, level=level, skill=skill)


@router.get("/categories")
async def list_categories(request: Request):
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        rows = conn.execute(
            """
            SELECT c.*, COALESCE(cnt.total, 0) AS resource_count
            FROM educational_categories c
            LEFT JOIN (
                SELECT category_id, COUNT(*) AS total FROM educational_resources GROUP BY category_id
            ) cnt ON cnt.category_id = c.id
            ORDER BY c.display_order ASC;
            """
        ).fetchall()
        return {"categories": [dict(r) for r in rows]}
    finally:
        conn.close()


@router.get("/resources/{resource_id}")
async def get_resource_detail(resource_id: str, request: Request):
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        r = conn.execute("SELECT * FROM educational_resources WHERE id = ?;", (resource_id,)).fetchone()
        if not r:
            raise HTTPException(404, "Recurso educativo no encontrado.")
        item = _serialize_resource_row(r, include_payload=True)
        lic = conn.execute("SELECT * FROM content_licenses WHERE id = ?;", (r["license_id"],)).fetchone()
        attr = (
            conn.execute("SELECT * FROM content_attributions WHERE id = ?;", (r["attribution_id"],)).fetchone()
            if r["attribution_id"]
            else None
        )
        prov = conn.execute("SELECT * FROM content_providers WHERE id = ?;", (r["provider_id"],)).fetchone()
        # Si hay quiz asociado en payload, incluir preguntas reales
        quiz_id = item["payload"].get("quiz_id") or (r["id"] if r["entity_table"] == "quizzes" else None)
        if quiz_id:
            q_rows = conn.execute(
                "SELECT * FROM quiz_questions WHERE quiz_id = ? ORDER BY order_index ASC;",
                (quiz_id,),
            ).fetchall()
            item["quiz_questions"] = [
                {
                    "id": qr["id"],
                    "kind": qr["kind"],
                    "prompt_es": qr["prompt_es"],
                    "audio_prompt_en": qr["audio_prompt_en"],
                    "options": json.loads(qr["options_json"] or "[]"),
                    "correct_index": int(qr["correct_index"]),
                    "expected_answer": qr["expected_answer"],
                    "explanation_es": qr["explanation_es"],
                }
                for qr in q_rows
            ]
        item["license"] = dict(lic) if lic else None
        item["attribution"] = dict(attr) if attr else None
        item["provider"] = dict(prov) if prov else None
        return item
    finally:
        conn.close()


@router.get("/providers")
async def list_providers(request: Request):
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        rows = conn.execute(
            """
            SELECT p.*, l.spdx_code, l.name AS license_name, l.storage_policy, l.url AS license_url,
                   COALESCE(cnt.total, 0) AS resource_count
            FROM content_providers p
            JOIN content_licenses l ON l.id = p.default_license_id
            LEFT JOIN (
                SELECT provider_id, COUNT(*) AS total FROM educational_resources GROUP BY provider_id
            ) cnt ON cnt.provider_id = p.id
            ORDER BY resource_count DESC, p.name ASC;
            """
        ).fetchall()
        return {"providers": [dict(r) for r in rows]}
    finally:
        conn.close()


@router.post("/recommendations")
async def post_recommendations(
    req: RecommendationRequest,
    request: Request,
    x_app_token: str | None = Header(default=None),
):
    _verify_app_token(x_app_token)
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        profile = LearnerProfileInput(
            user_id=req.user_id,
            cefr_level=req.cefr_level,
            listening_score=req.listening_score,
            grammar_score=req.grammar_score,
            vocabulary_score=req.vocabulary_score,
            speaking_score=req.speaking_score,
            pronunciation_score=req.pronunciation_score,
            weak_skills=req.weak_skills,
            error_tags=req.error_tags,
            completed_ids=set(req.completed_ids),
            mastered_ids=set(req.mastered_ids),
            known_vocab_count=req.known_vocab_count,
            weekly_practice_minutes=req.weekly_practice_minutes,
            offline_only=req.offline_only,
        )
        return generate_personalized_recommendations(conn, profile, limit=req.limit, save_path=True)
    finally:
        conn.close()


@router.get("/progress/{user_id}")
async def get_user_progress(
    user_id: str,
    request: Request,
    x_app_token: str | None = Header(default=None),
):
    _verify_app_token(x_app_token)
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        rows = conn.execute(
            """
            SELECT p.*, r.title, r.title_es, r.cefr_level, r.primary_skill, r.category_id, r.content_type
            FROM user_progress p
            JOIN educational_resources r ON r.id = p.resource_id
            WHERE p.user_id = ?
            ORDER BY p.updated_at DESC;
            """,
            (user_id,),
        ).fetchall()
        act_rows = conn.execute(
            "SELECT * FROM user_activity WHERE user_id = ? ORDER BY created_at DESC LIMIT 30;",
            (user_id,),
        ).fetchall()
        return {
            "user_id": user_id,
            "progress": [dict(r) for r in rows],
            "recent_activity": [dict(a) for a in act_rows],
        }
    finally:
        conn.close()


@router.post("/progress")
async def save_user_progress(
    req: ProgressUpsertRequest,
    request: Request,
    x_app_token: str | None = Header(default=None),
):
    _verify_app_token(x_app_token)
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        res = conn.execute("SELECT cefr_level, primary_skill FROM educational_resources WHERE id = ?;", (req.resource_id,)).fetchone()
        if not res:
            raise HTTPException(404, "Recurso no encontrado para registrar progreso.")
        now_ts = utc_now_iso()
        today = now_ts[:10]
        existing = conn.execute(
            "SELECT attempts, time_spent_sec FROM user_progress WHERE user_id = ? AND resource_id = ?;",
            (req.user_id, req.resource_id),
        ).fetchone()
        attempts = (int(existing["attempts"]) + 1) if existing else 1
        total_sec = (int(existing["time_spent_sec"]) + req.time_spent_sec) if existing else req.time_spent_sec
        completed_at = now_ts if req.status in {"completed", "mastered"} else None
        with conn:
            conn.execute(
                """
                INSERT OR REPLACE INTO user_progress(
                    user_id, resource_id, status, score, attempts, time_spent_sec,
                    last_position, error_tags_json, completed_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """,
                (
                    req.user_id, req.resource_id, req.status, req.score, attempts,
                    total_sec, req.last_position, json.dumps(req.error_tags, ensure_ascii=False),
                    completed_at, now_ts,
                ),
            )
            hist_action = "completed" if req.status in {"completed", "mastered"} else "practiced"
            conn.execute(
                """
                INSERT INTO content_history(user_id, resource_id, action, duration_sec, score, created_at)
                VALUES (?, ?, ?, ?, ?, ?);
                """,
                (req.user_id, req.resource_id, hist_action, req.time_spent_sec, req.score, now_ts),
            )
            conn.execute(
                """
                INSERT INTO user_activity(
                    user_id, activity_date, skill, cefr_level, minutes_practiced,
                    xp_earned, resources_completed, accuracy_pct, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """,
                (
                    req.user_id, today, res["primary_skill"], res["cefr_level"],
                    max(1, req.time_spent_sec // 60), req.xp_earned,
                    1 if req.status in {"completed", "mastered"} else 0, req.score, now_ts,
                ),
            )
        edu_cache.invalidate("stats")
        return {"ok": True, "user_id": req.user_id, "resource_id": req.resource_id, "status": req.status, "attempts": attempts}
    finally:
        conn.close()


@router.get("/favorites/{user_id}")
async def get_favorites(
    user_id: str,
    request: Request,
    x_app_token: str | None = Header(default=None),
):
    _verify_app_token(x_app_token)
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        rows = conn.execute(
            """
            SELECT r.*, f.created_at AS favorited_at
            FROM user_favorites f
            JOIN educational_resources r ON r.id = f.resource_id
            WHERE f.user_id = ?
            ORDER BY f.created_at DESC;
            """,
            (user_id,),
        ).fetchall()
        items = []
        for r in rows:
            item = _serialize_resource_row(r, include_payload=False)
            item["favorited_at"] = r["favorited_at"]
            items.append(item)
        return {"user_id": user_id, "total": len(items), "favorites": items}
    finally:
        conn.close()


@router.post("/favorites")
async def add_favorite(
    req: FavoriteRequest,
    request: Request,
    x_app_token: str | None = Header(default=None),
):
    _verify_app_token(x_app_token)
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        exists = conn.execute("SELECT 1 FROM educational_resources WHERE id = ?;", (req.resource_id,)).fetchone()
        if not exists:
            raise HTTPException(404, "Recurso no encontrado.")
        now_ts = utc_now_iso()
        with conn:
            conn.execute(
                "INSERT OR REPLACE INTO user_favorites(user_id, resource_id, created_at) VALUES (?, ?, ?);",
                (req.user_id, req.resource_id, now_ts),
            )
            conn.execute(
                "INSERT INTO content_history(user_id, resource_id, action, duration_sec, score, created_at) VALUES (?, ?, 'favorited', 0, NULL, ?);",
                (req.user_id, req.resource_id, now_ts),
            )
        edu_cache.invalidate("stats")
        return {"ok": True, "user_id": req.user_id, "resource_id": req.resource_id}
    finally:
        conn.close()


@router.delete("/favorites/{user_id}/{resource_id}")
async def delete_favorite(
    user_id: str,
    resource_id: str,
    request: Request,
    x_app_token: str | None = Header(default=None),
):
    _verify_app_token(x_app_token)
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        with conn:
            conn.execute("DELETE FROM user_favorites WHERE user_id = ? AND resource_id = ?;", (user_id, resource_id))
        edu_cache.invalidate("stats")
        return {"ok": True, "user_id": user_id, "resource_id": resource_id}
    finally:
        conn.close()


@router.get("/history/{user_id}")
async def get_history(
    user_id: str,
    request: Request,
    limit: int = Query(default=30, ge=1, le=100),
    x_app_token: str | None = Header(default=None),
):
    _verify_app_token(x_app_token)
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        rows = conn.execute(
            """
            SELECT h.id AS history_id, h.action, h.duration_sec, h.score, h.created_at AS visited_at,
                   r.id, r.title, r.title_es, r.cefr_level, r.category_id, r.primary_skill, r.content_type, r.source_type
            FROM content_history h
            JOIN educational_resources r ON r.id = h.resource_id
            WHERE h.user_id = ?
            ORDER BY h.created_at DESC, h.id DESC
            LIMIT ?;
            """,
            (user_id, limit),
        ).fetchall()
        return {"user_id": user_id, "history": [dict(r) for r in rows]}
    finally:
        conn.close()


@router.post("/history")
async def record_history(
    req: HistoryRequest,
    request: Request,
    x_app_token: str | None = Header(default=None),
):
    _verify_app_token(x_app_token)
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        exists = conn.execute("SELECT 1 FROM educational_resources WHERE id = ?;", (req.resource_id,)).fetchone()
        if not exists:
            raise HTTPException(404, "Recurso no encontrado.")
        now_ts = utc_now_iso()
        with conn:
            conn.execute(
                """
                INSERT INTO content_history(user_id, resource_id, action, duration_sec, score, created_at)
                VALUES (?, ?, ?, ?, ?, ?);
                """,
                (req.user_id, req.resource_id, req.action, req.duration_sec, req.score, now_ts),
            )
        edu_cache.invalidate("stats")
        return {"ok": True, "created_at": now_ts}
    finally:
        conn.close()


@router.get("/sync")
async def incremental_sync(
    request: Request,
    since: str | None = None,
    limit: int = Query(default=100, ge=1, le=500),
    offset: int = Query(default=0, ge=0),
):
    """Sincronización incremental para la app Android sin tener que reconstruir la APK."""
    _check_edu_rate(request)
    conn = get_connection(_db_path_override())
    try:
        server_time = utc_now_iso()
        if since and since.strip():
            total = int(
                conn.execute(
                    "SELECT COUNT(*) FROM educational_resources WHERE updated_at > ?;",
                    (since.strip(),),
                ).fetchone()[0]
            )
            rows = conn.execute(
                """
                SELECT * FROM educational_resources
                WHERE updated_at > ?
                ORDER BY updated_at ASC, id ASC
                LIMIT ? OFFSET ?;
                """,
                (since.strip(), limit, offset),
            ).fetchall()
        else:
            total = int(conn.execute("SELECT COUNT(*) FROM educational_resources;").fetchone()[0])
            rows = conn.execute(
                "SELECT * FROM educational_resources ORDER BY updated_at ASC, id ASC LIMIT ? OFFSET ?;",
                (limit, offset),
            ).fetchall()

        serialized = [_serialize_resource_row(r, include_payload=True) for r in rows]
        quiz_ids = [
            item["payload"].get("quiz_id") or (item["id"] if item.get("content_type") in {"quiz", "word_game"} else None)
            for item in serialized
        ]
        valid_quiz_ids = sorted({qid for qid in quiz_ids if isinstance(qid, str) and qid})
        quizzes_map: dict[str, list[dict[str, Any]]] = {}
        if valid_quiz_ids:
            placeholders = ",".join("?" for _ in valid_quiz_ids)
            for qr in conn.execute(
                f"SELECT * FROM quiz_questions WHERE quiz_id IN ({placeholders}) ORDER BY quiz_id ASC, order_index ASC;",
                valid_quiz_ids,
            ).fetchall():
                quizzes_map.setdefault(qr["quiz_id"], []).append(
                    {
                        "id": qr["id"],
                        "kind": qr["kind"],
                        "prompt_es": qr["prompt_es"],
                        "audio_prompt_en": qr["audio_prompt_en"],
                        "options": json.loads(qr["options_json"] or "[]"),
                        "correct_index": int(qr["correct_index"]),
                        "expected_answer": qr["expected_answer"],
                        "explanation_es": qr["explanation_es"],
                    }
                )
            for item in serialized:
                qid = item["payload"].get("quiz_id") or (item["id"] if item.get("content_type") in {"quiz", "word_game"} else None)
                if qid and qid in quizzes_map:
                    item["payload"]["questions"] = quizzes_map[qid]

        providers = [dict(r) for r in conn.execute("SELECT * FROM content_providers;").fetchall()]
        licenses = [dict(r) for r in conn.execute("SELECT * FROM content_licenses;").fetchall()]
        attributions = [dict(r) for r in conn.execute("SELECT * FROM content_attributions;").fetchall()]
        return {
            "server_time": server_time,
            "since": since,
            "total": total,
            "offset": offset,
            "limit": limit,
            "has_more": (offset + len(rows)) < total,
            "resources": serialized,
            "providers": providers,
            "licenses": licenses,
            "attributions": attributions,
        }
    finally:
        conn.close()


@router.get("/availability")
async def check_availability(
    request: Request,
    verify_live: bool = False,
    limit: int = Query(default=10, ge=1, le=25),
    x_app_token: str | None = Header(default=None),
):
    _check_edu_rate(request)
    if verify_live:
        _verify_app_token(x_app_token)
        checked = await check_links_availability(db_path=_db_path_override(), limit=limit)
        edu_cache.invalidate_all()
        return {"verified_live": True, "checked": checked}

    conn = get_connection(_db_path_override())
    try:
        summary = [
            dict(r)
            for r in conn.execute(
                """
                SELECT availability_status, COUNT(*) AS count
                FROM educational_resources
                GROUP BY availability_status;
                """
            ).fetchall()
        ]
        recent_runs = [
            dict(r)
            for r in conn.execute(
                "SELECT * FROM aggregation_runs ORDER BY id DESC LIMIT 10;"
            ).fetchall()
        ]
        return {"verified_live": False, "summary": summary, "recent_runs": recent_runs}
    finally:
        conn.close()


@router.post("/aggregate")
async def trigger_aggregation(
    req: AggregateRequest,
    request: Request,
    x_app_token: str | None = Header(default=None),
):
    _verify_app_token(x_app_token)
    _check_edu_rate(request)
    engine = ContentAggregationEngine()
    report = await engine.run_aggregation(
        db_path=_db_path_override(),
        provider_ids=req.provider_ids,
        limit_per_provider=req.limit_per_provider,
    )
    edu_cache.invalidate_all()
    return {
        "total_fetched": report.total_fetched,
        "total_imported": report.total_imported,
        "total_skipped_duplicates": report.total_skipped_duplicates,
        "total_rejected_license": report.total_rejected_license,
        "total_rejected_security": report.total_rejected_security,
        "total_errors": report.total_errors,
        "providers": report.provider_results,
    }
