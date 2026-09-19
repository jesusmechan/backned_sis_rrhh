package pe.andina.rrhh.adapter.in.web;

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
import pe.andina.rrhh.application.dto.AppDtos.FlujoRequest;
import pe.andina.rrhh.application.dto.AppDtos.FlujoResponse;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.port.in.FlujoUseCase;

@RestController
@RequestMapping("/api/flujos")
@Tag(name = "Flujos", description = "Configuración de circuitos de aprobación")
public class FlujoController {

    private final FlujoUseCase flujoService;

    public FlujoController(FlujoUseCase flujoService) {
        this.flujoService = flujoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public PageResponse<FlujoResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipoOrigen,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(flujoService.listar())
                .search(q, f -> PageResponses.text(f.codigo(), f.nombre(), f.tipoOrigen(), f.tipoPermiso(), f.descripcion()))
                .donde(tipoOrigen == null || tipoOrigen.isBlank() ? null
                        : f -> tipoOrigen.equalsIgnoreCase(String.valueOf(f.tipoOrigen())))
                .donde(activo == null ? null : f -> activo.equals(f.activo()))
                .pagina(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public FlujoResponse obtener(@PathVariable Integer id) {
        return flujoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public FlujoResponse crear(@Valid @RequestBody FlujoRequest request) {
        return flujoService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public FlujoResponse actualizar(@PathVariable Integer id, @Valid @RequestBody FlujoRequest request) {
        return flujoService.actualizar(id, request);
    }
}
