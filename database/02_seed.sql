-- =============================================================================
-- Datos iniciales - Consultora Contable Andina S.A.C.
-- Contraseña de todos los usuarios de demostración: Andina2026
--
-- pgAdmin: abre Query Tool sobre la base rrhh_andina (después de 01_schema.sql)
-- y ejecuta este archivo completo con F5. No uses \i ni \c; pgAdmin no los interpreta.
-- =============================================================================

INSERT INTO area (nombre, descripcion) VALUES
    ('Gerencia', 'Dirección general de la consultora'),
    ('Recursos Humanos', 'Administración del personal y procesos internos'),
    ('Contabilidad', 'Servicios contables a clientes'),
    ('Tributario', 'Asesoría tributaria y cumplimiento fiscal'),
    ('Administración', 'Soporte administrativo y operaciones internas');

INSERT INTO cargo (nombre, descripcion) VALUES
    ('Gerente General', 'Representante legal y sponsor de la organización'),
    ('Jefe de Recursos Humanos', 'Responsable de la gestión del personal'),
    ('Jefe de Contabilidad', 'Responsable del área contable'),
    ('Contador', 'Ejecución de labores contables'),
    ('Analista Contable', 'Análisis y registro de operaciones contables'),
    ('Asistente Contable', 'Apoyo operativo del área contable'),
    ('Analista Tributario', 'Análisis de obligaciones tributarias'),
    ('Asistente Tributario', 'Apoyo operativo del área tributaria'),
    ('Asistente Administrativo', 'Soporte administrativo interno'),
    ('Asistente de Recepción', 'Atención y apoyo operativo general'),
    ('Jefe de Tributario', 'Responsable del área tributaria'),
    ('Asistente de Recursos Humanos', 'Apoyo operativo de RR. HH.');

INSERT INTO horario_laboral (nombre, hora_ingreso, hora_salida, minutos_refrigerio) VALUES
    ('Jornada administrativa', '09:00', '18:00', 60),
    ('Jornada gerencial', '09:00', '18:30', 60);

INSERT INTO tipo_permiso (codigo, nombre, requiere_sustento) VALUES
    ('PARTICULAR', 'Permiso particular', FALSE),
    ('SALUD', 'Permiso por salud', TRUE),
    ('COMISION', 'Comisión de servicios', FALSE),
    ('CAPACITACION', 'Capacitación', FALSE),
    ('DUELO', 'Permiso por duelo', TRUE),
    ('VACACIONES', 'Vacaciones', FALSE);

INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('max_horas_extras_diarias', '4', 'Máximo de horas extras permitidas por día (RRN-03)'),
    ('max_horas_extras_semanales', '12', 'Máximo de horas extras permitidas por semana'),
    ('zona_horaria', 'America/Lima', 'Zona horaria oficial de las marcaciones'),
    ('empresa_razon_social', 'Consultora Contable Andina S.A.C.', 'Razón social de la empresa'),
    ('empresa_ruc', '20601234567', 'RUC de demostración');

INSERT INTO rol (codigo, nombre, descripcion) VALUES
    ('ADMIN', 'Administrador', 'Administra usuarios, roles, configuración y auditoría'),
    ('RRHH', 'Responsable de RR. HH.', 'Gestiona personal, supervisa solicitudes y genera reportes'),
    ('APROBADOR', 'Aprobador', 'Revisa, aprueba o rechaza permisos y horas extras'),
    ('EMPLEADO', 'Empleado', 'Registra asistencia, permisos y horas extras propias');

