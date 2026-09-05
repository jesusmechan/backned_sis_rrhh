package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Empleado;
import pe.andina.rrhh.domain.Marcacion;
import pe.andina.rrhh.domain.enums.TipoMarcacion;
import pe.andina.rrhh.dto.AppDtos.MarcacionRequest;
import pe.andina.rrhh.dto.AppDtos.MarcacionResponse;
import pe.andina.rrhh.repo.MarcacionRepository;
import pe.andina.rrhh.security.SecurityUtils;
import pe.andina.rrhh.security.UsuarioPrincipal;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AsistenciaService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final MarcacionRepository marcacionRepository;
    private final EmpleadoService empleadoService;
    private final AuditoriaService auditoriaService;

    public AsistenciaService(MarcacionRepository marcacionRepository,
                             EmpleadoService empleadoService,
                             AuditoriaService auditoriaService) {
        this.marcacionRepository = marcacionRepository;
        this.empleadoService = empleadoService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public MarcacionResponse marcar(MarcacionRequest request) {
        UsuarioPrincipal me = SecurityUtils.current();
        Empleado empleado;
        String origen = request.origen() != null ? request.origen() : "WEB";
        if (SecurityUtils.isAdminOrRrhh() && request.idEmpleado() != null) {
            empleado = empleadoService.buscar(request.idEmpleado());
            if (request.origen() == null) {
                origen = "MANUAL";
            }
        } else {
            if (me.getIdEmpleado() == null) {
                throw ApiException.badRequest("El usuario no está asociado a un trabajador");
            }
            empleado = empleadoService.buscar(me.getIdEmpleado());
        }
        OffsetDateTime fechaHora = request.fechaHora() != null ? request.fechaHora() : OffsetDateTime.now();
        LocalDate fecha = fechaHora.atZoneSameInstant(LIMA).toLocalDate();
        if (!marcacionRepository.findByEmpleado_IdEmpleadoAndTipoAndFecha(empleado.getIdEmpleado(), request.tipo(), fecha).isEmpty()
                && "WEB".equals(origen)) {
            throw ApiException.conflict("Ya existe una marcación de " + request.tipo() + " para hoy");
        }
        Marcacion m = new Marcacion();
        m.setEmpleado(empleado);
        m.setTipo(request.tipo());
        m.setFechaHora(fechaHora);
        m.setOrigen(origen);
        m.setObservacion(request.observacion());
        m.setUsuarioRegistro(me.getUsuario());
        marcacionRepository.saveAndFlush(m);
        auditoriaService.registrar(me.getUsuario(), "MARCAR", "ASISTENCIA", m.getIdMarcacion(), request.tipo().name());
        return DtoMapper.marcacion(marcacionRepository.findById(m.getIdMarcacion()).orElse(m));
    }

    @Transactional(readOnly = true)
    public List<MarcacionResponse> listar(Integer idEmpleado, LocalDate desde, LocalDate hasta) {
        LocalDate ini = desde != null ? desde : LocalDate.now().minusDays(15);
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        if (!SecurityUtils.isAdminOrRrhh()) {
            Integer propio = SecurityUtils.current().getIdEmpleado();
            if (propio == null) {
                throw ApiException.forbidden("No tiene trabajador asociado");
            }
            return marcacionRepository.findByEmpleado_IdEmpleadoOrderByFechaHoraDesc(propio).stream()
                    .filter(m -> !m.getFechaHora().toLocalDate().isBefore(ini) && !m.getFechaHora().toLocalDate().isAfter(fin))
                    .map(DtoMapper::marcacion)
                    .toList();
        }
        if (idEmpleado != null) {
            return marcacionRepository.findByEmpleado_IdEmpleadoOrderByFechaHoraDesc(idEmpleado).stream()
                    .map(DtoMapper::marcacion).toList();
        }
        return marcacionRepository.findByFechaBetweenOrderByFechaHoraDesc(ini, fin).stream()
                .map(DtoMapper::marcacion).toList();
    }

    @Transactional(readOnly = true)
    public MarcacionResponse obtener(Integer id) {
        Marcacion m = marcacionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Marcación no encontrada"));
        if (!SecurityUtils.isAdminOrRrhh()
                && !m.getEmpleado().getIdEmpleado().equals(SecurityUtils.current().getIdEmpleado())) {
            throw ApiException.forbidden("No puede consultar la asistencia de otro trabajador");
        }
        return DtoMapper.marcacion(m);
    }

    @Transactional
    public MarcacionResponse actualizar(Integer id, MarcacionRequest request) {
        if (!SecurityUtils.isAdminOrRrhh()) {
            throw ApiException.forbidden("Solo RR. HH. o Administrador puede corregir marcaciones");
        }
        Marcacion m = marcacionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Marcación no encontrada"));
        if (request.tipo() != null) {
            m.setTipo(request.tipo());
        }
        if (request.fechaHora() != null) {
            m.setFechaHora(request.fechaHora());
        }
        if (request.observacion() != null) {
            m.setObservacion(request.observacion());
        }
        m.setOrigen("CORRECCION");
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "CORREGIR", "ASISTENCIA", id, null);
        return DtoMapper.marcacion(m);
    }
}
