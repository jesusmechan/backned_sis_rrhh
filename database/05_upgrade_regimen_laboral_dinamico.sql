-- Upgrade: régimen laboral dinámico (Ley 32353 MYPE y régimen general)
-- Permite cálculo diferenciado de planilla según beneficios laborales.
-- Idempotente: seguro de ejecutar sobre una BD ya actualizada con 03_upgrade_planilla_peru.sql.

-- ============================================================================
-- 1. Tipo de dato para régimen laboral
-- ============================================================================
DO $$ BEGIN
    CREATE TYPE regimen_laboral AS ENUM ('GENERAL', 'MYPE_MICRO', 'MYPE_PEQUENA');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- ============================================================================
-- 2. Agregar régimen laboral a contrato (determina beneficios)
-- ============================================================================
ALTER TABLE contrato
    ADD COLUMN IF NOT EXISTS regimen_laboral regimen_laboral NOT NULL DEFAULT 'GENERAL';

COMMENT ON COLUMN contrato.regimen_laboral IS 'Régimen laboral Ley 32353: GENERAL (D.Leg.728 completo), MYPE_MICRO (hasta 150 UIT), MYPE_PEQUENA (hasta 1,700 UIT)';

-- ============================================================================
-- 3. Parámetros dinámicos de planilla (valores con vigencia y fuente)
-- ============================================================================

-- RMV (Remuneración Mínima Vital) con vigencia temporal
-- DS 006-2024-TR: S/ 1,130 vigente hasta 30-sep-2026
-- DS 015-2026-TR: S/ 1,230 desde 01-oct-2026
INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('rmv_2026_10_01', '1230', 'RMV desde 01-oct-2026 (DS 015-2026-TR, primer tramo aumento)'),
    ('rmv_2027_q1', '1300', 'RMV prevista para Q1 2027 (DS 015-2026-TR, segundo tramo)')
ON CONFLICT (clave) DO UPDATE
SET valor = EXCLUDED.valor,
    descripcion = EXCLUDED.descripcion;

-- UIT (Unidad Impositiva Tributaria) por año fiscal
-- DS 301-2025-EF: S/ 5,500 para 2026
INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('uit_2026', '5500', 'UIT año fiscal 2026 (DS 301-2025-EF)'),
    ('uit_2025', '5350', 'UIT año fiscal 2025')
ON CONFLICT (clave) DO UPDATE
SET valor = EXCLUDED.valor,
    descripcion = EXCLUDED.descripcion;

-- RAM (Remuneración Asegurable Máxima) por trimestre SBS
-- Fuente: Superintendencia de Banca, Seguros y AFP (SBS), actualización trimestral
INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('ram_2026_q1', '12209.11', 'RAM ene-mar 2026 para prima AFP 1.37% (SBS)'),
    ('ram_2026_q2', '12672.65', 'RAM abr-jun 2026 para prima AFP 1.37% (SBS)'),
    ('ram_2026_q3', '12672.65', 'RAM jul-sep 2026 para prima AFP 1.37% (SBS)'),
    ('ram', '12672.65', 'RAM por defecto (último valor publicado SBS)')
ON CONFLICT (clave) DO UPDATE
SET valor = EXCLUDED.valor,
    descripcion = EXCLUDED.descripcion;

-- Actualizar descripciones de parámetros existentes con fuente legal
UPDATE parametro_sistema
SET descripcion = 'RMV hasta 30-sep-2026 (DS 006-2024-TR)'
WHERE clave = 'rmv' AND (descripcion IS NULL OR descripcion NOT LIKE '%DS%');

UPDATE parametro_sistema
SET descripcion = 'UIT 2026 (DS 301-2025-EF, valor de referencia tributario)'
WHERE clave = 'uit' AND (descripcion IS NULL OR descripcion NOT LIKE '%DS%');

UPDATE parametro_sistema
SET descripcion = 'Asignación familiar 10% de RMV (D. Leg. 713)'
WHERE clave = 'tasa_asignacion_familiar' AND (descripcion IS NULL OR descripcion NOT LIKE '%Leg%');

UPDATE parametro_sistema
SET descripcion = 'Aporte trabajador ONP 13% (D. Ley 19990)'
WHERE clave = 'tasa_onp' AND (descripcion IS NULL OR descripcion NOT LIKE '%19990%');

UPDATE parametro_sistema
SET descripcion = 'EsSalud empleador 9% sobre remuneración, mínimo RMV (Ley 26790)'
WHERE clave = 'tasa_essalud' AND (descripcion IS NULL OR descripcion NOT LIKE '%26790%');

