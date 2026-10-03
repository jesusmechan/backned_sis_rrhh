-- =============================================================================
-- Limpia trámites y deja las 4 cuentas de prueba.
-- Conserva: áreas, cargos, horarios, tipos de permiso, parámetros, vigencias,
-- catálogos, AFP, regímenes, tramos de 5ta, roles, permisos funcionales, menú,
-- plan de cuentas y cabeceras de flujos.
-- Restaura los pasos de los flujos y vuelve a sembrar marcaciones hábiles
-- desde el 2026-08-03 hasta hoy.
--
-- Requiere una base instalada con 01_install.sql.
-- pgAdmin: Query Tool sobre rrhh_andina, F5.
-- Contraseña: Andina2026. Guía: docs/PRUEBAS.md
-- =============================================================================

DELETE FROM refresh_token;
DELETE FROM notificacion;
DELETE FROM auditoria;
DELETE FROM historial_solicitud;
DELETE FROM solicitud_paso_aprobacion;
DELETE FROM solicitud_permiso;
DELETE FROM solicitud_hora_extra;
DELETE FROM marcacion;
DELETE FROM carga_masiva_detalle;
DELETE FROM carga_masiva;
DELETE FROM reporte_generado;
DELETE FROM postulacion;
DELETE FROM convocatoria;
DELETE FROM evaluacion_desempeno;
DELETE FROM asiento_linea;
DELETE FROM asiento_contable;
DELETE FROM planilla_detalle;
DELETE FROM planilla;
DELETE FROM contrato;
DELETE FROM usuario;
UPDATE empleado SET id_jefe_inmediato = NULL;
DELETE FROM empleado;

-- -----------------------------------------------------------------------------
-- Pasos de los flujos de aprobación
-- -----------------------------------------------------------------------------
DELETE FROM configuracion_aprobacion_detalle;
INSERT INTO configuracion_aprobacion_detalle (
    id_configuracion, numero_paso, nombre_paso, tipo_aprobador, id_rol, id_usuario, es_obligatorio
)
SELECT c.id_configuracion, v.numero_paso, v.nombre_paso, v.tipo_aprobador::tipo_aprobador,
       r.id_rol, NULL, v.es_obligatorio
