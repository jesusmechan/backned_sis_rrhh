package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
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
import pe.andina.rrhh.application.dto.AppDtos.CesarOrganigramaRequest;
import pe.andina.rrhh.application.dto.AppDtos.OrganigramaAreaResponse;
import pe.andina.rrhh.application.dto.AppDtos.OrganigramaNodoResponse;
import pe.andina.rrhh.application.dto.AppDtos.OrganigramaResponse;
import pe.andina.rrhh.application.dto.AppDtos.PromoverOrganigramaRequest;
import pe.andina.rrhh.application.dto.AppDtos.ReasignarOrganigramaRequest;
import pe.andina.rrhh.application.port.in.OrganigramaUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/organigrama")
@Tag(name = "Organigrama", description = "Árbol jerárquico, áreas y movilidad de cargos")
public class OrganigramaController {

    private final OrganigramaUseCase organigramaService;

    public OrganigramaController(OrganigramaUseCase organigramaService) {
        this.organigramaService = organigramaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIGRAMA_CONSULTAR') or hasAuthority('PERSONAL_CONSULTAR')")
    @Operation(summary = "Obtener el organigrama completo (árbol por jefe inmediato)")
    public OrganigramaResponse arbol(
            @RequestParam(required = false, defaultValue = "false") boolean incluirInactivos) {
        return organigramaService.arbol(incluirInactivos);
    }

    @GetMapping("/areas")
    @PreAuthorize("hasAuthority('ORGANIGRAMA_CONSULTAR') or hasAuthority('PERSONAL_CONSULTAR')")
    @Operation(summary = "Vista por área: jefe de área y colaboradores")
    public List<OrganigramaAreaResponse> areas(
            @RequestParam(required = false, defaultValue = "false") boolean incluirInactivos) {
        return organigramaService.areas(incluirInactivos);
    }

    @GetMapping("/equipo/{idEmpleado}")
    @PreAuthorize("hasAuthority('ORGANIGRAMA_CONSULTAR') or hasAuthority('PERSONAL_CONSULTAR') or hasAuthority('ALCANCE_EQUIPO')")
    @Operation(summary = "Equipo directo e indirecto bajo un jefe")
    public OrganigramaNodoResponse equipo(
            @PathVariable Integer idEmpleado,
            @RequestParam(required = false, defaultValue = "false") boolean incluirInactivos) {
        return organigramaService.equipo(idEmpleado, incluirInactivos);
    }

    @PutMapping("/reasignar")
    @PreAuthorize("hasAuthority('ORGANIGRAMA_GESTIONAR') or hasAuthority('PERSONAL_ACTUALIZAR')")
    @Operation(summary = "Reasignar el jefe inmediato de uno o más colaboradores")
    public OrganigramaResponse reasignar(@Valid @RequestBody ReasignarOrganigramaRequest request) {
        return organigramaService.reasignar(request);
    }

    @PostMapping("/promover")
    @PreAuthorize("hasAuthority('ORGANIGRAMA_GESTIONAR') or hasAuthority('PERSONAL_ACTUALIZAR')")
    @Operation(summary = "Promover a un colaborador (p. ej. a jefe de área) y opcionalmente cederle el equipo")
    public OrganigramaResponse promover(@Valid @RequestBody PromoverOrganigramaRequest request) {
        return organigramaService.promover(request);
    }

    @PostMapping("/cesar")
    @PreAuthorize("hasAuthority('ORGANIGRAMA_GESTIONAR') or hasAuthority('PERSONAL_ACTUALIZAR')")
    @Operation(summary = "Cesar a un trabajador reasignando sus reportes (sucesor o nuevo jefe)")
    public OrganigramaResponse cesar(@Valid @RequestBody CesarOrganigramaRequest request) {
        return organigramaService.cesar(request);
    }
}
