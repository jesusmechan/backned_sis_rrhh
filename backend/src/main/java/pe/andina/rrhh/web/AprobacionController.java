package pe.andina.rrhh.web;

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
import pe.andina.rrhh.common.PageResponses;
import pe.andina.rrhh.dto.AppDtos.BandejaItem;
import pe.andina.rrhh.dto.AppDtos.DecisionRequest;
import pe.andina.rrhh.dto.AppDtos.PageResponse;
import pe.andina.rrhh.dto.AppDtos.PasoResponse;
import pe.andina.rrhh.service.SolicitudService;

@RestController
@RequestMapping("/api")
@Tag(name = "Aprobación", description = "Bandeja y decisión de pasos")
public class AprobacionController {

    private final SolicitudService solicitudService;

    public AprobacionController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping("/bandeja")
    @Operation(summary = "Bandeja de pasos pendientes del usuario, paginada")
    public PageResponse<BandejaItem> bandeja(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String q) {
        var data = solicitudService.bandeja();
        if (tipo != null && !tipo.isBlank()) {
            data = data.stream().filter(item -> tipo.equalsIgnoreCase(item.tipoSolicitud())).toList();
        }
        return PageResponses.of(
                PageResponses.search(data, q, item -> PageResponses.text(
                        item.solicitante(), item.tipoSolicitud(), item.tipoTramite(), item.motivo())),
                page, size);
    }

    @PostMapping("/pasos/{id}/aprobar")
    @Operation(summary = "Aprobar un paso", description = "El id es idPasoSolicitud de la bandeja")
    public PasoResponse aprobar(@PathVariable Integer id, @Valid @RequestBody DecisionRequest request) {
        return solicitudService.decidir(id, true, request);
    }

    @PostMapping("/pasos/{id}/rechazar")
    @Operation(summary = "Rechazar un paso")
    public PasoResponse rechazar(@PathVariable Integer id, @Valid @RequestBody DecisionRequest request) {
        return solicitudService.decidir(id, false, request);
    }
}
