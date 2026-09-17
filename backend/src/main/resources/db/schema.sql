-- Parche idempotente para bases ya instaladas.
-- El esquema (CREATE TABLE) está solo en database/01_install.sql.

INSERT INTO horario_laboral (nombre, hora_ingreso, hora_salida, minutos_refrigerio)
SELECT 'Jornada de prácticas', TIME '09:00', TIME '14:00', 0
WHERE NOT EXISTS (SELECT 1 FROM horario_laboral WHERE nombre = 'Jornada de prácticas');

INSERT INTO parametro_sistema (clave, valor, descripcion)
VALUES
    ('dias_vacaciones_mensuales', '1.5', 'Días de vacaciones que acumula un trabajador por cada mes completo, sea colaborador o practicante'),
    ('tasa_hora_extra', '1.25', 'Multiplicador de la hora extra sobre el valor hora (sueldo / 240)'),
    ('tasa_essalud', '0.09', 'Aporte de EsSalud a cargo del empleador'),
    ('tasa_onp', '0.13', 'Descuento ONP del colaborador (modelo simplificado)'),
    ('horas_mensuales_base', '240', 'Horas mensuales para valorizar la hora ordinaria')
ON CONFLICT (clave) DO NOTHING;

ALTER TABLE contrato
    ADD COLUMN IF NOT EXISTS remuneracion_basica NUMERIC(12, 2) NOT NULL DEFAULT 0;

UPDATE contrato SET remuneracion_basica = 4200 WHERE codigo = 'CTR-001' AND remuneracion_basica = 0;
UPDATE contrato SET remuneracion_basica = 3800 WHERE codigo = 'CTR-002' AND remuneracion_basica = 0;
UPDATE contrato SET remuneracion_basica = 3200 WHERE codigo = 'CTR-003' AND remuneracion_basica = 0;

INSERT INTO cuenta_contable (codigo, nombre, uso, naturaleza)
SELECT v.codigo, v.nombre, v.uso, v.naturaleza
FROM (VALUES
    ('6211', 'Sueldos y salarios', 'SUELDOS', 'GASTO'),
    ('6271', 'EsSalud', 'ESSALUD_GASTO', 'GASTO'),
    ('4031', 'ONP por pagar', 'ONP_POR_PAGAR', 'PASIVO'),
    ('4032', 'EsSalud por pagar', 'ESSALUD_POR_PAGAR', 'PASIVO'),
    ('4111', 'Remuneraciones por pagar', 'REMU_POR_PAGAR', 'PASIVO'),
    ('4699', 'Descuentos por ausencias', 'DESC_AUSENCIAS', 'PASIVO')
) AS v(codigo, nombre, uso, naturaleza)
WHERE NOT EXISTS (SELECT 1 FROM cuenta_contable c WHERE c.uso = v.uso);

INSERT INTO menu_item (codigo, etiqueta, ruta, icono, grupo, descripcion, orden)
SELECT v.codigo, v.etiqueta, v.ruta, v.icono, v.grupo, v.descripcion, v.orden
FROM (VALUES
    ('INICIO', 'Inicio', '/', 'Home', 'Operación', 'Resumen de la sesión y accesos del perfil.', 10),
    ('BANDEJA', 'Bandeja', '/bandeja', 'Inbox', 'Operación', 'Atender y seguir solicitudes del circuito.', 20),
    ('PERMISOS', 'Permisos', '/permisos', 'ClipboardCheck', 'Operación', 'Registrar y seguir solicitudes de permiso.', 30),
    ('HORAS_EXTRAS', 'Horas extras', '/horas-extras', 'Clock3', 'Operación', 'Registrar y consultar tiempo extra.', 40),
    ('MARCAR', 'Marcar', '/marcar', 'LogIn', 'Operación', 'Registrar entrada o salida del día.', 50),
    ('ASISTENCIA', 'Asistencia', '/asistencia', 'Fingerprint', 'Operación', 'Consultar el historial de marcaciones.', 60),
    ('PERFIL', 'Mi perfil', '/perfil', 'User', 'Operación', 'Ficha de personal y datos de la cuenta.', 70),
    ('PERSONAL', 'Personal', '/empleados', 'Users', 'Administración', 'Directorio, alta y carga de colaboradores.', 80),
    ('CONTRATOS', 'Contratos', '/contratos', 'FileText', 'Administración', 'Modalidad, horario y vigencia del vínculo laboral.', 85),
    ('MAESTROS', 'Maestros', '/maestros', 'Library', 'Administración', 'Áreas, cargos, horarios, tipos de permiso, parámetros y plan de cuentas.', 88),
    ('USUARIOS', 'Usuarios', '/usuarios', 'UserCog', 'Administración', 'Cuentas de acceso y asignación de perfil.', 90),
    ('ROLES', 'Roles', '/roles', 'KeyRound', 'Administración', 'Mantenedor de perfiles, menús y permisos.', 92),
    ('MENU', 'Menú', '/menu', 'List', 'Administración', 'Mantenedor de opciones de menú por perfil.', 95),
    ('FLUJOS', 'Flujos', '/flujos', 'GitBranch', 'Administración', 'Circuitos de aprobación.', 100),
    ('PLANILLAS', 'Planillas', '/planillas', 'Wallet', 'Gestión', 'Cálculo mensual de remuneraciones y boletas.', 102),
    ('CONTABILIDAD', 'Contabilidad', '/contabilidad', 'BookOpen', 'Gestión', 'Asientos generados al cerrar la planilla.', 104),
    ('DESEMPENO', 'Desempeño', '/desempeno', 'Star', 'Gestión', 'Evaluación periódica del colaborador.', 106),
    ('RECLUTAMIENTO', 'Reclutamiento', '/reclutamiento', 'UserPlus', 'Gestión', 'Convocatorias y seguimiento de postulantes.', 108),
    ('REPORTES', 'Reportes', '/reportes', 'FileSpreadsheet', 'Control', 'Exportar permisos, asistencia y personal.', 110),
    ('AUDITORIA', 'Auditoría', '/auditoria', 'Shield', 'Control', 'Trazabilidad de operaciones.', 120)
) AS v(codigo, etiqueta, ruta, icono, grupo, descripcion, orden)
WHERE NOT EXISTS (SELECT 1 FROM menu_item m WHERE m.codigo = v.codigo);

