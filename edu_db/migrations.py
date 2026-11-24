"""
Gestor de conexiones SQLite, respaldos automáticos y migraciones versionadas no destructivas.
"""

from __future__ import annotations

import os
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

from edu_db.schema import DDL_V1_STATEMENTS, DDL_V2_STATEMENTS

ROOT_DIR = Path(__file__).resolve().parent.parent
DEFAULT_DB_PATH = Path(os.getenv("EDU_DB_PATH", str(ROOT_DIR / "data" / "fluentvoice_edu.db")))


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def get_connection(db_path: Path | str | None = None) -> sqlite3.Connection:
    target = Path(db_path) if db_path is not None else Path(os.getenv("EDU_DB_PATH", str(DEFAULT_DB_PATH)))
    target.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(target), check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode = WAL;")
    conn.execute("PRAGMA foreign_keys = ON;")
    conn.execute("PRAGMA busy_timeout = 5000;")
    return conn


def create_backup(db_path: Path | str, backup_dir: Path | str | None = None, tag: str = "auto") -> Path | None:
    """Crea un respaldo consistente usando SQLite online backup API antes de migrar."""
    src = Path(db_path)
    if not src.exists() or src.stat().st_size == 0:
        return None
    dest_dir = Path(backup_dir) if backup_dir else (src.parent / "backups")
    dest_dir.mkdir(parents=True, exist_ok=True)
    ts = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S_%f")
    dest = dest_dir / f"{src.stem}_{tag}_{ts}.db.bak"

    src_conn = sqlite3.connect(str(src))
    dst_conn = sqlite3.connect(str(dest))
    try:
        src_conn.backup(dst_conn)
    finally:
        dst_conn.close()
        src_conn.close()
    return dest


def _apply_v1(conn: sqlite3.Connection) -> None:
    for stmt in DDL_V1_STATEMENTS:
        conn.execute(stmt)


def _apply_v2(conn: sqlite3.Connection) -> None:
    for stmt in DDL_V2_STATEMENTS:
        conn.execute(stmt)


MIGRATIONS: list[tuple[int, str, Callable[[sqlite3.Connection], None]]] = [
    (1, "initial_24_entity_unified_schema", _apply_v1),
    (2, "diagnostic_questions_and_search_indexes", _apply_v2),
]


def get_current_schema_version(conn: sqlite3.Connection) -> int:
    row = conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name='schema_migrations';"
    ).fetchone()
    if not row:
        return 0
    ver = conn.execute("SELECT COALESCE(MAX(version), 0) AS v FROM schema_migrations;").fetchone()
    return int(ver["v"]) if ver else 0


def run_migrations(
    db_path: Path | str | None = None,
    target_version: int | None = None,
    backup_before_migrate: bool = True,
) -> dict:
    """Ejecuta migraciones pendientes en orden, creando respaldo previo si ya existe la BD."""
    target = Path(db_path) if db_path is not None else Path(os.getenv("EDU_DB_PATH", str(DEFAULT_DB_PATH)))
    existed_with_data = target.exists() and target.stat().st_size > 0
    backup_path: Path | None = None

    conn = get_connection(target)
    try:
        current_version = get_current_schema_version(conn)
        max_ver = target_version if target_version is not None else MIGRATIONS[-1][0]
        pending = [(v, name, fn) for (v, name, fn) in MIGRATIONS if current_version < v <= max_ver]

        if pending and existed_with_data and backup_before_migrate:
            conn.close()
            backup_path = create_backup(target, tag=f"pre_v{pending[0][0]}")
            conn = get_connection(target)

        applied: list[int] = []
        for version, name, fn in pending:
            with conn:
                fn(conn)
                conn.execute(
                    "INSERT OR REPLACE INTO schema_migrations(version, name, applied_at) VALUES (?, ?, ?);",
                    (version, name, utc_now_iso()),
                )
            applied.append(version)

        final_version = get_current_schema_version(conn)
        return {
            "db_path": str(target),
            "previous_version": current_version,
            "current_version": final_version,
            "applied_migrations": applied,
            "backup_path": str(backup_path) if backup_path else None,
        }
    finally:
        conn.close()


def init_db(db_path: Path | str | None = None) -> dict:
    """Inicializa y migra la base de datos hasta la última versión."""
    return run_migrations(db_path=db_path)
