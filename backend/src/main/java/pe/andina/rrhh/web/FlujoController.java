package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.dto.AppDtos.FlujoRequest;
import pe.andina.rrhh.dto.AppDtos.FlujoResponse;
import pe.andina.rrhh.service.FlujoService;

import java.util.List;

@RestController
@RequestMapping("/api/flujos")
@Tag(name = "Flujos", description = "Configuración de circuitos de aprobación")
public class FlujoController {

    private final FlujoService flujoService;

    public FlujoController(FlujoService flujoService) {
        this.flujoService = flujoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public List<FlujoResponse> listar() {
        return flujoService.listar();
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
