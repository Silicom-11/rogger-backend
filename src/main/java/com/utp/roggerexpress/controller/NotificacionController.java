package com.utp.roggerexpress.controller;

import com.utp.roggerexpress.dto.NotificacionResponse;
import com.utp.roggerexpress.model.EstadoNotificacion;
import com.utp.roggerexpress.security.UsuarioActual;
import com.utp.roggerexpress.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bitacora de avisos al cliente: consulta y reenvio de los que fallaron.
 */
@RestController
@RequestMapping("/api/v1/notificaciones")
@Tag(name = "Notificaciones", description = "Avisos enviados al destinatario")
public class NotificacionController {

    private static final int LIMITE_MAXIMO = 200;

    private final NotificacionService notificacionService;
    private final UsuarioActual usuarioActual;

    public NotificacionController(NotificacionService notificacionService, UsuarioActual usuarioActual) {
        this.notificacionService = notificacionService;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    @Operation(summary = "Bitacora de avisos al cliente, acotada a la agencia del usuario")
    public ResponseEntity<List<NotificacionResponse>> listar(
            @RequestParam(required = false) EstadoNotificacion estado,
            @RequestParam(defaultValue = "50") int limite) {

        int tope = Math.min(Math.max(1, limite), LIMITE_MAXIMO);

        return ResponseEntity.ok(notificacionService
                .listar(usuarioActual.alcance(), estado, tope)
                .stream().map(NotificacionResponse::from).toList());
    }

    @PostMapping("/{id}/reintento")
    @Operation(summary = "Vuelve a despachar un aviso que no pudo entregarse")
    public ResponseEntity<NotificacionResponse> reintentar(@PathVariable String id) {
        return ResponseEntity.ok(NotificacionResponse.from(notificacionService.reintentar(id)));
    }
}
