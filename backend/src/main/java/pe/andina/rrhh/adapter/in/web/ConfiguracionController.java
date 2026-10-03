package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.port.in.CatalogoUseCase;

import java.util.Map;

@RestController
@RequestMapping("/api/configuracion")
@Tag(name = "Configuración", description = "Marca y formato regional visibles antes de iniciar sesión")
public class ConfiguracionController {

    private final CatalogoUseCase catalogoService;

    public ConfiguracionController(CatalogoUseCase catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/publica")
    @Operation(summary = "Parámetros de ámbito PUBLICO (sin autenticación)")
    public Map<String, String> publica() {
        return catalogoService.configuracionPublica();
    }
}
