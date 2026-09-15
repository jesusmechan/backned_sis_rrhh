package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Empleado;
import pe.andina.rrhh.domain.EvaluacionDesempeno;
import pe.andina.rrhh.domain.enums.EstadoEvaluacion;
import pe.andina.rrhh.dto.AppDtos.EvaluacionRequest;
import pe.andina.rrhh.dto.AppDtos.EvaluacionResponse;
import pe.andina.rrhh.repo.EvaluacionDesempenoRepository;
import pe.andina.rrhh.security.SecurityUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class DesempenoService {

    private final EvaluacionDesempenoRepository evaluacionRepository;
    private final EmpleadoService empleadoService;
    private final AuditoriaService auditoriaService;

    public DesempenoService(EvaluacionDesempenoRepository evaluacionRepository,
                            EmpleadoService empleadoService,
                            AuditoriaService auditoriaService) {
        this.evaluacionRepository = evaluacionRepository;
        this.empleadoService = empleadoService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResponse> listar() {
        if (SecurityUtils.isAdminOrRrhh() || SecurityUtils.hasRole("APROBADOR")) {
            return evaluacionRepository.findAllByOrderByFechaDescIdEvaluacionDesc().stream().map(this::toResponse).toList();
        }
        Integer propio = SecurityUtils.current().getIdEmpleado();
        if (propio == null) {
            return List.of();
        }
        return evaluacionRepository.findByEmpleado_IdEmpleadoOrderByFechaDesc(propio).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EvaluacionResponse obtener(Integer id) {
        EvaluacionDesempeno e = buscar(id);
        if (!SecurityUtils.isAdminOrRrhh() && !SecurityUtils.hasRole("APROBADOR")) {
            Integer propio = SecurityUtils.current().getIdEmpleado();
            if (propio == null || !propio.equals(e.getEmpleado().getIdEmpleado())) {
                throw ApiException.forbidden("No puede consultar evaluaciones de otro trabajador");
            }
        }
        return toResponse(e);
    }

    @Transactional
    public EvaluacionResponse crear(EvaluacionRequest request) {
        if (!SecurityUtils.isAdminOrRrhh() && !SecurityUtils.hasRole("APROBADOR")) {
            throw ApiException.forbidden("Solo un aprobador o RR. HH. puede registrar evaluaciones");
        }
        Empleado empleado = empleadoService.buscar(request.idEmpleado());
        EvaluacionDesempeno e = new EvaluacionDesempeno();
        e.setEmpleado(empleado);
        e.setEvaluador(SecurityUtils.current().getUsuario());
        e.setPeriodo(request.periodo().trim());
        e.setFecha(request.fecha() != null ? request.fecha() : LocalDate.now());
        e.setPuntualidad(request.puntualidad());
        e.setCalidad(request.calidad());
        e.setCooperacion(request.cooperacion());
        e.setIniciativa(request.iniciativa());
        e.setComentario(request.comentario());
        e.setPromedio(promedio(request));
        e.setEstado(EstadoEvaluacion.CERRADA);
        evaluacionRepository.save(e);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "REGISTRAR", "DESEMPENO", e.getIdEvaluacion(), e.getPeriodo());
        return toResponse(e);
    }

    private BigDecimal promedio(EvaluacionRequest r) {
        return BigDecimal.valueOf(r.puntualidad() + r.calidad() + r.cooperacion() + r.iniciativa())
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
    }

    private EvaluacionDesempeno buscar(Integer id) {
        return evaluacionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Evaluación no encontrada"));
    }

    private EvaluacionResponse toResponse(EvaluacionDesempeno e) {
        return new EvaluacionResponse(
                e.getIdEvaluacion(),
                e.getEmpleado().getIdEmpleado(),
                e.getEmpleado().nombreCompleto(),
                e.getEvaluador() != null ? e.getEvaluador().getNombreUsuario() : null,
                e.getPeriodo(),
                e.getFecha(),
                e.getPuntualidad(),
                e.getCalidad(),
                e.getCooperacion(),
                e.getIniciativa(),
                e.getPromedio(),
                e.getComentario(),
                e.getEstado()
        );
    }
}
