-- =============================================================================
-- Errores comunes en el diseño de bases de datos — demostración sobre rrhh_andina
-- Avance 2, semana 6: "Identificar errores comunes en el diseño de base de datos
-- mediante ejemplos".
--
-- Cada bloque muestra el error (MAL) y cómo lo evita el esquema del proyecto (BIEN).
-- Todo corre dentro de una transacción que termina en ROLLBACK: no deja cambios.
-- Ejecutar sobre rrhh_andina con 01_install.sql ya aplicado. Los resultados salen
-- como NOTICE (pestaña "Mensajes" en pgAdmin / DBeaver) y como filas de consulta.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Guardar dinero en FLOAT / REAL
-- -----------------------------------------------------------------------------
-- MAL: los flotantes no representan exactamente 0.10; al acumular aparece el error.
-- BIEN: NUMERIC(12,2), como en contrato.remuneracion_basica y planilla_detalle.*
SELECT 'Ej. 1: dinero'                                   AS ejemplo,
       (0.1::float8 + 0.2::float8) = 0.3::float8          AS float_suma_exacta,
       (0.1::numeric + 0.2::numeric) = 0.3::numeric       AS numeric_suma_exacta,
       (SELECT sum(0.10::real)    FROM generate_series(1, 1000))::text AS mil_montos_de_010_real,
       (SELECT sum(0.10::numeric) FROM generate_series(1, 1000))::text AS mil_montos_de_010_numeric;

-- -----------------------------------------------------------------------------
-- 2. Fechas sin zona horaria
-- -----------------------------------------------------------------------------
-- Una salida marcada a las 20:30 en Lima es 01:30 del día siguiente en UTC.
-- MAL: tomar la fecha "cruda" en un servidor configurado en UTC -> día equivocado.
-- BIEN: TIMESTAMPTZ + fecha calculada en America/Lima (marcacion.fecha es GENERATED).
SET LOCAL TimeZone = 'UTC';
SELECT 'Ej. 2: zona horaria'                                                   AS ejemplo,
       ('2026-09-01 20:30-05'::timestamptz)::date                               AS fecha_en_servidor_utc,
       (('2026-09-01 20:30-05'::timestamptz) AT TIME ZONE 'America/Lima')::date AS fecha_en_lima;
RESET TimeZone;

-- -----------------------------------------------------------------------------
-- 3. Estados como texto libre
-- -----------------------------------------------------------------------------
-- MAL: con VARCHAR conviven variantes y los reportes cuentan mal.
CREATE TEMP TABLE demo_estado_texto (estado VARCHAR(20)) ON COMMIT DROP;
INSERT INTO demo_estado_texto VALUES ('APROBADO'), ('Aprobado'), ('aprobada'), ('APROBADO ');
SELECT 'Ej. 3: texto libre' AS ejemplo, estado, count(*) AS cantidad
FROM demo_estado_texto GROUP BY estado ORDER BY estado;

-- BIEN: el tipo ENUM estado_solicitud rechaza cualquier valor fuera del dominio.
DO $$
BEGIN
    PERFORM 'aprobada'::estado_solicitud;
    RAISE NOTICE 'Ej. 3: el ENUM aceptó un valor inválido (no debería)';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Ej. 3 BIEN: ENUM rechaza ''aprobada'' -> % (%)', SQLERRM, SQLSTATE;
END $$;

-- -----------------------------------------------------------------------------
-- 4. Validar solo en el frontend
-- -----------------------------------------------------------------------------
-- Si alguien llama a la API o a la base directamente, la regla debe seguir viva.
-- BIEN: CHECK ck_empleado_dni exige 8 dígitos cuando el documento es DNI.
DO $$
BEGIN
    INSERT INTO empleado (codigo_empleado, numero_documento, nombres, apellido_paterno,
                          apellido_materno, correo_institucional, fecha_ingreso,
                          id_area, id_cargo, id_horario)
    VALUES ('DEMO-01', '1234', 'Prueba', 'Demo', 'Demo', 'demo.error@andina.pe', CURRENT_DATE,
            (SELECT min(id_area) FROM area), (SELECT min(id_cargo) FROM cargo),
            (SELECT min(id_horario) FROM horario_laboral));
    RAISE NOTICE 'Ej. 4: se insertó un DNI de 4 dígitos (no debería)';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Ej. 4 BIEN: DNI inválido rechazado -> % (%)', SQLERRM, SQLSTATE;
END $$;

-- -----------------------------------------------------------------------------
-- 5. Autorrelación sin control (un empleado jefe de sí mismo)
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    UPDATE empleado SET id_jefe_inmediato = id_empleado
    WHERE id_empleado = (SELECT min(id_empleado) FROM empleado);
    RAISE NOTICE 'Ej. 5: un empleado quedó como su propio jefe (no debería)';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Ej. 5 BIEN: ck_empleado_no_autojefe -> % (%)', SQLERRM, SQLSTATE;
END $$;

