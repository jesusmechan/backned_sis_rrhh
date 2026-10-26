package pe.andina.rrhh.application.trace;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Registra entrada/salida de casos de uso para seguir el flujo interno de cada request.
 */
@Aspect
@Component
public class ServiceTraceAspect {

    private static final Logger log = LoggerFactory.getLogger("pe.andina.rrhh.trace.service");

    @Around("within(pe.andina.rrhh.application.service.*Service) "
            + "&& execution(public * *(..)) "
            + "&& !within(pe.andina.rrhh.application.service.ParametroSistemaService) "
            + "&& !within(pe.andina.rrhh.application.service.AuditoriaService)")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!log.isDebugEnabled() && !log.isInfoEnabled()) {
            return joinPoint.proceed();
        }
        String type = AopUtils.getTargetClass(joinPoint.getTarget()).getSimpleName();
        String method = joinPoint.getSignature().getName();
        String args = summarizeArgs(method, joinPoint.getArgs());
        long started = System.nanoTime();
        log.info("svc {}.{}({})", type, method, args);
        try {
            Object result = joinPoint.proceed();
            log.info("svc {}.{} ok {}ms", type, method, elapsedMs(started));
            return result;
        } catch (Throwable ex) {
            log.warn("svc {}.{} error {} {}ms", type, method, ex.getClass().getSimpleName(), elapsedMs(started));
            throw ex;
        }
    }

    private static long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }

    private static String summarizeArgs(String method, Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        boolean sensitiveMethod = isSensitiveMethod(method);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(summarize(args[i], sensitiveMethod));
        }
        return out.toString();
    }

    private static boolean isSensitiveMethod(String method) {
        String name = method.toLowerCase();
        return name.contains("password") || name.contains("token")
                || name.contains("login") || name.contains("refresh") || name.contains("logout");
    }

    private static String summarize(Object arg, boolean sensitiveMethod) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof CharSequence text) {
            if (sensitiveMethod || looksLikeSecret(text)) {
                return "***";
            }
            return text.length() > 80 ? "\"" + text.subSequence(0, 80) + "…\"" : "\"" + text + "\"";
        }
        String name = arg.getClass().getSimpleName();
        if (name.contains("Password") || name.contains("Login") || name.contains("Refresh")
                || name.contains("Token") || name.contains("Auth")) {
            return name + "[redacted]";
        }
        if (arg instanceof byte[] bytes) {
            return "byte[" + bytes.length + "]";
        }
        if (arg instanceof MultipartFile file) {
            return "file=" + file.getOriginalFilename();
        }
        if (arg instanceof Collection<?> collection) {
            return name + "[size=" + collection.size() + "]";
        }
        if (arg instanceof Map<?, ?> map) {
            return name + "[size=" + map.size() + "]";
        }
        if (arg instanceof Number || arg instanceof Boolean || arg instanceof UUID || arg instanceof Enum<?>) {
            return String.valueOf(arg);
        }
        if (name.endsWith("Request") || name.endsWith("Response")) {
            return name;
        }
        String text = String.valueOf(arg);
        return text.length() > 80 ? name : text;
    }

    private static boolean looksLikeSecret(CharSequence text) {
        int dots = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '.') {
                dots++;
            }
        }
        return dots == 2 && text.length() > 40;
    }
}
