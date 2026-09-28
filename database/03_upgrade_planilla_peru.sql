-- Upgrade: planilla según normativa laboral peruana (RMV, AF, ONP/AFP, HE 25/35, 5ta).
-- Idempotente: seguro de ejecutar sobre una BD ya instalada con 01_install.sql.

DO $$ BEGIN
    CREATE TYPE regimen_pensionario AS ENUM ('ONP', 'AFP', 'NINGUNO');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

ALTER TABLE contrato
    ADD COLUMN IF NOT EXISTS regimen_pensionario regimen_pensionario NOT NULL DEFAULT 'ONP',
    ADD COLUMN IF NOT EXISTS afp_nombre VARCHAR(20),
    ADD COLUMN IF NOT EXISTS tiene_asignacion_familiar BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE contrato
SET regimen_pensionario = 'NINGUNO'
WHERE modalidad = 'PRACTICANTE' AND regimen_pensionario = 'ONP';

ALTER TABLE planilla_detalle
    ADD COLUMN IF NOT EXISTS asignacion_familiar NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS afp_aporte NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS afp_comision NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS afp_seguro NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quinta_categoria NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS regimen_pensionario VARCHAR(20),
    ADD COLUMN IF NOT EXISTS dias_computados NUMERIC(6, 2) NOT NULL DEFAULT 30;

INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('rmv', '1130', 'Remuneración Mínima Vital vigente (S/)'),
    ('uit', '5500', 'Unidad Impositiva Tributaria vigente (S/)'),
    ('tasa_asignacion_familiar', '0.10', 'Asignación familiar = tasa × RMV (Ley 25129)'),
    ('tasa_afp_aporte', '0.10', 'Aporte obligatorio al fondo AFP'),
    ('tasa_afp_seguro', '0.0137', 'Prima de seguro previsional AFP'),
    ('tasa_afp_comision_habitat', '0.0147', 'Comisión por flujo AFP Habitat'),
    ('tasa_afp_comision_integra', '0.0155', 'Comisión por flujo AFP Integra'),
    ('tasa_afp_comision_prima', '0.0160', 'Comisión por flujo AFP Prima'),
    ('tasa_afp_comision_profuturo', '0.0169', 'Comisión por flujo AFP Profuturo'),
    ('tasa_hora_extra_tramo1', '1.25', 'Multiplicador primeras 2 h extras (+25%)'),
    ('tasa_hora_extra_tramo2', '1.35', 'Multiplicador horas extras siguientes (+35%)'),
    ('horas_extra_tramo1', '2', 'Horas del primer tramo de sobretasa')
ON CONFLICT (clave) DO UPDATE
SET valor = EXCLUDED.valor,
    descripcion = EXCLUDED.descripcion;

UPDATE parametro_sistema
SET descripcion = 'Tasa ONP del colaborador (13%)'
WHERE clave = 'tasa_onp';

INSERT INTO cuenta_contable (codigo, nombre, uso, naturaleza) VALUES
    ('4033', 'AFP por pagar', 'AFP_POR_PAGAR', 'PASIVO'),
    ('4017', 'Renta 5ta categoría por pagar', 'QUINTA_POR_PAGAR', 'PASIVO')
ON CONFLICT (uso) DO NOTHING;
