package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.application.dto.AppDtos.CesarOrganigramaRequest;
import pe.andina.rrhh.application.dto.AppDtos.OrganigramaAreaResponse;
import pe.andina.rrhh.application.dto.AppDtos.OrganigramaNodoResponse;
import pe.andina.rrhh.application.dto.AppDtos.OrganigramaResponse;
import pe.andina.rrhh.application.dto.AppDtos.PromoverOrganigramaRequest;
import pe.andina.rrhh.application.dto.AppDtos.ReasignarOrganigramaRequest;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import pe.andina.rrhh.application.port.in.OrganigramaUseCase;
import pe.andina.rrhh.application.port.out.AreaPort;
import pe.andina.rrhh.application.port.out.CargoPort;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.out.EmpleadoPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Area;
import pe.andina.rrhh.domain.model.Cargo;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.model.enums.NivelJerarquico;
import pe.andina.rrhh.domain.service.OrganigramaReglas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrganigramaService implements OrganigramaUseCase {

    private final EmpleadoPort empleadoRepository;
    private final AreaPort areaRepository;
    private final CargoPort cargoRepository;
    private final OrganigramaReglas reglas;
    private final AuditoriaUseCase auditoriaService;
    private final CurrentUserPort currentUser;

    public OrganigramaService(EmpleadoPort empleadoRepository,
                              AreaPort areaRepository,
                              CargoPort cargoRepository,
                              OrganigramaReglas reglas,
                              AuditoriaUseCase auditoriaService,
                              CurrentUserPort currentUser) {
        this.empleadoRepository = empleadoRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.reglas = reglas;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public OrganigramaResponse arbol(boolean incluirInactivos) {
        List<Empleado> todos = empleadoRepository.findAll();
        return construirArbol(todos, incluirInactivos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganigramaAreaResponse> areas(boolean incluirInactivos) {
        List<Empleado> todos = empleadoRepository.findAll();
        Map<Integer, List<Empleado>> porArea = todos.stream()
                .filter(e -> e.getArea() != null && e.getArea().getIdArea() != null)
                .filter(e -> incluirInactivos || reglas.esActivo(e))
                .collect(Collectors.groupingBy(e -> e.getArea().getIdArea()));

        return areaRepository.findAll().stream()
                .sorted(Comparator.comparing(Area::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(area -> {
                    List<Empleado> delArea = porArea.getOrDefault(area.getIdArea(), List.of());
                    Empleado jefe = delArea.stream()
                            .filter(e -> reglas.nivelDe(e) == NivelJerarquico.JEFE_AREA)
                            .filter(reglas::esActivo)
                            .max(Comparator.comparingInt(e -> reportesDirectos(todos, e.getIdEmpleado(), true).size()))
                            .orElse(null);
                    List<OrganigramaNodoResponse> colaboradores = delArea.stream()
                            .filter(e -> jefe == null || !Objects.equals(e.getIdEmpleado(), jefe.getIdEmpleado()))
                            .sorted(Comparator.comparing(Empleado::nombreCompleto, String.CASE_INSENSITIVE_ORDER))
                            .map(e -> aNodoPlano(e, todos, incluirInactivos))
                            .toList();
                    return new OrganigramaAreaResponse(
                            area.getIdArea(),
                            area.getNombre(),
                            jefe == null ? null : aNodoPlano(jefe, todos, incluirInactivos),
                            colaboradores
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganigramaNodoResponse equipo(Integer idEmpleado, boolean incluirInactivos) {
        List<Empleado> todos = empleadoRepository.findAll();
        Empleado jefe = buscar(idEmpleado, todos);
        return aNodoConHijos(jefe, hijosPorJefe(todos, incluirInactivos), incluirInactivos, new HashSet<>());
    }

    @Override
    @Transactional
    public OrganigramaResponse reasignar(ReasignarOrganigramaRequest request) {
        List<Empleado> todos = empleadoRepository.findAll();
        Empleado nuevoJefe = request.idNuevoJefe() == null ? null : buscar(request.idNuevoJefe(), todos);
        if (nuevoJefe != null) {
            reglas.exigirActivo(nuevoJefe, "El nuevo jefe debe estar activo");
        }

        Set<Integer> ids = new HashSet<>(request.idsEmpleado());
        for (Integer id : ids) {
            Empleado e = buscar(id, todos);
            reglas.validarNuevaJefatura(e, nuevoJefe, todos);
            e.setJefeInmediato(nuevoJefe);
            empleadoRepository.save(e);
        }

        auditoriaService.registrar(currentUser.usuario(), "REASIGNAR", "ORGANIGRAMA",
                request.idNuevoJefe(), "empleados=" + ids);
        return construirArbol(empleadoRepository.findAll(), false);
    }

    @Override
    @Transactional
    public OrganigramaResponse promover(PromoverOrganigramaRequest request) {
        List<Empleado> todos = empleadoRepository.findAll();
        Empleado promovido = buscar(request.idEmpleado(), todos);
        reglas.exigirActivo(promovido, "Solo se puede promover a un colaborador activo");

        Cargo cargoNuevo = cargoRepository.findById(request.idCargoNuevo())
                .orElseThrow(() -> DomainException.badRequest("Cargo destino no existe"));
        if (!Boolean.TRUE.equals(cargoNuevo.getActivo())) {
            throw DomainException.badRequest("El cargo destino está inactivo");
        }

        if (request.idArea() != null) {
            Area area = areaRepository.findById(request.idArea())
                    .orElseThrow(() -> DomainException.badRequest("Área no existe"));
            promovido.setArea(area);
        }

        promovido.setCargo(cargoNuevo);

        Empleado nuevoJefePromovido = request.idNuevoJefeDelPromovido() == null
                ? promovido.getJefeInmediato()
                : buscar(request.idNuevoJefeDelPromovido(), todos);
        reglas.validarNuevaJefatura(promovido, nuevoJefePromovido, todos);
        promovido.setJefeInmediato(nuevoJefePromovido);
        empleadoRepository.save(promovido);

        if (request.idJefeSaliente() != null) {
            Empleado saliente = buscar(request.idJefeSaliente(), todos);
            Empleado jefeDelSaliente = request.idNuevoJefeDelSaliente() == null
                    ? promovido
                    : buscar(request.idNuevoJefeDelSaliente(), todos);
            reglas.validarNuevaJefatura(saliente, jefeDelSaliente, todos);
            saliente.setJefeInmediato(jefeDelSaliente);
            empleadoRepository.save(saliente);

            if (Boolean.TRUE.equals(request.asumirEquipoDelJefeActual())) {
                reasignarReportes(saliente.getIdEmpleado(), promovido, todos);
            }
        } else if (Boolean.TRUE.equals(request.asumirEquipoDelJefeActual())
                && promovido.getJefeInmediato() != null
                && promovido.getJefeInmediato().getIdEmpleado() != null) {
            // Sin jefe saliente explícito: no se reasigna equipo automáticamente.
        }

        auditoriaService.registrar(currentUser.usuario(), "PROMOVER", "ORGANIGRAMA",
                promovido.getIdEmpleado(), promovido.getCodigoEmpleado() + " -> " + cargoNuevo.getNombre());
        return construirArbol(empleadoRepository.findAll(), false);
    }

    @Override
    @Transactional
    public OrganigramaResponse cesar(CesarOrganigramaRequest request) {
        List<Empleado> todos = empleadoRepository.findAll();
        Empleado cesado = buscar(request.idEmpleado(), todos);
        if (cesado.getEstado() == EstadoEmpleado.CESADO) {
            throw DomainException.conflict("El trabajador ya está cesado");
        }

        Empleado sucesor = null;
        if (request.idSucesor() != null) {
            sucesor = buscar(request.idSucesor(), todos);
            reglas.exigirActivo(sucesor, "El sucesor debe estar activo");
            if (request.idCargoSucesor() != null) {
                Cargo cargo = cargoRepository.findById(request.idCargoSucesor())
                        .orElseThrow(() -> DomainException.badRequest("Cargo del sucesor no existe"));
                sucesor.setCargo(cargo);
                sucesor.setArea(cesado.getArea());
                // El sucesor reporta a quien reportaba el cesado
                reglas.validarNuevaJefatura(sucesor, cesado.getJefeInmediato(), todos);
                sucesor.setJefeInmediato(cesado.getJefeInmediato());
                empleadoRepository.save(sucesor);
            }
        }

        List<Empleado> reportesActivos = reportesDirectos(todos, cesado.getIdEmpleado(), true);
        Empleado nuevoJefeReportes = sucesor;
        if (nuevoJefeReportes == null && request.idNuevoJefeDeReportes() != null) {
            nuevoJefeReportes = buscar(request.idNuevoJefeDeReportes(), todos);
        }

        if (!reportesActivos.isEmpty() && nuevoJefeReportes == null) {
            throw DomainException.badRequest(
                    "El trabajador tiene " + reportesActivos.size()
                            + " reporte(s) activo(s). Indique idSucesor o idNuevoJefeDeReportes");
        }

        if (nuevoJefeReportes != null) {
            reglas.exigirActivo(nuevoJefeReportes, "El nuevo jefe de los reportes debe estar activo");
            for (Empleado reporte : reportesActivos) {
                if (Objects.equals(reporte.getIdEmpleado(), nuevoJefeReportes.getIdEmpleado())) {
                    continue;
                }
                reglas.validarNuevaJefatura(reporte, nuevoJefeReportes, todos);
                reporte.setJefeInmediato(nuevoJefeReportes);
                empleadoRepository.save(reporte);
            }
        }

        cesado.setEstado(EstadoEmpleado.CESADO);
        cesado.setFechaCese(request.fechaCese() != null ? request.fechaCese() : LocalDate.now());
        cesado.setJefeInmediato(null);
        empleadoRepository.save(cesado);

        auditoriaService.registrar(currentUser.usuario(), "CESAR", "ORGANIGRAMA",
                cesado.getIdEmpleado(), cesado.getCodigoEmpleado());
        return construirArbol(empleadoRepository.findAll(), false);
    }

    private void reasignarReportes(Integer idJefeAnterior, Empleado nuevoJefe, List<Empleado> todos) {
        for (Empleado reporte : reportesDirectos(todos, idJefeAnterior, true)) {
            if (Objects.equals(reporte.getIdEmpleado(), nuevoJefe.getIdEmpleado())) {
                continue;
            }
            reglas.validarNuevaJefatura(reporte, nuevoJefe, todos);
            reporte.setJefeInmediato(nuevoJefe);
            empleadoRepository.save(reporte);
        }
    }

    private OrganigramaResponse construirArbol(List<Empleado> todos, boolean incluirInactivos) {
        Map<Integer, List<Empleado>> hijos = hijosPorJefe(todos, incluirInactivos);
        List<Empleado> visibles = todos.stream()
                .filter(e -> incluirInactivos || reglas.esActivo(e))
                .toList();

        Set<Integer> idsVisibles = visibles.stream().map(Empleado::getIdEmpleado).collect(Collectors.toSet());
        List<OrganigramaNodoResponse> raices = visibles.stream()
                .filter(e -> e.getJefeInmediato() == null
                        || e.getJefeInmediato().getIdEmpleado() == null
                        || !idsVisibles.contains(e.getJefeInmediato().getIdEmpleado()))
                .sorted(Comparator.comparing(Empleado::nombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .map(e -> aNodoConHijos(e, hijos, incluirInactivos, new HashSet<>()))
                .toList();

        int activos = (int) todos.stream().filter(reglas::esActivo).count();
        return new OrganigramaResponse(activos, visibles.size(), raices);
    }

    private Map<Integer, List<Empleado>> hijosPorJefe(List<Empleado> todos, boolean incluirInactivos) {
        Map<Integer, List<Empleado>> map = new HashMap<>();
        for (Empleado e : todos) {
            if (!incluirInactivos && !reglas.esActivo(e)) {
                continue;
            }
            if (e.getJefeInmediato() == null || e.getJefeInmediato().getIdEmpleado() == null) {
                continue;
            }
            map.computeIfAbsent(e.getJefeInmediato().getIdEmpleado(), k -> new ArrayList<>()).add(e);
        }
        for (List<Empleado> lista : map.values()) {
            lista.sort(Comparator.comparing(Empleado::nombreCompleto, String.CASE_INSENSITIVE_ORDER));
        }
        return map;
    }

    private OrganigramaNodoResponse aNodoConHijos(Empleado e,
                                                  Map<Integer, List<Empleado>> hijos,
                                                  boolean incluirInactivos,
                                                  Set<Integer> enCamino) {
        if (e.getIdEmpleado() != null && !enCamino.add(e.getIdEmpleado())) {
            return aNodoPlano(e, List.of(), incluirInactivos);
        }
        List<Empleado> reportes = hijos.getOrDefault(e.getIdEmpleado(), List.of());
        List<OrganigramaNodoResponse> hijosNodos = reportes.stream()
                .map(hijo -> aNodoConHijos(hijo, hijos, incluirInactivos, new HashSet<>(enCamino)))
                .toList();
        return new OrganigramaNodoResponse(
                e.getIdEmpleado(),
                e.getCodigoEmpleado(),
                e.nombreCompleto(),
                e.getArea() != null ? e.getArea().getIdArea() : null,
                e.getArea() != null ? e.getArea().getNombre() : null,
                e.getCargo() != null ? e.getCargo().getIdCargo() : null,
                e.getCargo() != null ? e.getCargo().getNombre() : null,
                reglas.nivelDe(e),
                e.getEstado(),
                e.getJefeInmediato() != null ? e.getJefeInmediato().getIdEmpleado() : null,
                e.getJefeInmediato() != null ? e.getJefeInmediato().nombreCompleto() : null,
                hijosNodos.size(),
                hijosNodos
        );
    }

    private OrganigramaNodoResponse aNodoPlano(Empleado e, List<Empleado> todos, boolean incluirInactivos) {
        // Con incluirInactivos=false solo cuenta reportes activos.
        int reportes = reportesDirectos(todos, e.getIdEmpleado(), !incluirInactivos).size();
        return new OrganigramaNodoResponse(
                e.getIdEmpleado(),
                e.getCodigoEmpleado(),
                e.nombreCompleto(),
                e.getArea() != null ? e.getArea().getIdArea() : null,
                e.getArea() != null ? e.getArea().getNombre() : null,
                e.getCargo() != null ? e.getCargo().getIdCargo() : null,
                e.getCargo() != null ? e.getCargo().getNombre() : null,
                reglas.nivelDe(e),
                e.getEstado(),
                e.getJefeInmediato() != null ? e.getJefeInmediato().getIdEmpleado() : null,
                e.getJefeInmediato() != null ? e.getJefeInmediato().nombreCompleto() : null,
                reportes,
                List.of()
        );
    }

    private List<Empleado> reportesDirectos(List<Empleado> todos, Integer idJefe, boolean soloActivos) {
        if (idJefe == null) {
            return List.of();
        }
        return todos.stream()
                .filter(e -> e.getJefeInmediato() != null
                        && Objects.equals(e.getJefeInmediato().getIdEmpleado(), idJefe))
                .filter(e -> !soloActivos || reglas.esActivo(e))
                .toList();
    }

    private Empleado buscar(Integer id, List<Empleado> todos) {
        return todos.stream()
                .filter(e -> Objects.equals(e.getIdEmpleado(), id))
                .findFirst()
                .orElseGet(() -> empleadoRepository.findById(id)
                        .orElseThrow(() -> DomainException.notFound("Trabajador no encontrado")));
    }
}
