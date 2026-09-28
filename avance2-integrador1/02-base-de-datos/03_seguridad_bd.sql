-- =============================================================================
-- rrhh_andina — Endurecimiento de seguridad (propuesta Avance 2)
-- Ejecutar en pgAdmin, Query Tool sobre rrhh_andina, DESPUÉS de 01_install.sql,
-- con un usuario superusuario (postgres).
--
-- Objetivo: que la API no se conecte como "postgres". Se crean tres roles:
--   rrhh_owner     dueño del esquema (DDL). Sin LOGIN.
--   rrhh_app       usuario de la API Spring Boot: solo DML, sin DDL.
--   rrhh_reportes  solo lectura sobre las vistas de reporte (BI / Excel).
--
-- Antes de usar en un entorno real, reemplazar las contraseñas de ejemplo.
-- Luego cambiar backend/.env:
--   DB_USERNAME=rrhh_app
--   DB_PASSWORD=<la contraseña definida abajo>
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Roles
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rrhh_owner') THEN
        CREATE ROLE rrhh_owner NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rrhh_app') THEN
        CREATE ROLE rrhh_app LOGIN PASSWORD 'CambiarEstaClave_App_2026!' CONNECTION LIMIT 30;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rrhh_reportes') THEN
        CREATE ROLE rrhh_reportes LOGIN PASSWORD 'CambiarEstaClave_Rep_2026!' CONNECTION LIMIT 5;
    END IF;
END $$;

-- Una consulta colgada no debe bloquear la API ni la base.
ALTER ROLE rrhh_app SET statement_timeout = '30s';
ALTER ROLE rrhh_app SET idle_in_transaction_session_timeout = '60s';
ALTER ROLE rrhh_reportes SET statement_timeout = '120s';
ALTER ROLE rrhh_reportes SET default_transaction_read_only = on;

-- -----------------------------------------------------------------------------
-- 2. Base y esquema: nadie crea objetos salvo el dueño
-- -----------------------------------------------------------------------------
REVOKE ALL ON DATABASE rrhh_andina FROM PUBLIC;
GRANT CONNECT ON DATABASE rrhh_andina TO rrhh_app, rrhh_reportes;

REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO rrhh_owner;
GRANT USAGE ON SCHEMA public TO rrhh_app, rrhh_reportes;

-- -----------------------------------------------------------------------------
-- 3. Transferir la propiedad de tablas, vistas, secuencias y funciones
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
        EXECUTE format('ALTER TABLE public.%I OWNER TO rrhh_owner', r.tablename);
    END LOOP;
    FOR r IN SELECT viewname FROM pg_views WHERE schemaname = 'public' LOOP
        EXECUTE format('ALTER VIEW public.%I OWNER TO rrhh_owner', r.viewname);
    END LOOP;
    FOR r IN SELECT p.oid::regprocedure AS firma
             FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
             WHERE n.nspname = 'public' LOOP
        EXECUTE format('ALTER FUNCTION %s OWNER TO rrhh_owner', r.firma);
    END LOOP;
    FOR r IN SELECT typname FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
             WHERE n.nspname = 'public' AND t.typtype = 'e' LOOP
        EXECUTE format('ALTER TYPE public.%I OWNER TO rrhh_owner', r.typname);
    END LOOP;
END $$;

-- -----------------------------------------------------------------------------
-- 4. Permisos de la API (solo DML)
-- -----------------------------------------------------------------------------
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO rrhh_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO rrhh_app;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO rrhh_app;

-- Bitácoras de solo inserción: la API puede escribir y leer, pero nunca
-- modificar ni borrar la evidencia de auditoría.
REVOKE UPDATE, DELETE ON auditoria FROM rrhh_app;
REVOKE UPDATE, DELETE ON historial_solicitud FROM rrhh_app;
REVOKE UPDATE, DELETE ON reporte_generado FROM rrhh_app;

-- Objetos creados en el futuro por rrhh_owner heredan los mismos permisos.
ALTER DEFAULT PRIVILEGES FOR ROLE rrhh_owner IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rrhh_app;
ALTER DEFAULT PRIVILEGES FOR ROLE rrhh_owner IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO rrhh_app;
ALTER DEFAULT PRIVILEGES FOR ROLE rrhh_owner IN SCHEMA public
    GRANT EXECUTE ON FUNCTIONS TO rrhh_app;

-- -----------------------------------------------------------------------------
-- 5. Permisos de reportes (solo vistas, nunca tablas con datos sensibles)
-- usuario.password_hash y refresh_token.token no son visibles para este rol.
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT viewname FROM pg_views
             WHERE schemaname = 'public'
               AND (viewname LIKE 'v\_reporte\_%' OR viewname = 'v_trazabilidad_solicitudes') LOOP
        EXECUTE format('GRANT SELECT ON public.%I TO rrhh_reportes', r.viewname);
    END LOOP;
END $$;

-- -----------------------------------------------------------------------------
-- 6. Verificación rápida
-- -----------------------------------------------------------------------------
SELECT grantee, table_name, string_agg(privilege_type, ', ' ORDER BY privilege_type) AS privilegios
FROM information_schema.role_table_grants
WHERE grantee IN ('rrhh_app', 'rrhh_reportes')
  AND table_name IN ('usuario', 'auditoria', 'historial_solicitud', 'v_reporte_permisos')
GROUP BY grantee, table_name
ORDER BY grantee, table_name;

-- -----------------------------------------------------------------------------
-- Recomendaciones fuera de SQL (servidor)
-- - pg_hba.conf: método scram-sha-256 y solo 127.0.0.1/::1 o la IP del backend.
-- - postgresql.conf: ssl = on si la API y la base están en equipos distintos.
-- - Respaldo diario con pg_dump -Fc y prueba de restauración mensual.
-- - backend/.env fuera del control de versiones (ya está en .gitignore).
-- -----------------------------------------------------------------------------
