package pe.andina.rrhh.domain.service;

import org.springframework.stereotype.Component;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import pe.andina.rrhh.domain.model.TipoPermiso;
import pe.andina.rrhh.domain.model.enums.EstadoSolicitud;
import pe.andina.rrhh.application.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.application.port.out.SolicitudPermisoPort;
import pe.andina.rrhh.application.port.in.ContratoUseCase;

import java.util.EnumSet;

@Component
public class PermisoReglas {

    private final SolicitudPermisoPort permisoRepository;
    private final ContratoUseCase contratoService;

    public PermisoReglas(SolicitudPermisoPort permisoRepository, ContratoUseCase contratoService) {
        this.permisoRepository = permisoRepository;
        this.contratoService = contratoService;
    }

    public void validarAlCrear(Empleado empleado, TipoPermiso tipo, PermisoRequest request) {
        if ((request.horaInicio() == null) != (request.horaFin() == null)) {
            throw DomainException.badRequest("Indique hora de inicio y de fin, o deje ambas vacías.");
        }
        if (request.horaInicio() != null && !request.horaFin().isAfter(request.horaInicio())) {
            throw DomainException.badRequest(
                    "La hora de fin debe ser posterior a la de inicio. Para un día completo, deje las horas vacías.");
        }
        if ("VACACIONES".equalsIgnoreCase(tipo.getCodigo())) {
            contratoService.validarVacaciones(empleado, request.fechaInicio(), request.fechaFin());
        }
        validarTraslapo(empleado.getIdEmpleado(), request);
    }

    private void validarTraslapo(Integer idEmpleado, PermisoRequest request) {
        var vigentes = EnumSet.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.APROBADO);
        for (SolicitudPermiso existente : permisoRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudPermisoDesc(idEmpleado)) {
            if (!vigentes.contains(existente.getEstado())) {
                continue;
            }
            if (existente.getFechaFin().isBefore(request.fechaInicio()) || existente.getFechaInicio().isAfter(request.fechaFin())) {
                continue;
            }
            if (traslapaHorario(existente, request)) {
                throw DomainException.conflict("Ya existe un permiso pendiente o aprobado que se cruza con esas fechas.");
            }
        }
    }

    private boolean traslapaHorario(SolicitudPermiso existente, PermisoRequest request) {
        if (existente.getHoraInicio() == null || existente.getHoraFin() == null
                || request.horaInicio() == null || request.horaFin() == null) {
            return true;
        }
        boolean mismoDiaUnico = existente.getFechaInicio().equals(existente.getFechaFin())
                && request.fechaInicio().equals(request.fechaFin())
                && existente.getFechaInicio().equals(request.fechaInicio());
        if (!mismoDiaUnico) {
            return true;
        }
        return request.horaInicio().isBefore(existente.getHoraFin())
                && existente.getHoraInicio().isBefore(request.horaFin());
    }
}
