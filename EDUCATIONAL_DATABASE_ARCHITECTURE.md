# FluentVoice — Arquitectura de Base de Datos Educativa (A1–C2)

## 1. Visión General
FluentVoice implementa una arquitectura híbrida **offline-first + sincronización incremental** basada en SQLite (`WAL`, integridad referencial con claves foráneas y migraciones versionadas con respaldo automático previo):

- **Backend Python / FastAPI (`edu_db/` + `main.py`)**: almacena el catálogo maestro y ejecuta el motor de incorporación externa en `data/fluentvoice_edu.db` (configurable mediante `EDU_DB_PATH`).
- **Cliente Android (`com.example.englishapp.edu`)**: precarga `assets/edu_catalog_bundle.json` en `SQLiteOpenHelper` (`fluentvoice_edu.db`) en el primer inicio y sincroniza deltas incrementales contra `/api/v1/edu/sync` **sin necesidad de reconstruir la APK**.

## 2. Esquema Unificado (24 Entidades + Catálogo Maestro)
1. `cefr_levels` (A1–C2)
2. `educational_categories` (12 categorías temáticas)
3. `learning_units` (24 unidades)
4. `lessons` (96 lecciones + 96 retos de voz)
5. `teaching_cards` (288 tarjetas)
6. `quizzes` (180 evaluaciones)
7. `quiz_questions` (492 reactivos)
8. `vocabulary` (468 entradas estructuradas: 288 base + 180 ampliación)
9. `phonetic_exercises` (48 ejercicios fonéticos)
10. `stories` (24 historias interactivas)
11. `reading_materials` (36 lecturas graduadas propias)
12. `listening_materials` (30 audios/dictados graduados)
13. `conversation_scenarios` (60 escenarios: 48 base + 12 especializados)
14. `writing_exercises` (24 talleres de escritura guiada)
15. `grammar_exercises` (60 ejercicios de gramática)
16. `external_resources` (recursos abiertos, APIs y enlaces externos)
17. `content_providers` (17 proveedores clasificados)
18. `content_licenses` (11 licencias con política explícita `store_full` / `store_metadata_only` / `link_only`)
19. `content_attributions` (registro legal de autoría y enlace de origen)
20. `learning_paths` (rutas adaptativas generadas para el estudiante)
21. `user_favorites`
22. `user_progress`
23. `user_activity`
24. `content_history`
- Tabla índice unificada: `educational_resources` + `diagnostic_questions` (36) + `aggregation_runs` + `schema_migrations`.

## 3. Respaldos y Migraciones Seguras
Antes de aplicar cualquier migración de esquema pendiente en `edu_db/migrations.py`, `create_backup()` ejecuta una copia consistente en línea de SQLite hacia `data/backups/`. Nunca se ejecutan migraciones destructivas (`DROP TABLE`). En Android, `EduDatabaseHelper.onUpgrade()` crea respaldo en `filesDir/edu_backups/` previo a actualizar índices o columnas.

## 4. Procedimiento de Mantenimiento
- **Re-sembrar de forma idempotente y verificar integridad**:
  ```bash
  .venv/Scripts/python.exe -m edu_db.export_android_bundle
  ```
- **Ejecutar todas las pruebas**:
  ```bash
  .venv/Scripts/pytest.exe -v
  ```
- **Verificar disponibilidad real de enlaces externos**:
  `GET /api/v1/edu/availability?verify_live=true&limit=15`

## 5. Cómo Añadir una Nueva Fuente Educativa Sin Romper el Currículo
1. **Registrar la licencia** (si es nueva) en `edu_db/security.py -> STANDARD_LICENSES` indicando `storage_policy` (`store_full`, `store_metadata_only` o `link_only`).
2. **Registrar el proveedor** en `edu_db/seed_internal.py -> PROVIDER_ROWS` con su `integration_mode` (`api`, `authorized_download`, `external_link` o `dataset`) y `rate_limit_rpm`.
3. **Crear adaptador** heredando de `BaseProviderAdapter` en `edu_db/providers.py`, definiendo `provider_id`, `allowed_domains` y `fetch_items()`, devolviendo instancias de `NormalizedExternalItem`.
4. **Registrar en `PROVIDER_ADAPTERS`**.
5. **Ejecutar agregación incremental**:
   - En caliente vía `POST /api/v1/edu/aggregate` (con header `X-App-Token`), el nuevo material pasa automáticamente por sanitización, verificación SSRF, validación de licencia, clasificación CEFR transparente, deduplicación SHA-256 e invalidación de caché.
   - Los dispositivos Android reciben las novedades automáticamente en `/api/v1/edu/sync?since=...` sin recompilar ni reinstalar la APK.
6. **Para importar un corpus TSV de Mozilla Common Voice (CC0)**:
   Configurar `COMMON_VOICE_TSV_PATH=/ruta/validated.tsv` en el entorno del servidor (nunca incrustar claves MDC dentro del APK) y ejecutar agregación sobre `"mozilla_common_voice"`.
