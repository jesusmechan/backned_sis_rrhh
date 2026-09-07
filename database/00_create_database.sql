-- =============================================================================
-- Crear la base de datos — ejecutar en pgAdmin
-- =============================================================================
-- 1. Query Tool sobre la base "postgres" (NO sobre rrhh_andina).
-- 2. En la barra del Query Tool, deja activado Auto commit.
-- 3. Ejecuta solo este archivo (F5).
-- 4. Databases > Refresh. Luego Query Tool sobre rrhh_andina y ejecuta 01_install.sql.
--
-- Si rrhh_andina ya existe: no la borres aquí. En el árbol de la izquierda,
-- clic derecho en rrhh_andina > Delete/Drop y vuelve a ejecutar este script.
-- =============================================================================

CREATE DATABASE rrhh_andina
    WITH
    ENCODING = 'UTF8'
    TEMPLATE = template0;
