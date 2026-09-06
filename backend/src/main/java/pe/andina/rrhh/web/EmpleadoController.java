package pe.andina.rrhh.web;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.andina.rrhh.common.PageResponses;
import pe.andina.rrhh.dto.AppDtos.CargaResponse;
import pe.andina.rrhh.dto.AppDtos.EmpleadoRequest;
import pe.andina.rrhh.dto.AppDtos.EmpleadoResponse;
import pe.andina.rrhh.dto.AppDtos.PageResponse;
import pe.andina.rrhh.service.CargaExcelService;
import pe.andina.rrhh.service.EmpleadoService;

@RestController
@RequestMapping("/api/empleados")
@Tag(name = "Personal", description = "CRUD de empleados y carga masiva Excel")
public class EmpleadoController {

    private final EmpleadoService empleadoService;
    private final CargaExcelService cargaExcelService;

    public EmpleadoController(EmpleadoService empleadoService, CargaExcelService cargaExcelService) {
        this.empleadoService = empleadoService;
        this.cargaExcelService = cargaExcelService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    @Operation(summary = "Listar empleados paginado")
    public PageResponse<EmpleadoResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q) {
        return PageResponses.of(
                PageResponses.search(empleadoService.listar(), q, e ->
                        PageResponses.text(e.codigoEmpleado(), e.nombreCompleto(), e.area(), e.cargo())),
                page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EmpleadoResponse obtener(@PathVariable Integer id) {
        return empleadoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public EmpleadoResponse crear(@Valid @RequestBody EmpleadoRequest request) {
        return empleadoService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public EmpleadoResponse actualizar(@PathVariable Integer id, @Valid @RequestBody EmpleadoRequest request) {
        return empleadoService.actualizar(id, request);
    }

    @GetMapping("/plantilla-excel")
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    @Operation(summary = "Descargar plantilla Excel de trabajadores")
    public ResponseEntity<byte[]> plantilla() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla_trabajadores.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cargaExcelService.plantilla());
    }

    @PostMapping(value = "/carga-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    @Operation(summary = "Cargar trabajadores desde Excel")
    public CargaResponse cargar(@RequestPart("archivo") MultipartFile archivo) {
        return cargaExcelService.cargar(archivo);
    }
}
