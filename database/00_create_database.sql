-- =============================================================================
-- Crear la base de datos — ejecutar en pgAdmin
-- =============================================================================
-- Install limpio:
-- 1. Si existe rrhh_andina: clic derecho → Delete/Drop.
-- 2. Query Tool sobre "postgres" (NO sobre rrhh_andina). Auto commit activo.
-- 3. Ejecuta este archivo (F5).
-- 4. Databases > Refresh → Query Tool sobre rrhh_andina → 01_install.sql.
-- Guía: database/README.md
-- =============================================================================

CREATE DATABASE rrhh_andina
    WITH
    ENCODING = 'UTF8'
    TEMPLATE = template0;
