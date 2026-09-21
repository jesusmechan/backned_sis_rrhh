package pe.andina.rrhh.adapter.in.ws;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import pe.andina.rrhh.adapter.in.security.JwtService;
import pe.andina.rrhh.adapter.in.security.UsuarioDetailsService;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ATTR = "WS_USER";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtHandshakeInterceptor(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extraerToken(request);
        if (token == null || !jwtService.isAccessToken(token)) {
            return true;
        }
        try {
            String username = jwtService.parse(token).getSubject();
            var details = usuarioDetailsService.loadUserByUsername(username);
            if (details.isEnabled()) {
                attributes.put(USER_ATTR, username);
            }
        } catch (Exception ignored) {
            /* el canal STOMP vuelve a validar en CONNECT */
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        /* no-op */
    }

    private String extraerToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servlet) {
            String query = servlet.getServletRequest().getParameter("access_token");
            if (query != null && !query.isBlank()) {
                return query;
            }
        }
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
