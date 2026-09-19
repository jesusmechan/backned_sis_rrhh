package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoItem;
import pe.andina.rrhh.application.dto.AppDtos.IdNombre;
import pe.andina.rrhh.application.dto.AppDtos.PermisoFuncionalItem;
import pe.andina.rrhh.application.port.in.CatalogoUseCase;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogos")
@Tag(name = "Catálogos", description = "Áreas, cargos, horarios, tipos de permiso, roles y parámetros")
public class CatalogoController {

    private final CatalogoUseCase catalogoService;

    public CatalogoController(CatalogoUseCase catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/areas")
    public List<IdNombre> areas() { return catalogoService.areas(); }

    @GetMapping("/cargos")
    public List<IdNombre> cargos() { return catalogoService.cargos(); }

    @GetMapping("/horarios")
    public List<IdNombre> horarios() { return catalogoService.horarios(); }

    @GetMapping("/tipos-permiso")
    public List<CatalogoItem> tiposPermiso() { return catalogoService.tiposPermiso(); }

    @GetMapping("/roles")
    public List<CatalogoItem> roles() { return catalogoService.roles(); }

    @GetMapping("/permisos-funcionales")
    public List<PermisoFuncionalItem> permisos() { return catalogoService.permisosFuncionales(); }

    @GetMapping("/parametros")
    public List<Map<String, String>> parametros() { return catalogoService.parametros(); }
}
