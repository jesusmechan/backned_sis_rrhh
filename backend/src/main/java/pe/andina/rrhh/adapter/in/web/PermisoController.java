package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import pe.andina.rrhh.application.dto.AppDtos.AdjuntoEntrada;
import pe.andina.rrhh.application.dto.AppDtos.ArchivoDescarga;
import pe.andina.rrhh.application.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.application.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.application.dto.AppDtos.RequisitosAdjuntoResponse;
import pe.andina.rrhh.application.port.in.SolicitudUseCase;
import pe.andina.rrhh.domain.exception.DomainException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/permisos")
@Tag(name = "Permisos", description = "Solicitudes de permiso y su historial")
public class PermisoController {

    private final SolicitudUseCase solicitudService;

    public PermisoController(SolicitudUseCase solicitudService) {
        this.solicitudService = solicitudService;
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

    @GetMapping("/requisitos-adjunto")
    @Operation(summary = "Actividades del flujo y cuáles exigen sustento")
    public RequisitosAdjuntoResponse requisitos(@RequestParam Integer idTipoPermiso) {
        return solicitudService.requisitosAdjunto(idTipoPermiso);
    }

    @GetMapping("/{id}")
    public PermisoResponse obtener(@PathVariable Integer id) {
        return solicitudService.obtenerPermiso(id);
    }

    @GetMapping("/{id}/adjuntos/{idAdjunto}")
    @Operation(summary = "Descargar un sustento de la solicitud")
    public ResponseEntity<byte[]> descargar(@PathVariable Integer id, @PathVariable Integer idAdjunto) {
        ArchivoDescarga archivo = solicitudService.descargarAdjunto(id, idAdjunto);
        MediaType media = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (archivo.tipoContenido() != null && !archivo.tipoContenido().isBlank()) {
                media = MediaType.parseMediaType(archivo.tipoContenido());
            }
        } catch (Exception ignored) {
            media = MediaType.APPLICATION_OCTET_STREAM;
        }
        String nombre = archivo.nombre() == null ? "adjunto" : archivo.nombre().replaceAll("[\\r\\n\"]", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(media)
                .body(archivo.contenido());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registrar permiso", description = "PostgreSQL instancia el flujo de aprobación al insertar")
    public PermisoResponse crear(@Valid @RequestBody PermisoRequest request) {
        return solicitudService.crearPermiso(request, List.of());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Registrar permiso con sustentos", description = "Un archivo por cada actividad del flujo que lo exige")
    public PermisoResponse crearConAdjuntos(
            @RequestPart("datos") @Valid PermisoRequest request,
            @RequestPart(value = "archivos", required = false) List<MultipartFile> archivos,
            @RequestParam(value = "numeroPaso", required = false) List<Integer> numeroPaso) {
        List<MultipartFile> files = archivos == null ? List.of() : archivos;
        List<Integer> numeros = numeroPaso == null ? List.of() : numeroPaso;
        if (files.size() != numeros.size()) {
            throw DomainException.badRequest("Cada archivo debe indicar la actividad a la que sustenta");
        }
        List<AdjuntoEntrada> entradas = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            byte[] contenido;
            try {
                contenido = file.getBytes();
            } catch (IOException ex) {
                throw DomainException.badRequest("No se pudo leer el archivo adjunto");
            }
            entradas.add(new AdjuntoEntrada(numeros.get(i), file.getOriginalFilename(), file.getContentType(), contenido));
        }
        return solicitudService.crearPermiso(request, entradas);
    }

    @PostMapping("/{id}/cancelar")
    public PermisoResponse cancelar(@PathVariable Integer id) {
        return solicitudService.cancelarPermiso(id);
    }

    @GetMapping("/{id}/historial")
    public List<HistorialResponse> historial(@PathVariable Integer id) {
        return solicitudService.historialPermiso(id);
    }
}
