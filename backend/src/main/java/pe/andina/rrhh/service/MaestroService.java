package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Area;
import pe.andina.rrhh.domain.Cargo;
import pe.andina.rrhh.domain.CuentaContable;
import pe.andina.rrhh.domain.HorarioLaboral;
import pe.andina.rrhh.domain.ParametroSistema;
import pe.andina.rrhh.domain.TipoPermiso;
import pe.andina.rrhh.dto.AppDtos.AreaRequest;
import pe.andina.rrhh.dto.AppDtos.AreaResponse;
import pe.andina.rrhh.dto.AppDtos.CargoRequest;
import pe.andina.rrhh.dto.AppDtos.CargoResponse;
import pe.andina.rrhh.dto.AppDtos.CuentaRequest;
import pe.andina.rrhh.dto.AppDtos.CuentaResponse;
import pe.andina.rrhh.dto.AppDtos.HorarioRequest;
import pe.andina.rrhh.dto.AppDtos.HorarioResponse;
import pe.andina.rrhh.dto.AppDtos.ParametroRequest;
import pe.andina.rrhh.dto.AppDtos.ParametroResponse;
import pe.andina.rrhh.dto.AppDtos.TipoPermisoMaestroResponse;
import pe.andina.rrhh.dto.AppDtos.TipoPermisoRequest;
import pe.andina.rrhh.repo.AreaRepository;
import pe.andina.rrhh.repo.CargoRepository;
import pe.andina.rrhh.repo.CuentaContableRepository;
import pe.andina.rrhh.repo.HorarioLaboralRepository;
import pe.andina.rrhh.repo.ParametroSistemaRepository;
import pe.andina.rrhh.repo.TipoPermisoRepository;
import pe.andina.rrhh.security.SecurityUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class MaestroService {

    private static final String VACACIONES = "VACACIONES";

    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final HorarioLaboralRepository horarioRepository;
    private final TipoPermisoRepository tipoPermisoRepository;
    private final ParametroSistemaRepository parametroRepository;
    private final CuentaContableRepository cuentaRepository;
    private final AuditoriaService auditoriaService;

    public MaestroService(AreaRepository areaRepository,
                          CargoRepository cargoRepository,
                          HorarioLaboralRepository horarioRepository,
                          TipoPermisoRepository tipoPermisoRepository,
                          ParametroSistemaRepository parametroRepository,
                          CuentaContableRepository cuentaRepository,
                          AuditoriaService auditoriaService) {
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.horarioRepository = horarioRepository;
        this.tipoPermisoRepository = tipoPermisoRepository;
        this.parametroRepository = parametroRepository;
        this.cuentaRepository = cuentaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> areas() {
        return areaRepository.findAll().stream()
                .sorted(Comparator.comparing(Area::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::toArea)
                .toList();
    }

    @Transactional
    public AreaResponse crearArea(AreaRequest r) {
        exigirLibre(areaRepository.findByNombreIgnoreCase(trim(r.nombre())).map(Area::getIdArea), null, "Ya existe un área con ese nombre");
        Area a = new Area();
        aplicar(a, r);
        areaRepository.save(a);
        auditar("CREAR", "AREA", a.getIdArea(), a.getNombre());
        return toArea(a);
    }

    @Transactional
    public AreaResponse actualizarArea(Integer id, AreaRequest r) {
        Area a = areaRepository.findById(id).orElseThrow(() -> ApiException.notFound("Área no existe"));
        exigirLibre(areaRepository.findByNombreIgnoreCase(trim(r.nombre())).map(Area::getIdArea), a.getIdArea(), "Ya existe un área con ese nombre");
        aplicar(a, r);
        areaRepository.save(a);
        auditar("ACTUALIZAR", "AREA", a.getIdArea(), a.getNombre());
        return toArea(a);
    }

    @Transactional(readOnly = true)
    public List<CargoResponse> cargos() {
        return cargoRepository.findAll().stream()
                .sorted(Comparator.comparing(Cargo::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::toCargo)
                .toList();
    }

    @Transactional
    public CargoResponse crearCargo(CargoRequest r) {
        exigirLibre(cargoRepository.findByNombreIgnoreCase(trim(r.nombre())).map(Cargo::getIdCargo), null, "Ya existe un cargo con ese nombre");
        Cargo c = new Cargo();
        aplicar(c, r);
        cargoRepository.save(c);
        auditar("CREAR", "CARGO", c.getIdCargo(), c.getNombre());
        return toCargo(c);
    }

    @Transactional
    public CargoResponse actualizarCargo(Integer id, CargoRequest r) {
        Cargo c = cargoRepository.findById(id).orElseThrow(() -> ApiException.notFound("Cargo no existe"));
        exigirLibre(cargoRepository.findByNombreIgnoreCase(trim(r.nombre())).map(Cargo::getIdCargo), c.getIdCargo(), "Ya existe un cargo con ese nombre");
        aplicar(c, r);
        cargoRepository.save(c);
        auditar("ACTUALIZAR", "CARGO", c.getIdCargo(), c.getNombre());
        return toCargo(c);
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> horarios() {
        return horarioRepository.findAll().stream()
                .sorted(Comparator.comparing(HorarioLaboral::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::toHorario)
                .toList();
    }

    @Transactional
    public HorarioResponse crearHorario(HorarioRequest r) {
        validarHorario(r);
        exigirLibre(horarioRepository.findByNombreIgnoreCase(trim(r.nombre())).map(HorarioLaboral::getIdHorario), null, "Ya existe un horario con ese nombre");
        HorarioLaboral h = new HorarioLaboral();
        aplicar(h, r);
        horarioRepository.save(h);
        auditar("CREAR", "HORARIO", h.getIdHorario(), h.getNombre());
        return toHorario(h);
    }

    @Transactional
    public HorarioResponse actualizarHorario(Integer id, HorarioRequest r) {
        validarHorario(r);
        HorarioLaboral h = horarioRepository.findById(id).orElseThrow(() -> ApiException.notFound("Horario no existe"));
        exigirLibre(horarioRepository.findByNombreIgnoreCase(trim(r.nombre())).map(HorarioLaboral::getIdHorario), h.getIdHorario(), "Ya existe un horario con ese nombre");
        aplicar(h, r);
        horarioRepository.save(h);
        auditar("ACTUALIZAR", "HORARIO", h.getIdHorario(), h.getNombre());
        return toHorario(h);
    }

    @Transactional(readOnly = true)
    public List<TipoPermisoMaestroResponse> tiposPermiso() {
        return tipoPermisoRepository.findAll().stream()
                .sorted(Comparator.comparing(TipoPermiso::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::toTipo)
                .toList();
    }

    @Transactional
    public TipoPermisoMaestroResponse crearTipoPermiso(TipoPermisoRequest r) {
        String codigo = codigo(r.codigo());
        exigirLibre(tipoPermisoRepository.findByCodigoIgnoreCase(codigo).map(TipoPermiso::getIdTipoPermiso), null, "Ya existe un tipo de permiso con ese código");
        exigirLibre(tipoPermisoRepository.findByNombreIgnoreCase(trim(r.nombre())).map(TipoPermiso::getIdTipoPermiso), null, "Ya existe un tipo de permiso con ese nombre");
        TipoPermiso t = new TipoPermiso();
        aplicar(t, r, codigo);
        tipoPermisoRepository.save(t);
        auditar("CREAR", "TIPO_PERMISO", t.getIdTipoPermiso(), t.getCodigo());
        return toTipo(t);
    }

    @Transactional
    public TipoPermisoMaestroResponse actualizarTipoPermiso(Integer id, TipoPermisoRequest r) {
        TipoPermiso t = tipoPermisoRepository.findById(id).orElseThrow(() -> ApiException.notFound("Tipo de permiso no existe"));
        String codigo = codigo(r.codigo());
        if (VACACIONES.equalsIgnoreCase(t.getCodigo()) && !VACACIONES.equals(codigo)) {
            throw ApiException.badRequest("El código VACACIONES lo usan planilla y el saldo de vacaciones");
        }
        if (VACACIONES.equalsIgnoreCase(t.getCodigo()) && Boolean.FALSE.equals(r.activo())) {
            throw ApiException.badRequest("El tipo VACACIONES no puede desactivarse");
        }
        exigirLibre(tipoPermisoRepository.findByCodigoIgnoreCase(codigo).map(TipoPermiso::getIdTipoPermiso), t.getIdTipoPermiso(), "Ya existe un tipo de permiso con ese código");
        exigirLibre(tipoPermisoRepository.findByNombreIgnoreCase(trim(r.nombre())).map(TipoPermiso::getIdTipoPermiso), t.getIdTipoPermiso(), "Ya existe un tipo de permiso con ese nombre");
        aplicar(t, r, codigo);
        tipoPermisoRepository.save(t);
        auditar("ACTUALIZAR", "TIPO_PERMISO", t.getIdTipoPermiso(), t.getCodigo());
        return toTipo(t);
    }

    @Transactional(readOnly = true)
    public List<ParametroResponse> parametros() {
        return parametroRepository.findAll().stream()
                .sorted(Comparator.comparing(ParametroSistema::getClave, String.CASE_INSENSITIVE_ORDER))
                .map(this::toParametro)
                .toList();
    }

    @Transactional
    public ParametroResponse crearParametro(ParametroRequest r) {
        String clave = clave(r.clave());
        if (parametroRepository.existsById(clave)) {
            throw ApiException.conflict("Ya existe un parámetro con esa clave");
        }
        ParametroSistema p = new ParametroSistema();
        p.setClave(clave);
        p.setValor(trim(r.valor()));
        p.setDescripcion(blankToNull(r.descripcion()));
        parametroRepository.save(p);
        auditar("CREAR", "PARAMETRO", null, p.getClave());
        return toParametro(p);
    }

    @Transactional
    public ParametroResponse actualizarParametro(String clave, ParametroRequest r) {
        ParametroSistema p = parametroRepository.findById(clave)
                .orElseThrow(() -> ApiException.notFound("Parámetro no existe"));
        p.setValor(trim(r.valor()));
        if (r.descripcion() != null) {
            p.setDescripcion(blankToNull(r.descripcion()));
        }
        parametroRepository.save(p);
        auditar("ACTUALIZAR", "PARAMETRO", null, p.getClave());
        return toParametro(p);
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> cuentas() {
        return cuentaRepository.findAll().stream()
                .sorted(Comparator.comparing(CuentaContable::getCodigo, String.CASE_INSENSITIVE_ORDER))
                .map(this::toCuenta)
                .toList();
    }

    @Transactional
    public CuentaResponse crearCuenta(CuentaRequest r) {
        String codigo = codigo(r.codigo());
        String uso = codigo(r.uso());
        exigirLibre(cuentaRepository.findByCodigoIgnoreCase(codigo).map(CuentaContable::getIdCuenta), null, "Ya existe una cuenta con ese código");
        exigirLibre(cuentaRepository.findByUsoIgnoreCase(uso).map(CuentaContable::getIdCuenta), null, "Ese uso de cuenta ya está asignado");
        CuentaContable c = new CuentaContable();
        aplicar(c, r, codigo, uso);
        cuentaRepository.save(c);
        auditar("CREAR", "CUENTA", c.getIdCuenta(), c.getCodigo());
        return toCuenta(c);
    }

    @Transactional
    public CuentaResponse actualizarCuenta(Integer id, CuentaRequest r) {
        CuentaContable c = cuentaRepository.findById(id).orElseThrow(() -> ApiException.notFound("Cuenta no existe"));
        String codigo = codigo(r.codigo());
        String uso = codigo(r.uso());
        exigirLibre(cuentaRepository.findByCodigoIgnoreCase(codigo).map(CuentaContable::getIdCuenta), c.getIdCuenta(), "Ya existe una cuenta con ese código");
        exigirLibre(cuentaRepository.findByUsoIgnoreCase(uso).map(CuentaContable::getIdCuenta), c.getIdCuenta(), "Ese uso de cuenta ya está asignado");
        aplicar(c, r, codigo, uso);
        cuentaRepository.save(c);
        auditar("ACTUALIZAR", "CUENTA", c.getIdCuenta(), c.getCodigo());
        return toCuenta(c);
    }

    private void aplicar(Area a, AreaRequest r) {
        a.setNombre(trim(r.nombre()));
        a.setDescripcion(blankToNull(r.descripcion()));
        a.setActivo(r.activo() == null || r.activo());
    }

    private void aplicar(Cargo c, CargoRequest r) {
        c.setNombre(trim(r.nombre()));
        c.setDescripcion(blankToNull(r.descripcion()));
        c.setActivo(r.activo() == null || r.activo());
    }

    private void aplicar(HorarioLaboral h, HorarioRequest r) {
        h.setNombre(trim(r.nombre()));
        h.setHoraIngreso(r.horaIngreso());
        h.setHoraSalida(r.horaSalida());
        h.setMinutosRefrigerio(r.minutosRefrigerio() == null ? 60 : r.minutosRefrigerio());
        h.setActivo(r.activo() == null || r.activo());
    }

    private void aplicar(TipoPermiso t, TipoPermisoRequest r, String codigo) {
        t.setCodigo(codigo);
        t.setNombre(trim(r.nombre()));
        t.setRequiereSustento(Boolean.TRUE.equals(r.requiereSustento()));
        t.setActivo(r.activo() == null || r.activo());
    }

    private void aplicar(CuentaContable c, CuentaRequest r, String codigo, String uso) {
        c.setCodigo(codigo);
        c.setNombre(trim(r.nombre()));
        c.setUso(uso);
        String nat = r.naturaleza() == null || r.naturaleza().isBlank() ? "GASTO" : r.naturaleza().trim().toUpperCase(Locale.ROOT);
        c.setNaturaleza(nat);
        c.setActivo(r.activo() == null || r.activo());
    }

    private void validarHorario(HorarioRequest r) {
        if (!r.horaSalida().isAfter(r.horaIngreso())) {
            throw ApiException.badRequest("La hora de salida debe ser posterior al ingreso");
        }
    }

    private void exigirLibre(java.util.Optional<Integer> idEncontrado, Integer idActual, String mensaje) {
        idEncontrado.filter(id -> idActual == null || !id.equals(idActual))
                .ifPresent(id -> { throw ApiException.conflict(mensaje); });
    }

    private AreaResponse toArea(Area a) {
        return new AreaResponse(a.getIdArea(), a.getNombre(), a.getDescripcion(), a.getActivo());
    }

    private CargoResponse toCargo(Cargo c) {
        return new CargoResponse(c.getIdCargo(), c.getNombre(), c.getDescripcion(), c.getActivo());
    }

    private HorarioResponse toHorario(HorarioLaboral h) {
        return new HorarioResponse(h.getIdHorario(), h.getNombre(), h.getHoraIngreso(), h.getHoraSalida(),
                h.getMinutosRefrigerio(), h.getActivo());
    }

    private TipoPermisoMaestroResponse toTipo(TipoPermiso t) {
        return new TipoPermisoMaestroResponse(t.getIdTipoPermiso(), t.getCodigo(), t.getNombre(),
                t.getRequiereSustento(), t.getActivo());
    }

    private ParametroResponse toParametro(ParametroSistema p) {
        return new ParametroResponse(p.getClave(), p.getValor(), p.getDescripcion());
    }

    private CuentaResponse toCuenta(CuentaContable c) {
        return new CuentaResponse(c.getIdCuenta(), c.getCodigo(), c.getNombre(), c.getUso(), c.getNaturaleza(), c.getActivo());
    }

    private void auditar(String accion, String entidad, Integer id, String detalle) {
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), accion, entidad, id, detalle);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String codigo(String value) {
        return trim(value).toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String clave(String value) {
        return trim(value).toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