INSERT INTO permiso_funcional (codigo, nombre, modulo) VALUES
    ('PERSONAL_REGISTRAR', 'Registrar trabajador', 'PERSONAL'),
    ('PERSONAL_ACTUALIZAR', 'Actualizar trabajador', 'PERSONAL'),
    ('PERSONAL_CONSULTAR', 'Consultar trabajador', 'PERSONAL'),
    ('PERSONAL_CARGAR_EXCEL', 'Cargar trabajadores desde Excel', 'PERSONAL'),
    ('PERMISO_REGISTRAR', 'Registrar solicitud de permiso', 'PERMISOS'),
    ('PERMISO_CONSULTAR_PROPIO', 'Consultar estado de permiso propio', 'PERMISOS'),
    ('PERMISO_APROBAR', 'Aprobar o rechazar permiso', 'PERMISOS'),
    ('PERMISO_GESTIONAR', 'Gestionar solicitudes de permisos', 'PERMISOS'),
    ('HEXTRA_REGISTRAR', 'Registrar horas extras', 'HORAS_EXTRAS'),
    ('HEXTRA_CONSULTAR_PROPIO', 'Consultar solicitud de horas extras propia', 'HORAS_EXTRAS'),
    ('HEXTRA_APROBAR', 'Aprobar o rechazar horas extras', 'HORAS_EXTRAS'),
    ('HEXTRA_GESTIONAR', 'Supervisar horas extras', 'HORAS_EXTRAS'),
    ('ASISTENCIA_MARCAR', 'Registrar marcación de asistencia', 'ASISTENCIA'),
    ('ASISTENCIA_CONSULTAR_PROPIA', 'Consultar historial de asistencia propio', 'ASISTENCIA'),
    ('ASISTENCIA_GESTIONAR', 'Gestionar registros de asistencia', 'ASISTENCIA'),
    ('USUARIO_GESTIONAR', 'Gestionar usuarios y roles', 'USUARIOS'),
    ('REPORTE_PERMISOS', 'Generar reporte de permisos', 'REPORTES'),
    ('REPORTE_HORAS_EXTRAS', 'Generar reporte de horas extras', 'REPORTES'),
    ('REPORTE_ASISTENCIA', 'Generar reporte de asistencia', 'REPORTES'),
    ('REPORTE_USUARIOS', 'Generar reporte de usuarios', 'REPORTES'),
    ('AUDITORIA_CONSULTAR', 'Consultar auditoría y trazabilidad', 'AUDITORIA'),
    ('FLUJO_CONFIGURAR', 'Configurar flujos de aprobación', 'APROBACIONES');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso_funcional p
WHERE r.codigo = 'ADMIN';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
JOIN permiso_funcional p ON p.codigo IN (
    'PERSONAL_REGISTRAR', 'PERSONAL_ACTUALIZAR', 'PERSONAL_CONSULTAR', 'PERSONAL_CARGAR_EXCEL',
    'PERMISO_CONSULTAR_PROPIO', 'PERMISO_GESTIONAR',
    'HEXTRA_CONSULTAR_PROPIO', 'HEXTRA_GESTIONAR',
    'ASISTENCIA_CONSULTAR_PROPIA', 'ASISTENCIA_GESTIONAR',
    'REPORTE_PERMISOS', 'REPORTE_HORAS_EXTRAS', 'REPORTE_ASISTENCIA', 'REPORTE_USUARIOS',
    'AUDITORIA_CONSULTAR', 'FLUJO_CONFIGURAR'
)
WHERE r.codigo = 'RRHH';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
JOIN permiso_funcional p ON p.codigo IN (
    'PERMISO_APROBAR', 'PERMISO_CONSULTAR_PROPIO',
    'HEXTRA_APROBAR', 'HEXTRA_CONSULTAR_PROPIO',
    'ASISTENCIA_MARCAR', 'ASISTENCIA_CONSULTAR_PROPIA'
)
WHERE r.codigo = 'APROBADOR';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
JOIN permiso_funcional p ON p.codigo IN (
    'PERMISO_REGISTRAR', 'PERMISO_CONSULTAR_PROPIO',
    'HEXTRA_REGISTRAR', 'HEXTRA_CONSULTAR_PROPIO',
    'ASISTENCIA_MARCAR', 'ASISTENCIA_CONSULTAR_PROPIA'
)
WHERE r.codigo = 'EMPLEADO';

