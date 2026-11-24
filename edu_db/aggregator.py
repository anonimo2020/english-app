"""
Motor automático de incorporación e importación incremental (Fase 5).

Responsabilidades:
- Consultar adaptadores habilitados respetando límites por minuto.
- Normalizar metadatos y validar licencias con LicensePolicyEngine.
- Sanitizar texto externo y bloquear prompt injection / scripts.
- Validar URLs seguras HTTPS y detectar duplicados por SHA-256.
- Clasificar por CEFR/habilidad indicando nivel de confianza.
- Registrar atribuciones, historial de ejecuciones y actualización incremental.
- Nunca modificar ni sobrescribir el currículo propio (`source_type = 'owned'`).
"""

from __future__ import annotations

import json
import sqlite3
import time
from collections import defaultdict, deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import httpx

from edu_db.classifier import classify_resource
from edu_db.migrations import get_connection, utc_now_iso
from edu_db.providers import DEFAULT_USER_AGENT, PROVIDER_ADAPTERS, BaseProviderAdapter, NormalizedExternalItem
from edu_db.security import (
    STANDARD_LICENSES,
    LicensePolicyEngine,
    assert_external_cannot_mutate_core,
    compute_content_hash,
    has_prompt_injection,
    sanitize_external_text,
    validate_external_url,
)


@dataclass
class AggregationReport:
    provider_results: dict[str, dict[str, Any]] = field(default_factory=dict)
    total_fetched: int = 0
    total_imported: int = 0
    total_skipped_duplicates: int = 0
    total_rejected_license: int = 0
    total_rejected_security: int = 0
    total_errors: int = 0


