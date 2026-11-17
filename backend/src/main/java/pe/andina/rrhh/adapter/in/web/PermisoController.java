package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.andina.rrhh.application.dto.PageResponses;
import pe.andina.rrhh.application.dto.AppDtos.AdjuntoArchivo;
import pe.andina.rrhh.application.dto.AppDtos.AdjuntoResponse;
import pe.andina.rrhh.application.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.application.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.application.port.in.AdjuntoUseCase;
import pe.andina.rrhh.application.port.in.SolicitudUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/permisos")
@Tag(name = "Permisos", description = "Solicitudes de permiso, historial y adjuntos de sustento")
public class PermisoController {

    private final SolicitudUseCase solicitudService;
    private final AdjuntoUseCase adjuntoService;

    public PermisoController(SolicitudUseCase solicitudService, AdjuntoUseCase adjuntoService) {
        this.solicitudService = solicitudService;
        this.adjuntoService = adjuntoService;
    }

    @GetMapping
    @Operation(summary = "Listar permisos paginado")
    public PageResponse<PermisoResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q) {
        return PageResponses.query(solicitudService.listarPermisos())
                .estados(estado, PermisoResponse::estado)
                .search(q, p -> PageResponses.text(p.empleado(), p.tipoPermiso(), p.motivo()))
                .pagina(page, size);
    }

    @GetMapping("/{id}")
    public PermisoResponse obtener(@PathVariable Integer id) {
        return solicitudService.obtenerPermiso(id);
    }

    @PostMapping
    @Operation(summary = "Registrar permiso", description = "PostgreSQL instancia el flujo de aprobación al insertar")
    public PermisoResponse crear(@Valid @RequestBody PermisoRequest request) {
        return solicitudService.crearPermiso(request);
    }

    @PostMapping("/{id}/cancelar")
    public PermisoResponse cancelar(@PathVariable Integer id) {
        return solicitudService.cancelarPermiso(id);
    }

    @GetMapping("/{id}/historial")
    public List<HistorialResponse> historial(@PathVariable Integer id) {
        return solicitudService.historialPermiso(id);
    }

    @GetMapping("/{id}/adjuntos")
    @Operation(summary = "Listar adjuntos de sustento del permiso")
    public List<AdjuntoResponse> listarAdjuntos(@PathVariable Integer id) {
        return adjuntoService.listarDePermiso(id);
    }

    @PostMapping(value = "/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir evidencia/sustento", description = "PDF, JPG, PNG, WEBP o Word. Máximo 5 MB. Opcional: idPaso del paso en curso.")
    public AdjuntoResponse subirAdjunto(
            @PathVariable Integer id,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam(required = false) Integer idPaso) {
        return adjuntoService.subirAPermiso(id, archivo, idPaso);
    }

    @GetMapping("/{id}/adjuntos/{idAdjunto}")
    @Operation(summary = "Descargar un adjunto")
    public ResponseEntity<byte[]> descargarAdjunto(@PathVariable Integer id, @PathVariable Integer idAdjunto) {
        AdjuntoArchivo archivo = adjuntoService.descargarDePermiso(id, idAdjunto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.nombreOriginal() + "\"")
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .body(archivo.contenido());
    }

    @DeleteMapping("/{id}/adjuntos/{idAdjunto}")
    @Operation(summary = "Eliminar un adjunto (solo solicitud pendiente)")
    public void eliminarAdjunto(@PathVariable Integer id, @PathVariable Integer idAdjunto) {
        adjuntoService.eliminarDePermiso(id, idAdjunto);
    }
}