FROM (VALUES
    ('CFG-PERMISO-DEFAULT', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, TRUE),
    ('CFG-PERMISO-PARTICULAR', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, TRUE),
    ('CFG-PERMISO-SALUD', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, TRUE),
    ('CFG-PERMISO-SALUD', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', TRUE),
    ('CFG-PERMISO-VACACIONES', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, TRUE),
    ('CFG-PERMISO-VACACIONES', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', TRUE),
    ('CFG-PERMISO-VACACIONES', 3, 'Autorización de Gerencia', 'ROL', 'GERENCIA', TRUE),
    ('CFG-PERMISO-CAPACITACION', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, TRUE),
    ('CFG-PERMISO-CAPACITACION', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', TRUE),
    ('CFG-PERMISO-COMISION', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, TRUE),
    ('CFG-PERMISO-COMISION', 2, 'Autorización de Gerencia', 'ROL', 'GERENCIA', TRUE),
    ('CFG-PERMISO-DUELO', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, TRUE),
    ('CFG-PERMISO-DUELO', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', TRUE),
    ('CFG-HEXTRA', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, TRUE),
    ('CFG-HEXTRA', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', TRUE)
) AS v(codigo, numero_paso, nombre_paso, tipo_aprobador, codigo_rol, es_obligatorio)
JOIN configuracion_aprobacion c ON c.codigo = v.codigo
LEFT JOIN rol r ON r.codigo = v.codigo_rol;

-- -----------------------------------------------------------------------------
-- Personal de demostración
-- Organigrama: Juan → Pantoja (jefe) → Mechan (gerencia). Carla (RR. HH.) reporta a Mechan.
-- -----------------------------------------------------------------------------
INSERT INTO empleado (
    codigo_empleado, tipo_documento, numero_documento,
    nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo,
    correo_institucional, telefono, direccion, fecha_ingreso,
    id_area, id_cargo, id_horario, tipo_contrato, estado, id_jefe_inmediato
) VALUES
    ('AND-001', 'DNI', '70123456', 'Jesús', 'Mechan', 'Gonzales', '1994-03-12', 'M',
     'jesus.mechan@andina.pe', '999111001', 'Av. Javier Prado 1200, San Isidro', '2021-11-08',
     (SELECT id_area FROM area WHERE nombre = 'Gerencia'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Gerente General'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada gerencial'),
     'PLANILLA', 'ACTIVO', NULL);

INSERT INTO empleado (
    codigo_empleado, tipo_documento, numero_documento,
    nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo,
    correo_institucional, telefono, direccion, fecha_ingreso,
    id_area, id_cargo, id_horario, tipo_contrato, estado, id_jefe_inmediato
) VALUES
    ('AND-002', 'DNI', '70234567', 'Jesús', 'Pantoja', 'Pantoja', '1990-07-21', 'M',
     'jesus.pantoja@andina.pe', '999111002', 'Av. Arequipa 890, Lince', '2019-07-10',
     (SELECT id_area FROM area WHERE nombre = 'Contabilidad'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Jefe de Contabilidad'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-001'));

INSERT INTO empleado (
    codigo_empleado, tipo_documento, numero_documento,
    nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo,
    correo_institucional, telefono, direccion, fecha_ingreso,
    id_area, id_cargo, id_horario, tipo_contrato, estado, id_jefe_inmediato
) VALUES
    ('AND-003', 'DNI', '70345678', 'Juan', 'Espinoza', '', '1998-05-04', 'M',
     'juan.espinoza@andina.pe', '999111003', 'Av. Universitaria 880, Los Olivos', '2025-06-02',
     (SELECT id_area FROM area WHERE nombre = 'Contabilidad'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Asistente Contable'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-002'));

INSERT INTO empleado (
    codigo_empleado, tipo_documento, numero_documento,
    nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo,
    correo_institucional, telefono, direccion, fecha_ingreso,
    id_area, id_cargo, id_horario, tipo_contrato, estado, id_jefe_inmediato
) VALUES
    ('AND-004', 'DNI', '70456789', 'Carla', 'Reyes', 'Huamán', '1992-09-18', 'F',
     'carla.reyes@andina.pe', '999111004', 'Jr. De la Unión 450, Lima', '2020-03-02',
     (SELECT id_area FROM area WHERE nombre = 'Recursos Humanos'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Jefe de Recursos Humanos'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-001'));

INSERT INTO usuario (id_empleado, id_rol, nombre_usuario, correo, password_hash, activo)
SELECT e.id_empleado, r.id_rol, v.nombre_usuario, e.correo_institucional,
       crypt('Andina2026', gen_salt('bf')), TRUE
FROM (VALUES
    ('AND-001', 'jesus.mechan',  'ADMIN'),
    ('AND-002', 'jesus.pantoja', 'JEFE'),
    ('AND-003', 'juan.espinoza', 'EMPLEADO'),
    ('AND-004', 'carla.reyes',   'RRHH')
) AS v(codigo_empleado, nombre_usuario, codigo_rol)
JOIN empleado e ON e.codigo_empleado = v.codigo_empleado
JOIN rol r ON r.codigo = v.codigo_rol;

INSERT INTO contrato (
    codigo, id_empleado, modalidad, id_horario, fecha_inicio, estado, observaciones,
    remuneracion_basica, regimen_pensionario, afp_nombre, regimen_laboral, tiene_asignacion_familiar
)
SELECT v.codigo, e.id_empleado, v.modalidad::modalidad_contrato, e.id_horario, e.fecha_ingreso, 'VIGENTE',
       'Contrato inicial de demostración', v.remuneracion,
       v.regimen::regimen_pensionario, v.afp, 'GENERAL', v.asignacion
FROM (VALUES
    ('CTR-001', 'AND-001', 'COLABORADOR', 4200::NUMERIC, 'ONP', NULL, TRUE),
    ('CTR-002', 'AND-002', 'COLABORADOR', 3800::NUMERIC, 'AFP', 'INTEGRA', TRUE),
    ('CTR-003', 'AND-003', 'COLABORADOR', 3200::NUMERIC, 'AFP', 'HABITAT', FALSE),
    ('CTR-004', 'AND-004', 'COLABORADOR', 3600::NUMERIC, 'ONP', NULL, FALSE)
) AS v(codigo, codigo_empleado, modalidad, remuneracion, regimen, afp, asignacion)
JOIN empleado e ON e.codigo_empleado = v.codigo_empleado;

INSERT INTO convocatoria (codigo, puesto, id_area, vacantes, fecha_inicio, fecha_fin, descripcion, estado)
SELECT 'CONV-001', 'Analista contable junior', a.id_area, 1, CURRENT_DATE - 10, CURRENT_DATE + 20,
       'Convocatoria de demostración para el área contable.', 'ABIERTA'
FROM area a
WHERE a.nombre ILIKE '%contab%'
LIMIT 1;

-- Asistencia: lun-vie desde el 3 de agosto de 2026 hasta hoy (America/Lima).
INSERT INTO marcacion (id_empleado, tipo, fecha_hora, origen, observacion, id_usuario_registro)
SELECT
    e.id_empleado,
    v.tipo::tipo_marcacion,
    ((d.dia + v.hora) + make_interval(mins => v.ajuste)) AT TIME ZONE 'America/Lima',
    'WEB',
    'Jornada ordinaria',
    u.id_usuario
FROM usuario u
JOIN empleado e ON e.id_empleado = u.id_empleado
JOIN horario_laboral h ON h.id_horario = e.id_horario
JOIN generate_series(
        DATE '2026-08-03',
        (CURRENT_TIMESTAMP AT TIME ZONE 'America/Lima')::date,
        INTERVAL '1 day'
     ) AS gs(ts) ON TRUE
CROSS JOIN LATERAL (SELECT gs.ts::date AS dia) AS d
CROSS JOIN LATERAL (
    VALUES
        ('INGRESO', h.hora_ingreso, (abs(hashtext(e.id_empleado::text || d.dia::text || 'I')) % 11) - 4),
        ('SALIDA',  h.hora_salida,  (abs(hashtext(e.id_empleado::text || d.dia::text || 'S')) % 16) - 2)
) AS v(tipo, hora, ajuste)
WHERE u.activo
  AND e.estado = 'ACTIVO'
  AND d.dia >= e.fecha_ingreso
  AND EXTRACT(ISODOW FROM d.dia) BETWEEN 1 AND 5;

INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad, detalle)
SELECT id_usuario, 'RESET_PRUEBAS', 'SISTEMA', NULL,
       jsonb_build_object('origen', '02_reset.sql', 'colaboradores', 4)
FROM usuario
WHERE nombre_usuario = 'jesus.mechan';
