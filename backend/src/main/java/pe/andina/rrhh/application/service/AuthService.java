package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import pe.andina.rrhh.application.port.in.SesionUseCase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.AuthUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.RefreshToken;
import pe.andina.rrhh.domain.model.Usuario;
import pe.andina.rrhh.application.dto.AuthDtos.AuthResponse;
import pe.andina.rrhh.application.dto.AuthDtos.LoginRequest;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.out.RefreshTokenPort;
import pe.andina.rrhh.application.port.out.TokenPort;
import pe.andina.rrhh.application.port.out.UsuarioPort;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private final UsuarioPort usuarioRepository;
    private final RefreshTokenPort refreshTokenRepository;
    private final TokenPort tokenPort;
    private final AuditoriaUseCase auditoriaService;
    private final PasswordEncoder passwordEncoder;
    private final SesionUseCase sesionService;
    private final CurrentUserPort currentUser;

    public AuthService(UsuarioPort usuarioRepository,
                       RefreshTokenPort refreshTokenRepository,
                       TokenPort tokenPort,
                       AuditoriaUseCase auditoriaService,
                       PasswordEncoder passwordEncoder,
                       SesionUseCase sesionService,
                       CurrentUserPort currentUser) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenPort = tokenPort;
        this.auditoriaService = auditoriaService;
        this.passwordEncoder = passwordEncoder;
        this.sesionService = sesionService;
        this.currentUser = currentUser;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(request.nombreUsuario())
                .orElseThrow(() -> DomainException.unauthorized("Usuario o contraseña incorrectos"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw DomainException.unauthorized("El usuario está inactivo");
        }
        if (!passwordMatches(request.password(), usuario.getPasswordHash())) {
            throw DomainException.unauthorized("Usuario o contraseña incorrectos");
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
                .orElseThrow(() -> DomainException.unauthorized("Refresh token inválido"));
        if (stored.getFechaExpiracion().isBefore(OffsetDateTime.now())) {
            stored.setRevocado(true);
            throw DomainException.unauthorized("Refresh token expirado");
        }
        stored.setRevocado(true);
        Usuario usuario = stored.getUsuario();
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw DomainException.unauthorized("El usuario está inactivo");
        }
        return emitirTokens(usuario);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenAndRevocadoFalse(refreshTokenValue)
                .ifPresent(token -> token.setRevocado(true));
    }

    @Transactional
    public void logoutAll() {
        refreshTokenRepository.findByUsuario_IdUsuarioAndRevocadoFalse(currentUser.idUsuario())
                .forEach(token -> token.setRevocado(true));
    }

    private AuthResponse emitirTokens(Usuario usuario) {
        String access = tokenPort.generateAccessToken(
                usuario.getIdUsuario(), usuario.getNombreUsuario(), usuario.getRol().getCodigo());
        RefreshToken refresh = new RefreshToken();
        refresh.setUsuario(usuario);
        refresh.setToken(UUID.randomUUID() + "-" + UUID.randomUUID());
        refresh.setFechaExpiracion(OffsetDateTime.now().plusSeconds(tokenPort.getRefreshExpirationMs() / 1000));
        refresh.setRevocado(false);
        refreshTokenRepository.save(refresh);
        return new AuthResponse(
                access,
                refresh.getToken(),
                "Bearer",
                tokenPort.getAccessExpirationMs() / 1000,
                sesionService.desde(usuario)
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
