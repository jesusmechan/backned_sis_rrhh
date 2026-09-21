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
UPDATE contrato SET remuneracion_basica = 3600 WHERE codigo = 'CTR-004' AND remuneracion_basica = 0;

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

INSERT INTO rol (codigo, nombre, descripcion)
SELECT v.codigo, v.nombre, v.descripcion
FROM (VALUES
    ('GERENCIA', 'Gerencia', 'Autoriza vacaciones, comisiones y decisiones de alto impacto'),
    ('JEFE', 'Jefe de área', 'Aprueba el primer paso de los trámites de su equipo (el asignado sale del organigrama)')
) AS v(codigo, nombre, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM rol r WHERE r.codigo = v.codigo);

UPDATE rol SET nombre = v.nombre, descripcion = v.descripcion, activo = TRUE
FROM (VALUES
    ('ADMIN', 'Administrador', 'Administra usuarios, roles, configuración y auditoría. No participa en los circuitos.'),
    ('GERENCIA', 'Gerencia', 'Autoriza vacaciones, comisiones y decisiones de alto impacto'),
    ('RRHH', 'Recursos Humanos', 'Valida solicitudes, gestiona personal y genera reportes'),
    ('JEFE', 'Jefe de área', 'Aprueba el primer paso de los trámites de su equipo (el asignado sale del organigrama)'),
    ('EMPLEADO', 'Colaborador', 'Registra asistencia, permisos y horas extras propias')
) AS v(codigo, nombre, descripcion)
WHERE rol.codigo = v.codigo;

INSERT INTO menu_rol (id_menu, id_rol)
SELECT m.id_menu, r.id_rol
FROM menu_item m
JOIN rol r ON r.codigo = 'JEFE'
WHERE m.codigo IN ('INICIO', 'BANDEJA', 'PERMISOS', 'HORAS_EXTRAS', 'MARCAR', 'ASISTENCIA', 'PERFIL', 'DESEMPENO')
  AND NOT EXISTS (SELECT 1 FROM menu_rol x WHERE x.id_menu = m.id_menu AND x.id_rol = r.id_rol);

INSERT INTO menu_rol (id_menu, id_rol)
SELECT m.id_menu, r.id_rol
FROM menu_item m
JOIN rol r ON r.codigo = 'GERENCIA'
WHERE m.codigo IN (
    'INICIO', 'BANDEJA', 'PERMISOS', 'HORAS_EXTRAS', 'MARCAR', 'ASISTENCIA', 'PERFIL',
    'PERSONAL', 'DESEMPENO', 'REPORTES'
)
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

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
JOIN permiso_funcional p ON p.codigo IN (
    'PERMISO_REGISTRAR', 'PERMISO_APROBAR', 'PERMISO_GESTIONAR', 'PERMISO_CONSULTAR_PROPIO',
    'HEXTRA_REGISTRAR', 'HEXTRA_APROBAR', 'HEXTRA_GESTIONAR', 'HEXTRA_CONSULTAR_PROPIO',
    'ASISTENCIA_MARCAR'
)
WHERE r.codigo = 'RRHH'
  AND NOT EXISTS (SELECT 1 FROM rol_permiso x WHERE x.id_rol = r.id_rol AND x.id_permiso = p.id_permiso);

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
JOIN permiso_funcional p ON p.codigo IN (
    'PERMISO_REGISTRAR', 'PERMISO_APROBAR', 'PERMISO_CONSULTAR_PROPIO',
    'HEXTRA_REGISTRAR', 'HEXTRA_APROBAR', 'HEXTRA_CONSULTAR_PROPIO',
    'ASISTENCIA_MARCAR', 'ASISTENCIA_CONSULTAR_PROPIA'
)
WHERE r.codigo = 'JEFE'
  AND NOT EXISTS (SELECT 1 FROM rol_permiso x WHERE x.id_rol = r.id_rol AND x.id_permiso = p.id_permiso);

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
JOIN permiso_funcional p ON p.codigo IN (
    'PERMISO_REGISTRAR', 'PERMISO_APROBAR', 'PERMISO_CONSULTAR_PROPIO',
    'HEXTRA_REGISTRAR', 'HEXTRA_APROBAR', 'HEXTRA_CONSULTAR_PROPIO',
    'ASISTENCIA_MARCAR', 'ASISTENCIA_CONSULTAR_PROPIA',
    'PERSONAL_CONSULTAR',
    'REPORTE_PERMISOS', 'REPORTE_HORAS_EXTRAS', 'REPORTE_ASISTENCIA'
)
WHERE r.codigo = 'GERENCIA'
  AND NOT EXISTS (SELECT 1 FROM rol_permiso x WHERE x.id_rol = r.id_rol AND x.id_permiso = p.id_permiso);

UPDATE usuario u
SET id_rol = r.id_rol
FROM rol r
WHERE u.nombre_usuario = 'jesus.pantoja'
  AND r.codigo = 'JEFE';

