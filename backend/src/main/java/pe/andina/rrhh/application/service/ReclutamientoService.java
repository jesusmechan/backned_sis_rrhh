package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.ReclutamientoUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Area;
import pe.andina.rrhh.domain.model.Convocatoria;
import pe.andina.rrhh.domain.model.Postulacion;
import pe.andina.rrhh.domain.model.enums.EstadoConvocatoria;
import pe.andina.rrhh.domain.model.enums.EstadoPostulacion;
import pe.andina.rrhh.application.dto.AppDtos.ConvocatoriaRequest;
import pe.andina.rrhh.application.dto.AppDtos.ConvocatoriaResponse;
import pe.andina.rrhh.application.dto.AppDtos.PostulacionRequest;
import pe.andina.rrhh.application.dto.AppDtos.PostulacionResponse;
import pe.andina.rrhh.application.port.out.AreaPort;
import pe.andina.rrhh.application.port.out.ConvocatoriaPort;
import pe.andina.rrhh.application.port.out.PostulacionPort;

import java.util.List;

@Service
public class ReclutamientoService implements ReclutamientoUseCase {

    private final ConvocatoriaPort convocatoriaRepository;
    private final PostulacionPort postulacionRepository;
    private final AreaPort areaRepository;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;

    public ReclutamientoService(ConvocatoriaPort convocatoriaRepository,
                                PostulacionPort postulacionRepository,
                                AreaPort areaRepository,
                                AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser) {
        this.convocatoriaRepository = convocatoriaRepository;
        this.postulacionRepository = postulacionRepository;
        this.areaRepository = areaRepository;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ConvocatoriaResponse> listar() {
        return convocatoriaRepository.findAllByOrderByIdConvocatoriaDesc().stream().map(this::toConvocatoria).toList();
    }

    @Transactional(readOnly = true)
    public ConvocatoriaResponse obtener(Integer id) {
        return toConvocatoria(buscar(id));
    }

    @Transactional
    public ConvocatoriaResponse crear(ConvocatoriaRequest request) {
        Convocatoria c = new Convocatoria();
        c.setCodigo(siguienteCodigo());
        aplicar(c, request);
        convocatoriaRepository.save(c);
        auditoriaService.registrar(currentUser.usuario(), "CREAR", "CONVOCATORIA", c.getIdConvocatoria(), c.getCodigo());
        return toConvocatoria(c);
    }

    @Transactional
    public ConvocatoriaResponse actualizar(Integer id, ConvocatoriaRequest request) {
        Convocatoria c = buscar(id);
        aplicar(c, request);
        auditoriaService.registrar(currentUser.usuario(), "ACTUALIZAR", "CONVOCATORIA", id, c.getCodigo());
        return toConvocatoria(c);
    }

    @Transactional(readOnly = true)
    public List<PostulacionResponse> postulaciones(Integer idConvocatoria) {
        buscar(idConvocatoria);
        return postulacionRepository.findByConvocatoria_IdConvocatoriaOrderByIdPostulacionDesc(idConvocatoria)
                .stream().map(this::toPostulacion).toList();
    }

    @Transactional
    public PostulacionResponse registrarPostulacion(Integer idConvocatoria, PostulacionRequest request) {
        Convocatoria c = buscar(idConvocatoria);
        if (c.getEstado() != EstadoConvocatoria.ABIERTA) {
            throw DomainException.badRequest("Solo se registran postulantes en una convocatoria abierta");
        }
        Postulacion p = new Postulacion();
        p.setConvocatoria(c);
        aplicar(p, request);
        if (p.getEstado() == null) {
            p.setEstado(EstadoPostulacion.POSTULADO);
        }
        postulacionRepository.save(p);
        auditoriaService.registrar(currentUser.usuario(), "REGISTRAR", "POSTULACION", p.getIdPostulacion(), p.nombreCompleto());
        return toPostulacion(p);
    }

    @Transactional
    public PostulacionResponse actualizarPostulacion(Integer id, PostulacionRequest request) {
        Postulacion p = postulacionRepository.findById(id).orElseThrow(() -> DomainException.notFound("Postulación no encontrada"));
        aplicar(p, request);
        auditoriaService.registrar(currentUser.usuario(), "ACTUALIZAR", "POSTULACION", id, p.getEstado().name());
        return toPostulacion(p);
    }

    private void aplicar(Convocatoria c, ConvocatoriaRequest r) {
        c.setPuesto(r.puesto().trim());
        c.setVacantes(r.vacantes() != null && r.vacantes() > 0 ? r.vacantes() : 1);
        c.setFechaInicio(r.fechaInicio());
        c.setFechaFin(r.fechaFin());
        c.setDescripcion(r.descripcion());
        c.setEstado(r.estado() != null ? r.estado() : EstadoConvocatoria.ABIERTA);
        if (r.idArea() != null) {
            Area area = areaRepository.findById(r.idArea()).orElseThrow(() -> DomainException.badRequest("Área no existe"));
            c.setArea(area);
        } else {
            c.setArea(null);
        }
    }

    private void aplicar(Postulacion p, PostulacionRequest r) {
        p.setNombres(r.nombres().trim());
        p.setApellidos(r.apellidos().trim());
        p.setDocumento(r.documento().trim());
        p.setCorreo(r.correo());
        p.setTelefono(r.telefono());
        p.setPuntaje(r.puntaje());
        p.setObservacion(r.observacion());
        if (r.estado() != null) {
            p.setEstado(r.estado());
        }
    }

    private String siguienteCodigo() {
        int max = convocatoriaRepository.findAll().stream()
                .mapToInt(c -> {
                    try {
                        return Integer.parseInt(c.getCodigo().replaceAll("\\D", ""));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return "CONV-" + String.format("%03d", max + 1);
    }

    private Convocatoria buscar(Integer id) {
        return convocatoriaRepository.findById(id).orElseThrow(() -> DomainException.notFound("Convocatoria no encontrada"));
    }

    private ConvocatoriaResponse toConvocatoria(Convocatoria c) {
        return new ConvocatoriaResponse(
                c.getIdConvocatoria(), c.getCodigo(), c.getPuesto(),
                c.getArea() != null ? c.getArea().getIdArea() : null,
                c.getArea() != null ? c.getArea().getNombre() : null,
                c.getVacantes(), c.getFechaInicio(), c.getFechaFin(), c.getDescripcion(), c.getEstado(),
                postulacionRepository.countByConvocatoria_IdConvocatoria(c.getIdConvocatoria())
        );
    }

    private PostulacionResponse toPostulacion(Postulacion p) {
        return new PostulacionResponse(
                p.getIdPostulacion(), p.getConvocatoria().getIdConvocatoria(), p.getConvocatoria().getPuesto(),
                p.getNombres(), p.getApellidos(), p.nombreCompleto(), p.getDocumento(),
                p.getCorreo(), p.getTelefono(), p.getPuntaje(), p.getEstado(), p.getObservacion()
        );
    }
}