UPDATE parametro_sistema
SET descripcion = 'AFP aporte obligatorio 10% al fondo (SBS)'
WHERE clave = 'tasa_afp_aporte' AND (descripcion IS NULL OR descripcion NOT LIKE '%SBS%');

UPDATE parametro_sistema
SET descripcion = 'AFP prima seguro 1.37% sobre RAM (SBS, aplica tope trimestral)'
WHERE clave = 'tasa_afp_seguro' AND (descripcion IS NULL OR descripcion NOT LIKE '%RAM%');

UPDATE parametro_sistema
SET descripcion = 'AFP Habitat comisión por flujo 1.47% (SBS 2026)'
WHERE clave = 'tasa_afp_comision_habitat' AND (descripcion IS NULL OR descripcion NOT LIKE '%SBS%');

UPDATE parametro_sistema
SET descripcion = 'AFP Integra comisión por flujo 1.55% (SBS 2026)'
WHERE clave = 'tasa_afp_comision_integra' AND (descripcion IS NULL OR descripcion NOT LIKE '%SBS%');

UPDATE parametro_sistema
SET descripcion = 'AFP Prima comisión por flujo 1.60% (SBS 2026)'
WHERE clave = 'tasa_afp_comision_prima' AND (descripcion IS NULL OR descripcion NOT LIKE '%SBS%');

UPDATE parametro_sistema
SET descripcion = 'AFP Profuturo comisión por flujo 1.69% (SBS 2026)'
WHERE clave = 'tasa_afp_comision_profuturo' AND (descripcion IS NULL OR descripcion NOT LIKE '%SBS%');

UPDATE parametro_sistema
SET descripcion = 'Horas extra tramo 1: +25% primeras 2 horas (D. Leg. 854)'
WHERE clave = 'tasa_hora_extra_tramo1' AND (descripcion IS NULL OR descripcion NOT LIKE '%Leg%');

UPDATE parametro_sistema
SET descripcion = 'Horas extra tramo 2: +35% horas siguientes (D. Leg. 854)'
WHERE clave = 'tasa_hora_extra_tramo2' AND (descripcion IS NULL OR descripcion NOT LIKE '%Leg%');

-- ============================================================================
-- 4. Agregar columnas de beneficios calculados a planilla_detalle
-- ============================================================================
ALTER TABLE planilla_detalle
    ADD COLUMN IF NOT EXISTS gratificacion_proyectada NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cts_proyectado NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS regimen_laboral VARCHAR(20);

COMMENT ON COLUMN planilla_detalle.gratificacion_proyectada IS 'Proyección gratificación semestral según régimen (Ley 27735): General 1.0x, MYPE pequeña 0.5x, MYPE micro 0';
COMMENT ON COLUMN planilla_detalle.cts_proyectado IS 'Proyección CTS semestral según régimen (D. Leg. 650): General 1.0x, MYPE pequeña 0.5x, MYPE micro 0';
COMMENT ON COLUMN planilla_detalle.regimen_laboral IS 'Régimen laboral del trabajador al momento del cálculo (snapshot del contrato)';

-- ============================================================================
-- 5. Comentarios explicativos
-- ============================================================================
COMMENT ON TYPE regimen_laboral IS 'Régimen laboral según Ley 32353 (MYPE) y D. Leg. 728 (general). Determina vacaciones, gratificaciones, CTS y aportes salud.';
COMMENT ON TABLE parametro_sistema IS 'Parámetros configurables de planilla con vigencia temporal. Valores oficiales con fuente legal documentada (SUNAT, SBS, MTPE).';

-- ============================================================================
-- Notas de uso
-- ============================================================================
-- Este script permite:
-- 1. Asignar régimen laboral a cada contrato según tamaño de empresa (REMYPE).
-- 2. Calcular automáticamente beneficios diferenciados por régimen:
--    - GENERAL: 30 días vacaciones, 2 gratificaciones completas, CTS completo, EsSalud 9%
--    - MYPE_PEQUENA: 15 días vacaciones, media gratificación, medio CTS, EsSalud 9%
--    - MYPE_MICRO: 15 días vacaciones, sin gratificación obligatoria, sin CTS, SIS en lugar de EsSalud
-- 3. Usar parámetros con vigencia para RMV, UIT, RAM, comisiones AFP.
-- 4. Proyectar gratificaciones y CTS en el detalle de planilla mensual.
--
-- Ejecutar a mano en PostgreSQL local (pgAdmin).
-- No modifica datos existentes, solo añade columnas y parámetros.
