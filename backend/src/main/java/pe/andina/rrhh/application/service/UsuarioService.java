package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.UsuarioUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Rol;
import pe.andina.rrhh.domain.model.Usuario;
import pe.andina.rrhh.application.dto.AppDtos.CambioPasswordRequest;
import pe.andina.rrhh.application.dto.AppDtos.UsuarioRequest;
import pe.andina.rrhh.application.dto.AppDtos.UsuarioResponse;
import pe.andina.rrhh.application.port.out.EmpleadoPort;
import pe.andina.rrhh.application.port.out.RolPort;
import pe.andina.rrhh.application.port.out.UsuarioPort;

import java.util.List;

@Service
public class UsuarioService implements UsuarioUseCase {

    private final UsuarioPort usuarioRepository;
    private final RolPort rolRepository;
    private final EmpleadoPort empleadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;

    public UsuarioService(UsuarioPort usuarioRepository,
                          RolPort rolRepository,
                          EmpleadoPort empleadoRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.empleadoRepository = empleadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(DtoMapper::usuario).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Integer id) {
        return DtoMapper.usuario(buscar(id));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse yo() {
        return DtoMapper.usuario(buscar(currentUser.idUsuario()));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw DomainException.badRequest("La contraseña es obligatoria");
        }
        if (usuarioRepository.existsByNombreUsuario(request.nombreUsuario())) {
            throw DomainException.conflict("El nombre de usuario ya existe");
        }
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw DomainException.conflict("El correo ya existe");
        }
        Usuario u = new Usuario();
        aplicar(u, request, true);
        usuarioRepository.save(u);
        auditoriaService.registrar(currentUser.usuario(), "CREAR", "USUARIO", u.getIdUsuario(), u.getNombreUsuario());
        return DtoMapper.usuario(u);
    }

    @Transactional
    public UsuarioResponse actualizar(Integer id, UsuarioRequest request) {
        Usuario u = buscar(id);
        aplicar(u, request, false);
        auditoriaService.registrar(currentUser.usuario(), "ACTUALIZAR", "USUARIO", id, u.getNombreUsuario());
        return DtoMapper.usuario(u);
    }

    @Transactional
    public UsuarioResponse cambiarPassword(CambioPasswordRequest request) {
        Usuario u = buscar(currentUser.idUsuario());
        if (!passwordEncoder.matches(request.actual(), u.getPasswordHash())) {
            throw DomainException.badRequest("La contraseña actual no es correcta");
        }
        if (request.nueva().equals(request.actual())) {
            throw DomainException.badRequest("La nueva contraseña debe ser distinta a la actual");
        }
        u.setPasswordHash(passwordEncoder.encode(request.nueva()));
        auditoriaService.registrar(u, "CAMBIAR_PASSWORD", "USUARIO", u.getIdUsuario(), null);
        return DtoMapper.usuario(u);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Integer id, boolean activo) {
        Usuario u = buscar(id);
        u.setActivo(activo);
        auditoriaService.registrar(currentUser.usuario(), activo ? "ACTIVAR" : "DESACTIVAR", "USUARIO", id, null);
        return DtoMapper.usuario(u);
    }

    public Usuario buscar(Integer id) {
        return usuarioRepository.findById(id).orElseThrow(() -> DomainException.notFound("Usuario no encontrado"));
    }

    private void aplicar(Usuario u, UsuarioRequest r, boolean nuevo) {
        Rol rol = rolRepository.findById(r.idRol()).orElseThrow(() -> DomainException.badRequest("Rol no existe"));
        if (!Boolean.TRUE.equals(rol.getActivo())
                && (u.getRol() == null || !rol.getIdRol().equals(u.getRol().getIdRol()))) {
            throw DomainException.badRequest("El rol está inactivo");
        }
        u.setRol(rol);
        u.setNombreUsuario(r.nombreUsuario());
        u.setCorreo(r.correo());
        if (r.idEmpleado() != null) {
            usuarioRepository.findByEmpleado_IdEmpleado(r.idEmpleado()).ifPresent(existente -> {
                if (u.getIdUsuario() == null || !existente.getIdUsuario().equals(u.getIdUsuario())) {
                    throw DomainException.conflict("Ese colaborador ya tiene una cuenta");
                }
            });
            u.setEmpleado(empleadoRepository.findById(r.idEmpleado())
                    .orElseThrow(() -> DomainException.badRequest("Trabajador no existe")));
        } else {
            u.setEmpleado(null);
        }
        if (r.password() != null && !r.password().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(r.password()));
        } else if (nuevo) {
            throw DomainException.badRequest("La contraseña es obligatoria");
        }
        if (r.activo() != null) {
            u.setActivo(r.activo());
        }
    }
}
