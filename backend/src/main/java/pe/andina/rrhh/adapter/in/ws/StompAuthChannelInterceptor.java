package pe.andina.rrhh.adapter.in.ws;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import pe.andina.rrhh.adapter.in.security.JwtService;
import pe.andina.rrhh.adapter.in.security.UsuarioDetailsService;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public StompAuthChannelInterceptor(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }
        if (accessor.getUser() != null) {
            return message;
        }
        String header = accessor.getFirstNativeHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }
        if (token == null || !jwtService.isAccessToken(token)) {
            throw new IllegalArgumentException("Token STOMP inválido");
        }
        try {
            String username = jwtService.parse(token).getSubject();
            var details = usuarioDetailsService.loadUserByUsername(username);
            if (!details.isEnabled()) {
                throw new IllegalArgumentException("Usuario inactivo");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo autenticar el WebSocket");
        }
        return message;
    }
}
