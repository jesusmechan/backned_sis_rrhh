package pe.andina.rrhh.service.validacion;

import org.springframework.stereotype.Component;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Empleado;
import pe.andina.rrhh.domain.SolicitudPermiso;
import pe.andina.rrhh.domain.TipoPermiso;
import pe.andina.rrhh.domain.enums.EstadoSolicitud;
import pe.andina.rrhh.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.repo.SolicitudPermisoRepository;
import pe.andina.rrhh.service.ContratoService;

import java.util.EnumSet;

@Component
public class PermisoReglas {

    private final SolicitudPermisoRepository permisoRepository;
    private final ContratoService contratoService;

    public PermisoReglas(SolicitudPermisoRepository permisoRepository, ContratoService contratoService) {
        this.permisoRepository = permisoRepository;
        this.contratoService = contratoService;
    }

    public void validarAlCrear(Empleado empleado, TipoPermiso tipo, PermisoRequest request) {
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
                throw ApiException.conflict("Ya existe un permiso pendiente o aprobado que se cruza con esas fechas.");
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
