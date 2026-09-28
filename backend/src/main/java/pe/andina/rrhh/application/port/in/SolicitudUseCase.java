package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.BandejaItem;
import pe.andina.rrhh.application.dto.AppDtos.DecisionRequest;
import pe.andina.rrhh.application.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraRequest;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.application.dto.AppDtos.PasoResponse;
import pe.andina.rrhh.application.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.application.dto.AppDtos.PermisoResponse;

/**
 * Trámites de permisos y horas extras y su circuito de aprobación.
 *
 * <p>La solicitud no guarda al aprobador. Al registrarla, la base de datos asigna la
 * {@code configuracion_aprobacion} del tipo de trámite e instancia sus pasos en
 * {@code solicitud_paso_aprobacion}: el primero queda {@code EN_CURSO} y el resto
 * {@code PENDIENTE}.</p>
 */
public interface SolicitudUseCase {

    /**
     * Registra un permiso del usuario actual (o del colaborador indicado, si es ADMIN o RRHH).
     *
     * @param request tipo, fechas, horas opcionales y motivo
     * @return la solicitud creada en estado {@code PENDIENTE}
     * @throws pe.andina.rrhh.domain.exception.DomainException si las horas son inconsistentes,
     *         se cruza con otro permiso pendiente o aprobado, o no hay saldo de vacaciones
     */
    PermisoResponse crearPermiso(PermisoRequest request);

    /**
     * Registra horas extras respetando los topes diario y semanal de {@code parametro_sistema}.
     *
     * @param request fecha, horario, cantidad de horas y motivo
     * @return la solicitud creada en estado {@code PENDIENTE}
     * @throws pe.andina.rrhh.domain.exception.DomainException si se supera un tope
     */
    HoraExtraResponse crearHoraExtra(HoraExtraRequest request);

    /** @return permisos visibles para el usuario actual según su rol */
    List<PermisoResponse> listarPermisos();

    /**
     * @param id identificador de la solicitud de permiso
     * @return el permiso con su paso actual
     */
    PermisoResponse obtenerPermiso(Integer id);

    /** @return horas extras visibles para el usuario actual según su rol */
    List<HoraExtraResponse> listarHorasExtras();

    /**
     * @param id identificador de la solicitud de horas extras
     * @return la solicitud con su paso actual
     */
    HoraExtraResponse obtenerHoraExtra(Integer id);

    /**
     * Cancela un permiso pendiente; sus pasos abiertos pasan a {@code CANCELADO}.
     *
     * @param id identificador de la solicitud
     * @return el permiso en estado {@code CANCELADO}
     */
    PermisoResponse cancelarPermiso(Integer id);

    /**
     * Cancela horas extras pendientes; sus pasos abiertos pasan a {@code CANCELADO}.
     *
     * @param id identificador de la solicitud
     * @return la solicitud en estado {@code CANCELADO}
     */
    HoraExtraResponse cancelarHoraExtra(Integer id);

    /**
     * Devuelve un paso para decidirlo desde la bandeja.
     *
     * @param idPaso paso en estado {@code EN_CURSO}
     * @return datos del paso y de la solicitud
     * @throws pe.andina.rrhh.domain.exception.DomainException si el paso no está en curso
     *         o no corresponde al usuario (jefe inmediato, rol o usuario asignado)
     */
    BandejaItem pasoPendiente(Integer idPaso);

    /**
     * Aprueba o rechaza el paso en curso. Aprobar activa el siguiente paso o cierra la
     * solicitud como {@code APROBADO}; rechazar la cierra como {@code RECHAZADO} y omite
     * los pasos restantes.
     *
     * @param idPaso  paso en estado {@code EN_CURSO}
     * @param aprobar {@code true} para aprobar, {@code false} para rechazar
     * @param request comentario de la decisión
     * @return el paso decidido
     */
    PasoResponse decidir(Integer idPaso, boolean aprobar, DecisionRequest request);

    /**
     * @param vista {@code SEGUIMIENTO} para las solicitudes en las que el usuario participó;
     *              cualquier otro valor (o {@code null}) para los pasos que debe atender
     * @return elementos de la bandeja
     */
    List<BandejaItem> bandeja(String vista);

    /**
     * @param id identificador de la solicitud de permiso
     * @return acciones registradas en {@code historial_solicitud}, en orden cronológico
     */
    List<HistorialResponse> historialPermiso(Integer id);

    /**
     * @param id identificador de la solicitud de horas extras
     * @return acciones registradas en {@code historial_solicitud}, en orden cronológico
     */
    List<HistorialResponse> historialHoraExtra(Integer id);
}