class ContentAggregationEngine:
    """Orquesta la incorporación incremental de recursos educativos externos."""

    def __init__(self, adapters: dict[str, BaseProviderAdapter] | None = None) -> None:
        self.adapters = adapters if adapters is not None else PROVIDER_ADAPTERS
        self.policy = LicensePolicyEngine(STANDARD_LICENSES)
        self._provider_hits: dict[str, deque[float]] = defaultdict(deque)

    def _allow_request(self, provider_id: str, rate_limit_rpm: int) -> bool:
        now = time.time()
        q = self._provider_hits[provider_id]
        while q and now - q[0] > 60:
            q.popleft()
        if len(q) >= max(1, rate_limit_rpm):
            return False
        q.append(now)
        return True

    def ingest_items(
        self,
        conn: sqlite3.Connection,
        provider_id: str,
        items: list[NormalizedExternalItem],
        allowed_domains: tuple[str, ...] = (),
    ) -> dict[str, Any]:
        started_at = utc_now_iso()
        fetched = len(items)
        imported = 0
        skipped_dup = 0
        rejected_license = 0
        rejected_security = 0
        error_count = 0
        details: list[str] = []

        for raw in items:
            try:
                # 1. Seguridad: detectar inyección o scripts antes de procesar
                combined_check = f"{raw.title} {raw.description} {raw.body_en}"
                if has_prompt_injection(combined_check):
                    rejected_security += 1
                    details.append(f"Bloqueado por seguridad ({raw.external_id})")
                    continue

                clean_title = sanitize_external_text(raw.title, max_len=240, reject_injection=True)
                clean_title_es = sanitize_external_text(raw.title_es or clean_title, max_len=240, reject_injection=True)
                clean_desc = sanitize_external_text(raw.description, max_len=900, reject_injection=True)
                clean_body = sanitize_external_text(raw.body_en, max_len=6000, reject_injection=True)
                clean_author = sanitize_external_text(raw.author, max_len=140) or provider_id
                clean_attr = sanitize_external_text(raw.attribution_text, max_len=360)

                # 2. Validación de URL segura (HTTPS + anti-SSRF)
                url_ok, url_reason = validate_external_url(raw.original_url, allowed_domains=allowed_domains or None)
                if not url_ok:
                    rejected_security += 1
                    details.append(f"URL rechazada ({raw.external_id}): {url_reason}")
                    continue

                # 3. Validación estricta de licencia y política de almacenamiento
                decision = self.policy.evaluate(
                    license_id=raw.license_id,
                    requested_access_mode=raw.access_mode,
                    has_full_body_copy=raw.has_full_body_copy,
                    attribution_text=clean_attr,
                )
                if not decision.allowed or not decision.license_id:
                    rejected_license += 1
                    details.append(f"Licencia rechazada ({raw.external_id}): {decision.reason}")
                    continue

                # 4. Control de duplicados e importación incremental
                res_id = f"ext_{provider_id}_{raw.external_id}"
                existing_by_ext = conn.execute(
                    "SELECT id FROM external_resources WHERE provider_id = ? AND external_id = ?;",
                    (provider_id, raw.external_id),
                ).fetchone()
                if existing_by_ext:
                    skipped_dup += 1
                    continue

                content_hash = compute_content_hash(provider_id, raw.external_id, f"{clean_title}|{raw.original_url}")
                existing_by_hash = conn.execute(
                    "SELECT source_type FROM educational_resources WHERE content_hash = ?;",
                    (content_hash,),
                ).fetchone()
                if existing_by_hash:
                    skipped_dup += 1
                    continue

                existing_res = conn.execute(
                    "SELECT source_type FROM educational_resources WHERE id = ?;",
                    (res_id,),
                ).fetchone()
                if existing_res:
                    assert_external_cannot_mutate_core(raw.access_mode, existing_res["source_type"])
                    skipped_dup += 1
                    continue

                # 5. Clasificación inteligente (CEFR, habilidad, dificultad, confianza)
                cls = classify_resource(
                    title=clean_title,
                    text=f"{clean_desc} {clean_body}".strip(),
                    declared_cefr=raw.cefr_hint,
                    category_hint=raw.category_hint,
                    skill_hint=raw.skill_hint,
                    content_type=raw.content_type,
                    source_is_curated=False,
                )

                now_ts = utc_now_iso()
                attr_id = f"attr_{provider_id}_{raw.external_id}"
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
                    (attr_id, provider_id, decision.license_id, clean_author, clean_attr, raw.original_url, now_ts),
                )

                payload = dict(raw.payload)
                payload["attribution"] = clean_attr
                payload["license"] = decision.license_id
                if decision.can_store_full_body and clean_body:
                    payload["body_en"] = clean_body

                availability = "available" if raw.offline_compatible else "online_required"
                conn.execute(
                    """
                    INSERT INTO external_resources (
                        id, provider_id, external_id, cefr_level, category_id, primary_skill,
                        title, title_es, description, language, original_url, content_type,
                        access_mode, license_id, attribution_id, author, excerpt_or_payload_json,
                        cefr_confidence, cefr_classification_source, validation_status,
                        availability_status, offline_compatible, added_at, last_verified_at, content_hash
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'en', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                    """,
                    (
                        res_id, provider_id, raw.external_id, cls.cefr_level, cls.category_id,
                        cls.primary_skill, clean_title, clean_title_es, clean_desc,
                        raw.original_url, raw.content_type, raw.access_mode, decision.license_id,
                        attr_id, clean_author, json.dumps(payload, ensure_ascii=False),
                        cls.confidence, cls.classification_source, cls.validation_status,
                        availability, raw.offline_compatible, now_ts, now_ts, content_hash,
                    ),
                )

                conn.execute(
                    """
                    INSERT INTO educational_resources (
                        id, title, title_es, description, language, cefr_level, category_id,
                        primary_skill, secondary_skills_json, topic, difficulty, estimated_duration_min,
                        source_type, provider_id, author, original_url, content_type, license_id,
                        attribution_id, added_at, last_verified_at, availability_status, validation_status,
                        cefr_confidence, cefr_classification_source, learning_objective_es, exercise_type,
                        offline_compatible, content_hash, entity_table, entity_id, payload_json, updated_at
                    ) VALUES (?, ?, ?, ?, 'en', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'external_resources', ?, ?, ?);
                    """,
                    (
                        res_id, clean_title, clean_title_es, clean_desc, cls.cefr_level, cls.category_id,
                        cls.primary_skill, json.dumps(cls.secondary_skills, ensure_ascii=False),
                        cls.topic, cls.difficulty, raw.estimated_duration_min,
                        raw.access_mode, provider_id, clean_author, raw.original_url,
                        raw.content_type, decision.license_id, attr_id, now_ts, now_ts,
                        availability, cls.validation_status, cls.confidence, cls.classification_source,
                        cls.learning_objective_es, cls.exercise_type, raw.offline_compatible,
                        content_hash, res_id, json.dumps(payload, ensure_ascii=False), now_ts,
                    ),
                )
                imported += 1
            except PermissionError:
                rejected_security += 1
                details.append(f"Intento bloqueado de mutar contenido propio ({raw.external_id})")
            except Exception as exc:
                error_count += 1
                details.append(f"Error procesando {raw.external_id}: {exc}")

        finished_at = utc_now_iso()
        status = "ok" if error_count == 0 else "partial_error"
        conn.execute(
            """
            INSERT INTO aggregation_runs(
                provider_id, started_at, finished_at, fetched_count, imported_count,
                skipped_duplicates, rejected_license_count, error_count, status, details
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """,
            (
                provider_id, started_at, finished_at, fetched, imported,
                skipped_dup, rejected_license, error_count, status,
                " | ".join(details[:10]),
            ),
        )
        conn.execute(
            "UPDATE content_providers SET last_sync_at = ?, last_status = ? WHERE id = ?;",
            (finished_at, status, provider_id),
        )
        return {
            "provider_id": provider_id,
            "fetched": fetched,
            "imported": imported,
            "skipped_duplicates": skipped_dup,
            "rejected_license": rejected_license,
            "rejected_security": rejected_security,
            "errors": error_count,
            "status": status,
            "details": details[:10],
        }

    async def run_aggregation(
        self,
        db_path: Path | str | None = None,
        provider_ids: list[str] | None = None,
        limit_per_provider: int = 8,
        timeout_sec: float = 15.0,
    ) -> AggregationReport:
        report = AggregationReport()
        conn = get_connection(db_path=db_path)
        try:
            targets = provider_ids or list(self.adapters.keys())
            async with httpx.AsyncClient(timeout=timeout_sec, follow_redirects=True) as http:
                for pid in targets:
                    adapter = self.adapters.get(pid)
                    if not adapter:
                        continue
                    prov_row = conn.execute(
                        "SELECT rate_limit_rpm, is_enabled FROM content_providers WHERE id = ?;",
                        (pid,),
                    ).fetchone()
                    if prov_row and not prov_row["is_enabled"]:
                        report.provider_results[pid] = {"provider_id": pid, "status": "disabled", "imported": 0}
                        continue
                    rpm = int(prov_row["rate_limit_rpm"]) if prov_row else 20
                    if not self._allow_request(pid, rpm):
                        report.provider_results[pid] = {"provider_id": pid, "status": "rate_limited", "imported": 0}
                        continue

                    try:
                        fetched_items = await adapter.fetch_items(http, limit=limit_per_provider)
                        with conn:
                            stats = self.ingest_items(conn, pid, fetched_items, allowed_domains=adapter.allowed_domains)
                    except Exception as exc:
                        now_ts = utc_now_iso()
                        err_msg = f"{type(exc).__name__}: {exc}" if str(exc) else type(exc).__name__
                        with conn:
                            conn.execute(
                                """
                                INSERT INTO aggregation_runs(
                                    provider_id, started_at, finished_at, fetched_count, imported_count,
                                    skipped_duplicates, rejected_license_count, error_count, status, details
                                ) VALUES (?, ?, ?, 0, 0, 0, 0, 1, 'network_error', ?);
                                """,
                                (pid, now_ts, now_ts, err_msg[:300]),
                            )
                            conn.execute(
                                "UPDATE content_providers SET last_sync_at = ?, last_status = 'network_error' WHERE id = ?;",
                                (now_ts, pid),
                            )
                        stats = {
                            "provider_id": pid,
                            "fetched": 0,
                            "imported": 0,
                            "skipped_duplicates": 0,
                            "rejected_license": 0,
                            "rejected_security": 0,
                            "errors": 1,
                            "status": "network_error",
                            "details": [err_msg[:300]],
                        }

                    report.provider_results[pid] = stats
                    report.total_fetched += stats.get("fetched", 0)
                    report.total_imported += stats.get("imported", 0)
                    report.total_skipped_duplicates += stats.get("skipped_duplicates", 0)
                    report.total_rejected_license += stats.get("rejected_license", 0)
                    report.total_rejected_security += stats.get("rejected_security", 0)
                    report.total_errors += stats.get("errors", 0)
            return report
        finally:
            conn.close()


