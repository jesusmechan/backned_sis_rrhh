package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.PageResponses;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.dto.AppDtos.PlanillaRequest;
import pe.andina.rrhh.application.dto.AppDtos.PlanillaResponse;
import pe.andina.rrhh.application.port.in.PlanillaUseCase;
import pe.andina.rrhh.application.dto.BoletaPdfFile;

@RestController
@RequestMapping("/api/planillas")
@PreAuthorize("hasAnyRole('ADMIN','RRHH')")
@Tag(name = "Planillas", description = "Cálculo mensual de remuneraciones")
public class PlanillaController {

    private final PlanillaUseCase planillaService;

    public PlanillaController(PlanillaUseCase planillaService) {
        this.planillaService = planillaService;
    }

    @GetMapping
    public PageResponse<PlanillaResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado) {
        return PageResponses.query(planillaService.listar())
                .estados(estado, PlanillaResponse::estado)
                .search(q, p -> PageResponses.text(p.periodo(), p.estado(), p.observaciones()))
                .pagina(page, size);
    }

    @GetMapping("/{id}")
    public PlanillaResponse obtener(@PathVariable Integer id) {
        return planillaService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Abrir planilla del periodo")
    public PlanillaResponse crear(@Valid @RequestBody PlanillaRequest request) {
        return planillaService.crear(request);
    }

    @PostMapping("/{id}/calcular")
    @Operation(summary = "Calcular boletas con sueldo, horas extras y descuentos")
    public PlanillaResponse calcular(@PathVariable Integer id) {
        return planillaService.calcular(id);
    }

    @PostMapping("/{id}/cerrar")
    @Operation(summary = "Cerrar planilla y generar asiento contable")
    public PlanillaResponse cerrar(@PathVariable Integer id) {
        return planillaService.cerrar(id);
    }

    @GetMapping("/{id}/boletas/pdf")
    @Operation(summary = "Descargar todas las boletas del periodo en un PDF")
    public ResponseEntity<byte[]> pdfTodas(@PathVariable Integer id) {
        return archivo(planillaService.pdfBoletas(id));
    }

    @GetMapping("/{id}/boletas/{idDetalle}/pdf")
    @Operation(summary = "Descargar la boleta de un trabajador")
    public ResponseEntity<byte[]> pdfUna(@PathVariable Integer id, @PathVariable Integer idDetalle) {
        return archivo(planillaService.pdfBoleta(id, idDetalle));
    }

    private ResponseEntity<byte[]> archivo(BoletaPdfFile pdf) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.nombre() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.contenido());
    }
}