-- 10 colaboradores de la empresa (OBN-03). Se insertan por niveles para resolver id_jefe_inmediato.
INSERT INTO empleado (
    codigo_empleado, tipo_documento, numero_documento,
    nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo,
    correo_institucional, telefono, direccion, fecha_ingreso,
    id_area, id_cargo, id_horario, tipo_contrato, estado, id_jefe_inmediato
) VALUES
    ('AND-001', 'DNI', '10456789', 'Roberto', 'Salas', 'Hidalgo', '1978-04-12', 'M',
     'roberto.salas@andina.pe', '999111001', 'Av. Javier Prado 1250, San Isidro', '2018-01-15',
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
    ('AND-002', 'DNI', '40567812', 'María Elena', 'Quispe', 'Rojas', '1986-09-03', 'F',
     'maria.quispe@andina.pe', '999111002', 'Jr. Lampa 340, Cercado de Lima', '2020-03-02',
     (SELECT id_area FROM area WHERE nombre = 'Recursos Humanos'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Jefe de Recursos Humanos'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-001')),
    ('AND-003', 'DNI', '41789034', 'Carlos Alberto', 'Mendoza', 'Paredes', '1984-11-21', 'M',
     'carlos.mendoza@andina.pe', '999111003', 'Av. Arequipa 890, Lince', '2019-07-10',
     (SELECT id_area FROM area WHERE nombre = 'Contabilidad'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Jefe de Contabilidad'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-001')),
    ('AND-006', 'DNI', '44012367', 'Patricia', 'Ramos', 'Delgado', '1991-06-27', 'F',
     'patricia.ramos@andina.pe', '999111006', 'Jr. Huancavelica 215, Breña', '2022-02-14',
     (SELECT id_area FROM area WHERE nombre = 'Tributario'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Analista Tributario'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-001'));

INSERT INTO empleado (
    codigo_empleado, tipo_documento, numero_documento,
    nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo,
    correo_institucional, telefono, direccion, fecha_ingreso,
    id_area, id_cargo, id_horario, tipo_contrato, estado, id_jefe_inmediato
) VALUES
    ('AND-004', 'DNI', '42890145', 'Ana Lucía', 'Torres', 'Vega', '1992-02-18', 'F',
     'ana.torres@andina.pe', '999111004', 'Calle Los Alamos 112, Surco', '2021-05-17',
     (SELECT id_area FROM area WHERE nombre = 'Contabilidad'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Contador'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-003')),
    ('AND-005', 'DNI', '43901256', 'Luis Fernando', 'Vargas', 'Cruz', '1996-08-09', 'M',
     'luis.vargas@andina.pe', '999111005', 'Av. Brasil 560, Pueblo Libre', '2023-01-09',
     (SELECT id_area FROM area WHERE nombre = 'Contabilidad'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Asistente Contable'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-003')),
    ('AND-007', 'DNI', '45123478', 'Jorge Luis', 'Huamán', 'Flores', '1998-12-01', 'M',
     'jorge.huaman@andina.pe', '999111007', 'Av. Colonial 1780, Cercado de Lima', '2024-03-04',
     (SELECT id_area FROM area WHERE nombre = 'Tributario'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Asistente Tributario'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-006')),
    ('AND-008', 'DNI', '46234589', 'Elena', 'Díaz', 'Salazar', '1990-01-30', 'F',
     'elena.diaz@andina.pe', '999111008', 'Calle Las Begonias 88, San Isidro', '2021-11-08',
     (SELECT id_area FROM area WHERE nombre = 'Administración'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Asistente Administrativo'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-002')),
    ('AND-009', 'DNI', '47345690', 'Miguel Ángel', 'Soto', 'Navarro', '1994-05-14', 'M',
     'miguel.soto@andina.pe', '999111009', 'Av. Universitaria 1450, Los Olivos', '2022-08-22',
     (SELECT id_area FROM area WHERE nombre = 'Contabilidad'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Analista Contable'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-003')),
    ('AND-010', 'DNI', '48456701', 'Rosa María', 'Flores', 'Campos', '1999-10-06', 'F',
     'rosa.flores@andina.pe', '999111010', 'Jr. Cusco 250, Cercado de Lima', '2025-01-13',
     (SELECT id_area FROM area WHERE nombre = 'Administración'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Asistente de Recepción'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-002'));

INSERT INTO empleado (
    codigo_empleado, tipo_documento, numero_documento,
    nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo,
    correo_institucional, telefono, direccion, fecha_ingreso,
    id_area, id_cargo, id_horario, tipo_contrato, estado, id_jefe_inmediato
) VALUES
    ('AND-014', 'DNI', '52890145', 'Silvia', 'Paredes', 'León', '1985-07-19', 'F',
     'silvia.paredes@andina.pe', '999111014', 'Av. Petit Thouars 1180, Lince', '2021-02-01',
     (SELECT id_area FROM area WHERE nombre = 'Tributario'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Jefe de Tributario'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-001'));

INSERT INTO empleado (
    codigo_empleado, tipo_documento, numero_documento,
    nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo,
    correo_institucional, telefono, direccion, fecha_ingreso,
    id_area, id_cargo, id_horario, tipo_contrato, estado, id_jefe_inmediato
) VALUES
    ('AND-011', 'DNI', '49567812', 'Diego', 'León', 'Salazar', '1995-03-11', 'M',
     'diego.leon@andina.pe', '999111011', 'Av. La Marina 2030, San Miguel', '2024-06-03',
     (SELECT id_area FROM area WHERE nombre = 'Contabilidad'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Analista Contable'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-003')),
    ('AND-012', 'DNI', '50678923', 'Carmen', 'Silva', 'Ortiz', '1993-09-25', 'F',
     'carmen.silva@andina.pe', '999111012', 'Jr. Camaná 410, Cercado de Lima', '2023-04-17',
     (SELECT id_area FROM area WHERE nombre = 'Recursos Humanos'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Asistente de Recursos Humanos'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-002')),
    ('AND-013', 'DNI', '51789034', 'Valeria', 'Chávez', 'Ríos', '1997-01-08', 'F',
     'valeria.chavez@andina.pe', '999111013', 'Av. Brasil 890, Jesús María', '2024-09-16',
     (SELECT id_area FROM area WHERE nombre = 'Tributario'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Analista Tributario'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-014')),
    ('AND-015', 'DNI', '53901256', 'Bruno', 'Aguilar', 'Medina', '2000-05-22', 'M',
     'bruno.aguilar@andina.pe', '999111015', 'Calle Los Pinos 45, Surco', '2025-03-10',
     (SELECT id_area FROM area WHERE nombre = 'Tributario'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Asistente Tributario'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-014')),
    ('AND-016', 'DNI', '54012367', 'Lucía', 'Benavides', 'Cruz', '1998-11-14', 'F',
     'lucia.benavides@andina.pe', '999111016', 'Av. Universitaria 880, Los Olivos', '2025-06-02',
     (SELECT id_area FROM area WHERE nombre = 'Contabilidad'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Asistente Contable'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-003')),
    ('AND-017', 'DNI', '55123478', 'Héctor', 'Palomino', 'Grau', '1996-04-03', 'M',
     'hector.palomino@andina.pe', '999111017', 'Jr. Cañete 155, Breña', '2025-08-18',
     (SELECT id_area FROM area WHERE nombre = 'Administración'),
     (SELECT id_cargo FROM cargo WHERE nombre = 'Asistente Administrativo'),
     (SELECT id_horario FROM horario_laboral WHERE nombre = 'Jornada administrativa'),
     'PLANILLA', 'ACTIVO',
     (SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-002'));

INSERT INTO usuario (id_empleado, id_rol, nombre_usuario, correo, password_hash, activo)
SELECT e.id_empleado, r.id_rol, v.nombre_usuario, e.correo_institucional,
       crypt('Andina2026', gen_salt('bf')), TRUE
FROM (VALUES
    ('AND-001', 'rsalas',    'APROBADOR'),
    ('AND-002', 'mquispe',   'RRHH'),
    ('AND-003', 'cmendoza',  'APROBADOR'),
    ('AND-004', 'atorres',   'EMPLEADO'),
    ('AND-005', 'lvargas',   'EMPLEADO'),
    ('AND-006', 'pramos',    'EMPLEADO'),
    ('AND-007', 'jhuaman',   'EMPLEADO'),
    ('AND-008', 'ediaz',     'ADMIN'),
    ('AND-009', 'msoto',     'EMPLEADO'),
    ('AND-010', 'rflores',   'EMPLEADO'),
    ('AND-011', 'dleon',     'EMPLEADO'),
    ('AND-012', 'csilva',    'RRHH'),
    ('AND-013', 'vchavez',   'EMPLEADO'),
    ('AND-014', 'sparedes',  'APROBADOR'),
    ('AND-015', 'baguilar',  'EMPLEADO'),
    ('AND-016', 'lbenavides','EMPLEADO'),
    ('AND-017', 'hpalomino', 'EMPLEADO')
) AS v(codigo_empleado, nombre_usuario, codigo_rol)
JOIN empleado e ON e.codigo_empleado = v.codigo_empleado
JOIN rol r ON r.codigo = v.codigo_rol;

-- Circuitos de aprobación: cabecera + detalle por tipo de trámite
INSERT INTO configuracion_aprobacion (codigo, nombre, tipo_origen, id_tipo_permiso, descripcion) VALUES
    ('CFG-PERMISO-DEFAULT', 'Permiso genérico', 'PERMISO', NULL,
     'Flujo por defecto si un tipo de permiso no tiene circuito propio.'),
    ('CFG-PERMISO-PARTICULAR', 'Permiso particular', 'PERMISO',
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'PARTICULAR'),
     'Solo jefe inmediato.'),
    ('CFG-PERMISO-SALUD', 'Permiso por salud', 'PERMISO',
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'SALUD'),
     'Jefe inmediato y luego RR. HH.'),
    ('CFG-PERMISO-VACACIONES', 'Vacaciones', 'PERMISO',
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'VACACIONES'),
     'Jefe inmediato, RR. HH. y Gerencia.'),
    ('CFG-PERMISO-CAPACITACION', 'Capacitación', 'PERMISO',
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'CAPACITACION'),
     'Jefe inmediato y RR. HH.'),
    ('CFG-PERMISO-COMISION', 'Comisión de servicios', 'PERMISO',
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'COMISION'),
     'Jefe inmediato y rol aprobador.'),
    ('CFG-PERMISO-DUELO', 'Permiso por duelo', 'PERMISO',
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'DUELO'),
     'Jefe inmediato y RR. HH.'),
    ('CFG-HEXTRA', 'Horas extras', 'HORA_EXTRA', NULL,
     'Jefe inmediato y validación de RR. HH.');

