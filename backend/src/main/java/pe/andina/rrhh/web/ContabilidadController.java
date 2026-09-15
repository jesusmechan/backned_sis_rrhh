package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.common.PageResponses;
import pe.andina.rrhh.dto.AppDtos.AsientoResponse;
import pe.andina.rrhh.dto.AppDtos.PageResponse;
import pe.andina.rrhh.service.PlanillaService;

@RestController
@RequestMapping("/api/asientos")
@PreAuthorize("hasAnyRole('ADMIN','RRHH')")
@Tag(name = "Contabilidad", description = "Asientos de planilla")
public class ContabilidadController {

    private final PlanillaService planillaService;

    public ContabilidadController(PlanillaService planillaService) {
        this.planillaService = planillaService;
    }

    @GetMapping
    public PageResponse<AsientoResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado) {
        return PageResponses.query(planillaService.listarAsientos())
                .estados(estado, AsientoResponse::estado)
                .search(q, a -> PageResponses.text(a.codigo(), a.glosa(), a.periodoPlanilla(), a.estado()))
                .pagina(page, size);
    }

    @GetMapping("/{id}")
    public AsientoResponse obtener(@PathVariable Integer id) {
        return planillaService.obtenerAsiento(id);
    }
}
