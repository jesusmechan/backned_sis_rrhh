package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.domain.enums.FormatoReporte;
import pe.andina.rrhh.domain.enums.TipoReporte;
import pe.andina.rrhh.dto.AppDtos.EmpleadoResponse;
import pe.andina.rrhh.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.dto.AppDtos.MarcacionResponse;
import pe.andina.rrhh.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.dto.AppDtos.UsuarioResponse;
import pe.andina.rrhh.service.ReporteService;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('ADMIN','RRHH')")
@Tag(name = "Reportes", description = "Consultas JSON y exportación Excel/PDF")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/trabajadores")
    public List<EmpleadoResponse> trabajadores() { return reporteService.trabajadores(); }

    @GetMapping("/permisos")
    public List<PermisoResponse> permisos() { return reporteService.permisos(); }

    @GetMapping("/horas-extras")
    public List<HoraExtraResponse> horasExtras() { return reporteService.horasExtras(); }

    @GetMapping("/asistencia")
    public List<MarcacionResponse> asistencia() { return reporteService.asistencia(); }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> usuarios() { return reporteService.usuarios(); }

    @GetMapping("/{tipo}/excel")
    @Operation(summary = "Exportar reporte Excel", description = "tipo: TRABAJADORES, PERMISOS, HORAS_EXTRAS, ASISTENCIA, USUARIOS")
    public ResponseEntity<byte[]> excel(@PathVariable TipoReporte tipo) {
        return archivo(reporteService.exportar(tipo, FormatoReporte.EXCEL), tipo.name().toLowerCase() + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/{tipo}/pdf")
    @Operation(summary = "Exportar reporte PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable TipoReporte tipo) {
        return archivo(reporteService.exportar(tipo, FormatoReporte.PDF), tipo.name().toLowerCase() + ".pdf",
                MediaType.APPLICATION_PDF_VALUE);
    }

    private ResponseEntity<byte[]> archivo(byte[] body, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
