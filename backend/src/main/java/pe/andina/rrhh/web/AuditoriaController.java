package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.common.PageResponses;
import pe.andina.rrhh.dto.AppDtos.AuditoriaResponse;
import pe.andina.rrhh.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.dto.AppDtos.PageResponse;
import pe.andina.rrhh.service.ConsultaService;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','RRHH')")
@Tag(name = "Auditoría", description = "Bitácora y trazabilidad de solicitudes")
public class AuditoriaController {

    private final ConsultaService consultaService;

    public AuditoriaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping("/auditoria")
    public PageResponse<AuditoriaResponse> auditoria(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q) {
        return PageResponses.of(
                PageResponses.search(consultaService.auditoria(), q, a ->
                        PageResponses.text(a.usuario(), a.accion(), a.entidad(), a.detalle())),
                page, size);
    }

    @GetMapping("/trazabilidad")
    public PageResponse<HistorialResponse> trazabilidad(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q) {
        return PageResponses.of(
                PageResponses.search(consultaService.trazabilidad(), q, h ->
                        PageResponses.text(h.accion(), h.usuario(), h.comentario(), h.estadoAnterior(), h.estadoNuevo())),
                page, size);
    }
}
