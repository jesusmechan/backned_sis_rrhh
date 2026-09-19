package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.EmpleadoUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.model.enums.TipoContrato;
import pe.andina.rrhh.domain.model.enums.TipoDocumento;
import pe.andina.rrhh.application.dto.AppDtos.EmpleadoRequest;
import pe.andina.rrhh.application.dto.AppDtos.EmpleadoResponse;
import pe.andina.rrhh.application.port.out.AreaPort;
import pe.andina.rrhh.application.port.out.CargoPort;
import pe.andina.rrhh.application.port.out.EmpleadoPort;
import pe.andina.rrhh.application.port.out.HorarioLaboralPort;

import java.util.List;

@Service
public class EmpleadoService implements EmpleadoUseCase {

    private final EmpleadoPort empleadoRepository;
    private final AreaPort areaRepository;
    private final CargoPort cargoRepository;
    private final HorarioLaboralPort horarioRepository;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;

    public EmpleadoService(EmpleadoPort empleadoRepository,
                           AreaPort areaRepository,
                           CargoPort cargoRepository,
                           HorarioLaboralPort horarioRepository,
                           AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser) {
        this.empleadoRepository = empleadoRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.horarioRepository = horarioRepository;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<EmpleadoResponse> listar() {
        return empleadoRepository.findAll().stream().map(DtoMapper::empleado).toList();
    }

    @Transactional(readOnly = true)
    public EmpleadoResponse obtener(Integer id) {
        if (!currentUser.isAdminOrRrhh()) {
            Integer propio = currentUser.idEmpleado();
            if (propio == null || !propio.equals(id)) {
                throw DomainException.forbidden("Solo puede consultar su ficha de personal");
            }
        }
        return DtoMapper.empleado(buscar(id));
    }

    @Transactional
    public EmpleadoResponse crear(EmpleadoRequest request) {
        Empleado e = new Empleado();
        aplicar(e, request);
        empleadoRepository.save(e);
        auditoriaService.registrar(currentUser.usuario(), "CREAR", "EMPLEADO", e.getIdEmpleado(), e.getCodigoEmpleado());
        return DtoMapper.empleado(e);
    }

    @Transactional
    public EmpleadoResponse actualizar(Integer id, EmpleadoRequest request) {
        Empleado e = buscar(id);
        aplicar(e, request);
        auditoriaService.registrar(currentUser.usuario(), "ACTUALIZAR", "EMPLEADO", id, e.getCodigoEmpleado());
        return DtoMapper.empleado(e);
    }

    public Empleado buscar(Integer id) {
        return empleadoRepository.findById(id).orElseThrow(() -> DomainException.notFound("Trabajador no encontrado"));
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
        e.setArea(areaRepository.findById(r.idArea()).orElseThrow(() -> DomainException.badRequest("Área no existe")));
        e.setCargo(cargoRepository.findById(r.idCargo()).orElseThrow(() -> DomainException.badRequest("Cargo no existe")));
        e.setHorario(horarioRepository.findById(r.idHorario()).orElseThrow(() -> DomainException.badRequest("Horario no existe")));
        e.setTipoContrato(r.tipoContrato() != null ? r.tipoContrato() : TipoContrato.PLANILLA);
        e.setEstado(r.estado() != null ? r.estado() : EstadoEmpleado.ACTIVO);
        if (r.idJefeInmediato() != null) {
            if (e.getIdEmpleado() != null && r.idJefeInmediato().equals(e.getIdEmpleado())) {
                throw DomainException.badRequest("El trabajador no puede ser su propio jefe");
            }
            e.setJefeInmediato(buscar(r.idJefeInmediato()));
        } else {
            e.setJefeInmediato(null);
        }
    }
}
