package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.common.PageResponses;
import pe.andina.rrhh.dto.AppDtos.EvaluacionRequest;
import pe.andina.rrhh.dto.AppDtos.EvaluacionResponse;
import pe.andina.rrhh.dto.AppDtos.PageResponse;
import pe.andina.rrhh.service.DesempenoService;

@RestController
@RequestMapping("/api/evaluaciones")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Desempeño", description = "Evaluación periódica del colaborador")
public class DesempenoController {

    private final DesempenoService desempenoService;

    public DesempenoController(DesempenoService desempenoService) {
        this.desempenoService = desempenoService;
    }

    @GetMapping
    public PageResponse<EvaluacionResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q) {
        return PageResponses.query(desempenoService.listar())
                .search(q, e -> PageResponses.text(e.empleado(), e.evaluador(), e.periodo(), e.comentario()))
                .pagina(page, size);
    }

    @GetMapping("/{id}")
    public EvaluacionResponse obtener(@PathVariable Integer id) {
        return desempenoService.obtener(id);
    }

    @PostMapping
    public EvaluacionResponse crear(@Valid @RequestBody EvaluacionRequest request) {
        return desempenoService.crear(request);
    }
}
