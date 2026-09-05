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
import pe.andina.rrhh.dto.AppDtos.BandejaItem;
import pe.andina.rrhh.dto.AppDtos.DecisionRequest;
import pe.andina.rrhh.dto.AppDtos.PasoResponse;
import pe.andina.rrhh.service.SolicitudService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Aprobación", description = "Bandeja y decisión de pasos")
public class AprobacionController {

    private final SolicitudService solicitudService;

    public AprobacionController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping("/bandeja")
    @Operation(summary = "Bandeja de pasos pendientes del usuario")
    public List<BandejaItem> bandeja() {
        return solicitudService.bandeja();
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
