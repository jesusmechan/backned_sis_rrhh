package pe.andina.rrhh.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Usuario;
import pe.andina.rrhh.dto.AppDtos.UsuarioRequest;
import pe.andina.rrhh.dto.AppDtos.UsuarioResponse;
import pe.andina.rrhh.repo.EmpleadoRepository;
import pe.andina.rrhh.repo.RolRepository;
import pe.andina.rrhh.repo.UsuarioRepository;
import pe.andina.rrhh.security.SecurityUtils;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          EmpleadoRepository empleadoRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.empleadoRepository = empleadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
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
        return DtoMapper.usuario(buscar(SecurityUtils.current().getIdUsuario()));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw ApiException.badRequest("La contraseña es obligatoria");
        }
        if (usuarioRepository.existsByNombreUsuario(request.nombreUsuario())) {
            throw ApiException.conflict("El nombre de usuario ya existe");
        }
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw ApiException.conflict("El correo ya existe");
        }
        Usuario u = new Usuario();
        aplicar(u, request, true);
        usuarioRepository.save(u);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "CREAR", "USUARIO", u.getIdUsuario(), u.getNombreUsuario());
        return DtoMapper.usuario(u);
    }

    @Transactional
    public UsuarioResponse actualizar(Integer id, UsuarioRequest request) {
        Usuario u = buscar(id);
        aplicar(u, request, false);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "ACTUALIZAR", "USUARIO", id, u.getNombreUsuario());
        return DtoMapper.usuario(u);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Integer id, boolean activo) {
        Usuario u = buscar(id);
        u.setActivo(activo);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), activo ? "ACTIVAR" : "DESACTIVAR", "USUARIO", id, null);
        return DtoMapper.usuario(u);
    }

    public Usuario buscar(Integer id) {
        return usuarioRepository.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    private void aplicar(Usuario u, UsuarioRequest r, boolean nuevo) {
        u.setRol(rolRepository.findById(r.idRol()).orElseThrow(() -> ApiException.badRequest("Rol no existe")));
        u.setNombreUsuario(r.nombreUsuario());
        u.setCorreo(r.correo());
        if (r.idEmpleado() != null) {
            u.setEmpleado(empleadoRepository.findById(r.idEmpleado())
                    .orElseThrow(() -> ApiException.badRequest("Trabajador no existe")));
        } else {
            u.setEmpleado(null);
        }
        if (r.password() != null && !r.password().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(r.password()));
        } else if (nuevo) {
            throw ApiException.badRequest("La contraseña es obligatoria");
        }
        if (r.activo() != null) {
            u.setActivo(r.activo());
        }
    }
}
