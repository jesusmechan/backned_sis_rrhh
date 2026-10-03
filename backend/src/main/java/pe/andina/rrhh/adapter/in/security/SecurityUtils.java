package pe.andina.rrhh.adapter.in.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.andina.rrhh.domain.exception.DomainException;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UsuarioPrincipal current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw DomainException.unauthorized("Sesión no válida");
        }
        return principal;
    }

    public static boolean tienePermiso(String codigoPermiso) {
        return current().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(codigoPermiso));
    }
}
