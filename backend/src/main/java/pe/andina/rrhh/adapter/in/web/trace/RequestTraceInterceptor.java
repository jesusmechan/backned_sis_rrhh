package pe.andina.rrhh.adapter.in.web.trace;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Completa el MDC con el endpoint concreto (Controller.metodo) y el usuario autenticado.
 */
public class RequestTraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method) {
            RequestTrace.putHandler(method.getBeanType().getSimpleName() + "." + method.getMethod().getName());
        }
        RequestTrace.putUserFromSecurity();
        return true;
    }
}
