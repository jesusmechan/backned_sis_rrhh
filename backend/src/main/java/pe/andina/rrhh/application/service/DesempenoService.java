package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import pe.andina.rrhh.application.port.in.EmpleadoUseCase;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.DesempenoUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.EvaluacionDesempeno;
import pe.andina.rrhh.domain.model.enums.EstadoEvaluacion;
import pe.andina.rrhh.application.dto.AppDtos.EvaluacionRequest;
import pe.andina.rrhh.application.dto.AppDtos.EvaluacionResponse;
import pe.andina.rrhh.application.port.out.EvaluacionDesempenoPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class DesempenoService implements DesempenoUseCase {

    private final EvaluacionDesempenoPort evaluacionRepository;
    private final EmpleadoUseCase empleadoService;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;

    public DesempenoService(EvaluacionDesempenoPort evaluacionRepository,
                            EmpleadoUseCase empleadoService,
                            AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser) {
        this.evaluacionRepository = evaluacionRepository;
        this.empleadoService = empleadoService;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<EvaluacionResponse> listar() {
        if (currentUser.isAdminOrRrhh() || currentUser.hasRole("JEFE") || currentUser.hasRole("GERENCIA")) {
            return evaluacionRepository.findAllByOrderByFechaDescIdEvaluacionDesc().stream().map(this::toResponse).toList();
        }
        Integer propio = currentUser.idEmpleado();
        if (propio == null) {
            return List.of();
        }
        return evaluacionRepository.findByEmpleado_IdEmpleadoOrderByFechaDesc(propio).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EvaluacionResponse obtener(Integer id) {
        EvaluacionDesempeno e = buscar(id);
        if (!currentUser.isAdminOrRrhh() && !currentUser.hasRole("JEFE") && !currentUser.hasRole("GERENCIA")) {
            Integer propio = currentUser.idEmpleado();
            if (propio == null || !propio.equals(e.getEmpleado().getIdEmpleado())) {
                throw DomainException.forbidden("No puede consultar evaluaciones de otro trabajador");
            }
        }
        return toResponse(e);
    }

    @Transactional
    public EvaluacionResponse crear(EvaluacionRequest request) {
        if (!currentUser.isAdminOrRrhh() && !currentUser.hasRole("JEFE") && !currentUser.hasRole("GERENCIA")) {
            throw DomainException.forbidden("Solo jefatura, gerencia o RR. HH. puede registrar evaluaciones");
        }
        Empleado empleado = empleadoService.buscar(request.idEmpleado());
        EvaluacionDesempeno e = new EvaluacionDesempeno();
        e.setEmpleado(empleado);
        e.setEvaluador(currentUser.usuario());
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
        auditoriaService.registrar(currentUser.usuario(), "REGISTRAR", "DESEMPENO", e.getIdEvaluacion(), e.getPeriodo());
        return toResponse(e);
    }

    private BigDecimal promedio(EvaluacionRequest r) {
        return BigDecimal.valueOf(r.puntualidad() + r.calidad() + r.cooperacion() + r.iniciativa())
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
    }

    private EvaluacionDesempeno buscar(Integer id) {
        return evaluacionRepository.findById(id).orElseThrow(() -> DomainException.notFound("Evaluación no encontrada"));
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