INSERT INTO menu_rol (id_menu, id_rol)
SELECT m.id_menu, r.id_rol
FROM menu_item m
CROSS JOIN rol r
WHERE r.codigo = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM menu_rol x WHERE x.id_menu = m.id_menu AND x.id_rol = r.id_rol);

INSERT INTO menu_rol (id_menu, id_rol)
SELECT m.id_menu, r.id_rol
FROM menu_item m
JOIN rol r ON r.codigo = 'RRHH'
WHERE m.codigo IN (
    'INICIO', 'BANDEJA', 'PERMISOS', 'HORAS_EXTRAS', 'MARCAR', 'ASISTENCIA', 'PERFIL',
    'PERSONAL', 'CONTRATOS', 'MAESTROS', 'USUARIOS', 'FLUJOS',
    'PLANILLAS', 'CONTABILIDAD', 'DESEMPENO', 'RECLUTAMIENTO',
    'REPORTES', 'AUDITORIA'
)
  AND NOT EXISTS (SELECT 1 FROM menu_rol x WHERE x.id_menu = m.id_menu AND x.id_rol = r.id_rol);

INSERT INTO menu_rol (id_menu, id_rol)
SELECT m.id_menu, r.id_rol
FROM menu_item m
JOIN rol r ON r.codigo = 'APROBADOR'
WHERE m.codigo IN ('INICIO', 'BANDEJA', 'PERMISOS', 'HORAS_EXTRAS', 'MARCAR', 'ASISTENCIA', 'PERFIL', 'DESEMPENO')
  AND NOT EXISTS (SELECT 1 FROM menu_rol x WHERE x.id_menu = m.id_menu AND x.id_rol = r.id_rol);

INSERT INTO menu_rol (id_menu, id_rol)
SELECT m.id_menu, r.id_rol
FROM menu_item m
JOIN rol r ON r.codigo = 'EMPLEADO'
WHERE m.codigo IN ('INICIO', 'BANDEJA', 'PERMISOS', 'HORAS_EXTRAS', 'MARCAR', 'ASISTENCIA', 'PERFIL')
  AND NOT EXISTS (SELECT 1 FROM menu_rol x WHERE x.id_menu = m.id_menu AND x.id_rol = r.id_rol);

INSERT INTO convocatoria (codigo, puesto, id_area, vacantes, fecha_inicio, fecha_fin, descripcion, estado)
SELECT 'CONV-001', 'Analista contable junior', a.id_area, 1, CURRENT_DATE - 10, CURRENT_DATE + 20,
       'Convocatoria de demostración para el área contable.', 'ABIERTA'
FROM area a
WHERE a.nombre ILIKE '%contab%'
  AND NOT EXISTS (SELECT 1 FROM convocatoria c WHERE c.codigo = 'CONV-001')
LIMIT 1;
