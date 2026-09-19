package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.PageResponses;
import pe.andina.rrhh.application.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraRequest;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.port.in.SolicitudUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/horas-extras")
@Tag(name = "Horas extras", description = "Solicitudes de horas extras y su historial")
public class HoraExtraController {

    private final SolicitudUseCase solicitudService;

    public HoraExtraController(SolicitudUseCase solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    @Operation(summary = "Listar horas extras paginado")
    public PageResponse<HoraExtraResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q) {
        return PageResponses.query(solicitudService.listarHorasExtras())
                .estados(estado, HoraExtraResponse::estado)
                .search(q, h -> PageResponses.text(h.empleado(), h.motivo()))
                .pagina(page, size);
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
