package pe.andina.rrhh.web;

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
import pe.andina.rrhh.common.PageResponses;
import pe.andina.rrhh.dto.AppDtos.ContratoRequest;
import pe.andina.rrhh.dto.AppDtos.ContratoResponse;
import pe.andina.rrhh.dto.AppDtos.PageResponse;
import pe.andina.rrhh.dto.AppDtos.VacacionSaldoResponse;
import pe.andina.rrhh.service.ContratoService;

@RestController
@RequestMapping("/api/contratos")
@Tag(name = "Contratos", description = "Modalidad colaborador/practicante, horario y saldo de vacaciones")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    @Operation(summary = "Listar contratos paginado")
    public PageResponse<ContratoResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String estado) {
        var data = PageResponses.search(contratoService.listar(), q, c ->
                PageResponses.text(c.codigo(), c.empleado(), c.codigoEmpleado(), c.modalidad(), c.horario(), c.estado()));
        if (modalidad != null && !modalidad.isBlank()) {
            String m = modalidad.toUpperCase();
            data = data.stream().filter(c -> c.modalidad() != null && m.equals(c.modalidad().name())).toList();
        }
        if (estado != null && !estado.isBlank()) {
            String e = estado.toUpperCase();
            data = data.stream().filter(c -> c.estado() != null && e.equals(c.estado().name())).toList();
        }
        return PageResponses.of(data, page, size);
    }

    @GetMapping("/saldo-vacaciones")
    @Operation(summary = "Saldo de vacaciones (1.5 días por mes completo)")
    public VacacionSaldoResponse saldo(@RequestParam(required = false) Integer idEmpleado) {
        return contratoService.saldoVacaciones(idEmpleado);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public ContratoResponse obtener(@PathVariable Integer id) {
        return contratoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public ContratoResponse crear(@Valid @RequestBody ContratoRequest request) {
        return contratoService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public ContratoResponse actualizar(@PathVariable Integer id, @Valid @RequestBody ContratoRequest request) {
        return contratoService.actualizar(id, request);
    }
}
