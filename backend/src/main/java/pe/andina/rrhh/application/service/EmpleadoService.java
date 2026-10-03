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

import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.domain.model.Permisos;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class EmpleadoService implements EmpleadoUseCase {

    private final EmpleadoPort empleadoRepository;
    private final AreaPort areaRepository;
    private final CargoPort cargoRepository;
    private final HorarioLaboralPort horarioRepository;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;
    private final ParametroPort parametros;
    private final CatalogoReglas catalogo;

    public EmpleadoService(EmpleadoPort empleadoRepository,
                           AreaPort areaRepository,
                           CargoPort cargoRepository,
                           HorarioLaboralPort horarioRepository,
                           AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser,
                           ParametroPort parametros,
                           CatalogoReglas catalogo) {
        this.parametros = parametros;
        this.catalogo = catalogo;
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
        if (!currentUser.tienePermiso(Permisos.PERSONAL_CONSULTAR) && !currentUser.alcanceTotal()) {
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

    /** Formato {@code <prefijo>-<correlativo>} según los parámetros del código de trabajador. */
    public void validarCodigo(String codigo) {
        String prefijo = parametros.texto("codigo_empleado_prefijo");
        int digitos = parametros.entero("codigo_empleado_digitos");
        String regex = Pattern.quote(prefijo) + "-[0-9]{" + digitos + ",}";
        if (codigo == null || !codigo.matches(regex)) {
            throw DomainException.badRequest("El código debe tener el formato " + prefijo + "-" + "0".repeat(Math.max(0, digitos - 1)) + "1");
        }
    }

    private void aplicar(Empleado e, EmpleadoRequest r) {
        validarCodigo(r.codigoEmpleado());
        TipoDocumento tipoDocumento = catalogo.oPorDefecto(r.tipoDocumento(), CatalogoReglas.TIPO_DOCUMENTO, TipoDocumento.class);
        catalogo.validarRegla(CatalogoReglas.TIPO_DOCUMENTO, tipoDocumento.name(), r.numeroDocumento(), "El número de documento");
        e.setCodigoEmpleado(r.codigoEmpleado());
        e.setTipoDocumento(tipoDocumento);
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
        e.setTipoContrato(catalogo.oPorDefecto(r.tipoContrato(), CatalogoReglas.TIPO_CONTRATO, TipoContrato.class));
        e.setEstado(catalogo.oPorDefecto(r.estado(), CatalogoReglas.ESTADO_EMPLEADO, EstadoEmpleado.class));
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
