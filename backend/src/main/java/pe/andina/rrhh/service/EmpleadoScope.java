package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Empleado;
import pe.andina.rrhh.security.SecurityUtils;
import pe.andina.rrhh.security.UsuarioPrincipal;

@Service
public class EmpleadoScope {

    private final EmpleadoService empleadoService;

    public EmpleadoScope(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    public Empleado resolverSolicitante(Integer idEmpleadoRequest) {
        UsuarioPrincipal me = SecurityUtils.current();
        if (SecurityUtils.isAdminOrRrhh() && idEmpleadoRequest != null) {
            return empleadoService.buscar(idEmpleadoRequest);
        }
        if (me.getIdEmpleado() == null) {
            throw ApiException.badRequest("El usuario no está asociado a un trabajador");
        }
        return empleadoService.buscar(me.getIdEmpleado());
    }

    public void assertDuenioORrhh(Integer idEmpleado) {
        if (SecurityUtils.isAdminOrRrhh()) {
            return;
        }
        if (!idEmpleado.equals(SecurityUtils.current().getIdEmpleado())) {
            throw ApiException.forbidden("Solo el solicitante o RR. HH. puede cancelar");
        }
    }

    public void assertPuedeConsultar(Integer idEmpleado, boolean participo) {
        if (SecurityUtils.isAdminOrRrhh() || SecurityUtils.hasRole("APROBADOR")) {
            return;
        }
        if (idEmpleado.equals(SecurityUtils.current().getIdEmpleado()) || participo) {
            return;
        }
        throw ApiException.forbidden("No puede consultar solicitudes de otro trabajador");
    }

    public void assertMismaPersona(Integer idEmpleado) {
        if (SecurityUtils.isAdminOrRrhh()) {
            return;
        }
        if (!idEmpleado.equals(SecurityUtils.current().getIdEmpleado())) {
            throw ApiException.forbidden("No puede consultar la asistencia de otro trabajador");
        }
    }
}
