package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.common.PageResponses;
import pe.andina.rrhh.dto.AppDtos.MarcacionRequest;
import pe.andina.rrhh.dto.AppDtos.MarcacionResponse;
import pe.andina.rrhh.dto.AppDtos.PageResponse;
import pe.andina.rrhh.service.AsistenciaService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/asistencias")
@Tag(name = "Asistencia", description = "Marcaciones de ingreso y salida")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @PostMapping("/marcar")
    @Operation(summary = "Registrar marcación de ingreso o salida")
    public MarcacionResponse marcar(@Valid @RequestBody MarcacionRequest request) {
        return asistenciaService.marcar(request);
    }

    @GetMapping
    @Operation(summary = "Listar marcaciones paginado")
    public PageResponse<MarcacionResponse> listar(
            @RequestParam(required = false) Integer idEmpleado,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q) {
        var data = asistenciaService.listar(idEmpleado, desde, hasta);
        if (tipo != null && !tipo.isBlank()) {
            data = data.stream().filter(m -> tipo.equalsIgnoreCase(m.tipo().name())).toList();
        }
        return PageResponses.of(
                PageResponses.search(data, q, m -> PageResponses.text(m.empleado(), m.origen(), m.observacion())),
                page, size);
    }

    @GetMapping("/{id}")
    public MarcacionResponse obtener(@PathVariable Integer id) {
        return asistenciaService.obtener(id);
    }

    @PutMapping("/{id}")
    public MarcacionResponse actualizar(@PathVariable Integer id, @RequestBody MarcacionRequest request) {
        return asistenciaService.actualizar(id, request);
    }
}
