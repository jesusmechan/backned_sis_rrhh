package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.service.SolicitudService;

import java.util.List;

@RestController
@RequestMapping("/api/permisos")
@Tag(name = "Permisos", description = "Solicitudes de permiso y su historial")
public class PermisoController {

    private final SolicitudService solicitudService;

    public PermisoController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public List<PermisoResponse> listar() {
        return solicitudService.listarPermisos();
    }

    @GetMapping("/{id}")
    public PermisoResponse obtener(@PathVariable Integer id) {
        return solicitudService.obtenerPermiso(id);
    }

    @PostMapping
    @Operation(summary = "Registrar permiso", description = "PostgreSQL instancia el flujo de aprobación al insertar")
    public PermisoResponse crear(@Valid @RequestBody PermisoRequest request) {
        return solicitudService.crearPermiso(request);
    }

    @PostMapping("/{id}/cancelar")
    public PermisoResponse cancelar(@PathVariable Integer id) {
        return solicitudService.cancelarPermiso(id);
    }

    @GetMapping("/{id}/historial")
    public List<HistorialResponse> historial(@PathVariable Integer id) {
        return solicitudService.historialPermiso(id);
    }
}
