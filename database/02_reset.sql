-- =============================================================================
-- Limpia trámites y deja solo las 3 cuentas de prueba.
-- Conserva: áreas, cargos, horarios, tipos de permiso, parámetros, roles,
-- permisos funcionales, menú y configuración de flujos.
--
-- pgAdmin: Query Tool sobre rrhh_andina, F5.
-- Contraseña: Andina2026. Guía: docs/PRUEBAS.md
-- =============================================================================

-- El paso USUARIO (vacaciones / Gerencia) no admite id_usuario NULL.
-- Se deja temporalmente como JEFE_INMEDIATO para poder borrar cuentas.
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
DELETE FROM usuario;
UPDATE empleado SET id_jefe_inmediato = NULL;
DELETE FROM empleado;

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

INSERT INTO usuario (id_empleado, id_rol, nombre_usuario, correo, password_hash, activo)
SELECT e.id_empleado, r.id_rol, v.nombre_usuario, e.correo_institucional,
       crypt('Andina2026', gen_salt('bf')), TRUE
FROM (VALUES
    ('AND-001', 'jesus.mechan',  'ADMIN'),
    ('AND-002', 'jesus.pantoja', 'APROBADOR'),
    ('AND-003', 'juan.espinoza', 'EMPLEADO')
) AS v(codigo_empleado, nombre_usuario, codigo_rol)
JOIN empleado e ON e.codigo_empleado = v.codigo_empleado
JOIN rol r ON r.codigo = v.codigo_rol;

-- Sin cuenta RRHH: el paso de validación lo atiende el administrador.
UPDATE configuracion_aprobacion_detalle d
SET id_rol = a.id_rol
FROM rol old, rol a
WHERE d.tipo_aprobador = 'ROL'
  AND d.id_rol = old.id_rol
  AND old.codigo = 'RRHH'
  AND a.codigo = 'ADMIN';

UPDATE configuracion_aprobacion_detalle d
SET tipo_aprobador = 'USUARIO',
    id_rol = NULL,
    id_usuario = u.id_usuario
FROM configuracion_aprobacion c, usuario u
WHERE d.id_configuracion = c.id_configuracion
  AND c.codigo = 'CFG-PERMISO-VACACIONES'
  AND d.numero_paso = 3
  AND u.nombre_usuario = 'jesus.mechan';

INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad, detalle)
SELECT id_usuario, 'RESET_PRUEBAS', 'SISTEMA', NULL,
       jsonb_build_object('origen', '02_reset.sql', 'colaboradores', 3)
FROM usuario
WHERE nombre_usuario = 'jesus.mechan';
