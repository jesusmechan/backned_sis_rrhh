package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.AppDtos.AfpRequest;
import pe.andina.rrhh.application.dto.AppDtos.AfpResponse;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoTipoResponse;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoValorRequest;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoValorResponse;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.dto.AppDtos.RegimenLaboralRequest;
import pe.andina.rrhh.application.dto.AppDtos.RegimenLaboralResponse;
import pe.andina.rrhh.application.dto.AppDtos.TramoQuintaRequest;
import pe.andina.rrhh.application.dto.AppDtos.TramoQuintaResponse;
import pe.andina.rrhh.application.dto.AppDtos.VigenciaRequest;
import pe.andina.rrhh.application.dto.AppDtos.VigenciaResponse;
import pe.andina.rrhh.application.dto.PageResponses;
import pe.andina.rrhh.application.port.in.ReglasMaestroUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/maestros")
@PreAuthorize("hasAuthority('MAESTRO_GESTIONAR')")
@Tag(name = "Maestros de reglas", description = "Catálogos, AFP, regímenes laborales, tramos de 5ta y valores con vigencia")
public class ReglasMaestroController {

    private final ReglasMaestroUseCase service;

    public ReglasMaestroController(ReglasMaestroUseCase service) {
        this.service = service;
    }

    @GetMapping("/catalogo-tipos")
    public List<CatalogoTipoResponse> tiposCatalogo() {
        return service.tiposCatalogo();
    }

    @GetMapping("/catalogos")
    public PageResponse<CatalogoValorResponse> catalogos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(service.valoresCatalogo())
                .search(q, v -> PageResponses.text(v.codigo(), v.nombre(), v.tipoNombre()))
                .donde(tipo == null || tipo.isBlank() ? null : v -> tipo.equals(v.tipo()))
                .donde(activo == null ? null : v -> activo.equals(v.activo()))
                .pagina(page, size);
    }

    @PostMapping("/catalogos")
    public CatalogoValorResponse crearValor(@Valid @RequestBody CatalogoValorRequest request) {
        return service.crearValor(request);
    }

    @PutMapping("/catalogos/{id}")
    public CatalogoValorResponse actualizarValor(@PathVariable Integer id, @Valid @RequestBody CatalogoValorRequest request) {
        return service.actualizarValor(id, request);
    }

    @GetMapping("/afps")
    public PageResponse<AfpResponse> afps(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(service.afps())
                .search(q, a -> PageResponses.text(a.codigo(), a.nombre()))
                .donde(activo == null ? null : a -> activo.equals(a.activo()))
                .pagina(page, size);
    }

    @PostMapping("/afps")
    public AfpResponse crearAfp(@Valid @RequestBody AfpRequest request) {
        return service.crearAfp(request);
    }

    @PutMapping("/afps/{codigo}")
    public AfpResponse actualizarAfp(@PathVariable String codigo, @Valid @RequestBody AfpRequest request) {
        return service.actualizarAfp(codigo, request);
    }

    @GetMapping("/regimenes-laborales")
    public PageResponse<RegimenLaboralResponse> regimenes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(service.regimenes())
                .search(q, g -> PageResponses.text(g.codigo(), g.nombre(), g.descripcion()))
                .donde(activo == null ? null : g -> activo.equals(g.activo()))
                .pagina(page, size);
    }

    @PostMapping("/regimenes-laborales")
    public RegimenLaboralResponse crearRegimen(@Valid @RequestBody RegimenLaboralRequest request) {
        return service.crearRegimen(request);
    }

    @PutMapping("/regimenes-laborales/{codigo}")
    public RegimenLaboralResponse actualizarRegimen(@PathVariable String codigo, @Valid @RequestBody RegimenLaboralRequest request) {
        return service.actualizarRegimen(codigo, request);
    }

    @GetMapping("/tramos-quinta")
    public PageResponse<TramoQuintaResponse> tramos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(service.tramos())
                .donde(activo == null ? null : t -> activo.equals(t.activo()))
                .pagina(page, size);
    }

    @PostMapping("/tramos-quinta")
    public TramoQuintaResponse crearTramo(@Valid @RequestBody TramoQuintaRequest request) {
        return service.crearTramo(request);
    }

    @PutMapping("/tramos-quinta/{id}")
    public TramoQuintaResponse actualizarTramo(@PathVariable Integer id, @Valid @RequestBody TramoQuintaRequest request) {
        return service.actualizarTramo(id, request);
    }

    @GetMapping("/vigencias")
    public PageResponse<VigenciaResponse> vigencias(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q) {
        return PageResponses.query(service.vigencias())
                .search(q, v -> PageResponses.text(v.clave(), v.descripcion()))
                .pagina(page, size);
    }

    @PostMapping("/vigencias")
    public VigenciaResponse crearVigencia(@Valid @RequestBody VigenciaRequest request) {
        return service.crearVigencia(request);
    }

    @PutMapping("/vigencias/{id}")
    public VigenciaResponse actualizarVigencia(@PathVariable Integer id, @Valid @RequestBody VigenciaRequest request) {
        return service.actualizarVigencia(id, request);
    }

    @DeleteMapping("/vigencias/{id}")
    public void eliminarVigencia(@PathVariable Integer id) {
        service.eliminarVigencia(id);
    }
}
