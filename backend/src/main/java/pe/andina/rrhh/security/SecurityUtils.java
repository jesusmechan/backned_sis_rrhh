package pe.andina.rrhh.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.andina.rrhh.common.ApiException;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UsuarioPrincipal current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw ApiException.unauthorized("Sesión no válida");
        }
        return principal;
    }

    public static boolean hasRole(String codigoRol) {
        return current().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + codigoRol));
    }

    public static boolean isAdminOrRrhh() {
        return hasRole("ADMIN") || hasRole("RRHH");
    }
}
