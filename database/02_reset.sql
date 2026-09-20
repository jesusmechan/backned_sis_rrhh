-- =============================================================================
-- Limpia trámites y deja las 4 cuentas de prueba.
-- Conserva: áreas, cargos, horarios, tipos de permiso, parámetros, roles,
-- permisos funcionales, menú y configuración de flujos.
-- Vuelve a sembrar marcaciones hábiles desde el 2026-08-03 hasta hoy.
--
-- pgAdmin: Query Tool sobre rrhh_andina, F5.
-- Contraseña: Andina2026. Guía: docs/PRUEBAS.md
-- =============================================================================

-- Por si quedó un paso USUARIO (vacaciones antiguas) apuntando a una cuenta.
UPDATE configuracion_aprobacion_detalle
SET tipo_aprobador = 'JEFE_INMEDIATO', id_rol = NULL, id_usuario = NULL
WHERE tipo_aprobador = 'USUARIO';

DELETE FROM refresh_token;
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

INSERT INTO rol (codigo, nombre, descripcion)
SELECT v.codigo, v.nombre, v.descripcion
FROM (VALUES
    ('ADMIN', 'Administrador', 'Administra usuarios, roles, configuración y auditoría. No participa en los circuitos.'),
    ('GERENCIA', 'Gerencia', 'Autoriza vacaciones, comisiones y decisiones de alto impacto'),
    ('RRHH', 'Recursos Humanos', 'Valida solicitudes, gestiona personal y genera reportes'),
    ('JEFE', 'Jefe de área', 'Aprueba el primer paso de los trámites de su equipo (el asignado sale del organigrama)'),
    ('EMPLEADO', 'Colaborador', 'Registra asistencia, permisos y horas extras propias')
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

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
JOIN permiso_funcional p ON p.codigo IN (
    'PERMISO_REGISTRAR', 'PERMISO_APROBAR', 'PERMISO_CONSULTAR_PROPIO', 'PERMISO_GESTIONAR',
    'HEXTRA_REGISTRAR', 'HEXTRA_APROBAR', 'HEXTRA_CONSULTAR_PROPIO', 'HEXTRA_GESTIONAR',
    'ASISTENCIA_MARCAR'
)
WHERE r.codigo = 'RRHH'
  AND NOT EXISTS (
        SELECT 1 FROM rol_permiso x WHERE x.id_rol = r.id_rol AND x.id_permiso = p.id_permiso);

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
JOIN permiso_funcional p ON p.codigo IN (
    'PERMISO_REGISTRAR', 'PERMISO_APROBAR', 'PERMISO_CONSULTAR_PROPIO',
    'HEXTRA_REGISTRAR', 'HEXTRA_APROBAR', 'HEXTRA_CONSULTAR_PROPIO',
    'ASISTENCIA_MARCAR', 'ASISTENCIA_CONSULTAR_PROPIA'
)
WHERE r.codigo = 'JEFE'
  AND NOT EXISTS (
        SELECT 1 FROM rol_permiso x WHERE x.id_rol = r.id_rol AND x.id_permiso = p.id_permiso);

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
  AND NOT EXISTS (
        SELECT 1 FROM rol_permiso x WHERE x.id_rol = r.id_rol AND x.id_permiso = p.id_permiso);

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

UPDATE configuracion_aprobacion SET descripcion = 'Jefe inmediato y Gerencia.'
WHERE codigo = 'CFG-PERMISO-COMISION';

DELETE FROM menu_rol WHERE id_rol IN (SELECT id_rol FROM rol WHERE codigo = 'APROBADOR');
DELETE FROM rol_permiso WHERE id_rol IN (SELECT id_rol FROM rol WHERE codigo = 'APROBADOR');
DELETE FROM rol WHERE codigo = 'APROBADOR';

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

INSERT INTO contrato (codigo, id_empleado, modalidad, id_horario, fecha_inicio, estado, observaciones, remuneracion_basica)
SELECT v.codigo, e.id_empleado, v.modalidad::modalidad_contrato, e.id_horario, e.fecha_ingreso, 'VIGENTE',
       'Contrato inicial de demostración', v.remuneracion
FROM (VALUES
    ('CTR-001', 'AND-001', 'COLABORADOR', 4200::NUMERIC),
    ('CTR-002', 'AND-002', 'COLABORADOR', 3800::NUMERIC),
    ('CTR-003', 'AND-003', 'COLABORADOR', 3200::NUMERIC),
    ('CTR-004', 'AND-004', 'COLABORADOR', 3600::NUMERIC)
) AS v(codigo, codigo_empleado, modalidad, remuneracion)
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
  AND EXTRACT(ISODOW FROM d.dia) BETWEEN 1 AND 5
  AND NOT EXISTS (
        SELECT 1
        FROM marcacion m
        WHERE m.id_empleado = e.id_empleado
          AND m.tipo = v.tipo::tipo_marcacion
          AND m.fecha = d.dia
  );

INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad, detalle)
SELECT id_usuario, 'RESET_PRUEBAS', 'SISTEMA', NULL,
       jsonb_build_object('origen', '02_reset.sql', 'colaboradores', 4)
FROM usuario
WHERE nombre_usuario = 'jesus.mechan';
