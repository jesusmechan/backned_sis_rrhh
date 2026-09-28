package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.PageResponses;
import pe.andina.rrhh.application.dto.AppDtos.BandejaItem;
import pe.andina.rrhh.application.dto.AppDtos.DecisionRequest;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.dto.AppDtos.PasoResponse;
import pe.andina.rrhh.application.port.in.SolicitudUseCase;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
@Tag(name = "Aprobación", description = "Bandeja y decisión de pasos")
public class AprobacionController {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final SolicitudUseCase solicitudService;

    public AprobacionController(SolicitudUseCase solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping("/bandeja")
    @Operation(summary = "Bandeja del usuario: pendientes o seguimiento, paginada",
            description = "Filtros: tipo, q, vista, estado, tramite, desde, hasta, orden=ASC|DESC")
    public PageResponse<BandejaItem> bandeja(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String vista,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tramite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String orden) {
        List<BandejaItem> items = solicitudService.bandeja(vista);
        boolean asc = orden != null && "ASC".equalsIgnoreCase(orden.trim());
        Comparator<java.time.OffsetDateTime> fechaCmp = asc
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();
        Comparator<BandejaItem> byFecha = Comparator.comparing(
                BandejaItem::fechaInicio,
                Comparator.nullsLast(fechaCmp));
        items = items.stream().sorted(byFecha).toList();

        String tramiteNeedle = tramite == null || tramite.isBlank()
                ? null
                : tramite.trim().toLowerCase(Locale.ROOT);

        return PageResponses.query(items)
                .donde(tipo == null || tipo.isBlank() ? null : item -> tipo.equalsIgnoreCase(item.tipoSolicitud()))
                .estadosTexto(estado, BandejaItem::estadoSolicitud)
                .donde(tramiteNeedle == null ? null : item -> {
                    String t = item.tipoTramite();
                    return t != null && t.toLowerCase(Locale.ROOT).contains(tramiteNeedle);
                })
                .donde(desde == null && hasta == null ? null : item -> enRango(item, desde, hasta))
                .search(q, item -> PageResponses.text(
                        item.solicitante(), item.tipoSolicitud(), item.tipoTramite(),
                        item.motivo(), item.estadoSolicitud(), item.nombrePaso()))
                .pagina(page, size);
    }

    private static boolean enRango(BandejaItem item, LocalDate desde, LocalDate hasta) {
        if (item.fechaInicio() == null) {
            return false;
        }
        LocalDate dia = item.fechaInicio().atZoneSameInstant(LIMA).toLocalDate();
        if (desde != null && dia.isBefore(desde)) {
            return false;
        }
        if (hasta != null && dia.isAfter(hasta)) {
            return false;
        }
        return true;
    }

    @GetMapping("/pasos/{id}")
    @Operation(summary = "Obtener un paso de bandeja si le corresponde al usuario")
    public BandejaItem paso(@PathVariable Integer id) {
        return solicitudService.pasoPendiente(id);
    }

    @PostMapping("/pasos/{id}/aprobar")
    @Operation(summary = "Aprobar un paso",
            description = "El id es idPasoSolicitud de la bandeja. Opcional: Idempotency-Key")
    public PasoResponse aprobar(@PathVariable Integer id, @Valid @RequestBody DecisionRequest request) {
        return solicitudService.decidir(id, true, request);
    }

    @PostMapping("/pasos/{id}/rechazar")
    @Operation(summary = "Rechazar un paso",
            description = "Opcional: cabecera Idempotency-Key para reintentos seguros")
    public PasoResponse rechazar(@PathVariable Integer id, @Valid @RequestBody DecisionRequest request) {
        return solicitudService.decidir(id, false, request);
    }
}
