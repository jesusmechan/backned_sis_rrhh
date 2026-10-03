package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Permisos;
import pe.andina.rrhh.domain.model.enums.FormatoReporte;
import pe.andina.rrhh.domain.model.enums.TipoReporte;
import pe.andina.rrhh.application.dto.AppDtos.EmpleadoResponse;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.application.dto.AppDtos.MarcacionResponse;
import pe.andina.rrhh.application.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.application.dto.AppDtos.UsuarioResponse;
import pe.andina.rrhh.application.port.in.ReporteUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Reportes", description = "Consultas JSON y exportación Excel/PDF")
public class ReporteController {

    private final ReporteUseCase reporteService;
    private final CurrentUserPort currentUser;

    public ReporteController(ReporteUseCase reporteService, CurrentUserPort currentUser) {
        this.reporteService = reporteService;
        this.currentUser = currentUser;
    }

    @GetMapping("/trabajadores")
    @PreAuthorize("hasAuthority('PERSONAL_CONSULTAR')")
    public List<EmpleadoResponse> trabajadores() { return reporteService.trabajadores(); }

    @GetMapping("/permisos")
    @PreAuthorize("hasAuthority('REPORTE_PERMISOS')")
    public List<PermisoResponse> permisos() { return reporteService.permisos(); }

    @GetMapping("/horas-extras")
    @PreAuthorize("hasAuthority('REPORTE_HORAS_EXTRAS')")
    public List<HoraExtraResponse> horasExtras() { return reporteService.horasExtras(); }

    @GetMapping("/asistencia")
    @PreAuthorize("hasAuthority('REPORTE_ASISTENCIA')")
    public List<MarcacionResponse> asistencia(
            @RequestParam(required = false) Integer idEmpleado,
            @RequestParam(required = false) java.time.LocalDate desde,
            @RequestParam(required = false) java.time.LocalDate hasta) {
        return reporteService.asistencia(idEmpleado, desde, hasta);
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('REPORTE_USUARIOS')")
    public List<UsuarioResponse> usuarios() { return reporteService.usuarios(); }

    @GetMapping("/{tipo}/excel")
    @Operation(summary = "Exportar reporte Excel", description = "tipo: TRABAJADORES, PERMISOS, HORAS_EXTRAS, ASISTENCIA, USUARIOS")
    public ResponseEntity<byte[]> excel(@PathVariable TipoReporte tipo) {
        exigirPermiso(tipo);
        return archivo(reporteService.exportar(tipo, FormatoReporte.EXCEL), tipo.name().toLowerCase() + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/{tipo}/pdf")
    @Operation(summary = "Exportar reporte PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable TipoReporte tipo) {
        exigirPermiso(tipo);
        return archivo(reporteService.exportar(tipo, FormatoReporte.PDF), tipo.name().toLowerCase() + ".pdf",
                MediaType.APPLICATION_PDF_VALUE);
    }

    private void exigirPermiso(TipoReporte tipo) {
        String permiso = switch (tipo) {
            case TRABAJADORES -> Permisos.PERSONAL_CONSULTAR;
            case PERMISOS -> Permisos.REPORTE_PERMISOS;
            case HORAS_EXTRAS -> Permisos.REPORTE_HORAS_EXTRAS;
            case ASISTENCIA -> Permisos.REPORTE_ASISTENCIA;
            case USUARIOS -> Permisos.REPORTE_USUARIOS;
        };
        if (!currentUser.tienePermiso(permiso)) {
            throw DomainException.forbidden("Su perfil no tiene acceso a este reporte");
        }
    }

    private ResponseEntity<byte[]> archivo(byte[] body, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
