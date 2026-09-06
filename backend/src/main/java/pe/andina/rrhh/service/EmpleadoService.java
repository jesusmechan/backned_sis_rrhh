package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Empleado;
import pe.andina.rrhh.domain.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.enums.TipoContrato;
import pe.andina.rrhh.domain.enums.TipoDocumento;
import pe.andina.rrhh.dto.AppDtos.EmpleadoRequest;
import pe.andina.rrhh.dto.AppDtos.EmpleadoResponse;
import pe.andina.rrhh.repo.AreaRepository;
import pe.andina.rrhh.repo.CargoRepository;
import pe.andina.rrhh.repo.EmpleadoRepository;
import pe.andina.rrhh.repo.HorarioLaboralRepository;
import pe.andina.rrhh.security.SecurityUtils;

import java.util.List;

@Service
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final HorarioLaboralRepository horarioRepository;
    private final AuditoriaService auditoriaService;

    public EmpleadoService(EmpleadoRepository empleadoRepository,
                           AreaRepository areaRepository,
                           CargoRepository cargoRepository,
                           HorarioLaboralRepository horarioRepository,
                           AuditoriaService auditoriaService) {
        this.empleadoRepository = empleadoRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.horarioRepository = horarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<EmpleadoResponse> listar() {
        return empleadoRepository.findAll().stream().map(DtoMapper::empleado).toList();
    }

    @Transactional(readOnly = true)
    public EmpleadoResponse obtener(Integer id) {
        if (!SecurityUtils.isAdminOrRrhh()) {
            Integer propio = SecurityUtils.current().getIdEmpleado();
            if (propio == null || !propio.equals(id)) {
                throw ApiException.forbidden("Solo puede consultar su ficha de personal");
            }
        }
        return DtoMapper.empleado(buscar(id));
    }

    @Transactional
    public EmpleadoResponse crear(EmpleadoRequest request) {
        Empleado e = new Empleado();
        aplicar(e, request);
        empleadoRepository.save(e);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "CREAR", "EMPLEADO", e.getIdEmpleado(), e.getCodigoEmpleado());
        return DtoMapper.empleado(e);
    }

    @Transactional
    public EmpleadoResponse actualizar(Integer id, EmpleadoRequest request) {
        Empleado e = buscar(id);
        aplicar(e, request);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "ACTUALIZAR", "EMPLEADO", id, e.getCodigoEmpleado());
        return DtoMapper.empleado(e);
    }

    public Empleado buscar(Integer id) {
        return empleadoRepository.findById(id).orElseThrow(() -> ApiException.notFound("Trabajador no encontrado"));
    }

    private void aplicar(Empleado e, EmpleadoRequest r) {
        e.setCodigoEmpleado(r.codigoEmpleado());
        e.setTipoDocumento(r.tipoDocumento() != null ? r.tipoDocumento() : TipoDocumento.DNI);
        e.setNumeroDocumento(r.numeroDocumento());
        e.setNombres(r.nombres());
        e.setApellidoPaterno(r.apellidoPaterno());
        e.setApellidoMaterno(r.apellidoMaterno());
        e.setFechaNacimiento(r.fechaNacimiento());
        e.setSexo(r.sexo());
        e.setCorreoInstitucional(r.correoInstitucional());
        e.setCorreoPersonal(r.correoPersonal());
        e.setTelefono(r.telefono());
        e.setDireccion(r.direccion());
        e.setFechaIngreso(r.fechaIngreso());
        e.setFechaCese(r.fechaCese());
        e.setArea(areaRepository.findById(r.idArea()).orElseThrow(() -> ApiException.badRequest("Área no existe")));
        e.setCargo(cargoRepository.findById(r.idCargo()).orElseThrow(() -> ApiException.badRequest("Cargo no existe")));
        e.setHorario(horarioRepository.findById(r.idHorario()).orElseThrow(() -> ApiException.badRequest("Horario no existe")));
        e.setTipoContrato(r.tipoContrato() != null ? r.tipoContrato() : TipoContrato.PLANILLA);
        e.setEstado(r.estado() != null ? r.estado() : EstadoEmpleado.ACTIVO);
        if (r.idJefeInmediato() != null) {
            if (e.getIdEmpleado() != null && r.idJefeInmediato().equals(e.getIdEmpleado())) {
                throw ApiException.badRequest("El trabajador no puede ser su propio jefe");
            }
            e.setJefeInmediato(buscar(r.idJefeInmediato()));
        } else {
            e.setJefeInmediato(null);
        }
    }
}
