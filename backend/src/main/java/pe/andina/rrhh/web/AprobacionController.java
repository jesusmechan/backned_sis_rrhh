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
    @Operation(summary = "Bandeja del usuario: pendientes o seguimiento, paginada")
    public PageResponse<BandejaItem> bandeja(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String vista,
            @RequestParam(required = false) String estado) {
        return PageResponses.query(solicitudService.bandeja(vista))
                .donde(tipo == null || tipo.isBlank() ? null : item -> tipo.equalsIgnoreCase(item.tipoSolicitud()))
                .estadosTexto(estado, BandejaItem::estadoSolicitud)
                .search(q, item -> PageResponses.text(
                        item.solicitante(), item.tipoSolicitud(), item.tipoTramite(), item.motivo(), item.estadoSolicitud()))
                .pagina(page, size);
    }

    @GetMapping("/pasos/{id}")
    @Operation(summary = "Obtener un paso de bandeja si le corresponde al usuario")
    public BandejaItem paso(@PathVariable Integer id) {
        return solicitudService.pasoPendiente(id);
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