-- -----------------------------------------------------------------------------
-- 6. Periodo de planilla duplicado (doble pago)
-- -----------------------------------------------------------------------------
-- BIEN: uq_planilla_periodo (anio, mes).
DO $$
BEGIN
    INSERT INTO planilla (anio, mes) VALUES (2099, 1);
    INSERT INTO planilla (anio, mes) VALUES (2099, 1);
    RAISE NOTICE 'Ej. 6: se crearon dos planillas del mismo mes (no debería)';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Ej. 6 BIEN: periodo duplicado rechazado -> % (%)', SQLERRM, SQLSTATE;
END $$;

-- -----------------------------------------------------------------------------
-- 7. Dos contratos vigentes para la misma persona
-- -----------------------------------------------------------------------------
-- MAL: un UNIQUE (id_empleado) impediría guardar el historial de contratos.
-- BIEN: índice único PARCIAL uq_contrato_empleado_vigente (solo filas VIGENTE).
DO $$
DECLARE
    v_emp INTEGER;
BEGIN
    SELECT id_empleado INTO v_emp FROM contrato WHERE estado = 'VIGENTE' LIMIT 1;
    IF v_emp IS NULL THEN
        RAISE NOTICE 'Ej. 7: no hay contratos vigentes para la demostración';
        RETURN;
    END IF;
    INSERT INTO contrato (codigo, id_empleado, modalidad, id_horario, fecha_inicio,
                          remuneracion_basica, estado)
    SELECT 'DEMO-CT-01', c.id_empleado, c.modalidad, c.id_horario, CURRENT_DATE,
           c.remuneracion_basica, 'VIGENTE'
    FROM contrato c WHERE c.id_empleado = v_emp AND c.estado = 'VIGENTE';
    RAISE NOTICE 'Ej. 7: el empleado % quedó con dos contratos vigentes (no debería)', v_emp;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Ej. 7 BIEN: segundo contrato vigente rechazado -> % (%)', SQLERRM, SQLSTATE;
END $$;

-- -----------------------------------------------------------------------------
-- 8. Arco exclusivo sin control
-- -----------------------------------------------------------------------------
-- Un paso de aprobación pertenece a un permiso O a una hora extra, nunca a ninguno
-- ni a ambos. BIEN: CHECK ck_paso_una_solicitud.
DO $$
BEGIN
    INSERT INTO solicitud_paso_aprobacion (id_configuracion, id_detalle, numero_paso,
                                           nombre_paso, tipo_aprobador)
    SELECT d.id_configuracion, d.id_detalle, d.numero_paso, d.nombre_paso, d.tipo_aprobador
    FROM configuracion_aprobacion_detalle d LIMIT 1;
    RAISE NOTICE 'Ej. 8: se creó un paso sin solicitud (no debería)';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Ej. 8 BIEN: paso huérfano rechazado -> % (%)', SQLERRM, SQLSTATE;
END $$;

-- -----------------------------------------------------------------------------
-- 9. Inyección SQL por concatenar texto
-- -----------------------------------------------------------------------------
-- MAL: armar la consulta pegando lo que escribe el usuario.
-- BIEN: parámetros (EXECUTE ... USING en PL/pgSQL; JPA / PreparedStatement en la API).
DO $$
DECLARE
    v_entrada  TEXT := 'x'' OR ''1''=''1';
    v_mal      BIGINT;
    v_bien     BIGINT;
BEGIN
    EXECUTE 'SELECT count(*) FROM usuario WHERE nombre_usuario = ''' || v_entrada || ''''
        INTO v_mal;
    EXECUTE 'SELECT count(*) FROM usuario WHERE nombre_usuario = $1'
        INTO v_bien USING v_entrada;
    RAISE NOTICE 'Ej. 9: entrada [%] -> concatenando devuelve % usuarios (MAL); con parámetro devuelve % (BIEN)',
        v_entrada, v_mal, v_bien;
END $$;

-- -----------------------------------------------------------------------------
-- 10. Contraseñas en texto plano
-- -----------------------------------------------------------------------------
-- BIEN: usuario.password_hash guarda BCrypt ($2a$ / $2b$ / $2y$). Debe dar 0.
SELECT 'Ej. 10: contraseñas' AS ejemplo,
       count(*) FILTER (WHERE password_hash !~ '^\$2[aby]\$\d{2}\$') AS sin_bcrypt,
       count(*)                                                    AS total_usuarios
FROM usuario;

-- -----------------------------------------------------------------------------
-- 11. Claves foráneas sin índice (revisión del propio esquema)
-- -----------------------------------------------------------------------------
-- Sin índice, los JOIN y los DELETE del padre recorren toda la tabla hija.
-- Lista las FK de una columna que no son la primera columna de ningún índice.
-- En tablas pequeñas (catálogos) es aceptable; para las que crecen ver 05_indices_fk_propuestos.sql.
SELECT 'Ej. 11: FK sin índice'   AS ejemplo,
       c.conrelid::regclass      AS tabla,
       a.attname                 AS columna,
       c.confrelid::regclass     AS referencia
FROM pg_constraint c
JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
WHERE c.contype = 'f'
  AND array_length(c.conkey, 1) = 1
  AND c.connamespace = 'public'::regnamespace
  AND NOT EXISTS (
        SELECT 1 FROM pg_index i
        WHERE i.indrelid = c.conrelid AND i.indkey[0] = c.conkey[1])
ORDER BY 2, 3;

ROLLBACK;
