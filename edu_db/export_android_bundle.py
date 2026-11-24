"""
Exportador del paquete compacto oficial `edu_catalog_bundle.json` para Android.

Mantiene una sola fuente de verdad: siembra la BD unificada en Python, ejecuta
(opcionalmente) agregación en vivo desde proveedores abiertos y genera el asset
JSON que Android carga al iniciar su base de datos SQLite local offline-first.
"""

from __future__ import annotations

import asyncio
import json
from pathlib import Path
from typing import Any

from edu_db.aggregator import ContentAggregationEngine
from edu_db.migrations import ROOT_DIR, get_connection, utc_now_iso
from edu_db.seed_internal import seed_all_internal_and_baseline

ANDROID_BUNDLE_PATH = ROOT_DIR / "android-app" / "app" / "src" / "main" / "assets" / "edu_catalog_bundle.json"


def export_android_bundle(
    db_path: Path | str | None = None,
    output_path: Path | str = ANDROID_BUNDLE_PATH,
) -> dict[str, Any]:
    conn = get_connection(db_path=db_path)
    try:
        levels = [dict(r) for r in conn.execute("SELECT * FROM cefr_levels ORDER BY rank ASC;").fetchall()]
        categories = [dict(r) for r in conn.execute("SELECT * FROM educational_categories ORDER BY display_order ASC;").fetchall()]
        licenses = [dict(r) for r in conn.execute("SELECT * FROM content_licenses ORDER BY id ASC;").fetchall()]
        providers = [dict(r) for r in conn.execute("SELECT * FROM content_providers ORDER BY id ASC;").fetchall()]
        attributions = [dict(r) for r in conn.execute("SELECT * FROM content_attributions ORDER BY id ASC;").fetchall()]

        # Exportamos todos los recursos educativos relevantes para Learning Hub:
        # Las lecturas, listening, gramática, escritura, pronunciación, historias,
        # exámenes, word games, unidades/lecciones, escenarios, vocabulario ampliado
        # (y todo recurso externo/abierto agregado) en un paquete compacto y fiel.
        res_rows = conn.execute(
            """
            SELECT * FROM educational_resources
            ORDER BY CASE cefr_level WHEN 'A1' THEN 1 WHEN 'A2' THEN 2 WHEN 'B1' THEN 3 WHEN 'B2' THEN 4 WHEN 'C1' THEN 5 ELSE 6 END,
                     id ASC;
            """
        ).fetchall()

        quizzes_map: dict[str, list[dict[str, Any]]] = {}
        for qr in conn.execute("SELECT * FROM quiz_questions ORDER BY quiz_id ASC, order_index ASC;").fetchall():
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

        resources: list[dict[str, Any]] = []
        for r in res_rows:
            payload = json.loads(r["payload_json"] or "{}")
            qid = payload.get("quiz_id") or (r["id"] if r["entity_table"] == "quizzes" else None)
            if qid and qid in quizzes_map:
                payload["questions"] = quizzes_map[qid]
            resources.append(
                {
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
                    "entity_table": r["entity_table"],
                    "entity_id": r["entity_id"],
                    "updated_at": r["updated_at"],
                    "payload_json": json.dumps(payload, ensure_ascii=False),
                }
            )

        bundle = {
            "schema_version": 2,
            "generated_at": utc_now_iso(),
            "levels": levels,
            "categories": categories,
            "licenses": licenses,
            "providers": providers,
            "attributions": attributions,
            "resources": resources,
        }
        dest = Path(output_path)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(json.dumps(bundle, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        return {
            "output_path": str(dest),
            "bytes": dest.stat().st_size,
            "resources_count": len(resources),
            "providers_count": len(providers),
            "licenses_count": len(licenses),
            "attributions_count": len(attributions),
        }
    finally:
        conn.close()


async def build_full_database_and_export(run_live_aggregation: bool = True) -> dict[str, Any]:
    seed_info = seed_all_internal_and_baseline()
    agg_summary = None
    if run_live_aggregation:
        engine = ContentAggregationEngine()
        report = await engine.run_aggregation(
            provider_ids=[
                "tatoeba",
                "gutenberg_gutendex",
                "wikimedia_commons",
                "simple_wikipedia",
                "librivox",
                "datamuse",
                "free_dictionary",
            ],
            limit_per_provider=6,
            timeout_sec=15.0,
        )
        agg_summary = {
            "total_fetched": report.total_fetched,
            "total_imported": report.total_imported,
            "total_skipped_duplicates": report.total_skipped_duplicates,
            "total_rejected_license": report.total_rejected_license,
            "total_rejected_security": report.total_rejected_security,
            "total_errors": report.total_errors,
            "providers": report.provider_results,
        }
    bundle_info = export_android_bundle()
    return {
        "seed": seed_info,
        "live_aggregation": agg_summary,
        "bundle": bundle_info,
    }


if __name__ == "__main__":
    result = asyncio.run(build_full_database_and_export(run_live_aggregation=True))
    print(json.dumps(result, indent=2, ensure_ascii=False))
