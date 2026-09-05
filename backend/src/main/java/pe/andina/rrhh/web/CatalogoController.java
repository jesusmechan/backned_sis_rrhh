package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.dto.AppDtos.CatalogoItem;
import pe.andina.rrhh.dto.AppDtos.IdNombre;
import pe.andina.rrhh.service.CatalogoService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogos")
@Tag(name = "Catálogos", description = "Áreas, cargos, horarios, tipos de permiso, roles y parámetros")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
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
    public List<CatalogoItem> permisos() { return catalogoService.permisosFuncionales(); }

    @GetMapping("/parametros")
    public List<Map<String, String>> parametros() { return catalogoService.parametros(); }
}