INSERT INTO configuracion_aprobacion_detalle (
    id_configuracion, numero_paso, nombre_paso, tipo_aprobador, id_rol, id_usuario, es_obligatorio
)
SELECT c.id_configuracion, v.numero_paso, v.nombre_paso, v.tipo_aprobador::tipo_aprobador,
       r.id_rol, u.id_usuario, v.es_obligatorio
FROM (VALUES
    ('CFG-PERMISO-DEFAULT', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, NULL, TRUE),
    ('CFG-PERMISO-PARTICULAR', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, NULL, TRUE),
    ('CFG-PERMISO-SALUD', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, NULL, TRUE),
    ('CFG-PERMISO-SALUD', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', NULL, TRUE),
    ('CFG-PERMISO-VACACIONES', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, NULL, TRUE),
    ('CFG-PERMISO-VACACIONES', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', NULL, TRUE),
    ('CFG-PERMISO-VACACIONES', 3, 'Autorización de Gerencia', 'USUARIO', NULL, 'rsalas', TRUE),
    ('CFG-PERMISO-CAPACITACION', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, NULL, TRUE),
    ('CFG-PERMISO-CAPACITACION', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', NULL, TRUE),
    ('CFG-PERMISO-COMISION', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, NULL, TRUE),
    ('CFG-PERMISO-COMISION', 2, 'Aprobación de jefatura', 'ROL', 'APROBADOR', NULL, TRUE),
    ('CFG-PERMISO-DUELO', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, NULL, TRUE),
    ('CFG-PERMISO-DUELO', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', NULL, TRUE),
    ('CFG-HEXTRA', 1, 'Jefe inmediato', 'JEFE_INMEDIATO', NULL, NULL, TRUE),
    ('CFG-HEXTRA', 2, 'Validación de RR. HH.', 'ROL', 'RRHH', NULL, TRUE)
) AS v(codigo, numero_paso, nombre_paso, tipo_aprobador, codigo_rol, nombre_usuario, es_obligatorio)
JOIN configuracion_aprobacion c ON c.codigo = v.codigo
LEFT JOIN rol r ON r.codigo = v.codigo_rol
LEFT JOIN usuario u ON u.nombre_usuario = v.nombre_usuario;

-- Las solicitudes nacen PENDIENTE; el trigger instancia los pasos del flujo.
INSERT INTO solicitud_permiso (id_empleado, id_tipo_permiso, fecha_inicio, fecha_fin, motivo) VALUES
    ((SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-005'),
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'PARTICULAR'),
     '2026-08-20', '2026-08-20',
     'Trámite personal en RENIEC durante la mañana.'),
    ((SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-004'),
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'SALUD'),
     '2026-08-28', '2026-08-29',
     'Descanso médico por evaluación particular.'),
    ((SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-007'),
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'CAPACITACION'),
     '2026-09-10', '2026-09-10',
     'Curso de actualización tributaria SUNAT.'),
    ((SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-010'),
     (SELECT id_tipo_permiso FROM tipo_permiso WHERE codigo = 'PARTICULAR'),
     '2026-09-03', '2026-09-03',
     'Cita personal en la mañana.');

INSERT INTO solicitud_hora_extra (id_empleado, fecha, hora_inicio, hora_fin, cantidad_horas, motivo) VALUES
    ((SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-004'),
     '2026-08-25', '18:00', '21:00', 3.00,
     'Cierre contable mensual de clientes PYME.'),
    ((SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-009'),
     '2026-08-26', '18:00', '20:30', 2.50,
     'Conciliación bancaria de cierre de mes.'),
    ((SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-006'),
     '2026-09-02', '18:00', '21:00', 3.00,
     'Declaración mensual de clientes con vencimiento SUNAT.'),
    ((SELECT id_empleado FROM empleado WHERE codigo_empleado = 'AND-005'),
     '2026-09-01', '18:00', '20:00', 2.00,
     'Regularización de asientos observados.');

-- Particular AND-005: un solo paso, lo aprueba su jefe (cmendoza)
UPDATE solicitud_paso_aprobacion p
SET estado = 'APROBADO',
    id_usuario_decision = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'cmendoza'),
    fecha_decision = '2026-08-18 10:15:00-05',
    comentario = 'Aprobado. Coordinar cobertura con el área.'
FROM solicitud_permiso sp
JOIN empleado e ON e.id_empleado = sp.id_empleado
WHERE p.id_solicitud_permiso = sp.id_solicitud_permiso
  AND e.codigo_empleado = 'AND-005'
  AND p.numero_paso = 1;

-- Salud AND-004: jefe y luego RR. HH.
UPDATE solicitud_paso_aprobacion p
SET estado = 'APROBADO',
    id_usuario_decision = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'cmendoza'),
    fecha_decision = '2026-08-27 16:40:00-05',
    comentario = 'Visto bueno del jefe de área.'
FROM solicitud_permiso sp
JOIN empleado e ON e.id_empleado = sp.id_empleado
JOIN tipo_permiso tp ON tp.id_tipo_permiso = sp.id_tipo_permiso
WHERE p.id_solicitud_permiso = sp.id_solicitud_permiso
  AND e.codigo_empleado = 'AND-004'
  AND tp.codigo = 'SALUD'
  AND p.numero_paso = 1;

UPDATE solicitud_paso_aprobacion p
SET estado = 'APROBADO',
    id_usuario_decision = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'mquispe'),
    fecha_decision = '2026-08-27 17:10:00-05',
    comentario = 'Aprobado con sustento médico.'
FROM solicitud_permiso sp
JOIN empleado e ON e.id_empleado = sp.id_empleado
JOIN tipo_permiso tp ON tp.id_tipo_permiso = sp.id_tipo_permiso
WHERE p.id_solicitud_permiso = sp.id_solicitud_permiso
  AND e.codigo_empleado = 'AND-004'
  AND tp.codigo = 'SALUD'
  AND p.numero_paso = 2;

-- Particular AND-010: lo rechaza su jefa (mquispe)
UPDATE solicitud_paso_aprobacion p
SET estado = 'RECHAZADO',
    id_usuario_decision = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'mquispe'),
    fecha_decision = '2026-09-02 09:20:00-05',
    comentario = 'Rechazado por alta carga operativa del día.'
FROM solicitud_permiso sp
JOIN empleado e ON e.id_empleado = sp.id_empleado
WHERE p.id_solicitud_permiso = sp.id_solicitud_permiso
  AND e.codigo_empleado = 'AND-010'
  AND p.numero_paso = 1;

-- Horas extras aprobadas: jefe + RR. HH.
UPDATE solicitud_paso_aprobacion p
SET estado = 'APROBADO',
    id_usuario_decision = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'cmendoza'),
    fecha_decision = '2026-08-26 09:10:00-05',
    comentario = 'Validado con el registro de asistencia.'
FROM solicitud_hora_extra he
JOIN empleado e ON e.id_empleado = he.id_empleado
WHERE p.id_solicitud_hora_extra = he.id_solicitud_hora_extra
  AND e.codigo_empleado = 'AND-004'
  AND he.fecha = '2026-08-25'
  AND p.numero_paso = 1;

UPDATE solicitud_paso_aprobacion p
SET estado = 'APROBADO',
    id_usuario_decision = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'mquispe'),
    fecha_decision = '2026-08-26 11:00:00-05',
    comentario = 'Validado por RR. HH.'
FROM solicitud_hora_extra he
JOIN empleado e ON e.id_empleado = he.id_empleado
WHERE p.id_solicitud_hora_extra = he.id_solicitud_hora_extra
  AND e.codigo_empleado = 'AND-004'
  AND he.fecha = '2026-08-25'
  AND p.numero_paso = 2;

UPDATE solicitud_paso_aprobacion p
SET estado = 'APROBADO',
    id_usuario_decision = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'cmendoza'),
    fecha_decision = '2026-08-27 08:45:00-05',
    comentario = 'Aprobado por jefatura.'
FROM solicitud_hora_extra he
JOIN empleado e ON e.id_empleado = he.id_empleado
WHERE p.id_solicitud_hora_extra = he.id_solicitud_hora_extra
  AND e.codigo_empleado = 'AND-009'
  AND he.fecha = '2026-08-26'
  AND p.numero_paso = 1;

UPDATE solicitud_paso_aprobacion p
SET estado = 'APROBADO',
    id_usuario_decision = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'mquispe'),
    fecha_decision = '2026-08-27 10:20:00-05',
    comentario = 'Aprobado.'
