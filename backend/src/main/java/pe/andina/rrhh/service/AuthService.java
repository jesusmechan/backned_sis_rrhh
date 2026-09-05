package pe.andina.rrhh.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.RefreshToken;
import pe.andina.rrhh.domain.Usuario;
import pe.andina.rrhh.dto.AuthDtos.AuthResponse;
import pe.andina.rrhh.dto.AuthDtos.LoginRequest;
import pe.andina.rrhh.dto.AuthDtos.UsuarioResumen;
import pe.andina.rrhh.repo.RefreshTokenRepository;
import pe.andina.rrhh.repo.UsuarioRepository;
import pe.andina.rrhh.security.JwtService;
import pe.andina.rrhh.security.UsuarioPrincipal;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       AuditoriaService auditoriaService,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(request.nombreUsuario())
                .orElseThrow(() -> ApiException.unauthorized("Usuario o contraseña incorrectos"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw ApiException.unauthorized("El usuario está inactivo");
        }
        if (!passwordMatches(request.password(), usuario.getPasswordHash())) {
            throw ApiException.unauthorized("Usuario o contraseña incorrectos");
        }
        usuario.getRol().getCodigo();
        if (usuario.getEmpleado() != null) {
            usuario.getEmpleado().nombreCompleto();
        }
        usuario.setUltimoAcceso(OffsetDateTime.now());
        auditoriaService.registrar(usuario, "LOGIN", "USUARIO", usuario.getIdUsuario(), null);
        return emitirTokens(usuario);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByTokenAndRevocadoFalse(refreshTokenValue)
                .orElseThrow(() -> ApiException.unauthorized("Refresh token inválido"));
        if (stored.getFechaExpiracion().isBefore(OffsetDateTime.now())) {
            stored.setRevocado(true);
            throw ApiException.unauthorized("Refresh token expirado");
        }
        stored.setRevocado(true);
        Usuario usuario = stored.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw ApiException.unauthorized("El usuario está inactivo");
        }
        return emitirTokens(usuario);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenAndRevocadoFalse(refreshTokenValue)
                .ifPresent(token -> token.setRevocado(true));
    }

    @Transactional
    public void logoutAll(UsuarioPrincipal principal) {
        refreshTokenRepository.findByUsuario_IdUsuarioAndRevocadoFalse(principal.getIdUsuario())
                .forEach(token -> token.setRevocado(true));
    }

    private AuthResponse emitirTokens(Usuario usuario) {
        String access = jwtService.generateAccessToken(
                usuario.getIdUsuario(), usuario.getNombreUsuario(), usuario.getRol().getCodigo());
        RefreshToken refresh = new RefreshToken();
        refresh.setUsuario(usuario);
        refresh.setToken(UUID.randomUUID() + "-" + UUID.randomUUID());
        refresh.setFechaExpiracion(OffsetDateTime.now().plusSeconds(jwtService.getRefreshExpirationMs() / 1000));
        refresh.setRevocado(false);
        refreshTokenRepository.save(refresh);
        String nombre = usuario.getEmpleado() != null ? usuario.getEmpleado().nombreCompleto() : usuario.getNombreUsuario();
        Integer idEmp = usuario.getEmpleado() != null ? usuario.getEmpleado().getIdEmpleado() : null;
        return new AuthResponse(
                access,
                refresh.getToken(),
                "Bearer",
                jwtService.getAccessExpirationMs() / 1000,
                new UsuarioResumen(
                        usuario.getIdUsuario(),
                        usuario.getNombreUsuario(),
                        usuario.getCorreo(),
                        usuario.getRol().getCodigo(),
                        idEmp,
                        nombre
                )
        );
    }

    private boolean passwordMatches(String raw, String hash) {
        try {
            return passwordEncoder.matches(raw, hash);
        } catch (Exception ex) {
            return false;
        }
    }
}
