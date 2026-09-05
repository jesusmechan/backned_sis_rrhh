package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.dto.AppDtos.AuditoriaResponse;
import pe.andina.rrhh.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.service.ConsultaService;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','RRHH')")
@Tag(name = "Auditoría", description = "Bitácora y trazabilidad de solicitudes")
public class AuditoriaController {

    private final ConsultaService consultaService;

    public AuditoriaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping("/auditoria")
    public List<AuditoriaResponse> auditoria() {
        return consultaService.auditoria();
    }

    @GetMapping("/trazabilidad")
    public List<HistorialResponse> trazabilidad() {
        return consultaService.trazabilidad();
    }
}
