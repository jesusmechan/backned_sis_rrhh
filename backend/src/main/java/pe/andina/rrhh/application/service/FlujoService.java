package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.FlujoUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacion;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacionDetalle;
import pe.andina.rrhh.domain.model.enums.TipoAprobador;
import pe.andina.rrhh.application.dto.AppDtos.FlujoPasoRequest;
import pe.andina.rrhh.application.dto.AppDtos.FlujoRequest;
import pe.andina.rrhh.application.dto.AppDtos.FlujoResponse;
import pe.andina.rrhh.application.port.out.ConfiguracionAprobacionDetallePort;
import pe.andina.rrhh.application.port.out.ConfiguracionAprobacionPort;
import pe.andina.rrhh.application.port.out.RolPort;
import pe.andina.rrhh.application.port.out.TipoPermisoPort;
import pe.andina.rrhh.application.port.out.UsuarioPort;

import java.util.List;

@Service
public class FlujoService implements FlujoUseCase {

    private final ConfiguracionAprobacionPort flujoRepository;
    private final ConfiguracionAprobacionDetallePort detalleRepository;
    private final TipoPermisoPort tipoPermisoRepository;
    private final RolPort rolRepository;
    private final UsuarioPort usuarioRepository;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;

    public FlujoService(ConfiguracionAprobacionPort flujoRepository,
                        ConfiguracionAprobacionDetallePort detalleRepository,
                        TipoPermisoPort tipoPermisoRepository,
                        RolPort rolRepository,
                        UsuarioPort usuarioRepository,
                        AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser) {
        this.flujoRepository = flujoRepository;
        this.detalleRepository = detalleRepository;
        this.tipoPermisoRepository = tipoPermisoRepository;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
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
            throw DomainException.conflict("Ya existe un flujo con ese código");
        }
        ConfiguracionAprobacion c = new ConfiguracionAprobacion();
        aplicarCabecera(c, request);
        flujoRepository.save(c);
        guardarPasos(c, request.pasos());
        auditoriaService.registrar(currentUser.usuario(), "CREAR", "FLUJO", c.getIdConfiguracion(), c.getCodigo());
        return toDto(c);
    }

    @Transactional
    public FlujoResponse actualizar(Integer id, FlujoRequest request) {
        ConfiguracionAprobacion c = buscar(id);
        aplicarCabecera(c, request);
        detalleRepository.deleteByConfiguracion_IdConfiguracion(id);
        detalleRepository.flush();
        guardarPasos(c, request.pasos());
        auditoriaService.registrar(currentUser.usuario(), "ACTUALIZAR", "FLUJO", id, c.getCodigo());
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
                    .orElseThrow(() -> DomainException.badRequest("Tipo de permiso no existe")));
        } else {
            c.setTipoPermiso(null);
        }
    }

    private void guardarPasos(ConfiguracionAprobacion c, List<FlujoPasoRequest> pasos) {
        if (pasos == null || pasos.isEmpty()) {
            throw DomainException.badRequest("El flujo debe tener al menos un paso");
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
                    throw DomainException.badRequest("El paso " + p.numeroPaso() + " requiere un perfil");
                }
                d.setRol(rolRepository.findById(p.idRol()).orElseThrow(() -> DomainException.badRequest("Perfil no existe")));
            } else if (p.tipoAprobador() == TipoAprobador.USUARIO) {
                if (p.idUsuario() == null) {
                    throw DomainException.badRequest("El paso " + p.numeroPaso() + " requiere un usuario");
                }
                d.setUsuario(usuarioRepository.findById(p.idUsuario()).orElseThrow(() -> DomainException.badRequest("Usuario no existe")));
            }
            detalleRepository.save(d);
        }
    }

    private ConfiguracionAprobacion buscar(Integer id) {
        return flujoRepository.findById(id).orElseThrow(() -> DomainException.notFound("Flujo no encontrado"));
    }

    private FlujoResponse toDto(ConfiguracionAprobacion c) {
        return DtoMapper.flujo(c, detalleRepository.findByConfiguracion_IdConfiguracionOrderByNumeroPasoAsc(c.getIdConfiguracion()));
    }
}
