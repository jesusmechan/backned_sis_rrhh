package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.EmpleadoUseCase;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;

@Service
public class EmpleadoScope {

    private final EmpleadoUseCase empleadoService;

    private final CurrentUserPort currentUser;

    public EmpleadoScope(EmpleadoUseCase empleadoService,
                           CurrentUserPort currentUser) {
        this.empleadoService = empleadoService;
        this.currentUser = currentUser;
    }

    public Empleado resolverSolicitante(Integer idEmpleadoRequest) {
        if (currentUser.isAdminOrRrhh() && idEmpleadoRequest != null) {
            return empleadoService.buscar(idEmpleadoRequest);
        }
        if (currentUser.idEmpleado() == null) {
            throw DomainException.badRequest("El usuario no está asociado a un trabajador");
        }
        return empleadoService.buscar(currentUser.idEmpleado());
    }

    public void assertDuenioORrhh(Integer idEmpleado) {
        if (currentUser.isAdminOrRrhh()) {
            return;
        }
        if (!idEmpleado.equals(currentUser.idEmpleado())) {
            throw DomainException.forbidden("Solo el solicitante o RR. HH. puede cancelar");
        }
    }

    public void assertPuedeConsultar(Integer idEmpleado, boolean participo) {
        if (currentUser.isAdminOrRrhh() || currentUser.hasRole("JEFE") || currentUser.hasRole("GERENCIA")) {
            return;
        }
        if (idEmpleado.equals(currentUser.idEmpleado()) || participo) {
            return;
        }
        throw DomainException.forbidden("No puede consultar solicitudes de otro trabajador");
    }

    public void assertMismaPersona(Integer idEmpleado) {
        if (currentUser.isAdminOrRrhh()) {
            return;
        }
        if (!idEmpleado.equals(currentUser.idEmpleado())) {
            throw DomainException.forbidden("No puede consultar la asistencia de otro trabajador");
        }
    }

    /** Admin/RRHH/Gerencia: cualquiera. Jefe: solo su equipo directo. */
    public void assertPuedeGestionarEquipo(Empleado subordinado) {
        if (currentUser.isAdminOrRrhh() || currentUser.hasRole("GERENCIA")) {
            return;
        }
        if (currentUser.hasRole("JEFE") && esJefeDirectoDe(subordinado)) {
            return;
        }
        throw DomainException.forbidden("Solo puede gestionar a su equipo directo");
    }

    public boolean esJefeDirectoDe(Empleado subordinado) {
        Integer jefeId = currentUser.idEmpleado();
        return jefeId != null
                && subordinado.getJefeInmediato() != null
                && jefeId.equals(subordinado.getJefeInmediato().getIdEmpleado());
    }
}
