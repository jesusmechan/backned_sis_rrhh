package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.dto.AppDtos.MenuGrupoResponse;
import pe.andina.rrhh.dto.AppDtos.SesionResponse;
import pe.andina.rrhh.service.SesionService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Sesión", description = "Perfil autenticado y menú asociado")
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    @GetMapping("/sesion")
    @Operation(summary = "Perfil, permisos y menú del usuario autenticado")
    public SesionResponse sesion() {
        return sesionService.actual();
    }

    @GetMapping("/menu")
    @Operation(summary = "Opciones de menú del perfil actual")
    public List<MenuGrupoResponse> menu() {
        return sesionService.actual().menu();
    }
}
