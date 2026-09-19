package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.PageResponses;
import pe.andina.rrhh.application.dto.AppDtos.ConvocatoriaRequest;
import pe.andina.rrhh.application.dto.AppDtos.ConvocatoriaResponse;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.dto.AppDtos.PostulacionRequest;
import pe.andina.rrhh.application.dto.AppDtos.PostulacionResponse;
import pe.andina.rrhh.application.port.in.ReclutamientoUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/convocatorias")
@PreAuthorize("hasAnyRole('ADMIN','RRHH')")
@Tag(name = "Reclutamiento", description = "Convocatorias y postulantes")
public class ReclutamientoController {

    private final ReclutamientoUseCase reclutamientoService;

    public ReclutamientoController(ReclutamientoUseCase reclutamientoService) {
        this.reclutamientoService = reclutamientoService;
    }

    @GetMapping
    public PageResponse<ConvocatoriaResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado) {
        return PageResponses.query(reclutamientoService.listar())
                .estados(estado, ConvocatoriaResponse::estado)
                .search(q, c -> PageResponses.text(c.codigo(), c.puesto(), c.area(), c.descripcion()))
                .pagina(page, size);
    }

    @GetMapping("/{id}")
    public ConvocatoriaResponse obtener(@PathVariable Integer id) {
        return reclutamientoService.obtener(id);
    }

    @PostMapping
    public ConvocatoriaResponse crear(@Valid @RequestBody ConvocatoriaRequest request) {
        return reclutamientoService.crear(request);
    }

    @PutMapping("/{id}")
    public ConvocatoriaResponse actualizar(@PathVariable Integer id, @Valid @RequestBody ConvocatoriaRequest request) {
        return reclutamientoService.actualizar(id, request);
    }

    @GetMapping("/{id}/postulaciones")
    public List<PostulacionResponse> postulaciones(@PathVariable Integer id) {
        return reclutamientoService.postulaciones(id);
    }

    @PostMapping("/{id}/postulaciones")
    public PostulacionResponse registrar(@PathVariable Integer id, @Valid @RequestBody PostulacionRequest request) {
        return reclutamientoService.registrarPostulacion(id, request);
    }

    @PutMapping("/postulaciones/{idPostulacion}")
    public PostulacionResponse actualizarPostulacion(@PathVariable Integer idPostulacion,
                                                     @Valid @RequestBody PostulacionRequest request) {
        return reclutamientoService.actualizarPostulacion(idPostulacion, request);
    }
}
