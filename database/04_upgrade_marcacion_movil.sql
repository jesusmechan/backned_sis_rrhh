-- Upgrade: la app móvil registra marcaciones con origen MOVIL.
-- Idempotente: seguro de ejecutar sobre una BD ya instalada con 01_install.sql.

ALTER TABLE marcacion DROP CONSTRAINT IF EXISTS ck_marcacion_origen;
ALTER TABLE marcacion ADD CONSTRAINT ck_marcacion_origen
    CHECK (origen IN ('WEB', 'MOVIL', 'MANUAL', 'CORRECCION'));
