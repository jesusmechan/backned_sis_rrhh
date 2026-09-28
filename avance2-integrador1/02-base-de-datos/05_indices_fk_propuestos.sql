-- =============================================================================
-- Índices propuestos para claves foráneas sin índice — rrhh_andina
-- Resultado del ejemplo 11 de 04_ejemplos_errores_comunes.sql.
--
-- Solo se indexan las FK de tablas que crecen cada mes o que se consultan desde
-- el padre (detalle de planilla, líneas de asiento, historial, postulantes).
-- Se dejan sin índice las FK hacia catálogos pequeños (cargo, horario_laboral,
-- tipo_permiso, rol en tablas de configuración): el costo de mantener el índice
-- supera el beneficio con decenas de filas.
--
-- Propuesta: no se ha ejecutado. Es idempotente (IF NOT EXISTS).
-- =============================================================================

-- Planilla: abrir una planilla lista sus boletas; la boleta de un empleado por periodo.
CREATE INDEX IF NOT EXISTS ix_planilla_detalle_planilla ON planilla_detalle (id_planilla);
CREATE INDEX IF NOT EXISTS ix_planilla_detalle_empleado ON planilla_detalle (id_empleado);

-- Contabilidad: asiento de una planilla y sus líneas.
CREATE INDEX IF NOT EXISTS ix_asiento_planilla     ON asiento_contable (id_planilla);
CREATE INDEX IF NOT EXISTS ix_asiento_linea_asiento ON asiento_linea (id_asiento);

-- Trazabilidad: historial por paso y acciones de un usuario.
CREATE INDEX IF NOT EXISTS ix_historial_paso    ON historial_solicitud (id_paso_solicitud);
CREATE INDEX IF NOT EXISTS ix_historial_usuario ON historial_solicitud (id_usuario, fecha_hora DESC);

-- Aprobaciones: decisiones tomadas por un usuario (reportes de gestión).
CREATE INDEX IF NOT EXISTS ix_paso_usuario_decision ON solicitud_paso_aprobacion (id_usuario_decision);

-- Desempeño y reclutamiento.
CREATE INDEX IF NOT EXISTS ix_evaluacion_empleado    ON evaluacion_desempeno (id_empleado, fecha DESC);
CREATE INDEX IF NOT EXISTS ix_postulacion_convocatoria ON postulacion (id_convocatoria, estado);

-- Auditoría de exportaciones.
CREATE INDEX IF NOT EXISTS ix_reporte_generado_usuario ON reporte_generado (id_usuario, fecha_generacion DESC);