FROM solicitud_hora_extra he
JOIN empleado e ON e.id_empleado = he.id_empleado
WHERE p.id_solicitud_hora_extra = he.id_solicitud_hora_extra
  AND e.codigo_empleado = 'AND-009'
  AND he.fecha = '2026-08-26'
  AND p.numero_paso = 2;

-- Horas extras AND-005: rechazo en el primer paso
UPDATE solicitud_paso_aprobacion p
SET estado = 'RECHAZADO',
    id_usuario_decision = (SELECT id_usuario FROM usuario WHERE nombre_usuario = 'cmendoza'),
    fecha_decision = '2026-09-02 11:05:00-05',
    comentario = 'Rechazado: el trabajo pudo ejecutarse en jornada regular.'
FROM solicitud_hora_extra he
JOIN empleado e ON e.id_empleado = he.id_empleado
WHERE p.id_solicitud_hora_extra = he.id_solicitud_hora_extra
  AND e.codigo_empleado = 'AND-005'
  AND he.fecha = '2026-09-01'
  AND p.numero_paso = 1;

-- Marcaciones de asistencia (última semana laboral)
INSERT INTO marcacion (id_empleado, tipo, fecha_hora, origen, id_usuario_registro)
SELECT e.id_empleado, v.tipo::tipo_marcacion,
       (v.dia || ' ' || v.hora || '-05')::timestamptz,
       'WEB', u.id_usuario
FROM empleado e
JOIN usuario u ON u.id_empleado = e.id_empleado
CROSS JOIN (VALUES
    ('2026-09-01', 'INGRESO', '08:58:12'),
    ('2026-09-01', 'SALIDA',  '18:07:41'),
    ('2026-09-02', 'INGRESO', '09:04:22'),
    ('2026-09-02', 'SALIDA',  '18:12:09'),
    ('2026-09-03', 'INGRESO', '08:55:03'),
    ('2026-09-03', 'SALIDA',  '18:03:18'),
    ('2026-09-04', 'INGRESO', '09:01:47'),
    ('2026-09-04', 'SALIDA',  '18:09:55')
) AS v(dia, tipo, hora)
WHERE e.estado = 'ACTIVO'
  AND e.codigo_empleado NOT IN ('AND-010');

INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad, detalle)
SELECT id_usuario, 'INICIALIZAR_BD', 'SISTEMA', NULL,
       jsonb_build_object(
           'origen', '02_seed.sql',
           'empresa', 'Consultora Contable Andina S.A.C.',
           'colaboradores', 17
       )
FROM usuario
WHERE nombre_usuario = 'ediaz';
