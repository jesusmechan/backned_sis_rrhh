package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.AppDtos.ContadorNotificaciones;
import pe.andina.rrhh.application.dto.AppDtos.NotificacionResponse;
import pe.andina.rrhh.application.port.in.NotificacionUseCase;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Campana de trámites y push STOMP")
public class NotificacionController {

    private final NotificacionUseCase notificacionService;

    public NotificacionController(NotificacionUseCase notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    @Operation(summary = "Listar notificaciones del usuario autenticado")
    public List<NotificacionResponse> listar() {
        return notificacionService.listar();
    }

    @GetMapping("/no-leidas")
    @Operation(summary = "Cantidad de notificaciones no leídas")
    public ContadorNotificaciones noLeidas() {
        return notificacionService.noLeidas();
    }

    @PostMapping("/{id}/leer")
    @Operation(summary = "Marcar una notificación como leída")
    public NotificacionResponse leer(@PathVariable Integer id) {
        return notificacionService.marcarLeida(id);
    }

    @PostMapping("/leer-todas")
    @Operation(summary = "Marcar todas como leídas")
    public void leerTodas() {
        notificacionService.marcarTodas();
    }
}
