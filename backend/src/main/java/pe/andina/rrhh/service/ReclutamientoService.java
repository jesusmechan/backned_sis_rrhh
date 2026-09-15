package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Area;
import pe.andina.rrhh.domain.Convocatoria;
import pe.andina.rrhh.domain.Postulacion;
import pe.andina.rrhh.domain.enums.EstadoConvocatoria;
import pe.andina.rrhh.domain.enums.EstadoPostulacion;
import pe.andina.rrhh.dto.AppDtos.ConvocatoriaRequest;
import pe.andina.rrhh.dto.AppDtos.ConvocatoriaResponse;
import pe.andina.rrhh.dto.AppDtos.PostulacionRequest;
import pe.andina.rrhh.dto.AppDtos.PostulacionResponse;
import pe.andina.rrhh.repo.AreaRepository;
import pe.andina.rrhh.repo.ConvocatoriaRepository;
import pe.andina.rrhh.repo.PostulacionRepository;
import pe.andina.rrhh.security.SecurityUtils;

import java.util.List;

@Service
public class ReclutamientoService {

    private final ConvocatoriaRepository convocatoriaRepository;
    private final PostulacionRepository postulacionRepository;
    private final AreaRepository areaRepository;
    private final AuditoriaService auditoriaService;

    public ReclutamientoService(ConvocatoriaRepository convocatoriaRepository,
                                PostulacionRepository postulacionRepository,
                                AreaRepository areaRepository,
                                AuditoriaService auditoriaService) {
        this.convocatoriaRepository = convocatoriaRepository;
        this.postulacionRepository = postulacionRepository;
        this.areaRepository = areaRepository;
        this.auditoriaService = auditoriaService;
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
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "CREAR", "CONVOCATORIA", c.getIdConvocatoria(), c.getCodigo());
        return toConvocatoria(c);
    }

    @Transactional
    public ConvocatoriaResponse actualizar(Integer id, ConvocatoriaRequest request) {
        Convocatoria c = buscar(id);
        aplicar(c, request);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "ACTUALIZAR", "CONVOCATORIA", id, c.getCodigo());
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
            throw ApiException.badRequest("Solo se registran postulantes en una convocatoria abierta");
        }
        Postulacion p = new Postulacion();
        p.setConvocatoria(c);
        aplicar(p, request);
        if (p.getEstado() == null) {
            p.setEstado(EstadoPostulacion.POSTULADO);
        }
        postulacionRepository.save(p);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "REGISTRAR", "POSTULACION", p.getIdPostulacion(), p.nombreCompleto());
        return toPostulacion(p);
    }

    @Transactional
    public PostulacionResponse actualizarPostulacion(Integer id, PostulacionRequest request) {
        Postulacion p = postulacionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Postulación no encontrada"));
        aplicar(p, request);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "ACTUALIZAR", "POSTULACION", id, p.getEstado().name());
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
            Area area = areaRepository.findById(r.idArea()).orElseThrow(() -> ApiException.badRequest("Área no existe"));
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
        return convocatoriaRepository.findById(id).orElseThrow(() -> ApiException.notFound("Convocatoria no encontrada"));
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
