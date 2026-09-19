package pe.andina.rrhh.adapter.in.security;

import org.springframework.stereotype.Component;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.domain.model.Usuario;

@Component
public class SpringCurrentUserAdapter implements CurrentUserPort {

    @Override
    public Usuario usuario() {
        return SecurityUtils.current().getUsuario();
    }

    @Override
    public Integer idUsuario() {
        return SecurityUtils.current().getIdUsuario();
    }

    @Override
    public Integer idEmpleado() {
        return SecurityUtils.current().getIdEmpleado();
    }

    @Override
    public String codigoRol() {
        return SecurityUtils.current().getCodigoRol();
    }

    @Override
    public boolean hasRole(String codigoRol) {
        return SecurityUtils.hasRole(codigoRol);
    }

    @Override
    public boolean isAdminOrRrhh() {
        return SecurityUtils.isAdminOrRrhh();
    }

    @Override
    public boolean puedeVerConjuntoOperativo() {
        return SecurityUtils.puedeVerConjuntoOperativo();
    }
}
