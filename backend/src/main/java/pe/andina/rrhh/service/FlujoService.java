package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.ConfiguracionAprobacion;
import pe.andina.rrhh.domain.ConfiguracionAprobacionDetalle;
import pe.andina.rrhh.domain.enums.TipoAprobador;
import pe.andina.rrhh.dto.AppDtos.FlujoPasoRequest;
import pe.andina.rrhh.dto.AppDtos.FlujoRequest;
import pe.andina.rrhh.dto.AppDtos.FlujoResponse;
import pe.andina.rrhh.repo.ConfiguracionAprobacionDetalleRepository;
import pe.andina.rrhh.repo.ConfiguracionAprobacionRepository;
import pe.andina.rrhh.repo.RolRepository;
import pe.andina.rrhh.repo.TipoPermisoRepository;
import pe.andina.rrhh.repo.UsuarioRepository;
import pe.andina.rrhh.security.SecurityUtils;

import java.util.List;

@Service
public class FlujoService {

    private final ConfiguracionAprobacionRepository flujoRepository;
    private final ConfiguracionAprobacionDetalleRepository detalleRepository;
    private final TipoPermisoRepository tipoPermisoRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public FlujoService(ConfiguracionAprobacionRepository flujoRepository,
                        ConfiguracionAprobacionDetalleRepository detalleRepository,
                        TipoPermisoRepository tipoPermisoRepository,
                        RolRepository rolRepository,
                        UsuarioRepository usuarioRepository,
                        AuditoriaService auditoriaService) {
        this.flujoRepository = flujoRepository;
        this.detalleRepository = detalleRepository;
        this.tipoPermisoRepository = tipoPermisoRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<FlujoResponse> listar() {
        return flujoRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FlujoResponse obtener(Integer id) {
        return toDto(buscar(id));
    }

    @Transactional
    public FlujoResponse crear(FlujoRequest request) {
        if (flujoRepository.findByCodigo(request.codigo()).isPresent()) {
            throw ApiException.conflict("Ya existe un flujo con ese código");
        }
        ConfiguracionAprobacion c = new ConfiguracionAprobacion();
        aplicarCabecera(c, request);
        flujoRepository.save(c);
        guardarPasos(c, request.pasos());
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "CREAR", "FLUJO", c.getIdConfiguracion(), c.getCodigo());
        return toDto(c);
    }

    @Transactional
    public FlujoResponse actualizar(Integer id, FlujoRequest request) {
        ConfiguracionAprobacion c = buscar(id);
        aplicarCabecera(c, request);
        detalleRepository.deleteByConfiguracion_IdConfiguracion(id);
        detalleRepository.flush();
        guardarPasos(c, request.pasos());
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "ACTUALIZAR", "FLUJO", id, c.getCodigo());
        return toDto(c);
    }

    private void aplicarCabecera(ConfiguracionAprobacion c, FlujoRequest r) {
        c.setCodigo(r.codigo());
        c.setNombre(r.nombre());
        c.setTipoOrigen(r.tipoOrigen());
        c.setDescripcion(r.descripcion());
        c.setActivo(r.activo() == null || r.activo());
        if (r.idTipoPermiso() != null) {
            c.setTipoPermiso(tipoPermisoRepository.findById(r.idTipoPermiso())
                    .orElseThrow(() -> ApiException.badRequest("Tipo de permiso no existe")));
        } else {
            c.setTipoPermiso(null);
        }
    }

    private void guardarPasos(ConfiguracionAprobacion c, List<FlujoPasoRequest> pasos) {
        if (pasos == null || pasos.isEmpty()) {
            throw ApiException.badRequest("El flujo debe tener al menos un paso");
        }
        for (FlujoPasoRequest p : pasos) {
            ConfiguracionAprobacionDetalle d = new ConfiguracionAprobacionDetalle();
            d.setConfiguracion(c);
            d.setNumeroPaso(p.numeroPaso());
            d.setNombrePaso(p.nombrePaso());
            d.setTipoAprobador(p.tipoAprobador());
            d.setEsObligatorio(p.esObligatorio() == null || p.esObligatorio());
            d.setActivo(true);
            if (p.tipoAprobador() == TipoAprobador.ROL) {
                if (p.idRol() == null) {
                    throw ApiException.badRequest("El paso " + p.numeroPaso() + " requiere un perfil");
                }
                d.setRol(rolRepository.findById(p.idRol()).orElseThrow(() -> ApiException.badRequest("Perfil no existe")));
            } else if (p.tipoAprobador() == TipoAprobador.USUARIO) {
                if (p.idUsuario() == null) {
                    throw ApiException.badRequest("El paso " + p.numeroPaso() + " requiere un usuario");
                }
                d.setUsuario(usuarioRepository.findById(p.idUsuario()).orElseThrow(() -> ApiException.badRequest("Usuario no existe")));
            }
            detalleRepository.save(d);
        }
    }

    private ConfiguracionAprobacion buscar(Integer id) {
        return flujoRepository.findById(id).orElseThrow(() -> ApiException.notFound("Flujo no encontrado"));
    }

    private FlujoResponse toDto(ConfiguracionAprobacion c) {
        return DtoMapper.flujo(c, detalleRepository.findByConfiguracion_IdConfiguracionOrderByNumeroPasoAsc(c.getIdConfiguracion()));
    }
}
