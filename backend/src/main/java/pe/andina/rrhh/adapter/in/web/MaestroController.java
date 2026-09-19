package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.PageResponses;
import pe.andina.rrhh.application.dto.AppDtos.AreaRequest;
import pe.andina.rrhh.application.dto.AppDtos.AreaResponse;
import pe.andina.rrhh.application.dto.AppDtos.CargoRequest;
import pe.andina.rrhh.application.dto.AppDtos.CargoResponse;
import pe.andina.rrhh.application.dto.AppDtos.CuentaRequest;
import pe.andina.rrhh.application.dto.AppDtos.CuentaResponse;
import pe.andina.rrhh.application.dto.AppDtos.HorarioRequest;
import pe.andina.rrhh.application.dto.AppDtos.HorarioResponse;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.dto.AppDtos.ParametroRequest;
import pe.andina.rrhh.application.dto.AppDtos.ParametroResponse;
import pe.andina.rrhh.application.dto.AppDtos.TipoPermisoMaestroResponse;
import pe.andina.rrhh.application.dto.AppDtos.TipoPermisoRequest;
import pe.andina.rrhh.application.port.in.MaestroUseCase;

@RestController
@RequestMapping("/api/maestros")
@PreAuthorize("hasAnyRole('ADMIN','RRHH')")
@Tag(name = "Maestros", description = "Áreas, cargos, horarios, tipos de permiso, parámetros y cuentas contables")
public class MaestroController {

    private final MaestroUseCase maestroService;

    public MaestroController(MaestroUseCase maestroService) {
        this.maestroService = maestroService;
    }

    @GetMapping("/areas")
    @Operation(summary = "Listar áreas")
    public PageResponse<AreaResponse> areas(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(maestroService.areas())
                .search(q, a -> PageResponses.text(a.nombre(), a.descripcion()))
                .donde(activo == null ? null : a -> activo.equals(a.activo()))
                .pagina(page, size);
    }

    @PostMapping("/areas")
    public AreaResponse crearArea(@Valid @RequestBody AreaRequest request) {
        return maestroService.crearArea(request);
    }

    @PutMapping("/areas/{id}")
    public AreaResponse actualizarArea(@PathVariable Integer id, @Valid @RequestBody AreaRequest request) {
        return maestroService.actualizarArea(id, request);
    }

    @GetMapping("/cargos")
    public PageResponse<CargoResponse> cargos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(maestroService.cargos())
                .search(q, c -> PageResponses.text(c.nombre(), c.descripcion()))
                .donde(activo == null ? null : c -> activo.equals(c.activo()))
                .pagina(page, size);
    }

    @PostMapping("/cargos")
    public CargoResponse crearCargo(@Valid @RequestBody CargoRequest request) {
        return maestroService.crearCargo(request);
    }

    @PutMapping("/cargos/{id}")
    public CargoResponse actualizarCargo(@PathVariable Integer id, @Valid @RequestBody CargoRequest request) {
        return maestroService.actualizarCargo(id, request);
    }

    @GetMapping("/horarios")
    public PageResponse<HorarioResponse> horarios(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(maestroService.horarios())
                .search(q, h -> PageResponses.text(h.nombre(), String.valueOf(h.horaIngreso()), String.valueOf(h.horaSalida())))
                .donde(activo == null ? null : h -> activo.equals(h.activo()))
                .pagina(page, size);
    }

    @PostMapping("/horarios")
    public HorarioResponse crearHorario(@Valid @RequestBody HorarioRequest request) {
        return maestroService.crearHorario(request);
    }

    @PutMapping("/horarios/{id}")
    public HorarioResponse actualizarHorario(@PathVariable Integer id, @Valid @RequestBody HorarioRequest request) {
        return maestroService.actualizarHorario(id, request);
    }

    @GetMapping("/tipos-permiso")
    public PageResponse<TipoPermisoMaestroResponse> tiposPermiso(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(maestroService.tiposPermiso())
                .search(q, t -> PageResponses.text(t.codigo(), t.nombre()))
                .donde(activo == null ? null : t -> activo.equals(t.activo()))
                .pagina(page, size);
    }

    @PostMapping("/tipos-permiso")
    public TipoPermisoMaestroResponse crearTipo(@Valid @RequestBody TipoPermisoRequest request) {
        return maestroService.crearTipoPermiso(request);
    }

    @PutMapping("/tipos-permiso/{id}")
    public TipoPermisoMaestroResponse actualizarTipo(@PathVariable Integer id, @Valid @RequestBody TipoPermisoRequest request) {
        return maestroService.actualizarTipoPermiso(id, request);
    }

    @GetMapping("/parametros")
    public PageResponse<ParametroResponse> parametros(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q) {
        return PageResponses.query(maestroService.parametros())
                .search(q, p -> PageResponses.text(p.clave(), p.valor(), p.descripcion()))
                .pagina(page, size);
    }

    @PostMapping("/parametros")
    public ParametroResponse crearParametro(@Valid @RequestBody ParametroRequest request) {
        return maestroService.crearParametro(request);
    }

    @PutMapping("/parametros/{clave}")
    public ParametroResponse actualizarParametro(@PathVariable String clave, @Valid @RequestBody ParametroRequest request) {
        return maestroService.actualizarParametro(clave, request);
    }

    @GetMapping("/cuentas")
    public PageResponse<CuentaResponse> cuentas(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(maestroService.cuentas())
                .search(q, c -> PageResponses.text(c.codigo(), c.nombre(), c.uso(), c.naturaleza()))
                .donde(activo == null ? null : c -> activo.equals(c.activo()))
                .pagina(page, size);
    }

    @PostMapping("/cuentas")
    public CuentaResponse crearCuenta(@Valid @RequestBody CuentaRequest request) {
        return maestroService.crearCuenta(request);
    }

    @PutMapping("/cuentas/{id}")
    public CuentaResponse actualizarCuenta(@PathVariable Integer id, @Valid @RequestBody CuentaRequest request) {
        return maestroService.actualizarCuenta(id, request);
    }
}
