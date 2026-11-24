"""
Paquete de base de datos educativa unificada FluentVoice (A1–C2).

Incluye esquema relacional de 24 entidades, migraciones versionadas con respaldo,
seguridad/licencias, clasificador CEFR, agregador de fuentes externas,
motor de recomendaciones personalizadas y endpoints FastAPI.
"""

from edu_db.migrations import DEFAULT_DB_PATH, create_backup, get_connection, init_db, run_migrations

__all__ = [
    "DEFAULT_DB_PATH",
    "create_backup",
    "get_connection",
    "init_db",
    "run_migrations",
]