async def check_links_availability(
    db_path: Path | str | None = None,
    limit: int = 12,
    timeout_sec: float = 8.0,
) -> list[dict[str, Any]]:
    """Verifica disponibilidad HTTP real de recursos externos enlazados sin modificar contenido propio."""
    conn = get_connection(db_path=db_path)
    results: list[dict[str, Any]] = []
    try:
        rows = conn.execute(
            """
            SELECT id, provider_id, original_url, availability_status, last_verified_at
            FROM educational_resources
            WHERE original_url != ''
            ORDER BY last_verified_at ASC
            LIMIT ?;
            """,
            (limit,),
        ).fetchall()
        now_ts = utc_now_iso()
        async with httpx.AsyncClient(timeout=timeout_sec, follow_redirects=True) as http:
            for r in rows:
                url = r["original_url"]
                ok_url, _ = validate_external_url(url)
                new_status = "unavailable"
                code = 0
                if ok_url:
                    try:
                        resp = await http.head(url, headers={"User-Agent": DEFAULT_USER_AGENT})
                        if resp.status_code == 405:
                            resp = await http.get(url, headers={"User-Agent": DEFAULT_USER_AGENT})
                        code = resp.status_code
                        new_status = "online_required" if 200 <= code < 400 else "unavailable"
                    except Exception:
                        new_status = "unavailable"
                with conn:
                    conn.execute(
                        "UPDATE educational_resources SET availability_status = ?, last_verified_at = ? WHERE id = ?;",
                        (new_status, now_ts, r["id"]),
                    )
                    conn.execute(
                        "UPDATE external_resources SET availability_status = ?, last_verified_at = ? WHERE id = ?;",
                        (new_status, now_ts, r["id"]),
                    )
                results.append(
                    {
                        "id": r["id"],
                        "provider_id": r["provider_id"],
                        "url": url,
                        "http_status": code,
                        "availability_status": new_status,
                        "verified_at": now_ts,
                    }
                )
        return results
    finally:
        conn.close()