INSERT INTO empleado (
    codigo_empleado, tipo_documento, numero_documento,
    nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo,
    correo_institucional, telefono, direccion, fecha_ingreso,
    id_area, id_cargo, id_horario, tipo_contrato, estado, id_jefe_inmediato
)
SELECT 'AND-004', 'DNI', '70456789', 'Carla', 'Reyes', 'Huamán', '1992-09-18', 'F',
       'carla.reyes@andina.pe', '999111004', 'Jr. De la Unión 450, Lima', '2020-03-02',
       a.id_area, c.id_cargo, h.id_horario, 'PLANILLA', 'ACTIVO', j.id_empleado
FROM area a, cargo c, horario_laboral h, empleado j
WHERE a.nombre = 'Recursos Humanos'
  AND c.nombre = 'Jefe de Recursos Humanos'
  AND h.nombre = 'Jornada administrativa'
  AND j.codigo_empleado = 'AND-001'
  AND NOT EXISTS (SELECT 1 FROM empleado e WHERE e.codigo_empleado = 'AND-004');

INSERT INTO usuario (id_empleado, id_rol, nombre_usuario, correo, password_hash, activo)
SELECT e.id_empleado, r.id_rol, 'carla.reyes', e.correo_institucional,
       crypt('Andina2026', gen_salt('bf')), TRUE
FROM empleado e
JOIN rol r ON r.codigo = 'RRHH'
WHERE e.codigo_empleado = 'AND-004'
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.nombre_usuario = 'carla.reyes');

INSERT INTO contrato (codigo, id_empleado, modalidad, id_horario, fecha_inicio, estado, observaciones, remuneracion_basica)
SELECT 'CTR-004', e.id_empleado, 'COLABORADOR', e.id_horario, e.fecha_ingreso, 'VIGENTE',
       'Contrato inicial de demostración', 3600
FROM empleado e
WHERE e.codigo_empleado = 'AND-004'
  AND NOT EXISTS (SELECT 1 FROM contrato c WHERE c.codigo = 'CTR-004');

UPDATE configuracion_aprobacion_detalle d
SET tipo_aprobador = 'ROL',
    id_rol = r.id_rol,
    id_usuario = NULL,
    nombre_paso = 'Validación de RR. HH.'
FROM configuracion_aprobacion c, rol r
WHERE d.id_configuracion = c.id_configuracion
  AND r.codigo = 'RRHH'
  AND d.numero_paso = 2
  AND c.codigo IN (
        'CFG-PERMISO-SALUD', 'CFG-PERMISO-VACACIONES', 'CFG-PERMISO-CAPACITACION',
        'CFG-PERMISO-DUELO', 'CFG-HEXTRA');

UPDATE configuracion_aprobacion_detalle d
SET tipo_aprobador = 'ROL',
    id_rol = r.id_rol,
    id_usuario = NULL,
    nombre_paso = 'Autorización de Gerencia'
FROM configuracion_aprobacion c, rol r
WHERE d.id_configuracion = c.id_configuracion
  AND r.codigo = 'GERENCIA'
  AND c.codigo = 'CFG-PERMISO-COMISION'
  AND d.numero_paso = 2;

UPDATE configuracion_aprobacion_detalle d
SET tipo_aprobador = 'ROL',
    id_rol = r.id_rol,
    id_usuario = NULL,
    nombre_paso = 'Autorización de Gerencia'
FROM configuracion_aprobacion c, rol r
WHERE d.id_configuracion = c.id_configuracion
  AND r.codigo = 'GERENCIA'
  AND c.codigo = 'CFG-PERMISO-VACACIONES'
  AND d.numero_paso = 3;

INSERT INTO configuracion_aprobacion_detalle (
    id_configuracion, numero_paso, nombre_paso, tipo_aprobador, id_rol, es_obligatorio
)
SELECT c.id_configuracion, 3, 'Autorización de Gerencia', 'ROL', r.id_rol, TRUE
FROM configuracion_aprobacion c
JOIN rol r ON r.codigo = 'GERENCIA'
WHERE c.codigo = 'CFG-PERMISO-VACACIONES'
  AND NOT EXISTS (
        SELECT 1 FROM configuracion_aprobacion_detalle d
        WHERE d.id_configuracion = c.id_configuracion AND d.numero_paso = 3);

UPDATE configuracion_aprobacion SET descripcion = 'Jefe inmediato y Gerencia.'
WHERE codigo = 'CFG-PERMISO-COMISION';

UPDATE rol SET activo = FALSE WHERE codigo = 'APROBADOR';

CREATE TABLE IF NOT EXISTS notificacion (
    id_notificacion INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_usuario      INTEGER NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    tipo            VARCHAR(40) NOT NULL,
    titulo          VARCHAR(160) NOT NULL,
    mensaje         VARCHAR(400) NOT NULL,
    ruta            VARCHAR(160),
    tipo_solicitud  VARCHAR(20),
    id_solicitud    INTEGER,
    id_paso         INTEGER,
    leida           BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_notificacion_usuario ON notificacion (id_usuario, leida, fecha_creacion DESC);

