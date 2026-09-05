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
import pe.andina.rrhh.dto.AppDtos.HoraExtraRequest;
import pe.andina.rrhh.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.service.SolicitudService;

import java.util.List;

@RestController
@RequestMapping("/api/horas-extras")
@Tag(name = "Horas extras", description = "Solicitudes de horas extras y su historial")
public class HoraExtraController {

    private final SolicitudService solicitudService;

    public HoraExtraController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public List<HoraExtraResponse> listar() {
        return solicitudService.listarHorasExtras();
    }

    @GetMapping("/{id}")
    public HoraExtraResponse obtener(@PathVariable Integer id) {
        return solicitudService.obtenerHoraExtra(id);
    }

    @PostMapping
    @Operation(summary = "Registrar horas extras")
    public HoraExtraResponse crear(@Valid @RequestBody HoraExtraRequest request) {
        return solicitudService.crearHoraExtra(request);
    }

    @PostMapping("/{id}/cancelar")
    public HoraExtraResponse cancelar(@PathVariable Integer id) {
        return solicitudService.cancelarHoraExtra(id);
    }

    @GetMapping("/{id}/historial")
    public List<HistorialResponse> historial(@PathVariable Integer id) {
        return solicitudService.historialHoraExtra(id);
    }
}
