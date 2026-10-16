-- Copia requiere_adjunto al instanciar el circuito. Se ejecuta entero (bloques $$).

CREATE OR REPLACE FUNCTION fn_instanciar_pasos_aprobacion()
RETURNS TRIGGER AS $$
DECLARE
    v_detalle RECORD;
    v_id_usuario INTEGER;
    v_estado estado_paso_aprobacion;
    v_id_empleado INTEGER;
    v_id_config INTEGER;
    v_id_permiso INTEGER;
    v_id_hextra INTEGER;
    v_motivo VARCHAR(400);
    v_hay_en_curso BOOLEAN := FALSE;
    v_id_usuario_solicitante INTEGER;
BEGIN
    v_id_empleado := NEW.id_empleado;
    v_id_config := NEW.id_configuracion;
    v_motivo := NEW.motivo;

    IF TG_TABLE_NAME = 'solicitud_permiso' THEN
        v_id_permiso := NEW.id_solicitud_permiso;
    ELSE
        v_id_hextra := NEW.id_solicitud_hora_extra;
    END IF;

    SELECT id_usuario INTO v_id_usuario_solicitante
    FROM usuario
    WHERE id_empleado = v_id_empleado
    LIMIT 1;

    IF NOT EXISTS (
        SELECT 1
        FROM configuracion_aprobacion_detalle
        WHERE id_configuracion = v_id_config
          AND activo
    ) THEN
        RAISE EXCEPTION 'La configuración % no tiene pasos de aprobación activos.', v_id_config;
    END IF;

    FOR v_detalle IN
        SELECT *
        FROM configuracion_aprobacion_detalle
        WHERE id_configuracion = v_id_config
          AND activo
        ORDER BY numero_paso
    LOOP
        v_id_usuario := fn_resolver_aprobador(
            v_detalle.tipo_aprobador,
            v_detalle.id_rol,
            v_detalle.id_usuario,
            v_id_empleado
        );

        IF v_detalle.tipo_aprobador <> 'ROL' AND v_id_usuario IS NULL THEN
            IF v_detalle.es_obligatorio THEN
                RAISE EXCEPTION 'No se pudo resolver el aprobador del paso "%" (%).',
                    v_detalle.nombre_paso, v_detalle.tipo_aprobador;
            END IF;
            v_estado := 'OMITIDO';
        ELSIF NOT v_hay_en_curso THEN
            v_estado := 'EN_CURSO';
            v_hay_en_curso := TRUE;
        ELSE
            v_estado := 'PENDIENTE';
        END IF;

        INSERT INTO solicitud_paso_aprobacion (
            id_solicitud_permiso,
            id_solicitud_hora_extra,
            id_configuracion,
            id_detalle,
            numero_paso,
            nombre_paso,
            tipo_aprobador,
            id_rol,
            id_usuario_asignado,
            estado,
            fecha_inicio,
            requiere_adjunto
        ) VALUES (
            v_id_permiso,
            v_id_hextra,
            v_id_config,
            v_detalle.id_detalle,
            v_detalle.numero_paso,
            v_detalle.nombre_paso,
            v_detalle.tipo_aprobador,
            v_detalle.id_rol,
            v_id_usuario,
            v_estado,
            CASE WHEN v_estado = 'EN_CURSO' THEN NOW() END,
            COALESCE(v_detalle.requiere_adjunto, FALSE)
        );
    END LOOP;

    IF v_id_usuario_solicitante IS NOT NULL THEN
        INSERT INTO historial_solicitud (
            id_solicitud_permiso,
            id_solicitud_hora_extra,
            id_usuario,
            accion,
            estado_anterior,
            estado_nuevo,
            comentario
        ) VALUES (
            v_id_permiso,
            v_id_hextra,
            v_id_usuario_solicitante,
            'REGISTRAR',
            NULL,
            'PENDIENTE'::estado_solicitud,
            v_motivo
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
